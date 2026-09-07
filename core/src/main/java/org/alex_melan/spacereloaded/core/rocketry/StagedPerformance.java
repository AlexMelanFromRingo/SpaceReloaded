package org.alex_melan.spacereloaded.core.rocketry;

import java.util.ArrayList;
import java.util.List;

/**
 * ЛТХ по ступеням (Полёт 2.0, FR-063): последовательный Циолковский.
 *
 * <p>Для ступени i берётся {@link StageLayout#activeView активный вид} — нижние
 * ступени уже сброшены, верхние едут грузом — и считается обычный
 * {@link PerformanceCalculator}. Так Δv_i = Isp_eff,i·g₀·ln(m₀,i / (m₀,i − p_i)),
 * где m₀,i — масса оставшегося стека на момент зажигания ступени, а TWR_i —
 * от той же массы при гравитации тела старта. Суммарный Δv = Σ Δv_i.
 * Одноступенчатый стек даёт в точности числа {@link PerformanceCalculator}.
 */
public final class StagedPerformance {

    /**
     * @param index        номер ступени снизу
     * @param stackMassKg  масса оставшегося стека на момент зажигания (с топливом)
     * @param dryMassKg    сухая масса самой ступени
     * @param propellantKg топливо ступени
     * @param thrustN      суммарная тяга двигателей ступени
     * @param twr          тяговооружённость на момент зажигания
     * @param deltaV       Δv ступени (0 без двигателей/топлива)
     * @param warnings     предупреждения расчёта ступени
     */
    public record StagePerformance(int index, double stackMassKg, double dryMassKg, double propellantKg,
                                   double thrustN, double twr, double deltaV,
                                   List<PerformanceWarning> warnings) {
        public StagePerformance {
            warnings = List.copyOf(warnings);
        }
    }

    /** Отчёт по всем ступеням снизу вверх и суммарный Δv. */
    public record StagedReport(List<StagePerformance> stages, double totalDeltaV) {
        public StagedReport {
            stages = List.copyOf(stages);
        }

        public int stageCount() {
            return stages.size();
        }
    }

    private StagedPerformance() {
    }

    /**
     * @param stagePropellantKg топливо по ступеням (кг)
     * @param surfaceGravity    гравитация тела старта, м/с² (для TWR каждой ступени)
     */
    public static StagedReport calculate(StageLayout layout, double[] stagePropellantKg, double surfaceGravity) {
        boolean multiStage = layout.stageCount() > 1;
        List<StagePerformance> result = new ArrayList<>(layout.stageCount());
        double total = 0;
        for (int i = 0; i < layout.stageCount(); i++) {
            StageLayout.Stage stage = layout.stage(i);
            RocketStructure view = layout.activeView(i, stagePropellantKg);
            RocketPerformance perf = PerformanceCalculator.calculate(view, surfaceGravity);

            List<PerformanceWarning> warnings = new ArrayList<>(perf.warnings());
            if (multiStage && !stage.hasEngines()) {
                warnings.remove(PerformanceWarning.NO_ENGINE);
                warnings.add(PerformanceWarning.STAGE_NO_ENGINE);
            }
            double propellant = i < stagePropellantKg.length
                    ? Math.clamp(stagePropellantKg[i], 0, stage.propellantCapacityKg()) : 0;
            double deltaV = stage.hasEngines() ? perf.deltaV() : 0;
            total += deltaV;
            result.add(new StagePerformance(i, perf.totalMassKg(), stage.dryMassKg(), propellant,
                    perf.totalThrustN(), perf.twr(), deltaV, warnings));
        }
        return new StagedReport(result, total);
    }
}
