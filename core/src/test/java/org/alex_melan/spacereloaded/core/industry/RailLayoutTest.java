package org.alex_melan.spacereloaded.core.industry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RailLayoutTest {

    @Test
    void stopsAtFirstGap() {
        RailLayout rail = RailLayout.fromCells(new int[] {1, 1, 2, 0, 2, 2}, 400);
        assertEquals(3, rail.length());
        assertArrayEquals(new int[] {1, 1, 2}, rail.tiers());
        assertFalse(rail.truncatedByLimit());
    }

    @Test
    void limitTruncates() {
        int[] cells = new int[20];
        java.util.Arrays.fill(cells, 1);
        RailLayout rail = RailLayout.fromCells(cells, 8);
        assertEquals(8, rail.length());
        assertTrue(rail.truncatedByLimit());
    }

    @Test
    void emptyWhenNoCoilInFront() {
        assertEquals(0, RailLayout.fromCells(new int[] {0, 1, 1}, 400).length());
    }

    @Test
    void accelerationsAndCounts() {
        RailLayout rail = RailLayout.fromCells(new int[] {1, 2, 2}, 400);
        double[] a = rail.accelerations(1000, 3000);
        assertEquals(1000 * MassDriverBallistics.G0, a[0], 1e-9);
        assertEquals(3000 * MassDriverBallistics.G0, a[2], 1e-9);
        assertEquals(1, rail.count(1));
        assertEquals(2, rail.count(2));
        assertEquals(2, rail.bestTier());
    }
}
