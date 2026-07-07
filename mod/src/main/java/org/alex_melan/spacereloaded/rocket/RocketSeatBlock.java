package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Кресло (role=seat в part_properties): в собранной ракете пассажиры
 * рассаживаются по креслам — стройте свои кабины.
 */
public class RocketSeatBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape CUSHION = Block.box(1, 0, 1, 15, 7, 15);
    private static final VoxelShape BACK_NORTH = Block.box(1, 7, 1, 15, 16, 4);
    private static final VoxelShape BACK_SOUTH = Block.box(1, 7, 12, 15, 16, 15);
    private static final VoxelShape BACK_WEST = Block.box(1, 7, 1, 4, 16, 15);
    private static final VoxelShape BACK_EAST = Block.box(12, 7, 1, 15, 16, 15);

    public RocketSeatBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Спинка — за спиной игрока
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        VoxelShape back = switch (state.getValue(FACING)) {
            case SOUTH -> BACK_SOUTH;
            case WEST -> BACK_WEST;
            case EAST -> BACK_EAST;
            default -> BACK_NORTH;
        };
        return Shapes.or(CUSHION, back);
    }
}
