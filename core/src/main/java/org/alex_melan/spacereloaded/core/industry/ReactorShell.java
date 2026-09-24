package org.alex_melan.spacereloaded.core.industry;

/**
 * Шаблон оболочки реголитового реактора 3×3×3 (004, FR-231, D36): все 26 внешних клеток —
 * огнеупорная футеровка, кроме клетки контроллера в центре одной боковой грани; центр куба —
 * воздух («ванна» расплава). Индекс клетки: x + 3·z + 9·y (0…2 по каждой оси, y — вверх).
 * Боковые центры граней: 10 (z=0), 12 (x=0), 14 (x=2), 16 (z=2); центр куба — 13.
 */
public final class ReactorShell {

    /** Класс клетки мира. */
    public enum Cell {
        REFRACTORY,
        AIR,
        CONTROLLER,
        OTHER
    }

    /**
     * Итог проверки.
     *
     * @param formed   шаблон выполнен
     * @param badIndex первая ошибочная клетка (−1 при успехе)
     * @param expected что должно быть в этой клетке
     */
    public record Result(boolean formed, int badIndex, Cell expected) {
        public static final Result OK = new Result(true, -1, null);
    }

    public static final int CENTER = 13;

    private static final int[] SIDE_CENTERS = {10, 12, 14, 16};

    private ReactorShell() {
    }

    public static int index(int x, int y, int z) {
        return x + 3 * z + 9 * y;
    }

    public static boolean isSideCenter(int index) {
        for (int side : SIDE_CENTERS) {
            if (side == index) {
                return true;
            }
        }
        return false;
    }

    /** Проверяет 27 клеток; контроллер должен стоять в центре боковой грани. */
    public static Result validate(Cell[] cells, int controllerIndex) {
        if (cells.length != 27) {
            throw new IllegalArgumentException("ожидается 27 клеток, получено " + cells.length);
        }
        if (!isSideCenter(controllerIndex)) {
            return new Result(false, controllerIndex, Cell.CONTROLLER);
        }
        for (int i = 0; i < 27; i++) {
            Cell expected = i == CENTER ? Cell.AIR : i == controllerIndex ? Cell.CONTROLLER : Cell.REFRACTORY;
            if (cells[i] != expected) {
                return new Result(false, i, expected);
            }
        }
        return Result.OK;
    }
}
