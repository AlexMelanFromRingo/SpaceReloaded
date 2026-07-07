package org.alex_melan.spacereloaded.core.rocketry;

/**
 * Параметры среды полёта — из профиля измерения (FR-030).
 *
 * @param gravity ускорение свободного падения, м/с², вниз (>= 0; 0 — невесомость)
 */
public record FlightEnvironment(double gravity) {
    public FlightEnvironment {
        if (gravity < 0) {
            throw new IllegalArgumentException("gravity < 0");
        }
    }

    public static final FlightEnvironment EARTH = new FlightEnvironment(9.81);
    public static final FlightEnvironment WEIGHTLESS = new FlightEnvironment(0);
}
