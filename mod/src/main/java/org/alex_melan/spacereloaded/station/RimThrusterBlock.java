package org.alex_melan.spacereloaded.station;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Двигатель обода (007, FR-519): сопло смотрит по FACING, тяга — в противоположную сторону;
 * работает при редстоун-сигнале, топливо — из баков сборки кольца. Как Джемини-11 раскручивал
 * связку с «Аджиной» двигателями: момент импульса L = I·ω оплачивается топливом m = L/(r·Isp·g₀).
 */
public class RimThrusterBlock extends Block {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    /** Тяга 2 кН, удельный импульс 300 с (керолокс, двигатель ориентации). */
    public static final double THRUST_N = 2000;
    public static final double ISP_S = 300;

    public RimThrusterBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getClickedFace());
    }
}
