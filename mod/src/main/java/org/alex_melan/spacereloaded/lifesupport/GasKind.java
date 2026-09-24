package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.util.StringRepresentable;
import org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere;

/**
 * Газ бака (007, D71): баллон 1 м³ при 20 МПа и 293 K — m = p·V·M/(R·T): O₂ 263 кг, N₂ 230 кг.
 */
public enum GasKind implements StringRepresentable {
    NONE(0), OXYGEN(CabinAtmosphere.M_O2), NITROGEN(CabinAtmosphere.M_N2);

    public static final double TANK_PRESSURE_PA = 20e6;

    public final double molarMass;

    GasKind(double molarMass) {
        this.molarMass = molarMass;
    }

    /** Ёмкость бака 1 м³ при 20 МПа, кг. */
    public double capacityKg() {
        return this == NONE ? 0 : TANK_PRESSURE_PA * molarMass / (CabinAtmosphere.R * CabinAtmosphere.T_CABIN);
    }

    @Override
    public String getSerializedName() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
