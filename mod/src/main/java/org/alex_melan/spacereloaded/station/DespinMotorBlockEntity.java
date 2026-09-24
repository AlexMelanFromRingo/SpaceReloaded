package org.alex_melan.spacereloaded.station;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Мотор противовращения (007, FR-519, D77) между двумя ступицами на одной оси: равные и
 * противоположные моменты — суммарный момент импульса станции остаётся нулём, топлива не нужно.
 * Редуктор даёт до 50 кН·м, мощность до 50 кВт (P = τ·Δω); энергия — из сети. Редстоун — работа,
 * Sneak+ПКМ — режим: раскрутка или торможение (рекуперации нет — энергия уходит в тепло тормоза).
 */
public class DespinMotorBlockEntity extends MachineBlockEntity {

    public static final double MAX_TORQUE = 5e4;
    public static final double MAX_POWER_W = 5e4;

    private boolean brake;
    private double debt;

    public DespinMotorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DESPIN_MOTOR, pos, state, 8000, 400, 0);
    }

    public boolean toggle() {
        brake = !brake;
        setChanged();
        return brake;
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        if (!level.hasNeighborSignal(worldPosition)) {
            return;
        }
        Direction.Axis axis = getBlockState().getValue(DespinMotorBlock.AXIS);
        BlockPos pa = worldPosition.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE));
        BlockPos pb = worldPosition.relative(Direction.fromAxisAndDirection(axis, Direction.AxisDirection.NEGATIVE));
        if (!(level.getBlockEntity(pa) instanceof SpinHubBlockEntity a) || !(level.getBlockEntity(pb) instanceof SpinHubBlockEntity b)
                || !a.isolated() || !b.isolated()) {
            return;
        }
        double relative = a.omega() - b.omega();
        double sign = brake ? -Math.signum(relative) : (relative >= 0 ? 1 : -1);
        if (brake && relative == 0) {
            return;
        }
        double torque = Math.min(MAX_TORQUE, MAX_POWER_W / Math.max(1e-3, Math.abs(relative)));
        double joules = torque * Math.abs(relative) * 1.0 + 100; // + потери холостого хода
        debt += EnergyScale.fromJoules(joules);
        long cost = (long) Math.floor(debt);
        if (energy.amount < cost) {
            return;
        }
        energy.amount -= cost;
        debt -= cost;
        a.applyImpulse(sign * torque);
        b.applyImpulse(-sign * torque);
        setChanged();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("brake", brake);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        brake = input.getBooleanOr("brake", false);
    }
}
