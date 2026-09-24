package org.alex_melan.spacereloaded.core.lifesupport;

import org.alex_melan.spacereloaded.core.sealing.FirstOrderMix;

/**
 * Газ герметичной зоны (007, FR-501/FR-509, D70): массы O₂, N₂, CO₂ в объёме V при T; давления —
 * идеальный газ p = m·R·T/(M·V). CO₂ — первопорядковое перемешивание (выдох экипажа, сжигатель,
 * фотосинтез в насыщении — приток G; поглотители и растения ниже насыщения — объёмное удаление k).
 * Время — игровые сутки. Утечка через отверстие — критическое истечение
 * ṁ = C·p·A/√(R_s·T), C = √γ·(2/(γ+1))^((γ+1)/(2(γ−1))) ≈ 0.685 для воздуха: масса убывает
 * экспоненциально с τ = V/(C·A·√(R_s·T)).
 */
public final class CabinAtmosphere {

    public static final double R = 8.314462618;
    public static final double M_O2 = 0.0319988;
    public static final double M_N2 = 0.0280134;
    public static final double M_CO2 = 0.04401;
    public static final double T_CABIN = 293.15;
    /** Сухой воздух: 20.95 % O₂ по объёму; CO₂ 0.04 %. */
    public static final double AIR_O2_FRACTION = 0.2095;
    public static final double AIR_CO2_FRACTION = 0.0004;
    public static final double SEA_LEVEL_KPA = 101.325;
    /** Коэффициент критического истечения воздуха (γ = 1.4). */
    public static final double CHOKED_FLOW = 0.6847;
    public static final double R_AIR = 287.05;

    private CabinAtmosphere() {
    }

    /** Парциальное давление газа, кПа. */
    public static double partialKpa(double massKg, double molarMass, double volumeM3, double temperatureK) {
        return massKg * R * temperatureK / (molarMass * volumeM3) / 1000.0;
    }

    /** Масса газа для парциального давления, кг. */
    public static double massFor(double partialKpa, double molarMass, double volumeM3, double temperatureK) {
        return partialKpa * 1000.0 * molarMass * volumeM3 / (R * temperatureK);
    }

    /** Масса CO₂ на кубометр, соответствующая парциальному давлению (кг/м³). */
    public static double co2Density(double partialKpa) {
        return massFor(partialKpa, M_CO2, 1, T_CABIN);
    }

    /**
     * Наполнение до давления p (кПа) с долей O₂ f: массы {O₂, N₂}, кг. Азот — буфер
     * (вместе с аргоном земного воздуха; для простоты модели буфер — N₂, ~1 % Ar отнесён к нему).
     */
    public static double[] fill(double totalKpa, double o2Fraction, double volumeM3) {
        return new double[] {massFor(totalKpa * o2Fraction, M_O2, volumeM3, T_CABIN),
                massFor(totalKpa * (1 - o2Fraction), M_N2, volumeM3, T_CABIN)};
    }

    /** Отрезок CO₂: масса в t0 и действующие приток (кг/сут) и удаление (м³/сут). */
    public record Co2(double massKg, double sourceKgPerDay, double removalM3PerDay, double volumeM3) {

        public double massAt(double dtDays) {
            return FirstOrderMix.at(massKg / volumeM3, sourceKgPerDay, removalM3PerDay, volumeM3, dtDays) * volumeM3;
        }

        public double partialKpaAt(double dtDays) {
            return partialKpa(massAt(dtDays), M_CO2, volumeM3, T_CABIN);
        }

        /** Масса CO₂, удалённая поглотителями за dt (кг) — интеграл k·c. */
        public double removedKg(double dtDays) {
            return removalM3PerDay * FirstOrderMix.integral(massKg / volumeM3, sourceKgPerDay, removalM3PerDay,
                    volumeM3, dtDays);
        }

        /** Через сколько суток pCO₂ достигнет порога (∞ — никогда). */
        public double daysTo(double partialKpa) {
            return FirstOrderMix.timeTo(massKg / volumeM3, sourceKgPerDay, removalM3PerDay, volumeM3,
                    co2Density(partialKpa));
        }

        public double steadyKpa() {
            return partialKpa(FirstOrderMix.steadyState(sourceKgPerDay, removalM3PerDay) * volumeM3, M_CO2,
                    volumeM3, T_CABIN);
        }
    }

    /** Постоянная времени истечения через отверстие A (м²), с. */
    public static double leakTauSeconds(double volumeM3, double holeM2, double temperatureK) {
        return volumeM3 / (CHOKED_FLOW * holeM2 * Math.sqrt(R_AIR * temperatureK));
    }

    /** Массовый расход истечения при давлении p (кПа) через A (м²), кг/с. */
    public static double chokedFlowKgPerS(double pressureKpa, double holeM2, double temperatureK) {
        return CHOKED_FLOW * pressureKpa * 1000 * holeM2 / Math.sqrt(R_AIR * temperatureK);
    }
}
