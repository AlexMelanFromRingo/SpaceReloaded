package org.alex_melan.spacereloaded.core.ballistics;

/**
 * Решение на выстрел орбитального орудия (Полёт 2.0, FR-090, D16): режим
 * наведения и случайное смещение точки удара относительно метки.
 *
 * <p>Наводимый лом (есть спутниковое покрытие целевого измерения) ложится в
 * круг малого радиуса; ненаводимый — в круг рассеивания. Смещение равномерно
 * по ПЛОЩАДИ круга: r = R·√u₁, φ = 2π·u₂ (у равномерного по радиусу центр был бы
 * перегружен). «Разброс до R блоков» — честное описание для игрока: 100% ударов
 * в круге, среднее смещение 2R/3.
 *
 * @param guided       наводимый ли лом
 * @param spreadRadius радиус круга рассеивания текущего режима, блоки
 * @param offsetX      смещение точки удара по X, блоки
 * @param offsetZ      смещение точки удара по Z, блоки
 */
public record StrikeSolution(boolean guided, double spreadRadius, double offsetX, double offsetZ) {

    /**
     * @param u1             случайное число [0,1) — радиус
     * @param u2             случайное число [0,1) — угол
     * @param guided         есть ли спутниковое покрытие цели
     * @param guidedRadius   радиус рассеивания наводимого лома, блоки
     * @param unguidedRadius радиус рассеивания без покрытия, блоки
     */
    public static StrikeSolution solve(double u1, double u2, boolean guided,
                                       double guidedRadius, double unguidedRadius) {
        double radius = Math.max(0, guided ? guidedRadius : unguidedRadius);
        double r = radius * Math.sqrt(Math.clamp(u1, 0, 1));
        double phi = 2 * Math.PI * u2;
        return new StrikeSolution(guided, radius, r * Math.cos(phi), r * Math.sin(phi));
    }

    /** Расстояние от метки до точки удара, блоки. */
    public double offsetDistance() {
        return Math.hypot(offsetX, offsetZ);
    }
}
