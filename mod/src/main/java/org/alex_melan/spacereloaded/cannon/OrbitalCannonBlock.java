package org.alex_melan.spacereloaded.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.energy.MachineBlock;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Блок орбитальной пушки: ПКМ ломом — зарядить, ПКМ целеуказателем — навести
 * на его метку, ПКМ пустой рукой — открыть терминал наведения и огня.
 */
public class OrbitalCannonBlock extends MachineBlock<OrbitalCannonBlockEntity> {

    public OrbitalCannonBlock(Properties properties) {
        super(properties, OrbitalCannonBlockEntity::new,
                () -> ModBlockEntities.ORBITAL_CANNON, OrbitalCannonBlockEntity::serverTick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, InteractionHand hand,
                                          BlockHitResult hitResult) {
        if (!stack.is(ModItems.TUNGSTEN_ROD) && !stack.is(ModItems.TARGETING_DESIGNATOR)) {
            // НЕ TRY_WITH_EMPTY_HAND: иначе клик киркой/факелом провалился бы
            // в useWithoutItem и произвёл выстрел (проверено по пайплайну 26.2)
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof OrbitalCannonBlockEntity cannon)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (stack.is(ModItems.TUNGSTEN_ROD)) {
            if (cannon.loadRod()) {
                stack.consume(1, player);
                serverPlayer.sendOverlayMessage(Component.translatable(
                        "message.spacereloaded.cannon.rod_loaded",
                        cannon.rods(), org.alex_melan.spacereloaded.SpaceReloaded.config().cannonMaxRods));
            } else {
                serverPlayer.sendOverlayMessage(
                        Component.translatable("message.spacereloaded.cannon.magazine_full"));
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        // Целеуказатель: перенести метку с пульта в пушку
        GlobalPos mark = TargetingDesignatorItem.mark(stack);
        if (mark == null) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.spacereloaded.cannon.no_mark"));
        } else {
            cannon.setTarget(mark);
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.cannon.target_set",
                    mark.pos().toShortString(), mark.dimension().identifier().toString()));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof OrbitalCannonBlockEntity cannon
                && player instanceof ServerPlayer serverPlayer) {
            // Пустая рука открывает терминал: стрельба — кнопкой в нём,
            // случайный клик по пушке больше не отправляет лом на планету
            net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
                    serverPlayer, cannon.snapshot((ServerLevel) level));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
