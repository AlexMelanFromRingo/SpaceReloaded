package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Планировщик бюджета маршрута (003, FR-105…FR-107, D23): подъём численно,
 * перелёт по таблице, посадка с TWR при гравитации цели, запас.
 */
class MissionPlannerTest {

    private static final String KEROLOX = "kerolox";
    private static final String METHALOX = "methalox";
    private static final FlightEnvironment ORBIT = new FlightEnvironment(1.62);
    private static final FlightEnvironment EARTH = new FlightEnvironment(9.81, new AtmosphereProfile(1.225, 110, 63));
    private static final FlightEnvironment MARS = new FlightEnvironment(3.72, new AtmosphereProfile(0.02, 150, 64));
    private static final double TO_MOON = 3955;
    private static final MissionPlanner.Landing MOON_LANDING = new MissionPlanner.Landing(1.62, 180, 5);

    /** Одноступенчатый керолокс: двигатель + N баков + командный модуль (Δv ≈ 2886 при N=1, 4314 при N=3). */
    private static RocketStructure kerolox(int tanks, double thrustN) {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(400, thrustN, 300, KEROLOX)));
        for (int i = 0; i < tanks; i++) {
            parts.add(PlacedPart.filledTank(0, 1 + i, 0, PartProperties.tank(300, 2000, KEROLOX)));
        }
        parts.add(PlacedPart.of(0, 1 + tanks, 0, PartProperties.command(500)));
        return new RocketStructure(parts);
    }

    private static MissionPlanner.Leg orbitToMoon(double margin) {
        return new MissionPlanner.Leg(ORBIT, 100, 260, TO_MOON, 0.5, MOON_LANDING);
    }

    @Test
    void oneTankCannotAffordMoonTransfer() {
        StageLayout layout = StageLayout.of(kerolox(1, 60_000));
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(orbitToMoon(0.05)), 0.05);

        assertFalse(report.feasible());
        assertEquals(MissionPlanner.Reason.TRANSFER_SHORTFALL, report.reason());
        assertTrue(report.shortfallMs() > 1000, "нехватка больше километра в секунду: " + report.shortfallMs());
        assertEquals(1, report.legs().size());
        assertTrue(report.legs().get(0).ascent().reachedTarget());
    }

    @Test
    void threeTanksReachTheMoonFromOrbit() {
        StageLayout layout = StageLayout.of(kerolox(3, 60_000));
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(orbitToMoon(0.05)), 0.05);

        assertTrue(report.feasible(), "3 бака с орбиты до Луны: " + report);
        assertEquals(MissionPlanner.Reason.OK, report.reason());
        assertTrue(report.remainingDeltaVMs() > 0);
        assertTrue(report.legs().get(0).landingDeltaVMs() > 0 && report.legs().get(0).landingTwr() > 1);
        assertTrue(report.firstAscentTimeS() > 0);
    }

    /** Метанокс с одним баком возвращается с Марса на орбиту Земли (ISRU-путь домой, FR-133). */
    @Test
    void methaloxSingleTankReturnsFromMars() {
        List<PlacedPart> parts = new ArrayList<>();
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.engine(420, 75_000, 330, METHALOX)));
        parts.add(PlacedPart.filledTank(0, 1, 0, PartProperties.tank(300, 2000, METHALOX)));
        parts.add(PlacedPart.of(0, 2, 0, PartProperties.command(500)));
        StageLayout layout = StageLayout.of(new RocketStructure(parts));
        MissionPlanner.Leg marsToOrbit = new MissionPlanner.Leg(MARS, 64, 240, 2103, 0.5, null);
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(marsToOrbit), 0.05);

        assertTrue(report.feasible(), "метанокс домой: " + report);
        assertTrue(report.remainingDeltaVMs() > 500, "остаток после возврата: " + report.remainingDeltaVMs());
    }

    /** Слабый двигатель: подъём с орбиты возможен, но при гравитации цели TWR ≤ 1 — посадка невозможна. */
    @Test
    void weakEngineCannotLandOnHeavyBody() {
        StageLayout layout = StageLayout.of(kerolox(1, 20_000));
        MissionPlanner.Leg toEarthLikeBody = new MissionPlanner.Leg(ORBIT, 100, 260, 0, 0.5,
                new MissionPlanner.Landing(9.81, 180, 5));
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(toEarthLikeBody), 0.0);

        assertFalse(report.feasible());
        assertEquals(MissionPlanner.Reason.LANDING_TWR, report.reason());
        assertTrue(report.legs().get(0).landingTwr() <= 1.0);
    }

    @Test
    void marginReducesRemainingBudget() {
        StageLayout layout = StageLayout.of(kerolox(3, 60_000));
        MissionPlanner.MissionReport noMargin = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(orbitToMoon(0)), 0.0);
        MissionPlanner.MissionReport withMargin = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, List.of(orbitToMoon(0.05)), 0.05);

        assertTrue(noMargin.feasible() && withMargin.feasible());
        double diff = noMargin.remainingDeltaVMs() - withMargin.remainingDeltaVMs();
        assertTrue(diff > 150 && diff < 260, "запас 5 % от ~4000 м/с ≈ 200: " + diff);
    }

    /** Подъём недостижим: TWR < 1 на старте. */
    @Test
    void ascentUnreachableIsReported() {
        StageLayout layout = StageLayout.of(kerolox(3, 60_000)); // 7800 кг · 9.81 > 60 кН
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0,
                List.of(new MissionPlanner.Leg(EARTH, 63, 260, 0, 0.5, null)), 0.05);

        assertFalse(report.feasible());
        assertEquals(MissionPlanner.Reason.ASCENT_UNREACHABLE, report.reason());
    }

    /** Два хопа: Земля → орбита (первая ступень) → Луна (перелёт съедает первую, вторая садится). */
    @Test
    void twoStageStackFliesEarthToMoonInTwoLegs() {
        List<PlacedPart> parts = new ArrayList<>();
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        parts.add(PlacedPart.of(1, 0, 0, engine));
        parts.add(PlacedPart.of(-1, 0, 0, engine));
        parts.add(PlacedPart.of(0, 0, 1, engine));
        parts.add(PlacedPart.of(0, 0, -1, engine));
        parts.add(PlacedPart.of(0, 0, 0, PartProperties.hull(300)));
        for (int i = 1; i <= 4; i++) {
            parts.add(PlacedPart.filledTank(0, i, 0, PartProperties.tank(300, 2000, KEROLOX)));
        }
        parts.add(PlacedPart.of(0, 5, 0, PartProperties.separator(120)));
        parts.add(PlacedPart.of(0, 6, 0, engine));
        for (int i = 7; i <= 9; i++) {
            parts.add(PlacedPart.filledTank(0, i, 0, PartProperties.tank(300, 2000, KEROLOX)));
        }
        parts.add(PlacedPart.of(0, 10, 0, PartProperties.command(500)));
        StageLayout layout = StageLayout.of(new RocketStructure(parts));
        assertEquals(2, layout.stageCount());

        List<MissionPlanner.Leg> legs = List.of(
                new MissionPlanner.Leg(EARTH, 63, 260, 0, 0.5, null),
                new MissionPlanner.Leg(ORBIT, 100, 260, TO_MOON, 0.5, MOON_LANDING));
        MissionPlanner.MissionReport report = MissionPlanner.plan(
                layout, layout.propellantByStage(), 0, legs, 0.05);

        assertTrue(report.feasible(), "двухступенчатый стек до Луны: " + report);
        assertEquals(2, report.legs().size());
        assertEquals(1, report.activeStageAfter(), "первая ступень сгорела в перелёте");
        assertEquals(0, report.propellantAfter()[0], 0);
        assertTrue(report.remainingDeltaVMs() > 0);
    }
}
