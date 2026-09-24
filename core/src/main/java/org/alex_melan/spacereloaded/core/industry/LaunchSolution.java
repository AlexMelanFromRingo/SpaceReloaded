package org.alex_melan.spacereloaded.core.industry;

/**
 * Отчёт катапульты (004, FR-207/FR-212): все величины решения и первая причина
 * неготовности (порядок проверок — FR-207).
 *
 * @param vMax              дульная скорость рельса, м/с
 * @param vRequired         требуемая скорость маршрута, м/с (0 — цель не задана)
 * @param massKg            масса капсулы с грузом, кг
 * @param energyJ           энергия выстрела, Дж
 * @param energyUnits       энергия выстрела в единицах мода
 * @param dynamicPressurePa напор на срезе, Па (0 в вакууме)
 * @param heatFluxWm2       тепловой поток на срезе, Вт/м² (0 в вакууме)
 * @param reason            первая причина неготовности или {@link Reason#OK}
 * @param missingSections   недостающие секции лучшего тира (для RAIL_SHORT)
 */
public record LaunchSolution(double vMax, double vRequired, double massKg, double energyJ, long energyUnits,
                             double dynamicPressurePa, double heatFluxWm2, Reason reason, int missingSections) {

    /** Причины отказа в порядке проверки. */
    public enum Reason {
        OK,
        NO_RAIL,
        NO_RADIUS,
        NO_TARGET,
        UNREACHABLE_TARGET,
        ATMOSPHERE_PRESSURE,
        ATMOSPHERE_HEAT,
        RAIL_SHORT,
        NO_POD,
        RECHARGING,
        RAIL_UNLOADED,
        MUZZLE_BLOCKED,
        NO_ENERGY
    }

    public boolean ready() {
        return reason == Reason.OK;
    }

    public LaunchSolution withReason(Reason newReason) {
        return new LaunchSolution(vMax, vRequired, massKg, energyJ, energyUnits, dynamicPressurePa, heatFluxWm2,
                newReason, missingSections);
    }
}
