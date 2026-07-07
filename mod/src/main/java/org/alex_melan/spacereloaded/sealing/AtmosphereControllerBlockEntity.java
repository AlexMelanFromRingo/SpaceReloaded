package org.alex_melan.spacereloaded.sealing;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.sealing.SealingResult;
import org.alex_melan.spacereloaded.core.sealing.SealingStatus;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Контроллер атмосферы (T023): владелец зоны, накопитель атмосферы.
 * Зона пересчитывается при загрузке (кэш не персистится — мир мог измениться,
 * пока чанк был выгружен). Энергопотребление — заглушка до US2 (FR-009 TODO).
 */
public class AtmosphereControllerBlockEntity extends BlockEntity {

    /** Прирост атмосферы за секунду при герметичной зоне. */
    private static final double FILL_RATE = 0.05;
    /** Скорость потери при разгерметизации — быстрее заполнения (осознанный хардкор). */
    private static final double DECAY_RATE = 0.25;

    private double atmosphere;
    private boolean scanQueued;
    private SealingStatus lastStatus = SealingStatus.INVALID_ORIGIN;

    public AtmosphereControllerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ATMOSPHERE_CONTROLLER, pos, state);
    }

    public void serverTick(ServerLevel level) {
        if (!scanQueued) {
            scanQueued = true;
            ZoneManager.registerController(level, getBlockPos());
        }
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        SealedZone zone = ZoneManager.zoneAt(level, getBlockPos());
        if (zone != null && zone.isSealed()) {
            atmosphere = Math.min(1.0, atmosphere + FILL_RATE);
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
