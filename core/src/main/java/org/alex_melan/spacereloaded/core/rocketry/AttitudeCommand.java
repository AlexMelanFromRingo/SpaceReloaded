package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Команда ориентации пилота (Полёт 2.0, FR-070, D14): командуемые тангаж и крен
 * в градусах. Пока клавиши наклона удержаны, каждая ось плавно идёт к своей цели
 * (предел × компонента направления) со скоростью {@code rate}; при отпускании —
 * так же плавно возвращается к нулю. Отрабатывают команду гиродины
 * ({@link FlightIntegrator}, PD-контур); без гиродинов команда не действует.
 *
 * <p>Знаки согласованы с {@link FlightIntegrator#rotate}: положительный тангаж
 * наклоняет ось тяги к +Z, положительный крен — к −X. Упрощение (документировано):
 * при диагональном наклоне оси складываются как малые углы, суммарный наклон
 * ≈ √(pitch² + roll²) — на пределе 45° погрешность направления < 5°.
 *
 * @param pitchDeg командуемый тангаж, градусы
 * @param rollDeg  командуемый крен, градусы
 */
public record AttitudeCommand(double pitchDeg, double rollDeg) {

    public static final AttitudeCommand LEVEL = new AttitudeCommand(0, 0);

    /**
     * Шаг команды.
     *
     * @param inputX        желаемое направление наклона по X (не нормировано; 0 — отпущено)
     * @param inputZ        желаемое направление наклона по Z
     * @param dt            шаг, с
     * @param rateDegPerSec скорость изменения угла, °/с
     * @param maxDeg        предел наклона, градусы
     */
    public AttitudeCommand step(double inputX, double inputZ, double dt, double rateDegPerSec, double maxDeg) {
        double length = Math.hypot(inputX, inputZ);
        double nx = length > 1e-9 ? inputX / length : 0;
        double nz = length > 1e-9 ? inputZ / length : 0;
        double targetPitch = maxDeg * nz;
        double targetRoll = -maxDeg * nx;
        double step = rateDegPerSec * dt;
        double pitch = approach(pitchDeg, targetPitch, step);
        double roll = approach(rollDeg, targetRoll, step);
        if (Math.abs(pitch) < 1e-9 && Math.abs(roll) < 1e-9) {
            return LEVEL;
        }
        return new AttitudeCommand(pitch, roll);
    }

    private static double approach(double value, double target, double maxStep) {
        double delta = target - value;
        if (Math.abs(delta) <= maxStep) {
            return target;
        }
        return value + Math.copySign(maxStep, delta);
    }

    /** Суммарный командуемый наклон, градусы (малые углы). */
    public double tiltDeg() {
        return Math.hypot(pitchDeg, rollDeg);
    }

    public double pitchRad() {
        return Math.toRadians(pitchDeg);
    }

    public double rollRad() {
        return Math.toRadians(rollDeg);
    }
}
