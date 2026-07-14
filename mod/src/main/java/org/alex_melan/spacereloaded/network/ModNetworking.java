package org.alex_melan.spacereloaded.network;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * Каналы для экранов. До этого мод слал только пакеты с сервера; терминал
 * орудия и карта полёта требуют обратного направления.
 *
 * <p>Каждый серверный обработчик заново проверяет право игрока на действие:
 * клиент присылает координаты, но не полномочия.
 */
public final class ModNetworking {

    /** Дальше этого расстояния до блока клик считается подделкой. */
    private static final double REACH_SQ = 64.0;

    public static void init() {
        PayloadTypeRegistry.clientboundPlay().register(
                CannonStatePayload.TYPE, CannonStatePayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                ScanReportPayload.TYPE, ScanReportPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(
                PlanetMapPayload.TYPE, PlanetMapPayload.CODEC);

        PayloadTypeRegistry.serverboundPlay().register(
                CannonActionPayload.TYPE, CannonActionPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                OpenPlanetMapPayload.TYPE, OpenPlanetMapPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(
                SetDestinationPayload.TYPE, SetDestinationPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CannonActionPayload.TYPE,
                (payload, context) -> handleCannonAction(payload, context.player()));
        ServerPlayNetworking.registerGlobalReceiver(OpenPlanetMapPayload.TYPE,
                (payload, context) -> handlePlanetMapRequest(context.server(), context.player()));
        ServerPlayNetworking.registerGlobalReceiver(SetDestinationPayload.TYPE,
                (payload, context) -> handleSetDestination(payload, context.player()));
    }

    private static void handleCannonAction(CannonActionPayload payload, ServerPlayer player) {
        ServerLevel level = player.level() instanceof ServerLevel serverLevel ? serverLevel : null;
        if (level == null || !level.dimension().identifier().equals(payload.cannonDim())) {
            return; // терминал открывается только у пушки, стоящей рядом
        }
        if (player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(payload.cannonPos())) > REACH_SQ
                || !level.isLoaded(payload.cannonPos())
                || !(level.getBlockEntity(payload.cannonPos())
                        instanceof OrbitalCannonBlockEntity cannon)) {
            return;
        }
        if (payload.action() == CannonActionPayload.FIRE) {
            player.sendSystemMessage(cannon.tryFire(level));
        }
        ServerPlayNetworking.send(player, cannon.snapshot(level));
    }

    /** Открыть игроку карту полёта (клавиша, ЦУП). */
    public static void sendPlanetMap(net.minecraft.server.MinecraftServer server, ServerPlayer player) {
        handlePlanetMapRequest(server, player);
    }

    private static void handlePlanetMapRequest(net.minecraft.server.MinecraftServer server,
                                               ServerPlayer player) {
        var network = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(server);
        List<Identifier> covered = new ArrayList<>();
        for (ServerLevel level : server.getAllLevels()) {
            if (network.hasCoverage(level.dimension())) {
                covered.add(level.dimension().identifier());
            }
        }
        // Флаг = «этот игрок может задать курс», то есть он пилот (первый пассажир).
        // Попутчик в кресле карту видит, но кнопку назначения цели ему гасим.
        boolean isPilot = player.getVehicle() instanceof RocketEntity rocket
                && rocket.getFirstPassenger() == player;
        ServerPlayNetworking.send(player, new PlanetMapPayload(covered, isPilot));
    }

    private static void handleSetDestination(SetDestinationPayload payload, ServerPlayer player) {
        if (!(player.getVehicle() instanceof RocketEntity rocket)
                || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        // Курс задаёт только пилот (первый пассажир): попутчик в кресле не
        // должен перебивать маршрут, как это делает вся input-навигация ракеты
        if (rocket.getFirstPassenger() != player) {
            return;
        }
        rocket.setDestination(level, payload.target());
    }

    /** Ключ измерения по идентификатору: сервер не доверяет клиентской строке. */
    public static ResourceKey<Level> dimensionKey(Identifier id) {
        return ResourceKey.create(Registries.DIMENSION, id);
    }

    private ModNetworking() {
    }
}
