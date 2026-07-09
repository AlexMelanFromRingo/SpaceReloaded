package org.alex_melan.spacereloaded.sealing;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.geometry.LongHashSet;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.sealing.GasFloodFill;
import org.alex_melan.spacereloaded.core.sealing.SealingRequest;
import org.alex_melan.spacereloaded.core.sealing.SealingResult;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Оркестратор герметичных зон (T022, принципы III и IV).
 *
 * <p>Конвейер: событие → {@link #markBlockChanged} (O(1) по обратному индексу) →
 * отложенная очередь конца тика → снимок в главном потоке →
 * {@code GasFloodFill} в фоновом executor'е → применение результата через
 * {@code server.execute}. Пересчёты по одной зоне коалесцируются: пока job в
 * полёте, новые события лишь взводят флаг «пересчитать ещё раз».
 *
 * <p>Всё состояние менеджера читается/мутируется только в главном потоке;
 * фоновому потоку передаётся только иммутабельный снимок.
 */
public final class ZoneManager {

    private static final Map<ResourceKey<Level>, LevelZones> LEVELS = new HashMap<>();
    private static ExecutorService executor;

    private static final class LevelZones {
        final Map<BlockPos, SealedZone> zones = new HashMap<>();
        final Long2ObjectOpenHashMap<SealedZone> posIndex = new Long2ObjectOpenHashMap<>();
        final LongOpenHashSet deferredChanges = new LongOpenHashSet();
        final Set<BlockPos> jobsInFlight = new HashSet<>();
        final Set<BlockPos> dirtyAgain = new HashSet<>();
        boolean vacuumWorld = false;
    }

    /** Результат фоновой части: сам анализ + производные для главного потока. */
    private record Computation(SealingResult result, LongHashSet footprint,
                               int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
    }

    private ZoneManager() {
    }

    // ---------- Жизненный цикл ----------

    public static synchronized ExecutorService executor() {
        if (executor == null) {
            AtomicInteger counter = new AtomicInteger();
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, "SpaceReloaded-Sealing-" + counter.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            };
            executor = Executors.newFixedThreadPool(SpaceReloaded.config().sealingMaxConcurrentJobs, factory);
        }
        return executor;
    }

    public static void shutdown() {
        LEVELS.clear();
        synchronized (ZoneManager.class) {
            if (executor != null) {
                executor.shutdownNow();
                executor = null;
            }
        }
    }

    private static LevelZones levelZones(ServerLevel level) {
        return LEVELS.computeIfAbsent(level.dimension(), key -> new LevelZones());
    }

    // ---------- Контроллеры ----------

    public static void registerController(ServerLevel level, BlockPos pos) {
        LevelZones lz = levelZones(level);
        lz.zones.computeIfAbsent(pos.immutable(), SealedZone::new);
        requestRecalc(level, pos.immutable());
    }

    public static void removeController(ServerLevel level, BlockPos pos) {
        LevelZones lz = levelZones(level);
        SealedZone zone = lz.zones.remove(pos);
        if (zone != null) {
            removeFromIndex(lz, zone);
        }
        lz.dirtyAgain.remove(pos);
    }

    public static SealedZone zoneAt(ServerLevel level, BlockPos controllerPos) {
        return levelZones(level).zones.get(controllerPos);
    }

    /** Ближайшая зона к точке (по контроллеру) в пределах радиуса — для сканера. */
    public static SealedZone nearestZone(ServerLevel level, BlockPos pos, int radius) {
        SealedZone best = null;
        double bestSq = (double) radius * radius;
        for (var entry : levelZones(level).zones.entrySet()) {
            double distSq = entry.getKey().distSqr(pos);
            if (distSq <= bestSq) {
                bestSq = distSq;
                best = entry.getValue();
            }
        }
        return best;
    }

    // ---------- Режим вакуума (debug; позже — из профиля планеты) ----------

    public static boolean isVacuumWorld(ServerLevel level) {
        return levelZones(level).vacuumWorld
                || !org.alex_melan.spacereloaded.planet.PlanetManager.isBreathable(level);
    }

    public static void setVacuumWorld(ServerLevel level, boolean vacuum) {
        LevelZones lz = levelZones(level);
        if (lz.vacuumWorld != vacuum) {
            lz.vacuumWorld = vacuum;
            lz.zones.keySet().forEach(pos -> requestRecalc(level, pos));
        }
    }

    // ---------- Инвалидация по событиям (FR-004) ----------

    /** O(1): позиция интересна, только если попадает в индекс объёмов/границ. */
    public static void markBlockChanged(ServerLevel level, BlockPos pos) {
        levelZones(level).deferredChanges.add(pos.asLong());
    }

    /** Конец тика уровня: сводим накопленные изменения к множеству затронутых зон. */
    public static void processDeferred(ServerLevel level) {
        LevelZones lz = levelZones(level);
        if (lz.deferredChanges.isEmpty()) {
            return;
        }
        Set<BlockPos> affected = new HashSet<>();
        var it = lz.deferredChanges.iterator();
        while (it.hasNext()) {
            SealedZone zone = lz.posIndex.get(it.nextLong());
            if (zone != null) {
                affected.add(zone.controllerPos());
            }
        }
        lz.deferredChanges.clear();
        affected.forEach(pos -> requestRecalc(level, pos));
    }

    // ---------- Проверка принадлежности (для урона вакуума, D10) ----------

    public static boolean isInsideSealedZone(ServerLevel level, BlockPos pos) {
        SealedZone zone = levelZones(level).posIndex.get(pos.asLong());
        return zone != null && zone.isSealed() && zone.volume().contains(pos.asLong());
    }

    // ---------- Конвейер пересчёта (FR-005) ----------

    public static void requestRecalc(ServerLevel level, BlockPos controllerPos) {
        MinecraftServer server = level.getServer();
        if (!server.isSameThread()) {
            throw new IllegalStateException("requestRecalc must run on the server thread");
        }
        LevelZones lz = levelZones(level);
        if (!lz.zones.containsKey(controllerPos)) {
            return;
        }
        if (!lz.jobsInFlight.add(controllerPos)) {
            lz.dirtyAgain.add(controllerPos); // коалесинг: job уже в полёте
            return;
        }

        int radius = SpaceReloaded.config().sealingMaxRadius;
        BlockPos origin = controllerPos.above();
        RegionSnapshot snapshot = RegionSnapshot.capture(level, origin, radius, isVacuumWorld(level));
        SealingRequest request = SealingRequest.fast(
                PackedPos.pack(origin.getX(), origin.getY(), origin.getZ()), radius,
                SpaceReloaded.config().sealingDiagonalLeaks);
        ResourceKey<Level> dimension = level.dimension();

        CompletableFuture
                .supplyAsync(() -> compute(snapshot, request), executor())
                .whenComplete((computation, error) -> server.execute(() -> {
                    ServerLevel liveLevel = server.getLevel(dimension);
                    if (liveLevel != null) {
                        applyResult(liveLevel, controllerPos, computation, error);
                    }
                }));
    }

    /** Фоновая часть: поиск + производные структуры. Мир не трогает (снимок). */
    private static Computation compute(RegionSnapshot snapshot, SealingRequest request) {
        SealingResult result = GasFloodFill.analyze(snapshot, request);

        // Footprint — все ячейки, изменение которых должно инвалидировать зону:
        // объём + его 26-граница + точки утечки (их починка должна перезапустить проверку)
        LongHashSet footprint = new LongHashSet(result.volume().size() * 2 + 16);
        footprint.addAll(result.volume());
        footprint.addAll(SealedZone.computeBoundary(result.volume()));
        footprint.addAll(result.leakPoints());
        footprint.addAll(result.escapePoints());

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        var it = result.volume().iterator();
        while (it.hasNext()) {
            long cell = it.nextLong();
            minX = Math.min(minX, PackedPos.unpackX(cell));
            minY = Math.min(minY, PackedPos.unpackY(cell));
            minZ = Math.min(minZ, PackedPos.unpackZ(cell));
            maxX = Math.max(maxX, PackedPos.unpackX(cell));
            maxY = Math.max(maxY, PackedPos.unpackY(cell));
            maxZ = Math.max(maxZ, PackedPos.unpackZ(cell));
        }
        return new Computation(result, footprint, minX, minY, minZ, maxX, maxY, maxZ);
    }

    /** Применение результата — строго в главном потоке. */
    private static void applyResult(ServerLevel level, BlockPos controllerPos,
                                    Computation computation, Throwable error) {
        LevelZones lz = levelZones(level);
        lz.jobsInFlight.remove(controllerPos);

        SealedZone zone = lz.zones.get(controllerPos);
        if (zone != null && error == null) {
            boolean wasSealed = zone.isSealed();
            removeFromIndex(lz, zone);
            org.alex_melan.spacereloaded.core.geometry.LongHashSet leaks =
                    new org.alex_melan.spacereloaded.core.geometry.LongHashSet(
                            computation.result().leakPoints().size()
                                    + computation.result().escapePoints().size() + 1);
            leaks.addAll(computation.result().leakPoints());
            leaks.addAll(computation.result().escapePoints());
            zone.update(computation.result().status(), computation.result().volume(),
                    computation.footprint(), leaks);
            addToIndex(lz, zone);

            BlockEntity blockEntity = level.getBlockEntity(controllerPos);
            if (blockEntity instanceof AtmosphereControllerBlockEntity controller) {
                controller.onZoneUpdated(computation.result());
            }

            if (wasSealed && !zone.isSealed()) {
                decompress(level, zone, computation);
            }
        } else if (error != null) {
            SpaceReloaded.LOGGER.error("Ошибка пересчёта зоны {}", controllerPos, error);
        }

        if (lz.dirtyAgain.remove(controllerPos)) {
            requestRecalc(level, controllerPos);
        }
    }

    private static void addToIndex(LevelZones lz, SealedZone zone) {
        var it = zone.footprint().iterator();
        while (it.hasNext()) {
            lz.posIndex.put(it.nextLong(), zone);
        }
    }

    private static void removeFromIndex(LevelZones lz, SealedZone zone) {
        var it = zone.footprint().iterator();
        while (it.hasNext()) {
            long cell = it.nextLong();
            if (lz.posIndex.get(cell) == zone) {
                lz.posIndex.remove(cell);
            }
        }
    }

    /** Взрывная декомпрессия (FR-007): импульс к пробоине, партиклы, звук. */
    private static void decompress(ServerLevel level, SealedZone zone, Computation computation) {
        SealingResult result = computation.result();
        long breach = firstOf(result.leakPoints(), result.escapePoints());
        if (breach == NO_BREACH || computation.minX() > computation.maxX()) {
            return;
        }
        Vec3 breachCenter = new Vec3(
                PackedPos.unpackX(breach) + 0.5,
                PackedPos.unpackY(breach) + 0.5,
                PackedPos.unpackZ(breach) + 0.5);

        AABB bounds = new AABB(computation.minX(), computation.minY(), computation.minZ(),
                computation.maxX() + 1, computation.maxY() + 1, computation.maxZ() + 1);
        double impulse = SpaceReloaded.config().decompressionImpulse;

        for (Entity entity : level.getEntities((Entity) null, bounds,
                e -> zone.volume().contains(e.blockPosition().asLong()))) {
            Vec3 direction = breachCenter.subtract(entity.position());
            if (direction.lengthSqr() > 1.0e-6) {
                entity.push(direction.normalize().scale(impulse));
                entity.hurtMarked = true;
            }
        }

        level.sendParticles(ParticleTypes.CLOUD,
                breachCenter.x, breachCenter.y, breachCenter.z, 40, 0.4, 0.4, 0.4, 0.08);
        level.playSound(null, BlockPos.containing(breachCenter.x, breachCenter.y, breachCenter.z),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.0f, 0.7f);
    }

    /** Long.MIN_VALUE зарезервирован в LongHashSet и не бывает валидной позицией. */
    private static final long NO_BREACH = Long.MIN_VALUE;

    private static long firstOf(LongHashSet primary, LongHashSet fallback) {
        if (!primary.isEmpty()) {
            return primary.iterator().nextLong();
        }
        if (!fallback.isEmpty()) {
            return fallback.iterator().nextLong();
        }
        return NO_BREACH;
    }
}
