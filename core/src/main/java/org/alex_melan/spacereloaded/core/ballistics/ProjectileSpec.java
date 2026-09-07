package org.alex_melan.spacereloaded.core.ballistics;

import org.alex_melan.spacereloaded.core.atmosphere.DragBody;

/**
 * Свойства кинетического снаряда (вольфрамовый лом, метеорит; FR-042, FR-082).
 *
 * <p>Полёт 2.0: квадратичная модель F_d = ½ρv²·C_d·A по плотности атмосферы
 * на текущей высоте (линейная модель 001 удалена — она не отличала Марс от Земли).
 *
 * @param massKg  масса снаряда, кг
 * @param cd      коэффициент сопротивления (0 — не тормозится)
 * @param areaM2  площадь сечения, м²
 */
public record ProjectileSpec(double massKg, double cd, double areaM2) {
    public ProjectileSpec {
        if (massKg <= 0) {
            throw new IllegalArgumentException("massKg must be > 0");
        }
        if (cd < 0 || areaM2 < 0) {
            throw new IllegalArgumentException("cd/areaM2 < 0");
        }
    }

    /** Снаряд без сопротивления (вакуум / аналитические тесты). */
    public static ProjectileSpec ballistic(double massKg) {
        return new ProjectileSpec(massKg, 0, 0);
    }

    public DragBody dragBody() {
        return DragBody.uniform(cd, areaM2);
    }
}
