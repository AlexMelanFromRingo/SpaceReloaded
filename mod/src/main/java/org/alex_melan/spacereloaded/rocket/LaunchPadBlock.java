package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Стартовая площадка: цельный блок; прямоугольник плит задаёт площадь сборки.
 * FORMED — визуальная подсветка сформированного комплекса (пад ≥ 9 плит +
 * пилон ≥ 3): жёлтая разметка.
 */
public class LaunchPadBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public LaunchPadBlock(Properties properties) {
        super(properties);
        registerDefaultState(getStateDefinition().any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos,
                           BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            RocketInteractions.formComplex(serverLevel, pos);
        }
    }
}
