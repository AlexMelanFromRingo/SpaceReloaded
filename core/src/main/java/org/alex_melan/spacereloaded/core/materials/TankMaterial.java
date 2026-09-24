package org.alex_melan.spacereloaded.core.materials;

/**
 * Масса стенки бака по материалу (006, FR-441, D67). Два режима: давление (тонкостенный сосуд,
 * m ∝ ρ/σ_т) и устойчивость/минимальная толщина при полётных нагрузках (m ∝ ρ/E^⅓); у больших
 * насосных баков низкого давления доминирует второй: m = m_Ti·[w·(ρ/σ)/(ρ/σ)_Ti + (1−w)·(ρ/E^⅓)/(ρ/E^⅓)_Ti],
 * w = 0.3 (доля «давленческой» массы — калибровка по реальным бакам). Эталон — титан (бак мода 300 кг).
 *
 * @param density   ρ, г/см³
 * @param yieldMpa  σ_т, МПа
 * @param modulusGpa E, ГПа
 */
public record TankMaterial(double density, double yieldMpa, double modulusGpa) {

    public static final TankMaterial TITANIUM = new TankMaterial(4.43, 880, 114);
    public static final TankMaterial STEEL = new TankMaterial(7.85, 700, 200);
    public static final TankMaterial ALUMINIUM_COPPER = new TankMaterial(2.84, 393, 73);
    public static final TankMaterial ALUMINIUM_LITHIUM = new TankMaterial(2.71, 510, 76);
    public static final double PRESSURE_WEIGHT = 0.3;

    /** Множитель массы бака относительно титанового. */
    public double massMultiplier() {
        double pressure = (density / yieldMpa) / (TITANIUM.density / TITANIUM.yieldMpa);
        double buckling = (density / Math.cbrt(modulusGpa)) / (TITANIUM.density / Math.cbrt(TITANIUM.modulusGpa));
        return PRESSURE_WEIGHT * pressure + (1 - PRESSURE_WEIGHT) * buckling;
    }
}
