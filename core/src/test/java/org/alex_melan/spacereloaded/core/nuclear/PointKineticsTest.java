package org.alex_melan.spacereloaded.core.nuclear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Кинетика и KRUSTY-подобное равновесие (research-design §2, SC-002, SC-003). */
class PointKineticsTest {

    @Test
    void periodAndPromptJump() {
        assertEquals((1 - 0.2) / (0.08 * 0.2), PointKinetics.period(0.2), 1e-9);   // 50 с при 20 ¢
        double p = PointKinetics.evolve(100, 0.2, 50);
        assertEquals(100 * Math.E, p, 0.5);
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
            h = Math.max(0, Math.min(1, h + ReactorRegulator.command(rho, temp, dTdt, set) * 0.01 * 0.05));
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

    private static double rodFor(double dollars, double worth) {
        double lo = 0, hi = 1;
        for (int k = 0; k < 60; k++) {
            double m = (lo + hi) / 2;
            if (PointKinetics.rodWorth(m, worth) < dollars) lo = m; else hi = m;
        }
        return lo;
    }
}
