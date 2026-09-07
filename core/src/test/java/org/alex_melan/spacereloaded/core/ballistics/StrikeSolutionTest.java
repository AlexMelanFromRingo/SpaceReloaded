package org.alex_melan.spacereloaded.core.ballistics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrikeSolutionTest {

    private static final double GUIDED = 0.5;
    private static final double UNGUIDED = 10.0;

    /** Наводимый лом ложится в метку: любое смещение ≤ радиуса наведения (≤ 1 блока). */
    @Test
    void guidedOffsetStaysWithinGuidedRadius() {
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 8; j++) {
                StrikeSolution solution = StrikeSolution.solve(i / 49.0, j / 8.0, true, GUIDED, UNGUIDED);
                assertTrue(solution.guided());
                assertEquals(GUIDED, solution.spreadRadius(), 0);
                assertTrue(solution.offsetDistance() <= GUIDED + 1e-9, "смещение " + solution.offsetDistance());
                assertTrue(solution.offsetDistance() <= 1.0, "наводимый — не дальше блока");
            }
        }
    }

    /** Без покрытия: смещение в круге радиуса рассеивания, а не за ним. */
    @Test
    void unguidedOffsetStaysWithinSpreadRadius() {
        for (int i = 0; i < 50; i++) {
            for (int j = 0; j < 8; j++) {
                StrikeSolution solution = StrikeSolution.solve(i / 49.0, j / 8.0, false, GUIDED, UNGUIDED);
                assertTrue(!solution.guided());
                assertEquals(UNGUIDED, solution.spreadRadius(), 0);
                assertTrue(solution.offsetDistance() <= UNGUIDED + 1e-9);
            }
        }
        // Край круга достижим (u₁ = 1 → r = R)
        assertEquals(UNGUIDED, StrikeSolution.solve(1.0, 0.25, false, GUIDED, UNGUIDED).offsetDistance(), 1e-9);
    }

    /** Серия из 20 ненаводимых выстрелов: среднее смещение больше блока (SC-006). */
    @Test
    void unguidedSeriesMissesOnAverage() {
        double sum = 0;
        for (int i = 0; i < 20; i++) {
            StrikeSolution solution = StrikeSolution.solve((i + 0.5) / 20.0, i / 20.0, false, GUIDED, UNGUIDED);
            sum += solution.offsetDistance();
        }
        double mean = sum / 20;
        assertTrue(mean > 1.0, "среднее смещение " + mean);
        // Равномерно по площади круга: E[r] = 2R/3
        assertEquals(2 * UNGUIDED / 3, mean, UNGUIDED * 0.05);
    }

    /** Равномерность по площади: половина попаданий — внутри r = R/√2 (половина площади). */
    @Test
    void offsetIsUniformOverArea() {
        int inside = 0;
        int total = 1000;
        for (int i = 0; i < total; i++) {
            StrikeSolution solution = StrikeSolution.solve((i + 0.5) / total, (i * 7 % total) / (double) total,
                    false, GUIDED, UNGUIDED);
            if (solution.offsetDistance() < UNGUIDED / Math.sqrt(2)) {
                inside++;
            }
        }
        assertEquals(0.5, inside / (double) total, 0.01);
    }

    @Test
    void angleCoversFullCircle() {
        StrikeSolution east = StrikeSolution.solve(1.0, 0.0, false, GUIDED, UNGUIDED);
        StrikeSolution south = StrikeSolution.solve(1.0, 0.25, false, GUIDED, UNGUIDED);
        StrikeSolution west = StrikeSolution.solve(1.0, 0.5, false, GUIDED, UNGUIDED);
        assertEquals(UNGUIDED, east.offsetX(), 1e-9);
        assertEquals(0, east.offsetZ(), 1e-9);
        assertEquals(UNGUIDED, south.offsetZ(), 1e-9);
        assertEquals(-UNGUIDED, west.offsetX(), 1e-9);
    }
}
