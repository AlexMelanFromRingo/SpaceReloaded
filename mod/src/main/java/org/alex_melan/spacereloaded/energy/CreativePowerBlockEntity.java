package org.alex_melan.spacereloaded.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.InfiniteEnergyStorage;

/** Креативный источник энергии — для тестов и quickstart-сценария. */
public class CreativePowerBlockEntity extends MachineBlockEntity {

    public CreativePowerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CREATIVE_POWER, pos, state, 1, 0, 0);
    }

    @Override
    public EnergyStorage energyStorage() {
        return InfiniteEnergyStorage.INSTANCE;
    }

    public void serverTick(ServerLevel level) {
        for (Direction dir : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, getBlockPos().relative(dir), dir.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                EnergyStorageUtil.move(InfiniteEnergyStorage.INSTANCE, target, Long.MAX_VALUE, transaction);
                transaction.commit();
            }
        }
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }
}
