package org.alex_melan.spacereloaded.core.industry;

/**
 * Качество двигателя → характеристики (005, FR-333, D56). Качество деталей q ∈ [0, 1] задаёт
 * реальные КПД: полнота сгорания η_c* = 0.94 + 0.05·q (форсуночная головка), коэффициент тяги
 * сопла η_Cf = 0.96 + 0.03·q (контур и шероховатость), КПД турбонасоса η_tp = 0.60 + 0.15·q
 * (давление в камере → тяга). Табличные характеристики датапака соответствуют q = 0.8, поэтому
 * множители нормированы к нему: Isp ∝ η_c*·η_Cf, тяга ∝ η_tp·Isp (расход при том же давлении).
 * Итог: q = 1 → Isp +1.6 %, тяга +5.9 %; q = 0.5 → Isp −2.4 %, тяга −8.4 %.
 */
public final class EngineQuality {

    /** Качество, которому соответствуют табличные характеристики. */
    public static final double REFERENCE = 0.8;
    /** Уровень состояния блока по умолчанию (старые двигатели). */
    public static final int DEFAULT_LEVEL = 8;
    /** Уровень кустарного рецепта. */
    public static final int HANDMADE_LEVEL = 5;

    private EngineQuality() {
    }

    public static double combustionEfficiency(double q) {
        return 0.94 + 0.05 * q;
    }

    public static double nozzleEfficiency(double q) {
        return 0.96 + 0.03 * q;
    }

    public static double pumpEfficiency(double q) {
        return 0.60 + 0.15 * q;
    }

    public static double ispMultiplier(double q) {
        return combustionEfficiency(q) * nozzleEfficiency(q)
                / (combustionEfficiency(REFERENCE) * nozzleEfficiency(REFERENCE));
    }

    public static double thrustMultiplier(double q) {
        return pumpEfficiency(q) / pumpEfficiency(REFERENCE) * ispMultiplier(q);
    }

    /** Дискретный уровень 0…10 из качества. */
    public static int level(double q) {
        return (int) Math.round(Math.max(0, Math.min(1, q)) * 10);
    }

    public static double fromLevel(int level) {
        return Math.max(0, Math.min(10, level)) / 10.0;
    }
}
