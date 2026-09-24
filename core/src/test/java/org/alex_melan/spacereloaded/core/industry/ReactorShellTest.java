package org.alex_melan.spacereloaded.core.industry;

import org.alex_melan.spacereloaded.core.industry.ReactorShell.Cell;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Шаблон 3×3×3 реактора (004, FR-231): валидный куб и каждая из клеток-ошибок. */
class ReactorShellTest {

    private static Cell[] valid(int controller) {
        Cell[] cells = new Cell[27];
        java.util.Arrays.fill(cells, Cell.REFRACTORY);
        cells[ReactorShell.CENTER] = Cell.AIR;
        cells[controller] = Cell.CONTROLLER;
        return cells;
    }

    @Test
    void validForAllFourSides() {
        for (int side : new int[] {10, 12, 14, 16}) {
            assertTrue(ReactorShell.validate(valid(side), side).formed(), "сторона " + side);
        }
    }

    @Test
    void everyWrongCellIsReported() {
        for (int i = 0; i < 27; i++) {
            if (i == 10) {
                continue;
            }
            Cell[] cells = valid(10);
            cells[i] = i == ReactorShell.CENTER ? Cell.REFRACTORY : Cell.OTHER;
            ReactorShell.Result result = ReactorShell.validate(cells, 10);
            assertFalse(result.formed(), "клетка " + i);
            assertEquals(i, result.badIndex());
        }
    }

    @Test
    void airHoleInShellFails() {
        Cell[] cells = valid(12);
        cells[ReactorShell.index(2, 2, 2)] = Cell.AIR;
        ReactorShell.Result result = ReactorShell.validate(cells, 12);
        assertFalse(result.formed());
        assertEquals(Cell.REFRACTORY, result.expected());
    }

    @Test
    void controllerMustBeSideCenter() {
        assertFalse(ReactorShell.validate(valid(4), 4).formed()); // центр нижней грани
        assertFalse(ReactorShell.validate(valid(0), 0).formed()); // угол
    }
}
