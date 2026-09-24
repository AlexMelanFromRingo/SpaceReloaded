package org.alex_melan.spacereloaded.core.worldgen;

/**
 * Укрытие (004, FR-241, D38): позиция без открытого неба под толщей непрозрачных блоков
 * не чувствует суточного размаха поверхности — температура равна стабильной температуре
 * укрытия тела (Луна: +17 °C на затенённом дне провалов, LRO Diviner; research-physics §9).
 * Упрощение: теплопроводность стен и время установления не моделируются.
 */
public final class ShelterRule {

    private ShelterRule() {
    }

    public static boolean isSheltered(boolean skyVisible, int opaqueAbove, int minOpaque) {
        return !skyVisible && opaqueAbove >= minOpaque;
    }
}
