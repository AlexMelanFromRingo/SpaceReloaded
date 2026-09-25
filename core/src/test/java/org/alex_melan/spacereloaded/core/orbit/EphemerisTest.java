package org.alex_melan.spacereloaded.core.orbit;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Эфемериды по элементам Standish: положение Земли на J2000, периоды, сохранение энергии орбиты. */
class EphemerisTest {

    /** 1 января 2000 г. Земля (барицентр) — x ≈ −0.177, y ≈ 0.967 а.е. (JPL Horizons). */
    @Test
    void earthAtJ2000() {
        var s = Ephemeris.state(Ephemeris.EARTH, 0);
        assertEquals(-0.177, s.r()[0] / Ephemeris.AU, 0.005);
        assertEquals(0.967, s.r()[1] / Ephemeris.AU, 0.005);
        assertEquals(0, s.r()[2] / Ephemeris.AU, 1e-4);
    }

    @Test
    void periods() {
        assertEquals(365.25, Ephemeris.EARTH.periodDays(), 0.1);
        assertEquals(686.98, Ephemeris.MARS.periodDays(), 0.5);
    }

    /** Vis-viva: v² = μ(2/r − 1/a) в любой точке орбиты. */
    @Test
    void visVivaHolds() {
        for (double d = 0; d < 700; d += 97) {
            var s = Ephemeris.state(Ephemeris.MARS, d);
            double r = Lambert.norm(s.r()), v = Lambert.norm(s.v());
            double a = Ephemeris.MARS.aAu() * Ephemeris.AU;
            assertEquals(Ephemeris.MU_SUN * (2 / r - 1 / a), v * v, 1e-6 * v * v);
        }
    }

    @Test
    void calendar() {
        assertEquals(6647, GameCalendar.day(0), 1e-9);
        assertEquals(6647 + 130, GameCalendar.day(24000), 1e-9);
        assertEquals(12000, GameCalendar.tick(6647 + 65, 6647));
    }
}
