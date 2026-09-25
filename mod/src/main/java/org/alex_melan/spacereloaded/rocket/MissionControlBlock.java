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
 * ЦУП: ПКМ — экран телеметрии бортов в радиусе 64 блока (статус, топливо,
 * высота, Δv), линий и спутников (010); Sneak+ПКМ — карта полёта; пустая карта в руке — заказ орбитального снимка (007). Данные из честной физики.
 */
public class MissionControlBlock extends Block implements org.alex_melan.spacereloaded.multiblock.BlockStatusProvider {

    private static final double RANGE = 64.0;

    public MissionControlBlock(Properties properties) {
        super(properties);
    }

    /** 007 (US5): пустая карта — заказ орбитального снимка; Sneak — выбор масштаба. */
    @Override
    protected InteractionResult useItemOn(net.minecraft.world.item.ItemStack stack, BlockState state, Level level,
                                          BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
                                          BlockHitResult hit) {
        if (!org.alex_melan.spacereloaded.orbit.OrbitalImages.isBlankMap(stack)
                && !org.alex_melan.spacereloaded.orbit.OrbitalImages.isBlankMineralMap(stack)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            if (player.isSecondaryUseActive()) {
                org.alex_melan.spacereloaded.orbit.OrbitalImages.cycleScale(serverLevel, serverPlayer, stack);
            } else {
                org.alex_melan.spacereloaded.orbit.OrbitalImages.order(serverLevel, pos, serverPlayer, stack);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
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
        // 010: экран ЦУПа вместо чата
        org.alex_melan.spacereloaded.network.ModNetworking.openStatus(serverPlayer, status(serverLevel, pos, serverPlayer));
        return InteractionResult.SUCCESS_SERVER;
    }

    /**
     * Экран ЦУПа (010, US1): линии грузовых терминалов, борта в радиусе 64 блоков (стоянка/полёт,
     * топливо, высота, Δv), спутники над телом и лучшая линия дальней связи; кнопка — карта полётов.
     */
    @Override
    public org.alex_melan.spacereloaded.network.MachineStatusPayload status(ServerLevel serverLevel, BlockPos pos,
                                                                          ServerPlayer player) {
        List<Component> lines = new java.util.ArrayList<>();
        List<RocketEntity> rockets = serverLevel.getEntities(
                EntityTypeTest.forClass(RocketEntity.class),
                new AABB(pos).inflate(RANGE), entity -> !entity.isDebris());
        List<org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity> terminals = terminalsInRange(serverLevel, pos);
        if (rockets.isEmpty() && terminals.isEmpty()) {
            lines.add(Component.translatable("message.spacereloaded.mission_control.empty"));
        }
        // 003 (FR-117): грузовые терминалы в радиусе — состояние линий
        for (var terminal : terminals) {
            lines.add(Component.translatable(
                    "message.spacereloaded.mission_control.terminal",
                    terminal.getBlockPos().toShortString(),
                    Component.translatable(terminal.stateKey()), terminal.detail(),
                    terminal.departures(), terminal.arrivals()));
        }
        if (!rockets.isEmpty()) {
            lines.add(Component.translatable("message.spacereloaded.mission_control.header", rockets.size()));
        }
        int index = 1;
        for (RocketEntity rocket : rockets) {
            String status = rocket.isParked() ? "▮" : "▲";
            lines.add(Component.translatable(
                    "message.spacereloaded.mission_control.entry_dv",
                    index++, status,
                    String.format(Locale.ROOT, "%.0f", rocket.propellantKg()),
                    String.format(Locale.ROOT, "%.0f", rocket.getY()),
                    String.format(Locale.ROOT, "%.0f", rocket.clientDeltaV()),
                    rocket.isParked()
                            ? Component.translatable("message.spacereloaded.mission_control.parked")
                            : Component.translatable("message.spacereloaded.mission_control.flight")));
        }
        // спутники над этим телом и связь (003, 007, 008, 009)
        var network = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(serverLevel.getServer());
        var body = org.alex_melan.spacereloaded.orbit.OrbitalImages.bodyUnder(serverLevel).orElse(serverLevel.dimension());
        lines.add(Component.translatable("message.spacereloaded.mission_control.satellites",
                network.hasCoverage(serverLevel.dimension()) ? Component.translatable("gui.yes") : Component.translatable("gui.no"),
                network.imagingSats(body), network.spectralSats(body)));
        return new org.alex_melan.spacereloaded.network.MachineStatusPayload(pos,
                Component.translatable("block.spacereloaded.mission_control"), lines, List.of(),
                List.of(new org.alex_melan.spacereloaded.network.MachineStatusPayload.Action("map",
                        Component.translatable("action.spacereloaded.mission_control.map"), 0)));
    }

    @Override
    public void action(ServerLevel level, BlockPos pos, ServerPlayer player, String action, double value) {
        if ("map".equals(action)) {
            org.alex_melan.spacereloaded.network.ModNetworking.sendPlanetMap(level.getServer(), player);
        }
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
