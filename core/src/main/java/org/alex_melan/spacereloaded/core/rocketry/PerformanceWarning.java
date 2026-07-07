package org.alex_melan.spacereloaded.core.rocketry;

/** Предупреждения предстартового расчёта — показываются игроку в бортовом GUI. */
public enum PerformanceWarning {
    NO_COMMAND_MODULE,
    MULTIPLE_COMMAND_MODULES,
    NO_ENGINE,
    /** В баках нет топлива, подходящего хотя бы одному двигателю. */
    NO_USABLE_PROPELLANT,
    /** Тяговооружённость меньше единицы — ракета не оторвётся. */
    TWR_BELOW_ONE,
    /**
     * Центр тяги смещён вбок относительно центра масс — на взлёте возникнет
     * крутящий момент (осознанный хардкор: несимметричная ракета летит криво).
     */
    ASYMMETRIC_THRUST
}
