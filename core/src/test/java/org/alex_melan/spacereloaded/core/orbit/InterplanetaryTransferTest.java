package org.alex_melan.spacereloaded.core.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Цена перелёта по Ламберту против эталонов research-design §3: день InSight, минимумы окон
 * 2018–2044 к Марсу и обратно, дороговизна между окнами, Церера с наклоном.
 */
class InterplanetaryTransferTest {

    static final InterplanetaryTransfer.Body EARTH = new InterplanetaryTransfer.Body(Ephemeris.EARTH, 3.986004418e14, 6.571e6, true);
    static final InterplanetaryTransfer.Body MARS = new InterplanetaryTransfer.Body(Ephemeris.MARS, 4.282837e13, 3.5895e6, true);
    static final InterplanetaryTransfer.Body CERES = new InterplanetaryTransfer.Body(Ephemeris.CERES, 6.26e10, 4.8e5, false);

    /** InSight: 5.05.2018, C3 8.2 км²/с² → 3.59 км/с с 200 км. */
    @Test
    void insightDay() {
        double dep = 6698.5;
        var fixed = InterplanetaryTransfer.cost(EARTH, MARS, dep, 205);
        assertEquals(3590, fixed.totalMs(), 0.02 * 3590);
        assertEquals(0, fixed.arrivalMs(), 1e-9);   // аэрозахват
    }

    /** Минимумы окон 2018–2044 лежат в 3.55…3.95 км/с; окно 2020 г. — в июле. */
    @Test
    void marsWindows() {
        for (double t = 6647; t < 6647 + 26 * 365; t += 780) {
            double day = InterplanetaryTransfer.nextMinimumDay(EARTH, MARS, t);
            double dv = InterplanetaryTransfer.best(EARTH, MARS, day).totalMs();
            assertTrue(dv > 3550 && dv < 3950, "окно " + day + ": " + dv);
        }
        double w2020 = InterplanetaryTransfer.nextMinimumDay(EARTH, MARS, 7300);
        assertEquals(7504, w2020, 20);   // 18.07.2020
    }

    /** Между окнами (половина синодического периода от минимума) — не меньше 1.5 × минимума. */
    @Test
    void expensiveBetweenWindows() {
        double day = InterplanetaryTransfer.nextMinimumDay(EARTH, MARS, 6647);
        double min = InterplanetaryTransfer.best(EARTH, MARS, day).totalMs();
        double mid = InterplanetaryTransfer.best(EARTH, MARS, day + InterplanetaryTransfer.synodicDays(EARTH, MARS) / 2).totalMs();
        assertTrue(mid > 1.5 * min, "между окнами " + mid + " при минимуме " + min);
    }

    @Test
    void returnsAndBelt() {
        for (double t = 6647; t < 6647 + 12 * 365; t += 780) {
            double back = InterplanetaryTransfer.best(MARS, EARTH, InterplanetaryTransfer.nextMinimumDay(MARS, EARTH, t)).totalMs();
            assertTrue(back > 1900 && back < 2700, "Марс → Земля " + back);
            double ceresBack = InterplanetaryTransfer.best(CERES, EARTH, InterplanetaryTransfer.nextMinimumDay(CERES, EARTH, t)).totalMs();
            assertTrue(ceresBack > 4300 && ceresBack < 5800, "Церера → Земля " + ceresBack);
            double toCeres = InterplanetaryTransfer.best(EARTH, CERES, InterplanetaryTransfer.nextMinimumDay(EARTH, CERES, t)).totalMs();
            assertTrue(toCeres > 9_500 && toCeres < 14_800, "Земля → Церера " + toCeres);
        }
    }
}
