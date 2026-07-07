package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.List;
import java.util.Locale;

/**
 * Заправочная колонка: раз в 10 тиков сканирует объём над собой (AABB) на
 * припаркованную ракету и перекачивает топливо из соседних баков в неё
 * (режим DRAIN — обратно). Автоматизация поверх ручного рукава.
 */
public class FuelingPumpBlockEntity extends BlockEntity {

    public enum Mode {
        FUEL, DRAIN, OFF;

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** кг за цикл (10 тиков) = 200 кг/с. */
    private static final double RATE = 100.0;
    private static final int HORIZONTAL_RANGE = 8;
    private static final int VERTICAL_RANGE = 48;

    private Mode mode = Mode.FUEL;

    public FuelingPumpBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUELING_PUMP, pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public Mode cycleMode() {
        mode = mode.next();
        setChanged();
        return mode;
    }

    public void serverTick(ServerLevel level) {
        if (mode == Mode.OFF || level.getGameTime() % 10 != 0) {
            return;
        }
        RocketEntity rocket = findRocket(level);
        if (rocket == null) {
            return;
        }
        if (mode == Mode.FUEL) {
            pumpIntoRocket(level, rocket);
        } else {
            drainFromRocket(level, rocket);
        }
    }

    private RocketEntity findRocket(ServerLevel level) {
        BlockPos pos = getBlockPos();
        AABB area = new AABB(
                pos.getX() - HORIZONTAL_RANGE, pos.getY(), pos.getZ() - HORIZONTAL_RANGE,
                pos.getX() + HORIZONTAL_RANGE + 1, pos.getY() + VERTICAL_RANGE,
                pos.getZ() + HORIZONTAL_RANGE + 1);
        List<RocketEntity> rockets = level.getEntities(
                EntityTypeTest.forClass(RocketEntity.class), area, RocketEntity::isParked);
        return rockets.isEmpty() ? null : rockets.get(0);
    }

    private void pumpIntoRocket(ServerLevel level, RocketEntity rocket) {
        double budget = RATE;
        String rocketFuel = rocket.rocketFuelType();
        for (Direction dir : Direction.values()) {
            if (budget <= 0) {
                break;
            }
            if (level.getBlockEntity(getBlockPos().relative(dir)) instanceof FuelTankBlockEntity tank) {
                if (tank.propellantKg() <= 0
                        || (!rocketFuel.isEmpty() && !tank.fuelType().equals(rocketFuel))) {
                    continue;
                }
                double available = Math.min(budget, tank.propellantKg());
                double accepted = rocket.refuel(available);
                tank.drain(accepted);
                budget -= accepted;
            }
        }
    }

    private void drainFromRocket(ServerLevel level, RocketEntity rocket) {
        double drained = rocket.drain(RATE);
        if (drained <= 0) {
            return;
        }
        String fuel = rocket.rocketFuelType();
        for (Direction dir : Direction.values()) {
            if (drained <= 0) {
                break;
            }
            if (level.getBlockEntity(getBlockPos().relative(dir)) instanceof FuelTankBlockEntity tank) {
                drained -= tank.fill(drained, fuel);
            }
        }
        if (drained > 0) {
            rocket.refuel(drained); // некуда сливать — вернуть в ракету
        }
    }

    public String describe() {
        return mode.name().toLowerCase(Locale.ROOT);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("mode", mode.ordinal());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        mode = Mode.values()[Math.floorMod(input.getIntOr("mode", 0), Mode.values().length)];
    }
}
