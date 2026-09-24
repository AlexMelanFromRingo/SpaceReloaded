package org.alex_melan.spacereloaded.core.multiblock;

import java.util.List;
import java.util.Optional;

/**
 * Шаблон мультиблока (005, FR-321, D55): фиксированные клетки + необязательный повторяемый
 * сегмент (ячейки стека, тарелки колонны) с шагом и пределами min…max. Координаты локальные:
 * +z — от лицевой грани ключа внутрь, +y — вверх, +x — вправо, если смотреть на лицо ключа.
 * Проверка возвращает первую неверную клетку — для сообщения молота и частицы-маркера.
 *
 * @param cells  фиксированные клетки (включая ключ в (0,0,0) — матчер ключа проверяет вызывающий)
 * @param repeat повторяемый сегмент
 */
public record MultiblockTemplate(List<Cell> cells, Optional<Repeat> repeat) {

    /** Клетка шаблона: смещение и матчер (id блока или «#тег»). */
    public record Cell(int x, int y, int z, String matcher) {
    }

    /** Повторяемый сегмент: клетки первого повтора, шаг, пределы, число для показа в руководстве. */
    public record Repeat(List<Cell> cells, int stepX, int stepY, int stepZ, int min, int max, int display) {
    }

    /** Мир глазами шаблона: совпадает ли клетка с матчером (в локальных координатах). */
    @FunctionalInterface
    public interface CellProbe {
        boolean matches(int x, int y, int z, String matcher);
    }

    /**
     * Итог.
     *
     * @param formed   шаблон выполнен
     * @param repeats  число повторов сегмента
     * @param bad      первая неверная клетка (или null)
     * @param expected что там ожидалось
     */
    public record Result(boolean formed, int repeats, int[] bad, String expected) {
    }

    public Result match(CellProbe probe) {
        for (Cell cell : cells) {
            if (!probe.matches(cell.x, cell.y, cell.z, cell.matcher)) {
                return new Result(false, 0, new int[] {cell.x, cell.y, cell.z}, cell.matcher);
            }
        }
        if (repeat.isEmpty()) {
            return new Result(true, 0, null, null);
        }
        Repeat r = repeat.get();
        int k = 0;
        int[] firstMiss = null;
        String missMatcher = null;
        while (k < r.max) {
            boolean all = true;
            for (Cell cell : r.cells) {
                int x = cell.x + k * r.stepX;
                int y = cell.y + k * r.stepY;
                int z = cell.z + k * r.stepZ;
                if (!probe.matches(x, y, z, cell.matcher)) {
                    all = false;
                    firstMiss = new int[] {x, y, z};
                    missMatcher = cell.matcher;
                    break;
                }
            }
            if (!all) {
                break;
            }
            k++;
        }
        if (k < r.min) {
            return new Result(false, k, firstMiss, missMatcher);
        }
        return new Result(true, k, null, null);
    }

    /**
     * Локальные (x, z) → мировое смещение по горизонтальному направлению лица ключа
     * (faceX, faceZ — единичный вектор наружу). Внутрь = −лицо; вправо = поворот «внутрь» на 90°.
     */
    public static int[] toWorld(int x, int y, int z, int faceX, int faceZ) {
        int inX = -faceX;
        int inZ = -faceZ;
        int rightX = -inZ;
        int rightZ = inX;
        return new int[] {x * rightX + z * inX, y, x * rightZ + z * inZ};
    }
}
