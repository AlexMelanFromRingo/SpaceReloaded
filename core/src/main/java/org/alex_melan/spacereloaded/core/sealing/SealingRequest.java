package org.alex_melan.spacereloaded.core.sealing;

/**
 * Параметры проверки герметичности.
 *
 * @param origin     стартовая позиция ({@code PackedPos})
 * @param maxRadius  лимит радиуса по метрике Чебышёва (куб поиска)
 * @param diagnostic true — полный обход со сбором всех точек утечки (для
 *                   подсветки игроку); false — ранний выход на первой утечке
 */
public record SealingRequest(long origin, int maxRadius, boolean diagnostic) {
    public SealingRequest {
        if (maxRadius < 1) {
            throw new IllegalArgumentException("maxRadius must be >= 1, got " + maxRadius);
        }
    }

    public static SealingRequest fast(long origin, int maxRadius) {
        return new SealingRequest(origin, maxRadius, false);
    }

    public static SealingRequest diagnostic(long origin, int maxRadius) {
        return new SealingRequest(origin, maxRadius, true);
    }
}
