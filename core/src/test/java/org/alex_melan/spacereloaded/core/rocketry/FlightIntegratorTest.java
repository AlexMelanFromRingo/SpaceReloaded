package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightIntegratorTest {

    private static final double G0 = PerformanceCalculator.G0;
    private static final String KEROLOX = "kerolox";
    private static final double DT = 0.01;

    private static RocketStructure symmetricRocket() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 1, engine));
        parts.add(PlacedPart.of(1, 0, -1, engine));
        parts.add(PlacedPart.of(-1, 0, 1, engine));
        parts.add(PlacedPart.of(-1, 0, -1, engine));
        return new RocketStructure(parts);
    }

    private static RocketStructure asymmetricRocket(double gyroTorque) {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 1, engine));
        parts.add(PlacedPart.of(1, 0, -1, engine));
        parts.add(PlacedPart.of(-1, 0, 1, engine)); // четвёртый двигатель снят
        if (gyroTorque > 0) {
            parts.add(PlacedPart.of(0, 0, 0, PartProperties.gyro(200, gyroTorque)));
        }
        return new RocketStructure(parts);
    }

    /**
     * Вертикальный подъём против замкнутой формы: при постоянной тяге и расходе
     * v(t) = u·ln(m₀/m(t)) − g·t,
     * h(t) = u·τ·(w·ln w − w + 1) − g·t²/2, где u = Isp·g₀, τ = m₀/ṁ, w = m(t)/m₀.
     */
    @Test
    void verticalAscentMatchesClosedForm() {
        RocketStructure rocket = symmetricRocket();
        double m0 = 4400;
        double thrust = 4 * 60_000;
        double u = 300 * G0;
        double mdot = thrust / u;
        double tau = m0 / mdot;
        double g = 9.81;
        double t = 10.0;

        FlightState state = FlightState.atRest(Vec3d.ZERO, 2000);
        FlightEnvironment env = new FlightEnvironment(g);
        int steps = (int) Math.round(t / DT);
        for (int i = 0; i < steps; i++) {
            state = FlightIntegrator.step(rocket, state, ControlInput.FULL_UNSTABILIZED, env, DT);
        }

        double w = (m0 - mdot * t) / m0;
        double expectedV = u * Math.log(1 / w) - g * t;
        double expectedH = u * tau * (w * Math.log(w) - w + 1) - g * t * t / 2;

        assertEquals(expectedV, state.vel().y(), Math.abs(expectedV) * 0.005,
                "скорость против аналитики (0.5%)");
        assertEquals(expectedH, state.pos().y(), Math.abs(expectedH) * 0.005,
                "высота против аналитики (0.5%)");
        assertEquals(0, state.pitch(), 1e-12, "симметричная ракета не вращается");
        assertEquals(0, state.roll(), 1e-12);
    }

    /** Полное выгорание в невесомости → конечная скорость равна Δv Циолковского. */
    @Test
    void burnoutSpeedEqualsTsiolkovskyDeltaV() {
        RocketStructure rocket = symmetricRocket();
        FlightState state = FlightState.atRest(Vec3d.ZERO, 2000);
        FlightEnvironment env = FlightEnvironment.WEIGHTLESS;

        int i = 0;
        while (state.propellantKg() > 0 && i++ < 100_000) {
            state = FlightIntegrator.step(rocket, state, ControlInput.FULL_UNSTABILIZED, env, DT);
        }

        double expected = 300 * G0 * Math.log(4400.0 / 2400.0);
        assertEquals(expected, state.vel().length(), expected * 0.01,
                "скорость выгорания = Δv по Циолковскому (1%)");
        assertEquals(0, state.propellantKg(), 1e-9);
    }

    /** Начальное угловое ускорение асимметричной ракеты соответствует τ/I. */
    @Test
    void asymmetricThrustProducesExpectedInitialAngularAcceleration() {
        RocketStructure rocket = asymmetricRocket(0);
        FlightState before = FlightState.atRest(Vec3d.ZERO, 2000);
        FlightState after = FlightIntegrator.step(rocket, before,
                ControlInput.FULL_UNSTABILIZED, FlightEnvironment.EARTH, DT);

        // Ожидаемые момент и инерция считаем той же моделью (точечные массы):
        RocketPerformance perf = PerformanceCalculator.calculate(rocket, 9.81);
        double torqueX = 0;
        double torqueZ = 0;
        for (PlacedPart part : rocket.parts()) {
            if (part.properties().role() == PartRole.ENGINE) {
                Vec3d r = part.center().subtract(perf.centerOfMass());
                torqueX += -r.z() * part.properties().thrustN();
                torqueZ += r.x() * part.properties().thrustN();
            }
        }
        double expectedPitchRate = torqueX / perf.momentOfInertia().x() * DT;
        double expectedRollRate = torqueZ / perf.momentOfInertia().z() * DT;

        assertEquals(expectedPitchRate, after.pitchRate(), Math.abs(expectedPitchRate) * 0.01);
        assertEquals(expectedRollRate, after.rollRate(), Math.abs(expectedRollRate) * 0.01);
        assertTrue(Math.abs(after.pitchRate()) > 0 || Math.abs(after.rollRate()) > 0,
                "асимметрия обязана создавать вращение");
    }

    /** SC-004: без стабилизации заваливается, с гиродинами держит вертикаль. */
    @Test
    void gyroStabilizationHoldsVerticalWhereUnstabilizedTips() {
        FlightEnvironment env = FlightEnvironment.EARTH;
        int steps = (int) Math.round(5.0 / DT);

        FlightState unstabilized = FlightState.atRest(Vec3d.ZERO, 2000);
        RocketStructure noGyro = asymmetricRocket(0);
        for (int i = 0; i < steps; i++) {
            unstabilized = FlightIntegrator.step(noGyro, unstabilized,
                    ControlInput.FULL_UNSTABILIZED, env, DT);
        }
        double tilt = Math.abs(unstabilized.pitch()) + Math.abs(unstabilized.roll());
        assertTrue(tilt > 0.15, "без гиродинов за 5 с крен+тангаж заметны, получено " + tilt);

        FlightState stabilized = FlightState.atRest(Vec3d.ZERO, 2000);
        RocketStructure withGyro = asymmetricRocket(500_000);
        for (int i = 0; i < steps; i++) {
            stabilized = FlightIntegrator.step(withGyro, stabilized,
                    ControlInput.FULL_STABILIZED, env, DT);
        }
        double stabilizedTilt = Math.abs(stabilized.pitch()) + Math.abs(stabilized.roll());
        assertTrue(stabilizedTilt < 0.02,
                "гиродины удерживают вертикаль, получено " + stabilizedTilt);
    }

    /** Топливо кончилось — тяга нулевая, начинается баллистическое падение. */
    @Test
    void thrustCeasesAtPropellantDepletion() {
        RocketStructure rocket = symmetricRocket();
        FlightState state = new FlightState(Vec3d.ZERO, Vec3d.ZERO, 0, 0, 0, 0, 0.5);
        FlightEnvironment env = FlightEnvironment.EARTH;

        // Первый шаг дожигает остаток
        state = FlightIntegrator.step(rocket, state, ControlInput.FULL_UNSTABILIZED, env, DT);
        assertEquals(0, state.propellantKg(), 1e-9);

        double vyAfterBurnout = state.vel().y();
        state = FlightIntegrator.step(rocket, state, ControlInput.FULL_UNSTABILIZED, env, DT);
        assertEquals(vyAfterBurnout - 9.81 * DT, state.vel().y(), 1e-9,
                "после выгорания — чистая гравитация");
    }
}
