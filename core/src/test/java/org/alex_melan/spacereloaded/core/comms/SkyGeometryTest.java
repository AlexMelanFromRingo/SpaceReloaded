package org.alex_melan.spacereloaded.core.comms;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Геометрия цели антенны: Марс в противостоянии и соединении. */
class SkyGeometryTest {

    @Test
    void marsDistanceAndElongation() {
        assertEquals(0.524, SkyGeometry.distanceM(1.524, 0) / SkyGeometry.AU, 0.001);
        assertEquals(2.524, SkyGeometry.distanceM(1.524, Math.PI) / SkyGeometry.AU, 0.001);
        assertEquals(Math.PI, SkyGeometry.elongation(1.524, 0), 1e-9);          // противостояние: напротив Солнца
        assertEquals(0, SkyGeometry.elongation(1.524, Math.PI), 1e-9);         // соединение: за Солнцем
    }

    @Test
    void skyArc() {
        assertTrue(SkyGeometry.aboveHorizon(SkyGeometry.skyAngle(6000, 0)));      // Солнце в полдень
        assertFalse(SkyGeometry.aboveHorizon(SkyGeometry.skyAngle(6000, Math.PI))); // Марс в противостоянии днём — под горизонтом
        assertTrue(SkyGeometry.aboveHorizon(SkyGeometry.skyAngle(18000, Math.PI))); // ...и в полночь в зените
    }
}
