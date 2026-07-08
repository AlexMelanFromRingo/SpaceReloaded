package org.alex_melan.spacereloaded.planet;

import org.alex_melan.spacereloaded.registry.ModRegistries;

/**
 * Окна Гомана (Phase 11): перелёт к цели с синодическим периодом возможен
 * только в окне запуска. Профиль цели задаёт период, ширину окна и фазу
 * (в тиках игрового времени). Период 0 значит «всегда открыто» (Луна, Земля).
 */
public final class TransferWindows {

    private TransferWindows() {
    }

    public static boolean hasWindow(ModRegistries.PlanetProfile target) {
        return target.synodicPeriodTicks() > 0 && target.windowWidthTicks() > 0;
    }

    public static boolean isOpen(long gameTime, ModRegistries.PlanetProfile target) {
        if (!hasWindow(target)) {
            return true;
        }
        long phase = Math.floorMod(gameTime - target.windowPhaseTicks(), target.synodicPeriodTicks());
        return phase < target.windowWidthTicks();
    }

    /** Тиков до открытия следующего окна (0, если открыто сейчас). */
    public static long ticksToOpen(long gameTime, ModRegistries.PlanetProfile target) {
        if (!hasWindow(target) || isOpen(gameTime, target)) {
            return 0;
        }
        long phase = Math.floorMod(gameTime - target.windowPhaseTicks(), target.synodicPeriodTicks());
        return target.synodicPeriodTicks() - phase;
    }

    /** Тиков до закрытия текущего окна (0, если закрыто). */
    public static long ticksToClose(long gameTime, ModRegistries.PlanetProfile target) {
        if (!hasWindow(target) || !isOpen(gameTime, target)) {
            return 0;
        }
        long phase = Math.floorMod(gameTime - target.windowPhaseTicks(), target.synodicPeriodTicks());
        return target.windowWidthTicks() - phase;
    }
}
