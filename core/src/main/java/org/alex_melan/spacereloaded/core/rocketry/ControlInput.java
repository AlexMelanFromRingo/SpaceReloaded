package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Ввод пилота (валидируется сервером — принцип VI).
 *
 * @param throttle  дроссель 0..1 (все двигатели, v1 без раздельного управления)
 * @param pitchCmd  желаемый тангаж, рад (отрабатывается гиродинами при stabilize)
 * @param rollCmd   желаемый крен, рад
 * @param stabilize включена ли стабилизация гиродинами
 */
public record ControlInput(double throttle, double pitchCmd, double rollCmd, boolean stabilize) {
    public static final ControlInput COAST = new ControlInput(0, 0, 0, false);
    public static final ControlInput FULL_STABILIZED = new ControlInput(1, 0, 0, true);
    public static final ControlInput FULL_UNSTABILIZED = new ControlInput(1, 0, 0, false);

    public ControlInput {
        throttle = Math.clamp(throttle, 0, 1);
    }
}
