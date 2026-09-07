package org.alex_melan.spacereloaded.core.ballistics;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Баллистика снаряда: гравитация + квадратичное сопротивление по плотности
 * атмосферы (FR-042, FR-082). Полу-неявный Эйлер, как в FlightIntegrator.
 */
public final class BallisticIntegrator {

    /** Предел модельного времени прогноза удара — предохранитель от вырожденных данных. */
    private static final double FORECAST_MAX_TIME_S = 600;
    private static final double FORECAST_DT = 0.05;

    private BallisticIntegrator() {
    }

    public record State(Vec3d pos, Vec3d vel) {
    }

    /**
     * Прогноз удара (терминал орудия): скорость и энергия при достижении высоты цели.
     *
     * @param impactSpeedMs  скорость удара, м/с
     * @param impactEnergyJ  E = ½mv², Дж
     * @param flightTimeS    время подлёта, с (NaN — цель не достигнута за предел времени)
     */
    public record Forecast(double impactSpeedMs, double impactEnergyJ, double flightTimeS) {
    }

    /**
     * Шаг с сопротивлением: a = g↓ − ½ρ|v|C_dA·v/m. Сопротивление за шаг ограничено
     * так, чтобы не развернуть скорость (устойчивость при экстремальном датапаке).
     *
     * @param density плотность атмосферы на текущей высоте, кг/м³ (0 — вакуум)
     */
    public static State step(State state, ProjectileSpec spec, double gravity, double density, double dt) {
        Vec3d accel = new Vec3d(0, -gravity, 0);
        if (density > 0 && spec.cd() > 0 && spec.areaM2() > 0) {
            Vec3d dragAccel = spec.dragBody().force(state.vel(), density).scale(1.0 / spec.massKg());
            double maxDecel = state.vel().length() / dt;
            double decel = dragAccel.length();
            if (decel > maxDecel && decel > 0) {
                dragAccel = dragAccel.scale(maxDecel / decel);
            }
            accel = accel.add(dragAccel);
        }
        Vec3d vel = state.vel().add(accel.scale(dt));
        Vec3d pos = state.pos().add(vel.scale(dt));
        return new State(pos, vel);
    }

    /** Шаг в вакууме (совместимость с расчётами 001). */
    public static State step(State state, ProjectileSpec spec, double gravity, double dt) {
        return step(state, spec, gravity, 0, dt);
    }

    /**
     * Прогноз удара: спуск с высоты {@code targetY + dropAltitude} со скоростью
     * {@code muzzleSpeed} вниз сквозь атмосферу до высоты цели.
     */
    public static Forecast impactForecast(ProjectileSpec spec, double dropAltitude, double muzzleSpeed,
                                          double gravity, AtmosphereProfile atmosphere, double targetY) {
        State state = new State(new Vec3d(0, targetY + dropAltitude, 0), new Vec3d(0, -muzzleSpeed, 0));
        double time = 0;
        while (state.pos().y() > targetY && time < FORECAST_MAX_TIME_S) {
            state = step(state, spec, gravity, atmosphere.density(state.pos().y()), FORECAST_DT);
            time += FORECAST_DT;
        }
        double speed = state.vel().length();
        boolean arrived = state.pos().y() <= targetY;
        return new Forecast(speed, ImpactEnergy.kineticEnergyJ(spec.massKg(), speed),
                arrived ? time : Double.NaN);
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
