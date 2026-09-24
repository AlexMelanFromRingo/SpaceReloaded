package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;

/**
 * Фрикционная муфта (005, FR-306): редстоун размыкает сеть; момент выше предела проскальзывания
 * — срабатывание предохранителя (TRIPPED) до следующего переднего фронта редстоуна.
 */
public class ClutchBlock extends KineticAxisBlock {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final BooleanProperty TRIPPED = BooleanProperty.create("tripped");

    public ClutchBlock(Properties properties) {
        super(properties, Kind.CLUTCH, 2.0, Material.STEEL, false, 0.5,
                KineticBlockEntity::new, () -> org.alex_melan.spacereloaded.registry.ModBlockEntities.KINETIC);
        registerDefaultState(defaultBlockState().setValue(POWERED, false).setValue(TRIPPED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(POWERED, TRIPPED);
    }

    @Override
    public boolean transmitsAxially(BlockState state) {
        return !state.getValue(POWERED) && !state.getValue(TRIPPED);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock,
                                   Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, orientation, movedByPiston);
        if (!(level instanceof ServerLevel)) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            BlockState next = state.setValue(POWERED, powered);
            if (powered) {
                next = next.setValue(TRIPPED, false); // передний фронт — взвести предохранитель заново
            }
            level.setBlock(pos, next, Block.UPDATE_ALL);
        }
    }
}
