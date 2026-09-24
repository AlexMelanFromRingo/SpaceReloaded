package org.alex_melan.spacereloaded.core.thermal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Цвет раскалённого металла (research-design §7). */
class BlackbodyTest {

    @Test
    void redToWhite() {
        int red = Blackbody.rgb(1000);
        assertEquals(255, red >> 16);
        assertTrue(((red >> 8) & 255) < 90 && (red & 255) == 0);  // тёмно-оранжево-красный
        int white = Blackbody.rgb(6500);
        assertTrue(((white >> 8) & 255) > 240 && (white & 255) > 230);
        assertEquals(0, Blackbody.glow(700), 1e-12);
        assertTrue(Blackbody.glow(1100) > 0.3 && Blackbody.glow(1100) < 1);
    }
}
