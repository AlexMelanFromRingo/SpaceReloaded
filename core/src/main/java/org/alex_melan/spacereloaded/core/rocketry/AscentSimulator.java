package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.DragBody;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;

/**
 * Численное моделирование вертикального подъёма (Полёт 2.0, FR-084, D12):
 * тот же интегратор, что и в полёте, — полная тяга, авто-отделение выгоревших
 * ступеней, сопротивление по профилю атмосферы, после выгорания последней
 * ступени — свободный полёт до апогея. Заменяет оценку 1.15·√(2gh): стоимость
 * подъёма включает гравитационные и аэродинамические потери честно.
 *
 * <p>Упрощения (документированы): подъём строго вертикальный, без наклона;
 * отделение мгновенное; модельное время ограничено {@link #MAX_TIME_S}.
 */
public final class AscentSimulator {

    public static final double DT = 0.05;
    /** Предохранитель от вырожденных конфигов (TWR ≈ 1 на веки вечные). */
    public static final double MAX_TIME_S = 600;

    /**
     * @param reachedTarget           достигнута ли высота перехода
     * @param apexM                   максимальная высота (абсолютная), м
     * @param deltaVSpentToTargetMs   Δv, потраченный до высоты перехода, м/с (0, если не достигнута)
     * @param maxDynamicPressurePa    пиковый скоростной напор, Па
     * @param timeToTargetS           время до высоты перехода, с (NaN, если не достигнута)
     * @param stagesUsed              сколько ступеней зажигалось
     */
    public record AscentReport(boolean reachedTarget, double apexM, double deltaVSpentToTargetMs,
                               double maxDynamicPressurePa, double timeToTargetS, int stagesUsed) {
    }

    private AscentSimulator() {
    }

    /**
     * @param layout             раскладка ступеней
     * @param stagePropellantKg  топливо по ступеням
     * @param env                среда старта (гравитация + атмосфера)
     * @param cd                 коэффициент сопротивления ракеты
     * @param startY             абсолютная высота старта (низ стека)
     * @param targetY            абсолютная высота перехода
     */
    public static AscentReport simulate(StageLayout layout, double[] stagePropellantKg,
                                        FlightEnvironment env, double cd, double startY, double targetY) {
        double[] fuel = new double[layout.stageCount()];
        for (int i = 0; i < fuel.length && i < stagePropellantKg.length; i++) {
            fuel[i] = Math.max(0, stagePropellantKg[i]);
        }
        int active = 0;
        int stagesUsed = 1;
        RocketStructure view = layout.activeView(active, fuel);
        DragBody drag = view.dragBody(cd);
        RocketPerformance stagePerf = PerformanceCalculator.calculate(view, Math.max(env.gravity(), 1e-6));
        FlightState state = FlightState.atRest(new Vec3d(0, startY, 0), fuel[active]);

        boolean reached = startY >= targetY;
        double timeToTarget = reached ? 0 : Double.NaN;
        double time = 0;
        double deltaVSpent = 0;
        double maxQ = 0;
        double apex = startY;

        while (time < MAX_TIME_S && !reached) {
            boolean thrusting = state.propellantKg() > 0;
            if (!thrusting && active < layout.stageCount() - 1) {
                // Выгорела — мгновенное отделение, следующая ступень (FR-065)
                fuel[active] = 0;
                active++;
                stagesUsed++;
                view = layout.activeView(active, fuel);
                drag = view.dragBody(cd);
                stagePerf = PerformanceCalculator.calculate(view, Math.max(env.gravity(), 1e-6));
                state = new FlightState(state.pos(), state.vel(), state.pitch(), state.roll(),
                        state.pitchRate(), state.rollRate(), fuel[active]);
                thrusting = state.propellantKg() > 0;
            }
            ControlInput input = thrusting ? ControlInput.FULL_STABILIZED : ControlInput.COAST;
            if (thrusting) {
                double mass = stagePerf.dryMassKg() + state.propellantKg();
                deltaVSpent += stagePerf.totalThrustN() / Math.max(mass, 1e-9) * DT;
            }
            FlightState next = FlightIntegrator.step(view, state, input, env, DT, drag);
            fuel[active] = next.propellantKg();
            time += DT;

            double density = env.density(next.pos().y());
            maxQ = Math.max(maxQ, Aerothermal.dynamicPressure(density, next.vel().length()));
            apex = Math.max(apex, next.pos().y());
            if (next.pos().y() >= targetY) {
                reached = true;
                timeToTarget = time;
            }
            boolean falling = next.vel().y() <= 0;
            if (falling && (!thrusting || next.pos().y() < startY)) {
                // Апогей пройден без тяги, либо тяги не хватает оторваться от стола
                state = next;
                break;
            }
            state = next;
        }
        if (!reached) {
            deltaVSpent = 0;
        }
        return new AscentReport(reached, apex, deltaVSpent, maxQ, timeToTarget, stagesUsed);
    }
}
