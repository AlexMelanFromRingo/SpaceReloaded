package org.alex_melan.spacereloaded.core.station;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Шлюз и кольцо (007, SC-002, SC-005). */
class StationTest {

    @Test
    void airlockTwoCubicMetres() {
        assertEquals(79, AirlockCycle.pumpSeconds(2, AirlockCycle.PUMP_SPEED, 101.325, AirlockCycle.PUMP_STOP_KPA), 1);
        assertEquals(0.33, AirlockCycle.ventLoss(2, AirlockCycle.PUMP_STOP_KPA), 0.01);
        assertEquals(2.41, AirlockCycle.airMass(101.325, 2), 0.02);
        assertEquals(101_325, AirlockCycle.hatchForce(101.325, 1), 1);
    }

    @Test
    void spinGravity() {
        assertEquals(9.81, SpinGravity.gravity(SpinGravity.omega(4), 55.9), 0.01);
        assertEquals(21.2, SpinGravity.radiusFor(3.72, 4), 0.1);
        // лунное кольцо 9.2 м при 4 об/мин: v_t 3.9 м/с, бег 1.42 м/с по вращению — вес ×1.86, против — ×0.41
        double w = SpinGravity.omega(4);
        assertEquals(1.62 * 1.86, SpinGravity.walkingGravity(w, 9.2, 1.42, true), 0.05);
        assertEquals(1.62 * 0.41, SpinGravity.walkingGravity(w, 9.2, 1.42, false), 0.05);
        // раскрутка примера: L = 7.5·10⁶ Н·м·с на r 21.2 м при Isp 300 — ~121 кг топлива
        assertEquals(120, SpinGravity.spinUpPropellant(7.5e6, 21.2, 300), 2);
    }

    @Test
    void dumbbellBalance() {
        var balanced = new RotatingAssembly(List.of(new RotatingAssembly.Mass(0, -21, 0, 20000),
                new RotatingAssembly.Mass(0, 21, 0, 20000)), RotatingAssembly.Axis.Z, 0, 0, 0);
        assertEquals(0, balanced.imbalance(), 1e-9);
        assertEquals(2 * 20000 * 21 * 21, balanced.inertia(), 1e-6);
        var lopsided = new RotatingAssembly(List.of(new RotatingAssembly.Mass(0, -21, 0, 20000),
                new RotatingAssembly.Mass(0, 21, 0, 10000)), RotatingAssembly.Axis.Z, 0, 0, 0);
        double w = SpinGravity.omega(4);
        assertEquals(10000 * 21 * w * w, lopsided.bearingForce(w), 1e-6);
    }
}
