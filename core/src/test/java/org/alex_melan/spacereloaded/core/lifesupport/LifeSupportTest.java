package org.alex_melan.spacereloaded.core.lifesupport;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Газ кабины, метаболизм, поглотители, культуры (007, SC-001, SC-003, SC-004). */
class LifeSupportTest {

    @Test
    void fillingModuleWeighs90Kg() {
        double[] m = CabinAtmosphere.fill(CabinAtmosphere.SEA_LEVEL_KPA, CabinAtmosphere.AIR_O2_FRACTION, 75);
        assertEquals(20.9, m[0], 0.3);
        assertEquals(68.6, m[1], 0.5);
        assertEquals(90, m[0] + m[1], 1.5);
        assertEquals(21.2, CabinAtmosphere.partialKpa(m[0], CabinAtmosphere.M_O2, 75, CabinAtmosphere.T_CABIN), 0.1);
    }

    @Test
    void co2ReachesIssLimitIn13Minutes() {
        double m0 = CabinAtmosphere.massFor(0.04, CabinAtmosphere.M_CO2, 75, CabinAtmosphere.T_CABIN);
        var co2 = new CabinAtmosphere.Co2(m0, Metabolism.CO2_PER_DAY, 0, 75);
        double days = co2.daysTo(0.53);
        assertEquals(0.66, days, 0.02);
        assertEquals(13.2, days * 20, 0.5); // игровые сутки = 20 мин
        assertEquals(0.53, co2.partialKpaAt(days), 1e-6);
    }

    @Test
    void scrubberHoldsSteadyState() {
        double k = Scrubber.removalM3PerDay(Scrubber.FAN_M3_PER_MIN, Scrubber.EFFICIENCY);
        var co2 = new CabinAtmosphere.Co2(0, Metabolism.CO2_PER_DAY, k, 75);
        assertTrue(co2.steadyKpa() < 0.53, "поглотитель держит норму: " + co2.steadyKpa());
        // картридж LiOH 1 кг (0.75 кг CO₂) у одного человека — ~18 игровых часов
        double days = 0.75 / Metabolism.CO2_PER_DAY;
        assertEquals(17.8, days * 24, 0.5);
        // баланс массы: выдохнуто = поглощено + осталось в воздухе
        assertEquals(Metabolism.CO2_PER_DAY * 5, co2.removedKg(5) + co2.massAt(5), 1e-9);
        assertEquals(0.919, Scrubber.LIOH_THEORETICAL, 0.001);
        assertEquals(0.727, Scrubber.SABATIER_O2_PER_CO2, 0.001);
    }

    @Test
    void thresholdsAndKleiber() {
        assertEquals(0, Metabolism.co2Level(0.3));
        assertEquals(1, Metabolism.co2Level(0.6));
        assertEquals(4, Metabolism.co2Level(8));
        assertEquals(0, Metabolism.hypoxiaLevel(21));
        assertEquals(2, Metabolism.hypoxiaLevel(12));
        assertEquals(5.0, Metabolism.kleiber(600), 0.2);
    }

    @Test
    void cropsPerPerson() {
        assertEquals(15, CropModel.areaForOxygen(CropModel.WHEAT, 1), 0.2);
        assertEquals(40, CropModel.areaForFood(CropModel.WHEAT, 1), 0.1);
        assertEquals(118, CropModel.LETTUCE.lampWattsPerM2(), 1);
        assertEquals(802, CropModel.WHEAT.lampWattsPerM2(), 2);
        // под небом орбиты (DLI 61) — 28 м² пшеницы на кислород человека
        assertEquals(28, CropModel.areaForOxygen(CropModel.WHEAT, CropModel.WHEAT.lightFactor(61)), 0.5);
        assertEquals(0.5, CropModel.co2Factor(0.06), 1e-9);
    }

    @Test
    void leakIsFractionOfSecond() {
        assertTrue(CabinAtmosphere.leakTauSeconds(75, 1, CabinAtmosphere.T_CABIN) < 0.5);
        assertEquals(240, CabinAtmosphere.chokedFlowKgPerS(101.325, 1, CabinAtmosphere.T_CABIN), 5);
    }
}
