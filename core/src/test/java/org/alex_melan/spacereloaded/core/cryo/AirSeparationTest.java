package org.alex_melan.spacereloaded.core.cryo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Колонна Линде (research-design §4, SC-005). */
class AirSeparationTest {

    @Test
    void fenskePurity() {
        assertTrue(AirSeparation.oxygenPurity(11) >= 0.995);
        assertEquals(0.90, AirSeparation.oxygenPurity(5), 0.03);
        assertEquals(0.99986, AirSeparation.nitrogenPurity(11), 0.00002);
        assertEquals(10, AirSeparation.traysFor(0.995), 1);
    }

    @Test
    void compressionEnergy() {
        assertEquals(0.0612, AirSeparation.KWH_PER_KG_AIR, 0.0005);
        assertEquals(0.279, AirSeparation.KWH_PER_KG_AIR / (AirSeparation.O2_MASS * AirSeparation.RECOVERY), 0.003);
        assertEquals(1e5 / (0.0612 * 3.6e6), AirSeparation.airFlow(1e5), 0.005);
    }
}
