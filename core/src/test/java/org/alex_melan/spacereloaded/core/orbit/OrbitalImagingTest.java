package org.alex_melan.spacereloaded.core.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Снимки Луны (007, SC-008). */
class OrbitalImagingTest {

    private static final double MOON_R = 1_737_400;
    private static final double MOON_MU = 4.9048695e12;

    @Test
    void moonFrom100km() {
        assertEquals(0.67, OrbitalImaging.gsd(OrbitalImaging.VISIBLE_M, 100_000, 0.10), 0.01);
        double t = OrbitalImaging.periodSeconds(MOON_R, 100_000, MOON_MU);
        assertEquals(118, t / 60, 1);
        assertEquals(89, OrbitalImaging.coverageDays(MOON_R, OrbitalImaging.LINE_PIXELS * 1.0, t), 1);
        assertEquals(5.6, OrbitalImaging.coverageDays(MOON_R, OrbitalImaging.LINE_PIXELS * 16.0, t), 0.1);
    }

    @Test
    void scaleIsBoundedByDiffraction() {
        assertEquals(0, OrbitalImaging.minScale(0.67));                 // Луна, 10 см: поблочно
        assertEquals(1, OrbitalImaging.minScale(OrbitalImaging.gsd(OrbitalImaging.VISIBLE_M, 200_000, 0.10))); // Земля 1.34 м
        assertEquals(-1, OrbitalImaging.minScale(20));
    }

    @Test
    void waitMatchesDesignTable() {
        // масштаб 4 над Луной: T_cov 5.6 сут / 130 = 0.043 игровых суток ≈ 1 минута
        long worst = OrbitalImaging.waitTicks(MOON_R, 100_000, MOON_MU, 4, 1, 1.0);
        assertEquals(1034, worst, 20);
        // масштаб 0: ≤ 0.7 игровых суток; два спутника делят ожидание пополам
        assertEquals(0.69, OrbitalImaging.waitTicks(MOON_R, 100_000, MOON_MU, 0, 1, 1.0) / 24000.0, 0.02);
        assertEquals(worst / 2.0, OrbitalImaging.waitTicks(MOON_R, 100_000, MOON_MU, 4, 2, 1.0), 1);
    }
}
