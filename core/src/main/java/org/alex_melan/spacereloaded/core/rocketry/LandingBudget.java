package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Бюджет пропульсивной посадки (003, FR-105, D22).
 *
 * <p>Скорость касания без торможения — свободное падение с высоты прибытия:
 * v = √(v₀² + 2gh). Торможение постоянной тягой a_т при гравитации g:
 * замедление a_т − g, время t = v/(a_т − g), потраченный Δv = a_т·t =
 * v·a_т/(a_т − g) = v·TWR/(TWR − 1). При TWR ≤ 1 остановиться нельзя.
 *
 * <p>Упрощение: атмосферное торможение не учитывается — оценка консервативна
 * для тел с атмосферой (реальный расход меньше).
 */
public final class LandingBudget {

    private LandingBudget() {
    }

    public static double touchdownSpeed(double arrivalSpeedMs, double gravity, double heightM) {
        return Math.sqrt(arrivalSpeedMs * arrivalSpeedMs + 2 * Math.max(0, gravity) * Math.max(0, heightM));
    }

    /** @return Δv посадки, м/с; {@link Double#POSITIVE_INFINITY} при TWR ≤ 1 */
    public static double propulsiveDeltaV(double touchdownSpeedMs, double twr) {
        if (!(twr > 1.0)) {
            return Double.POSITIVE_INFINITY;
        }
        return touchdownSpeedMs * twr / (twr - 1);
    }
}
