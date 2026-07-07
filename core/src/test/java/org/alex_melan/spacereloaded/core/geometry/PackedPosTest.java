package org.alex_melan.spacereloaded.core.geometry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PackedPosTest {

    @Test
    void roundTripIncludingNegativesAndExtremes() {
        int[][] samples = {
                {0, 0, 0},
                {1, 2, 3},
                {-1, -2, -3},
                {30_000_000, 2047, -30_000_000},
                {-30_000_000, -2048, 30_000_000},
                {12345, -60, -54321}
        };
        for (int[] s : samples) {
            long packed = PackedPos.pack(s[0], s[1], s[2]);
            assertEquals(s[0], PackedPos.unpackX(packed), "x for " + java.util.Arrays.toString(s));
            assertEquals(s[1], PackedPos.unpackY(packed), "y for " + java.util.Arrays.toString(s));
            assertEquals(s[2], PackedPos.unpackZ(packed), "z for " + java.util.Arrays.toString(s));
        }
    }

    @Test
    void offsetMovesEachAxis() {
        long p = PackedPos.pack(10, -20, 30);
        long moved = PackedPos.offset(p, -1, 2, -3);
        assertEquals(9, PackedPos.unpackX(moved));
        assertEquals(-18, PackedPos.unpackY(moved));
        assertEquals(27, PackedPos.unpackZ(moved));
    }

    @Test
    void chebyshevDistanceIsMaxOfAxes() {
        long a = PackedPos.pack(0, 0, 0);
        long b = PackedPos.pack(3, -7, 5);
        assertEquals(7, PackedPos.chebyshevDistance(a, b));
        assertEquals(0, PackedPos.chebyshevDistance(a, a));
    }
}
