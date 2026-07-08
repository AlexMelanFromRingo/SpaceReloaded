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
public class TelemetryScreenBlockEntity extends BlockEntity {

    public TelemetryScreenBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEMETRY_SCREEN, pos, state);
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
                : zone.status() == SealingStatus.SEALED ? 1 : 2;
        if (state.getValue(TelemetryScreenBlock.STATUS) != status) {
            level.setBlock(pos, state.setValue(TelemetryScreenBlock.STATUS, status), 3);
        }
    }
}
