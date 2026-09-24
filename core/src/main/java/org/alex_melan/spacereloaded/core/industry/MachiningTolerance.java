package org.alex_melan.spacereloaded.core.industry;

/**
 * Допуски многоступенчатой обработки (005, FR-331, D56). Операция станка вносит погрешность
 * δ = δ_ст·(1 + k·|ω/ω_ном − 1|) — отклонение скорости шпинделя от режима резания ухудшает
 * поверхность и размер. Погрешности независимых операций складываются по сумме квадратов
 * (RSS — стандартный расчёт размерных цепей): δ_Σ = √(Σδᵢ²). Качество q = clamp(1 − δ_Σ/δ_брак);
 * δ_Σ > δ_брак — брак. Балансировка ротора — единственная операция, уменьшающая погрешность
 * (дисбаланс вдвое: Σδ² /= 4).
 */
public final class MachiningTolerance {

    private MachiningTolerance() {
    }

    public static double operationDelta(double machineDeltaUm, double speedDeviation, double speedFactor) {
        return machineDeltaUm * (1 + speedFactor * Math.abs(speedDeviation));
    }

    /** Относительное отклонение скорости |ω/ω_ном − 1|. */
    public static double deviation(double omega, double nominal) {
        return nominal <= 0 ? 0 : Math.abs(Math.abs(omega) / nominal - 1);
    }

    public static double accumulate(double deltaSqSum, double deltaUm) {
        return deltaSqSum + deltaUm * deltaUm;
    }

    public static double balance(double deltaSqSum) {
        return deltaSqSum / 4.0;
    }

    public static boolean isScrap(double deltaSqSum, double scrapUm) {
        return Math.sqrt(deltaSqSum) > scrapUm;
    }

    public static double quality(double deltaSqSum, double scrapUm) {
        return Math.max(0, Math.min(1, 1 - Math.sqrt(deltaSqSum) / scrapUm));
    }
}
