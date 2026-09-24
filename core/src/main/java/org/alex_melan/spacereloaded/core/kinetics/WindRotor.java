package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Ветроколесо (005, FR-340, D59): P = ½·ρ·π·R²·v³·C_p(λ), λ = ω·R/v — быстроходность.
 * C_p(λ) = 0.40·(1 − ((λ − 6)/6)²) ≥ 0 — парабола с пиком 0.40 при λ = 6 (ниже предела Бетца
 * 16/27 ≈ 0.593, типично для трёхлопастных колёс). Стартовый момент: при λ < 1 момент считается
 * по λ = 1 (реальные колёса трогаются за счёт угла атаки лопастей на малых оборотах).
 */
public final class WindRotor {

    public static final double BETZ_LIMIT = 16.0 / 27.0;
    public static final double PEAK_CP = 0.40;
    public static final double OPTIMAL_TSR = 6.0;

    private WindRotor() {
    }

    public static double powerCoefficient(double tipSpeedRatio) {
        double x = (tipSpeedRatio - OPTIMAL_TSR) / OPTIMAL_TSR;
        return Math.max(0.0, PEAK_CP * (1 - x * x));
    }

    public static double power(double density, double radius, double windSpeed, double omega) {
        if (density <= 0 || windSpeed <= 0) {
            return 0.0;
        }
        double lambda = Math.abs(omega) * radius / windSpeed;
        return 0.5 * density * Math.PI * radius * radius * Math.pow(windSpeed, 3) * powerCoefficient(lambda);
    }

    /** Момент на валу τ(ω) = P/ω (с ограничением λ ≥ 1 для старта). */
    public static double torque(double density, double radius, double windSpeed, double omega) {
        if (density <= 0 || windSpeed <= 0 || radius <= 0) {
            return 0.0;
        }
        double omegaEff = Math.max(Math.abs(omega), windSpeed / radius);
        return power(density, radius, windSpeed, omegaEff) / omegaEff;
    }
}
