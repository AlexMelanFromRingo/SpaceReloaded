package org.alex_melan.spacereloaded.core.vehicle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Ровер на Луне: LRV-кейс (007, SC-006). */
class TerramechanicsTest {

    private static final double WHEEL_LOAD = 784 * 1.62 / 4; // Н

    @Test
    void lrvCase() {
        var soil = Terramechanics.Soil.LUNAR;
        var wheel = Terramechanics.Wheel.LRV;
        assertEquals(2.0, Terramechanics.sinkageCm(soil, wheel, WHEEL_LOAD), 0.1);
        double rc = Terramechanics.compactionN(soil, wheel, WHEEL_LOAD);
        assertEquals(37, rc, 1.5);
        double v = Terramechanics.maxSpeed(4 * 190, 0.7, 4 * rc);
        assertEquals(3.6, v, 0.1);
        assertEquals(13, v * 3.6, 0.5);
        assertEquals(59, Terramechanics.whPerKm(4 * rc, 0.7), 2);
    }

    @Test
    void slopeBySlip() {
        var soil = Terramechanics.Soil.LUNAR;
        var wheel = Terramechanics.Wheel.LRV;
        assertEquals(13, Terramechanics.maxSlopeDeg(soil, wheel, WHEEL_LOAD, 0.2), 2);
        assertEquals(22, Terramechanics.maxSlopeDeg(soil, wheel, WHEEL_LOAD, 0.5), 2);
    }
}
