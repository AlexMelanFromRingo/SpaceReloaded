package org.alex_melan.spacereloaded.core.ballistics;

/**
 * Свойства кинетического снаряда (вольфрамовый лом, FR-042).
 *
 * @param massKg    масса снаряда
 * @param dragCoeff линейный коэффициент сопротивления, 1/с (0 — вакуум;
 *                  v1 использует линейную модель F_d = −k·m·v — достаточная
 *                  точность для игровых скоростей, честная квадратичная
 *                  аэродинамика добавится вместе с плотностью атмосфер)
 */
public record ProjectileSpec(double massKg, double dragCoeff) {
    public ProjectileSpec {
        if (massKg <= 0) {
            throw new IllegalArgumentException("massKg must be > 0");
        }
        if (dragCoeff < 0) {
            throw new IllegalArgumentException("dragCoeff < 0");
        }
    }
}
