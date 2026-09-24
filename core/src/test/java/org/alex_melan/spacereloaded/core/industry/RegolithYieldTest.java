package org.alex_melan.spacereloaded.core.industry;

import org.junit.jupiter.api.Test;

import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Реголитовый реактор (004, SC-005): средние выхода на 10 000 циклов. */
class RegolithYieldTest {

    @Test
    void meansMatch() {
        RegolithYield yield = new RegolithYield(3.0, 1, 0.2, 1);
        SplittableRandom random = new SplittableRandom(7);
        double o2 = 0;
        long fe = 0, ti = 0, slag = 0;
        int n = 10_000;
        for (int i = 0; i < n; i++) {
            RegolithYield.Output out = yield.roll(random);
            o2 += out.oxygen();
            fe += out.ironDust();
            ti += out.titaniumDust();
            slag += out.slag();
        }
        assertEquals(3.0, o2 / n, 1e-9);
        assertEquals(1.0, fe / (double) n, 1e-9);
        assertEquals(1.0, slag / (double) n, 1e-9);
        assertEquals(0.2, ti / (double) n, 0.02);
    }

    @Test
    void rejectsBadChance() {
        assertThrows(IllegalArgumentException.class, () -> new RegolithYield(1, 1, 1.5, 1));
    }
}
