package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * Секция катушек рельса катапульты (004, FR-200/FR-201): тир задаёт предельное ускорение
 * (конфиг). {@code IN_RAIL}/{@code AXIS} — визуал «в составе рельса» с направлением,
 * выставляется казёнником при сборке линии.
 */
public class CoilBlock extends Block {

    public static final BooleanProperty IN_RAIL = BooleanProperty.create("in_rail");
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;

    private final int tier;

    public CoilBlock(Properties properties, int tier) {
        super(properties);
        this.tier = tier;
        registerDefaultState(getStateDefinition().any().setValue(IN_RAIL, false).setValue(AXIS, Direction.Axis.X));
    }

    public int tier() {
        return tier;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(IN_RAIL, AXIS);
    }
}
