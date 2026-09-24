package org.alex_melan.spacereloaded.core.kinetics;

/**
 * Предел кручения сплошного вала (005, FR-305): τ_max = π·d³·τ_y/16 (касательное напряжение
 * на поверхности τ = 16T/(πd³) достигает предела текучести при сдвиге τ_y). Сталь (τ_y ≈ 230 МПа,
 * 0.58·σ_y) при d = 0.25 м держит ≈ 706 кН·м, дерево (скалывание ≈ 8 МПа) — ≈ 24.5 кН·м:
 * мощность передают на высоких оборотах, момент поднимают редуктором у потребителя.
 */
public final class ShaftStrength {

    private ShaftStrength() {
    }

    public static double maxTorque(double diameterM, double shearStrengthPa) {
        return Math.PI * diameterM * diameterM * diameterM * shearStrengthPa / 16.0;
    }
}
