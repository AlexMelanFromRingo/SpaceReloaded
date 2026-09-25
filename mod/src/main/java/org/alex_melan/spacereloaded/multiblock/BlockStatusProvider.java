package org.alex_melan.spacereloaded.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;

/**
 * Экран состояния блока без блок-сущности (010, US1) — тот же {@code MachineStatusScreen}, что у
 * {@link StatusProvider}, но данные собирает сам блок по позиции (ЦУП: борта и терминалы вокруг).
 */
public interface BlockStatusProvider {

    MachineStatusPayload status(ServerLevel level, BlockPos pos, ServerPlayer player);

    default void action(ServerLevel level, BlockPos pos, ServerPlayer player, String action, double value) {
    }
}
