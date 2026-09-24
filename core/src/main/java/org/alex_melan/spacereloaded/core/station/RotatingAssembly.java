package org.alex_melan.spacereloaded.core.station;

import java.util.List;

/**
 * Вращающаяся сборка (007, FR-518, D77): момент инерции I = Σmᵢ·ρᵢ² и дисбаланс — вектор Σmᵢ·ρ⃗ᵢ
 * (ρ⃗ — перпендикуляр от оси), сила на подшипник F = |Σm·ρ⃗|·ω². Ось — прямая через точку
 * (ox, oy, oz) по единичному направлению (одна из координатных осей мира).
 */
public final class RotatingAssembly {

    /** Масса в точке (центр блока). */
    public record Mass(double x, double y, double z, double kg) {
    }

    public enum Axis { X, Y, Z }

    private final double inertia;
    private final double imbalance;
    private final double totalMass;

    public RotatingAssembly(List<Mass> masses, Axis axis, double ox, double oy, double oz) {
        double i = 0;
        double sa = 0;
        double sb = 0;
        double m = 0;
        for (Mass p : masses) {
            double a;
            double b;
            switch (axis) {
                case X -> { a = p.y - oy; b = p.z - oz; }
                case Y -> { a = p.x - ox; b = p.z - oz; }
                default -> { a = p.x - ox; b = p.y - oy; }
            }
            i += p.kg * (a * a + b * b);
            sa += p.kg * a;
            sb += p.kg * b;
            m += p.kg;
        }
        this.inertia = i;
        this.imbalance = Math.hypot(sa, sb);
        this.totalMass = m;
    }

    public double inertia() {
        return inertia;
    }

    /** |Σm·ρ⃗|, кг·м. */
    public double imbalance() {
        return imbalance;
    }

    public double totalMass() {
        return totalMass;
    }

    /** Сила дисбаланса на подшипнике при ω, Н. */
    public double bearingForce(double omega) {
        return imbalance * omega * omega;
    }
}
