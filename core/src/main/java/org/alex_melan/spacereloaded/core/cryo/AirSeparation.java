package org.alex_melan.spacereloaded.core.cryo;

/**
 * Криогенная ректификация воздуха (008, FR-640…FR-642, D85), двухколонная схема Линде. Бинарное
 * приближение N₂ / (O₂ + Ar) с относительной летучестью α = 4; колонна из N тарелок при полном
 * орошении делит разделение поровну выше и ниже ввода (Фенске): R_верх/R_воздух = R_воздух/R_низ =
 * α^(N/2). Аргон (α Ar/O₂ ≈ 1.5) отбирается при N ≥ 20. Сжатие до 6 бар: изотермически
 * RT·ln 6/M = 0.043 кВт·ч/кг, при изотермическом КПД 0.7 — 0.061 кВт·ч/кг воздуха; извлечение
 * O₂ и N₂ — 95 %, аргона — 80 % (**оценки** промышленных ВРУ).
 */
public final class AirSeparation {

    public static final double ALPHA = 4.0;
    public static final double N2 = 0.781;
    public static final double O2_MASS = 0.231;
    public static final double N2_MASS = 0.755;
    public static final double AR_MASS = 0.0129;
    public static final double KWH_PER_KG_AIR = 8.314 * 300 * Math.log(6) / 0.029 / 0.7 / 3.6e6;
    public static final double RECOVERY = 0.95;
    public static final double AR_RECOVERY = 0.8;
    public static final int ARGON_TRAYS = 20;

    private AirSeparation() {
    }

    /** Чистота азота вверху колонны, доля. */
    public static double nitrogenPurity(int trays) {
        double r = N2 / (1 - N2) * Math.pow(ALPHA, trays / 2.0);
        return r / (1 + r);
    }

    /** Чистота кислорода в испарителе, доля. */
    public static double oxygenPurity(int trays) {
        double r = N2 / (1 - N2) / Math.pow(ALPHA, trays / 2.0); // остаток азота к кислороду
        return 1 / (1 + r);
    }

    /** Минимум тарелок для заданной чистоты кислорода (Фенске). */
    public static int traysFor(double o2Purity) {
        double r = (1 - o2Purity) / o2Purity;
        return (int) Math.ceil(2 * Math.log(N2 / (1 - N2) / r) / Math.log(ALPHA));
    }

    /** Поток воздуха, кг/с, от мощности вала, Вт. */
    public static double airFlow(double shaftW) {
        return shaftW / (KWH_PER_KG_AIR * 3.6e6);
    }
}
