package org.alex_melan.spacereloaded.core.sealing;

/**
 * Первопорядковое перемешивание в объёме: V·dc/dt = G − k·c (G — приток, k — объёмный расход
 * удаления). Решение в замкнутой форме: c(t) = c_ss + (c₀ − c_ss)·e^(−t/τ), c_ss = G/k, τ = V/k;
 * при k = 0 — линейный рост c₀ + G·t/V. Общая математика воздуха зоны: частицы чистой комнаты
 * (006) и газы жизнеобеспечения (007) — один закон с разными G и k. Время в тех единицах, в
 * которых заданы G и k.
 */
public final class FirstOrderMix {

    private FirstOrderMix() {
    }

    public static double steadyState(double source, double removal) {
        return removal <= 0 ? Double.POSITIVE_INFINITY : source / removal;
    }

    public static double tau(double volume, double removal) {
        return removal <= 0 ? Double.POSITIVE_INFINITY : volume / removal;
    }

    /** Концентрация через dt после c0. */
    public static double at(double c0, double source, double removal, double volume, double dt) {
        double t = Math.max(0, dt);
        if (removal <= 0) {
            return c0 + source * t / Math.max(1e-12, volume);
        }
        double css = source / removal;
        return css + (c0 - css) * Math.exp(-t * removal / volume);
    }

    /** ∫c dt на [0, dt] — точная форма. */
    public static double integral(double c0, double source, double removal, double volume, double dt) {
        double t = Math.max(0, dt);
        if (t == 0) {
            return 0;
        }
        if (removal <= 0) {
            return (c0 + at(c0, source, removal, volume, t)) / 2 * t;
        }
        double css = source / removal;
        double tau = volume / removal;
        return css * t + (c0 - css) * tau * (1 - Math.exp(-t / tau));
    }

    /**
     * Через сколько концентрация достигнет target (или +∞, если никогда): t* = τ·ln((c₀ − c_ss)/(c* − c_ss)),
     * при k = 0 — (c* − c₀)·V/G.
     */
    public static double timeTo(double c0, double source, double removal, double volume, double target) {
        if (c0 == target) {
            return 0;
        }
        if (removal <= 0) {
            double slope = source / Math.max(1e-12, volume);
            double t = (target - c0) / slope;
            return slope != 0 && t >= 0 ? t : Double.POSITIVE_INFINITY;
        }
        double css = source / removal;
        double ratio = (c0 - css) / (target - css);
        if (ratio <= 1 || Double.isNaN(ratio) || Double.isInfinite(ratio)) {
            return Double.POSITIVE_INFINITY; // цель за стационаром или по другую сторону
        }
        return volume / removal * Math.log(ratio);
    }
}
