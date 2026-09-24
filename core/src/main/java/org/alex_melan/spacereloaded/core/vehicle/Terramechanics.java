package org.alex_melan.spacereloaded.core.vehicle;

/**
 * Механика грунта Беккера (007, FR-522, D78) — тот же закон, по которому проектировали лунный
 * ровер LRV. Единицы Беккера: сила Н, длины см; k_c в Н/см^(n+1), k_φ в Н/см^(n+2), c в Н/см².
 * <ul>
 *   <li>осадка колеса: z = [3W / ((3−n)·(k_c/b + k_φ)·b·√D)]^(2/(2n+1));</li>
 *   <li>сопротивление уплотнению: R_c = b·(k_c/b + k_φ)·z^(n+1)/(n+1);</li>
 *   <li>тяга (Мор–Кулон + Яноши–Ханамото): H = (A·c + W·tan φ)·[1 − K/(sL)·(1 − e^(−sL/K))],
 *       L ≈ √(D·z) — длина контакта, A = b·L;</li>
 *   <li>скорость: v = η·P/F_сопр; расход — F·1 км/η.</li>
 * </ul>
 */
public final class Terramechanics {

    /** Грунт: n, k_c, k_φ, c, φ (°), K (см). */
    public record Soil(double n, double kc, double kphi, double cohesion, double phiDeg, double shearK) {
        /** Лунный реголит (Lunar Sourcebook, табл. 9.14; Carrier 2006). */
        public static final Soil LUNAR = new Soil(1.0, 0.14, 0.82, 0.017, 35, 1.78);
    }

    /** Колесо: ширина b и диаметр D, см. */
    public record Wheel(double widthCm, double diameterCm) {
        /** Колесо LRV: Ø 81 × 23 см. */
        public static final Wheel LRV = new Wheel(23, 81);
    }

    private Terramechanics() {
    }

    public static double sinkageCm(Soil soil, Wheel wheel, double loadN) {
        double n = soil.n();
        double k = soil.kc() / wheel.widthCm() + soil.kphi();
        double base = 3 * loadN / ((3 - n) * k * wheel.widthCm() * Math.sqrt(wheel.diameterCm()));
        return Math.pow(Math.max(0, base), 2 / (2 * n + 1));
    }

    /** Сопротивление уплотнению одного колеса, Н. */
    public static double compactionN(Soil soil, Wheel wheel, double loadN) {
        double z = sinkageCm(soil, wheel, loadN);
        double n = soil.n();
        double k = soil.kc() / wheel.widthCm() + soil.kphi();
        return wheel.widthCm() * k * Math.pow(z, n + 1) / (n + 1);
    }

    /** Тяга одного колеса при проскальзывании s (0…1), Н. */
    public static double tractionN(Soil soil, Wheel wheel, double loadN, double slip) {
        double z = sinkageCm(soil, wheel, loadN);
        double length = Math.sqrt(wheel.diameterCm() * z);
        double area = wheel.widthCm() * length;
        double hmax = area * soil.cohesion() + loadN * Math.tan(Math.toRadians(soil.phiDeg()));
        double sl = Math.max(1e-9, slip) * length;
        double factor = 1 - soil.shearK() / sl * (1 - Math.exp(-sl / soil.shearK()));
        return hmax * factor;
    }

    /**
     * Предельный склон (°) при проскальзывании s: баланс H(W·cosθ) − R_c(W·cosθ) = W·sinθ на
     * каждом колесе (нагрузка W — вес на колесо при g тела). Решение — бисекция.
     */
    public static double maxSlopeDeg(Soil soil, Wheel wheel, double weightPerWheelN, double slip) {
        double lo = 0;
        double hi = 60;
        for (int i = 0; i < 60; i++) {
            double mid = (lo + hi) / 2;
            double th = Math.toRadians(mid);
            double normal = weightPerWheelN * Math.cos(th);
            double net = tractionN(soil, wheel, normal, slip) - compactionN(soil, wheel, normal);
            if (net >= weightPerWheelN * Math.sin(th)) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        return lo;
    }

    /** Установившаяся скорость по ровному, м/с. */
    public static double maxSpeed(double motorWatts, double efficiency, double resistanceN) {
        return efficiency * motorWatts / resistanceN;
    }

    /** Расход по ровному, Вт·ч/км. */
    public static double whPerKm(double resistanceN, double efficiency) {
        return resistanceN * 1000 / efficiency / 3600;
    }
}
