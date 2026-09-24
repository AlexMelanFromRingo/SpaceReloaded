package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Конденсатор батареи катапульты (004, FR-202, D31): накопитель энергии из сети с конечной
 * скоростью приёма. Отдачи в сеть нет (maxExtract 0) — энергию забирает только казённик
 * при выстреле, иначе кабельная сеть осушала бы батарею в соседние станки.
 */
public class CapacitorBlockEntity extends MachineBlockEntity {

    public CapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CAPACITOR, pos, state, SpaceReloaded.config().capacitorCapacity,
                SpaceReloaded.config().capacitorMaxInsert, 0);
    }

    public static void serverTick(CapacitorBlockEntity capacitor, ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            capacitor.ensureAdjacentCableNetworks(level);
        }
    }

    public long stored() {
        return energy.amount;
    }

    public long capacity() {
        return energy.capacity;
    }

    /** Разряд при выстреле: снимает до {@code amount}, возвращает снятое. */
    public long discharge(long amount) {
        long taken = Math.min(amount, energy.amount);
        energy.amount -= taken;
        setChanged();
        return taken;
    }
}
