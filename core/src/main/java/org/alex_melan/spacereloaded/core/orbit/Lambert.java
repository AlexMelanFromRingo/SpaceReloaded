package org.alex_melan.spacereloaded.core.orbit;

/**
 * Задача Ламберта (009, US1): орбита через две точки r₁, r₂ за время Δt вокруг тела μ —
 * универсальные переменные (Bate–Mueller–White §5.3, Curtis Algorithm 5.2), прямой перелёт
 * менее чем за один оборот. Корень уравнения времени F(z) = 0 ищется бисекцией: F монотонна
 * по z, поэтому бисекция сходится всегда, в том числе у переходов около 180°, где Ньютон
 * расходится.
 */
public final class Lambert {

    /** Скорости в начале и в конце дуги, м/с. */
    public record Solution(double[] v1, double[] v2) {
    }

    private Lambert() {
    }

    /** Функции Штумпфа C(z), S(z). */
    static double stumpffC(double z) {
        if (z > 1e-8) {
            return (1 - Math.cos(Math.sqrt(z))) / z;
        }
        if (z < -1e-8) {
            return (Math.cosh(Math.sqrt(-z)) - 1) / (-z);
        }
        return 0.5 - z / 24 + z * z / 720;
    }

    static double stumpffS(double z) {
        if (z > 1e-8) {
            double s = Math.sqrt(z);
            return (s - Math.sin(s)) / (s * s * s);
        }
        if (z < -1e-8) {
            double s = Math.sqrt(-z);
            return (Math.sinh(s) - s) / (s * s * s);
        }
        return 1.0 / 6 - z / 120 + z * z / 5040;
    }

    /** Прямой (против часовой стрелки над эклиптикой) перелёт; null — решения нет (Δθ ≈ 0 или 360°). */
    public static Solution solve(double[] r1, double[] r2, double dtS, double mu) {
        double n1 = norm(r1), n2 = norm(r2);
        double[] c = cross(r1, r2);
        double cosT = dot(r1, r2) / (n1 * n2);
        double theta = Math.acos(Math.max(-1, Math.min(1, cosT)));
        if (c[2] < 0) {
            theta = 2 * Math.PI - theta;
        }
        double a = Math.sin(theta) * Math.sqrt(n1 * n2 / (1 - Math.cos(theta)));
        if (!Double.isFinite(a) || Math.abs(a) < 1e-9) {
            return null;
        }
        double lo = -4 * Math.PI * Math.PI, hi = 4 * Math.PI * Math.PI - 1e-9;
        // нижняя граница — там, где y(z) > 0 (при A < 0 y отрицательна для малых z)
        while (y(lo, n1, n2, a) < 0 && lo < hi) {
            lo += 0.1;
        }
        double target = Math.sqrt(mu) * dtS;
        double z = 0;
        for (int k = 0; k < 200; k++) {
            z = 0.5 * (lo + hi);
            double yz = y(z, n1, n2, a);
            double f = yz < 0 ? -1 : Math.pow(yz / stumpffC(z), 1.5) * stumpffS(z) + a * Math.sqrt(yz);
            if (yz >= 0 && f > target) {
                hi = z;
            } else {
                lo = z;
            }
            if (hi - lo < 1e-12) {
                break;
            }
        }
        double yz = y(z, n1, n2, a);
        if (yz <= 0) {
            return null;
        }
        double f = 1 - yz / n1;
        double g = a * Math.sqrt(yz / mu);
        double gDot = 1 - yz / n2;
        double[] v1 = new double[3], v2 = new double[3];
        for (int i = 0; i < 3; i++) {
            v1[i] = (r2[i] - f * r1[i]) / g;
            v2[i] = (gDot * r2[i] - r1[i]) / g;
        }
        return new Solution(v1, v2);
    }

    private static double y(double z, double n1, double n2, double a) {
        double c = stumpffC(z);
        return n1 + n2 + a * (z * stumpffS(z) - 1) / Math.sqrt(c);
    }

    static double norm(double[] v) {
        return Math.sqrt(dot(v, v));
    }

    static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    static double[] cross(double[] a, double[] b) {
        return new double[] {a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]};
    }

    static double distance(double[] a, double[] b) {
        double dx = a[0] - b[0], dy = a[1] - b[1], dz = a[2] - b[2];
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}
