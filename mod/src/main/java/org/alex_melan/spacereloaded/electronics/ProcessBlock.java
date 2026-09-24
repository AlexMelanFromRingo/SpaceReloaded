package org.alex_melan.spacereloaded.electronics;

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
import org.alex_melan.spacereloaded.machine.MachineActivity;
import org.alex_melan.spacereloaded.registry.CosmeticState;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Блок процессной машины 006 (химический реактор, реактор осаждения, диффузионная печь,
 * литография, травильная ванна): FACING, {@code lit} в работе (свет, частицы), меню по ПКМ.
 * Смена {@code lit} косметическая — пересчёты зон и сетей не будит.
 */
public class ProcessBlock extends Block implements EntityBlock, CosmeticState {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private final BiFunction<BlockPos, BlockState, ? extends ProcessMachineBlockEntity> factory;
    private final Supplier<BlockEntityType<? extends ProcessMachineBlockEntity>> type;

    public ProcessBlock(Properties properties,
                        BiFunction<BlockPos, BlockState, ? extends ProcessMachineBlockEntity> factory,
                        Supplier<BlockEntityType<? extends ProcessMachineBlockEntity>> type) {
        super(properties);
        this.factory = factory;
        this.type = type;
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH)
                .setValue(MachineActivity.ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, MachineActivity.ACTIVE);
    }

    @Override
    public boolean onlyCosmetic(BlockState before, BlockState after) {
        return before.getValue(FACING) == after.getValue(FACING);
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
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != type.get()) {
            return null;
        }
        return (tickLevel, pos, tickState, be) -> {
            ProcessMachineBlockEntity machine = (ProcessMachineBlockEntity) be;
            machine.serverTick((ServerLevel) tickLevel);
            MachineActivity.update((ServerLevel) tickLevel, pos, machine.getBlockState(), machine);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                               BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof ProcessMachineBlockEntity machine
                && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(machine);
        }
        return InteractionResult.SUCCESS_SERVER;
    }
}
