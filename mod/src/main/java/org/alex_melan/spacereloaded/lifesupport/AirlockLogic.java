package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.station.AirlockCycle;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.VacuumHazard;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Физика шлюзового цикла люка (007, FR-506/FR-507, D74). Люк под перепадом давления прижат
 * тоннами (F = Δp·A), поэтому группа люков открывается только после выравнивания:
 * <ul>
 *   <li>перепад ≤ 1 кПа — обычный короткий цикл клапана;</li>
 *   <li>выход из тамбура с насосом: откачка до 14 кПа за t = (V/S)·ln(p₀/p_стоп), газ — в баки у
 *       насоса, остаток стравливается при открытии;</li>
 *   <li>вход в тамбур из зоны (наддув): клапан выравнивает давления, газ переходит из зоны в
 *       тамбур с сохранением масс и состава;</li>
 *   <li>без насоса — газ высокой стороны теряется целиком при открытии.</li>
 * </ul>
 * Защитный интерлок: люк не начнёт сбрасывать давление к вакууму, пока на высокой стороне
 * кто-то без полного скафандра и маски.
 */
public final class AirlockLogic {

    /** Отложенное действие цикла: по позиции первого люка группы (переживает только сессию). */
    private static final Map<Long, Runnable> PENDING = new HashMap<>();

    private AirlockLogic() {
    }

    /** Сторона люка: зона (или null — снаружи) и давление, кПа. */
    private record Side(SealedZone zone, double kpa, BlockPos cell) {
    }

    /** Результат планирования: длительность цикла (тики) или отказ. */
    public record Plan(int ticks, Component refusal) {
    }

    private static Side side(ServerLevel level, BlockPos cell) {
        SealedZone zone = ZoneManager.zoneContaining(level, cell);
        if (zone != null && zone.isSealed()) {
            LifeSupportState.Gas gas = LifeSupportState.now(level, zone);
            return new Side(zone, gas == null ? 0 : gas.pressure(), cell);
        }
        return new Side(null, LifeSupportState.ambientKpa(level), cell);
    }

    /** Две стороны люка: противоположные соседи, не твёрдые и не люки. */
    private static Side[] sides(ServerLevel level, BlockPos pos) {
        for (Direction.Axis axis : Direction.Axis.values()) {
            BlockPos a = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
            BlockPos b = pos.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
            if (open(level, a) && open(level, b)) {
                return new Side[] {side(level, a), side(level, b)};
            }
        }
        return null;
    }

    private static boolean open(ServerLevel level, BlockPos cell) {
        var state = level.getBlockState(cell);
        return !(state.getBlock() instanceof org.alex_melan.spacereloaded.sealing.HermeticHatchBlock)
                && !state.isCollisionShapeFullBlock(level, cell);
    }

    /** Спланировать цикл группы; действие с газом выполнится по окончании. */
    public static Plan plan(ServerLevel level, BlockPos pos, List<BlockPos> group) {
        int base = SpaceReloaded.config().airlockCycleTicks;
        // стороны — по любому люку группы (вся группа разделяет одни и те же два объёма)
        Side[] s = sides(level, pos);
        for (int i = 0; s == null && i < group.size(); i++) {
            s = sides(level, group.get(i));
        }
        if (s == null || Math.abs(s[0].kpa - s[1].kpa) <= AirlockCycle.HATCH_OPEN_KPA) {
            return new Plan(base, null);
        }
        Side high = s[0].kpa > s[1].kpa ? s[0] : s[1];
        Side low = high == s[0] ? s[1] : s[0];
        if (low.kpa < org.alex_melan.spacereloaded.lifesupport.CrewHazard.VACUUM_KPA
                && SpaceReloaded.config().airlockSuitInterlock && high.zone != null && unsuited(level, high.zone)) {
            return new Plan(0, Component.translatable("message.spacereloaded.airlock_suit"));
        }
        long key = pos.asLong();
        if (high.zone != null && level.getBlockEntity(high.zone.controllerPos()) instanceof AirlockPumpBlockEntity pump) {
            double volume = high.zone.volume().size();
            double stop = Math.max(AirlockCycle.PUMP_STOP_KPA, low.kpa);
            double seconds = AirlockCycle.pumpSeconds(volume, pump.speed(), high.kpa, stop);
            if (pump.draw(AirlockPumpBlockEntity.energyFor(seconds))) {
                SealedZone zone = high.zone;
                double fraction = 1 - stop / high.kpa;
                PENDING.put(key, () -> {
                    LifeSupportState.Gas gas = LifeSupportState.now(level, zone);
                    if (gas == null) {
                        return;
                    }
                    double o2 = gas.mO2() * fraction;
                    double n2 = gas.mN2() * fraction;
                    double storedO2 = GasTankBlockEntity.pushToNeighbors(level, zone.controllerPos(), GasKind.OXYGEN, o2);
                    double storedN2 = GasTankBlockEntity.pushToNeighbors(level, zone.controllerPos(), GasKind.NITROGEN, n2);
                    LifeSupportState.add(level, zone, -storedO2, -storedN2, 0); // остальное уйдёт при открытии
                });
                notify(level, pos, Component.translatable("message.spacereloaded.airlock_pump",
                        String.format(Locale.ROOT, "%.0f", seconds),
                        String.format(Locale.ROOT, "%.2f", AirlockCycle.ventLoss(volume, stop))));
                return new Plan(Math.max(base, (int) Math.ceil(seconds * 20)), null);
            }
        }
        if (low.zone != null && high.zone != null) {
            // наддув тамбура из зоны: давления выравниваются, массы и состав сохраняются
            SealedZone from = high.zone;
            SealedZone to = low.zone;
            PENDING.put(key, () -> equalize(level, from, to));
        } else if (low.zone != null) {
            SealedZone to = low.zone;
            PENDING.put(key, () -> fillWithAmbient(level, to)); // наружный воздух тела заходит в тамбур
        }
        return new Plan(base, null);
    }

    /** Цикл сорван интерлоком — действие отменено. */
    public static void cancel(BlockPos pos) {
        PENDING.remove(pos.asLong());
    }

    /** По окончании цикла — выполнить действие с газом. */
    public static void complete(BlockPos pos) {
        Runnable action = PENDING.remove(pos.asLong());
        if (action != null) {
            action.run();
        }
    }

    private static void equalize(ServerLevel level, SealedZone from, SealedZone to) {
        LifeSupportState.Gas a = LifeSupportState.now(level, from);
        LifeSupportState.Gas b = LifeSupportState.now(level, to);
        if (a == null || b == null) {
            return;
        }
        double share = b.volume() / (a.volume() + b.volume());
        double o2 = a.mO2() * share - b.mO2() * (1 - share);
        double n2 = a.mN2() * share - b.mN2() * (1 - share);
        double co2 = a.mCo2() * share - b.mCo2() * (1 - share);
        LifeSupportState.add(level, from, -o2, -n2, -co2);
        LifeSupportState.add(level, to, o2, n2, co2);
    }

    private static void fillWithAmbient(ServerLevel level, SealedZone to) {
        double ambient = LifeSupportState.ambientKpa(level);
        LifeSupportState.Gas b = LifeSupportState.now(level, to);
        if (b == null || ambient <= 0) {
            return;
        }
        double[] m = org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.fill(ambient,
                org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.AIR_O2_FRACTION, b.volume());
        LifeSupportState.add(level, to, Math.max(0, m[0] - b.mO2()), Math.max(0, m[1] - b.mN2()), 0);
    }

    private static boolean unsuited(ServerLevel level, SealedZone zone) {
        for (ServerPlayer player : level.players()) {
            if (!player.isCreative() && !player.isSpectator()
                    && zone.volume().contains(player.blockPosition().asLong())
                    && !(VacuumHazard.hasFullSuit(player)
                    && player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                    .is(org.alex_melan.spacereloaded.registry.ModItems.OXYGEN_MASK))) {
                return true;
            }
        }
        return false;
    }

    private static void notify(ServerLevel level, BlockPos pos, Component message) {
        for (ServerPlayer player : level.players()) {
            if (player.blockPosition().closerThan(pos, 8)) {
                player.sendOverlayMessage(message);
            }
        }
    }
}
