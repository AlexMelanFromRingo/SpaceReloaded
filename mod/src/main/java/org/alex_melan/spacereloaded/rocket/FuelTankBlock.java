package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.Locale;

/** Бак: ПКМ показывает заполнение. */
public class FuelTankBlock extends Block implements EntityBlock {

    public FuelTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelTankBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank
                && player instanceof ServerPlayer serverPlayer) {
            Component fuelName = tank.fuelType().isEmpty()
                    ? Component.translatable("fuel.spacereloaded.empty")
                    : Component.translatable("fuel.spacereloaded."
                            + tank.fuelType().substring(tank.fuelType().indexOf(':') + 1));
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.spacereloaded.fuel_tank.level",
                    String.format(Locale.ROOT, "%.0f", tank.propellantKg()),
                    String.format(Locale.ROOT, "%.0f", tank.capacityKg()),
                    fuelName));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Ведро в руке: пустое наполняется топливом из бака, полное сливается в бак.
     * Ровно один ковш за клик, как у воды.
     */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state,
                                          Level level, BlockPos pos, Player player,
                                          net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hitResult) {
        boolean bucketInHand = stack.is(net.minecraft.world.item.Items.BUCKET)
                || org.alex_melan.spacereloaded.fluid.ModFluids.byBucket(stack.getItem()) != null;
        if (!bucketInHand) {
            // НЕ PASS: в 26.2 только TRY_WITH_EMPTY_HAND проваливается в
            // useWithoutItem, иначе ПКМ с предметом в руке не показал бы уровень
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof FuelTankBlockEntity tank)) {
            return InteractionResult.PASS;
        }
        if (stack.is(net.minecraft.world.item.Items.BUCKET)) {
            return fillBucket(stack, tank, level, pos, player, hand);
        }
        return emptyBucket(stack, tank, level, pos, player, hand);
    }

    private InteractionResult fillBucket(net.minecraft.world.item.ItemStack stack,
                                         FuelTankBlockEntity tank, Level level, BlockPos pos,
                                         Player player, net.minecraft.world.InteractionHand hand) {
        var propellant = org.alex_melan.spacereloaded.fluid.ModFluids.byFuelId(tank.fuelType());
        if (propellant == null || tank.propellantKg() < propellant.kgPerBucket()) {
            return InteractionResult.CONSUME; // нечего наливать
        }
        tank.drain(propellant.kgPerBucket());
        stack.shrink(1);
        var filled = new net.minecraft.world.item.ItemStack(propellant.bucket());
        if (!player.getInventory().add(filled)) {
            player.drop(filled, false);
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_FILL,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS_SERVER;
    }

    private InteractionResult emptyBucket(net.minecraft.world.item.ItemStack stack,
                                          FuelTankBlockEntity tank, Level level, BlockPos pos,
                                          Player player, net.minecraft.world.InteractionHand hand) {
        var propellant = org.alex_melan.spacereloaded.fluid.ModFluids.byBucket(stack.getItem());
        double accepted = tank.fill(propellant.kgPerBucket(), propellant.fuelId());
        if (accepted < propellant.kgPerBucket()) {
            // Половину ведра не льём: либо бак полон, либо в нём другое топливо
            tank.drain(accepted);
            return InteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) {
            player.setItemInHand(hand, new net.minecraft.world.item.ItemStack(
                    net.minecraft.world.item.Items.BUCKET));
        }
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.BUCKET_EMPTY,
                net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        return InteractionResult.SUCCESS_SERVER;
    }
}
