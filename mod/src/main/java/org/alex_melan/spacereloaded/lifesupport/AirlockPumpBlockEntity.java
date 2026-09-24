package org.alex_melan.spacereloaded.lifesupport;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.core.station.AirlockCycle;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

/**
 * Насос шлюза (007, FR-506, D74): владелец зоны-тамбура (как контроллер — заливка от клетки над
 * ним). При выходе в вакуум люк не открывается сразу: насос откачивает тамбур по
 * p(t) = p₀·e^(−S·t/V) до 14 кПа и сжимает газ в соседние газовые баки, остаток стравливается.
 * Пластинчато-роторный насос 0.05 м³/с потребляет ~4 кВт.
 */
public class AirlockPumpBlockEntity extends MachineBlockEntity {

    public static final double POWER_W = 4000;

    private boolean registered;

    public AirlockPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.AIRLOCK_PUMP, pos, state, 20_000, 400, 0);
    }

    public void serverTick(ServerLevel level) {
        if (!registered) {
            registered = true;
            ZoneManager.registerController(level, getBlockPos());
        }
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }

    /** Энергия откачки за t секунд, E. */
    public static long energyFor(double seconds) {
        return (long) Math.ceil(EnergyScale.fromJoules(POWER_W * seconds));
    }

    /** Взять энергию на цикл; false — не хватает. */
    public boolean draw(long amount) {
        if (energy.amount < amount) {
            return false;
        }
        energy.amount -= amount;
        setChanged();
        return true;
    }

    public double speed() {
        return AirlockCycle.PUMP_SPEED;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (getLevel() instanceof ServerLevel serverLevel) {
            LifeSupportState.onZoneRemoved(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            ZoneManager.removeController(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }
}
