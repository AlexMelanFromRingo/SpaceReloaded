package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Механика узла в его собственной системе (005, D51): линеаризованный источник τ ≈ a − b·ω_loc
 * (мотор: a = τ_st, b = τ_st/ω₀), кулоновская нагрузка c ≥ 0 (трение, станок — всегда против
 * вращения), момент инерции I; {@code breakaway} — добавка трения покоя подшипника (держит сеть с места).
 */
public record NodeLoad(double a, double b, double c, double inertia, double breakaway) {

    public static final NodeLoad NONE = new NodeLoad(0, 0, 0, 0);

    public NodeLoad {
        if (b < 0 || c < 0 || inertia < 0 || breakaway < 0) {
            throw new IllegalArgumentException("b, c, I, breakaway >= 0");
        }
    }

    /** Без добавки трения покоя (011: {@code breakaway} — превышение пускового момента подшипника над рабочим). */
    public NodeLoad(double a, double b, double c, double inertia) {
        this(a, b, c, inertia, 0);
    }

    /** Момент источника при локальной скорости. */
    public double sourceTorque(double omegaLocal) {
        return a - b * omegaLocal;
    }
}
