package org.alex_melan.spacereloaded.core.electronics;

import java.util.SplittableRandom;

/**
 * Выход годных кристаллов (006, FR-424, D63/D65): модель Пуассона Y = e^(−D·A), число кристаллов
 * на пластину DPW = π·d²/(4A) − π·d/√(2A) (стандартная формула с потерями по краю). Плотность
 * дефектов D = Σ по слоям (d₀ + s·C̄/C₅) + примеси 0.01·10^(9 − N9): каждая недостающая девятка
 * чистоты ×10 «убийственных» дефектов. Кластеризация дефектов (отрицательное биномиальное) опущена.
 */
public final class DieYield {

    /** Собственные дефекты процесса на слой, 1/см². */
    public static final double INTRINSIC_PER_LAYER = 0.01;
    /** Калибровка осаждения частиц: при C = C₅ (предел ISO 5) — 0.05 1/см² на слой. */
    public static final double PARTICLE_PER_LAYER = 0.05;

    private DieYield() {
    }

    public static int diesPerWafer(double waferDiameterCm, double dieAreaCm2) {
        double dpw = Math.PI * waferDiameterCm * waferDiameterCm / (4 * dieAreaCm2)
                - Math.PI * waferDiameterCm / Math.sqrt(2 * dieAreaCm2);
        return Math.max(0, (int) Math.floor(dpw));
    }

    public static double yield(double defectDensity, double dieAreaCm2) {
        return Math.exp(-defectDensity * dieAreaCm2);
    }

    /** Дефекты одного литографического слоя при средней концентрации частиц C̄ (на м³). */
    public static double layerDefects(double meanConcentration) {
        return INTRINSIC_PER_LAYER + PARTICLE_PER_LAYER * meanConcentration / CleanroomAir.ISO5_LIMIT;
    }

    /** «Убийственные» дефекты от примесей недочищенного кремния. */
    public static double impurityDefects(double n9) {
        return 0.01 * Math.pow(10, Math.max(0, Purity.ELECTRONIC_GRADE - n9));
    }

    /** Годные кристаллы: Binomial(DPW, Y) по зерну. */
    public static int sampleGood(long seed, int dies, double yield) {
        SplittableRandom random = new SplittableRandom(seed);
        int good = 0;
        for (int i = 0; i < dies; i++) {
            if (random.nextDouble() < yield) {
                good++;
            }
        }
        return good;
    }
}
