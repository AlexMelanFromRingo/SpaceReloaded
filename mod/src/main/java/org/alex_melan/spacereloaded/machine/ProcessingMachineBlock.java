package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Блок станка: BE-фабрика, серверный тикер, открытие меню по ПКМ. */
public class ProcessingMachineBlock extends Block implements EntityBlock {

    private final BiFunction<BlockPos, BlockState, ? extends ProcessingMachineBlockEntity> factory;
    private final Supplier<BlockEntityType<? extends ProcessingMachineBlockEntity>> typeSupplier;

    public ProcessingMachineBlock(Properties properties,
                                  BiFunction<BlockPos, BlockState, ? extends ProcessingMachineBlockEntity> factory,
                                  Supplier<BlockEntityType<? extends ProcessingMachineBlockEntity>> typeSupplier) {
        super(properties);
        this.factory = factory;
        this.typeSupplier = typeSupplier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide() || type != typeSupplier.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                ((ProcessingMachineBlockEntity) blockEntity).serverTick((ServerLevel) tickLevel);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider provider
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
