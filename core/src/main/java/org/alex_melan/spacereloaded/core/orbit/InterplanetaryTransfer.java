package org.alex_melan.spacereloaded.core.orbit;

/**
 * Цена межпланетного перелёта на дату отлёта (009, US1): гелиоцентрическая дуга Ламберта между
 * эфемеридными положениями тел, импульсы — по гиперболическому избытку v∞ на концах дуги
 * (сопряжённые конические сечения, как в 003). Отлёт с круговой парковочной орбиты — по Оберту
 * √(v∞² + 2μ/r) − √(μ/r); у малого тела (μ ≈ 0) — уравнивание скоростей |v∞|; прибытие к телу с
 * атмосферой — аэрозахват (бесплатен, допущение 003), к безатмосферному — тем же импульсом.
 * Время полёта выбирается минимизацией Δv (перебор и золотое сечение в [0.4, 1.6] полупериода
 * Гомана) — это нижняя кромка «свиной отбивной» на каждую дату отлёта.
 */
public final class InterplanetaryTransfer {

    /** Тело перелёта: элементы орбиты, μ (0 — малое тело), радиус парковочной орбиты, аэрозахват. */
    public record Body(Ephemeris.Elements orbit, double mu, double parkRadiusM, boolean aerocapture) {
    }

    /** Лучший вариант на дату: сумма, отлёт, прибытие (м/с), время полёта (сут). */
    public record Option(double totalMs, double departureMs, double arrivalMs, double tofDays) {
        public static final Option NONE = new Option(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 0, 0);
    }

    private InterplanetaryTransfer() {
    }

    /** Импульс ухода с парковочной орбиты (или уравнивания у малого тела) при избытке v∞. */
    public static double escapeBurn(Body body, double vInf) {
        if (body.mu() <= 0) {
            return vInf;
        }
        double r = body.parkRadiusM();
        return Math.sqrt(vInf * vInf + 2 * body.mu() / r) - Math.sqrt(body.mu() / r);
    }

    /** Полупериод орбиты Гомана между средними расстояниями тел, сут. */
    public static double hohmannDays(Body from, Body to) {
        double a = 0.5 * (from.orbit().aAu() + to.orbit().aAu()) * Ephemeris.AU;
        return Math.PI * Math.sqrt(a * a * a / Ephemeris.MU_SUN) / Ephemeris.DAY_S;
    }

    /** Δv при отлёте в день departDay (сут после J2000) с временем полёта tofDays. */
    public static Option cost(Body from, Body to, double departDay, double tofDays) {
        Ephemeris.State s1 = Ephemeris.state(from.orbit(), departDay);
        Ephemeris.State s2 = Ephemeris.state(to.orbit(), departDay + tofDays);
        Lambert.Solution arc = Lambert.solve(s1.r(), s2.r(), tofDays * Ephemeris.DAY_S, Ephemeris.MU_SUN);
        if (arc == null) {
            return Option.NONE;
        }
        double dep = escapeBurn(from, Lambert.distance(arc.v1(), s1.v()));
        double arr = to.aerocapture() ? 0 : escapeBurn(to, Lambert.distance(arc.v2(), s2.v()));
        if (!Double.isFinite(dep) || !Double.isFinite(arr)) {
            return Option.NONE;
        }
        return new Option(dep + arr, dep, arr, tofDays);
    }

    /** Лучшее время полёта для отлёта в departDay. */
    public static Option best(Body from, Body to, double departDay) {
        double th = hohmannDays(from, to);
        double lo = 0.4 * th, hi = 1.6 * th;
        int n = 40;
        Option best = Option.NONE;
        int bi = -1;
        for (int k = 0; k <= n; k++) {
            Option o = cost(from, to, departDay, lo + (hi - lo) * k / n);
            if (o.totalMs() < best.totalMs()) {
                best = o;
                bi = k;
            }
        }
        if (bi < 0) {
            return best;
        }
        // золотое сечение в соседних узлах
        double a = lo + (hi - lo) * Math.max(0, bi - 1) / n, b = lo + (hi - lo) * Math.min(n, bi + 1) / n;
        double g = (Math.sqrt(5) - 1) / 2;
        double c = b - g * (b - a), d = a + g * (b - a);
        Option oc = cost(from, to, departDay, c), od = cost(from, to, departDay, d);
        for (int k = 0; k < 40; k++) {
            if (oc.totalMs() < od.totalMs()) {
                b = d;
                d = c;
                od = oc;
                c = b - g * (b - a);
                oc = cost(from, to, departDay, c);
            } else {
                a = c;
                c = d;
                oc = od;
                d = a + g * (b - a);
                od = cost(from, to, departDay, d);
            }
        }
        Option refined = oc.totalMs() < od.totalMs() ? oc : od;
        return refined.totalMs() < best.totalMs() ? refined : best;
    }

    /** Синодический период пары, сут. */
    public static double synodicDays(Body a, Body b) {
        double pa = a.orbit().periodDays(), pb = b.orbit().periodDays();
        return 1 / Math.abs(1 / pa - 1 / pb);
    }

    /** Ближайший (не раньше from) день отлёта с минимумом Δv в пределах синодического периода; шаг сканирования — 2 сут. */
    public static double nextMinimumDay(Body from, Body to, double fromDay) {
        double span = synodicDays(from, to);
        double bestDay = fromDay, bestDv = Double.POSITIVE_INFINITY;
        for (double t = fromDay; t <= fromDay + span; t += 2) {
            double dv = best(from, to, t).totalMs();
            if (dv < bestDv) {
                bestDv = dv;
                bestDay = t;
            }
        }
        return bestDay;
    }
}
