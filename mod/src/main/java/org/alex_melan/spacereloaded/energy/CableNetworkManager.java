package org.alex_melan.spacereloaded.energy;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongArrayFIFOQueue;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModTags;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Кабельные сети (T032, принцип III): топология кэшируется, инвалидация по
 * событиям блоков; открытие сети — по требованию от машин (generator/потребитель
 * при тике регистрирует соседний кабель — кабелям не нужны block entities).
 *
 * <p>Раздача за тик: из всех extract-хранилищ на концах сети в insert-хранилища,
 * не более {@code cableThroughput} суммарно на сеть. Всё в главном потоке.
 */
public final class CableNetworkManager {

    private static final Map<ResourceKey<Level>, LevelNetworks> LEVELS = new HashMap<>();

    private static final class LevelNetworks {
        final Long2ObjectOpenHashMap<CableNetwork> byPos = new Long2ObjectOpenHashMap<>();
        final List<CableNetwork> networks = new ArrayList<>();
        final LongOpenHashSet dirty = new LongOpenHashSet();
    }

    private static final class CableNetwork {
        final LongOpenHashSet cables = new LongOpenHashSet();
    }

    private CableNetworkManager() {
    }

    private static LevelNetworks levelNetworks(ServerLevel level) {
        return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelNetworks());
    }

    public static void clearAll() {
        LEVELS.clear();
    }

    /** Из обработчиков событий блоков (вместе с ZoneManager.markBlockChanged). */
    public static void markBlockChanged(ServerLevel level, BlockPos pos) {
        levelNetworks(level).dirty.add(pos.asLong());
    }

    /** Машины вызывают на тике для своих кабельных соседей — ленивое открытие сети. */
    public static void ensureNetwork(ServerLevel level, BlockPos cablePos) {
        LevelNetworks ln = levelNetworks(level);
        if (!ln.byPos.containsKey(cablePos.asLong())) {
            rebuildFrom(level, ln, cablePos.asLong());
        }
    }

    /** Конец тика: структурные изменения → перестройка затронутых сетей. */
    public static void processDirty(ServerLevel level) {
        LevelNetworks ln = levelNetworks(level);
        if (ln.dirty.isEmpty()) {
            return;
        }
        LongOpenHashSet seeds = new LongOpenHashSet();
        var it = ln.dirty.iterator();
        while (it.hasNext()) {
            long pos = it.nextLong();
            boolean isCable = isCable(level, pos);
            boolean wasCable = ln.byPos.containsKey(pos);
            if (isCable == wasCable) {
                continue; // не структурное изменение сети
            }
            // Сброс всех сетей, касающихся точки, и повторный обход от соседей
            dropNetworkAt(ln, pos);
            if (isCable) {
                seeds.add(pos);
            }
            for (Direction dir : Direction.values()) {
                long neighbor = BlockPos.asLong(
                        BlockPos.getX(pos) + dir.getStepX(),
                        BlockPos.getY(pos) + dir.getStepY(),
                        BlockPos.getZ(pos) + dir.getStepZ());
                if (ln.byPos.containsKey(neighbor)) {
                    dropNetworkAt(ln, neighbor);
                }
                if (isCable(level, neighbor)) {
                    seeds.add(neighbor);
                }
            }
        }
        ln.dirty.clear();
        var seedIt = seeds.iterator();
        while (seedIt.hasNext()) {
            long seed = seedIt.nextLong();
            if (!ln.byPos.containsKey(seed)) {
                rebuildFrom(level, ln, seed);
            }
        }
    }

    /** Раздача энергии по всем сетям уровня. */
    public static void tick(ServerLevel level) {
        LevelNetworks ln = levelNetworks(level);
        if (ln.networks.isEmpty()) {
            return;
        }
        long throughput = SpaceReloaded.config().cableThroughput;
        for (CableNetwork network : ln.networks) {
            distributeEnergy(level, network, throughput);
        }
    }

    /**
     * Фазовая раздача (фикс «карусели» между батареями): хранилища делятся на
     * генераторы (только отдача), потребителей (только приём) и буферы-батареи
     * (и то и другое). Порядок: генераторы → потребители, излишек → батареи
     * (наименее заполненная первой), дефицит → из батарей (наиболее заполненная
     * первой). В конце буферы ВЫРАВНИВАЮТСЯ к средней доле заряда — но только
     * при разнице долей > 5% (гистерезис против вечного перекачивания).
     */
    private static void distributeEnergy(ServerLevel level, CableNetwork network, long budget) {
        List<EnergyStorage> sources = new ArrayList<>();
        List<EnergyStorage> sinks = new ArrayList<>();
        List<EnergyStorage> buffers = new ArrayList<>();

        var it = network.cables.iterator();
        while (it.hasNext()) {
            long cable = it.nextLong();
            BlockPos cablePos = BlockPos.of(cable);
            // Хранилище на самой клетке сети (РИТЭГ-кондуит) — тоже участник
            classify(EnergyStorage.SIDED.find(level, cablePos, Direction.UP),
                    sources, sinks, buffers);
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = cablePos.relative(dir);
                if (network.cables.contains(neighborPos.asLong())) {
                    continue;
                }
                classify(EnergyStorage.SIDED.find(level, neighborPos, dir.getOpposite()),
                        sources, sinks, buffers);
            }
        }
        if (sources.isEmpty() && buffers.isEmpty()) {
            return;
        }
        try (Transaction transaction = Transaction.openOuter()) {
            // 1. Генераторы кормят машины напрямую
            budget = moveAll(sources, sinks, budget, transaction);
            // 2. Излишек генерации — в батареи, наименее заполненная первой
            buffers.sort(java.util.Comparator.comparingDouble(CableNetworkManager::fillFraction));
            budget = moveAll(sources, buffers, budget, transaction);
            // 3. Дефицит машин покрывают батареи, наиболее заполненная первой
            java.util.Collections.reverse(buffers);
            budget = moveAll(buffers, sinks, budget, transaction);
            // 4. Выравнивание батарей между собой (гистерезис 5%)
            if (buffers.size() >= 2 && budget > 0) {
                EnergyStorage rich = buffers.get(0);
                EnergyStorage poor = buffers.get(buffers.size() - 1);
                double richFraction = fillFraction(rich);
                double poorFraction = fillFraction(poor);
                if (richFraction - poorFraction > 0.05 && poor.getCapacity() > 0) {
                    double average = (richFraction + poorFraction) / 2.0;
                    long toMove = (long) ((average - poorFraction) * poor.getCapacity());
                    EnergyStorageUtil.move(rich, poor, Math.min(budget, toMove), transaction);
                }
            }
            transaction.commit();
        }
    }

    private static void classify(EnergyStorage storage, List<EnergyStorage> sources,
                                 List<EnergyStorage> sinks, List<EnergyStorage> buffers) {
        if (storage == null) {
            return;
        }
        boolean extracts = storage.supportsExtraction();
        boolean inserts = storage.supportsInsertion();
        List<EnergyStorage> bucket = extracts && inserts ? buffers
                : extracts ? sources
                : inserts ? sinks
                : null;
        if (bucket != null && !bucket.contains(storage)) {
            bucket.add(storage);
        }
    }

    private static double fillFraction(EnergyStorage storage) {
        long capacity = storage.getCapacity();
        return capacity <= 0 ? 0 : (double) storage.getAmount() / capacity;
    }

    private static long moveAll(List<EnergyStorage> from, List<EnergyStorage> to,
                                long budget, Transaction transaction) {
        if (budget <= 0) {
            return 0;
        }
        for (EnergyStorage provider : from) {
            for (EnergyStorage receiver : to) {
                if (provider == receiver) {
                    continue;
                }
                budget -= EnergyStorageUtil.move(provider, receiver, budget, transaction);
                if (budget <= 0) {
                    return 0;
                }
            }
        }
        return budget;
    }

    private static void dropNetworkAt(LevelNetworks ln, long pos) {
        CableNetwork network = ln.byPos.get(pos);
        if (network == null) {
            return;
        }
        ln.networks.remove(network);
        var it = network.cables.iterator();
        while (it.hasNext()) {
            ln.byPos.remove(it.nextLong());
        }
    }

    /** BFS по кабелям (6-связность) от затравки; только прогруженные чанки. */
    private static void rebuildFrom(ServerLevel level, LevelNetworks ln, long seed) {
        if (!isCable(level, seed)) {
            return;
        }
        CableNetwork network = new CableNetwork();
        LongArrayFIFOQueue queue = new LongArrayFIFOQueue();
        queue.enqueue(seed);
        network.cables.add(seed);
        while (!queue.isEmpty()) {
            long current = queue.dequeueLong();
            for (Direction dir : Direction.values()) {
                long neighbor = BlockPos.asLong(
                        BlockPos.getX(current) + dir.getStepX(),
                        BlockPos.getY(current) + dir.getStepY(),
                        BlockPos.getZ(current) + dir.getStepZ());
                if (network.cables.contains(neighbor) || !isCable(level, neighbor)) {
                    continue;
                }
                network.cables.add(neighbor);
                queue.enqueue(neighbor);
            }
        }
        var it = network.cables.iterator();
        while (it.hasNext()) {
            long pos = it.nextLong();
            CableNetwork previous = ln.byPos.put(pos, network);
            if (previous != null && previous != network) {
                ln.networks.remove(previous);
            }
        }
        ln.networks.add(network);
    }

    private static boolean isCable(ServerLevel level, long packedPos) {
        BlockPos pos = BlockPos.of(packedPos);
        if (level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) == null) {
            return false;
        }
        return level.getBlockState(pos).is(ModTags.ENERGY_CONDUIT);
    }
}
