package org.alex_melan.spacereloaded.core.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Решатель трансмиссии (005, SC-001) против аналитики. */
class KineticSolverTest {

    // Мотор-генератор по умолчанию: 300 кВт пика, ω₀ = 1500 об/мин
    private static final double OMEGA0 = 1500 * 2 * Math.PI / 60;
    private static final double STALL = 4 * 300_000 / OMEGA0;
    private static final double DT = 0.05;

    private static NodeLoad motor() {
        return new NodeLoad(STALL, STALL / OMEGA0, 2, 1.0);
    }

    private static double settle(KineticGraph.Analysis a, NodeLoad[] loads) {
        double omega = 0;
        for (int i = 0; i < 4000; i++) {
            omega = KineticSolver.step(a, loads, omega, DT).omega();
        }
        return omega;
    }

    @Test
    void motorWithFrictionReachesAnalyticEquilibrium() {
        KineticGraph g = new KineticGraph();
        int m = g.addNode();
        NodeLoad[] loads = new NodeLoad[11];
        loads[m] = motor();
        int prev = m;
        for (int i = 1; i <= 10; i++) {
            int s = g.addNode();
            g.addEdge(prev, s, 1, 1);
            loads[s] = new NodeLoad(0, 0, 2, 0.5);
            prev = s;
        }
        KineticGraph.Analysis a = g.analyze(512);
        double c = 11 * 2;
        double expected = (STALL - c) / (STALL / OMEGA0);
        assertEquals(expected, settle(a, loads), expected * 0.005);
        assertEquals(expected, KineticSolver.equilibrium(a, loads), 1e-9);
    }

    @Test
    void gearPairConservesPowerWithEfficiency() {
        KineticGraph g = new KineticGraph();
        int m = g.addNode();
        int out = g.addNode();
        g.addEdge(m, out, -0.5, 0.98);
        double load = 10_000; // Н·м на медленном валу (приведённые 5 кН·м < пускового 7.6 кН·м)
        NodeLoad[] loads = {new NodeLoad(STALL, STALL / OMEGA0, 0, 1), new NodeLoad(0, 0, load, 10)};
        KineticGraph.Analysis a = g.analyze(512);
        double omega = settle(a, loads);
        assertTrue(omega > 10, "сеть должна вращаться, ω = " + omega);
        // Мощность мотора = мощность нагрузки / η
        double motorPower = (STALL - STALL / OMEGA0 * omega) * omega;
        double loadPower = load * 0.5 * omega;
        assertEquals(loadPower / 0.98, motorPower, motorPower * 0.001);
        // Медленный вал вращается вдвое медленнее и в обратную сторону
        assertEquals(-0.5 * omega, a.ratio()[out] * omega, 1e-9);
    }

    @Test
    void transmittedTorqueEqualsLoadAtSteadyState() {
        KineticGraph g = new KineticGraph();
        int m = g.addNode();
        int out = g.addNode();
        g.addEdge(m, out, -0.5, 1.0);
        double load = 10_000;
        NodeLoad[] loads = {new NodeLoad(STALL, STALL / OMEGA0, 0, 1), new NodeLoad(0, 0, load, 10)};
        KineticGraph.Analysis a = g.analyze(512);
        double omega = settle(a, loads);
        KineticSolver.Step s = KineticSolver.step(a, loads, omega, DT);
        assertEquals(load, s.transmitted()[out], load * 0.001);
    }

    @Test
    void spinUpTorqueIsInertiaTimesLocalAcceleration() {
        // Маховик за понижением ×4: при разгоне вал к маховику несёт I·α_loc
        KineticGraph g = new KineticGraph();
        int m = g.addNode();
        int fw = g.addNode();
        g.addEdge(m, fw, 0.25, 1.0);
        NodeLoad[] loads = {new NodeLoad(STALL, STALL / OMEGA0, 0, 0), new NodeLoad(0, 0, 0, 385)};
        KineticGraph.Analysis a = g.analyze(512);
        KineticSolver.Step s = KineticSolver.step(a, loads, 0, DT);
        double alphaLocal = 0.25 * s.alpha();
        assertEquals(385 * alphaLocal, s.transmitted()[fw], 385 * alphaLocal * 1e-9);
        // Порядок величины: ≈ 30 кН·м — деревянный вал (24.5 кН·м) срежется, стальной выдержит
        assertTrue(s.transmitted()[fw] > ShaftStrength.maxTorque(0.25, 8e6));
        assertTrue(s.transmitted()[fw] < ShaftStrength.maxTorque(0.25, 230e6));
    }

    @Test
    void overdrivenMotorGenerates() {
        // Мотор B через повышающую передачу ×2: сеть крутит его выше ω₀ — он генерирует
        KineticGraph g = new KineticGraph();
        int a = g.addNode();
        int b = g.addNode();
        g.addEdge(a, b, 2.0, 1.0);
        NodeLoad[] loads = {motor(), motor()};
        KineticGraph.Analysis an = g.analyze(512);
        double omega = settle(an, loads);
        KineticSolver.Step s = KineticSolver.step(an, loads, omega, DT);
        assertTrue(s.power()[a] > 0, "ведущий мотор потребляет");
        assertTrue(s.power()[b] < 0, "перекрученный мотор генерирует");
    }

    @Test
    void staticFrictionHoldsAndStopsWithoutSource() {
        KineticGraph g = new KineticGraph();
        g.addNode();
        KineticGraph.Analysis a = g.analyze(512);
        NodeLoad[] loads = {new NodeLoad(10, 0, 50, 1)};
        KineticSolver.Step s = KineticSolver.step(a, loads, 0, DT);
        assertTrue(s.resting());
        assertEquals(0.0, s.omega());
        // Выбег: без источника вращение гасится трением и не меняет знак
        NodeLoad[] coast = {new NodeLoad(0, 0, 50, 1)};
        double omega = 10;
        for (int i = 0; i < 200; i++) {
            omega = KineticSolver.step(a, coast, omega, DT).omega();
            assertTrue(omega >= 0);
        }
        assertEquals(0.0, omega);
    }

    @Test
    void flywheelSpinUpTimeMatchesIntegral() {
        // J·dω/dt = A − Bω → ω(t) = ω*(1 − e^(−Bt/J)); за t = J/B достигаем 63.2 %
        KineticGraph g = new KineticGraph();
        g.addNode();
        KineticGraph.Analysis a = g.analyze(512);
        NodeLoad[] loads = {new NodeLoad(STALL, STALL / OMEGA0, 0, 385)};
        double tau = 385 / (STALL / OMEGA0);
        int steps = (int) Math.round(tau / 0.001);
        double omega = 0;
        for (int i = 0; i < steps; i++) {
            omega = KineticSolver.step(a, loads, omega, 0.001).omega();
        }
        assertEquals(OMEGA0 * (1 - Math.exp(-1)), omega, OMEGA0 * 0.01);
    }
}
