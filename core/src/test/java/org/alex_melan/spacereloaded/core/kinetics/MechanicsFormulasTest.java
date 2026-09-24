package org.alex_melan.spacereloaded.core.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Прочность вала, маховик, ветроколесо (005) против справочных чисел research-design. */
class MechanicsFormulasTest {

    @Test
    void shaftLimits() {
        assertEquals(706_000, ShaftStrength.maxTorque(0.25, 230e6), 706_000 * 0.01);
        assertEquals(24_500, ShaftStrength.maxTorque(0.25, 8e6), 24_500 * 0.01);
    }

    @Test
    void steelFlywheel() {
        double mass = Math.PI * 0.25 * 0.5 * 7850; // диск r 0.5 м, толщина 0.5 м
        double inertia = Flywheel.solidDiskInertia(mass, 0.5);
        assertEquals(385, inertia, 385 * 0.01);
        double omegaMax = Flywheel.maxOmega(400e6, 7850, 0.5, 0.3);
        assertEquals(703, omegaMax, 703 * 0.01);
        assertEquals(95e6, Flywheel.energy(385, 703), 95e6 * 0.01);
    }

    @Test
    void windRotorNumbers() {
        double peak = 6 * 8 / 10.0; // ω при λ = 6, R = 10, v = 8
        assertEquals(39_400, WindRotor.power(1.225, 10, 8, peak), 39_400 * 0.01);
        assertEquals(643, WindRotor.power(0.02, 10, 8, peak), 643 * 0.02);
        assertEquals(19_600, WindRotor.power(0.02, 10, 25, 6 * 25 / 10.0), 19_600 * 0.01);
        assertEquals(0.0, WindRotor.power(0.0, 10, 8, peak));
        assertTrue(WindRotor.PEAK_CP < WindRotor.BETZ_LIMIT);
        assertTrue(WindRotor.torque(1.225, 10, 8, 0) > 0, "колесо трогается с места");
    }
}
