package org.alex_melan.spacereloaded.core.thermal;

/**
 * Цвет свечения нагретого тела (008, D81) — аппроксимация кривой Планка в sRGB (Tanner Helland,
 * 1000–40000 K); ниже 1000 K — тот же тёмно-красный с яркостью по Стефану (T/1000)⁴, ниже ~750 K
 * свечения глаз не видит (закон Дрейпера ≈ 798 K).
 */
public final class Blackbody {

    public static final double DRAPER_K = 798;

    private Blackbody() {
    }

    /** Цвет 0xRRGGBB при температуре T (без учёта яркости). */
    public static int rgb(double kelvin) {
        double t = Math.max(1000, Math.min(40000, kelvin)) / 100;
        double r = t <= 66 ? 255 : 329.698727446 * Math.pow(t - 60, -0.1332047592);
        double g = t <= 66 ? 99.4708025861 * Math.log(t) - 161.1195681661 : 288.1221695283 * Math.pow(t - 60, -0.0755148492);
        double b = t >= 66 ? 255 : t <= 19 ? 0 : 138.5177312231 * Math.log(t - 10) - 305.0447927307;
        return (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /** Яркость свечения 0…1: невидимо ниже точки Дрейпера, дальше ∝ T⁴ (насыщение к 1400 K). */
    public static double glow(double kelvin) {
        if (kelvin <= DRAPER_K) {
            return 0;
        }
        return Math.min(1, (Math.pow(kelvin, 4) - Math.pow(DRAPER_K, 4)) / (Math.pow(1400, 4) - Math.pow(DRAPER_K, 4)));
    }

    private static int clamp(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
