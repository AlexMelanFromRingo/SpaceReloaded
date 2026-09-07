package org.alex_melan.spacereloaded.core.rocketry;

import org.alex_melan.spacereloaded.core.atmosphere.AtmosphereProfile;

import java.util.Objects;

/**
 * Параметры среды полёта — из профиля измерения (FR-030, FR-080).
 *
 * @param gravity    ускорение свободного падения, м/с², вниз (>= 0; 0 — невесомость)
 * @param atmosphere профиль атмосферы; {@link AtmosphereProfile#VACUUM} — без сопротивления
 */
public record FlightEnvironment(double gravity, AtmosphereProfile atmosphere) {
    public FlightEnvironment {
        if (gravity < 0) {
            throw new IllegalArgumentException("gravity < 0");
        }
        Objects.requireNonNull(atmosphere, "atmosphere");
    }

    /** Совместимость с 001: среда без атмосферы (вакуум). */
    public FlightEnvironment(double gravity) {
        this(gravity, AtmosphereProfile.VACUUM);
    }

    /** Земная гравитация БЕЗ атмосферы — эталон тестов 001 (сопротивление включается явно). */
    public static final FlightEnvironment EARTH = new FlightEnvironment(9.81);
    public static final FlightEnvironment WEIGHTLESS = new FlightEnvironment(0);

    /** Плотность атмосферы на высоте y, кг/м³. */
    public double density(double y) {
        return atmosphere.density(y);
    }
}
