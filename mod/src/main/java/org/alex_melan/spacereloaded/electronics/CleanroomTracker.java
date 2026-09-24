package org.alex_melan.spacereloaded.electronics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.electronics.CleanroomAir;
import org.alex_melan.spacereloaded.network.MarsClimate;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModTags;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Чистые комнаты (006, FR-421, D64). Воздух каждой зоны — отрезок замкнутой формулы ядра
 * (C₀, t₀, G, Q, V, q, C_out) плюс накопленный интеграл ∫C dt до t₀; состояние хранится в
 * SavedData уровня и переживает перезапуск. Отрезок меняется только по событиям:
 * <ul>
 *   <li>пересчёт зоны (герметизация, люк, пробоина, модуль поставлен/снят) — хук ZoneManager;
 *       негерметичная зона подсасывает наружный воздух через каждую точку утечки
 *       ({@link #INFILTRATION_PER_LEAK} — проём 1 м² при сквозняке 0.5 м/с);</li>
 *   <li>смена источников — раз в секунду сверяется сумма G: люди в зоне (стоит / ходит / в
 *       скафандре с замкнутым контуром) и работающие машины (дробилка и пресс пылят, станки
 *       фаба почти нет); пока G не изменился, ничего не пересчитывается.</li>
 * </ul>
 * Шаг фаба берёт точную среднюю концентрацию как разность накопленных интегралов на концах
 * операции ({@link #exposure}, {@link Exposure#meanSince}) — при любом числе событий внутри.
 * Модули фильтрации пассивны: 150 Вт вентилятора — одна единица энергии мода (15 кДж) в 100 с,
 * ниже кванта энергосети.
 */
public final class CleanroomTracker extends SavedData {

    /** Воздух Земли, частиц ≥ 0.5 мкм на м³ (ISO 9). */
    public static final double EARTH_AIR = 3.5e7;
    /** Свежая атмосфера из электролизного кислорода — почти стерильна. */
    public static final double PROCESS_AIR = 1e3;
    /** Реголитовая пыль у безатмосферных тел (выносится на скафандрах через шлюз). */
    public static final double REGOLITH_DUST = 1e5;
    /** Расход одного модуля, м³/мин (реальный модуль 2×2 фута). */
    public static final double FFU_FLOW = 10;
    /** Подсос через точку утечки, м³/мин: проём ~1 м² при 0.5 м/с. */
    public static final double INFILTRATION_PER_LEAK = 30;
    /** Генерация частиц человеком, 1/мин: стоит / ходит, в скафандре. */
    public static final double PERSON_STANDING = 1e5;
    public static final double PERSON_WALKING = 5e6;
    public static final double PERSON_SUITED = 1e3;
    /** Машины, 1/мин: пылящие (дробилка, пресс) и прочие станки. */
    public static final double DUSTY_MACHINE = 1e8;
    public static final double MACHINE = 1e3;

    /** Воздух зоны: отрезок формулы и интеграл ∫C dt от начала отсчёта до t₀. */
    public record Air(CleanroomAir.State state, double acc) {
        static final Codec<CleanroomAir.State> STATE_CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("c0").forGetter(CleanroomAir.State::c0),
                Codec.DOUBLE.fieldOf("t0").forGetter(CleanroomAir.State::t0),
                Codec.DOUBLE.fieldOf("g").forGetter(CleanroomAir.State::source),
                Codec.DOUBLE.fieldOf("q").forGetter(CleanroomAir.State::flow),
                Codec.DOUBLE.fieldOf("v").forGetter(CleanroomAir.State::volume),
                Codec.DOUBLE.optionalFieldOf("infiltration", 0.0).forGetter(CleanroomAir.State::infiltration),
                Codec.DOUBLE.optionalFieldOf("outside", 0.0).forGetter(CleanroomAir.State::outside)
        ).apply(i, CleanroomAir.State::new));
        public static final Codec<Air> CODEC = RecordCodecBuilder.create(i -> i.group(
                STATE_CODEC.fieldOf("state").forGetter(Air::state),
                Codec.DOUBLE.fieldOf("acc").forGetter(Air::acc)
        ).apply(i, Air::new));

        double integralAt(double t) {
            return acc + state.integral(t);
        }

        Air evolve(double t, double source, double flow, double volume, double infiltration, double outside) {
            return new Air(state.evolve(t, source, flow, volume, infiltration, outside), integralAt(t));
        }
    }

    /** Отметка экспозиции: зона (или {@link #OUTSIDE}), ∫C dt и момент, мин. */
    public record Exposure(long zone, double integral, double minutes) {
        /**
         * Точная средняя концентрация между двумя отметками. Смена зоны за операцию (пробой,
         * перестройка комнаты) — пластина видела наружный воздух.
         */
        public double meanSince(Exposure start, double outside) {
            double dt = minutes - start.minutes;
            if (zone != start.zone || zone == OUTSIDE) {
                return outside;
            }
            return dt <= 0 ? 0 : (integral - start.integral) / dt;
        }
    }

    public static final long OUTSIDE = Long.MIN_VALUE;

    private static final Codec<Map<Long, Air>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Air.CODEC)
            .xmap(m -> {
                Map<Long, Air> out = new HashMap<>();
                m.forEach((k, v) -> out.put(Long.parseLong(k), v));
                return out;
            }, m -> {
                Map<String, Air> out = new HashMap<>();
                m.forEach((k, v) -> out.put(Long.toString(k), v));
                return out;
            });

    public static final Codec<CleanroomTracker> CODEC = RecordCodecBuilder.create(i -> i.group(
            MAP_CODEC.optionalFieldOf("zones", Map.of()).forGetter(t -> t.zones)
    ).apply(i, CleanroomTracker::new));

    public static final SavedDataType<CleanroomTracker> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "cleanroom_air"),
            CleanroomTracker::new, CODEC, DataFixTypes.LEVEL);

    /**
     * Работающие машины уровня: позиция → {генерация, тик отметки}. Работающая машина отмечается
     * раз в секунду; отметка старше двух секунд — машина сломана или выгружена.
     */
    private static final Map<ResourceKey<Level>, Map<Long, double[]>> MACHINES = new HashMap<>();
    private static final Map<UUID, Vec3> LAST_POSITION = new HashMap<>();

    private final Map<Long, Air> zones;

    public CleanroomTracker() {
        this(Map.of());
    }

    private CleanroomTracker(Map<Long, Air> zones) {
        this.zones = new HashMap<>(zones);
    }

    public static CleanroomTracker get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void clearTransient() {
        MACHINES.clear();
        LAST_POSITION.clear();
    }

    private static double minutes(ServerLevel level) {
        return level.getGameTime() / 1200.0;
    }

    /** Наружная концентрация измерения. */
    public static double outside(ServerLevel level) {
        boolean breathable = PlanetManager.profileFor(level).map(p -> p.breathable()).orElse(true);
        double base = breathable ? EARTH_AIR : REGOLITH_DUST;
        return MarsClimate.stormActive(level) ? base * 100 : base;
    }

    /**
     * Воздух, которым заполняется только что загерметизированная зона: на планете с атмосферой —
     * наружный; в вакууме зону наполняет контроллер атмосферы стерильным электролизным кислородом.
     */
    private static double initialAir(ServerLevel level) {
        return ZoneManager.isVacuumWorld(level) ? PROCESS_AIR : outside(level);
    }

    // ---------------- события ----------------

    /** Хук ZoneManager: зона пересчитана (герметизация, пробой, модуль, люк). */
    public static void onZoneUpdated(ServerLevel level, SealedZone zone) {
        CleanroomTracker tracker = get(level);
        long key = zone.controllerPos().asLong();
        double now = minutes(level);
        double volume = zone.volume().size();
        double flow = filters(level, zone) * FFU_FLOW;
        double source = sources(level, zone);
        boolean sealed = zone.isSealed();
        double infiltration = sealed ? 0 : INFILTRATION_PER_LEAK * Math.max(1, zone.leakPoints().size());
        double outside = outside(level);
        Air air = tracker.zones.get(key);
        if (air == null) {
            double c0 = sealed ? initialAir(level) : outside;
            air = new Air(new CleanroomAir.State(c0, now, source, flow, Math.max(1, volume), infiltration, outside), 0);
        } else {
            air = air.evolve(now, source, flow, Math.max(1, volume), infiltration, outside);
        }
        tracker.zones.put(key, air);
        tracker.setDirty();
    }

    /** Контроллер снят — воздух зоны забыт. */
    public static void onZoneRemoved(ServerLevel level, BlockPos controller) {
        CleanroomTracker tracker = get(level);
        if (tracker.zones.remove(controller.asLong()) != null) {
            tracker.setDirty();
        }
    }

    /** Машина сообщает о пуске/останове (генерация частиц, 0 — стоит). */
    public static void machine(ServerLevel level, BlockPos pos, double rate) {
        Map<Long, double[]> machines = MACHINES.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        if (rate <= 0) {
            machines.remove(pos.asLong());
        } else {
            machines.put(pos.asLong(), new double[] {rate, level.getGameTime()});
        }
    }

    /** Раз в секунду: сверить источники каждой зоны; при изменении G начать новый отрезок. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        CleanroomTracker tracker = level.getDataStorage().get(TYPE);
        if (tracker == null || tracker.zones.isEmpty()) {
            return;
        }
        double now = minutes(level);
        for (Map.Entry<Long, Air> entry : Set.copyOf(tracker.zones.entrySet())) {
            SealedZone zone = ZoneManager.zoneAt(level, BlockPos.of(entry.getKey()));
            if (zone == null) {
                continue; // контроллер в невыгруженном чанке — воздух ждёт
            }
            Air air = entry.getValue();
            double source = sources(level, zone);
            double outside = air.state().infiltration() > 0 ? outside(level) : air.state().outside();
            if (source != air.state().source() || outside != air.state().outside()) {
                tracker.zones.put(entry.getKey(), air.evolve(now, source, air.state().flow(), air.state().volume(),
                        air.state().infiltration(), outside));
                tracker.setDirty();
            }
        }
        for (ServerPlayer player : level.players()) {
            LAST_POSITION.put(player.getUUID(), player.position());
        }
    }

    // ---------------- запросы ----------------

    /**
     * Зона воздуха в точке. Машина — твёрдый блок и в объём не входит: она дышит воздухом зоны,
     * с которой граничит.
     */
    public static SealedZone zoneAround(ServerLevel level, BlockPos pos) {
        SealedZone zone = ZoneManager.zoneContaining(level, pos);
        if (zone != null) {
            return zone;
        }
        for (Direction d : Direction.values()) {
            zone = ZoneManager.zoneContaining(level, pos.relative(d));
            if (zone != null) {
                return zone;
            }
        }
        return null;
    }

    /** Концентрация частиц в точке сейчас, 1/м³. */
    public static double concentration(ServerLevel level, BlockPos pos) {
        SealedZone zone = zoneAround(level, pos);
        if (zone == null) {
            return outside(level);
        }
        Air air = get(level).zones.get(zone.controllerPos().asLong());
        return air == null ? outside(level) : air.state().at(minutes(level));
    }

    /** Отметка экспозиции в точке — для средней за операцию фаба. */
    public static Exposure exposure(ServerLevel level, BlockPos pos) {
        double now = minutes(level);
        SealedZone zone = zoneAround(level, pos);
        Air air = zone == null ? null : get(level).zones.get(zone.controllerPos().asLong());
        if (air == null) {
            return new Exposure(OUTSIDE, outside(level) * now, now);
        }
        return new Exposure(zone.controllerPos().asLong(), air.integralAt(now), now);
    }

    // ---------------- расчёт параметров ----------------

    private static int filters(ServerLevel level, SealedZone zone) {
        Set<Long> seen = new HashSet<>();
        var it = zone.volume().iterator();
        while (it.hasNext()) {
            BlockPos cell = BlockPos.of(it.nextLong());
            for (Direction d : Direction.values()) {
                BlockPos n = cell.relative(d);
                if (!seen.contains(n.asLong()) && level.getBlockState(n).is(ModBlocks.FAN_FILTER_UNIT)) {
                    seen.add(n.asLong());
                }
            }
        }
        return seen.size();
    }

    private static boolean suited(ServerPlayer player) {
        return player.getItemBySlot(EquipmentSlot.HEAD).is(ModTags.SPACE_SUIT)
                && player.getItemBySlot(EquipmentSlot.CHEST).is(ModTags.SPACE_SUIT)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModTags.SPACE_SUIT)
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModTags.SPACE_SUIT);
    }

    private static double sources(ServerLevel level, SealedZone zone) {
        double g = 0;
        List<ServerPlayer> players = level.players();
        for (ServerPlayer player : players) {
            if (player.isSpectator() || !zone.volume().contains(player.blockPosition().asLong())) {
                continue;
            }
            if (suited(player)) {
                g += PERSON_SUITED;
            } else {
                Vec3 last = LAST_POSITION.get(player.getUUID());
                // шаг за секунду > 0.3 блока — человек ходит (порог ниже медленного шага 0.8 м/с)
                boolean walking = last != null && last.distanceToSqr(player.position()) > 0.09;
                g += walking ? PERSON_WALKING : PERSON_STANDING;
            }
        }
        Map<Long, double[]> machines = MACHINES.get(level.dimension());
        if (machines != null) {
            long now = level.getGameTime();
            machines.values().removeIf(m -> now - (long) m[1] > 40);
            for (Map.Entry<Long, double[]> machine : machines.entrySet()) {
                if (zone.volume().contains(machine.getKey()) || adjacent(zone, machine.getKey())) {
                    g += machine.getValue()[0];
                }
            }
        }
        return g;
    }

    /** Машина — твёрдый блок: она не в объёме, а граничит с ним (стоит внутри комнаты). */
    private static boolean adjacent(SealedZone zone, long pos) {
        BlockPos p = BlockPos.of(pos);
        for (Direction d : Direction.values()) {
            if (zone.volume().contains(p.relative(d).asLong())) {
                return true;
            }
        }
        return false;
    }
}
