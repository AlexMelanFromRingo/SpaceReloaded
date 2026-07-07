package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * РИТЭГ в стиле KSP: центральный цилиндр (аппроксимирован колонной) с четырьмя
 * радиаторными пластинами. Частичная форма, но герметичен через тег airtight
 * (осознанно: сам агрегат — герметичный узел).
 */
public class RtgBlock extends MachineBlock<RtgBlockEntity> {

    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(4, 0, 4, 12, 16, 12),   // колонна-цилиндр
            Block.box(0, 2, 7, 16, 14, 9),    // пластины X
            Block.box(7, 2, 0, 9, 14, 16));   // пластины Z

    public RtgBlock(Properties properties) {
        super(properties, RtgBlockEntity::new,
                () -> ModBlockEntities.RTG, RtgBlockEntity::serverTick);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                  CollisionContext context) {
        return SHAPE;
    }
}
