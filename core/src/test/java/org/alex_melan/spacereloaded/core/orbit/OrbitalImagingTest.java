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
}
