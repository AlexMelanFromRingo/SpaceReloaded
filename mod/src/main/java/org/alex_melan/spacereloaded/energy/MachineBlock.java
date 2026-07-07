package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Универсальный блок-машина: фабрика BE + серверный тикер. */
public class MachineBlock<T extends MachineBlockEntity> extends Block implements EntityBlock {

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final Supplier<BlockEntityType<T>> typeSupplier;
    private final Ticker<T> ticker;

    @FunctionalInterface
    public interface Ticker<T> {
        void tick(T blockEntity, ServerLevel level);
    }

    public MachineBlock(Properties properties, BiFunction<BlockPos, BlockState, T> factory,
                        Supplier<BlockEntityType<T>> typeSupplier, Ticker<T> ticker) {
        super(properties);
        this.factory = factory;
        this.typeSupplier = typeSupplier;
        this.ticker = ticker;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return factory.apply(pos, state);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <B extends BlockEntity> BlockEntityTicker<B> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<B> type) {
        if (level.isClientSide() || type != typeSupplier.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) ->
                ticker.tick((T) blockEntity, (ServerLevel) tickLevel);
    }
}
