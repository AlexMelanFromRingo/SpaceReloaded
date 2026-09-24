package org.alex_melan.spacereloaded.core.electronics;

/**
 * Выращивание монокристалла по Чохральскому (006, FR-414, D62). Затравка вытягивается из расплава
 * с вращением; колебания скорости срывают фронт кристаллизации — растёт поликристалл. Порог —
 * среднее относительное отклонение оборотов 5 % за операцию (игровая калибровка). Легирующая
 * примесь распределяется по Шайлю C_s = k·C₀·(1 − g)^(k−1): годна часть слитка, где сопротивление
 * в допуске r: g_max = 1 − r^(−1/(1−k)) (фосфор k = 0.35, r = 1.5 → 46 %), остальное — в переплав.
 */
public final class CrystalGrowth {

    public static final double STABILITY_LIMIT = 0.05;

    private CrystalGrowth() {
    }

    public static boolean monocrystalline(double meanSpeedDeviation) {
        return meanSpeedDeviation <= STABILITY_LIMIT;
    }

    /** Коэффициенты сегрегации в кремнии: фосфор (n-тип), алюминий (p-тип), бор (примесь). */
    public static final double K_PHOSPHORUS = 0.35;
    public static final double K_ALUMINIUM = 0.002;
    public static final double K_BORON = 0.8;
    /** Допуск удельного сопротивления вдоль слитка (±, отношение). */
    public static final double RESISTIVITY_TOLERANCE = 1.5;

    /**
     * Разделение примеси между годной частью слитка [0, g] и хвостом расплава (Шайль, масса
     * примеси сохраняется). Средняя концентрация в твёрдом на [0, g]: C̄_s/C₀ = (1 − (1 − g)^k)/g;
     * хвост получает остаток: C_t/C₀ = (1 − g·C̄_s/C₀)/(1 − g).
     *
     * @return {ratio твёрдого, ratio хвоста} — множители к исходной доле примеси
     */
    public static double[] segregate(double k, double g) {
        if (g <= 0) {
            return new double[] {k, 1};
        }
        double solid = (1 - Math.pow(1 - g, k)) / g;
        double tail = g >= 1 ? 0 : (1 - g * solid) / (1 - g);
        return new double[] {solid, tail};
    }

    /** Годная доля слитка по Шайлю. */
    public static double usableFraction(double segregationCoefficient, double resistivityTolerance) {
        return 1 - Math.pow(resistivityTolerance, -1 / (1 - segregationCoefficient));
    }
}
