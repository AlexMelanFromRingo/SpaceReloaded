package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Списание Δv перелёта по Циолковскому через ступени (003, FR-102). */
class TransferBurnTest {

    private static final double G0 = PerformanceCalculator.G0;
    private static final String KEROLOX = "kerolox";

    /** Одна ступень: двигатель 400 кг (Isp 300), бак 300 + 2000 кг, командный модуль 500. */
    private static RocketStructure singleStage() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, 60_000, 300, KEROLOX)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        return new RocketStructure(parts);
    }

    /** Две ступени: нижняя — 2 двигателя + бак 2000, разделитель; верхняя — как singleStage. */
    private static RocketStructure twoStage() {
        List<PlacedPart> parts = new ArrayList<>();
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(-1, 0, 0, engine));
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.hull(300)));
        parts.add(PlacedPart.of(1, 0, 0, engine));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.separator(120)));
        parts.add(PlacedPart.of(0, 3, 0, engine));
        parts.add(PlacedPart.filledTank(0, 4, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, 5, 0, PartProperties.command(500)));
        return new RocketStructure(parts);
    }

    @Test
    void singleStageBurnMatchesTsiolkovsky() {
        StageLayout layout = StageLayout.of(singleStage());
        double[] fuel = layout.propellantByStage();
        double m0 = 3200;
        double mDry = 1200;
        double deltaV = 1000;
        TransferBurn.Result result = TransferBurn.apply(layout, fuel, 0, deltaV);

        assertTrue(result.achieved());
        assertEquals(0, result.shortfallMs(), 1e-9);
        assertEquals(0, result.activeStage());
        double expected = m0 * Math.exp(-deltaV / (300 * G0)) - mDry;
        assertEquals(expected, result.propellantKg()[0], expected * 0.001, "остаток по Циолковскому");
        assertEquals(2000, fuel[0], 1e-9, "вход не мутирует");
    }

    @Test
    void zeroDeltaVLeavesPropellantUntouched() {
        StageLayout layout = StageLayout.of(singleStage());
        TransferBurn.Result result = TransferBurn.apply(layout, layout.propellantByStage(), 0, 0);
        assertTrue(result.achieved());
        assertEquals(2000, result.propellantKg()[0], 1e-9);
    }

    @Test
    void shortfallWhenStackCannotAfford() {
        StageLayout layout = StageLayout.of(singleStage());
        double available = 300 * G0 * Math.log(3200.0 / 1200.0); // ≈ 2886
        TransferBurn.Result result = TransferBurn.apply(layout, layout.propellantByStage(), 0, 4000);

        assertFalse(result.achieved());
        assertEquals(4000 - available, result.shortfallMs(), 1.0, "нехватка = Δv − доступное");
        assertEquals(0, result.propellantKg()[0], 1e-9, "ступень сожжена целиком");
    }

    /** Δv больше нижней ступени: она сгорает и отбрасывается, остаток списывается с верхней. */
    @Test
    void burnCrossesStageBoundary() {
        StageLayout layout = StageLayout.of(twoStage());
        assertEquals(2, layout.stageCount());
        double[] fuel = layout.propellantByStage();
        // Нижняя ступень: m0 = 800 + 300 + 300 + 2000 + 120 + (400 + 300 + 2000 + 500) = 6720, сухая часть 4720
        double lowerDeltaV = 300 * G0 * Math.log(6720.0 / 4720.0);
        double deltaV = lowerDeltaV + 500;
        TransferBurn.Result result = TransferBurn.apply(layout, fuel, 0, deltaV);

        assertTrue(result.achieved(), "верхняя ступень покрывает остаток");
        assertEquals(1, result.activeStage());
        assertEquals(1, result.stagesDropped());
        assertEquals(0, result.propellantKg()[0], 1e-9);
        double upperExpected = 3200 * Math.exp(-500 / (300 * G0)) - 1200;
        assertEquals(upperExpected, result.propellantKg()[1], upperExpected * 0.001);
    }

    /** Старт с верхней ступени: нижняя игнорируется. */
    @Test
    void startsFromGivenActiveStage() {
        StageLayout layout = StageLayout.of(twoStage());
        double[] fuel = {0, 2000};
        TransferBurn.Result result = TransferBurn.apply(layout, fuel, 1, 100);
        assertTrue(result.achieved());
        assertEquals(1, result.activeStage());
        assertEquals(0, result.stagesDropped());
        assertTrue(result.propellantKg()[1] < 2000 && result.propellantKg()[1] > 1850, "100 м/с ≈ 107 кг");
    }
}
