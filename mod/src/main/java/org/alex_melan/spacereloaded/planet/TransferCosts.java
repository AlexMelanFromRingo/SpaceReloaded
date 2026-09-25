package org.alex_melan.spacereloaded.planet;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.orbit.GameCalendar;
import org.alex_melan.spacereloaded.core.orbit.InterplanetaryTransfer;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Цена перелёта на дату (009, FR-711…FR-713, D92): если у обоих концов в профиле есть орбита и
 * они разные — дуга Ламберта на день календаря мира ({@link InterplanetaryTransfer#best}), иначе
 * таблица {@code transfer_delta_v} (Луна, спуск с орбиты, датапак-тела без орбиты). Множитель
 * сервера {@code transferDeltaVScale} — к обоим. Одинаково считается на сервере и клиенте
 * (профили синхронизируются), поэтому карта полётов показывает ту же цену, что спишет переход.
 *
 * <p>Кэш — по полусуткам небесной механики (92 тика): цена за это время меняется на доли процента.
 */
public final class TransferCosts {

    /** Шаг кэша, тики (½ сут небесной механики). */
    public static final long BUCKET_TICKS = 92;
    /** Шаг перебора дат при поиске минимума и доступного дня, сут. */
    public static final double SCAN_DAYS = 2;

    private record Key(Identifier from, Identifier to, long bucket) {
    }

    private static final Map<Key, InterplanetaryTransfer.Option> CACHE = new ConcurrentHashMap<>();

    private TransferCosts() {
    }

    /** Тело для Ламберта из профиля (пусто, если орбиты нет). */
    public static Optional<InterplanetaryTransfer.Body> body(ModRegistries.PlanetProfile profile) {
        if (profile == null || profile.transfer() == null || profile.transfer().orbit().isEmpty()) {
            return Optional.empty();
        }
        var t = profile.transfer();
        return Optional.of(new InterplanetaryTransfer.Body(t.orbit().get().elements(), t.mu(), t.departureRadius(),
                t.aerocapture()));
    }

    /** Перелёт считается небесной механикой (оба конца на своих гелиоцентрических орбитах). */
    public static boolean celestial(RegistryAccess access, ModRegistries.PlanetProfile from, Identifier to) {
        var target = PlanetManager.profileById(access, to);
        return target.isPresent() && celestial(from, target.get());
    }

    private static boolean celestial(ModRegistries.PlanetProfile from, ModRegistries.PlanetProfile to) {
        var a = body(from);
        var b = body(to);
        return a.isPresent() && b.isPresent() && !a.get().orbit().equals(b.get().orbit());
    }

    public static double day(long tick) {
        return GameCalendar.day(tick, SpaceReloaded.config().calendarEpochDayJ2000);
    }

    /** Лучший вариант перелёта на тик (без множителя сервера); пусто — перелёт табличный. */
    public static Optional<InterplanetaryTransfer.Option> option(RegistryAccess access, ModRegistries.PlanetProfile from,
                                                                 Identifier to, long tick) {
        var target = PlanetManager.profileById(access, to);
        if (target.isEmpty() || !celestial(from, target.get())) {
            return Optional.empty();
        }
        long bucket = Math.floorDiv(tick, BUCKET_TICKS);
        Key key = new Key(from.dimension(), to, bucket);
        var cached = CACHE.get(key);
        if (cached != null) {
            return Optional.of(cached);
        }
        if (CACHE.size() > 8192) {   // ≈ 10 пар × синодический период по полусуткам
            CACHE.clear();
        }
        var o = InterplanetaryTransfer.best(body(from).get(), body(target.get()).get(), day(bucket * BUCKET_TICKS));
        CACHE.put(key, o);
        return Optional.of(o);
    }

    /** Цена перелёта сегодня, м/с (с множителем сервера); 0 — записи нет. */
    public static double cost(RegistryAccess access, ModRegistries.PlanetProfile from, Identifier to, long tick) {
        double scale = SpaceReloaded.config().transferDeltaVScale;
        var o = option(access, from, to, tick);
        if (o.isPresent()) {
            return Double.isFinite(o.get().totalMs()) ? o.get().totalMs() * scale : Double.POSITIVE_INFINITY;
        }
        return from.transferDeltaVTo(to) * scale;
    }

    /** Синодический период пары, тики (0 — перелёт табличный). */
    public static long synodicTicks(RegistryAccess access, ModRegistries.PlanetProfile from, Identifier to) {
        var target = PlanetManager.profileById(access, to);
        if (target.isEmpty() || !celestial(from, target.get())) {
            return 0;
        }
        return GameCalendar.ticks(InterplanetaryTransfer.synodicDays(body(from).get(), body(target.get()).get()));
    }

    /** Минимум цены за синодический период от tick: {тик минимума, цена}; для табличного — {tick, цена}. */
    public static double[] minimumInPeriod(RegistryAccess access, ModRegistries.PlanetProfile from, Identifier to, long tick) {
        long span = synodicTicks(access, from, to);
        if (span <= 0) {
            return new double[] {tick, cost(access, from, to, tick)};
        }
        long step = Math.max(1, GameCalendar.ticks(SCAN_DAYS));
        double best = Double.POSITIVE_INFINITY;
        long at = tick;
        for (long t = tick; t <= tick + span; t += step) {
            double c = cost(access, from, to, t);
            if (c < best) {
                best = c;
                at = t;
            }
        }
        return new double[] {at, best};
    }

    /** Ближайший тик (не раньше tick), когда цена ≤ бюджета, в пределах синодического периода; −1 — не хватит. */
    public static long nextAffordableTick(RegistryAccess access, ModRegistries.PlanetProfile from, Identifier to,
                                          long tick, double budgetMs) {
        long span = synodicTicks(access, from, to);
        if (span <= 0) {
            return cost(access, from, to, tick) <= budgetMs ? tick : -1;
        }
        long step = Math.max(1, GameCalendar.ticks(SCAN_DAYS));
        for (long t = tick; t <= tick + span; t += step) {
            if (cost(access, from, to, t) <= budgetMs) {
                return t;
            }
        }
        return -1;
    }
}
