package org.alex_melan.spacereloaded.core.materials;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Баки по материалу и предел турбины (006, SC-005). */
class MaterialsTest {

    @Test
    void tankMasses() {
        assertEquals(300, 300 * TankMaterial.TITANIUM.massMultiplier(), 1e-9);
        assertEquals(242, 300 * TankMaterial.ALUMINIUM_LITHIUM.massMultiplier(), 2);
        assertEquals(285, 300 * TankMaterial.ALUMINIUM_COPPER.massMultiplier(), 2);
        assertEquals(509, 300 * TankMaterial.STEEL.massMultiplier(), 5);
    }

    @Test
    void turbine() {
        assertEquals(1.0, TurbineLimit.thrustMultiplier(TurbineLimit.REFERENCE_INLET_K), 1e-12);
        assertEquals(1.3, TurbineLimit.thrustMultiplier(TurbineLimit.SUPERALLOY_INLET_K), 1e-12);
    }
}
