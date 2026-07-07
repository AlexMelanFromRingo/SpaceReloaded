package org.alex_melan.spacereloaded.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

/** Общие энергооперации машин (устраняет дубли между иерархиями BE). */
public final class EnergyUtil {

    public static void pushToNeighbors(ServerLevel level, BlockPos pos, EnergyStorage from) {
        for (Direction dir : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, pos.relative(dir), dir.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                EnergyStorageUtil.move(from, target, Long.MAX_VALUE, transaction);
                transaction.commit();
            }
        }
    }

    /** Ленивое открытие кабельных сетей: кабелям не нужны block entities. */
    public static void ensureAdjacentCableNetworks(ServerLevel level, BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.ENERGY_CABLE)) {
                CableNetworkManager.ensureNetwork(level, neighbor);
            }
        }
    }

    private EnergyUtil() {
    }
}
