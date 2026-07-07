package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Бак-блок с РЕАЛЬНЫМ хранилищем топлива (конец эпохи «полных баков бесплатно»):
 * сборка ракеты читает фактический запас, разборка возвращает остаток обратно.
 * Заправка — электролизёром по соседству (US6 ISRU).
 */
public class FuelTankBlockEntity extends BlockEntity {

    /** Должна совпадать с propellant_capacity_kg в part_properties/fuel_tank.json. */
    public static final double CAPACITY_KG = 2000.0;

    private double propellantKg;

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK, pos, state);
    }

    public double propellantKg() {
        return propellantKg;
    }

    public double capacityKg() {
        return CAPACITY_KG;
    }

    public void setPropellantKg(double value) {
        propellantKg = Math.clamp(value, 0, CAPACITY_KG);
        setChanged();
    }

    /** @return сколько реально принято */
    public double fill(double amountKg) {
        double accepted = Math.min(amountKg, CAPACITY_KG - propellantKg);
        if (accepted > 0) {
            propellantKg += accepted;
            setChanged();
        }
        return accepted;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("propellant", propellantKg);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        propellantKg = Math.clamp(input.getDoubleOr("propellant", 0), 0, CAPACITY_KG);
    }
}
