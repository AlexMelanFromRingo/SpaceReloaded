package org.alex_melan.spacereloaded.core.nuclear;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Стирлинг и радиатор (research-design §2.2). */
class ReactorThermalTest {

    @Test
    void stirlingAndRadiator() {
        assertEquals(0.345, ReactorThermal.stirlingEfficiency(1073, 400), 0.002);
        assertEquals(2 * 1107, ReactorThermal.radiatorW(1, 400, 250), 5);
    }

    @Test
    void fourStirlingsGiveAKilowatt() {
        double[] r = ReactorThermal.extract(1073, 4, 1, 250);   // 4 двигателя, 1 панель (2 м²)
        assertEquals(1050, r[1], 30);
        assertEquals(390, r[2], 5);
        assertTrue(ReactorThermal.extract(1073, 4, 4, 250)[1] > r[1]);  // больше радиатора — холоднее конец
    }
}
