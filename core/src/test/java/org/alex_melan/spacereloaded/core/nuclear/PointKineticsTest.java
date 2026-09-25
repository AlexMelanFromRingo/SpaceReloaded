package org.alex_melan.spacereloaded.core.nuclear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Кинетика и KRUSTY-подобное равновесие (research-design §2, SC-002, SC-003). */
class PointKineticsTest {

    @Test
    void periodAndPromptJump() {
        assertEquals((1 - 0.2) / (0.08 * 0.2), PointKinetics.period(0.2), 1e-9);   // 50 с при 20 ¢
        double p = PointKinetics.evolve(1e4, 0.2, 50);             // источник (12 мВт/с) на 10 кВт незаметен
        assertEquals(1e4 * Math.E, p, 5);
        assertEquals(100 * 1.0 / 0.8, PointKinetics.jump(100, 0, 0.2), 1e-9);
        assertTrue(PointKinetics.promptCritical(1.0));
    }

    @Test
    void rodCurveAndFuel() {
        assertEquals(0, PointKinetics.rodWorth(0, 5), 1e-12);
        assertEquals(2.5, PointKinetics.rodWorth(0.5, 5), 1e-12);
        assertEquals(5, PointKinetics.rodWorth(1, 5), 1e-12);
        assertEquals(2.78, PointKinetics.fuelExcess(28), 0.01);
        assertTrue(PointKinetics.fuelExcess(0.2 * 28) < 0);        // НОУ в той же корзине подкритичен
    }

    @Test
    void decayHeatAndBurnup() {
        double year = 3.15e7;
        assertEquals(0.064, PointKinetics.decayHeatFraction(1, year), 0.002);
        assertEquals(0.0107, PointKinetics.decayHeatFraction(3600, year), 0.001);
        // 10 кВт(т) год: ≈ 4.5 г U-235
        assertEquals(4.5e-3, PointKinetics.burnedKg(1e4 * year), 0.4e-3);
    }

    /** Быстрый ручной вывод стержня (2 %/с) к положению равновесия 1073 K: разгон обгоняет обратную связь — перегрев. */
    @Test
    void fastManualPullOverheats() {
        double alpha = 0.001, excess = PointKinetics.fuelExcess(28), worth = 5, t0 = 300;
        double hStar = rodFor(worth - excess + (1073 - t0) * alpha, worth);
        double h = 0, p = PointKinetics.SOURCE_W, temp = t0, rhoPrev = excess - worth, maxRho = -9, maxT = 0;
        for (int i = 0; i < 20 * 600; i++) {
            h = Math.min(hStar, h + 0.02 * 0.05);
            double rho = excess - worth + PointKinetics.rodWorth(h, worth) - alpha * (temp - t0);
            maxRho = Math.max(maxRho, rho);
            p = PointKinetics.evolve(PointKinetics.jump(p, rhoPrev, rho), rho, 0.05);
            rhoPrev = rho;
            temp = ReactorThermal.step(temp, p, ReactorThermal.extract(temp, 4, 1, 250)[0], 0.05);
            maxT = Math.max(maxT, temp);
        }
        assertTrue(maxRho < 1);                        // до мгновенной критичности не дошло...
        assertTrue(maxT > ReactorThermal.MELT_K);      // ...но зона перегрелась выше плавления
    }

    /** Регулятор выводит реактор на 1073 K без перегрева, ≈ 1 кВт(э); реактор следует за нагрузкой. */
    @Test
    void regulatorStartsAndFollowsLoad() {
        double alpha = 0.001, excess = PointKinetics.fuelExcess(28), worth = 5, t0 = 300, set = 1073;
        double h = 0, p = PointKinetics.SOURCE_W, temp = t0, rhoPrev = excess - worth, maxT = 0;
        double[] out = ReactorThermal.extract(temp, 4, 1, 250);
        int stirlings = 4;
        for (int i = 0; i < 20 * 7200; i++) {
            if (i == 20 * 3600) {
                assertEquals(1049, out[1], 20);
                assertEquals(set, temp, 3);
                stirlings = 2;
            }
            double rho = excess - worth + PointKinetics.rodWorth(h, worth) - alpha * (temp - t0);
            double dTdt = (p - out[0]) / ReactorThermal.HEAT_CAPACITY_J_PER_K;
            double lo = Math.max(0, h - 1e-3), hi = Math.min(1, h + 1e-3);
            double slope = (PointKinetics.rodWorth(hi, worth) - PointKinetics.rodWorth(lo, worth)) / (hi - lo);
            h = Math.max(0, Math.min(1, h + ReactorRegulator.rodDelta(rho, temp, dTdt, set, slope, 0.01 * 0.05)));
            rho = excess - worth + PointKinetics.rodWorth(h, worth) - alpha * (temp - t0);
            p = PointKinetics.evolve(PointKinetics.jump(p, rhoPrev, rho), rho, 0.05);
            rhoPrev = rho;
            out = ReactorThermal.extract(temp, stirlings, 1, 250);
            temp = ReactorThermal.step(temp, p, out[0], 0.05);
            if (i > 200) {
                maxT = Math.max(maxT, temp);
            }
        }
        assertTrue(maxT < set + 80, "перелёт " + (maxT - set));
        assertTrue(maxT < ReactorThermal.DAMAGE_K);
        assertEquals(600, out[1], 40);                 // половина двигателей — половина мощности
        assertEquals(set, temp, 3);                    // температура задана уставкой, не нагрузкой
    }

    /**
     * Регулятор в условиях мода: шаг 0.5 с (время реактора ×10 при 20 тиках/с), пассивные потери
     * 3 Вт/K, среда 298 K. Пуск с холода сходится к уставке без колебаний: перелёт < 60 K,
     * недолёт после перелёта < 10 K (итог: ≈ 7 и ≈ 1 K), через 20 минут модели — ±3 K.
     */
    @Test
    void regulatorSettlesAtModTimeStep() {
        double alpha = 0.001, excess = PointKinetics.fuelExcess(28), worth = 5, t0 = 300, set = 1073, env = 298, dt = 0.5;
        double h = 0, p = PointKinetics.SOURCE_W, temp = t0, rhoPrev = Double.NaN, maxT = 0, minAfter = 1e9;
        boolean crossed = false;
        for (int i = 0; i < (int) (4000 / dt); i++) {
            double[] out = ReactorThermal.extract(temp, 4, 1, env);
            double dTdt = (p - out[0] - 3 * (temp - env)) / ReactorThermal.HEAT_CAPACITY_J_PER_K;
            double rho = excess - worth + PointKinetics.rodWorth(h, worth) - alpha * (temp - t0);
            double slope = (PointKinetics.rodWorth(Math.min(1, h + 1e-3), worth) - PointKinetics.rodWorth(Math.max(0, h - 1e-3), worth))
                    / (Math.min(1, h + 1e-3) - Math.max(0, h - 1e-3));
            h = Math.max(0, Math.min(1, h + ReactorRegulator.rodDelta(rho, temp, dTdt, set, slope, 0.01 * dt)));
            rho = excess - worth + PointKinetics.rodWorth(h, worth) - alpha * (temp - t0);
            if (!Double.isNaN(rhoPrev)) {
                p = PointKinetics.jump(p, rhoPrev, rho);
            }
            p = PointKinetics.evolve(p, rho, dt);
            rhoPrev = rho;
            temp = ReactorThermal.step(temp, p, out[0] + 3 * (temp - env), dt);
            maxT = Math.max(maxT, temp);
            if (temp > set) {
                crossed = true;
            }
            if (crossed && maxT > set + 5) {
                minAfter = Math.min(minAfter, temp);
            }
        }
        assertTrue(maxT < set + 60, "перелёт " + (maxT - set));
        assertTrue(minAfter > set - 10, "недолёт " + (set - minAfter));
        assertEquals(set, temp, 3);
    }

    private static double rodFor(double dollars, double worth) {
        double lo = 0, hi = 1;
        for (int k = 0; k < 60; k++) {
            double m = (lo + hi) / 2;
            if (PointKinetics.rodWorth(m, worth) < dollars) lo = m; else hi = m;
        }
        return lo;
    }
}
