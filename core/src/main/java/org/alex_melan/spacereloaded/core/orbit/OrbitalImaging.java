package org.alex_melan.spacereloaded.core.orbit;

/**
 * Орбитальная съёмка (007, FR-524, D79): дифракционный предел Рэлея GSD = 1.22·λ·h/D; период
 * T = 2π√(a³/μ); полоса пушбрум-линейки w = N_pix·GSD; время покрытия экватора полярной орбитой
 * T_cov = 2πR/(w·n_орб), n_орб — витков в сутки.
 * <p>
 * Снимок масштаба k (пиксель карты = 2^k м): вариообъектив подбирает фокус так, что геометрический
 * пиксель равен 2^k м, а полоса w = N_pix·2^k; дифракция задаёт нижнюю границу — k меньше
 * log₂(GSD) даст размытый кадр, поэтому недоступен. Ожидание — доля u ∈ [0, 1) (детерминирована
 * заказом) от T_cov/N для N спутников, сжатая астрономическим коэффициентом k_астро = 130
 * (тем же, что окна перелётов 003: 780 сут синодического периода Марса = 6 игровых суток).
 */
public final class OrbitalImaging {

    public static final double VISIBLE_M = 550e-9;
    /** Пикселей в линейке сенсора (**оценка**). */
    public static final int LINE_PIXELS = 10_000;

    private OrbitalImaging() {
    }

    public static final double ASTRO_COMPRESSION = 130;
    public static final int MAX_SCALE = 4;

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

    /** Наименьший масштаб карты k, у которого пиксель 2^k м не мельче дифракционного GSD; -1 — даже k = 4 мельче. */
    public static int minScale(double gsdM) {
        for (int k = 0; k <= MAX_SCALE; k++) {
            if ((1 << k) >= gsdM) {
                return k;
            }
        }
        return -1;
    }

    /** Ожидание снимка масштаба k, игровые тики (24000 тиков = игровые сутки). */
    public static long waitTicks(double radiusM, double altitudeM, double mu, int scale, int satellites, double u) {
        double period = periodSeconds(radiusM, altitudeM, mu);
        double days = coverageDays(radiusM, LINE_PIXELS * (double) (1 << scale), period);
        return Math.round(u * days / Math.max(1, satellites) / ASTRO_COMPRESSION * 24000);
    }
}
