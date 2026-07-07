package org.alex_melan.spacereloaded.core.sealing;

import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.voxel.ArrayVoxelGrid;
import org.alex_melan.spacereloaded.core.voxel.GasPermeability;
import org.junit.jupiter.api.Test;

import static org.alex_melan.spacereloaded.core.voxel.GasPermeability.BLOCKED;
import static org.alex_melan.spacereloaded.core.voxel.GasPermeability.OPEN;
import static org.alex_melan.spacereloaded.core.voxel.GasPermeability.VACUUM;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Эталонные сцены из конституции (принцип VII): замкнутая комната,
 * диагональная щель, лимит радиуса, дверь, производительность.
 */
class GasFloodFillTest {

    private static long at(int x, int y, int z) {
        return PackedPos.pack(x, y, z);
    }

    /** Оболочка 7×7×7 из BLOCKED вокруг полости 5×5×5. Вокруг — вакуум. */
    private static ArrayVoxelGrid.Builder sealedRoom() {
        return ArrayVoxelGrid.builder(0, 0, 0, 10, 10, 10)
                .fillAll(VACUUM)
                .fillBox(2, 2, 2, 8, 8, 8, BLOCKED)
                .fillBox(3, 3, 3, 7, 7, 7, OPEN);
    }

    @Test
    void sealedRoomIsSealed() {
        SealingResult result = GasFloodFill.analyze(sealedRoom().build(),
                SealingRequest.fast(at(5, 5, 5), 16));

        assertEquals(SealingStatus.SEALED, result.status());
        assertEquals(125, result.volume().size(), "полость 5×5×5");
        assertTrue(result.leakPoints().isEmpty());
        assertTrue(result.escapePoints().isEmpty());
    }

    /**
     * Главный тест осознанного дизайна: убираем угловой блок оболочки (8,8,8).
     * Внутренняя ячейка (7,7,7) касается его ТОЛЬКО диагональю {1,1,1} —
     * все 6 гранёвых соседей внутренних ячеек остаются стенами. 26-направленный
     * обход обязан найти утечку; 6-направленный счёл бы комнату герметичной.
     */
    @Test
    void diagonalCornerGapIsLeak() {
        ArrayVoxelGrid grid = sealedRoom()
                .set(8, 8, 8, VACUUM)
                .build();

        // Санити: у всех внутренних ячеек гранёвые соседи — не вакуум (дырка только диагональная)
        int[][] faces = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
        for (int x = 3; x <= 7; x++) {
            for (int y = 3; y <= 7; y++) {
                for (int z = 3; z <= 7; z++) {
                    for (int[] f : faces) {
                        GasPermeability p = grid.permeabilityAt(at(x + f[0], y + f[1], z + f[2]));
                        assertTrue(p == BLOCKED || p == OPEN,
                                "гранёвый сосед (" + (x + f[0]) + "," + (y + f[1]) + "," + (z + f[2]) + ") = " + p);
                    }
                }
            }
        }

        SealingResult result = GasFloodFill.analyze(grid, SealingRequest.fast(at(5, 5, 5), 16));

        assertEquals(SealingStatus.LEAK, result.status(), "26 направлений обязаны поймать диагональную щель");
        assertTrue(result.leakPoints().contains(at(8, 8, 8)), "точка утечки — вынутый угол");
    }

    @Test
    void openVolumeHitsRadiusLimit() {
        ArrayVoxelGrid grid = ArrayVoxelGrid.builder(0, 0, 0, 40, 40, 40)
                .fillAll(OPEN)
                .build();

        SealingResult result = GasFloodFill.analyze(grid, SealingRequest.fast(at(20, 20, 20), 5));

        assertEquals(SealingStatus.UNBOUNDED, result.status(),
                "выход за радиус — UNBOUNDED, а не LEAK: дыра не доказана");
        assertTrue(result.leakPoints().isEmpty());
        assertEquals(1, result.escapePoints().size(), "ранний выход — одна точка");
    }

    @Test
    void doorClosedSealsDoorOpenLeaks() {
        // Дверной проём в стене: ячейка (2,5,5) оболочки
        ArrayVoxelGrid closed = sealedRoom().set(2, 5, 5, BLOCKED).build();
        ArrayVoxelGrid open = sealedRoom().set(2, 5, 5, OPEN).build();

        assertEquals(SealingStatus.SEALED,
                GasFloodFill.analyze(closed, SealingRequest.fast(at(5, 5, 5), 16)).status());

        SealingResult leaked = GasFloodFill.analyze(open, SealingRequest.fast(at(5, 5, 5), 16));
        assertEquals(SealingStatus.LEAK, leaked.status(),
                "через открытую дверь заполнение выходит в вакуум");
    }

    @Test
    void invalidOriginReported() {
        SealingResult result = GasFloodFill.analyze(sealedRoom().build(),
                SealingRequest.fast(at(2, 2, 2), 16)); // стартуем внутри стены

        assertEquals(SealingStatus.INVALID_ORIGIN, result.status());
        assertEquals(0, result.blocksVisited());
    }

    @Test
    void diagnosticCollectsAllLeaksFastModeStopsAtFirst() {
        // Две независимые пробоины в противоположных стенах (гранёвые, для простоты)
        ArrayVoxelGrid grid = sealedRoom()
                .set(2, 5, 5, OPEN)
                .set(8, 5, 5, OPEN)
                .build();

        SealingResult fast = GasFloodFill.analyze(grid, SealingRequest.fast(at(5, 5, 5), 16));
        SealingResult diag = GasFloodFill.analyze(grid, SealingRequest.diagnostic(at(5, 5, 5), 16));

        assertEquals(SealingStatus.LEAK, fast.status());
        assertEquals(1, fast.leakPoints().size(), "ранний выход: первая утечка завершает поиск");

        assertEquals(SealingStatus.LEAK, diag.status());
        assertTrue(diag.leakPoints().size() >= 2, "диагностика собирает все точки утечки");
        assertTrue(diag.blocksVisited() >= fast.blocksVisited());
    }

    @Test
    void leakBeatsUnboundedInDiagnostics() {
        // Комната с дырой в вакуум, стоящая в большом открытом зале: диагностика
        // видит и утечку, и выход за радиус — приоритет у доказанной дыры.
        ArrayVoxelGrid grid = ArrayVoxelGrid.builder(-40, -40, -40, 40, 40, 40)
                .fillAll(OPEN)
                .fillBox(2, 2, 2, 8, 8, 8, BLOCKED)
                .fillBox(3, 3, 3, 7, 7, 7, OPEN)
                .set(8, 8, 8, VACUUM)
                .set(2, 5, 5, OPEN) // дверь в открытый зал
                .build();

        SealingResult result = GasFloodFill.analyze(grid,
                SealingRequest.diagnostic(at(5, 5, 5), 10));

        assertEquals(SealingStatus.LEAK, result.status());
        assertTrue(result.escapePoints().size() > 0, "выход за радиус тоже зафиксирован");
    }

    /** SC-002-прокси: полость ≥10 000 ячеек обрабатывается быстро. */
    @Test
    void tenThousandBlockCavityIsFast() {
        // Полость 22×22×22 = 10 648 ячеек
        ArrayVoxelGrid grid = ArrayVoxelGrid.builder(0, 0, 0, 25, 25, 25)
                .fillAll(VACUUM)
                .fillBox(1, 1, 1, 24, 24, 24, BLOCKED)
                .fillBox(2, 2, 2, 23, 23, 23, OPEN)
                .build();

        long start = System.nanoTime();
        SealingResult result = GasFloodFill.analyze(grid,
                SealingRequest.fast(at(12, 12, 12), 15));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(SealingStatus.SEALED, result.status());
        assertEquals(22 * 22 * 22, result.volume().size());
        assertTrue(elapsedMs < 200, "10к ячеек за " + elapsedMs + " мс (лимит 200)");
    }
}
