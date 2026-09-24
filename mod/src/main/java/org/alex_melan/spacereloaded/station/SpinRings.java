package org.alex_melan.spacereloaded.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;

/**
 * Реестр вращающихся колец уровня и их действие на игроков (007, FR-517/FR-520, D77):
 * гравитация ω²·r по высоте ниже ступицы, Кориолис a = −2ω×v каждый второй тик (горячий путь —
 * только игроки внутри границ сборки), выход из кольца — касательная скорость ω×ρ.
 */
public final class SpinRings {

    private static final Map<ResourceKey<Level>, Set<Long>> HUBS = new HashMap<>();
    private static final Map<UUID, Long> RIDERS = new HashMap<>();
    /** Последняя ω, отправленная клиенту для вращения неба. */
    private static final Map<UUID, Double> SKY = new HashMap<>();

    private SpinRings() {
    }

    public static void clearAll() {
        HUBS.clear();
        RIDERS.clear();
        SKY.clear();
    }

    public static void register(ServerLevel level, BlockPos hub) {
        HUBS.computeIfAbsent(level.dimension(), k -> new HashSet<>()).add(hub.asLong());
    }

    /** Блок изменился — пересобрать кольца, чьи границы его касаются. */
    public static void markBlockChanged(ServerLevel level, BlockPos pos) {
        Set<Long> hubs = HUBS.get(level.dimension());
        if (hubs == null) {
            return;
        }
        for (long h : hubs) {
            if (level.getBlockEntity(BlockPos.of(h)) instanceof SpinHubBlockEntity hub
                    && (hub.bounds() == null || hub.bounds().inflate(1).contains(Vec3.atCenterOf(pos)))) {
                hub.markDirty();
            }
        }
    }

    private static SpinHubBlockEntity hubFor(ServerLevel level, ServerPlayer player) {
        Set<Long> hubs = HUBS.get(level.dimension());
        if (hubs == null) {
            return null;
        }
        for (long h : Set.copyOf(hubs)) {
            if (!(level.getBlockEntity(BlockPos.of(h)) instanceof SpinHubBlockEntity hub)) {
                hubs.remove(h);
                continue;
            }
            if (hub.isolated() && hub.bounds() != null && hub.bounds().contains(player.position())
                    && player.getY() < BlockPos.of(h).getY()) {
                return hub;
            }
        }
        return null;
    }

    /** Гравитация игрока в кольце (или пусто — действует гравитация тела). */
    public static OptionalDouble gravityFor(ServerLevel level, ServerPlayer player) {
        SpinHubBlockEntity hub = hubFor(level, player);
        return hub == null ? OptionalDouble.empty() : OptionalDouble.of(hub.gravityAt(player.getY()));
    }

    /** Каждый тик: Кориолис и выход с обода. */
    public static void tick(ServerLevel level) {
        if (HUBS.getOrDefault(level.dimension(), Set.of()).isEmpty()) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            SpinHubBlockEntity hub = hubFor(level, player);
            Long was = RIDERS.get(player.getUUID());
            if (hub == null) {
                if (was != null && level.getBlockEntity(BlockPos.of(was)) instanceof SpinHubBlockEntity old) {
                    fling(player, old);
                }
                RIDERS.remove(player.getUUID());
                if (SKY.remove(player.getUUID()) != null) {
                    net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                            new org.alex_melan.spacereloaded.network.SpinSkyPayload(0, 0f));
                }
                continue;
            }
            RIDERS.put(player.getUUID(), hub.getBlockPos().asLong());
            Double sent = SKY.get(player.getUUID());
            if (sent == null || Math.abs(sent - hub.omega()) > 1e-3) {
                SKY.put(player.getUUID(), hub.omega());
                net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(player,
                        new org.alex_melan.spacereloaded.network.SpinSkyPayload(
                                hub.axis() == Direction.Axis.X ? 1 : 2, (float) hub.omega()));
            }
            if (level.getGameTime() % 2 != 0 || hub.omega() == 0) {
                continue;
            }
            double w = hub.omega() / 20; // рад/тик
            Vec3 axis = hub.axis() == Direction.Axis.X ? new Vec3(w, 0, 0) : new Vec3(0, 0, w);
            Vec3 v = player.getDeltaMovement();
            Vec3 coriolis = axis.cross(v).scale(-2 * 2); // за 2 тика
            if (coriolis.lengthSqr() > 1e-8) {
                player.setDeltaMovement(v.add(coriolis));
                player.hurtMarked = true;
            }
        }
    }

    /** Вышел из кольца — сохраняет касательную скорость ω×ρ. */
    private static void fling(ServerPlayer player, SpinHubBlockEntity hub) {
        BlockPos c = hub.getBlockPos();
        Vec3 rho = player.position().subtract(Vec3.atCenterOf(c));
        double w = hub.omega() / 20;
        Vec3 axis = hub.axis() == Direction.Axis.X ? new Vec3(w, 0, 0) : new Vec3(0, 0, w);
        Vec3 tangential = axis.cross(rho);
        player.setDeltaMovement(player.getDeltaMovement().add(tangential));
        player.hurtMarked = true;
    }
}
