package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.alex_melan.spacereloaded.registry.ModTags;

import java.util.Map;

/**
 * Энергокабель: жила 6×6×6 с отводами к соседям. Соединение — по тегу
 * {@code #spacereloaded:energy_connectable} (кабели и все энергоблоки),
 * состояние обновляется через getStateForPlacement/updateShape.
 */
public class CableBlock extends Block {

    public static final Map<Direction, BooleanProperty> CONNECTIONS = Map.of(
            Direction.NORTH, BlockStateProperties.NORTH,
            Direction.SOUTH, BlockStateProperties.SOUTH,
            Direction.EAST, BlockStateProperties.EAST,
            Direction.WEST, BlockStateProperties.WEST,
            Direction.UP, BlockStateProperties.UP,
            Direction.DOWN, BlockStateProperties.DOWN);

    private static final VoxelShape CORE = Block.box(5, 5, 5, 11, 11, 11);
    private static final Map<Direction, VoxelShape> ARMS = Map.of(
            Direction.NORTH, Block.box(5, 5, 0, 11, 11, 5),
            Direction.SOUTH, Block.box(5, 5, 11, 11, 11, 16),
            Direction.WEST, Block.box(0, 5, 5, 5, 11, 11),
            Direction.EAST, Block.box(11, 5, 5, 16, 11, 11),
            Direction.DOWN, Block.box(5, 0, 5, 11, 5, 11),
            Direction.UP, Block.box(5, 11, 5, 11, 16, 11));

    public CableBlock(Properties properties) {
        super(properties);
        BlockState state = getStateDefinition().any();
        for (BooleanProperty property : CONNECTIONS.values()) {
            state = state.setValue(property, false);
        }
        registerDefaultState(state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.NORTH, BlockStateProperties.SOUTH,
                BlockStateProperties.EAST, BlockStateProperties.WEST,
                BlockStateProperties.UP, BlockStateProperties.DOWN);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        BlockPos pos = context.getClickedPos();
        for (Direction dir : Direction.values()) {
            state = state.setValue(CONNECTIONS.get(dir),
                    connects(context.getLevel().getBlockState(pos.relative(dir))));
        }
        return state;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks,
                                     BlockPos pos, Direction direction, BlockPos neighborPos,
                                     BlockState neighborState, RandomSource random) {
        return state.setValue(CONNECTIONS.get(direction), connects(neighborState));
    }

    private static boolean connects(BlockState neighbor) {
        return neighbor.is(ModTags.ENERGY_CONNECTABLE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        VoxelShape shape = CORE;
        for (Direction dir : Direction.values()) {
            if (state.getValue(CONNECTIONS.get(dir))) {
                shape = Shapes.or(shape, ARMS.get(dir));
            }
        }
        return shape;
    }
}
