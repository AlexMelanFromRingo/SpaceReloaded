package org.alex_melan.spacereloaded.core.orbit;

/**
 * Календарь мира (009, FR-700, D90): игровые сутки (24000 тиков) = 130 суток небесной механики
 * (сжатие 003: синодический период Марса 780 сут ≈ 6 игровых суток). Тик 0 — 14 марта 2018 г.
 * (6647 сут после J2000): новый мир встречает дешёвое окно к Марсу (май 2018) через ½ игровых суток.
 */
public final class GameCalendar {

    public static final double DAYS_PER_GAME_DAY = 130;
    public static final double EPOCH_DAY_J2000 = 6647;
    public static final double TICKS_PER_GAME_DAY = 24000;

    private GameCalendar() {
    }

    /** Сутки после J2000 на тик мира. */
    public static double day(long tick, double epochDayJ2000) {
        return epochDayJ2000 + tick / TICKS_PER_GAME_DAY * DAYS_PER_GAME_DAY;
    }

    public static double day(long tick) {
        return day(tick, EPOCH_DAY_J2000);
    }

    /** Тик мира на сутки после J2000. */
    public static long tick(double dayJ2000, double epochDayJ2000) {
        return Math.round((dayJ2000 - epochDayJ2000) / DAYS_PER_GAME_DAY * TICKS_PER_GAME_DAY);
    }

    /** Длительность в сутках небесной механики → тики мира. */
    public static long ticks(double days) {
        return Math.round(days / DAYS_PER_GAME_DAY * TICKS_PER_GAME_DAY);
    }
}
