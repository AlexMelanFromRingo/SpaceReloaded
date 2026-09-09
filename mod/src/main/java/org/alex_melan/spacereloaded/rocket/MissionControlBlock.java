package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;
import java.util.Locale;

/**
 * ЦУП: ПКМ — телеметрия всех бортов в радиусе 64 блока (статус, топливо,
 * высота); Sneak+ПКМ — карта полёта. Данные из честной физики.
 */
public class MissionControlBlock extends Block {

    private static final double RANGE = 64.0;

    public MissionControlBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(level instanceof ServerLevel serverLevel)
                || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }
        if (player.isSecondaryUseActive()) {
            org.alex_melan.spacereloaded.network.ModNetworking.sendPlanetMap(
                    serverLevel.getServer(), serverPlayer);
            return InteractionResult.SUCCESS_SERVER;
        }
        List<RocketEntity> rockets = serverLevel.getEntities(
                EntityTypeTest.forClass(RocketEntity.class),
                new AABB(pos).inflate(RANGE), entity -> true);
        List<org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity> terminals = terminalsInRange(serverLevel, pos);
        if (rockets.isEmpty() && terminals.isEmpty()) {
            serverPlayer.sendSystemMessage(
                    Component.translatable("message.spacereloaded.mission_control.empty"));
            return InteractionResult.SUCCESS_SERVER;
        }
        // 003 (FR-117): грузовые терминалы в радиусе — состояние линий
        for (var terminal : terminals) {
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.mission_control.terminal",
                    terminal.getBlockPos().toShortString(),
                    Component.translatable(terminal.stateKey()), terminal.detail(),
                    terminal.departures(), terminal.arrivals()));
        }
        serverPlayer.sendSystemMessage(Component.translatable(
                "message.spacereloaded.mission_control.header", rockets.size()));
        int index = 1;
        for (RocketEntity rocket : rockets) {
            String status = rocket.isParked() ? "▮" : "▲";
            serverPlayer.sendSystemMessage(Component.translatable(
                    "message.spacereloaded.mission_control.entry",
                    index++, status,
                    String.format(Locale.ROOT, "%.0f", rocket.propellantKg()),
                    String.format(Locale.ROOT, "%.0f", rocket.getY()),
                    rocket.isParked()
                            ? Component.translatable("message.spacereloaded.mission_control.parked")
                            : Component.translatable("message.spacereloaded.mission_control.flight")));
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Терминалы в загруженных чанках радиуса (обход блок-сущностей чанков — по клику, не по тику). */
    private static List<org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity> terminalsInRange(
            ServerLevel level, BlockPos center) {
        List<org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity> found = new java.util.ArrayList<>();
        int chunkRadius = (int) Math.ceil(RANGE / 16.0);
        int cx = center.getX() >> 4;
        int cz = center.getZ() >> 4;
        for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
            for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                var chunk = level.getChunkSource().getChunkNow(cx + dx, cz + dz);
                if (chunk == null) {
                    continue;
                }
                for (var blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity terminal
                            && terminal.getBlockPos().distSqr(center) <= RANGE * RANGE) {
                        found.add(terminal);
                    }
                }
            }
        }
        return found;
    }
}
