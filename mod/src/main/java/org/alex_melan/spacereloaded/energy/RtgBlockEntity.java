package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** РИТЭГ (FR-011): слабая, но постоянная выработка — день/ночь/атмосфера не важны. */
public class RtgBlockEntity extends MachineBlockEntity {

    public RtgBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RTG, pos, state,
                SpaceReloaded.config().generatorBufferCapacity, 0, Long.MAX_VALUE);
    }

    public void serverTick(ServerLevel level) {
        energy.amount = Math.min(energy.capacity,
                energy.amount + SpaceReloaded.config().rtgEnergyPerTick);
        setChanged();
        pushEnergyToNeighbors(level);
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }
}
