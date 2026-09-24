package org.alex_melan.spacereloaded.core.station;

import org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere;

/**
 * Шлюзовой цикл (007, FR-506/FR-507, D74), изотермически. Насос быстроты S на объёме V:
 * p(t) = p₀·e^(−S·t/V), время до p_стоп: t = (V/S)·ln(p₀/p_стоп); ниже p_стоп насос неэффективен —
 * остаток стравливается: m = p_стоп·V·M/(R·T). Без насоса теряется весь газ тамбура. Люк площади A
 * при перепаде Δp прижат силой F = Δp·A (1 м² при 101 кПа — ~10 т): поэтому интерлок.
 */
public final class AirlockCycle {

    /** Порог насоса, кПа (реальность МКС Quest: ~13–14 кПа). */
    public static final double PUMP_STOP_KPA = 14.0;
    /** Быстрота пластинчато-роторного насоса, м³/с (**оценка** ~180 м³/ч). */
    public static final double PUMP_SPEED = 0.05;
    /** Перепад, при котором люк ещё открывается рукой с рычагом, кПа. */
    public static final double HATCH_OPEN_KPA = 1.0;
    private static final double M_AIR = 0.028965;

    private AirlockCycle() {
    }

    public static double pumpSeconds(double volumeM3, double speedM3PerS, double fromKpa, double toKpa) {
        if (fromKpa <= toKpa) {
            return 0;
        }
        return volumeM3 / speedM3PerS * Math.log(fromKpa / toKpa);
    }

    /** Масса воздуха при давлении p в объёме V, кг. */
    public static double airMass(double pressureKpa, double volumeM3) {
        return pressureKpa * 1000 * volumeM3 * M_AIR / (CabinAtmosphere.R * CabinAtmosphere.T_CABIN);
    }

    /** Потеря за цикл с насосом (стравливание остатка). */
    public static double ventLoss(double volumeM3, double stopKpa) {
        return airMass(stopKpa, volumeM3);
    }

    /** Сила на люк, Н. */
    public static double hatchForce(double deltaKpa, double areaM2) {
        return deltaKpa * 1000 * areaM2;
    }

    public static boolean hatchOpens(double deltaKpa) {
        return Math.abs(deltaKpa) <= HATCH_OPEN_KPA;
    }
}
