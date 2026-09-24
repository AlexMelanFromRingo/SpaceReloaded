package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Маховик — сплошной диск (005, FR-310, R4): I = ½·m·r², запас E = ½·I·ω². Предельная
 * скорость — по окружному напряжению в центре вращающегося диска σ = (3+ν)/8·ρ·ω²·r² ≤ σ_доп:
 * ω_max = √(8·σ_доп / ((3+ν)·ρ·r²)). Сталь (ρ 7850, ν 0.3, σ 400 МПа), r = 0.5 м → 703 рад/с.
 */
public final class Flywheel {

    private Flywheel() {
    }

    public static double solidDiskInertia(double massKg, double radiusM) {
        return 0.5 * massKg * radiusM * radiusM;
    }

    public static double maxOmega(double allowableStressPa, double density, double radiusM, double poisson) {
        return Math.sqrt(8 * allowableStressPa / ((3 + poisson) * density * radiusM * radiusM));
    }

    public static double energy(double inertia, double omega) {
        return 0.5 * inertia * omega * omega;
    }
}
