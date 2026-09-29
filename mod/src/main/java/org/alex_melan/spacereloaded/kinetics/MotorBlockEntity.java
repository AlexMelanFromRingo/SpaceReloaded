package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * Мотор-генератор (005, FR-304): обратимая машина с линейной характеристикой
 * τ(ω) = τ_st·(1 − ω/ω₀), пиковая мощность P_max = τ_st·ω₀/4 при ω₀/2. Потребляет τ·ω/η из
 * энергобуфера по канонической шкале мода (1 E = 15 кДж — масштаб 004); если сеть крутит его
 * быстрее ω₀, момент меняет знак и машина генерирует |τ·ω|·η. Нехватка энергии — момент
 * масштабируется долей доступной энергии k.
 *
 * <p>011: скорость задаётся напряжением якоря u = 10…100 % (кнопки экрана): у машины постоянного тока
 * с постоянным сопротивлением якоря ω₀ ∝ U и τ_st ∝ U, поэтому холостая скорость и пусковой момент
 * пропорциональны u, пиковая мощность — u².
 */
public class MotorBlockEntity extends KineticBlockEntity {

    private static final double MOTOR_INERTIA = 5.0;

    private final SimpleEnergyStorage energy;
    private double availability = 1.0;
    private double energyAccumulator;
    /** Напряжение якоря, доля номинала (011). */
    private double voltage = 1.0;

    public MotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MOTOR, pos, state);
        long buffer = SpaceReloaded.config().motorEnergyBuffer;
        this.energy = new SimpleEnergyStorage(buffer, buffer, buffer);
    }

    public EnergyStorage energyStorage() {
        return energy;
    }

    public static double noLoadOmega() {
        return SpaceReloaded.config().motorNoLoadRpm * 2 * Math.PI / 60;
    }

    public static double stallTorque() {
        return 4 * SpaceReloaded.config().motorPowerW / noLoadOmega();
    }

    /** Холостая скорость при текущем напряжении, рад/с. */
    public double noLoadOmegaNow() {
        return noLoadOmega() * voltage;
    }

    /** Пусковой момент при текущем напряжении, Н·м. */
    public double stallTorqueNow() {
        return stallTorque() * voltage;
    }

    public double voltage() {
        return voltage;
    }

    public void setVoltage(double v) {
        voltage = Math.max(0.1, Math.min(1.0, Math.round(v * 10) / 10.0));
        setChanged();
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        double stall = stallTorqueNow() * availability;
        return rotorLoad(level, stall, stall / noLoadOmegaNow(), 0, MOTOR_INERTIA);
    }

    @Override
    protected void extraReport(java.util.List<net.minecraft.network.chat.Component> lines) {
        lines.add(net.minecraft.network.chat.Component.translatable("message.spacereloaded.motor.voltage",
                String.format(java.util.Locale.ROOT, "%.0f", voltage * 100),
                String.format(java.util.Locale.ROOT, "%.0f", toRpm(noLoadOmegaNow())),
                String.format(java.util.Locale.ROOT, "%.1f", SpaceReloaded.config().motorPowerW * voltage * voltage / 1000)));
    }

    @Override
    protected java.util.List<org.alex_melan.spacereloaded.network.MachineStatusPayload.Action> statusActions() {
        return java.util.List.of(
                new org.alex_melan.spacereloaded.network.MachineStatusPayload.Action("voltage",
                        net.minecraft.network.chat.Component.translatable("action.spacereloaded.motor.slower"), -0.1),
                new org.alex_melan.spacereloaded.network.MachineStatusPayload.Action("voltage",
                        net.minecraft.network.chat.Component.translatable("action.spacereloaded.motor.faster"), 0.1));
    }

    @Override
    public void action(ServerLevel level, net.minecraft.server.level.ServerPlayer player, String action, double value) {
        if ("voltage".equals(action)) {
            setVoltage(voltage + value);
            KineticNetworks.wake(level, getBlockPos());
        }
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        SpaceReloadedConfig config = SpaceReloaded.config();
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
        }
        double torque = stallTorqueNow() * availability * (1 - omega / noLoadOmegaNow());
        double mechanical = torque * omega; // Вт, > 0 — отдаёт в сеть
        double jPerE = config.massDriverJoulesPerEnergy;
        double nextAvailability = availability;
        if (mechanical > 0) {
            energyAccumulator += mechanical / config.motorEfficiency * KineticNetworks.DT / jPerE;
            long need = (long) Math.floor(energyAccumulator);
            if (need > 0) {
                long taken = Math.min(need, energy.amount);
                energy.amount -= taken;
                energyAccumulator -= taken;
                nextAvailability = taken >= need ? Math.min(1.0, availability + 0.05) : (double) taken / need;
                if (taken < need) {
                    energyAccumulator = 0; // долг не копим — просто меньше момента
                }
            }
        } else {
            nextAvailability = 1.0;
            energyAccumulator -= mechanical * config.motorEfficiency * KineticNetworks.DT / jPerE;
            long gain = (long) Math.floor(energyAccumulator);
            if (gain > 0) {
                long stored = Math.min(gain, energy.capacity - energy.amount);
                energy.amount += stored;
                energyAccumulator -= gain;
            }
            pushEnergy(level);
        }
        if (energy.amount > 0 && nextAvailability < 1.0 && omega == 0) {
            nextAvailability = 1.0; // запуск с места при появлении энергии
        }
        if (level.getGameTime() % 40 == Math.floorMod(getBlockPos().asLong(), 40) && Math.abs(omega) > 5) {
            level.playSound(null, getBlockPos(), org.alex_melan.spacereloaded.registry.ModSounds.MOTOR_HUM,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5f,
                    (float) Math.max(0.5, Math.min(2.0, Math.abs(omega) / noLoadOmega() * 1.2)));  // тон — от оборотов
        }
        if (Math.abs(nextAvailability - availability) > 0.02) {
            availability = nextAvailability;
            KineticNetworks.wake(level, getBlockPos());
        }
        setChanged();
    }

    private void pushEnergy(ServerLevel level) {
        if (energy.amount <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            EnergyStorage target = EnergyStorage.SIDED.find(level, getBlockPos().relative(dir), dir.getOpposite());
            if (target == null || !target.supportsInsertion()) {
                continue;
            }
            try (Transaction transaction = Transaction.openOuter()) {
                EnergyStorageUtil.move(energy, target, energy.amount, transaction);
                transaction.commit();
            }
        }
    }

    public double availability() {
        return availability;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("energy", energy.amount);
        output.putDouble("availability", availability);
        output.putDouble("voltage", voltage);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
        availability = input.getDoubleOr("availability", 1.0);
        voltage = input.getDoubleOr("voltage", 1.0);
    }
}
