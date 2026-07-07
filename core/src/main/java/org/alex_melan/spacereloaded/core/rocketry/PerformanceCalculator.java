package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.geometry.Vec3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Расчёт ЛТХ из структуры (FR-021, принцип I).
 *
 * <p>Формулы:
 * <ul>
 *   <li>центр масс: r_cm = Σ(m_i·r_i)/Σm_i, массы блоков — в геометрических центрах;</li>
 *   <li>центр тяги: r_ct = Σ(F_i·r_i)/ΣF_i;</li>
 *   <li>эффективный Isp при одновременной работе двигателей:
 *       ṁ_Σ = Σ F_i/(Isp_i·g₀); Isp_eff = ΣF_i/(g₀·ṁ_Σ);</li>
 *   <li>Δv = Isp_eff·g₀·ln(m₀/m₁) — уравнение Циолковского, m₁ = m₀ − доступное топливо;</li>
 *   <li>момент инерции: точечные массы, I_x = Σ m_i·(dy²+dz²) и т.д.
 *       (упрощение: блок как точка в центре; достаточно для игровой динамики).</li>
 * </ul>
 */
public final class PerformanceCalculator {

    /** Стандартное ускорение свободного падения — константа определения Isp (не гравитация планеты!). */
    public static final double G0 = 9.80665;

    /**
     * Порог бокового смещения центра тяги от центра масс (в блоках), после
     * которого выдаётся ASYMMETRIC_THRUST. Полблока заметно валит ракету без
     * гиродинов; 0.05 — уровень «практически соосно».
     */
    public static final double ASYMMETRY_THRESHOLD = 0.05;

    private PerformanceCalculator() {
    }

    /**
     * @param surfaceGravity гравитация измерения старта, м/с² (для TWR)
     */
    public static RocketPerformance calculate(RocketStructure structure, double surfaceGravity) {
        if (surfaceGravity <= 0) {
            throw new IllegalArgumentException("surfaceGravity must be > 0");
        }

        double dryMass = 0;
        double propellantMass = 0;
        Vec3d massMoment = Vec3d.ZERO;

        double totalThrust = 0;
        Vec3d thrustMoment = Vec3d.ZERO;
        double totalMassFlow = 0; // ṁ_Σ, кг/с при полной тяге

        int commandModules = 0;
        int engines = 0;
        Set<String> engineFuels = new HashSet<>();

        for (PlacedPart part : structure.parts()) {
            PartProperties props = part.properties();
            double partMass = props.massKg() + part.propellantKg();
            dryMass += props.massKg();
            propellantMass += part.propellantKg();
            massMoment = massMoment.add(part.center().scale(partMass));

            switch (props.role()) {
                case ENGINE -> {
                    engines++;
                    engineFuels.add(props.fuelType());
                    totalThrust += props.thrustN();
                    thrustMoment = thrustMoment.add(part.center().scale(props.thrustN()));
                    totalMassFlow += props.thrustN() / (props.ispSec() * G0);
                }
                case COMMAND -> commandModules++;
                default -> {
                }
            }
        }

        double totalMass = dryMass + propellantMass;
        Vec3d centerOfMass = totalMass > 0 ? massMoment.scale(1.0 / totalMass) : Vec3d.ZERO;
        Vec3d centerOfThrust = totalThrust > 0 ? thrustMoment.scale(1.0 / totalThrust) : Vec3d.ZERO;
        double effectiveIsp = totalMassFlow > 0 ? totalThrust / (G0 * totalMassFlow) : 0;

        // Доступное топливо: только из баков, чей тип потребляет хотя бы один двигатель
        double usablePropellant = 0;
        for (PlacedPart part : structure.parts()) {
            if (part.properties().role() == PartRole.TANK
                    && engineFuels.contains(part.properties().fuelType())) {
                usablePropellant += part.propellantKg();
            }
        }

        double deltaV = 0;
        if (engines > 0 && usablePropellant > 0 && totalMass > 0) {
            deltaV = effectiveIsp * G0 * Math.log(totalMass / (totalMass - usablePropellant));
        }

        double twr = totalMass > 0 ? totalThrust / (totalMass * surfaceGravity) : 0;

        // Момент инерции точечными массами относительно ЦМ
        double ix = 0;
        double iy = 0;
        double iz = 0;
        for (PlacedPart part : structure.parts()) {
            double m = part.properties().massKg() + part.propellantKg();
            Vec3d d = part.center().subtract(centerOfMass);
            ix += m * (d.y() * d.y() + d.z() * d.z());
            iy += m * (d.x() * d.x() + d.z() * d.z());
            iz += m * (d.x() * d.x() + d.y() * d.y());
        }

        List<PerformanceWarning> warnings = new ArrayList<>();
        if (commandModules == 0) {
            warnings.add(PerformanceWarning.NO_COMMAND_MODULE);
        }
        if (commandModules > 1) {
            warnings.add(PerformanceWarning.MULTIPLE_COMMAND_MODULES);
        }
        if (engines == 0) {
            warnings.add(PerformanceWarning.NO_ENGINE);
        } else {
            if (usablePropellant <= 0) {
                warnings.add(PerformanceWarning.NO_USABLE_PROPELLANT);
            }
            if (twr < 1) {
                warnings.add(PerformanceWarning.TWR_BELOW_ONE);
            }
            if (centerOfThrust.horizontalDistanceTo(centerOfMass) > ASYMMETRY_THRESHOLD) {
                warnings.add(PerformanceWarning.ASYMMETRIC_THRUST);
            }
        }

        return new RocketPerformance(dryMass, propellantMass, totalMass,
                centerOfMass, centerOfThrust, totalThrust, effectiveIsp,
                new Vec3d(ix, iy, iz), twr, deltaV, warnings);
    }
}
