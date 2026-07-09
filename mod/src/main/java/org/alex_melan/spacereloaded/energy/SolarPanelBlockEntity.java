package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.sealing.ZoneManager;

/**
 * Солнечная панель (FR-011): день × открытое небо; в безатмосферном измерении
 * выработка выше (нет атмосферного ослабления) — сценарий US2-3.
 * TODO US5: множитель solarEfficiency из профиля планеты вместо константы.
 */
public class SolarPanelBlockEntity extends MachineBlockEntity {

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SOLAR_PANEL, pos, state,
                SpaceReloaded.config().generatorBufferCapacity, 0, Long.MAX_VALUE);
    }

    public void serverTick(ServerLevel level) {
        long generated = generationPerTick(level);
        if (generated > 0) {
            energy.amount = Math.min(energy.capacity, energy.amount + generated);
            setChanged();
        }
        pushEnergyToNeighbors(level);
        if (level.getGameTime() % 20 == 0) {
            ensureAdjacentCableNetworks(level);
        }
    }

    private long generationPerTick(ServerLevel level) {
        if (!level.isBrightOutside()) {
            return 0; // ночь
        }
        BlockPos pos = getBlockPos();
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, pos.getX(), pos.getZ());
        if (surface > pos.getY() + 1) {
            return 0; // над панелью есть блоки
        }
        long base = SpaceReloaded.config().solarEnergyPerTick;
        // Эффективность из профиля планеты (орбита без атмосферы — выше);
        // debug-режим вакуума сохраняет старый множитель для тестов
        double efficiency = org.alex_melan.spacereloaded.planet.PlanetManager.solarEfficiency(level);
        if (ZoneManager.isVacuumWorld(level) && efficiency <= 1.0) {
            efficiency = SpaceReloaded.config().solarVacuumMultiplier;
        }
        if (org.alex_melan.spacereloaded.network.MarsClimate.stormActive(level)) {
            efficiency *= SpaceReloaded.config().dustStormSolarMultiplier; // буря глушит панели
        }
        return (long) (base * efficiency);
    }
}
