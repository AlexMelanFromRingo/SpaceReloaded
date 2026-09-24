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
 * Контроллер атмосферы (T023 + T033; 007 D71): владелец зоны и регулятор её газа. Газ берётся
 * только из соседних баков O₂ и N₂ (энергия воздух не создаёт — решение автора для всех зон):
 * регулятор доводит pO₂ до 21 % уставки, а азотом — общее давление, с расходом через клапан не
 * больше {@link #VALVE_KG_PER_S}; избыток кислорода (оранжерея) компрессор возвращает в бак.
 * Энергия идёт на климат-контроль и компрессор; без энергии зона держит газ, но не пополняется.
 * Зона пересчитывается при загрузке (кэш не персистится — мир мог измениться, пока чанк был
 * выгружен); газ зоны — {@link org.alex_melan.spacereloaded.lifesupport.LifeSupportState}.
 */
public class AtmosphereControllerBlockEntity extends MachineBlockEntity {

    /** Расход клапана регулятора, кг/с (редуктор баллона). */
    public static final double VALVE_KG_PER_S = 1.0;
    /** Верх коридора O₂ (объёмная доля) — выше компрессор возвращает кислород в бак. */
    private static final double O2_UPPER = 0.235;

    private double pressure;
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

        // Тепловая нагрузка: горячее/холодное тело поднимает расход климат-контроля
        long cost = (long) Math.ceil(SpaceReloaded.config().controllerEnergyPerSecond
                * org.alex_melan.spacereloaded.network.Thermal.climateLoadFactor(level, getBlockPos()));
        if (energy.amount >= cost) {
            energy.amount -= cost;
            powered = true;
        } else {
            powered = false;
        }

        SealedZone zone = ZoneManager.zoneAt(level, getBlockPos());
        var gas = zone != null && zone.isSealed()
                ? org.alex_melan.spacereloaded.lifesupport.LifeSupportState.now(level, zone) : null;
        pressure = gas == null ? 0 : gas.pressure();
        if (gas != null && powered) {
            regulate(level, zone, gas);
        }
        setChanged();
    }

    /** Регулятор: O₂ до уставки, N₂ — буфер давления, избыток O₂ — обратно в бак. */
    private void regulate(ServerLevel level, SealedZone zone,
                          org.alex_melan.spacereloaded.lifesupport.LifeSupportState.Gas gas) {
        var o2 = org.alex_melan.spacereloaded.lifesupport.GasKind.OXYGEN;
        var n2 = org.alex_melan.spacereloaded.lifesupport.GasKind.NITROGEN;
        var config = SpaceReloaded.config();
        double v = gas.volume();
        double t = org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.T_CABIN;
        double targetO2 = org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.massFor(
                config.cabinPressureKpa * config.cabinO2Fraction,
                org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.M_O2, v, t);
        double targetN2 = org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.massFor(
                config.cabinPressureKpa * (1 - config.cabinO2Fraction),
                org.alex_melan.spacereloaded.core.lifesupport.CabinAtmosphere.M_N2, v, t);
        double budget = VALVE_KG_PER_S;
        double dO2 = 0;
        double dN2 = 0;
        if (gas.mO2() < targetO2) {
            dO2 = org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity.pullFromNeighbors(level, getBlockPos(),
                    o2, Math.min(budget, targetO2 - gas.mO2()));
            budget -= dO2;
        } else {
            double upper = targetO2 * O2_UPPER / config.cabinO2Fraction;
            if (gas.mO2() > upper) {
                dO2 = -org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity.pushToNeighbors(level, getBlockPos(),
                        o2, Math.min(budget, gas.mO2() - targetO2));
                budget += dO2;
            }
        }
        if (gas.mN2() < targetN2 && budget > 0) {
            dN2 = org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity.pullFromNeighbors(level, getBlockPos(),
                    n2, Math.min(budget, targetN2 - gas.mN2()));
        }
        if (dO2 != 0 || dN2 != 0) {
            org.alex_melan.spacereloaded.lifesupport.LifeSupportState.add(level, zone, dO2, dN2, 0);
        }
    }

    /** Вызывается ZoneManager'ом из главного потока после пересчёта. */
    public void onZoneUpdated(SealingResult result) {
        this.lastStatus = result.status();
        setChanged();
    }

    /** Давление зоны при последней регулировке, кПа. */
    public double pressure() {
        return pressure;
    }

    public boolean powered() {
        return powered;
    }

    public SealingStatus lastStatus() {
        return lastStatus;
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        if (getLevel() instanceof ServerLevel serverLevel) {
            // контроллер сломан (не выгрузка чанка) — воздух чистой комнаты забыт
            org.alex_melan.spacereloaded.electronics.CleanroomTracker.onZoneRemoved(serverLevel, pos);
            org.alex_melan.spacereloaded.lifesupport.LifeSupportState.onZoneRemoved(serverLevel, pos);
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

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("pressure", pressure);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        pressure = input.getDoubleOr("pressure", 0.0);
    }
}
