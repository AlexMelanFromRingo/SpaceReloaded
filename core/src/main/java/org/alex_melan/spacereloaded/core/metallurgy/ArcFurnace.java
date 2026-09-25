package org.alex_melan.spacereloaded.core.metallurgy;

/**
 * Дуговая сталеплавильная печь (008, FR-650…FR-652, D86). Энергия плавки — нагрев до выпуска
 * (ΔT ≈ 1575 K, c ≈ 0.75 кДж/(кг·K)) и плавление (L = 272 кДж/кг) при КПД дуги 0.85 —
 * 0.475 кВт·ч/кг. Кислородная продувка выжигает углерод шихты с 2 % до 0.2 % (C + ½O₂ → CO,
 * −110.5 кДж/моль): 0.166 МДж/кг тепла, 24 г O₂ на кг. Электроды — 1.8 кг графита на тонну.
 */
public final class ArcFurnace {

    public static final double C_STEEL = 750;
    public static final double DELTA_T = 1575;
    public static final double LATENT = 272e3;
    public static final double ARC_EFFICIENCY = 0.85;
    public static final double CARBON_BURNED = 0.018;
    public static final double CO_HEAT_J_PER_MOL = 110.5e3;
    public static final double ELECTRODE_KG_PER_T = 1.8;
    public static final double SLAG_LOSS = 0.05;

    private ArcFurnace() {
    }

    /** Электроэнергия плавки m кг, Дж; с продувкой — минус экзотермия выгорания углерода. */
    public static double meltJ(double massKg, boolean oxygenBlow) {
        double heat = massKg * (C_STEEL * DELTA_T + LATENT);
        if (oxygenBlow) {
            heat -= massKg * CARBON_BURNED / 0.012 * CO_HEAT_J_PER_MOL;
        }
        return heat / ARC_EFFICIENCY;
    }

    /** Кислород продувки, кг. */
    public static double oxygenKg(double massKg) {
        return massKg * CARBON_BURNED / 0.012 * 0.5 * 0.032;
    }

    public static double seconds(double massKg, double powerW, boolean oxygenBlow) {
        return meltJ(massKg, oxygenBlow) / powerW;
    }

    public static double electrodeKg(double massKg) {
        return electrodeKg(massKg, ELECTRODE_KG_PER_T);
    }

    public static double electrodeKg(double massKg, double kgPerTonne) {
        return massKg / 1000 * kgPerTonne;
    }

    public static double steelKg(double massKg) {
        return massKg * (1 - SLAG_LOSS);
    }
}
