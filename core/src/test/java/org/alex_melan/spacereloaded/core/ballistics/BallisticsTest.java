package org.alex_melan.spacereloaded.core.ballistics;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticsTest {

    private static final double DT = 0.005;

    @Test
    void freeFallMatchesAnalyticTimeAndImpactSpeed() {
        double h = 320;
        double g = 9.81;
        ProjectileSpec rod = new ProjectileSpec(1000, 0);

        BallisticIntegrator.State state = new BallisticIntegrator.State(
                new Vec3d(0, h, 0), Vec3d.ZERO);
        double t = 0;
        while (state.pos().y() > 0) {
            state = BallisticIntegrator.step(state, rod, g, DT);
            t += DT;
        }

        double expectedT = Math.sqrt(2 * h / g);
        double expectedV = g * expectedT;
        assertEquals(expectedT, t, expectedT * 0.01, "время падения (1%)");
        assertEquals(expectedV, Math.abs(state.vel().y()), expectedV * 0.01, "скорость удара (1%)");
    }

    @Test
    void horizontalLaunchFollowsParabolicRange() {
        double h = 100;
        double g = 9.81;
        double vx = 50;
        ProjectileSpec rod = new ProjectileSpec(1000, 0);

        BallisticIntegrator.State state = new BallisticIntegrator.State(
                new Vec3d(0, h, 0), new Vec3d(vx, 0, 0));
        while (state.pos().y() > 0) {
            state = BallisticIntegrator.step(state, rod, g, DT);
        }

        double expectedRange = vx * Math.sqrt(2 * h / g);
        assertEquals(expectedRange, state.pos().x(), expectedRange * 0.01, "дальность (1%)");
    }

    @Test
    void dragReducesImpactSpeed() {
        double h = 500;
        ProjectileSpec vacuumRod = new ProjectileSpec(1000, 0);
        ProjectileSpec airRod = new ProjectileSpec(1000, 0.05);

        double vVacuum = impactSpeed(vacuumRod, h);
        double vAir = impactSpeed(airRod, h);
        assertTrue(vAir < vVacuum, "сопротивление снижает скорость удара");
    }

    private static double impactSpeed(ProjectileSpec spec, double h) {
        BallisticIntegrator.State state = new BallisticIntegrator.State(
                new Vec3d(0, h, 0), Vec3d.ZERO);
        while (state.pos().y() > 0) {
            state = BallisticIntegrator.step(state, spec, 9.81, DT);
        }
        return state.vel().length();
    }

    @Test
    void etaToAltitudeMatchesSimulation() {
        double y0 = 320;
        double vy = -20;
        double g = 9.81;
        double eta = BallisticIntegrator.etaToAltitude(y0, vy, 0, g);

        // Аналитически: 320 − 20t − 4.905t² = 0
        double expected = (-(-vy) + Math.sqrt(vy * vy + 2 * g * y0)) / g;
        assertEquals(expected, eta, 1e-9);

        assertTrue(Double.isNaN(BallisticIntegrator.etaToAltitude(0, 50, 100, 9.81))
                        || BallisticIntegrator.etaToAltitude(0, 50, 100, 9.81) > 0,
                "подъём к достижимой высоте либо валиден, либо NaN для недостижимой");
        assertTrue(Double.isNaN(BallisticIntegrator.etaToAltitude(0, 1, 1000, 9.81)),
                "недостижимая высота → NaN");
    }

    @Test
    void impactEnergyAndCraterScaling() {
        // 1 т на 1 км/с → E = 5·10⁸ Дж → эталонный кратер 8 блоков
        double e = ImpactEnergy.kineticEnergyJ(1000, 1000);
        assertEquals(5.0e8, e, 1e-3);
        assertEquals(8.0, ImpactEnergy.craterRadiusBlocks(e), 1e-9);

        // Кубическое подобие: 8× энергии → 2× радиус
        assertEquals(16.0, ImpactEnergy.craterRadiusBlocks(8 * e), 1e-9);
        assertEquals(0, ImpactEnergy.craterRadiusBlocks(0), 1e-9);
    }
}
