package org.alex_melan.spacereloaded.core.rocketry;

import java.util.ArrayList;
import java.util.List;

/**
 * Планировщик бюджета маршрута (003, FR-105…FR-107, D23). Маршрут — список хопов;
 * для каждого: численный подъём с сопротивлением и гравитационными потерями
 * ({@link AscentSimulator}) от стартовой высоты до высоты перехода, затем списание
 * Δv перелёта ({@link TransferBurn}) с запасом; для хопа с посадкой — пропульсивная
 * посадка ({@link LandingBudget}) по TWR оставшегося стека при гравитации цели.
 *
 * <p>Запас применяется к перелёту и посадке (подъём моделируется точно).
 */
public final class MissionPlanner {

    /** Посадка на теле назначения: гравитация, высота и скорость прибытия. */
    public record Landing(double gravity, double arrivalHeightM, double arrivalSpeedMs) {
    }

    /**
     * @param env              среда старта хопа (гравитация + атмосфера)
     * @param startY           стартовая высота (низ стека)
     * @param transitionY      высота перехода
     * @param transferDeltaVMs Δv перелёта после перехода (таблица профиля × множитель)
     * @param cd               коэффициент сопротивления ракеты
     * @param landing          посадка в конце хопа либо {@code null} (прибытие на платформу)
     */
    public record Leg(FlightEnvironment env, double startY, double transitionY, double transferDeltaVMs,
                      double cd, Landing landing) {
    }

    public enum Reason {
        OK, ASCENT_UNREACHABLE, TRANSFER_SHORTFALL, LANDING_SHORTFALL, LANDING_TWR
    }

    public record LegReport(AscentSimulator.AscentReport ascent, double transferDeltaVMs,
                            double landingDeltaVMs, double landingTwr, Reason reason, double shortfallMs) {
    }

    /**
     * @param remainingDeltaVMs Δv стека после всего маршрута (сумма оставшихся ступеней)
     * @param propellantAfter   топливо по ступеням после маршрута
     * @param activeStageAfter  активная ступень после маршрута
     * @param firstAscentTimeS  время подъёма первого хопа, с (NaN, если не достигнут)
     */
    public record MissionReport(boolean feasible, Reason reason, double shortfallMs, List<LegReport> legs,
                                double remainingDeltaVMs, double[] propellantAfter, int activeStageAfter,
                                double firstAscentTimeS) {
        public MissionReport {
            legs = List.copyOf(legs);
        }
    }

    private MissionPlanner() {
    }

    public static MissionReport plan(StageLayout layout, double[] stagePropellantKg, int activeStage,
                                     List<Leg> legs, double marginFraction) {
        double[] fuel = new double[layout.stageCount()];
        for (int i = 0; i < fuel.length && i < stagePropellantKg.length; i++) {
            fuel[i] = Math.max(0, stagePropellantKg[i]);
        }
        int stage = Math.clamp(activeStage, 0, layout.stageCount() - 1);
        double margin = 1 + Math.max(0, marginFraction);
        List<LegReport> reports = new ArrayList<>(legs.size());
        double firstAscentTime = Double.NaN;

        for (int i = 0; i < legs.size(); i++) {
            Leg leg = legs.get(i);
            AscentSimulator.AscentReport ascent = AscentSimulator.simulate(
                    layout, fuel, stage, leg.env(), leg.cd(), leg.startY(), leg.transitionY());
            if (!ascent.reachedTarget()) {
                reports.add(new LegReport(ascent, 0, 0, Double.NaN, Reason.ASCENT_UNREACHABLE, Double.NaN));
                return new MissionReport(false, Reason.ASCENT_UNREACHABLE, Double.NaN, reports,
                        remaining(layout, fuel, stage), fuel, stage, firstAscentTime);
            }
            if (i == 0) {
                firstAscentTime = ascent.timeToTargetS();
            }
            fuel = ascent.propellantAfter();
            stage = ascent.activeStageAfter();

            double transfer = Math.max(0, leg.transferDeltaVMs()) * margin;
            TransferBurn.Result burn = TransferBurn.apply(layout, fuel, stage, transfer);
            if (!burn.achieved()) {
                reports.add(new LegReport(ascent, transfer, 0, Double.NaN, Reason.TRANSFER_SHORTFALL, burn.shortfallMs()));
                return new MissionReport(false, Reason.TRANSFER_SHORTFALL, burn.shortfallMs(), reports,
                        0, burn.propellantKg(), burn.activeStage(), firstAscentTime);
            }
            fuel = burn.propellantKg();
            stage = burn.activeStage();

            double landingDeltaV = 0;
            double landingTwr = Double.NaN;
            if (leg.landing() != null) {
                Landing landing = leg.landing();
                RocketStructure view = layout.activeView(stage, fuel);
                RocketPerformance perf = PerformanceCalculator.calculate(view, Math.max(landing.gravity(), 1e-6));
                landingTwr = perf.twr();
                double touchdown = LandingBudget.touchdownSpeed(landing.arrivalSpeedMs(), landing.gravity(),
                        landing.arrivalHeightM());
                double required = LandingBudget.propulsiveDeltaV(touchdown, landingTwr);
                if (Double.isInfinite(required)) {
                    reports.add(new LegReport(ascent, transfer, required, landingTwr, Reason.LANDING_TWR,
                            Double.POSITIVE_INFINITY));
                    return new MissionReport(false, Reason.LANDING_TWR, Double.POSITIVE_INFINITY, reports,
                            remaining(layout, fuel, stage), fuel, stage, firstAscentTime);
                }
                landingDeltaV = required * margin;
                TransferBurn.Result landingBurn = TransferBurn.apply(layout, fuel, stage, landingDeltaV);
                if (!landingBurn.achieved()) {
                    reports.add(new LegReport(ascent, transfer, landingDeltaV, landingTwr,
                            Reason.LANDING_SHORTFALL, landingBurn.shortfallMs()));
                    return new MissionReport(false, Reason.LANDING_SHORTFALL, landingBurn.shortfallMs(), reports,
                            0, landingBurn.propellantKg(), landingBurn.activeStage(), firstAscentTime);
                }
                fuel = landingBurn.propellantKg();
                stage = landingBurn.activeStage();
            }
            reports.add(new LegReport(ascent, transfer, landingDeltaV, landingTwr, Reason.OK, 0));
        }
        return new MissionReport(true, Reason.OK, 0, reports, remaining(layout, fuel, stage), fuel, stage,
                firstAscentTime);
    }

    /** Остаток Δv стека от активной ступени (без учёта гравитации — Циолковский). */
    public static double remaining(StageLayout layout, double[] fuel, int stage) {
        StagedPerformance.StagedReport report = StagedPerformance.calculate(layout, fuel, PerformanceCalculator.G0);
        double total = 0;
        for (int i = stage; i < report.stages().size(); i++) {
            total += report.stages().get(i).deltaV();
        }
        return total;
    }
}
