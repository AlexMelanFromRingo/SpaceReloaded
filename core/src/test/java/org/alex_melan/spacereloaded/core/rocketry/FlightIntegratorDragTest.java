package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.alex_melan.spacereloaded.core.atmosphere.DragBody;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlightIntegratorDragTest {

    private static final String KEROLOX = "kerolox";
    private static final double DT = 0.05;
    private static final double CD = 0.5;

    /** Колонна 1×3×1: двигатель, бак, командный модуль — площадь сечения сверху 1 м². */
    private static RocketStructure column(double propellantKg) {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, 60_000, 300, KEROLOX)));
        parts.add(new PlacedPart(org.alex_melan.spacereloaded.core.geometry.PackedPos.pack(0, 1, 0),
                PartProperties.tank(300, 2000, KEROLOX), propellantKg));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        return new RocketStructure(parts);
    }

    @Test
    void dragBodyProjectsBoundingBox() {
        DragBody body = column(0).dragBody(CD);
        assertEquals(1.0, body.areaY(), 1e-9, "сверху/снизу — 1×1");
        assertEquals(3.0, body.areaX(), 1e-9, "сбоку — 1×3");
        assertEquals(3.0, body.areaZ(), 1e-9);
        assertEquals(CD, body.cd(), 1e-9);
        assertEquals(1.0, body.effectiveArea(new Vec3d(0, -10, 0)), 1e-9);
        assertEquals(3.0, body.effectiveArea(new Vec3d(7, 0, 0)), 1e-9);
        assertEquals(DragBody.NONE, column(0).dragBody(0));
    }

    /** Падение без тяги при постоянной плотности сходится к v_t = √(2mg/(ρC_dA)) (2%). */
    @Test
    void freeFallConvergesToTerminalVelocity() {
        RocketStructure rocket = column(0);
        double mass = 1200;
        double g = 9.81;
        double rho = 1.225;
        AtmosphereProfile constant = new AtmosphereProfile(rho, 1.0e9, 0); // H → ∞: плотность постоянна
        FlightEnvironment env = new FlightEnvironment(g, constant);
        DragBody drag = rocket.dragBody(CD);

        FlightState state = FlightState.atRest(new Vec3d(0, 10_000, 0), 0);
        for (int i = 0; i < (int) (60 / DT); i++) {
            state = FlightIntegrator.step(rocket, state, ControlInput.COAST, env, DT, drag);
        }
        double expected = Math.sqrt(2 * mass * g / (rho * CD * 1.0));
        assertEquals(expected, Math.abs(state.vel().y()), expected * 0.02,
                "терминальная скорость (2%): ожидалось " + expected + ", получено " + state.vel().y());
        assertTrue(Math.abs(state.vel().y()) <= expected * 1.001, "скорость не превышает терминальную");
    }

    /** В вакууме траектория с DragBody тождественна старому расчёту (0.1% → здесь 1e-9). */
    @Test
    void vacuumTrajectoryIsIdenticalToLegacyStep() {
        RocketStructure rocket = column(2000);
        FlightEnvironment vacuum = new FlightEnvironment(9.81, AtmosphereProfile.VACUUM);
        FlightEnvironment legacy = new FlightEnvironment(9.81);
        DragBody drag = rocket.dragBody(CD);

        FlightState a = FlightState.atRest(Vec3d.ZERO, 2000);
        FlightState b = FlightState.atRest(Vec3d.ZERO, 2000);
        for (int i = 0; i < 200; i++) {
            a = FlightIntegrator.step(rocket, a, ControlInput.FULL_STABILIZED, vacuum, DT, drag);
            b = FlightIntegrator.step(rocket, b, ControlInput.FULL_STABILIZED, legacy, DT);
        }
        assertEquals(b.pos().y(), a.pos().y(), 1e-9);
        assertEquals(b.vel().y(), a.vel().y(), 1e-9);
        assertEquals(b.propellantKg(), a.propellantKg(), 1e-9);
    }

    /** Атмосфера на тяге: ускорение меньше, чем в вакууме, и убывает с высотой по профилю. */
    @Test
    void atmosphereReducesAccelerationAndThinsOut() {
        RocketStructure rocket = column(2000);
        AtmosphereProfile earth = new AtmosphereProfile(1.225, 110, 63);
        DragBody drag = rocket.dragBody(CD);

        FlightState air = FlightState.atRest(new Vec3d(0, 63, 0), 2000);
        FlightState vac = FlightState.atRest(new Vec3d(0, 63, 0), 2000);
        for (int i = 0; i < 200; i++) {
            air = FlightIntegrator.step(rocket, air, ControlInput.FULL_STABILIZED,
                    new FlightEnvironment(9.81, earth), DT, drag);
            vac = FlightIntegrator.step(rocket, vac, ControlInput.FULL_STABILIZED,
                    new FlightEnvironment(9.81), DT, drag);
        }
        assertTrue(air.vel().y() < vac.vel().y(), "сопротивление съедает скорость");
        assertTrue(air.vel().y() > 0.8 * vac.vel().y(), "…но на игровых скоростях — проценты, не разы");
        assertTrue(earth.density(450) < 0.05 * earth.density(63), "у высоты перехода — проценты плотности");
    }

    /** Экстремальный старт (500 м/с у поверхности): ни NaN, ни разворота скорости. */
    @Test
    void extremeSpeedStaysStableWithClipping() {
        RocketStructure rocket = column(0);
        AtmosphereProfile dense = new AtmosphereProfile(50, 1000, 0); // предел датапака
        FlightEnvironment env = new FlightEnvironment(9.81, dense);
        DragBody drag = rocket.dragBody(3.0);

        FlightState state = new FlightState(new Vec3d(0, 100, 0), new Vec3d(500, 0, 0), 0, 0, 0, 0, 0);
        for (int i = 0; i < 100; i++) {
            FlightState next = FlightIntegrator.step(rocket, state, ControlInput.COAST, env, DT, drag);
            assertFalse(Double.isNaN(next.vel().x()) || Double.isNaN(next.pos().x()), "нет NaN");
            assertTrue(next.vel().x() >= -1e-9, "сопротивление не разворачивает скорость: " + next.vel().x());
            state = next;
        }
        assertTrue(state.vel().x() < 1.0, "плотная атмосфера остановила тело");
    }
}
