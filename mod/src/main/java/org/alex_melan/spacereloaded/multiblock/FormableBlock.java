package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.Collection;

/**
 * Блок-член мультиблока (005, облик сформированных структур в духе Immersive Engineering):
 * свойство {@code formed} переключает модель и хитбокс — по виду сразу понятно, что структура
 * активна (ячейки стека соединены шиной, тарелки сливаются в колонну, футеровка — в оболочку).
 */
public class FormableBlock extends Block {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    private final VoxelShape formedShape;

    public FormableBlock(Properties properties) {
        this(properties, Shapes.block());
    }

    public FormableBlock(Properties properties, VoxelShape formedShape) {
        super(properties);
        this.formedShape = formedShape;
        registerDefaultState(getStateDefinition().any().setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FORMED) ? formedShape : Shapes.block();
    }

    /** Выставить облик клеткам структуры (только блокам-членам, без обновления соседей). */
    public static void apply(ServerLevel level, Collection<BlockPos> cells, boolean formed) {
        for (BlockPos pos : cells) {
            if (!level.isLoaded(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof FormableBlock && state.getValue(FORMED) != formed) {
                level.setBlock(pos, state.setValue(FORMED, formed), Block.UPDATE_CLIENTS);
            }
        }
    }
}
