package org.alex_melan.spacereloaded.core.nuclear;

/**
 * Автоматический регулятор реактора (008, D83): держит температуру зоны на уставке, управляя
 * <b>реактивностью</b>, а не стержнем напрямую, — как штатные системы управления. Желаемая
 * реактивность пропорциональна недогреву по прогнозу температуры на 150 с вперёд и ограничена
 * сверху +0.05 $ (ограничение скорости ввода реактивности: при быстром выводе мощность уходит в
 * разгон раньше, чем обратная связь успевает остановить нагрев, — перегрев выше предела сплава).
 * Стержень движется к этой реактивности со скоростью привода. Стержень ведётся как следящий привод (rodDelta). Проверено симуляцией с шагом
 * мода 0.5 с: пуск на 1073 K с перелётом ≈ 7 K, сброс половины нагрузки без изменения температуры.
 */
public final class ReactorRegulator {

    public static final double MAX_INSERTION_DOLLARS = 0.05;
    public static final double GAIN_DOLLARS_PER_K = 0.003;
    public static final double HORIZON_S = 150;
    public static final double DEADBAND_DOLLARS = 0.003;

    private ReactorRegulator() {
    }

    /** Желаемая реактивность, $: пропорционально недогреву по прогнозу, не выше предела ввода. */
    public static double wanted(double coreK, double dTdt, double setpointK) {
        double predicted = coreK + dTdt * HORIZON_S;
        return Math.max(-0.5, Math.min(MAX_INSERTION_DOLLARS, GAIN_DOLLARS_PER_K * (setpointK - predicted)));
    }

    /**
     * Ход стержня за шаг как у следящего привода: ровно столько, чтобы реактивность стала желаемой
     * (по местному наклону dρ/dh S-кривой), но не быстрее привода. Релейное «вывести на полный шаг»
     * при крупном шаге времени перескакивает цель на десятки центов и раскачивает зону.
     */
    public static double rodDelta(double rhoDollars, double coreK, double dTdt, double setpointK,
                                  double dRhoPerStroke, double maxStep) {
        double err = wanted(coreK, dTdt, setpointK) - rhoDollars;
        if (Math.abs(err) < DEADBAND_DOLLARS / 3) {
            return 0;
        }
        double step = err / Math.max(1e-3, dRhoPerStroke);
        return Math.max(-maxStep, Math.min(maxStep, step));
    }

    /** Направление привода: +1 — выводить, −1 — вводить, 0 — стоять. */
    public static int command(double rhoDollars, double coreK, double dTdt, double setpointK) {
        double want = wanted(coreK, dTdt, setpointK);
        if (rhoDollars < want - DEADBAND_DOLLARS) {
            return 1;
        }
        return rhoDollars > want + DEADBAND_DOLLARS ? -1 : 0;
    }
}
