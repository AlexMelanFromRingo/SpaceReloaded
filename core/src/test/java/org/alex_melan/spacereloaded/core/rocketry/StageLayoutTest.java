package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StageLayoutTest {

    private static final String KEROLOX = "kerolox";
    private static final PartProperties ENGINE = PartProperties.engine(400, 60_000, 300, KEROLOX);
    private static final PartProperties TANK = PartProperties.tank(300, 2000, KEROLOX);
    private static final PartProperties SEPARATOR = PartProperties.separator(120);
    private static final PartProperties COMMAND = PartProperties.command(500);

    /**
     * Эталон: y=0 два двигателя, y=1 бак, y=2 разделитель | y=3 двигатель,
     * y=4 бак, y=5 командный модуль. Баки залиты полностью.
     */
    static RocketStructure twoStage() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, ENGINE));
        parts.add(PlacedPart.of(1, 0, 0, ENGINE));
        parts.add(PlacedPart.filledTank(0, 1, 0, TANK));
        parts.add(PlacedPart.of(0, 2, 0, SEPARATOR));
        parts.add(PlacedPart.of(0, 3, 0, PartProperties.engine(450, 32_000, 450, KEROLOX)));
        parts.add(PlacedPart.filledTank(0, 4, 0, TANK));
        parts.add(PlacedPart.of(0, 5, 0, COMMAND));
        return new RocketStructure(parts);
    }

    private static long command(RocketStructure s) {
        return s.parts().stream()
                .filter(p -> p.properties().role() == PartRole.COMMAND)
                .findFirst().orElseThrow().packedPos();
    }

    @Test
    void separatorsSplitStackIntoStagesBottomUp() {
        List<PlacedPart> parts = new ArrayList<>();
        for (int y = 0; y <= 7; y++) {
            parts.add(PlacedPart.of(0, y, 0, y == 2 || y == 5 ? SEPARATOR : y == 7 ? COMMAND : PartProperties.hull(100)));
        }
        StageLayout layout = StageLayout.of(new RocketStructure(parts));

        assertEquals(3, layout.stageCount());
        assertEquals(List.of(0, 1, 2), layout.stage(0).parts().stream().map(p -> PackedPos.unpackY(p.packedPos())).sorted().toList());
        assertEquals(List.of(3, 4, 5), layout.stage(1).parts().stream().map(p -> PackedPos.unpackY(p.packedPos())).sorted().toList());
        assertEquals(List.of(6, 7), layout.stage(2).parts().stream().map(p -> PackedPos.unpackY(p.packedPos())).sorted().toList());
        assertEquals(2, layout.stage(0).topY());
        assertEquals(5, layout.stage(1).topY());
        assertEquals(7, layout.stage(2).topY());
        assertEquals(0, layout.stage(0).index());
        // Каждая деталь ровно в одной ступени
        int total = layout.stages().stream().mapToInt(s -> s.parts().size()).sum();
        assertEquals(parts.size(), total);
    }

    @Test
    void separatorsOnSameLevelFormOnePlane() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, ENGINE));
        parts.add(PlacedPart.of(1, 0, 0, ENGINE));
        parts.add(PlacedPart.of(0, 1, 0, SEPARATOR));
        parts.add(PlacedPart.of(1, 1, 0, SEPARATOR)); // кольцо из двух блоков
        parts.add(PlacedPart.of(0, 2, 0, COMMAND));
        StageLayout layout = StageLayout.of(new RocketStructure(parts));

        assertEquals(2, layout.stageCount());
        assertEquals(4, layout.stage(0).parts().size());
        assertEquals(1, layout.stage(1).parts().size());
    }

    /** Без разделителей — одна ступень, активный вид тождествен структуре. */
    @Test
    void singleStageViewIsIdentity() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, ENGINE));
        parts.add(PlacedPart.filledTank(0, 1, 0, TANK));
        parts.add(PlacedPart.of(0, 2, 0, COMMAND));
        RocketStructure structure = new RocketStructure(parts);
        StageLayout layout = StageLayout.of(structure);

        assertEquals(1, layout.stageCount());
        assertTrue(layout.stage(0).hasEngines());
        RocketStructure view = layout.activeView(0, new double[] {2000});
        RocketPerformance original = PerformanceCalculator.calculate(structure, 9.81);
        RocketPerformance viewed = PerformanceCalculator.calculate(view, 9.81);
        assertEquals(original.totalMassKg(), viewed.totalMassKg(), 1e-9);
        assertEquals(original.totalThrustN(), viewed.totalThrustN(), 1e-9);
        assertEquals(original.deltaV(), viewed.deltaV(), 1e-9);
        assertEquals(structure.totalPropellantCapacityKg(), view.totalPropellantCapacityKg(), 1e-9);
        assertTrue(StageLayout.validate(structure, command(structure)).isEmpty());
    }

    @Test
    void validationRejectsSeparatorAtOrAboveCommandModule() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, ENGINE));
        parts.add(PlacedPart.of(0, 1, 0, COMMAND));
        parts.add(PlacedPart.of(1, 1, 0, SEPARATOR)); // тот же ярус, что и командный модуль
        RocketStructure structure = new RocketStructure(parts);

        Optional<StageLayout.StageError> error = StageLayout.validate(structure, command(structure));
        assertTrue(error.isPresent());
        assertEquals("stage_above_command", error.get().key());
        assertEquals(PackedPos.pack(1, 1, 0), error.get().packedPos());
    }

    @Test
    void validationRejectsEmptyLowerStage() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, SEPARATOR)); // под разделителем ничего нет
        parts.add(PlacedPart.of(0, 1, 0, ENGINE));
        parts.add(PlacedPart.of(0, 2, 0, COMMAND));
        RocketStructure structure = new RocketStructure(parts);

        Optional<StageLayout.StageError> error = StageLayout.validate(structure, command(structure));
        assertTrue(error.isPresent());
        assertEquals("stage_empty_below", error.get().key());
        assertEquals(PackedPos.pack(0, 0, 0), error.get().packedPos());
    }

    /** Деталь верхней ступени, висящая на нижней сбоку, после отсечения теряет связь. */
    @Test
    void validationRejectsUpperPartConnectedOnlyThroughLowerStage() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, ENGINE));
        parts.add(PlacedPart.of(0, 1, 0, SEPARATOR));
        parts.add(PlacedPart.of(1, 1, 0, PartProperties.hull(100))); // ярус разделителя — нижняя ступень
        parts.add(PlacedPart.of(2, 1, 0, PartProperties.hull(100))); // нижняя ступень, консоль вбок
        parts.add(PlacedPart.of(0, 2, 0, COMMAND));                   // командный модуль над разделителем
        parts.add(PlacedPart.of(2, 2, 0, PartProperties.hull(100))); // верхняя, держится только на (2,1,0)
        RocketStructure structure = new RocketStructure(parts);

        Optional<StageLayout.StageError> error = StageLayout.validate(structure, command(structure));
        assertTrue(error.isPresent());
        assertEquals("stage_disconnected", error.get().key());
        assertEquals(PackedPos.pack(2, 2, 0), error.get().packedPos());
    }

    /** Верхние ступени в активном виде — груз: двигатели без тяги, баки без ёмкости, но с массой топлива. */
    @Test
    void activeViewFreezesUpperStagesAsCargo() {
        StageLayout layout = StageLayout.of(twoStage());
        RocketStructure view = layout.activeView(0, new double[] {2000, 800});

        RocketPerformance perf = PerformanceCalculator.calculate(view, 9.81);
        assertEquals(120_000, perf.totalThrustN(), 1e-9, "тяга только от активной ступени");
        assertEquals(2000, view.totalPropellantCapacityKg(), 1e-9, "ёмкость только у активной ступени");
        // 1220 (сухая ступень 1) + 2000 + 450 (двигатель как корпус) + 1100 (бак + 800 кг) + 500
        assertEquals(5270, perf.totalMassKg(), 1e-9);
        assertEquals(7, view.parts().size(), "все детали остаются в стеке");
        assertFalse(view.parts().stream().anyMatch(p ->
                PackedPos.unpackY(p.packedPos()) > 2 && p.properties().role() == PartRole.ENGINE));

        // После сброса первой ступени вид содержит только вторую
        RocketStructure second = layout.activeView(1, new double[] {0, 800});
        assertEquals(3, second.parts().size());
        assertEquals(32_000, PerformanceCalculator.calculate(second, 9.81).totalThrustN(), 1e-9);
        assertEquals(800, second.totalPropellantKg(), 1e-9);
    }

    @Test
    void propellantHelpersFollowCapacities() {
        StageLayout layout = StageLayout.of(twoStage());
        double[] scanned = layout.propellantByStage();
        assertEquals(2000, scanned[0], 1e-9);
        assertEquals(2000, scanned[1], 1e-9);

        double[] spread = layout.distributeByCapacity(1000);
        assertEquals(500, spread[0], 1e-9);
        assertEquals(500, spread[1], 1e-9);
        assertEquals(3, layout.remainingAfter(0).parts().size(), "выше первой ступени — три детали");
        assertEquals(1220, layout.stage(0).dryMassKg(), 1e-9);
        assertEquals(2000, layout.stage(1).propellantCapacityKg(), 1e-9);
    }
}
