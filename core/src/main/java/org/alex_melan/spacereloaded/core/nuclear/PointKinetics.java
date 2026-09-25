package org.alex_melan.spacereloaded.core.nuclear;

/**
 * Точечная кинетика реактора (008, FR-620…FR-624, D83) — одна группа запаздывающих нейтронов в
 * приближении мгновенного скачка; реактивность — в долларах (ρ/β, β = 0.0065 для U-235):
 * <ul>
 *   <li>при постоянной ρ < 1 $ мощность меняется экспоненциально со скоростью λρ/(1 − ρ);</li>
 *   <li>при скачке ρ₁ → ρ₂ мощность мгновенно умножается на (1 − ρ₁)/(1 − ρ₂);</li>
 *   <li>ρ ≥ 1 $ — мгновенная критичность: разгон на мгновенных нейтронах (мс), расплав зоны.</li>
 * </ul>
 * Стержень — интегральная S-кривая W·(h − sin 2πh/2π) (h — выведенная доля). Запас реактивности
 * топлива — линейно около критики: S·(m − m_c)/m_c (**оценка**: m_c = 18 кг U-235 с отражателем BeO,
 * S = 5 $). Остаточное тепло — Вэй–Вигнер. Выгорание: 1.05 г U-235 делится на МВт·сут
 * (200 МэВ на деление), с захватами расходуется 1.23 г.
 */
public final class PointKinetics {

    public static final double BETA = 0.0065;
    public static final double LAMBDA = 0.08;
    public static final double CRITICAL_MASS_KG = 18;
    public static final double EXCESS_SLOPE_DOLLARS = 5;
    public static final double G_CONSUMED_PER_MWD = 1.23;
    /** Мощность источника нейтронов (спонтанное деление, Am-Be), Вт — от неё стартует подкритичный реактор. */
    public static final double SOURCE_W = 1e-3;

    private PointKinetics() {
    }

    /** Интегральная эффективность стержня, $, при выведенной доле h ∈ [0, 1]. */
    public static double rodWorth(double h, double worthDollars) {
        double x = Math.max(0, Math.min(1, h));
        return worthDollars * (x - Math.sin(2 * Math.PI * x) / (2 * Math.PI));
    }

    /** Запас реактивности корзины, $, при массе U-235 (кг). */
    public static double fuelExcess(double u235Kg) {
        return EXCESS_SLOPE_DOLLARS * (u235Kg - CRITICAL_MASS_KG) / CRITICAL_MASS_KG;
    }

    /** Скорость изменения ln P, 1/с. */
    public static double rate(double rhoDollars) {
        return LAMBDA * rhoDollars / (1 - rhoDollars);
    }

    /** Период (e-кратного роста), с; бесконечность при ρ ≤ 0. */
    public static double period(double rhoDollars) {
        return rhoDollars <= 0 ? Double.POSITIVE_INFINITY : (1 - rhoDollars) / (LAMBDA * rhoDollars);
    }

    public static boolean promptCritical(double rhoDollars) {
        return rhoDollars >= 1;
    }

    /** Мгновенный скачок мощности при смене реактивности. */
    public static double jump(double power, double rhoFrom, double rhoTo) {
        return power * (1 - rhoFrom) / (1 - rhoTo);
    }

    /**
     * Шаг dt при постоянной ρ < 1 $ с источником нейтронов — точное решение dP/dt = r·P + q,
     * q = λS/β: подкритичный реактор стремится к размножению источника S(1 − ρ)/(−ρβ), около
     * критики (ρ → 0) — сколь угодно медленно, без скачка; надкритичный растёт по периоду.
     */
    public static double evolve(double power, double rhoDollars, double dt) {
        double r = rate(rhoDollars);
        double q = LAMBDA * SOURCE_W / BETA;
        double next = Math.abs(r * dt) < 1e-9 ? power + q * dt : (power + q / r) * Math.exp(r * dt) - q / r;
        return Math.max(SOURCE_W, next);
    }

    /** Доля остаточного тепловыделения через t с после останова после работы T с (Вэй–Вигнер). */
    public static double decayHeatFraction(double t, double operatingS) {
        double tt = Math.max(1, t);
        return Math.max(0, 0.066 * (Math.pow(tt, -0.2) - Math.pow(tt + operatingS, -0.2)));
    }

    /** Израсходовано U-235, кг, за энергию E (Дж). */
    public static double burnedKg(double joules) {
        return joules / 8.64e10 * G_CONSUMED_PER_MWD / 1000;
    }
}
