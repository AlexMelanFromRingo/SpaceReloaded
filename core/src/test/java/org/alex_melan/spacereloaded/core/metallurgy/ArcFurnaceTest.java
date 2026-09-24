package org.alex_melan.spacereloaded.core.metallurgy;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Дуговая печь (research-design §5, SC-006). */
class ArcFurnaceTest {

    @Test
    void tonneAtMegawatt() {
        assertEquals(0.475, ArcFurnace.meltJ(1, false) / 3.6e6, 0.003);
        assertEquals(28.5, ArcFurnace.seconds(1000, 1e6, false) / 60, 0.5);
        assertTrue(ArcFurnace.meltJ(1000, true) < ArcFurnace.meltJ(1000, false));
        assertEquals(24, ArcFurnace.oxygenKg(1000), 0.5);
        assertEquals(1.8, ArcFurnace.electrodeKg(1000), 1e-9);
    }
}
