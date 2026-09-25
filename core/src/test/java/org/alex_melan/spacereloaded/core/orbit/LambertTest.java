package org.alex_melan.spacereloaded.core.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** Задача Ламберта: эталон Curtis (пример 5.2) и совпадение с Гоманом между круговыми орбитами. */
class LambertTest {

    private static final double MU_EARTH = 3.986004418e14;

    /** Curtis, пример 5.2: r₁, r₂ за 1 ч → v₁ = (−5.9925, 1.9254, 3.2456), v₂ = (−3.3125, −4.1966, −0.38529) км/с. */
    @Test
    void curtisExample() {
        var s = Lambert.solve(new double[] {5000e3, 10000e3, 2100e3}, new double[] {-14600e3, 2500e3, 7000e3}, 3600, 398600e9);
        assertNotNull(s);
        assertEquals(-5992.5, s.v1()[0], 2);
        assertEquals(1925.4, s.v1()[1], 2);
        assertEquals(3245.6, s.v1()[2], 2);
        assertEquals(-3312.5, s.v2()[0], 2);
        assertEquals(-4196.6, s.v2()[1], 2);
        assertEquals(-385.29, s.v2()[2], 2);
    }

    /** Полуоборот почти 180° между круговыми орбитами за полупериод Гомана даёт скорости Гомана. */
    @Test
    void matchesHohmannNearHalfTurn() {
        double r1 = 7000e3, r2 = 14000e3, a = (r1 + r2) / 2;
        double th = Math.PI * Math.sqrt(a * a * a / MU_EARTH);
        double ang = Math.toRadians(179.9);
        var s = Lambert.solve(new double[] {r1, 0, 0}, new double[] {r2 * Math.cos(ang), r2 * Math.sin(ang), 0}, th, MU_EARTH);
        assertNotNull(s);
        double vPeri = Math.sqrt(MU_EARTH * (2 / r1 - 1 / a));
        assertEquals(vPeri, Lambert.norm(s.v1()), 0.005 * vPeri);
    }
}
