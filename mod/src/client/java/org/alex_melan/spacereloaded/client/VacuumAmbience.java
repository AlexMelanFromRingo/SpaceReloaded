package org.alex_melan.spacereloaded.client;

/**
 * Клиентское состояние «в открытом вакууме» (синхронизируется сервером,
 * VacuumStatePayload): вакуум не проводит звук — внешние источники глушатся.
 */
public final class VacuumAmbience {

    private static volatile boolean exposed;

    public static boolean isExposed() {
        return exposed;
    }

    public static void setExposed(boolean value) {
        exposed = value;
    }

    private VacuumAmbience() {
    }
}
