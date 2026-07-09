package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.network.SpaceNetworkState;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Ректенна (Phase 14): наземная антенна, принимающая энергию с орбитальных
 * энергоспутников. Выработка пропорциональна числу развёрнутых энергоспутников
 * на орбите Земли и требует чистого неба над собой (как солнечная панель).
 * После спутниковой инфраструктуры даёт пассивную мощность вне зависимости от дня.
 */
public class RectennaBlockEntity extends MachineBlockEntity {

    private static final ResourceKey<Level> BEAM_SOURCE = ResourceKey.create(Registries.DIMENSION,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "earth_orbit"));

    public RectennaBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RECTENNA, pos, state,
                SpaceReloaded.config().generatorBufferCapacity, 0, Long.MAX_VALUE);
    }

    public void serverTick(ServerLevel level) {
        long generated = generationPerTick(level);
        if (generated > 0) {
            energy.amount = Math.min(energy.capacity, energy.amount + generated);
            setChanged();
        }
        pushEnergyToNeighbors(level);
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }

    private long generationPerTick(ServerLevel level) {
        int sats = SpaceNetworkState.get(level.getServer()).powerSats(BEAM_SOURCE);
        if (sats <= 0) {
            return 0; // нет энергоспутников на орбите
        }
        BlockPos pos = getBlockPos();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (surface > pos.getY() + 1) {
            return 0; // небо над ректенной перекрыто — луч не пройдёт
        }
        return SpaceReloaded.config().rectennaEnergyPerSat * sats;
    }
}
