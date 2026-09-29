package org.alex_melan.spacereloaded.core.kinetics;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Потери ротора: подшипник по весу, пусковой момент, аэродинамика диска. */
class BearingTest {

    /** Стальной маховик 385 кг·м², r = 0.5 м → 3080 кг; на валу r = 0.125 м — 5.7 Н·м на Земле, 0.9 на Луне. */
    @Test
    void flywheelBearing() {
        double m = Bearing.diskMass(385, 0.5);
        assertEquals(3080, m, 1);
        assertEquals(5.67, Bearing.frictionTorque(m, 9.81, 0.125, 0.5), 0.02);
        assertEquals(0.94, Bearing.frictionTorque(m, 1.62, 0.125, 0.5), 0.02);
        assertEquals(0.5, Bearing.frictionTorque(1, 9.81, 0.125, 0.5), 1e-12);   // лёгкий вал — уплотнения
    }

    /** Диск r = 1 м на 35 рад/с в воздухе (ρ 1.2): ≈ 7.4 Н·м; в вакууме — ноль. */
    @Test
    void windage() {
        assertEquals(7.35, Bearing.windageTorque(1.2, 35, 1.0), 0.05);
        assertEquals(0, Bearing.windageTorque(0, 35, 1.0), 1e-12);
        assertEquals(Bearing.windageTorque(1.2, 35, 1.0), Bearing.windageCoefficient(1.2, 35, 1.0) * 35, 1e-9);
    }

    /** Покоящаяся сеть держится трением покоя: момент между рабочим и пусковым трением её не сдвигает. */
    @Test
    void breakawayHoldsNetwork() {
        var g = new KineticGraph();
        g.addNode();
        var analysis = g.analyze(8);
        double c = 5;
        double extra = (Bearing.BREAKAWAY - 1) * c;
        var load = new NodeLoad(c * 1.2, 0, c, 10, extra);   // момент 1.2·c < 1.5·c
        var s = KineticSolver.step(analysis, new NodeLoad[] {load}, 0, 0.05);
        assertTrue(s.resting() && s.omega() == 0);
        var harder = new NodeLoad(c * 1.6, 0, c, 10, extra);
        assertTrue(KineticSolver.step(analysis, new NodeLoad[] {harder}, 0, 0.05).omega() > 0);
    }
}
