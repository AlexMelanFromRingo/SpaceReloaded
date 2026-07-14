package org.alex_melan.spacereloaded.fluid;

import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;

/**
 * Взгляд на топливный бак глазами Fabric Transfer API: снаружи это
 * односекционное хранилище жидкости, внутри — прежние килограммы.
 *
 * <p>Источник истины остаётся один: {@link FuelTankBlockEntity}. Здесь только
 * пересчёт капель в массу по плотности топлива и откат транзакций через
 * снимок пары «масса + тип».
 */
public class FuelTankFluidStorage extends SnapshotParticipant<FuelTankFluidStorage.Snapshot>
        implements SingleSlotStorage<FluidVariant> {

    /** Плотность пустого бака: он ещё не знает, что в него нальют. */
    private static final double DEFAULT_KG_PER_BUCKET = ModFluids.KEROLOX.kgPerBucket();

    public record Snapshot(double propellantKg, String fuelType) {
    }

    private final FuelTankBlockEntity tank;

    public FuelTankFluidStorage(FuelTankBlockEntity tank) {
        this.tank = tank;
    }

    private double kgPerBucket() {
        ModFluids.Propellant propellant = ModFluids.byFuelId(tank.fuelType());
        return propellant == null ? DEFAULT_KG_PER_BUCKET : propellant.kgPerBucket();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        ModFluids.Propellant propellant = ModFluids.byFluid(resource.getFluid());
        if (propellant == null || maxAmount <= 0) {
            return 0;
        }
        String current = tank.fuelType();
        if (!current.isEmpty() && !current.equals(propellant.fuelId())) {
            return 0; // смешивание топлив запрещено и в трубах тоже
        }
        long freeDroplets = ModFluids.kgToDroplets(
                tank.capacityKg() - tank.propellantKg(), propellant.kgPerBucket());
        long accepted = Math.min(maxAmount, freeDroplets);
        if (accepted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        tank.fill(ModFluids.dropletsToKg(accepted, propellant.kgPerBucket()), propellant.fuelId());
        return accepted;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        ModFluids.Propellant propellant = ModFluids.byFuelId(tank.fuelType());
        if (propellant == null || maxAmount <= 0 || resource.getFluid() != propellant.source()) {
            return 0;
        }
        long available = ModFluids.kgToDroplets(tank.propellantKg(), propellant.kgPerBucket());
        long extracted = Math.min(maxAmount, available);
        if (extracted <= 0) {
            return 0;
        }
        updateSnapshots(transaction);
        tank.drain(ModFluids.dropletsToKg(extracted, propellant.kgPerBucket()));
        return extracted;
    }

    @Override
    public boolean isResourceBlank() {
        return tank.fuelType().isEmpty();
    }

    @Override
    public FluidVariant getResource() {
        ModFluids.Propellant propellant = ModFluids.byFuelId(tank.fuelType());
        return propellant == null ? FluidVariant.blank() : FluidVariant.of(propellant.source());
    }

    @Override
    public long getAmount() {
        return ModFluids.kgToDroplets(tank.propellantKg(), kgPerBucket());
    }

    @Override
    public long getCapacity() {
        return ModFluids.kgToDroplets(tank.capacityKg(), kgPerBucket());
    }

    @Override
    protected Snapshot createSnapshot() {
        return new Snapshot(tank.propellantKg(), tank.fuelType());
    }

    @Override
    protected void readSnapshot(Snapshot snapshot) {
        tank.setPropellant(snapshot.propellantKg(), snapshot.fuelType());
    }

    @Override
    protected void onFinalCommit() {
        tank.setChanged();
    }
}
