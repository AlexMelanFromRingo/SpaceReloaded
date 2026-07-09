package org.alex_melan.spacereloaded.core.sealing;

/**
 * Параметры проверки герметичности.
 *
 * @param origin        стартовая позиция ({@code PackedPos})
 * @param maxRadius     лимит радиуса по метрике Чебышёва (куб поиска)
 * @param diagnostic    true — полный обход со сбором всех точек утечки (для
 *                      подсветки игроку); false — ранний выход на первой утечке
 * @param diagonalLeaks true — 26 направлений: газ просачивается через
 *                      диагональную щель, угол обязан быть заложен (замысел мода);
 *                      false — классические 6 направлений, как в соседних модах
 */
public record SealingRequest(long origin, int maxRadius, boolean diagnostic, boolean diagonalLeaks) {
    public SealingRequest {
        if (maxRadius < 1) {
            throw new IllegalArgumentException("maxRadius must be >= 1, got " + maxRadius);
        }
    }

    public static SealingRequest fast(long origin, int maxRadius) {
        return fast(origin, maxRadius, true);
    }

    public static SealingRequest fast(long origin, int maxRadius, boolean diagonalLeaks) {
        return new SealingRequest(origin, maxRadius, false, diagonalLeaks);
    }

    public static SealingRequest diagnostic(long origin, int maxRadius) {
        return diagnostic(origin, maxRadius, true);
    }

    public static SealingRequest diagnostic(long origin, int maxRadius, boolean diagonalLeaks) {
        return new SealingRequest(origin, maxRadius, true, diagonalLeaks);
    }
}
