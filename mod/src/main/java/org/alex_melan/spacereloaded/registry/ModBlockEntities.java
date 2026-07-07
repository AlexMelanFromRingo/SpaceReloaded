package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.BatteryBlockEntity;
import org.alex_melan.spacereloaded.energy.CreativePowerBlockEntity;
import org.alex_melan.spacereloaded.machine.AssemblyTableBlockEntity;
import org.alex_melan.spacereloaded.machine.CoalGeneratorBlockEntity;
import org.alex_melan.spacereloaded.machine.CrusherBlockEntity;
import org.alex_melan.spacereloaded.machine.ElectrolyzerBlockEntity;
import org.alex_melan.spacereloaded.machine.RefineryBlockEntity;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;
import org.alex_melan.spacereloaded.machine.ElectricFurnaceBlockEntity;
import org.alex_melan.spacereloaded.energy.RtgBlockEntity;
import org.alex_melan.spacereloaded.energy.SolarPanelBlockEntity;
import org.alex_melan.spacereloaded.sealing.AtmosphereControllerBlockEntity;
import team.reborn.energy.api.EnergyStorage;

import java.util.Set;

public final class ModBlockEntities {

    public static final BlockEntityType<AtmosphereControllerBlockEntity> ATMOSPHERE_CONTROLLER =
            register("atmosphere_controller", new BlockEntityType<>(AtmosphereControllerBlockEntity::new,
                    Set.of(ModBlocks.ATMOSPHERE_CONTROLLER)));

    public static final BlockEntityType<SolarPanelBlockEntity> SOLAR_PANEL =
            register("solar_panel", new BlockEntityType<>(SolarPanelBlockEntity::new,
                    Set.of(ModBlocks.SOLAR_PANEL)));

    public static final BlockEntityType<RtgBlockEntity> RTG =
            register("rtg", new BlockEntityType<>(RtgBlockEntity::new,
                    Set.of(ModBlocks.RTG)));

    public static final BlockEntityType<BatteryBlockEntity> BATTERY =
            register("battery", new BlockEntityType<>(BatteryBlockEntity::new,
                    Set.of(ModBlocks.BATTERY)));

    public static final BlockEntityType<CreativePowerBlockEntity> CREATIVE_POWER =
            register("creative_power", new BlockEntityType<>(CreativePowerBlockEntity::new,
                    Set.of(ModBlocks.CREATIVE_POWER)));

    public static final BlockEntityType<CrusherBlockEntity> CRUSHER =
            register("crusher", new BlockEntityType<>(CrusherBlockEntity::new,
                    Set.of(ModBlocks.CRUSHER)));

    public static final BlockEntityType<ElectricFurnaceBlockEntity> ELECTRIC_FURNACE =
            register("electric_furnace", new BlockEntityType<>(ElectricFurnaceBlockEntity::new,
                    Set.of(ModBlocks.ELECTRIC_FURNACE)));

    public static final BlockEntityType<AssemblyTableBlockEntity> ASSEMBLY_TABLE =
            register("assembly_table", new BlockEntityType<>(AssemblyTableBlockEntity::new,
                    Set.of(ModBlocks.ASSEMBLY_TABLE)));

    public static final BlockEntityType<CoalGeneratorBlockEntity> COAL_GENERATOR =
            register("coal_generator", new BlockEntityType<>(CoalGeneratorBlockEntity::new,
                    Set.of(ModBlocks.COAL_GENERATOR)));

    public static final BlockEntityType<FuelTankBlockEntity> FUEL_TANK =
            register("fuel_tank", new BlockEntityType<>(FuelTankBlockEntity::new,
                    Set.of(ModBlocks.FUEL_TANK)));

    public static final BlockEntityType<ElectrolyzerBlockEntity> ELECTROLYZER =
            register("electrolyzer", new BlockEntityType<>(ElectrolyzerBlockEntity::new,
                    Set.of(ModBlocks.ELECTROLYZER)));

    public static final BlockEntityType<RefineryBlockEntity> REFINERY =
            register("refinery", new BlockEntityType<>(RefineryBlockEntity::new,
                    Set.of(ModBlocks.REFINERY)));

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name), type);
    }

    public static void init() {
        // Публикация энергохранилищ в Fabric API lookup (решение D7: Team Reborn Energy)
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), SOLAR_PANEL);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), RTG);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), BATTERY);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ATMOSPHERE_CONTROLLER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CREATIVE_POWER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CRUSHER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ELECTRIC_FURNACE);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ASSEMBLY_TABLE);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), COAL_GENERATOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ELECTROLYZER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), REFINERY);
    }

    private ModBlockEntities() {
    }
}
