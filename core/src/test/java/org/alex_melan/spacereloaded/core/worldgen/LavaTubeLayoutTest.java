package org.alex_melan.spacereloaded.core.worldgen;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Лавовые трубки (004, FR-240): детерминизм, размеры, крыша, окно, сшивка по чанкам. */
class LavaTubeLayoutTest {

    private static final double SURFACE = 100; // плоская поверхность для геометрических проверок

    private static LavaTubeLayout anyTube(long seed) {
        for (int r = 0; r < 64; r++) {
            Optional<LavaTubeLayout> tube = LavaTubeLayout.forRegion(seed, r, -r, 0.85);
            if (tube.isPresent()) {
                return tube.get();
            }
        }
        throw new AssertionError("нет трубки в 64 регионах");
    }

    @Test
    void deterministic() {
        assertEquals(LavaTubeLayout.forRegion(99, 3, 4, 0.85), LavaTubeLayout.forRegion(99, 3, 4, 0.85));
    }

    @Test
    void chanceControlsFrequency() {
        int present = 0;
        for (int r = 0; r < 400; r++) {
            if (LavaTubeLayout.forRegion(5, r, r * 7, 0.85).isPresent()) {
                present++;
            }
        }
        assertEquals(0.85, present / 400.0, 0.06);
    }

    @Test
    void crossSectionWithinSpec() {
        for (long seed = 1; seed <= 40; seed++) {
            LavaTubeLayout tube = anyTube(seed);
            double width = 2 * tube.halfWidth();
            double height = tube.halfHeight() * (1 + 0.7);
            assertTrue(width >= 7 && width <= 13, "ширина " + width);
            assertTrue(height >= 4.25 && height <= 9, "высота " + height);
        }
    }

    @Test
    void roofAtLeastFourBlocksOutsideSkylights() {
        for (long seed = 1; seed <= 20; seed++) {
            LavaTubeLayout tube = anyTube(seed);
            double[] b = tube.bounds();
            for (int x = (int) b[0]; x <= b[2]; x++) {
                for (int z = (int) b[1]; z <= b[3]; z++) {
                    for (int y = (int) SURFACE - 3; y <= SURFACE; y++) {
                        assertTrue(!tube.contains(x, y, z, SURFACE), "полость в " + (SURFACE - y) + " от поверхности");
                    }
                }
            }
        }
    }

    @Test
    void skylightOpensOntoTube() {
        LavaTubeLayout tube = anyTube(11);
        LavaTubeLayout.Skylight light = tube.skylights().get(0);
        boolean cavityBelow = false;
        for (int y = (int) SURFACE; y > SURFACE - 40; y--) {
            if (tube.contains(light.x(), y, light.z(), SURFACE)) {
                cavityBelow = true;
            }
        }
        assertTrue(tube.isSkylight(light.x(), light.z()));
        assertTrue(cavityBelow, "под окном нет полости");
    }

    @Test
    void chunksStitchIntoWholeTube() {
        long seed = 1234;
        int rx = 2, rz = 3;
        LavaTubeLayout tube = LavaTubeLayout.forRegion(seed, rx, rz, 1.0).orElseThrow();
        double[] b = tube.bounds();
        Set<Long> whole = new HashSet<>();
        for (int x = (int) Math.floor(b[0]); x <= b[2]; x++) {
            for (int z = (int) Math.floor(b[1]); z <= b[3]; z++) {
                for (int y = 60; y <= 100; y++) {
                    if (tube.contains(x, y, z, SURFACE)) {
                        whole.add(key(x, y, z));
                    }
                }
            }
        }
        // Чанк за чанком: только регионы, трубка которых касается чанка, только клетки чанка
        Set<Long> stitched = new HashSet<>();
        int cx0 = Math.floorDiv((int) Math.floor(b[0]), 16), cx1 = Math.floorDiv((int) b[2], 16);
        int cz0 = Math.floorDiv((int) Math.floor(b[1]), 16), cz1 = Math.floorDiv((int) b[3], 16);
        for (int cx = cx0; cx <= cx1; cx++) {
            for (int cz = cz0; cz <= cz1; cz++) {
                if (!tube.touchesChunk(cx * 16, cz * 16)) {
                    continue;
                }
                for (int x = cx * 16; x < cx * 16 + 16; x++) {
                    for (int z = cz * 16; z < cz * 16 + 16; z++) {
                        for (int y = 60; y <= 100; y++) {
                            if (tube.contains(x, y, z, SURFACE)) {
                                stitched.add(key(x, y, z));
                            }
                        }
                    }
                }
            }
        }
        assertTrue(whole.size() > 500, "трубка слишком мала: " + whole.size());
        assertEquals(whole, stitched);
    }

    @Test
    void reachCoversRegionRadius() {
        assertTrue(LavaTubeLayout.regionRadius() * LavaTubeLayout.REGION >= LavaTubeLayout.MAX_REACH);
    }

    private static long key(int x, int y, int z) {
        return ((long) x & 0x1FFFFF) | (((long) z & 0x1FFFFF) << 21) | (((long) y & 0x3FFFFF) << 42);
    }
}
