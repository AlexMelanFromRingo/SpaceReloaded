package org.alex_melan.spacereloaded.vehicle;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.alex_melan.spacereloaded.energy.MachineBlockEntity;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Зарядная станция ровера (007, FR-523): заряжает батарею ровера в радиусе 3 блоков током C/5
 * (никель-железный аккумулятор: 1.74 кВт для 8.7 кВт·ч, КПД заряда ~0.7 — остальное тепло).
 * Механика в реальном времени: полный заряд — около 5 часов, как у настоящей Ni–Fe батареи.
 */
public class RoverChargerBlockEntity extends MachineBlockEntity {

    public static final double C_RATE = 0.2;
    public static final double CHARGE_EFFICIENCY = 0.7;

    private double debt;

    public RoverChargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROVER_CHARGER, pos, state, 2000, 200, 0);
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        ensureAdjacentCableNetworks(level);
        double watts = C_RATE * RoverEntity.BATTERY_KWH * 1000;
        for (RoverEntity rover : level.getEntitiesOfClass(RoverEntity.class, new AABB(worldPosition).inflate(3))) {
            if (!rover.hasBattery() || rover.charge() >= RoverEntity.capacityE()) {
                continue;
            }
            double stored = EnergyScale.fromJoules(watts);          // в батарею за секунду
            debt += stored / CHARGE_EFFICIENCY;                       // из сети
            long cost = (long) Math.floor(debt);
            if (energy.amount < cost) {
                debt = Math.min(debt, 1);
                continue;
            }
            energy.amount -= cost;
            debt -= cost;
            rover.charge(stored);
            setChanged();
        }
    }
}
