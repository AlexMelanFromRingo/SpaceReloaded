package org.alex_melan.spacereloaded.core.survey;

import org.alex_melan.spacereloaded.core.orbit.OrbitalImaging;

import java.util.Map;

/**
 * Гиперспектральная съёмка (009, US3, FR-730/731): спектрометр ближнего ИК за телескопом 10 см
 * (как у спутника-камеры 007) различает минералы открытой поверхности по диагностическим
 * полосам поглощения 1–2.5 мкм (M³, CRISM). Разрешение — дифракция на рабочей длине волны
 * λ = 2 мкм: GSD = 1.22·λh/D. Пиксель — линейная смесь площадей минералов; минерал отмечается,
 * если его доля не меньше порога обнаружения (**оценка** 25 %, у M³/CRISM 10–30 % по полосе).
 */
public final class SpectralMapping {

    /** Рабочая длина волны (полосы воды и гидроксила 1.9–2.3 мкм), м. */
    public static final double SWIR_M = 2.0e-6;
    public static final double DEFAULT_THRESHOLD = 0.25;

    private SpectralMapping() {
    }

    /** Разрешение на местности в ближнем ИК, м. */
    public static double gsd(double altitudeM, double apertureM) {
        return OrbitalImaging.gsd(SWIR_M, altitudeM, apertureM);
    }

    /** Минимальный масштаб карты (1 пиксель = 2^k м не мельче GSD). */
    public static int minScale(double altitudeM, double apertureM) {
        return OrbitalImaging.minScale(gsd(altitudeM, apertureM));
    }

    /**
     * Минерал пикселя по площадям: наибольшая доля среди минералов с полосами, если она не меньше
     * порога; иначе null (пиксель — фон, спектр без уверенного разделения).
     *
     * @param areas площадь (число верхних блоков) каждого минерала в пикселе
     * @param total площадь пикселя (все верхние блоки, включая минералы без полос)
     */
    public static String classify(Map<String, Integer> areas, int total, double threshold) {
        if (total <= 0) {
            return null;
        }
        String best = null;
        int bestArea = 0;
        for (var e : areas.entrySet()) {
            if (e.getValue() > bestArea) {
                best = e.getKey();
                bestArea = e.getValue();
            }
        }
        return best != null && bestArea >= threshold * total ? best : null;
    }
}
