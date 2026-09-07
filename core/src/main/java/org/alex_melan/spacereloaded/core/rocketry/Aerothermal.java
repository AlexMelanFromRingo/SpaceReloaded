package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Аэротермодинамика входа (Полёт 2.0, FR-083/FR-084).
 *
 * <p>Индекс нагрева — закон Саттона-Грейвса q̇ = k·√(ρ/R_n)·v³ без коэффициента k
 * и радиуса затупления R_n: они постоянны для «типовой» ракеты и свёрнуты
 * в порог конфига (упрощение задокументировано). Скоростной напор q = ½ρv².
 */
public final class Aerothermal {

    private Aerothermal() {
    }

    /** Индекс теплового потока √ρ·v³ (безразмерная шкала, сравнивается с порогом). */
    public static double heatIndex(double density, double speed) {
        if (density <= 0 || speed <= 0) {
            return 0;
        }
        return Math.sqrt(density) * speed * speed * speed;
    }

    /** Скоростной напор ½ρv², Па. */
    public static double dynamicPressure(double density, double speed) {
        if (density <= 0) {
            return 0;
        }
        return 0.5 * density * speed * speed;
    }
}
