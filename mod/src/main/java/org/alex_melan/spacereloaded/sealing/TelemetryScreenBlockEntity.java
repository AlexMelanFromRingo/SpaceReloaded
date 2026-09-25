package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * BE экрана телеметрии: раз в секунду обновляет свойство STATUS по ближайшей
 * зоне (визуальный отклик «стены экранов»); подробности — по ПКМ через блок.
 */
public class TelemetryScreenBlockEntity extends BlockEntity implements org.alex_melan.spacereloaded.multiblock.StatusProvider {

    public TelemetryScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEMETRY_SCREEN, pos, state);
    }

    /** 007: герметичная зона «зелёная», только если воздух пригоден (давление, pO₂, pCO₂). */
    private static boolean breathable(ServerLevel level, SealedZone zone) {
        var gas = org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(level, zone);
        return gas != null && gas.pressure() >= org.alex_melan.spacereloaded.lifesupport.CrewHazard.VACUUM_KPA
                && org.alex_melan.spacereloaded.core.lifesupport.Metabolism.co2Level(gas.pCo2()) < 2
                && org.alex_melan.spacereloaded.core.lifesupport.Metabolism.hypoxiaLevel(gas.pO2()) < 2;
    }

    public static void serverTick(ServerLevel level, BlockPos pos) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof TelemetryScreenBlock)) {
            return;
        }
        SealedZone zone = ZoneManager.nearestZone(level, pos, 16);
        int status = zone == null ? 0
                : zone.status() == SealingStatus.SEALED && breathable(level, zone) ? 1 : 2;
        if (state.getValue(TelemetryScreenBlock.STATUS) != status) {
            level.setBlock(pos, state.setValue(TelemetryScreenBlock.STATUS, status), 3);
        }
    }

    /** Экран телеметрии (010, US1): статус ближайшей зоны и жизнеобеспечение — вместо чата. */
    @Override
    public org.alex_melan.spacereloaded.network.MachineStatusPayload status(ServerLevel level) {
        var zone = org.alex_melan.spacereloaded.sealing.ZoneManager.nearestZone(level, getBlockPos(), 16);
        boolean vacuum = org.alex_melan.spacereloaded.sealing.ZoneManager.isVacuumWorld(level);
        net.minecraft.network.chat.Component env = net.minecraft.network.chat.Component.translatable(vacuum
                ? "message.spacereloaded.telemetry.env_vacuum" : "message.spacereloaded.telemetry.env_air");
        java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
        if (zone == null) {
            lines.add(net.minecraft.network.chat.Component.translatable("message.spacereloaded.telemetry.no_zone", env));
        } else {
            lines.add(net.minecraft.network.chat.Component.translatable("message.spacereloaded.telemetry.report",
                    net.minecraft.network.chat.Component.translatable("sealing.spacereloaded.status."
                            + zone.status().name().toLowerCase(java.util.Locale.ROOT)), zone.volume().size(), env));
            lines.addAll(org.alex_melan.spacereloaded.lifesupport.LifeSupportReport.lines(level, zone));
        }
        return new org.alex_melan.spacereloaded.network.MachineStatusPayload(getBlockPos(),
                net.minecraft.network.chat.Component.translatable("block.spacereloaded.telemetry_screen"),
                lines, java.util.List.of(), java.util.List.of());
    }
}
