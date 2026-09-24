package org.alex_melan.spacereloaded.core.multiblock;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiblockTemplateTest {

    private static final MultiblockTemplate STACK = new MultiblockTemplate(List.of(),
            Optional.of(new MultiblockTemplate.Repeat(List.of(new MultiblockTemplate.Cell(0, 0, 1, "cell")),
                    0, 0, 1, 1, 15, 4)));

    private static MultiblockTemplate.CellProbe world(Map<String, String> blocks) {
        return (x, y, z, m) -> m.equals(blocks.get(x + "," + y + "," + z));
    }

    @Test
    void repeatCountsConsecutiveCells() {
        Map<String, String> w = new HashMap<>();
        for (int z = 1; z <= 7; z++) {
            w.put("0,0," + z, "cell");
        }
        MultiblockTemplate.Result r = STACK.match(world(w));
        assertTrue(r.formed());
        assertEquals(7, r.repeats());
    }

    @Test
    void belowMinimumReportsFirstMissingCell() {
        MultiblockTemplate column = new MultiblockTemplate(List.of(),
                Optional.of(new MultiblockTemplate.Repeat(List.of(new MultiblockTemplate.Cell(0, 1, 0, "tray")),
                        0, 1, 0, 4, 16, 8)));
        Map<String, String> w = new HashMap<>();
        w.put("0,1,0", "tray");
        w.put("0,2,0", "tray");
        MultiblockTemplate.Result r = column.match(world(w));
        assertFalse(r.formed());
        assertArrayEquals(new int[] {0, 3, 0}, r.bad());
        assertEquals("tray", r.expected());
    }

    @Test
    void fixedCellError() {
        MultiblockTemplate t = new MultiblockTemplate(List.of(new MultiblockTemplate.Cell(1, 0, 0, "a"),
                new MultiblockTemplate.Cell(-1, 0, 0, "b")), Optional.empty());
        Map<String, String> w = new HashMap<>();
        w.put("1,0,0", "a");
        MultiblockTemplate.Result r = t.match(world(w));
        assertFalse(r.formed());
        assertArrayEquals(new int[] {-1, 0, 0}, r.bad());
    }

    @Test
    void rotationKeepsInwardDirection() {
        // Лицо на север (0, −1): «внутрь» — на юг (+z мира)
        assertArrayEquals(new int[] {0, 0, 3}, MultiblockTemplate.toWorld(0, 0, 3, 0, -1));
        // Лицо на восток (+1, 0): внутрь — на запад (−x)
        assertArrayEquals(new int[] {-3, 0, 0}, MultiblockTemplate.toWorld(0, 0, 3, 1, 0));
        // Правая рука при лице на север — запад или восток, но перпендикулярно внутреннему
        int[] right = MultiblockTemplate.toWorld(1, 0, 0, 0, -1);
        assertEquals(0, right[2]);
        assertEquals(1, Math.abs(right[0]));
    }
}
