package org.alex_melan.spacereloaded.core.atmosphere;

import org.alex_melan.spacereloaded.core.rocketry.FlightEnvironment;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AtmosphereProfileTest {

    private static final AtmosphereProfile EARTH = new AtmosphereProfile(1.225, 110.0, 63.0);

    @Test
    void vacuumHasNoDensityAnywhere() {
        assertTrue(AtmosphereProfile.VACUUM.isVacuum());
        assertEquals(0, AtmosphereProfile.VACUUM.density(-64), 0);
        assertEquals(0, AtmosphereProfile.VACUUM.density(63), 0);
        assertEquals(0, AtmosphereProfile.VACUUM.density(10_000), 0);
    }

    /** ρ(datum) = ρ₀, ρ(datum + H) = ρ₀/e — экспоненциальный профиль. */
    @Test
    void exponentialProfileMatchesScaleHeight() {
        assertFalse(EARTH.isVacuum());
        assertEquals(1.225, EARTH.density(63.0), 1e-12);
        assertEquals(1.225 / Math.E, EARTH.density(63.0 + 110.0), 1e-9);
        assertEquals(1.225 * Math.exp(-2), EARTH.density(63.0 + 220.0), 1e-9);
        // У высоты перехода Земли (450 м) остаются проценты приземной плотности
        assertTrue(EARTH.density(450) < 0.05 * 1.225);
    }

    /** Ниже уровня отсчёта плотность не растёт (шахты — не батискаф). */
    @Test
    void densityDoesNotGrowBelowDatum() {
        assertEquals(1.225, EARTH.density(0), 1e-12);
        assertEquals(1.225, EARTH.density(-60), 1e-12);
    }

    @Test
    void rejectsPhysicallyInvalidParameters() {
        assertThrows(IllegalArgumentException.class, () -> new AtmosphereProfile(-1, 100, 0));
        assertThrows(IllegalArgumentException.class, () -> new AtmosphereProfile(1, 0, 0));
    }

    /** Старый конструктор среды — вакуум: поведение 001 не меняется. */
    @Test
    void legacyEnvironmentConstructorIsVacuum() {
        FlightEnvironment legacy = new FlightEnvironment(9.81);
        assertEquals(new FlightEnvironment(9.81, AtmosphereProfile.VACUUM), legacy);
        assertTrue(legacy.atmosphere().isVacuum());
        assertEquals(0, legacy.density(100), 0);
        assertEquals(1.225, new FlightEnvironment(9.81, EARTH).density(63), 1e-12);
        assertTrue(FlightEnvironment.EARTH.atmosphere().isVacuum(), "EARTH в тестах 001 — без атмосферы");
    }
}
