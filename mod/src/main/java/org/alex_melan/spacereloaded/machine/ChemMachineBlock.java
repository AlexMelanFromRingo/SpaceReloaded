package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** Химическая машина Марса (GUI нет): FACING, серверный тикер, статус по ПКМ. */
public class ChemMachineBlock<T extends ChemMachineBlockEntity> extends Block implements EntityBlock,
        org.alex_melan.spacereloaded.registry.CosmeticState {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BiFunction<BlockPos, BlockState, T> factory;
    private final Supplier<BlockEntityType<T>> typeSupplier;

    public ChemMachineBlock(Properties properties, BiFunction<BlockPos, BlockState, T> factory,
                            Supplier<BlockEntityType<T>> typeSupplier) {
        super(properties);
        this.factory = factory;
        this.typeSupplier = typeSupplier;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH)
                .setValue(MachineActivity.ACTIVE, false));
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return before.getValue(FACING) == after.getValue(FACING);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MachineActivity.ACTIVE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
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
        return (tickLevel, pos, tickState, be) -> {
            T machine = (T) be;
            machine.serverTick((ServerLevel) tickLevel);
            MachineActivity.update((ServerLevel) tickLevel, pos, machine.getBlockState(), machine);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ChemMachineBlockEntity machine
                && player instanceof ServerPlayer serverPlayer && level instanceof ServerLevel serverLevel) {
            if (machine instanceof net.minecraft.world.MenuProvider menu && !player.isSecondaryUseActive()) {
                serverPlayer.openMenu(menu); // 006: у реактора Сабатье есть окно (режим газ-твёрдое)
            } else {
                serverPlayer.sendSystemMessage(machine.status(serverLevel));
            }
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
