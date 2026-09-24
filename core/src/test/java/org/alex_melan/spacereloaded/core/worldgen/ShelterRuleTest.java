package org.alex_melan.spacereloaded.core.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShelterRuleTest {

    @Test
    void needsRoofAndThickness() {
        assertTrue(ShelterRule.isSheltered(false, 4, 4));
        assertFalse(ShelterRule.isSheltered(false, 3, 4));
        assertFalse(ShelterRule.isSheltered(true, 40, 4));
    }
}
