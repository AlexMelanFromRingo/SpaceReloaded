package org.alex_melan.spacereloaded.core.ballistics;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BallisticsTest {

    private static final double DT = 0.005;
    private static final AtmosphereProfile EARTH = new AtmosphereProfile(1.225, 110, 63);
    private static final AtmosphereProfile MARS = new AtmosphereProfile(0.020, 150, 64);
    /** Вольфрамовый лом: 2 т, C_d 0.1, Ø ≈ 0.2 м. */
    private static final ProjectileSpec ROD = new ProjectileSpec(2000, 0.1, 0.03);

    @Test
    void freeFallMatchesAnalyticTimeAndImpactSpeed() {
        double h = 320;
        double g = 9.81;
        ProjectileSpec rod = ProjectileSpec.ballistic(1000);

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
        ProjectileSpec rod = ProjectileSpec.ballistic(1000);

        BallisticIntegrator.State state = new BallisticIntegrator.State(
                new Vec3d(0, h, 0), new Vec3d(vx, 0, 0));
        while (state.pos().y() > 0) {
            state = BallisticIntegrator.step(state, rod, g, DT);
        }

        double expectedRange = vx * Math.sqrt(2 * h / g);
        assertEquals(expectedRange, state.pos().x(), expectedRange * 0.01, "дальность (1%)");
    }

    /** Квадратичное сопротивление: в плотной атмосфере скорость удара ниже; в вакууме — как без него. */
    @Test
    void quadraticDragReducesImpactSpeedOnlyInAtmosphere() {
        double h = 500;
        ProjectileSpec blunt = new ProjectileSpec(1000, 1.0, 1.0);

        double vVacuum = impactSpeed(blunt, h, AtmosphereProfile.VACUUM);
        double vBallistic = impactSpeed(ProjectileSpec.ballistic(1000), h, EARTH);
        double vAir = impactSpeed(blunt, h, new AtmosphereProfile(1.225, 1.0e9, 0));
        assertEquals(vVacuum, vBallistic, 1e-9, "без C_d атмосфера не тормозит; в вакууме C_d не важен");
        assertTrue(vAir < vVacuum, "сопротивление снижает скорость удара");
        // Терминальная скорость √(2mg/(ρC_dA)) = 126.5 м/с — не превышается
        assertTrue(vAir <= Math.sqrt(2 * 1000 * 9.81 / (1.225 * 1.0 * 1.0)) * 1.001);
    }

    private static double impactSpeed(ProjectileSpec spec, double h, AtmosphereProfile atmosphere) {
        BallisticIntegrator.State state = new BallisticIntegrator.State(
                new Vec3d(0, h, 0), Vec3d.ZERO);
        while (state.pos().y() > 0) {
            state = BallisticIntegrator.step(state, spec, 9.81, atmosphere.density(state.pos().y()), DT);
        }
        return state.vel().length();
    }

    /** Прогноз удара лома: на Марсе быстрее, чем на Земле; обтекаемый лом теряет проценты. */
    @Test
    void impactForecastReflectsTargetAtmosphere() {
        BallisticIntegrator.Forecast earth = BallisticIntegrator.impactForecast(ROD, 2400, 1500, 9.81, EARTH, 64);
        BallisticIntegrator.Forecast mars = BallisticIntegrator.impactForecast(ROD, 2400, 1500, 3.72, MARS, 64);
        BallisticIntegrator.Forecast vacuum = BallisticIntegrator.impactForecast(ROD, 2400, 1500, 9.81,
                AtmosphereProfile.VACUUM, 64);

        assertTrue(earth.impactSpeedMs() < vacuum.impactSpeedMs(), "Земля тормозит лом");
        assertTrue(earth.impactSpeedMs() > 1400, "…но лишь на проценты: " + earth.impactSpeedMs());
        assertTrue(mars.impactSpeedMs() > earth.impactSpeedMs() - 20, "тонкая атмосфера Марса тормозит меньше");
        assertEquals(ImpactEnergy.kineticEnergyJ(2000, earth.impactSpeedMs()), earth.impactEnergyJ(), 1e-6);
        assertTrue(earth.flightTimeS() > 1.4 && earth.flightTimeS() < 1.8, "подлёт ~1.6 с: " + earth.flightTimeS());
        // Вакуумный прогноз совпадает с аналитикой v² = v₀² + 2gh
        assertEquals(Math.sqrt(1500 * 1500 + 2 * 9.81 * 2400), vacuum.impactSpeedMs(), 1.0);
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
