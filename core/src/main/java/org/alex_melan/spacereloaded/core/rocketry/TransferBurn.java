package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Списание Δv перелёта по уравнению Циолковского через ступени (003, FR-102, D21).
 *
 * <p>Для активной ступени m₀ — масса «активного вида» (сухая масса оставшегося
 * стека + всё его топливо, верхние ступени — груз), v_e = Isp_eff·g₀ (эффективный
 * удельный импульс по тяге двигателей ступени). Доступное Δv ступени
 * v_e·ln(m₀/(m₀ − p)). Хватает — остаток p' = m₀·e^(−Δv/v_e) − (m₀ − p); не хватает —
 * ступень сгорает целиком и отбрасывается, следующая продолжает с остатком Δv.
 *
 * <p>Упрощение (задокументировано): импульс мгновенный, сгоревшие ступени не
 * оставляют обломков в измерении (остаются на орбите перелёта).
 */
public final class TransferBurn {

    /**
     * @param propellantKg  топливо по ступеням после списания
     * @param activeStage   активная ступень после списания
     * @param achieved      весь Δv списан
     * @param shortfallMs   нехватка Δv, м/с (0 при успехе)
     * @param stagesDropped сколько ступеней сгорело целиком
     */
    public record Result(double[] propellantKg, int activeStage, boolean achieved,
                         double shortfallMs, int stagesDropped) {
    }

    private TransferBurn() {
    }

    public static Result apply(StageLayout layout, double[] stagePropellantKg, int activeStage, double deltaVMs) {
        int count = layout.stageCount();
        double[] fuel = new double[count];
        for (int i = 0; i < count && i < stagePropellantKg.length; i++) {
            fuel[i] = Math.max(0, stagePropellantKg[i]);
        }
        int stage = Math.clamp(activeStage, 0, count - 1);
        double remaining = Math.max(0, deltaVMs);
        int dropped = 0;
        if (remaining == 0) {
            return new Result(fuel, stage, true, 0, 0);
        }
        while (true) {
            RocketStructure view = layout.activeView(stage, fuel);
            RocketPerformance perf = PerformanceCalculator.calculate(view, PerformanceCalculator.G0);
            double ve = perf.effectiveIspSec() * PerformanceCalculator.G0;
            double m0 = perf.totalMassKg();
            double propellant = perf.propellantMassKg();
            double mDry = m0 - propellant;
            double available = (ve > 0 && propellant > 0 && mDry > 0) ? ve * Math.log(m0 / mDry) : 0;
            if (available >= remaining && ve > 0) {
                double mAfter = m0 * Math.exp(-remaining / ve);
                fuel[stage] = Math.max(0, mAfter - mDry);
                return new Result(fuel, stage, true, 0, dropped);
            }
            remaining -= available;
            fuel[stage] = 0;
            if (stage == count - 1) {
                return new Result(fuel, stage, false, remaining, dropped);
            }
            stage++;
            dropped++;
        }
    }
}
