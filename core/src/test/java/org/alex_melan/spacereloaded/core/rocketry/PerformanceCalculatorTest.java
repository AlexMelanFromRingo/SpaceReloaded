package org.alex_melan.spacereloaded.core.rocketry;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PerformanceCalculatorTest {

    private static final double G0 = PerformanceCalculator.G0;
    private static final String KEROLOX = "kerolox";

    /**
     * Симметричная эталонная ракета: командный модуль сверху, бак в центре,
     * 4 двигателя по углам нижнего яруса.
     */
    private static List<PlacedPart> symmetricRocketParts() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 1, engine));
        parts.add(PlacedPart.of(1, 0, -1, engine));
        parts.add(PlacedPart.of(-1, 0, 1, engine));
        parts.add(PlacedPart.of(-1, 0, -1, engine));
        return parts;
    }

    @Test
    void deltaVMatchesTsiolkovskyAnalytically() {
        RocketStructure structure = new RocketStructure(symmetricRocketParts());
        RocketPerformance perf = PerformanceCalculator.calculate(structure, 9.81);

        // m0 = 500+300+4·400+2000 = 4400 кг; m1 = 2400 кг; Isp одинаковый у всех = 300 с
        double expected = 300 * G0 * Math.log(4400.0 / 2400.0);
        assertEquals(expected, perf.deltaV(), expected * 0.01, "Δv по Циолковскому в пределах 1%");
        assertEquals(4400, perf.totalMassKg(), 1e-9);
        assertEquals(2000, perf.propellantMassKg(), 1e-9);
        assertEquals(300, perf.effectiveIspSec(), 1e-9, "одинаковые Isp → Isp_eff равен им");
    }

    @Test
    void effectiveIspIsFlowWeightedForMixedEngines() {
        // Два двигателя одинаковой тяги F, Isp 200 и 400:
        // ṁ = F/(200·g0) + F/(400·g0); Isp_eff = 2F/(g0·ṁ) = 2/(1/200+1/400) = 266.67
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 1, 0, PartProperties.command(100)));
        parts.add(PlacedPart.filledTank(0, 0, 0, PartProperties.tank(100, 500, KEROLOX)));
        parts.add(PlacedPart.of(1, 0, 0, PartProperties.engine(100, 10_000, 200, KEROLOX)));
        parts.add(PlacedPart.of(-1, 0, 0, PartProperties.engine(100, 10_000, 400, KEROLOX)));

        RocketPerformance perf = PerformanceCalculator.calculate(new RocketStructure(parts), 9.81);
        assertEquals(2.0 / (1.0 / 200 + 1.0 / 400), perf.effectiveIspSec(), 1e-6);
    }

    @Test
    void centerOfMassAndThrustForSymmetricRocket() {
        RocketStructure structure = new RocketStructure(symmetricRocketParts());
        RocketPerformance perf = PerformanceCalculator.calculate(structure, 9.81);

        // Симметрия по X и Z: оба центра на оси (x=0.5, z=0.5 — центры блоков колонки x=z=0
        // и симметричные двигатели вокруг неё)
        assertEquals(0.5, perf.centerOfMass().x(), 1e-9);
        assertEquals(0.5, perf.centerOfMass().z(), 1e-9);
        assertEquals(0.5, perf.centerOfThrust().x(), 1e-9);
        assertEquals(0.5, perf.centerOfThrust().z(), 1e-9);
        assertEquals(0.5, perf.centerOfThrust().y(), 1e-9, "двигатели на ярусе y=0");
        assertFalse(perf.warnings().contains(PerformanceWarning.ASYMMETRIC_THRUST));
    }

    @Test
    void removedEngineTriggersAsymmetryWarning() {
        List<PlacedPart> parts = symmetricRocketParts();
        parts.removeLast(); // снимаем двигатель (-1, 0, -1)

        RocketPerformance perf = PerformanceCalculator.calculate(new RocketStructure(parts), 9.81);

        assertTrue(perf.warnings().contains(PerformanceWarning.ASYMMETRIC_THRUST),
                "снятый двигатель смещает центр тяги вбок");
        assertTrue(perf.centerOfThrust().horizontalDistanceTo(perf.centerOfMass())
                > PerformanceCalculator.ASYMMETRY_THRESHOLD);
    }

    @Test
    void lowThrustTriggersTwrWarning() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 1, 0, PartProperties.command(5000)));
        parts.add(PlacedPart.filledTank(0, 0, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, -1, 0, PartProperties.engine(400, 10_000, 300, KEROLOX)));

        RocketPerformance perf = PerformanceCalculator.calculate(new RocketStructure(parts), 9.81);

        assertTrue(perf.twr() < 1);
        assertTrue(perf.warnings().contains(PerformanceWarning.TWR_BELOW_ONE));
    }

    @Test
    void missingPartsProduceWarnings() {
        // Только корпус: ни командного модуля, ни двигателей
        RocketPerformance hullOnly = PerformanceCalculator.calculate(
                new RocketStructure(List.of(PlacedPart.of(0, 0, 0, PartProperties.hull(100)))), 9.81);
        assertTrue(hullOnly.warnings().contains(PerformanceWarning.NO_COMMAND_MODULE));
        assertTrue(hullOnly.warnings().contains(PerformanceWarning.NO_ENGINE));

        // Двигатель на водороде, бак с керосином: топливо не подходит
        List<PlacedPart> mismatch = new ArrayList<>();
        mismatch.add(PlacedPart.of(0, 1, 0, PartProperties.command(100)));
        mismatch.add(PlacedPart.filledTank(0, 0, 0, PartProperties.tank(100, 500, KEROLOX)));
        mismatch.add(PlacedPart.of(0, -1, 0, PartProperties.engine(100, 50_000, 450, "hydrolox")));
        RocketPerformance wrongFuel = PerformanceCalculator.calculate(new RocketStructure(mismatch), 9.81);
        assertTrue(wrongFuel.warnings().contains(PerformanceWarning.NO_USABLE_PROPELLANT));
        assertEquals(0, wrongFuel.deltaV(), 1e-9, "без подходящего топлива Δv = 0");
    }

    @Test
    void momentOfInertiaOfTwoPointMasses() {
        // Две точки по 100 кг на расстоянии 4 по X (центры блоков x=0.5 и x=4.5):
        // ЦМ в x=2.5, I_z = I_y = 2·100·2² = 800; I_x = 0 (обе точки на оси X)
        List<PlacedPart> parts = List.of(
                PlacedPart.of(0, 0, 0, PartProperties.hull(100)),
                PlacedPart.of(4, 0, 0, PartProperties.hull(100)));

        RocketPerformance perf = PerformanceCalculator.calculate(new RocketStructure(parts), 9.81);

        assertEquals(0, perf.momentOfInertia().x(), 1e-9);
        assertEquals(800, perf.momentOfInertia().y(), 1e-9);
        assertEquals(800, perf.momentOfInertia().z(), 1e-9);
    }
}
