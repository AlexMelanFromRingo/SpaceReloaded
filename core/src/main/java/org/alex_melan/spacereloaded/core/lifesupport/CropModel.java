package org.alex_melan.spacereloaded.core.lifesupport;

/**
 * Культуры гидропоники (007, FR-510…FR-512, D75): нормы NASA BVAD (табл. 4-89…4-91), камера BPC.
 * Продуктивность пропорциональна свету ниже номинала (постоянная радиационная эффективность,
 * Wheeler 2008): P = P_ном·min(DLI/DLI_ном, 1). Поглощение CO₂ насыщается выше ~0.12 кПа
 * (**оценка** по BVAD 0.1–0.2 кПа), ниже — линейно по pCO₂: при истощении CO₂ фотосинтез
 * встаёт (точка компенсации). Фитолампа — светодиоды 1.66 мкмоль/Дж: мощность на м² =
 * DLI/(1.66·10⁻⁶·86400) Вт.
 */
public final class CropModel {

    public static final double LED_MOL_PER_J = 1.66e-6;
    public static final double CO2_SATURATION_KPA = 0.12;
    public static final double FOOD_MJ_PER_KG_DRY = 15;

    /**
     * Культура: свет (моль/м²·сут), цикл (сут), съедобная биомасса (г сух./м²·сут), индекс урожая,
     * O₂ и CO₂ (г/м²·сут), вода (кг/м²·сут).
     */
    public record Crop(String id, double dli, double cycleDays, double edible, double harvestIndex,
                       double o2, double co2, double water) {

        /** Доля номинальной продуктивности при свете dli. */
        public double lightFactor(double actualDli) {
            return Math.max(0, Math.min(1, actualDli / dli));
        }

        /** Мощность фитолампы на м² для номинального света, Вт. */
        public double lampWattsPerM2() {
            return lampWatts(dli);
        }

        /** Пищевая энергия, МДж/м²·сут. */
        public double foodMj() {
            return edible / 1000 * FOOD_MJ_PER_KG_DRY;
        }

        /** Несъедобная (соломистая) биомасса, г/м²·сут. */
        public double inedible() {
            return edible / harvestIndex - edible;
        }
    }

    public static final Crop WHEAT = new Crop("wheat", 115, 80, 20.0, 0.40, 56.0, 77.0, 11.8);
    public static final Crop POTATO = new Crop("potato", 28, 132, 21.1, 0.70, 32.2, 45.2, 4.0);
    public static final Crop LETTUCE = new Crop("lettuce", 17, 28, 6.6, 0.90, 7.8, 10.7, 2.1);

    private CropModel() {
    }

    public static double lampWatts(double dli) {
        return dli / (LED_MOL_PER_J * 86400);
    }

    /** Множитель фотосинтеза от pCO₂ (линейно ниже насыщения). */
    public static double co2Factor(double partialKpa) {
        return Math.max(0, Math.min(1, partialKpa / CO2_SATURATION_KPA));
    }

    /** Площадь посадок (м²), покрывающая кислород человека. */
    public static double areaForOxygen(Crop crop, double lightFactor) {
        return Metabolism.O2_PER_DAY * 1000 / (crop.o2() * lightFactor);
    }

    /** Площадь посадок (м²), кормящая человека. */
    public static double areaForFood(Crop crop, double lightFactor) {
        return Metabolism.FOOD_MJ_PER_DAY / (crop.foodMj() * lightFactor);
    }
}
