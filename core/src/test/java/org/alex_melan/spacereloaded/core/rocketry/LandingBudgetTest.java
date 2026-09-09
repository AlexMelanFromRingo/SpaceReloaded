package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Бюджет пропульсивной посадки (003, FR-105, D22). */
class LandingBudgetTest {

    @Test
    void touchdownSpeedFromFreeFall() {
        // Марс: v₀ = 5 м/с, g = 3.72, h = 180 → √(25 + 1339.2)
        assertEquals(Math.sqrt(25 + 2 * 3.72 * 180), LandingBudget.touchdownSpeed(5, 3.72, 180), 1e-9);
        assertEquals(5, LandingBudget.touchdownSpeed(5, 3.72, 0), 1e-9, "без высоты — только скорость прибытия");
    }

    @Test
    void propulsiveDeltaVGrowsWithGravityLosses() {
        double v = 30;
        assertEquals(2 * v, LandingBudget.propulsiveDeltaV(v, 2), 1e-9, "TWR 2: половина тяги уходит на гравитацию");
        assertEquals(1.5 * v, LandingBudget.propulsiveDeltaV(v, 3), 1e-9, "TWR 3");
        assertEquals(v, LandingBudget.propulsiveDeltaV(v, 1e9), v * 1e-6, "мгновенный импульс при TWR → ∞");
    }

    @Test
    void cannotLandBelowUnitThrustToWeight() {
        assertTrue(Double.isInfinite(LandingBudget.propulsiveDeltaV(30, 1.0)));
        assertTrue(Double.isInfinite(LandingBudget.propulsiveDeltaV(30, 0.5)));
    }
}
