package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AscentSimulatorTest {

    private static final double G0 = PerformanceCalculator.G0;
    private static final String KEROLOX = "kerolox";
    private static final AtmosphereProfile EARTH_AIR = new AtmosphereProfile(1.225, 110, 63);

    /** Эталон 001: 4 двигателя по 60 кН, бак 2000 кг, m₀ = 4400 кг. */
    private static RocketStructure symmetricRocket() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 1, engine));
        parts.add(PlacedPart.of(1, 0, -1, engine));
        parts.add(PlacedPart.of(-1, 0, 1, engine));
        parts.add(PlacedPart.of(-1, 0, -1, engine));
        return new RocketStructure(parts);
    }

    /**
     * Двухступенчатый: короткая первая ступень (30 кг топлива, два двигателя
     * симметрично по бокам корпуса — без гиродинов асимметрия опрокинула бы стек)
     * + полноценная вторая (60 кН, 2000 кг).
     */
    private static RocketStructure shortFirstStage(boolean withSecond) {
        List<PlacedPart> parts = new ArrayList<>();
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(-1, 0, 0, engine));
        parts.add(PlacedPart.of(1, 0, 0, engine));
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.hull(300)));
        parts.add(new PlacedPart(org.alex_melan.spacereloaded.core.geometry.PackedPos.pack(0, 1, 0),
                PartProperties.tank(300, 2000, KEROLOX), 30));
        if (withSecond) {
            parts.add(PlacedPart.of(0, 2, 0, PartProperties.separator(120)));
            parts.add(PlacedPart.of(0, 3, 0, engine));
            parts.add(PlacedPart.filledTank(0, 4, 0, PartProperties.tank(300, 2000, KEROLOX)));
            parts.add(PlacedPart.of(0, 5, 0, PartProperties.command(500)));
        } else {
            parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        }
        return new RocketStructure(parts);
    }

    /**
     * Вакуум: время и Δv до 450 м совпадают с замкнутой формой вертикального подъёма
     * h(t) = u·τ·(w·ln w − w + 1) − g·t²/2, Δv(t) = u·ln(1/w), w = 1 − t/τ (1%).
     */
    @Test
    void vacuumAscentMatchesClosedForm() {
        RocketStructure rocket = symmetricRocket();
        StageLayout layout = StageLayout.of(rocket);
        FlightEnvironment vacuum = new FlightEnvironment(9.81);
        AscentSimulator.AscentReport report = AscentSimulator.simulate(
                layout, layout.propellantByStage(), vacuum, 0.5, 0, 450);

        double m0 = 4400;
        double u = 300 * G0;
        double mdot = 4 * 60_000 / u;
        double tau = m0 / mdot;
        double g = 9.81;
        // Бисекция по времени: h(t) = 450
        double lo = 0;
        double hi = 40;
        for (int i = 0; i < 200; i++) {
            double mid = (lo + hi) / 2;
            double w = 1 - mid / tau;
            double h = u * tau * (w * Math.log(w) - w + 1) - g * mid * mid / 2;
            if (h < 450) {
                lo = mid;
            } else {
                hi = mid;
            }
        }
        double t = (lo + hi) / 2;
        double expectedDv = u * Math.log(1 / (1 - t / tau));

        assertTrue(report.reachedTarget());
        assertEquals(t, report.timeToTargetS(), Math.max(0.1, t * 0.01), "время до высоты (1%)");
        assertEquals(expectedDv, report.deltaVSpentToTargetMs(), expectedDv * 0.01, "Δv до высоты (1%)");
        assertEquals(0, report.maxDynamicPressurePa(), 0, "в вакууме напора нет");
        assertEquals(1, report.stagesUsed());
    }

    /** Атмосфера дороже вакуума при той же гравитации; Луна дешевле Земли. */
    @Test
    void atmosphereAndGravityRaiseAscentCost() {
        StageLayout layout = StageLayout.of(symmetricRocket());
        double[] fuel = layout.propellantByStage();
        AscentSimulator.AscentReport earthAir = AscentSimulator.simulate(
                layout, fuel, new FlightEnvironment(9.81, EARTH_AIR), 0.5, 63, 450);
        AscentSimulator.AscentReport earthVacuum = AscentSimulator.simulate(
                layout, fuel, new FlightEnvironment(9.81), 0.5, 63, 450);
        AscentSimulator.AscentReport moon = AscentSimulator.simulate(
                layout, fuel, new FlightEnvironment(1.62), 0.5, 63, 220);

        assertTrue(earthAir.reachedTarget() && earthVacuum.reachedTarget() && moon.reachedTarget());
        assertTrue(earthAir.deltaVSpentToTargetMs() > earthVacuum.deltaVSpentToTargetMs(),
                "сопротивление стоит Δv: " + earthAir.deltaVSpentToTargetMs() + " > " + earthVacuum.deltaVSpentToTargetMs());
        assertTrue(earthVacuum.deltaVSpentToTargetMs() > moon.deltaVSpentToTargetMs(),
                "Луна дешевле Земли");
        assertTrue(earthAir.maxDynamicPressurePa() > 0 && earthVacuum.maxDynamicPressurePa() == 0);
    }

    /** Одна короткая ступень до 450 м не дотягивает; две ступени — дотягивают, зажигаются обе. */
    @Test
    void stagingReachesWhatSingleStageCannot() {
        FlightEnvironment vacuum = new FlightEnvironment(9.81);
        StageLayout single = StageLayout.of(shortFirstStage(false));
        AscentSimulator.AscentReport one = AscentSimulator.simulate(
                single, single.propellantByStage(), vacuum, 0.5, 0, 450);
        assertFalse(one.reachedTarget(), "30 кг топлива до 450 м не хватает, апогей " + one.apexM());
        assertTrue(one.apexM() > 0 && one.apexM() < 450);
        assertTrue(Double.isNaN(one.timeToTargetS()));
        assertEquals(0, one.deltaVSpentToTargetMs(), 0);

        StageLayout two = StageLayout.of(shortFirstStage(true));
        AscentSimulator.AscentReport both = AscentSimulator.simulate(
                two, two.propellantByStage(), vacuum, 0.5, 0, 450);
        assertTrue(both.reachedTarget(), "две ступени достигают перехода: " + both);
        assertEquals(2, both.stagesUsed());
        assertTrue(both.deltaVSpentToTargetMs() > 0);
    }

    /** TWR < 1: не отрывается — не достигнута, апогей = старт. */
    @Test
    void underpoweredStackNeverLeavesPad() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, 10_000, 300, KEROLOX)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, KEROLOX)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(5000)));
        StageLayout layout = StageLayout.of(new RocketStructure(parts));
        AscentSimulator.AscentReport report = AscentSimulator.simulate(
                layout, layout.propellantByStage(), new FlightEnvironment(9.81), 0.5, 100, 450);

        assertFalse(report.reachedTarget());
        assertEquals(100, report.apexM(), 1e-9);
        assertTrue(Double.isNaN(report.timeToTargetS()));
    }
}
