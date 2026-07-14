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
    /**
     * Ниже этого остаток считается нулём. Капля топлива Transfer API это доли
     * грамма (напр. керолокс 24/81000 кг); без порога слив трубой оставлял бы
     * долю капли, из-за которой бак читался пустым, но был заперт на своём типе.
     */
    private static final double MIN_PROPELLANT_KG = 1.0e-3;

    private double propellantKg;
    /** Тип топлива в баке; пустая строка — бак пуст и примет любой. */
    private String fuelType = "";
    /** Тот же бак глазами Fabric Transfer API: трубы соседних модов. */
    private final org.alex_melan.spacereloaded.fluid.FuelTankFluidStorage fluidStorage =
            new org.alex_melan.spacereloaded.fluid.FuelTankFluidStorage(this);

    public FuelTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_TANK, pos, state);
    }

    public org.alex_melan.spacereloaded.fluid.FuelTankFluidStorage fluidStorage() {
        return fluidStorage;
    }

    public double propellantKg() {
        return propellantKg;
    }

    public double capacityKg() {
        return CAPACITY_KG;
    }

    public String fuelType() {
        return propellantKg > 0 ? fuelType : "";
    }

    public void setPropellant(double value, String type) {
        propellantKg = Math.clamp(value, 0, CAPACITY_KG);
        fuelType = propellantKg > 0 ? type : "";
        normalizeResidue();
        setChanged();
    }

    /** @return сколько реально принято (пустой бак принимает любой тип) */
    public double fill(double amountKg, String type) {
        if (propellantKg > 0 && !fuelType.equals(type)) {
            return 0; // смешивание топлив запрещено
        }
        double accepted = Math.min(amountKg, CAPACITY_KG - propellantKg);
        if (accepted > 0) {
            propellantKg += accepted;
            fuelType = type;
            setChanged();
        }
        return accepted;
    }

    public double drain(double amountKg) {
        double drained = Math.min(amountKg, propellantKg);
        propellantKg -= drained;
        normalizeResidue();
        setChanged();
        return drained;
    }

    /**
     * Остаток меньше одной капли текущего топлива это округление Transfer API,
     * а не топливо: обнуляем и снимаем тип. Иначе бак читается пустым
     * ({@code getAmount()==0}), но заперт на своём типе и не принимает другой.
     */
    private void normalizeResidue() {
        if (propellantKg <= 0) {
            propellantKg = 0;
            fuelType = "";
            return;
        }
        var propellant = org.alex_melan.spacereloaded.fluid.ModFluids.byFuelId(fuelType);
        double oneDroplet = propellant == null ? MIN_PROPELLANT_KG
                : propellant.kgPerBucket()
                        / net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET;
        if (propellantKg < oneDroplet) {
            propellantKg = 0;
            fuelType = "";
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("propellant", propellantKg);
        output.putString("fuel_type", fuelType);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        propellantKg = Math.clamp(input.getDoubleOr("propellant", 0), 0, CAPACITY_KG);
        fuelType = input.getStringOr("fuel_type", "");
    }
}
