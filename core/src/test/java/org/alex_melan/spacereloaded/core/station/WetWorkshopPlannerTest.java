package org.alex_melan.spacereloaded.core.station;

import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.rocketry.PartProperties;
import org.alex_melan.spacereloaded.core.rocketry.PlacedPart;
import org.alex_melan.spacereloaded.core.rocketry.RocketStructure;
import org.alex_melan.spacereloaded.core.sealing.GasFloodFill;
import org.alex_melan.spacereloaded.core.sealing.SealingRequest;
import org.alex_melan.spacereloaded.core.sealing.SealingResult;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.core.voxel.ArrayVoxelGrid;
import org.alex_melan.spacereloaded.core.voxel.GasPermeability;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** План конверсии борта в модуль станции (003, FR-122, FR-123, FR-127, D26). */
class WetWorkshopPlannerTest {

    private static final String KEROLOX = "kerolox";

    /** Куб n×n×n баков с координатами 0…n−1; опционально слой двигателей снизу и командный модуль сверху. */
    private static RocketStructure tankCube(int n, boolean enginesBelow, boolean commandOnTop) {
        List<PlacedPart> parts = new ArrayList<>();
        int y0 = enginesBelow ? 1 : 0;
        for (int x = 0; x < n; x++) {
            for (int z = 0; z < n; z++) {
                if (enginesBelow) {
                    parts.add(PlacedPart.of(x, 0, z, PartProperties.engine(400, 60_000, 300, KEROLOX)));
                }
                for (int y = 0; y < n; y++) {
                    parts.add(PlacedPart.of(x, y0 + y, z, PartProperties.tank(300, 2000, KEROLOX)));
                }
            }
        }
        if (commandOnTop) {
            parts.add(PlacedPart.of(n / 2, y0 + n, n / 2, PartProperties.command(500)));
        }
        return new RocketStructure(parts);
    }

    @Test
    void cubeOfThreeHasSingleInteriorCell() {
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(tankCube(3, false, false));
        assertEquals(26, plan.shellCount());
        assertEquals(1, plan.interiorCount());
        assertEquals(0, plan.keepCount());
        assertEquals(WetWorkshopPlanner.Kind.INTERIOR, plan.kindAt(1, 1, 1));
        assertEquals(WetWorkshopPlanner.Kind.SHELL, plan.kindAt(0, 1, 1));
    }

    @Test
    void cubeOfFiveWithEnginesAndCommand() {
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(tankCube(5, true, true));
        assertEquals(98, plan.shellCount(), "5³ − 3³ = 98 клеток оболочки");
        assertEquals(27, plan.interiorCount());
        assertEquals(26, plan.keepCount(), "25 двигателей + командный модуль");
        assertEquals(WetWorkshopPlanner.Kind.KEEP, plan.kindAt(2, 0, 2));
        assertEquals(WetWorkshopPlanner.Kind.KEEP, plan.kindAt(2, 6, 2));
        assertEquals(WetWorkshopPlanner.Kind.INTERIOR, plan.kindAt(2, 3, 2));
        // Бак над двигателем и бак под командным модулем — стенки: чужие роли вне гермооболочки
        assertEquals(WetWorkshopPlanner.Kind.SHELL, plan.kindAt(2, 1, 2));
        assertEquals(WetWorkshopPlanner.Kind.SHELL, plan.kindAt(2, 5, 2));
    }

    @Test
    void singleColumnHasNoInterior() {
        List<PlacedPart> parts = new ArrayList<>();
        for (int y = 0; y < 5; y++) {
            parts.add(PlacedPart.of(0, y, 0, PartProperties.tank(300, 2000, KEROLOX)));
        }
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(new RocketStructure(parts));
        assertEquals(5, plan.shellCount());
        assertEquals(0, plan.interiorCount());
    }

    @Test
    void hullAndCargoConvertLikeTanksOtherRolesKeep() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.hull(300)));
        parts.add(PlacedPart.of(0, 1, 0, PartProperties.gyro(200, 1000)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.separator(120)));
        parts.add(PlacedPart.of(0, 3, 0, PartProperties.seat(100)));
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(new RocketStructure(parts));
        assertEquals(1, plan.shellCount());
        assertEquals(3, plan.keepCount());
    }

    @Test
    void hatchCellMustBeShell() {
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(tankCube(3, true, false));
        OptionalLong hatch = WetWorkshopPlanner.hatchCell(plan, 0, 2, 1);
        assertTrue(hatch.isPresent());
        assertEquals(PackedPos.pack(0, 2, 1), hatch.getAsLong());
        assertFalse(WetWorkshopPlanner.hatchCell(plan, 0, 0, 1).isPresent(), "двигатель — не стенка");
        assertFalse(WetWorkshopPlanner.hatchCell(plan, 5, 5, 5).isPresent(), "вне структуры");
    }

    /** Герметичность оболочки 5×5×5 с закрытым люком по штатному флудфиллу; открытый люк — утечка. */
    @Test
    void convertedShellIsAirtightWithHatchClosed() {
        WetWorkshopPlanner.Plan plan = WetWorkshopPlanner.plan(tankCube(5, true, true));
        long hatch = WetWorkshopPlanner.hatchCell(plan, 0, 3, 2).getAsLong();

        SealingResult closed = GasFloodFill.analyze(grid(plan, hatch, false),
                SealingRequest.fast(PackedPos.pack(2, 3, 2), 16));
        assertEquals(SealingStatus.SEALED, closed.status());
        assertEquals(27, closed.volume().size(), "внутренний объём 3×3×3");

        SealingResult open = GasFloodFill.analyze(grid(plan, hatch, true),
                SealingRequest.fast(PackedPos.pack(2, 3, 2), 16));
        assertFalse(open.isSealed(), "открытый люк — утечка в вакуум");
    }

    private static ArrayVoxelGrid grid(WetWorkshopPlanner.Plan plan, long hatch, boolean hatchOpen) {
        ArrayVoxelGrid.Builder builder = ArrayVoxelGrid.builder(-2, -2, -2, 8, 9, 8)
                .fillAll(GasPermeability.VACUUM);
        for (WetWorkshopPlanner.Cell cell : plan.cells()) {
            int x = PackedPos.unpackX(cell.packedPos());
            int y = PackedPos.unpackY(cell.packedPos());
            int z = PackedPos.unpackZ(cell.packedPos());
            // Сохранённые детали (двигатели, модуль) в игре не герметичны — как вакуум снаружи
            GasPermeability p = switch (cell.kind()) {
                case INTERIOR -> GasPermeability.OPEN;
                case SHELL -> GasPermeability.BLOCKED;
                case KEEP -> GasPermeability.VACUUM;
            };
            if (cell.packedPos() == hatch) {
                p = hatchOpen ? GasPermeability.OPEN : GasPermeability.BLOCKED;
            }
            builder.set(x, y, z, p);
        }
        return builder.build();
    }
}
