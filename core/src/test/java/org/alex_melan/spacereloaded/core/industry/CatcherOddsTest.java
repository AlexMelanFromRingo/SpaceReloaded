package org.alex_melan.spacereloaded.core.industry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Ловушка масс (004, SC-003): эмпирическая доля приёма совпадает с распределением Рэлея. */
class CatcherOddsTest {

    @Test
    void radiusGrowsWithSqrtAndClamps() {
        assertEquals(1.5, CatcherOdds.captureRadius(0, 1.5, 0.6, 12), 1e-12);
        assertEquals(1.5 + 0.6 * 5, CatcherOdds.captureRadius(25, 1.5, 0.6, 12), 1e-12);
        assertEquals(12, CatcherOdds.captureRadius(100_000, 1.5, 0.6, 12), 1e-12);
    }

    @Test
    void empiricalMatchesRayleigh() {
        double sigma = 10;
        for (double r : new double[] {1.5, 4.5, 8.1, 14}) {
            int hits = 0;
            int n = 10_000;
            for (int i = 0; i < n; i++) {
                double[] d = CatcherOdds.sampleOffset(1_000_003L * i + 17, sigma);
                if (Math.hypot(d[0], d[1]) <= r) {
                    hits++;
                }
            }
            assertEquals(CatcherOdds.captureProbability(r, sigma), hits / (double) n, 0.02, "r = " + r);
        }
    }

    @Test
    void coverageMakesCatchCertain() {
        // σ 0.5 при покрытии: даже ловушка без сетки (r 1.5) ловит ≥ 98.9 % (1 − e^−4.5)
        assertTrue(CatcherOdds.captureProbability(1.5, 0.5) >= 0.988);
        // без покрытия и без сетки — < 5 %; сетка 21×21 (441, r = 12) — > 50 %
        assertTrue(CatcherOdds.captureProbability(1.5, 10) < 0.05);
        assertTrue(CatcherOdds.captureProbability(CatcherOdds.captureRadius(441, 1.5, 0.6, 12), 10) > 0.5);
    }

    @Test
    void seedIsDeterministic() {
        assertArrayEquals(CatcherOdds.sampleOffset(42, 3), CatcherOdds.sampleOffset(42, 3));
    }
}
