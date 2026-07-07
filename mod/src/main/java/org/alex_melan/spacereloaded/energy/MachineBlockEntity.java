package org.alex_melan.spacereloaded.energy;

import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * База машин с энергобуфером (US2): персистентность заряда, выталкивание
 * энергии соседям и ленивая регистрация соседних кабелей в сети.
 */
public abstract class MachineBlockEntity extends BlockEntity {

    protected final SimpleEnergyStorage energy;

    protected MachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                 long capacity, long maxInsert, long maxExtract) {
        super(type, pos, state);
        this.energy = new SimpleEnergyStorage(capacity, maxInsert, maxExtract);
    }

    public EnergyStorage energyStorage() {
        return energy;
    }

    /** Выталкивает энергию во все соседние хранилища (для генераторов). */
    protected void pushEnergyToNeighbors(ServerLevel level) {
        if (energy.amount <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, getBlockPos().relative(dir), dir.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                EnergyStorageUtil.move(energy, target, Long.MAX_VALUE, transaction);
                transaction.commit();
            }
            if (energy.amount <= 0) {
                return;
            }
        }
    }

    /** Ленивое открытие кабельных сетей: кабелям не нужны block entities. */
    protected void ensureAdjacentCableNetworks(ServerLevel level) {
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = getBlockPos().relative(dir);
            if (level.getBlockState(neighbor).is(ModBlocks.ENERGY_CABLE)) {
                CableNetworkManager.ensureNetwork(level, neighbor);
            }
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
    }
}
