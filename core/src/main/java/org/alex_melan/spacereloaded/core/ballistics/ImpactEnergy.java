package org.alex_melan.spacereloaded.core.ballistics;

/**
 * Разрушения из кинетической энергии (FR-043): не фиксированная «сила взрыва»,
 * а E = ½mv² с кубическим масштабированием радиуса кратера.
 *
 * <p>Масштабирование r ∝ E^(1/3) — стандартное энергетическое подобие
 * (объём разрушения пропорционален энергии). Калибровка: {@code REFERENCE_*}
 * привязывает 1 тонну на 1 км/с (E ≈ 5·10⁸ Дж) к кратеру радиуса 8 блоков —
 * игровой баланс, а не геофизика; подгонка в конфиге мода.
 */
public final class ImpactEnergy {

    public static final double REFERENCE_ENERGY_J = 5.0e8;
    public static final double REFERENCE_RADIUS_BLOCKS = 8.0;

    private ImpactEnergy() {
    }

    public static double kineticEnergyJ(double massKg, double speedMs) {
        return 0.5 * massKg * speedMs * speedMs;
    }

    public static double craterRadiusBlocks(double energyJ) {
        if (energyJ <= 0) {
            return 0;
        }
        return REFERENCE_RADIUS_BLOCKS * Math.cbrt(energyJ / REFERENCE_ENERGY_J);
    }
}
