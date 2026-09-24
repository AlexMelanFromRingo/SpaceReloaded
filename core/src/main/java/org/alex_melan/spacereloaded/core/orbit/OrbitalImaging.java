package org.alex_melan.spacereloaded.core.orbit;

/**
 * Орбитальная съёмка (007, FR-524, D79): дифракционный предел Рэлея GSD = 1.22·λ·h/D; период
 * T = 2π√(a³/μ); полоса пушбрум-линейки w = N_pix·GSD; время покрытия экватора полярной орбитой
 * T_cov = 2πR/(w·n_орб), n_орб — витков в сутки.
 */
public final class OrbitalImaging {

    public static final double VISIBLE_M = 550e-9;
    /** Пикселей в линейке сенсора (**оценка**). */
    public static final int LINE_PIXELS = 10_000;

    private OrbitalImaging() {
    }

    public static double gsd(double wavelength, double altitudeM, double apertureM) {
        return 1.22 * wavelength * altitudeM / apertureM;
    }

    public static double periodSeconds(double radiusM, double altitudeM, double mu) {
        double a = radiusM + altitudeM;
        return 2 * Math.PI * Math.sqrt(a * a * a / mu);
    }

    /** Время покрытия экватора полосой w (м), сутки. */
    public static double coverageDays(double radiusM, double swathM, double periodS) {
        double orbitsPerDay = 86400 / periodS;
        return 2 * Math.PI * radiusM / (swathM * orbitsPerDay);
    }
}
