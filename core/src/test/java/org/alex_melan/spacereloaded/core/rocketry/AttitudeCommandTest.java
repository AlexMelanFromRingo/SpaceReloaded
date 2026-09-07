package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AttitudeCommandTest {

    private static final double RATE = 30.0;
    private static final double MAX = 45.0;
    private static final double DT = 0.05;

    private static AttitudeCommand hold(AttitudeCommand cmd, double dirX, double dirZ, double seconds) {
        int steps = (int) Math.round(seconds / DT);
        for (int i = 0; i < steps; i++) {
            cmd = cmd.step(dirX, dirZ, DT, RATE, MAX);
        }
        return cmd;
    }

    /** Удержание: угол растёт со скоростью rate до предела; отпускание — спад к нулю. */
    @Test
    void tiltRampsUpWhileHeldAndBackWhenReleased() {
        AttitudeCommand cmd = hold(AttitudeCommand.LEVEL, 0, 1, 1.0);
        assertEquals(30.0, cmd.tiltDeg(), 1e-9, "через 1 с при 30°/с");
        cmd = hold(cmd, 0, 1, 2.0);
        assertEquals(MAX, cmd.tiltDeg(), 1e-9, "предел 45°");
        cmd = hold(cmd, 0, 0, 1.0);
        assertEquals(15.0, cmd.tiltDeg(), 1e-9, "отпустили — спадает");
        cmd = hold(cmd, 0, 0, 5.0);
        assertEquals(0.0, cmd.tiltDeg(), 1e-9);
        assertEquals(0.0, cmd.pitchRad(), 1e-12);
        assertEquals(0.0, cmd.rollRad(), 1e-12);
    }

    /**
     * Знаки: наклон в сторону (dirX, dirZ) даёт ось тяги (по {@link FlightIntegrator#axis})
     * с горизонтальной составляющей в ту же сторону — для 8 направлений.
     */
    @Test
    void thrustAxisTiltsTowardCommandedDirection() {
        double[][] directions = {
                {1, 0}, {-1, 0}, {0, 1}, {0, -1},
                {1, 1}, {1, -1}, {-1, 1}, {-1, -1}
        };
        for (double[] dir : directions) {
            double len = Math.hypot(dir[0], dir[1]);
            double nx = dir[0] / len;
            double nz = dir[1] / len;
            AttitudeCommand cmd = hold(AttitudeCommand.LEVEL, dir[0], dir[1], 1.0); // ~30°
            Vec3d axis = FlightIntegrator.axis(cmd.pitchRad(), cmd.rollRad());
            double horizontal = Math.hypot(axis.x(), axis.z());
            assertTrue(horizontal > 0.3, "наклон даёт горизонтальную составляющую: " + horizontal);
            double cos = (axis.x() * nx + axis.z() * nz) / horizontal;
            assertTrue(cos > 0.99, "ось тяги смотрит в командуемую сторону " + nx + "," + nz + ": cos=" + cos);
            assertTrue(axis.y() > 0.7, "ось всё ещё в основном вверх (диагональ ≈ 42°)");
        }
    }

    /** Смена направления — плавная: угол не прыгает, а перетекает через ноль. */
    @Test
    void directionChangeIsContinuous() {
        AttitudeCommand cmd = hold(AttitudeCommand.LEVEL, 0, 1, 1.5);   // 45° к +Z
        AttitudeCommand next = cmd.step(0, -1, DT, RATE, MAX);
        assertEquals(cmd.pitchDeg() - RATE * DT, next.pitchDeg(), 1e-9, "к −Z угол убывает шагом rate·dt");
        assertTrue(Math.abs(next.tiltDeg() - cmd.tiltDeg()) < RATE * DT + 1e-9);
    }

    @Test
    void zeroInputFromLevelStaysLevel() {
        AttitudeCommand cmd = AttitudeCommand.LEVEL.step(0, 0, DT, RATE, MAX);
        assertEquals(AttitudeCommand.LEVEL, cmd);
    }
}
