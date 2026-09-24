package org.alex_melan.spacereloaded.rocket;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import org.alex_melan.spacereloaded.core.industry.EngineQuality;

/**
 * Ракетный двигатель с качеством изготовления (005, FR-332/FR-334, D56): уровень 0…10 — часть
 * состояния блока, поэтому переживает сборку ракеты в сущность и разборку обратно. Уровень по
 * умолчанию {@value EngineQuality#DEFAULT_LEVEL} соответствует табличным характеристикам —
 * двигатели, стоявшие в мирах до 005, не меняются.
 */
public class EngineBlock extends Block {

    public static final IntegerProperty QUALITY = IntegerProperty.create("quality", 0, 10);

    public EngineBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(QUALITY, EngineQuality.DEFAULT_LEVEL));
    }

    /** Хитбокс по модели: монтажная плита, горловина, раструб сопла. */
    private static final net.minecraft.world.phys.shapes.VoxelShape SHAPE = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(0, 12, 0, 16, 16, 16), Block.box(5, 8, 5, 11, 12, 11),
            Block.box(3, 4, 3, 13, 8, 13), Block.box(1, 0, 1, 15, 4, 15));

    @Override
    protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state,
            net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(QUALITY);
    }
}
