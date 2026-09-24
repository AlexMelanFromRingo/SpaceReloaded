package org.alex_melan.spacereloaded.core.lifesupport;

/**
 * Метаболизм экипажа (007, FR-503/FR-504, D72): нормы NASA BVAD Rev2 (табл. 3-23) — 0.84 кг O₂ и
 * 1.01 кг CO₂ на человека в сутки (дыхательный коэффициент 0.87), ~12 МДж пищи. Животные — закон
 * Клейбера: обмен ∝ M^0.75 (человек 70 кг). Пороги эффектов — NASA OCHMO, NIOSH, OSHA.
 */
public final class Metabolism {

    public static final double O2_PER_DAY = 0.84;
    public static final double CO2_PER_DAY = 1.01;
    public static final double FOOD_MJ_PER_DAY = 12.0;
    public static final double HUMAN_KG = 70;

    /** Пороги pCO₂, кПа: предел МКС, головная боль, одышка, потеря сознания. */
    public static final double[] CO2_LEVELS = {0.53, 1.0, 3.0, 7.0};
    /** Пороги pO₂, кПа (сверху вниз): норма, работоспособность, координация. */
    public static final double[] O2_LEVELS = {19.5, 16.0, 10.0};

    private Metabolism() {
    }

    /** Обмен животного массой M в «людях». */
    public static double kleiber(double massKg) {
        return Math.pow(massKg / HUMAN_KG, 0.75);
    }

    /** Уровень отравления CO₂: 0 — норма … 4 — потеря сознания. */
    public static int co2Level(double partialKpa) {
        int level = 0;
        for (double threshold : CO2_LEVELS) {
            if (partialKpa >= threshold) {
                level++;
            }
        }
        return level;
    }

    /** Уровень гипоксии: 0 — норма … 3 — потеря сознания. */
    public static int hypoxiaLevel(double partialKpa) {
        int level = 0;
        for (double threshold : O2_LEVELS) {
            if (partialKpa < threshold) {
                level++;
            }
        }
        return level;
    }
}
