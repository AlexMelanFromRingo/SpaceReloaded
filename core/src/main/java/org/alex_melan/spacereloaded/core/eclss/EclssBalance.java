package org.alex_melan.spacereloaded.core.eclss;

/**
 * Баланс масс стойки жизнеобеспечения (008, FR-610…FR-614, D82) по ECLSS МКС. Модули:
 * OGS — электролиз 2H₂O → 2H₂ + O₂ (1.125 кг воды и 0.125 кг H₂ на кг O₂; 5.36 кВт·ч/кг O₂ при
 * 1.6 В на ячейку); Sabatier — CO₂ + 4H₂ → CH₄ + 2H₂O, ограничен водородом OGS (на кг CO₂ нужно
 * 8/44 кг H₂: на человека H₂ хватает на 57 % CO₂); CDRA — удаление CO₂ из зоны (2 МДж/кг, как цеолит
 * 007); WRS — регенерация 93.5 % конденсата (≈ 0.1 кВт·ч/кг, **оценка** по VCD МКС). Питьевой цикл
 * экипажа в игре не ведётся, поэтому в воду стойки честно возвращается только <b>метаболическая</b>
 * вода — та, что образуется при окислении пищи (0.35 кг на человека в сутки, BVAD): остальная влага
 * выпита из того же контура.
 */
public final class EclssBalance {

    public static final double WATER_PER_O2 = 36.0 / 32.0;
    public static final double H2_PER_O2 = 4.0 / 32.0;
    public static final double H2_PER_CO2 = 8.0 / 44.0;
    public static final double WATER_PER_CO2 = 36.0 / 44.0;
    public static final double CH4_PER_CO2 = 16.0 / 44.0;
    public static final double OGS_KWH_PER_KG_O2 = 285.8e3 / 0.5 / 0.032 / 3.6e6 * 1.6 / 1.48;
    public static final double CDRA_KWH_PER_KG_CO2 = 2e6 / 3.6e6;
    public static final double WRS_KWH_PER_KG = 0.1;
    public static final double WRS_RECOVERY = 0.935;
    public static final double METABOLIC_WATER_PER_DAY = 0.35;

    private EclssBalance() {
    }

    /**
     * Баланс за сутки: water — вода в электролиз, makeup — подпитка извне, o2 — выработка,
     * co2Removed/co2Reduced — удалено из зоны / восстановлено в Сабатье, ch4 — метан, h2Left — H₂
     * без пары, energyKwh — энергия модулей.
     */
    public record Day(double water, double makeup, double o2, double co2Removed, double co2Reduced, double ch4,
                      double waterBack, double h2Left, double energyKwh) {
    }

    public static Day day(double o2Demand, double co2Load, double condensate, boolean ogs, boolean sabatier,
                          boolean cdra, boolean wrs) {
        double o2 = ogs ? o2Demand : 0;
        double water = o2 * WATER_PER_O2;
        double h2 = o2 * H2_PER_O2;
        double removed = cdra ? co2Load : 0;
        double reduced = sabatier && cdra ? Math.min(removed, h2 / H2_PER_CO2) : 0;
        double back = reduced * WATER_PER_CO2 + (wrs ? WRS_RECOVERY * condensate : 0);
        double energy = o2 * OGS_KWH_PER_KG_O2 + removed * CDRA_KWH_PER_KG_CO2
                + (wrs ? WRS_RECOVERY * condensate * WRS_KWH_PER_KG : 0);
        return new Day(water, Math.max(0, water - back), o2, removed, reduced, reduced * CH4_PER_CO2, back,
                h2 - reduced * H2_PER_CO2, energy);
    }
}
