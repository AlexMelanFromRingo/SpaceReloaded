package org.alex_melan.spacereloaded.core.ballistics;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Баллистика снаряда: гравитация + линейное сопротивление (FR-042).
 * Полу-неявный Эйлер, как в FlightIntegrator.
 */
public final class BallisticIntegrator {

    private BallisticIntegrator() {
    }

    public record State(Vec3d pos, Vec3d vel) {
    }

    public static State step(State state, ProjectileSpec spec, double gravity, double dt) {
        Vec3d accel = new Vec3d(0, -gravity, 0)
                .add(state.vel().scale(-spec.dragCoeff()));
        Vec3d vel = state.vel().add(accel.scale(dt));
        Vec3d pos = state.pos().add(vel.scale(dt));
        return new State(pos, vel);
    }

    /**
     * Оценка времени падения до высоты {@code targetY} без сопротивления —
     * для упреждающей загрузки чанков цели (FR-043): корень
     * y₀ + v_y·t − g·t²/2 = y_t.
     *
     * @return секунды; {@code Double.NaN}, если цель недостижима
     */
    public static double etaToAltitude(double y0, double vy, double targetY, double gravity) {
        if (gravity <= 0) {
            // Без гравитации — равномерное движение
            double dy = targetY - y0;
            if (vy == 0) {
                return dy == 0 ? 0 : Double.NaN;
            }
            double t = dy / vy;
            return t >= 0 ? t : Double.NaN;
        }
        // −g/2·t² + vy·t + (y0 − targetY) = 0
        double a = -gravity / 2;
        double b = vy;
        double c = y0 - targetY;
        double disc = b * b - 4 * a * c;
        if (disc < 0) {
            return Double.NaN;
        }
        double sqrt = Math.sqrt(disc);
        double t1 = (-b + sqrt) / (2 * a);
        double t2 = (-b - sqrt) / (2 * a);
        double t = Math.min(t1 > 0 ? t1 : Double.MAX_VALUE, t2 > 0 ? t2 : Double.MAX_VALUE);
        return t == Double.MAX_VALUE ? Double.NaN : t;
    }
}
