package org.alex_melan.spacereloaded.core.nuclear;

/**
 * Автоматический регулятор реактора (008, D83): держит температуру зоны на уставке, управляя
 * <b>реактивностью</b>, а не стержнем напрямую, — как штатные системы управления. Желаемая
 * реактивность пропорциональна недогреву по прогнозу температуры на 150 с вперёд и ограничена
 * сверху +0.05 $ (ограничение скорости ввода реактивности: при быстром выводе мощность уходит в
 * разгон раньше, чем обратная связь успевает остановить нагрев, — перегрев выше предела сплава).
 * Стержень движется к этой реактивности со скоростью привода. Проверено симуляцией: пуск на 1073 K
 * с перелётом ≈ 60 K, сброс половины нагрузки без изменения температуры.
 */
public final class ReactorRegulator {

    public static final double MAX_INSERTION_DOLLARS = 0.05;
    public static final double GAIN_DOLLARS_PER_K = 0.001;
    public static final double HORIZON_S = 150;
    public static final double DEADBAND_DOLLARS = 0.003;

    private ReactorRegulator() {
    }

    /** Направление привода: +1 — выводить, −1 — вводить, 0 — стоять. */
    public static int command(double rhoDollars, double coreK, double dTdt, double setpointK) {
        double predicted = coreK + dTdt * HORIZON_S;
        double want = Math.max(-0.5, Math.min(MAX_INSERTION_DOLLARS, GAIN_DOLLARS_PER_K * (setpointK - predicted)));
        if (rhoDollars < want - DEADBAND_DOLLARS) {
            return 1;
        }
        return rhoDollars > want + DEADBAND_DOLLARS ? -1 : 0;
    }
}
