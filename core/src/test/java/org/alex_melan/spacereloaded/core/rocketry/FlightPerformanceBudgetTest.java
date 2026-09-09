package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;
import org.alex_melan.spacereloaded.core.atmosphere.DragBody;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Бюджет производительности (SC-007, конституция «Бюджет производительности»):
 * стек из 500 деталей в 2 ступени — 1000 шагов полёта с сопротивлением быстрее
 * секунды (в игре 1 шаг на тик, бюджет 1 мс), скан со ступенями и моделированием
 * подъёма быстрее 100 мс. Пороги мягкие (×2) для медленных CI-раннеров.
 */
class FlightPerformanceBudgetTest {

    private static final String KEROLOX = "kerolox";

    /**
     * 525 деталей: 21 ярус 5×5. Нижняя ступень — два яруса двигателей, ярус баков,
     * лёгкий корпус, ярус разделителей; верхняя — ярус двигателей, баки, корпус,
     * командный модуль. Массы подобраны так, чтобы стек летел (TWR ≈ 1.9 у обеих ступеней).
     */
    private static RocketStructure bigStack() {
        List<PlacedPart> parts = new ArrayList<>();
        PartProperties engine = PartProperties.engine(400, 60_000, 300, KEROLOX);
        PartProperties tank = PartProperties.tank(300, 2000, KEROLOX);
        PartProperties skin = PartProperties.hull(40);
        for (int y = 0; y < 21; y++) {
            for (int x = -2; x <= 2; x++) {
                for (int z = -2; z <= 2; z++) {
                    if (y == 0 || y == 1 || y == 11) {
                        parts.add(PlacedPart.of(x, y, z, engine));
                    } else if (y == 2 || y == 12) {
                        parts.add(PlacedPart.filledTank(x, y, z, tank));
                    } else if (y == 10) {
                        parts.add(PlacedPart.of(x, y, z, PartProperties.separator(120)));
                    } else if (y == 20 && x == 0 && z == 0) {
                        parts.add(PlacedPart.of(x, y, z, PartProperties.command(500)));
                    } else if (x == 0 && z == 0 && y == 5) {
                        parts.add(PlacedPart.of(x, y, z, PartProperties.gyro(200, 500_000)));
                    } else {
                        parts.add(PlacedPart.of(x, y, z, skin));
                    }
                }
            }
        }
        return new RocketStructure(parts);
    }

    @Test
    void thousandFlightStepsOfBigStackUnderOneSecond() {
        RocketStructure rocket = bigStack();
        assertTrue(rocket.parts().size() >= 500, "стек ≥ 500 деталей: " + rocket.parts().size());
        StageLayout layout = StageLayout.of(rocket);
        double[] fuel = layout.propellantByStage();
        RocketStructure view = layout.activeView(0, fuel);
        DragBody drag = view.dragBody(0.5);
        FlightEnvironment env = new FlightEnvironment(9.81, new AtmosphereProfile(1.225, 110, 63));
        FlightState state = FlightState.atRest(new Vec3d(0, 64, 0), fuel[0]);

        // Прогрев JIT
        for (int i = 0; i < 200; i++) {
            state = FlightIntegrator.step(view, state, ControlInput.FULL_STABILIZED, env, 0.05, drag);
        }
        long start = System.nanoTime();
        for (int i = 0; i < 1000; i++) {
            state = FlightIntegrator.step(view, state, ControlInput.FULL_STABILIZED, env, 0.05, drag);
        }
        double seconds = (System.nanoTime() - start) / 1.0e9;
        assertTrue(seconds < 2.0, "1000 шагов полёта 500 деталей: " + seconds + " с (бюджет 1 с, мягко 2 с)");
    }

    @Test
    void stagedScanWithAscentSimulationUnder100Ms() {
        RocketStructure rocket = bigStack();
        StageLayout layout = StageLayout.of(rocket);
        double[] fuel = layout.propellantByStage();
        FlightEnvironment env = new FlightEnvironment(9.81, new AtmosphereProfile(1.225, 110, 63));

        // Прогрев
        StagedPerformance.calculate(layout, fuel, 9.81);
        AscentSimulator.simulate(layout, fuel, env, 0.5, 64, 450);

        long start = System.nanoTime();
        StagedPerformance.StagedReport report = StagedPerformance.calculate(layout, fuel, 9.81);
        AscentSimulator.AscentReport ascent = AscentSimulator.simulate(layout, fuel, env, 0.5, 64, 450);
        double millis = (System.nanoTime() - start) / 1.0e6;
        assertTrue(report.stageCount() == 2 && ascent.reachedTarget(), "стек летит: " + ascent);
        assertTrue(millis < 200, "скан + подъём 500 деталей: " + millis + " мс (бюджет 100, мягко 200)");
    }

    /** 003 (SC-003): планировщик маршрута из 3 хопов для стека 500 деталей — быстрее 300 мс (мягко 600). */
    @Test
    void threeLegMissionPlanUnder300Ms() {
        RocketStructure rocket = bigStack();
        StageLayout layout = StageLayout.of(rocket);
        double[] fuel = layout.propellantByStage();
        FlightEnvironment earth = new FlightEnvironment(9.81, new AtmosphereProfile(1.225, 110, 63));
        FlightEnvironment orbit = new FlightEnvironment(1.62);
        java.util.List<MissionPlanner.Leg> legs = java.util.List.of(
                new MissionPlanner.Leg(earth, 64, 450, 0, 0.5, null),
                new MissionPlanner.Leg(orbit, 100, 260, 200, 0.5, null),
                new MissionPlanner.Leg(orbit, 100, 260, 100, 0.5, new MissionPlanner.Landing(1.62, 180, 5)));

        MissionPlanner.plan(layout, fuel, 0, legs, 0.05); // прогрев
        long start = System.nanoTime();
        MissionPlanner.MissionReport report = MissionPlanner.plan(layout, fuel, 0, legs, 0.05);
        double millis = (System.nanoTime() - start) / 1.0e6;
        assertTrue(report.legs().size() >= 1, "план посчитан: " + report.reason());
        assertTrue(millis < 600, "планировщик 3 хопа, 500 деталей: " + millis + " мс (бюджет 300, мягко 600)");
    }
}
