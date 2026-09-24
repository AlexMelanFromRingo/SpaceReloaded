package org.alex_melan.spacereloaded.core.industry;

import java.util.Arrays;

/**
 * Раскладка рельса катапульты (004, FR-201): непрерывная прямая линия секций от передней
 * грани казённика. Вход — тиры клеток вдоль оси (0 — не катушка); рельс обрывается на
 * первом разрыве и не длиннее предела конфига.
 *
 * @param tiers            тиры секций рельса по порядку от казённика (1 или 2)
 * @param truncatedByLimit рельс упёрся в предел длины (дальше ещё есть катушки)
 */
public record RailLayout(int[] tiers, boolean truncatedByLimit) {

    public static final RailLayout EMPTY = new RailLayout(new int[0], false);

    /** Строит рельс из тиров клеток вдоль оси (cells[0] — клетка перед казёнником). */
    public static RailLayout fromCells(int[] cells, int maxSections) {
        int n = 0;
        while (n < cells.length && n < maxSections && cells[n] > 0) {
            n++;
        }
        boolean truncated = n == maxSections && n < cells.length && cells[n] > 0;
        return new RailLayout(Arrays.copyOf(cells, n), truncated);
    }

    public int length() {
        return tiers.length;
    }

    public int count(int tier) {
        int c = 0;
        for (int t : tiers) {
            if (t == tier) {
                c++;
            }
        }
        return c;
    }

    /** Ускорения секций, м/с², по ускорениям тиров в g (индекс = тир − 1). */
    public double[] accelerations(double... tierAccelG) {
        double[] out = new double[tiers.length];
        for (int i = 0; i < tiers.length; i++) {
            out[i] = tierAccelG[tiers[i] - 1] * MassDriverBallistics.G0;
        }
        return out;
    }

    /** Лучший тир рельса (для подсказки «не хватает N секций»); без секций — 1. */
    public int bestTier() {
        int best = 1;
        for (int t : tiers) {
            best = Math.max(best, t);
        }
        return best;
    }
}
