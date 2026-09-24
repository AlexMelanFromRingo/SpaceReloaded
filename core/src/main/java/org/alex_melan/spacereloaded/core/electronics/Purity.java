package org.alex_melan.spacereloaded.core.electronics;

/**
 * Чистота кремния в «девятках» N9 = −log10(доля примесей) (006, FR-411/FR-413, D61).
 * Ректификация трихлорсилана: примеси BCl₃ и PCl₃ кипят далеко от SiHCl₃ (α_эфф ≈ 2.5), и по
 * Фенске каждая теоретическая тарелка делит примесь на α → N9 растёт на K·log10 α ≈ 0.4·K за
 * проход. Технический кремний — 2N; потолок процесса — 11N. Солнечному кремнию хватает 6N,
 * процессору — 9N.
 */
public final class Purity {

    public static final double METALLURGICAL = 2.0;
    public static final double CEILING = 11.0;
    /** log10 α_эфф ≈ log10 2.5. */
    public static final double PER_TRAY = 0.4;
    public static final double SOLAR_GRADE = 6.0;
    public static final double ELECTRONIC_GRADE = 9.0;

    private Purity() {
    }

    /** Чистота после одного прохода колонны из K тарелок. */
    public static double afterColumn(double n9, int trays) {
        return Math.min(CEILING, n9 + PER_TRAY * Math.max(0, trays));
    }

    /** Доля примесей 10^(−N9). */
    public static double impurityFraction(double n9) {
        return Math.pow(10, -n9);
    }

    /** Чистота по доле примесей. */
    public static double fromImpurity(double fraction) {
        return fraction <= 0 ? CEILING : Math.min(CEILING, -Math.log10(fraction));
    }

    /**
     * Смешение партий: масса примесей сохраняется, N9 = −log10((m₁·10^−N₁ + m₂·10^−N₂)/(m₁ + m₂)).
     * Грязная партия портит чистую непропорционально — одна «двойка» к ведру «девяток» даёт ~3.3N.
     */
    public static double blend(double n1, double m1, double n2, double m2) {
        double mass = m1 + m2;
        if (mass <= 0) {
            return Math.min(n1, n2);
        }
        return fromImpurity((m1 * impurityFraction(n1) + m2 * impurityFraction(n2)) / mass);
    }
}
