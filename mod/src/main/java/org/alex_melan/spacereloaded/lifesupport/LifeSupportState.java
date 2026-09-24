package org.alex_melan.spacereloaded.lifesupport;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere;
import org.alex_melan.spacereloaded.core.lifesupport.Metabolism;
import org.alex_melan.spacereloaded.core.sealing.FirstOrderMix;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.sealing.SealedZone;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Газ герметичных зон (007, FR-501…FR-503, D70). Каждая зона — отрезок замкнутой формулы
 * ({@link Gas}): массы O₂/N₂/CO₂ в момент t₀ и действующие на отрезке вклады — выдох и потребление
 * экипажа, насыщенный фотосинтез и сжигатели (притоки), поглотители и фотосинтез ниже насыщения
 * (объёмное удаление k). CO₂ — первопорядковое перемешивание, O₂ — линейный расход плюс
 * производство растений ∝ ∫c dt. Время — игровые сутки (решение автора: биология ×72).
 * <p>
 * Отрезок меняется только по событиям: пересчёт зоны (герметизация, пробой, объём), раз в секунду
 * сверка суммы вкладов (игроки и животные в зоне, отметки машин и лотков), дозаправка газа
 * контроллером, перенос газа шлюзом. Состояние — SavedData уровня.
 */
public final class LifeSupportState extends SavedData {

    /** Отрезок газа зоны. */
    public record Gas(double mO2, double mN2, double mCo2, double t0, double volume,
                      double o2Rate, double co2Source, double co2Removal, double o2PerIntegral, double acc) {

        static final Codec<Gas> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.DOUBLE.fieldOf("o2").forGetter(Gas::mO2),
                Codec.DOUBLE.fieldOf("n2").forGetter(Gas::mN2),
                Codec.DOUBLE.fieldOf("co2").forGetter(Gas::mCo2),
                Codec.DOUBLE.fieldOf("t0").forGetter(Gas::t0),
                Codec.DOUBLE.fieldOf("v").forGetter(Gas::volume),
                Codec.DOUBLE.optionalFieldOf("o2_rate", 0.0).forGetter(Gas::o2Rate),
                Codec.DOUBLE.optionalFieldOf("co2_source", 0.0).forGetter(Gas::co2Source),
                Codec.DOUBLE.optionalFieldOf("co2_removal", 0.0).forGetter(Gas::co2Removal),
                Codec.DOUBLE.optionalFieldOf("o2_per_integral", 0.0).forGetter(Gas::o2PerIntegral),
                Codec.DOUBLE.optionalFieldOf("acc", 0.0).forGetter(Gas::acc)
        ).apply(i, Gas::new));

        static Gas empty(double t, double volume) {
            return new Gas(0, 0, 0, t, volume, 0, 0, 0, 0, 0);
        }

        /** Воздух с давлением p (кПа) и долями земного воздуха. */
        static Gas air(double t, double volume, double kpa) {
            double[] m = CabinAtmosphere.fill(kpa, CabinAtmosphere.AIR_O2_FRACTION, volume);
            double co2 = CabinAtmosphere.massFor(kpa * CabinAtmosphere.AIR_CO2_FRACTION, CabinAtmosphere.M_CO2,
                    volume, CabinAtmosphere.T_CABIN);
            return new Gas(m[0], m[1], co2, t, volume, 0, 0, 0, 0, 0);
        }

        /** ∫c dt CO₂ (кг·сут/м³) на [t₀, t]. */
        double co2Integral(double t) {
            return FirstOrderMix.integral(mCo2 / volume, co2Source, co2Removal, volume, t - t0);
        }

        /** Состояние в момент t (те же вклады). */
        public Gas at(double t) {
            double dt = Math.max(0, t - t0);
            double integral = co2Integral(t);
            double co2 = FirstOrderMix.at(mCo2 / volume, co2Source, co2Removal, volume, dt) * volume;
            double o2 = Math.max(0, mO2 - o2Rate * dt + o2PerIntegral * integral);
            return new Gas(o2, mN2, Math.max(0, co2), Math.max(t, t0), volume, o2Rate, co2Source, co2Removal,
                    o2PerIntegral, acc + integral);
        }

        Gas withSources(double o2Rate, double co2Source, double co2Removal, double o2PerIntegral) {
            return new Gas(mO2, mN2, mCo2, t0, volume, o2Rate, co2Source, co2Removal, o2PerIntegral, acc);
        }

        Gas add(double dO2, double dN2, double dCo2) {
            return new Gas(Math.max(0, mO2 + dO2), Math.max(0, mN2 + dN2), Math.max(0, mCo2 + dCo2), t0, volume,
                    o2Rate, co2Source, co2Removal, o2PerIntegral, acc);
        }

        Gas withVolume(double v) {
            return new Gas(mO2, mN2, mCo2, t0, Math.max(1, v), o2Rate, co2Source, co2Removal, o2PerIntegral, acc);
        }

        public double pO2() {
            return CabinAtmosphere.partialKpa(mO2, CabinAtmosphere.M_O2, volume, CabinAtmosphere.T_CABIN);
        }

        public double pN2() {
            return CabinAtmosphere.partialKpa(mN2, CabinAtmosphere.M_N2, volume, CabinAtmosphere.T_CABIN);
        }

        public double pCo2() {
            return CabinAtmosphere.partialKpa(mCo2, CabinAtmosphere.M_CO2, volume, CabinAtmosphere.T_CABIN);
        }

        public double pressure() {
            return pO2() + pN2() + pCo2();
        }

        /** Через сколько суток pCO₂ достигнет порога (∞ — никогда). */
        public double daysToCo2(double kpa) {
            return new CabinAtmosphere.Co2(mCo2, co2Source, co2Removal, volume).daysTo(kpa);
        }

        /** Через сколько суток кончится O₂ до порога гипоксии (при текущих вкладах, без дозаправки). */
        public double daysToO2(double kpa) {
            double target = CabinAtmosphere.massFor(kpa, CabinAtmosphere.M_O2, volume, CabinAtmosphere.T_CABIN);
            double net = o2Rate - o2PerIntegral * FirstOrderMix.steadyState(co2Source, co2Removal);
            if (mO2 <= target) {
                return 0;
            }
            return net > 0 ? (mO2 - target) / net : Double.POSITIVE_INFINITY;
        }
    }

    /** Вклад машины или лотка в газ зоны (отметка раз в секунду). */
    public record Contribution(double o2Source, double co2Source, double co2Removal, double o2PerIntegral) {
        public static final Contribution NONE = new Contribution(0, 0, 0, 0);
    }

    private static final Codec<Map<Long, Gas>> MAP_CODEC = Codec.unboundedMap(Codec.STRING, Gas.CODEC).xmap(m -> {
        Map<Long, Gas> out = new HashMap<>();
        m.forEach((k, v) -> out.put(Long.parseLong(k), v));
        return out;
    }, m -> {
        Map<String, Gas> out = new HashMap<>();
        m.forEach((k, v) -> out.put(Long.toString(k), v));
        return out;
    });

    private static final Codec<Map<Long, Long>> MERGED_CODEC = Codec.unboundedMap(Codec.STRING, Codec.LONG).xmap(m -> {
        Map<Long, Long> out = new HashMap<>();
        m.forEach((k, v) -> out.put(Long.parseLong(k), v));
        return out;
    }, m -> {
        Map<String, Long> out = new HashMap<>();
        m.forEach((k, v) -> out.put(Long.toString(k), v));
        return out;
    });

    public static final Codec<LifeSupportState> CODEC = RecordCodecBuilder.create(i -> i.group(
            MAP_CODEC.optionalFieldOf("zones", Map.of()).forGetter(s -> s.zones),
            MERGED_CODEC.optionalFieldOf("merged", Map.of()).forGetter(s -> s.merged)
    ).apply(i, LifeSupportState::new));

    public static final SavedDataType<LifeSupportState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "life_support"),
            LifeSupportState::new, CODEC, DataFixTypes.LEVEL);

    private static final Map<ResourceKey<Level>, Map<Long, Object[]>> CONTRIBUTIONS = new HashMap<>();
    /** Игроки, которым показан HUD газа (для сброса при выходе из зоны). */
    private static final java.util.Set<java.util.UUID> HUD_SHOWN = new java.util.HashSet<>();

    private final Map<Long, Gas> zones;
    /**
     * Слитые зоны: когда два владельца (контроллер и насос шлюза) видят один и тот же объём
     * (открыт внутренний люк тамбура), газ у них общий и хранится у первичного — иначе он
     * считался бы дважды. Ключ — вторичная зона, значение — первичная.
     */
    private final Map<Long, Long> merged;

    public LifeSupportState() {
        this(Map.of(), Map.of());
    }

    private LifeSupportState(Map<Long, Gas> zones, Map<Long, Long> merged) {
        this.zones = new HashMap<>(zones);
        this.merged = new HashMap<>(merged);
    }

    public static LifeSupportState get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    public static void clearTransient() {
        CONTRIBUTIONS.clear();
    }

    /** Время биологии: игровые сутки. */
    public static double days(ServerLevel level) {
        return level.getGameTime() / 24000.0;
    }

    /** Давление окружающей среды тела, кПа (атмосфера — земная, иначе вакуум). */
    public static double ambientKpa(ServerLevel level) {
        return ZoneManager.isVacuumWorld(level) ? 0 : CabinAtmosphere.SEA_LEVEL_KPA;
    }

    // ---------------- события ----------------

    /** Хук ZoneManager: зона пересчитана. */
    public static void onZoneUpdated(ServerLevel level, SealedZone zone) {
        LifeSupportState state = get(level);
        long key = zone.controllerPos().asLong();
        double now = days(level);
        double volume = Math.max(1, zone.volume().size());
        double ambient = ambientKpa(level);
        if (!zone.isSealed()) {
            // пробой: газ уходит критическим истечением за доли секунды; в атмосфере — внутри наружный воздух
            state.split(level, key, volume, now);
            state.zones.put(key, ambient > 0 ? Gas.air(now, volume, ambient) : Gas.empty(now, volume));
            state.setDirty();
            return;
        }
        Long primary = state.merged.get(key);
        if (primary != null) {
            SealedZone other = ZoneManager.zoneAt(level, BlockPos.of(primary));
            if (other != null && overlaps(zone, other)) {
                return; // всё ещё один объём — газ у первичной зоны
            }
            state.separate(key, primary, volume, now); // люк закрыт — зоны разошлись, газ делится по объёму
        }
        Gas gas = state.zones.get(key);
        if (gas == null) {
            // только что загерметизированная зона запирает то, что в ней было: воздух тела или вакуум
            gas = ambient > 0 ? Gas.air(now, volume, ambient) : Gas.empty(now, volume);
        } else {
            gas = gas.at(now);
        }
        state.zones.put(key, gas);
        state.split(level, key, volume, now);
        state.zones.put(key, state.zones.get(key).withVolume(volume));
        state.mergeOverlapping(level, zone, now);
        state.setDirty();
    }

    /** Первичная зона — у контроллера атмосферы (регулятор), иначе меньшая позиция. */
    private static boolean primaryOver(ServerLevel level, long a, long b) {
        boolean ca = level.getBlockEntity(BlockPos.of(a)) instanceof org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlockEntity;
        boolean cb = level.getBlockEntity(BlockPos.of(b)) instanceof org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlockEntity;
        return ca != cb ? ca : a < b;
    }

    private static boolean overlaps(SealedZone a, SealedZone b) {
        SealedZone small = a.volume().size() <= b.volume().size() ? a : b;
        SealedZone large = small == a ? b : a;
        var it = small.volume().iterator();
        while (it.hasNext()) {
            if (large.volume().contains(it.nextLong())) {
                return true;
            }
        }
        return false;
    }

    /** Найти тот же объём у другого владельца и слить газ. */
    private void mergeOverlapping(ServerLevel level, SealedZone zone, double now) {
        long key = zone.controllerPos().asLong();
        for (Long other : Set.copyOf(zones.keySet())) {
            if (other == key || merged.containsKey(other)) {
                continue;
            }
            SealedZone otherZone = ZoneManager.zoneAt(level, BlockPos.of(other));
            if (otherZone == null || !otherZone.isSealed() || !overlaps(zone, otherZone)) {
                continue;
            }
            long primary = primaryOver(level, key, other) ? key : other;
            long secondary = primary == key ? other : key;
            Gas p = zones.get(primary).at(now);
            Gas q = zones.get(secondary).at(now);
            zones.put(primary, p.add(q.mO2(), q.mN2(), q.mCo2()).withVolume(Math.max(p.volume(), zone.volume().size())));
            zones.put(secondary, Gas.empty(now, q.volume()));
            merged.put(secondary, primary);
            if (secondary == key) {
                return;
            }
        }
    }

    /** Вторичная зона отделилась: её доля газа — по объёму. */
    private void separate(long secondary, long primary, double volume, double now) {
        merged.remove(secondary);
        Gas p = zones.get(primary);
        if (p == null) {
            return;
        }
        p = p.at(now);
        double share = Math.min(1, volume / Math.max(volume, p.volume()));
        zones.put(primary, p.add(-p.mO2() * share, -p.mN2() * share, -p.mCo2() * share));
        zones.put(secondary, new Gas(p.mO2() * share, p.mN2() * share, p.mCo2() * share, now, volume, 0, 0, 0, 0, 0));
    }

    /** Первичная зона изменилась: отпустить вторичные, чьи объёмы разошлись. */
    private void split(ServerLevel level, long primary, double volume, double now) {
        SealedZone zone = ZoneManager.zoneAt(level, BlockPos.of(primary));
        for (Map.Entry<Long, Long> e : Set.copyOf(merged.entrySet())) {
            if (e.getValue() != primary) {
                continue;
            }
            SealedZone other = ZoneManager.zoneAt(level, BlockPos.of(e.getKey()));
            if (zone == null || other == null || !zone.isSealed() || !overlaps(zone, other)) {
                separate(e.getKey(), primary, other == null ? 1 : Math.max(1, other.volume().size()), now);
            }
        }
    }

    /** Контроллер сломан — газ зоны забыт. */
    public static void onZoneRemoved(ServerLevel level, BlockPos controller) {
        LifeSupportState state = get(level);
        long key = controller.asLong();
        state.merged.remove(key);
        Gas gas = state.zones.remove(key);
        // газ в объёме остаётся: его забирает первая вторичная зона того же объёма
        boolean handed = false;
        for (Map.Entry<Long, Long> e : Set.copyOf(state.merged.entrySet())) {
            if (e.getValue() == key) {
                state.merged.remove(e.getKey());
                if (!handed && gas != null) {
                    state.zones.put(e.getKey(), gas.at(days(level)));
                    handed = true;
                }
            }
        }
        state.setDirty();
    }

    /** Машина или лоток сообщают свой вклад (раз в секунду; устаревает через 2 с). */
    public static void contribute(ServerLevel level, BlockPos pos, Contribution contribution) {
        Map<Long, Object[]> map = CONTRIBUTIONS.computeIfAbsent(level.dimension(), k -> new HashMap<>());
        if (contribution == null || contribution.equals(Contribution.NONE)) {
            map.remove(pos.asLong());
        } else {
            map.put(pos.asLong(), new Object[] {contribution, level.getGameTime()});
        }
    }

    /** Раз в секунду: сверить вклады зон; при изменении — новый отрезок. */
    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        LifeSupportState state = level.getDataStorage().get(TYPE);
        if (state == null || state.zones.isEmpty()) {
            return;
        }
        double now = days(level);
        Map<Long, Object[]> contributions = CONTRIBUTIONS.getOrDefault(level.dimension(), Map.of());
        java.util.Set<java.util.UUID> reported = new java.util.HashSet<>();
        long tick = level.getGameTime();
        contributions.values().removeIf(c -> tick - (long) c[1] > 40);
        for (Map.Entry<Long, Gas> entry : Set.copyOf(state.zones.entrySet())) {
            if (state.merged.containsKey(entry.getKey())) {
                continue;
            }
            SealedZone zone = ZoneManager.zoneAt(level, BlockPos.of(entry.getKey()));
            if (zone == null || !zone.isSealed()) {
                continue;
            }
            double[] s = sources(level, zone, contributions);
            Gas gas = entry.getValue();
            // HUD газа: игрокам в зоне — текущие p, pO₂, pCO₂
            Gas current = gas.at(now);
            for (ServerPlayer player : level.players()) {
                if (zone.volume().contains(player.blockPosition().asLong()) && reported.add(player.getUUID())) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                            new org.alex_melan.spacereloaded.network.CabinGasPayload((float) current.pressure(),
                                    (float) current.pO2(), (float) current.pCo2()));
                }
            }
            // «Зелёный воздух»: экипаж в зоне, а растения покрывают весь его кислород
            if (s[0] <= 0 && s[4] > 0 && s[5] > 0) {
                for (ServerPlayer player : level.players()) {
                    if (zone.volume().contains(player.blockPosition().asLong())) {
                        org.alex_melan.spacereloaded.industry.IndustryAdvancements.award(player,
                                org.alex_melan.spacereloaded.industry.IndustryAdvancements.GREEN_AIR);
                    }
                }
            }
            if (s[0] != gas.o2Rate() || s[1] != gas.co2Source() || s[2] != gas.co2Removal()
                    || s[3] != gas.o2PerIntegral()) {
                state.zones.put(entry.getKey(), gas.at(now).withSources(s[0], s[1], s[2], s[3]));
                state.setDirty();
            }
        }
        for (ServerPlayer player : level.players()) {
            if (!reported.contains(player.getUUID()) && HUD_SHOWN.remove(player.getUUID())) {
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        new org.alex_melan.spacereloaded.network.CabinGasPayload(-1, 0, 0));
            }
        }
        HUD_SHOWN.addAll(reported);
    }

    /** {расход O₂ (кг/сут), приток CO₂ (кг/сут), удаление CO₂ (м³/сут), O₂ на ∫c}. */
    private static double[] sources(ServerLevel level, SealedZone zone, Map<Long, Object[]> contributions) {
        double people = 0;
        List<ServerPlayer> players = level.players();
        for (ServerPlayer player : players) {
            if (!player.isSpectator() && zone.volume().contains(player.blockPosition().asLong())) {
                people += 1;
            }
        }
        people += animals(level, zone);
        double o2 = people * Metabolism.O2_PER_DAY;
        double co2 = people * Metabolism.CO2_PER_DAY;
        double removal = 0;
        double o2PerIntegral = 0;
        for (Map.Entry<Long, Object[]> c : contributions.entrySet()) {
            if (zone.volume().contains(c.getKey()) || adjacent(zone, c.getKey())) {
                Contribution k = (Contribution) c.getValue()[0];
                o2 -= k.o2Source();
                co2 += k.co2Source();
                removal += k.co2Removal();
                o2PerIntegral += k.o2PerIntegral();
            }
        }
        double plantO2 = 0;
        for (Map.Entry<Long, Object[]> c : contributions.entrySet()) {
            if (zone.volume().contains(c.getKey()) || adjacent(zone, c.getKey())) {
                plantO2 += Math.max(0, ((Contribution) c.getValue()[0]).o2Source());
            }
        }
        return new double[] {o2, co2, removal, o2PerIntegral, people, plantO2};
    }

    /** Животные в зоне — «люди» по закону Клейбера. */
    private static double animals(ServerLevel level, SealedZone zone) {
        var it = zone.volume().iterator();
        if (!it.hasNext()) {
            return 0;
        }
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        while (it.hasNext()) {
            BlockPos p = BlockPos.of(it.nextLong());
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
        }
        double total = 0;
        for (LivingEntity mob : level.getEntitiesOfClass(LivingEntity.class,
                new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1),
                e -> !(e instanceof Player) && e.isAlive() && zone.volume().contains(e.blockPosition().asLong()))) {
            total += Metabolism.kleiber(AnimalMass.of(mob));
        }
        return total;
    }

    private static boolean adjacent(SealedZone zone, long pos) {
        BlockPos p = BlockPos.of(pos);
        for (Direction d : Direction.values()) {
            if (zone.volume().contains(p.relative(d).asLong())) {
                return true;
            }
        }
        return false;
    }

    // ---------------- запросы и изменения ----------------

    /** Зона воздуха в точке (машина граничит с объёмом — дышит им). */
    public static SealedZone zoneAround(ServerLevel level, BlockPos pos) {
        return org.alex_melan.spacereloaded.electronics.CleanroomTracker.zoneAround(level, pos);
    }

    /** Газ зоны сейчас (или null — зона не ведётся). */
    public static Gas now(ServerLevel level, SealedZone zone) {
        if (zone == null) {
            return null;
        }
        LifeSupportState state = level.getDataStorage().get(TYPE);
        if (state == null) {
            return null;
        }
        Gas gas = state.zones.get(state.resolve(zone.controllerPos().asLong()));
        return gas == null ? null : gas.at(days(level));
    }

    /** Добавить (или отнять) газ зоне сейчас: заправка, шлюз, растения. */
    public static void add(ServerLevel level, SealedZone zone, double dO2, double dN2, double dCo2) {
        LifeSupportState state = get(level);
        long key = state.resolve(zone.controllerPos().asLong());
        Gas gas = state.zones.get(key);
        if (gas == null) {
            return;
        }
        state.zones.put(key, gas.at(days(level)).add(dO2, dN2, dCo2));
        state.setDirty();
    }

    private long resolve(long key) {
        Long primary = merged.get(key);
        return primary == null ? key : primary;
    }

    /** Накопленный ∫c(CO₂) dt зоны на сейчас — для поглотителей (удалено = k·Δ∫c). */
    public static double co2Exposure(ServerLevel level, SealedZone zone) {
        Gas gas = now(level, zone);
        return gas == null ? 0 : gas.acc();
    }
}
