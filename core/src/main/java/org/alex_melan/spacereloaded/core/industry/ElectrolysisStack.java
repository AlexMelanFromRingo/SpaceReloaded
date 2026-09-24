package org.alex_melan.spacereloaded.core.industry;

/**
 * Электролизный стек (005, FR-322, D57): по закону Фарадея скорость разложения воды в ячейке
 * ṅ = I/(z·F); ячейки фильтр-прессовой сборки соединены последовательно по току, поэтому
 * N ячеек при том же токе разлагают в N раз больше воды за то же время при N-кратном
 * напряжении — энергия на килограмм продукта постоянна. Контроллер (сам электролизёр) — ячейка
 * №0, итого N + 1.
 */
public final class ElectrolysisStack {

    private ElectrolysisStack() {
    }

    /** Сколько единиц сырья перерабатывает цикл: min(N + 1, доступно). */
    public static int unitsPerCycle(int cells, int available) {
        return Math.max(0, Math.min(cells + 1, available));
    }

    /** Энергия цикла: наследная энергия единицы × единицы (энергия/кг постоянна). */
    public static long energyPerCycle(long baseEnergy, int units) {
        return baseEnergy * units;
    }
}
