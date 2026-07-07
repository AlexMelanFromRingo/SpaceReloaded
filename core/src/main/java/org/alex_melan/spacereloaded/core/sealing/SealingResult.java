package org.alex_melan.spacereloaded.core.sealing;

import org.alex_melan.spacereloaded.core.geometry.LongHashSet;

/**
 * Результат проверки герметичности.
 *
 * @param status        итоговый статус
 * @param volume        посещённые проницаемые ячейки (внутренний объём);
 *                      при раннем выходе — частичный обход
 * @param leakPoints    ячейки вакуума, которых коснулось заполнение (LEAK);
 *                      при diagnostic=false — не более одной
 * @param escapePoints  ячейки за радиусом, в которые «вытек» поиск (UNBOUNDED);
 *                      при diagnostic=false — не более одной
 * @param blocksVisited число обработанных ячеек (метрика стоимости)
 * @param elapsedNanos  длительность расчёта
 */
public record SealingResult(
        SealingStatus status,
        LongHashSet volume,
        LongHashSet leakPoints,
        LongHashSet escapePoints,
        int blocksVisited,
        long elapsedNanos
) {
    public boolean isSealed() {
        return status == SealingStatus.SEALED;
    }
}
