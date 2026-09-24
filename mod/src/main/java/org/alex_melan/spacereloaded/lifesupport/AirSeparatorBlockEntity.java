package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Воздухоразделительная установка (007, D71): криогенная ректификация воздуха тела с атмосферой —
 * 0.4 кВт·ч на килограмм воздуха (реальные установки 0.2–0.5), 23.1 % массы — кислород, 76.9 % —
 * азот (аргон 1.3 % отнесён к азоту как буферный газ). Продукты уходят в соседние газовые баки,
 * невостребованный продукт стравливается (разделитель не встаёт). Работает только там, где есть
 * воздух: на Земле. Производительность 0.2 кг/с (малая мембранно-криогенная установка).
 */
public class AirSeparatorBlockEntity extends MachineBlockEntity implements org.alex_melan.spacereloaded.machine.MachineActivity.Source {

    public static final double AIR_KG_PER_S = 0.2;
    public static final double O2_MASS_FRACTION = 0.231;
    /** 0.4 кВт·ч/кг × 31 E на кВт·ч (масштаб 006). */
    public static final double ENERGY_PER_KG = 0.4 * 31;

    private double energyDebt;
    private boolean working;
    private int activeHold;

    public AirSeparatorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIR_SEPARATOR, pos, state, 4000, 400, 0);
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        boolean air = PlanetManager.profileFor(level).map(p -> p.breathable()).orElse(true);
        energyDebt += AIR_KG_PER_S * ENERGY_PER_KG;
        long cost = (long) Math.floor(energyDebt);
        working = air && energy.amount >= cost && hasTank(level);
        if (!working) {
            energyDebt = Math.min(energyDebt, AIR_KG_PER_S * ENERGY_PER_KG);
            return;
        }
        energy.amount -= cost;
        energyDebt -= cost;
        GasTankBlockEntity.pushToNeighbors(level, getBlockPos(), GasKind.OXYGEN, AIR_KG_PER_S * O2_MASS_FRACTION);
        GasTankBlockEntity.pushToNeighbors(level, getBlockPos(), GasKind.NITROGEN, AIR_KG_PER_S * (1 - O2_MASS_FRACTION));
        setChanged();
    }

    private boolean hasTank(ServerLevel level) {
        for (var d : net.minecraft.core.Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(d)) instanceof GasTankBlockEntity) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isWorking() {
        return working;
    }

    @Override
    public int activeHold() {
        return activeHold;
    }

    @Override
    public void setActiveHold(int hold) {
        activeHold = hold;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("debt", energyDebt);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energyDebt = input.getDoubleOr("debt", 0);
    }
}
