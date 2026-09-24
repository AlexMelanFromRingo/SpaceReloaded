package org.alex_melan.spacereloaded.core.comms;

/**
 * Где на небе цель антенны (008, D87) и как далеко. Небо Minecraft плоское: Солнце и Луна ходят по
 * одной дуге восток → зенит → запад; планеты кладутся на ту же дугу (эклиптика через зенит —
 * **упрощение**, документировано) с элонгацией из взаимного положения Земли и планеты на круговых
 * орбитах по синодической фазе φ (0 — противостояние): планета в (a·cos φ, a·sin φ) а. е., Земля в
 * (1, 0), Солнце в начале координат.
 */
public final class SkyGeometry {

    public static final double AU = 1.495978707e11;

    private SkyGeometry() {
    }

    /** Расстояние Земля — планета, м, при синодической фазе φ (рад) и радиусе орбиты a (а. е.). */
    public static double distanceM(double a, double phase) {
        return AU * Math.sqrt(1 + a * a - 2 * a * Math.cos(phase));
    }

    /** Элонгация планеты от Солнца, рад (0 — у Солнца, π — противостояние). */
    public static double elongation(double a, double phase) {
        double px = a * Math.cos(phase) - 1;
        double py = a * Math.sin(phase);
        // направление на Солнце из Земли — (−1, 0)
        double cos = -px / Math.hypot(px, py);
        return Math.acos(Math.max(-1, Math.min(1, cos)));
    }

    /**
     * Угол на дуге неба от восточного горизонта, рад: Солнце — 2π·t/24000 (t — время суток; 0 — восход),
     * Луна и противостоящие планеты — напротив; выше горизонта, если синус положителен.
     */
    public static double skyAngle(long dayTime, double elongationRad) {
        double sun = 2 * Math.PI * (Math.floorMod(dayTime, 24000L) / 24000.0);
        return sun + elongationRad;
    }

    public static boolean aboveHorizon(double skyAngle) {
        return Math.sin(skyAngle) > 0.02;
    }
}
