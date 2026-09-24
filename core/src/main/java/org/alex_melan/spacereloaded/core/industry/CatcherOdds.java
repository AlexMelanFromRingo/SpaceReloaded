package org.alex_melan.spacereloaded.core.industry;

import java.util.SplittableRandom;

/**
 * Ловушка масс (004, FR-222/FR-223, D35): радиус захвата от площади сетки и гауссово
 * рассеивание прибытия. Модель точности — та же, что у наведения орбитальной пушки
 * (спутниковое покрытие = навигация): смещение — двумерная нормаль N(0, σ²) по каждой оси,
 * поэтому |смещение| распределено по Рэлею и P(|d| ≤ r) = 1 − exp(−r²/2σ²).
 */
public final class CatcherOdds {

    private CatcherOdds() {
    }

    /**
     * Радиус захвата: r = min(r₀ + k·√n, r_max). √n — эквивалент радиуса круга той же площади,
     * что сетка из n клеток (r = √(n/π) ≈ 0.56·√n; k по умолчанию 0.6 с учётом краёв клеток).
     */
    public static double captureRadius(int netBlocks, double baseRadius, double perSqrtNet, double maxRadius) {
        return Math.min(baseRadius + perSqrtNet * Math.sqrt(Math.max(0, netBlocks)), maxRadius);
    }

    /** Вероятность приёма при радиусе захвата r и σ рассеивания: 1 − exp(−r²/2σ²). */
    public static double captureProbability(double radius, double sigma) {
        return 1.0 - Math.exp(-radius * radius / (2 * sigma * sigma));
    }

    /**
     * Детерминированное смещение прибытия по зерну выстрела (Бокс–Мюллер): {dx, dz}, блоки.
     * Зерно фиксируется в момент выстрела — перезапуск сервера не меняет исход.
     */
    public static double[] sampleOffset(long seed, double sigma) {
        SplittableRandom random = new SplittableRandom(seed);
        double u1 = Math.max(Double.MIN_VALUE, random.nextDouble());
        double u2 = random.nextDouble();
        double mag = sigma * Math.sqrt(-2 * Math.log(u1));
        return new double[] {mag * Math.cos(2 * Math.PI * u2), mag * Math.sin(2 * Math.PI * u2)};
    }
}
