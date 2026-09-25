package org.alex_melan.spacereloaded.core.orbit;

/**
 * Гелиоцентрические эфемериды по кеплеровым элементам (009, US1): положение и скорость тела в
 * эклиптической системе J2000 на заданный день. Элементы — JPL «Approximate Positions of the
 * Planets» (Standish), средняя долгота растёт со средним движением n = √(μ☉/a³); вековые уходы
 * элементов (доли градуса за век) не учитываются — на десятилетия игры это меньше процента Δv.
 *
 * <p>Уравнение Кеплера M = E − e·sin E решается Ньютоном; положение в плоскости орбиты
 * поворачивается на аргумент перицентра ω = ϖ − Ω, наклон i и долготу узла Ω.
 */
public final class Ephemeris {

    public static final double MU_SUN = 1.32712440018e20;
    public static final double AU = 1.495978707e11;
    public static final double DAY_S = 86_400;

    /** Кеплеровы элементы: a (а.е.), e, i, Ω, ϖ (долгота перицентра), L₀ (средняя долгота на J2000), градусы. */
    public record Elements(double aAu, double e, double iDeg, double nodeDeg, double periDeg, double meanLonDeg) {
        /** Сидерический период, сут. */
        public double periodDays() {
            double a = aAu * AU;
            return 2 * Math.PI * Math.sqrt(a * a * a / MU_SUN) / DAY_S;
        }
    }

    /** Земля — барицентр Земля–Луна (Standish, табл. 1). */
    public static final Elements EARTH = new Elements(1.00000261, 0.01671123, -0.00001531, 0.0, 102.93768193, 100.46457166);
    /** Марс (Standish, табл. 1). */
    public static final Elements MARS = new Elements(1.52371034, 0.09339410, 1.84969142, 49.55953891, -23.94362959, -4.55343205);

    /** Церера (JPL SBDB; L₀ пересчитана к J2000 из M = 60.1° на 13.09.2023 средним движением 0.2141°/сут): тело «пояса астероидов». */
    public static final Elements CERES = new Elements(2.7675, 0.0758, 10.59, 80.3, 153.9, 160.7);

    /** Состояние: положение (м) и скорость (м/с) в эклиптике J2000. */
    public record State(double[] r, double[] v) {
    }

    private Ephemeris() {
    }

    /** Эксцентрическая аномалия из средней (Ньютон). */
    public static double eccentricAnomaly(double m, double e) {
        double x = e < 0.8 ? m : Math.PI;
        for (int k = 0; k < 50; k++) {
            double d = (x - e * Math.sin(x) - m) / (1 - e * Math.cos(x));
            x -= d;
            if (Math.abs(d) < 1e-13) {
                break;
            }
        }
        return x;
    }

    /** Состояние тела через t сут после J2000. */
    public static State state(Elements el, double days) {
        double a = el.aAu() * AU;
        double e = el.e();
        double n = Math.sqrt(MU_SUN / (a * a * a));                  // рад/с
        double m = Math.toRadians(el.meanLonDeg() - el.periDeg()) + n * days * DAY_S;
        m = Math.IEEEremainder(m, 2 * Math.PI);
        double ea = eccentricAnomaly(m, e);
        double cosE = Math.cos(ea), sinE = Math.sin(ea);
        double root = Math.sqrt(1 - e * e);
        double xp = a * (cosE - e), yp = a * root * sinE;
        double k = a * n / (1 - e * cosE);
        double vxp = -k * sinE, vyp = k * root * cosE;
        double w = Math.toRadians(el.periDeg() - el.nodeDeg());
        double node = Math.toRadians(el.nodeDeg()), inc = Math.toRadians(el.iDeg());
        return new State(rotate(xp, yp, w, inc, node), rotate(vxp, vyp, w, inc, node));
    }

    /** Поворот из плоскости орбиты в эклиптику: R_z(Ω)·R_x(i)·R_z(ω). */
    private static double[] rotate(double x, double y, double w, double inc, double node) {
        double cw = Math.cos(w), sw = Math.sin(w), ci = Math.cos(inc), si = Math.sin(inc);
        double cn = Math.cos(node), sn = Math.sin(node);
        double x1 = cw * x - sw * y, y1 = sw * x + cw * y;
        double y2 = ci * y1, z2 = si * y1;
        return new double[] {cn * x1 - sn * y2, sn * x1 + cn * y2, z2};
    }
}
