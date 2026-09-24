package org.alex_melan.spacereloaded.core.lifesupport;

/**
 * Поглотители CO₂ (007, FR-505, D73).
 * <ul>
 *   <li>LiOH: 2LiOH + CO₂ → Li₂CO₃ + H₂O — теоретически 44.01/(2·23.95) = 0.919 кг CO₂ на кг,
 *       практически ~0.75 (не весь гидроксид успевает прореагировать, **оценка** по картриджам
 *       Аполлона); воздух прокачивается вентилятором, удаление k = Q·η до исчерпания.</li>
 *   <li>Цеолит 5A (CDRA МКС): регенерируемый, тот же k = Q·η, десорбция ≈ 2 МДж на кг CO₂
 *       (**оценка** 1–3 МДж/кг), CO₂ уходит в трубу — в реактор Сабатье.</li>
 *   <li>Сабатье + электролиз: CO₂ + 4H₂ → CH₄ + 2H₂O, 2H₂O → 2H₂ + O₂: из 1 кг CO₂ — 0.818 кг воды →
 *       0.727 кг O₂ (при подводе водорода).</li>
 * </ul>
 */
public final class Scrubber {

    public static final double LIOH_THEORETICAL = 44.01 / (2 * 23.95);
    public static final double LIOH_PRACTICAL = 0.75;
    public static final double ZEOLITE_MJ_PER_KG = 2.0;
    public static final double SABATIER_O2_PER_CO2 = 2 * 18.015 / 44.01 * (31.999 / (2 * 18.015));
    /** Вентилятор поглотителя, м³/мин, и эффективность одного прохода. */
    public static final double FAN_M3_PER_MIN = 1.0;
    public static final double EFFICIENCY = 0.9;

    private Scrubber() {
    }

    /** Объёмное удаление CO₂ поглотителем, м³ в сутки. */
    public static double removalM3PerDay(double fanM3PerMin, double efficiency) {
        return fanM3PerMin * 1440 * efficiency;
    }
}
