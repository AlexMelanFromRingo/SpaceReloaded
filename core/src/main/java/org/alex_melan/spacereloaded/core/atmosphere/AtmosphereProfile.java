package org.alex_melan.spacereloaded.core.atmosphere;

/**
 * Профиль атмосферы небесного тела (Полёт 2.0, FR-080): изотермическая
 * экспоненциальная модель ρ(h) = ρ₀·exp(−h/H), h = высота над уровнем отсчёта.
 *
 * <p>Упрощения (задокументированы, принцип I):
 * <ul>
 *   <li>высота шкалы H — в игровых метрах, а не в реальных (8.5 км у Земли):
 *       вертикаль мира сжата, высота перехода Земли — 450 м, и при H = 110
 *       на ней остаётся ~3% приземной плотности — «атмосфера кончается до орбиты»;</li>
 *   <li>ниже уровня отсчёта плотность не растёт (шахта — не батискаф);</li>
 *   <li>состав газа на сопротивление не влияет — только плотность.</li>
 * </ul>
 *
 * @param surfaceDensity плотность у уровня отсчёта, кг/м³ (0 — вакуум)
 * @param scaleHeightM   высота шкалы, м (> 0)
 * @param datumY         уровень отсчёта высоты (поверхность / уровень моря тела)
 */
public record AtmosphereProfile(double surfaceDensity, double scaleHeightM, double datumY) {

    /** Вакуум: сопротивления и нагрева нет; значение по умолчанию для тел без полей атмосферы. */
    public static final AtmosphereProfile VACUUM = new AtmosphereProfile(0, 1, 0);

    public AtmosphereProfile {
        if (surfaceDensity < 0) {
            throw new IllegalArgumentException("surfaceDensity < 0");
        }
        if (scaleHeightM <= 0) {
            throw new IllegalArgumentException("scaleHeightM must be > 0");
        }
    }

    public boolean isVacuum() {
        return surfaceDensity <= 0;
    }

    /** Плотность на абсолютной высоте y, кг/м³. */
    public double density(double y) {
        if (surfaceDensity <= 0) {
            return 0;
        }
        double h = Math.max(0, y - datumY);
        return surfaceDensity * Math.exp(-h / scaleHeightM);
    }
}
