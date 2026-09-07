package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StagedPerformanceTest {

    private static final double G0 = PerformanceCalculator.G0;
    private static final String KEROLOX = "kerolox";

    /**
     * Δv двухступенчатого стека = сумма последовательных членов Циолковского:
     * ступень 1 несёт верхнюю как груз, ступень 2 стартует с массы остатка.
     */
    @Test
    void twoStageDeltaVIsSumOfSequentialTsiolkovskyTerms() {
        StageLayout layout = StageLayout.of(StageLayoutTest.twoStage());
        double[] propellant = layout.propellantByStage();
        StagedPerformance.StagedReport report = StagedPerformance.calculate(layout, propellant, 9.81);

        double m0 = 1220 + 2000 + 1250 + 2000;             // весь стек
        double dv1 = 300 * G0 * Math.log(m0 / (m0 - 2000));
        double m0b = 1250 + 2000;                          // без первой ступени
        double dv2 = 450 * G0 * Math.log(m0b / (m0b - 2000));

        assertEquals(2, report.stages().size());
        assertEquals(dv1, report.stages().get(0).deltaV(), dv1 * 0.01, "Δv ступени 1 (1%)");
        assertEquals(dv2, report.stages().get(1).deltaV(), dv2 * 0.01, "Δv ступени 2 (1%)");
        assertEquals(dv1 + dv2, report.totalDeltaV(), (dv1 + dv2) * 0.01, "сумма ступеней (1%)");

        // TWR ступени 2 — от массы оставшегося стека, ступени 1 — от полной
        assertEquals(120_000 / (m0 * 9.81), report.stages().get(0).twr(), 1e-9);
        assertEquals(32_000 / (m0b * 9.81), report.stages().get(1).twr(), 1e-9);
        assertEquals(m0, report.stages().get(0).stackMassKg(), 1e-9);
        assertEquals(m0b, report.stages().get(1).stackMassKg(), 1e-9);
        assertEquals(1220, report.stages().get(0).dryMassKg(), 1e-9);
        assertEquals(2000, report.stages().get(1).propellantKg(), 1e-9);

        // Двухступенчатый выигрывает у одноступенчатого той же стартовой массы
        List<PlacedPart> single = new ArrayList<>();
        single.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, 60_000, 300, KEROLOX)));
        single.add(PlacedPart.of(1, 0, 0, PartProperties.engine(400, 60_000, 300, KEROLOX)));
        single.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 4000, KEROLOX)));
        single.add(PlacedPart.of(0, 2, 0, PartProperties.hull(1370)));
        single.add(PlacedPart.of(0, 3, 0, PartProperties.command(500)));
        double singleDv = PerformanceCalculator.calculate(new RocketStructure(single), 9.81).deltaV();
        assertTrue(report.totalDeltaV() > singleDv,
                "ступени дают больше Δv: " + report.totalDeltaV() + " > " + singleDv);
    }

    /** Одноступенчатый стек: отчёт ступеней совпадает с обычным расчётом. */
    @Test
    void singleStageMatchesPerformanceCalculator() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 1, engine));
        parts.add(PlacedPart.of(1, 0, -1, engine));
        parts.add(PlacedPart.of(-1, 0, 1, engine));
        parts.add(PlacedPart.of(-1, 0, -1, engine));
        RocketStructure structure = new RocketStructure(parts);
        StageLayout layout = StageLayout.of(structure);

        RocketPerformance plain = PerformanceCalculator.calculate(structure, 9.81);
        StagedPerformance.StagedReport report = StagedPerformance.calculate(
                layout, new double[] {2000}, 9.81);

        assertEquals(1, report.stages().size());
        assertEquals(plain.deltaV(), report.totalDeltaV(), plain.deltaV() * 0.001, "0.1%");
        assertEquals(plain.twr(), report.stages().get(0).twr(), 1e-9);
        assertEquals(plain.warnings(), report.stages().get(0).warnings());
    }

    /** Ступень без двигателей (капсула-груз): Δv 0 и предупреждение STAGE_NO_ENGINE. */
    @Test
    void engineLessStageGetsWarningAndZeroDeltaV() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, 60_000, 300, KEROLOX)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.separator(120)));
        parts.add(PlacedPart.of(0, 3, 0, PartProperties.command(500)));
        StageLayout layout = StageLayout.of(new RocketStructure(parts));
        StagedPerformance.StagedReport report = StagedPerformance.calculate(
                layout, layout.propellantByStage(), 9.81);

        assertEquals(0, report.stages().get(1).deltaV(), 1e-9);
        assertTrue(report.stages().get(1).warnings().contains(PerformanceWarning.STAGE_NO_ENGINE));
        assertTrue(report.stages().get(0).deltaV() > 0);
        assertEquals(report.stages().get(0).deltaV(), report.totalDeltaV(), 1e-9);
    }
}
