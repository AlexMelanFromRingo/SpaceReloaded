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
                    .forGetter(s -> toList(s.interceptors))
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

    public SpaceNetworkState() {
        this(Map.of(), Map.of(), Map.of(), List.of(), List.of());
    }

    private SpaceNetworkState(Map<ResourceKey<Level>, Integer> coverage,
                             Map<ResourceKey<Level>, Long> stormUntil,
                             Map<ResourceKey<Level>, Integer> powerSats,
                             List<PosEntry> beaconFrequency, List<PosEntry> interceptors) {
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

    // --- Энергоспутники (Phase 14) ---

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
