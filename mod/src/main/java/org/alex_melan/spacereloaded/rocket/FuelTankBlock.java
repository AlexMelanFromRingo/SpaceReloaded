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
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.spacereloaded.fuel_tank.level",
                    String.format(Locale.ROOT, "%.0f", tank.propellantKg()),
                    String.format(Locale.ROOT, "%.0f", tank.capacityKg())));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
