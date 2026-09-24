package org.alex_melan.spacereloaded.core.materials;

/**
 * Турбина турбонасоса (006, FR-442, D68): при той же доле газа на турбину напор насоса Δp ∝ T_вх
 * → давление в камере p_c ∝ T_вх → тяга F ∝ p_c (горло то же). Потолок — охлаждение стенки
 * камеры (Бартц: q ∝ p_c^0.8): медная регенеративная рубашка держит до ×1.3 номинала.
 * Титановое колесо ~700 K (эталон), жаропрочный никелевый сплав ~1100 K → ×1.3.
 */
public final class TurbineLimit {

    public static final double REFERENCE_INLET_K = 700;
    public static final double SUPERALLOY_INLET_K = 1100;
    public static final double COOLING_CAP = 1.3;

    private TurbineLimit() {
    }

    public static double thrustMultiplier(double inletTemperatureK) {
        return Math.min(inletTemperatureK / REFERENCE_INLET_K, COOLING_CAP);
    }
}
