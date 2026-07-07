package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Аккумулятор (FR-010): пассивное хранилище, принимает и отдаёт через сеть/соседей. */
public class BatteryBlockEntity extends MachineBlockEntity {

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY, pos, state,
                SpaceReloaded.config().batteryCapacity,
                SpaceReloaded.config().batteryMaxTransfer,
                SpaceReloaded.config().batteryMaxTransfer);
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }
}
