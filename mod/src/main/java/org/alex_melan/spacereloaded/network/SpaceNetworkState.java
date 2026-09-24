package org.alex_melan.spacereloaded.network;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.alex_melan.spacereloaded.SpaceReloaded;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Глобальное состояние орбитальной сети (Phase 12): спутниковое покрытие по
 * измерениям, окна пылевых бурь, частоты маяков и реестр перехватчиков.
 * Спутники — НЕ тикающие сущности, а записи здесь (Gemini-совет).
 * Хранится в SavedData оверворлда (сервер-глобально), переживает перезапуск.
 *
 * <p>Ключи по позиции (маяки/перехватчики) сериализуются СПИСКОМ записей, а не
 * {@code unboundedMap(GlobalPos.CODEC, ...)}: GlobalPos кодируется в CompoundTag,
 * а NBT-карта требует строковых ключей — иначе весь стейт не сохранился бы.
 */
public class SpaceNetworkState extends SavedData {

    private record PosEntry(GlobalPos pos, int value) {
        static final Codec<PosEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(PosEntry::pos),
                Codec.INT.fieldOf("value").forGetter(PosEntry::value)
        ).apply(instance, PosEntry::new));
    }

    /** Наземная антенна дальней связи (008): позиция, тело цели, скорость линии, бит/с. */
    private record LinkEntry(GlobalPos pos, Identifier target, double rate) {
        static final Codec<LinkEntry> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                GlobalPos.CODEC.fieldOf("pos").forGetter(LinkEntry::pos),
                Identifier.CODEC.fieldOf("target").forGetter(LinkEntry::target),
                Codec.DOUBLE.fieldOf("rate").forGetter(LinkEntry::rate)
        ).apply(instance, LinkEntry::new));
    }

    public static final Codec<SpaceNetworkState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, Codec.INT)
                    .optionalFieldOf("coverage", Map.of()).forGetter(s -> s.coverage),
            Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, Codec.LONG)
                    .optionalFieldOf("storm_until", Map.of()).forGetter(s -> s.stormUntil),
            Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, Codec.INT)
                    .optionalFieldOf("power_sats", Map.of()).forGetter(s -> s.powerSats),
            PosEntry.CODEC.listOf().optionalFieldOf("beacon_frequency", List.of())
                    .forGetter(s -> toList(s.beaconFrequency)),
            PosEntry.CODEC.listOf().optionalFieldOf("interceptors", List.of())
                    .forGetter(s -> toList(s.interceptors)),
            Codec.unboundedMap(Level.RESOURCE_KEY_CODEC, Codec.INT)
                    .optionalFieldOf("imaging_sats", Map.of()).forGetter(s -> s.imagingSats),
            LinkEntry.CODEC.listOf().optionalFieldOf("ground_links", List.of())
                    .forGetter(s -> List.copyOf(s.groundLinks.values()))
    ).apply(instance, SpaceNetworkState::new));

    public static final SavedDataType<SpaceNetworkState> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "network"),
            SpaceNetworkState::new, CODEC, DataFixTypes.LEVEL);

    private final Map<ResourceKey<Level>, Integer> coverage;
    private final Map<ResourceKey<Level>, Long> stormUntil;
    /** Частота (канал) маяка: 0 = открытый (может быть перехвачен). */
    private final Map<GlobalPos, Integer> beaconFrequency;
    /** Перехватчики: позиция дишa (и приёмник) → слушаемый канал (0 = открытые). */
    private final Map<GlobalPos, Integer> interceptors;

    private final Map<ResourceKey<Level>, Integer> powerSats;
    /** Спутники-камеры (007, US5): измерение-тело под орбитой → число аппаратов. */
    private final Map<ResourceKey<Level>, Integer> imagingSats;
    private final Map<GlobalPos, LinkEntry> groundLinks = new HashMap<>();

    public SpaceNetworkState() {
        this(Map.of(), Map.of(), Map.of(), List.of(), List.of(), Map.of(), List.of());
    }

    private SpaceNetworkState(Map<ResourceKey<Level>, Integer> coverage,
                             Map<ResourceKey<Level>, Long> stormUntil,
                             Map<ResourceKey<Level>, Integer> powerSats,
                             List<PosEntry> beaconFrequency, List<PosEntry> interceptors,
                             Map<ResourceKey<Level>, Integer> imagingSats, List<LinkEntry> links) {
        this.imagingSats = new HashMap<>(imagingSats);
        for (LinkEntry e : links) {
            groundLinks.put(e.pos(), e);
        }
        this.coverage = new HashMap<>(coverage);
        this.stormUntil = new HashMap<>(stormUntil);
        this.powerSats = new HashMap<>(powerSats);
        this.beaconFrequency = fromList(beaconFrequency);
        this.interceptors = fromList(interceptors);
    }

    private static List<PosEntry> toList(Map<GlobalPos, Integer> map) {
        return map.entrySet().stream().map(e -> new PosEntry(e.getKey(), e.getValue())).toList();
    }

    private static Map<GlobalPos, Integer> fromList(List<PosEntry> list) {
        Map<GlobalPos, Integer> map = new HashMap<>();
        for (PosEntry e : list) {
            map.put(e.pos(), e.value());
        }
        return map;
    }

    public static SpaceNetworkState get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // --- Покрытие спутниками ---

    public int coverage(ResourceKey<Level> dimension) {
        return coverage.getOrDefault(dimension, 0);
    }

    public void addCoverage(ResourceKey<Level> dimension) {
        coverage.merge(dimension, 1, Integer::sum);
        setDirty();
    }

    public boolean hasCoverage(ResourceKey<Level> dimension) {
        return coverage(dimension) > 0;
    }

    /** Прямая установка числа узлов покрытия (стенд, админ-команды); 0 удаляет запись. */
    public void setCoverage(ResourceKey<Level> dimension, int nodes) {
        if (nodes <= 0) {
            coverage.remove(dimension);
        } else {
            coverage.put(dimension, nodes);
        }
        setDirty();
    }

    // --- Энергоспутники (Phase 14) ---

    /** Линия наземной антенны (0 — нет связи: цель за горизонтом или антенна разобрана). */
    public void setGroundLink(GlobalPos antenna, Identifier target, double rateBps) {
        LinkEntry old = groundLinks.get(antenna);
        if (rateBps <= 0) {
            if (groundLinks.remove(antenna) != null) {
                setDirty();
            }
            return;
        }
        if (old == null || !old.target().equals(target) || Math.abs(old.rate() - rateBps) > 0.01 * rateBps) {
            groundLinks.put(antenna, new LinkEntry(antenna, target, rateBps));
            setDirty();
        }
    }

    /** Лучшая скорость наземной линии к телу, бит/с. */
    public double groundLinkRate(Identifier target) {
        double best = 0;
        for (LinkEntry e : groundLinks.values()) {
            if (e.target().equals(target)) {
                best = Math.max(best, e.rate());
            }
        }
        return best;
    }

    public int imagingSats(ResourceKey<Level> body) {
        return imagingSats.getOrDefault(body, 0);
    }

    /** Прямая установка (развёртывание — +1; стенд и админ-команды); 0 удаляет запись. */
    public void setImagingSats(ResourceKey<Level> body, int count) {
        if (count <= 0) {
            imagingSats.remove(body);
        } else {
            imagingSats.put(body, count);
        }
        setDirty();
    }

    public int powerSats(ResourceKey<Level> orbit) {
        return powerSats.getOrDefault(orbit, 0);
    }

    public void addPowerSat(ResourceKey<Level> orbit) {
        powerSats.merge(orbit, 1, Integer::sum);
        setDirty();
    }

    // --- Пылевые бури ---

    public boolean stormActive(ResourceKey<Level> dimension, long gameTime) {
        return gameTime < stormUntil.getOrDefault(dimension, 0L);
    }

    public void startStorm(ResourceKey<Level> dimension, long untilTick) {
        stormUntil.put(dimension, untilTick);
        setDirty();
    }

    // --- Защищённая маршрутизация (Phase 12 CTF) ---

    public int beaconFrequency(GlobalPos beacon) {
        return beaconFrequency.getOrDefault(beacon, 0);
    }

    public void secureBeacon(GlobalPos beacon, int frequency) {
        if (frequency == 0) {
            beaconFrequency.remove(beacon);
        } else {
            beaconFrequency.put(beacon, frequency);
        }
        setDirty();
    }

    public void registerInterceptor(GlobalPos dish, int listenFrequency) {
        if (!Objects.equals(interceptors.get(dish), listenFrequency)) {
            interceptors.put(dish, listenFrequency);
            setDirty(); // помечаем грязным только при реальном изменении
        }
    }

    public void unregisterInterceptor(GlobalPos dish) {
        if (interceptors.remove(dish) != null) {
            setDirty();
        }
    }

    public Map<GlobalPos, Integer> interceptors() {
        return interceptors;
    }
}
