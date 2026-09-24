package org.alex_melan.spacereloaded.core.station;

/**
 * Псевдогравитация вращением (007, FR-517/FR-519, D77): g = ω²·r, окружная скорость v_t = ω·r,
 * градиент голова–ноги Δg/g = h/r, вес при ходьбе по/против вращения g' = g·(1 ± v/v_t)²,
 * Кориолис a_c = 2ω·v. Раскрутка двигателями на радиусе r: топливо m = L/(r·Isp·g₀), L = I·ω;
 * энергия вращения E = ½·I·ω². Пределы комфорта (Hall; NASA «Physics of Artificial Gravity»):
 * ω ≤ 2 об/мин всем, до 4–6 после адаптации; v_t ≥ 6 м/с.
 */
public final class SpinGravity {

    public static final double G0 = 9.80665;
    public static final double COMFORT_RPM = 2.0;
    public static final double ADAPTED_RPM = 4.0;
    public static final double MIN_RIM_SPEED = 6.0;

    private SpinGravity() {
    }

    public static double omega(double rpm) {
        return rpm * 2 * Math.PI / 60;
    }

    public static double rpm(double omega) {
        return omega * 60 / (2 * Math.PI);
    }

    public static double gravity(double omega, double radius) {
        return omega * omega * radius;
    }

    public static double radiusFor(double gravity, double rpm) {
        double w = omega(rpm);
        return gravity / (w * w);
    }

    public static double gradient(double headHeight, double radius) {
        return headHeight / radius;
    }

    /** Вес при движении со скоростью v по вращению (prograde) или против. */
    public static double walkingGravity(double omega, double radius, double speed, boolean prograde) {
        double vt = omega * radius;
        double f = prograde ? 1 + speed / vt : 1 - speed / vt;
        return gravity(omega, radius) * f * f;
    }

    public static double coriolis(double omega, double speed) {
        return 2 * omega * speed;
    }

    public static double angularMomentum(double inertia, double omega) {
        return inertia * omega;
    }

    public static double energy(double inertia, double omega) {
        return 0.5 * inertia * omega * omega;
    }

    /** Топливо двигателей на ободе для раскрутки до момента импульса L. */
    public static double spinUpPropellant(double angularMomentum, double radius, double isp) {
        return angularMomentum / (radius * isp * G0);
    }
}
