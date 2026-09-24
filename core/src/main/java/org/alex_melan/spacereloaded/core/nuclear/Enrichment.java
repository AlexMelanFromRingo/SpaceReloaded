package org.alex_melan.spacereloaded.core.nuclear;

/**
 * Обогащение урана каскадом газовых центрифуг (008, FR-631, D84). Отношение R = x/(1 − x) растёт
 * в α раз на ступень (α ≈ 1.3 — современная центрифуга, **оценка**): после N ступеней обогатительной
 * части R_p = R_f·α^N; N для заданного продукта — ln(R_p/R_f)/ln α. Материальный баланс F = P + W,
 * F·x_f = P·x_p + W·x_w; работа разделения — функция ценности V(x) = (2x − 1)·ln(x/(1 − x)).
 */
public final class Enrichment {

    public static final double NATURAL = 0.00711;
    public static final double TAILS = 0.0025;
    public static final double ALPHA = 1.3;

    private Enrichment() {
    }

    private static double ratio(double x) {
        return x / (1 - x);
    }

    public static double stages(double feed, double product, double alpha) {
        return Math.log(ratio(product) / ratio(feed)) / Math.log(alpha);
    }

    /** Обогащение продукта каскада из N центрифуг. */
    public static double product(int n, double feed, double alpha) {
        double r = ratio(feed) * Math.pow(alpha, n);
        return r / (1 + r);
    }

    /** Доля продукта от питания: P/F = (x_f − x_w)/(x_p − x_w). */
    public static double productPerFeed(double feed, double product, double tails) {
        return (feed - tails) / (product - tails);
    }

    public static double value(double x) {
        return (2 * x - 1) * Math.log(ratio(x));
    }

    /** Работа разделения (ЕРР, кг) на P кг продукта. */
    public static double swu(double productKg, double feed, double product, double tails) {
        double f = productKg / productPerFeed(feed, product, tails);
        double w = f - productKg;
        return productKg * value(product) + w * value(tails) - f * value(feed);
    }
}
