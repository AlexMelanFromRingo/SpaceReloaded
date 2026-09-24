package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Механика узла в его собственной системе (005, D51): линеаризованный источник τ ≈ a − b·ω_loc
 * (мотор: a = τ_st, b = τ_st/ω₀), кулоновская нагрузка c ≥ 0 (трение, станок — всегда против
 * вращения), момент инерции I.
 */
public record NodeLoad(double a, double b, double c, double inertia) {

    public static final NodeLoad NONE = new NodeLoad(0, 0, 0, 0);

    public NodeLoad {
        if (b < 0 || c < 0 || inertia < 0) {
            throw new IllegalArgumentException("b, c, I >= 0");
        }
    }

    /** Момент источника при локальной скорости. */
    public double sourceTorque(double omegaLocal) {
        return a - b * omegaLocal;
    }
}
