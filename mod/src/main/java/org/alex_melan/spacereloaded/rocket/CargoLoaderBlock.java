package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Погрузчик: ПКМ циклит режим (погрузка/разгрузка/выкл). */
public class CargoLoaderBlock extends Block implements EntityBlock {

    public CargoLoaderBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CargoLoaderBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != ModBlockEntities.CARGO_LOADER) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                CargoLoaderBlockEntity.serverTick((CargoLoaderBlockEntity) blockEntity,
                        (ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CargoLoaderBlockEntity loader
                && player instanceof ServerPlayer serverPlayer) {
            CargoLoaderBlockEntity.Mode mode = loader.cycleMode();
            serverPlayer.sendOverlayMessage(Component.translatable(
                    "message.spacereloaded.cargo_loader.mode."
                            + mode.name().toLowerCase(java.util.Locale.ROOT)));
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
