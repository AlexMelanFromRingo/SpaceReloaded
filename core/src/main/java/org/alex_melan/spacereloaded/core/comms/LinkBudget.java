package org.alex_melan.spacereloaded.core.comms;

/**
 * Бюджет радиолинии (008, FR-660, D87), X-диапазон 8.4 ГГц как у DSN: P_r = P_t·G_t·G_r·(λ/4πR)²,
 * G = η·(πD/λ)² (η = 0.6), скорость = P_r/(k·T_sys·Eb/N₀), T_sys = 30 K, Eb/N₀ = 2.5 дБ (турбо-код).
 */
public final class LinkBudget {

    public static final double FREQ = 8.4e9;
    public static final double LAMBDA = 299_792_458.0 / FREQ;
    public static final double EFFICIENCY = 0.6;
    public static final double K = 1.380649e-23;
    public static final double T_SYS = 30;
    public static final double EB_N0 = Math.pow(10, 2.5 / 10);

    private LinkBudget() {
    }

    public static double gain(double diameterM) {
        return EFFICIENCY * Math.pow(Math.PI * diameterM / LAMBDA, 2);
    }

    /** Скорость линии, бит/с. */
    public static double rate(double txW, double txDishM, double rxDishM, double rangeM) {
        double pr = txW * gain(txDishM) * gain(rxDishM) * Math.pow(LAMBDA / (4 * Math.PI * rangeM), 2);
        return pr / (K * T_SYS * EB_N0);
    }
}
