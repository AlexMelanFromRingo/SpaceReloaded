package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.sealing.SealingResult;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Контроллер атмосферы (T023 + T033): владелец зоны, накопитель атмосферы,
 * потребитель энергии (FR-009). Без энергии атмосфера не восстанавливается —
 * контроллер «деградирует предсказуемо» (сценарий US2-4).
 * Зона пересчитывается при загрузке (кэш не персистится — мир мог измениться,
 * пока чанк был выгружен).
 */
public class AtmosphereControllerBlockEntity extends MachineBlockEntity {

    /** Прирост атмосферы за секунду при герметичной зоне и наличии энергии. */
    private static final double FILL_RATE = 0.05;
    /** Скорость потери при разгерметизации — быстрее заполнения (осознанный хардкор). */
    private static final double DECAY_RATE = 0.25;

    private double atmosphere;
    private boolean scanQueued;
    private boolean powered;
    private SealingStatus lastStatus = SealingStatus.INVALID_ORIGIN;

    public AtmosphereControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_CONTROLLER, pos, state,
                SpaceReloaded.config().controllerEnergyCapacity,
                SpaceReloaded.config().controllerEnergyCapacity, 0);
    }

    public void serverTick(ServerLevel level) {
        if (!scanQueued) {
            scanQueued = true;
            ZoneManager.registerController(level, getBlockPos());
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);

        long cost = SpaceReloaded.config().controllerEnergyPerSecond;
        if (energy.amount >= cost) {
            energy.amount -= cost;
            powered = true;
        } else {
            powered = false;
        }

        SealedZone zone = ZoneManager.zoneAt(level, getBlockPos());
        if (zone != null && zone.isSealed()) {
            if (powered) {
                atmosphere = Math.min(1.0, atmosphere + FILL_RATE);
            }
            // Без энергии герметичная зона держит атмосферу, но не пополняет её (FR-009)
        } else {
            atmosphere = Math.max(0.0, atmosphere - DECAY_RATE);
        }
        setChanged();
    }

    /** Вызывается ZoneManager'ом из главного потока после пересчёта. */
    public void onZoneUpdated(SealingResult result) {
        this.lastStatus = result.status();
        setChanged();
    }

    public double atmosphere() {
        return atmosphere;
    }

    public boolean powered() {
        return powered;
    }

    public SealingStatus lastStatus() {
        return lastStatus;
    }

    @Override
    public void setRemoved() {
        if (getLevel() instanceof ServerLevel serverLevel) {
            ZoneManager.removeController(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("atmosphere", atmosphere);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        atmosphere = input.getDoubleOr("atmosphere", 0.0);
    }
}
