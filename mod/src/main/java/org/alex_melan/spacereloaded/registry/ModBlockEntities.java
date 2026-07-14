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
import org.alex_melan.spacereloaded.rocket.FuelingPumpBlockEntity;
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

    public static final BlockEntityType<FuelingPumpBlockEntity> FUELING_PUMP =
            register("fueling_pump", new BlockEntityType<>(FuelingPumpBlockEntity::new,
                    Set.of(ModBlocks.FUELING_PUMP)));

    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> BlockEntityType<T> register(
            String name, BlockEntityType<T> type) {
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name), type);
    }

    public static final BlockEntityType<org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity> ORBITAL_CANNON =
            register("orbital_cannon", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.cannon.OrbitalCannonBlockEntity::new,
                    Set.of(ModBlocks.ORBITAL_CANNON)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.rocket.CargoHoldBlockEntity> CARGO_HOLD =
            register("cargo_hold", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.rocket.CargoHoldBlockEntity::new,
                    Set.of(ModBlocks.CARGO_HOLD)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.rocket.CargoLoaderBlockEntity> CARGO_LOADER =
            register("cargo_loader", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.rocket.CargoLoaderBlockEntity::new,
                    Set.of(ModBlocks.CARGO_LOADER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.sealing.TelemetryScreenBlockEntity> TELEMETRY_SCREEN =
            register("telemetry_screen", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.sealing.TelemetryScreenBlockEntity::new,
                    Set.of(ModBlocks.TELEMETRY_SCREEN)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.machine.AtmosphericCollectorBlockEntity> ATMOSPHERIC_COLLECTOR =
            register("atmospheric_collector", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.machine.AtmosphericCollectorBlockEntity::new,
                    Set.of(ModBlocks.ATMOSPHERIC_COLLECTOR)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.machine.SabatierReactorBlockEntity> SABATIER_REACTOR =
            register("sabatier_reactor", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.machine.SabatierReactorBlockEntity::new,
                    Set.of(ModBlocks.SABATIER_REACTOR)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.network.InterceptorDishBlockEntity> INTERCEPTOR_DISH =
            register("interceptor_dish", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.network.InterceptorDishBlockEntity::new,
                    Set.of(ModBlocks.INTERCEPTOR_DISH)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.energy.RectennaBlockEntity> RECTENNA =
            register("rectenna", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.energy.RectennaBlockEntity::new,
                    Set.of(ModBlocks.RECTENNA)));

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
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ORBITAL_CANNON);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ATMOSPHERIC_COLLECTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), SABATIER_REACTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), RECTENNA);

        // Топливо как жидкость: бак виден трубам соседних модов (Fabric Transfer API)
        net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.SIDED.registerForBlockEntity(
                (be, direction) -> be.fluidStorage(), FUEL_TANK);
        for (var propellant : org.alex_melan.spacereloaded.fluid.ModFluids.all()) {
            // Полное ведро отдаёт топливо и превращается в пустое
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.ITEM.registerForItems(
                    (stack, context) -> new net.fabricmc.fabric.api.transfer.v1.fluid.base
                            .FullItemFluidStorage(context, net.minecraft.world.item.Items.BUCKET,
                            net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant.of(propellant.source()),
                            net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET),
                    propellant.bucket());
            // Пустое ведро принимает топливо и становится полным
            net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage.combinedItemApiProvider(
                    net.minecraft.world.item.Items.BUCKET).register(context ->
                    new net.fabricmc.fabric.api.transfer.v1.fluid.base.EmptyItemFluidStorage(
                            context, propellant.bucket(), propellant.source(),
                            net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants.BUCKET));
        }
    }

    private ModBlockEntities() {
    }
}
