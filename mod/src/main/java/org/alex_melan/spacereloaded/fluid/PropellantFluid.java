package org.alex_melan.spacereloaded.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * Ракетное топливо как жидкость: керолокс, гидролокс, метанокс.
 *
 * <p>Ведёт себя как маловязкая холодная жидкость: растекается недалеко, не
 * порождает новых источников (бесконечного топлива из двух луж не бывает).
 *
 * <p>Комплект заполняется по частям: конструктор {@code LiquidBlock} уже
 * спрашивает у жидкости источник и поток, а ведро появляется только после
 * блока. Поэтому ссылки лежат в изменяемой ячейке {@link ModFluids.Cell},
 * а не в готовом record.
 */
public abstract class PropellantFluid extends FlowingFluid {

    private final ModFluids.Cell cell;

    protected PropellantFluid(ModFluids.Cell cell) {
        this.cell = cell;
    }

    /** Строковый id топлива, как он лежит в баках: {@code spacereloaded:kerolox}. */
    public String fuelId() {
        return cell.fuelId;
    }

    @Override
    public Fluid getFlowing() {
        return cell.flowing;
    }

    @Override
    public Fluid getSource() {
        return cell.source;
    }

    @Override
    public Item getBucket() {
        return cell.bucket;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return cell.block.defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    public boolean isSame(Fluid fluid) {
        return fluid == getSource() || fluid == getFlowing();
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false; // из двух луж не рождается третья: топливо не вода
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        // Смытые блоки просто исчезают: дропать их незачем
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 3;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 2; // сходит на нет вдвое быстрее воды
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 5;
    }

    @Override
    protected float getExplosionResistance() {
        return 100.0f;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos,
                                        Fluid fluid, Direction direction) {
        return direction == Direction.DOWN && !fluid.is(ModFluids.PROPELLANT);
    }

    /** Источник: полный уровень, не течёт. */
    public static class Source extends PropellantFluid {
        public Source(ModFluids.Cell cell) {
            super(cell);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    /** Поток: уровень в свойстве LEVEL. */
    public static class Flowing extends PropellantFluid {
        public Flowing(ModFluids.Cell cell) {
            super(cell);
        }

        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
