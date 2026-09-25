package org.alex_melan.spacereloaded.logistics;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.rocketry.FlightEnvironment;
import org.alex_melan.spacereloaded.core.rocketry.MissionPlanner;
import org.alex_melan.spacereloaded.core.rocketry.StageLayout;
import org.alex_melan.spacereloaded.planet.Navigation;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModRegistries;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Адаптер планировщика маршрута (003, D23): собирает хопы из реестра планет
 * (гравитация и аэропрофиль — из профилей, без загрузки измерений) и отдаёт
 * отчёт ядра плюс исходный остаток Δv для сообщений «нужно/есть».
 */
public final class MissionPlanning {

    /** Скорость прибытия над маяком при спуске, м/с (как в RocketEntity.postArrival). */
    public static final double ARRIVAL_SPEED_MS = 5.0;

    /**
     * @param report         отчёт ядра
     * @param initialDeltaVMs остаток Δv стека до маршрута
     * @param target         финальная цель (id записи планеты)
     * @param hops           число хопов
     */
    public record Plan(MissionPlanner.MissionReport report, double initialDeltaVMs, Identifier target, int hops) {
        public boolean feasible() {
            return report.feasible();
        }
    }

    private MissionPlanning() {
    }

    /**
     * План маршрута борта от текущего измерения к финальной цели.
     *
     * @return {@code null}, если маршрута нет (цель недостижима или уже здесь)
     */
    public static Plan plan(ServerLevel level, RocketEntity rocket, Identifier finalTarget) {
        StageLayout layout = rocket.layout();
        if (layout == null || finalTarget == null) {
            return null;
        }
        RegistryAccess access = level.registryAccess();
        Identifier here = Navigation.entryIdFor(access, level.dimension().identifier());
        List<Identifier> route = Navigation.route(access, here, finalTarget);
        if (route.size() < 2) {
            return null;
        }
        var config = SpaceReloaded.config();
        List<MissionPlanner.Leg> legs = new ArrayList<>(route.size() - 1);
        for (int i = 0; i + 1 < route.size(); i++) {
            var from = PlanetManager.profileById(access, route.get(i));
            var to = PlanetManager.profileById(access, route.get(i + 1));
            if (from.isEmpty() || to.isEmpty()) {
                return null;
            }
            ModRegistries.PlanetProfile fromProfile = from.get();
            ModRegistries.PlanetProfile toProfile = to.get();
            FlightEnvironment env = new FlightEnvironment(fromProfile.gravity(), fromProfile.aero().toCore());
            double startY = i == 0 ? rocket.getY()
                    : "platform".equals(fromProfile.arrival()) ? PlanetManager.ORBIT_PLATFORM_Y
                    : fromProfile.aero().datumY();
            // 009: межпланетное плечо — по Ламберту на сегодня (дата следующих плеч неизвестна —
            // тоже сегодняшняя, как и в 003 перелёт мгновенный)
            double transfer = org.alex_melan.spacereloaded.planet.TransferCosts.cost(access, fromProfile,
                    route.get(i + 1), level.getGameTime());
            boolean last = i + 2 == route.size();
            MissionPlanner.Landing landing = last && !"platform".equals(toProfile.arrival())
                    ? new MissionPlanner.Landing(toProfile.gravity(), config.arrivalHeightM, ARRIVAL_SPEED_MS)
                    : null;
            legs.add(new MissionPlanner.Leg(env, startY, fromProfile.transitionAltitude(), transfer,
                    config.rocketDragCoefficient, landing));
        }
        double[] fuel = rocket.stagePropellantSnapshot();
        int active = rocket.activeStage();
        double initial = MissionPlanner.remaining(layout, fuel, active);
        // Тир наведения (006): T1 — жёсткая программа тангажа (разомкнутая схема), T2 — бортовой
        // компьютер пересчитывает траекторию в полёте, запас меньше
        double marginPercent = rocket.guidanceTier() >= 2 ? config.guidedDeltaVMarginPercent
                : config.cargoLineDeltaVMarginPercent;
        MissionPlanner.MissionReport report = MissionPlanner.plan(layout, fuel, active, legs, marginPercent / 100.0);
        return new Plan(report, initial, finalTarget, legs.size());
    }

    /**
     * Отказ из-за нехватки на межпланетном плече (009, D93): сегодняшняя цена минус нехватка —
     * бюджет плеча; ближайший день, когда цена ≤ бюджета, или «в этом периоде не хватит».
     * {@code null} — нехватка не на межпланетном плече (обычное описание).
     */
    public static Component waitForWindow(ServerLevel level, Plan plan) {
        MissionPlanner.MissionReport report = plan.report();
        if (report.reason() != MissionPlanner.Reason.TRANSFER_SHORTFALL || !Double.isFinite(report.shortfallMs())) {
            return null;
        }
        RegistryAccess access = level.registryAccess();
        Identifier here = Navigation.entryIdFor(access, level.dimension().identifier());
        List<Identifier> route = Navigation.route(access, here, plan.target());
        int legIndex = report.legs().size() - 1;   // план обрывается на плече с нехваткой
        if (legIndex < 0 || legIndex + 1 >= route.size()) {
            return null;
        }
        var from = PlanetManager.profileById(access, route.get(legIndex));
        Identifier to = route.get(legIndex + 1);
        if (from.isEmpty() || !org.alex_melan.spacereloaded.planet.TransferCosts.celestial(access, from.get(), to)) {
            return null;
        }
        long now = level.getGameTime();
        double today = org.alex_melan.spacereloaded.planet.TransferCosts.cost(access, from.get(), to, now);
        double budget = today - report.shortfallMs();
        long at = org.alex_melan.spacereloaded.planet.TransferCosts.nextAffordableTick(access, from.get(), to, now, budget);
        Component planet = Component.translatable("planet.spacereloaded." + to.getPath());
        if (at < 0) {
            return Component.translatable("message.spacereloaded.mission.no_window", planet, fmt(today), fmt(budget));
        }
        long wait = at - now;
        return Component.translatable("message.spacereloaded.mission.wait_window", planet, fmt(today), fmt(budget),
                wait / 24000L, (wait % 24000L) / 1000L);
    }

    /** Суммарно требуемый Δv по отчёту: подъёмы + перелёты + посадки + нехватка. */
    public static double requiredDeltaV(MissionPlanner.MissionReport report) {
        double total = 0;
        for (MissionPlanner.LegReport leg : report.legs()) {
            total += leg.ascent().reachedTarget() ? leg.ascent().deltaVSpentToTargetMs() : 0;
            total += leg.transferDeltaVMs();
            total += Double.isFinite(leg.landingDeltaVMs()) ? leg.landingDeltaVMs() : 0;
        }
        if (Double.isFinite(report.shortfallMs())) {
            total += 0; // нехватка уже входит в transfer/landing хопа, где отказ
        }
        return total;
    }

    /** Человекочитаемая причина (ключи message.spacereloaded.mission.*). */
    public static Component describe(Plan plan) {
        MissionPlanner.MissionReport report = plan.report();
        String have = fmt(plan.initialDeltaVMs());
        String need = fmt(requiredDeltaV(report));
        return switch (report.reason()) {
            case OK -> Component.translatable("message.spacereloaded.mission.ok",
                    fmt(report.remainingDeltaVMs()), plan.hops());
            case ASCENT_UNREACHABLE -> Component.translatable("message.spacereloaded.mission.ascent_unreachable");
            case TRANSFER_SHORTFALL -> Component.translatable("message.spacereloaded.mission.transfer_shortfall",
                    fmt(report.shortfallMs()), need, have);
            case LANDING_SHORTFALL -> Component.translatable("message.spacereloaded.mission.landing_shortfall",
                    fmt(report.shortfallMs()), need, have);
            case LANDING_TWR -> {
                double twr = report.legs().isEmpty() ? 0 : report.legs().get(report.legs().size() - 1).landingTwr();
                yield Component.translatable("message.spacereloaded.mission.landing_twr",
                        String.format(Locale.ROOT, "%.2f", twr));
            }
        };
    }

    public static String fmt(double value) {
        return Double.isFinite(value) ? String.format(Locale.ROOT, "%.0f", value) : "∞";
    }
}
