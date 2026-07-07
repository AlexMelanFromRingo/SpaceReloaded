package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

import java.util.List;

/**
 * Лётно-технические характеристики, рассчитанные из фактической структуры (FR-021).
 *
 * @param dryMassKg        сухая масса
 * @param propellantMassKg масса топлива (всего)
 * @param totalMassKg      стартовая масса
 * @param centerOfMass     центр масс (локальные координаты структуры)
 * @param centerOfThrust   центр тяги; {@code Vec3d.ZERO}, если двигателей нет
 * @param totalThrustN     суммарная тяга
 * @param effectiveIspSec  эффективный удельный импульс (взвешен по расходу)
 * @param momentOfInertia  диагональ тензора инерции относительно ЦМ (кг·м²);
 *                         аппроксимация точечными массами в центрах блоков —
 *                         на блочных решётках погрешность мала и не влияет на геймплей
 * @param twr              тяговооружённость на стартовой массе при заданной гравитации
 * @param deltaV           запас характеристической скорости по Циолковскому (м/с)
 * @param warnings         предупреждения для игрока
 */
public record RocketPerformance(
        double dryMassKg,
        double propellantMassKg,
        double totalMassKg,
        Vec3d centerOfMass,
        Vec3d centerOfThrust,
        double totalThrustN,
        double effectiveIspSec,
        Vec3d momentOfInertia,
        double twr,
        double deltaV,
        List<PerformanceWarning> warnings
) {
    public RocketPerformance {
        warnings = List.copyOf(warnings);
    }
}
