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
                    Set.of(ModBlocks.SOLAR_PANEL, ModBlocks.MONO_SOLAR_PANEL)));

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
                    Set.of(ModBlocks.FUEL_TANK, ModBlocks.ALUMINIUM_FUEL_TANK, ModBlocks.AL_LI_FUEL_TANK)));

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

    public static final BlockEntityType<org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity> CARGO_TERMINAL =
            register("cargo_terminal", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.logistics.CargoTerminalBlockEntity::new,
                    Set.of(ModBlocks.CARGO_TERMINAL)));

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

    // --- Лунная индустрия (004) ---
    public static final BlockEntityType<org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity> MASS_DRIVER_BREECH =
            register("mass_driver_breech", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.industry.MassDriverBreechBlockEntity::new,
                    Set.of(ModBlocks.MASS_DRIVER_BREECH)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.industry.CapacitorBlockEntity> CAPACITOR =
            register("capacitor", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.industry.CapacitorBlockEntity::new,
                    Set.of(ModBlocks.CAPACITOR)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.industry.MassCatcherBlockEntity> MASS_CATCHER =
            register("mass_catcher", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.industry.MassCatcherBlockEntity::new,
                    Set.of(ModBlocks.MASS_CATCHER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity> REGOLITH_REACTOR =
            register("regolith_reactor", new BlockEntityType<>(
                    org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity::new,
                    Set.of(ModBlocks.REGOLITH_REACTOR)));

    // --- Инженерия (005) ---
    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.KineticBlockEntity> KINETIC =
            register("kinetic", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.KineticBlockEntity::new,
                    Set.of(ModBlocks.WOODEN_SHAFT, ModBlocks.STEEL_SHAFT, ModBlocks.SMALL_GEAR, ModBlocks.LARGE_GEAR,
                            ModBlocks.GEARBOX, ModBlocks.CLUTCH)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.MotorBlockEntity> MOTOR =
            register("motor", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.MotorBlockEntity::new, Set.of(ModBlocks.MOTOR)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.FlywheelBlockEntity> FLYWHEEL =
            register("flywheel", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.FlywheelBlockEntity::new, Set.of(ModBlocks.FLYWHEEL)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.PressBlockEntity> PRESS =
            register("mechanical_press", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.PressBlockEntity::new,
                    Set.of(ModBlocks.MECHANICAL_PRESS)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.LatheBlockEntity> LATHE =
            register("lathe", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.LatheBlockEntity::new, Set.of(ModBlocks.LATHE)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.kinetics.WindHubBlockEntity> WIND_HUB =
            register("wind_hub", new BlockEntityType<>(org.alex_melan.spacereloaded.kinetics.WindHubBlockEntity::new, Set.of(ModBlocks.WIND_HUB)));

    // --- Материалы и электроника (006) ---
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.ChemicalReactorBlockEntity> CHEMICAL_REACTOR =
            register("chemical_reactor", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.ChemicalReactorBlockEntity::new,
                    Set.of(ModBlocks.CHEMICAL_REACTOR)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.CrystalPullerBlockEntity> CRYSTAL_PULLER =
            register("crystal_puller", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.CrystalPullerBlockEntity::new,
                    Set.of(ModBlocks.CRYSTAL_PULLER)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.WaferSawBlockEntity> WAFER_SAW =
            register("wafer_saw", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.WaferSawBlockEntity::new, Set.of(ModBlocks.WAFER_SAW)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.DepositionReactorBlockEntity> DEPOSITION_REACTOR =
            register("deposition_reactor", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.DepositionReactorBlockEntity::new,
                    Set.of(ModBlocks.DEPOSITION_REACTOR)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.DiffusionFurnaceBlockEntity> DIFFUSION_FURNACE =
            register("diffusion_furnace", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.DiffusionFurnaceBlockEntity::new,
                    Set.of(ModBlocks.DIFFUSION_FURNACE)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.EtchBathBlockEntity> ETCH_BATH =
            register("etch_bath", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.EtchBathBlockEntity::new,
                    Set.of(ModBlocks.ETCH_BATH)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.electronics.LithographyStationBlockEntity> LITHOGRAPHY_STATION =
            register("lithography_station", new BlockEntityType<>(org.alex_melan.spacereloaded.electronics.LithographyStationBlockEntity::new,
                    Set.of(ModBlocks.LITHOGRAPHY_STATION)));

    // --- Тяжёлая индустрия (008) ---
    public static final BlockEntityType<org.alex_melan.spacereloaded.eclss.EclssControllerBlockEntity> ECLSS_CONTROLLER =
            register("eclss_controller", new BlockEntityType<>(org.alex_melan.spacereloaded.eclss.EclssControllerBlockEntity::new,
                    Set.of(ModBlocks.ECLSS_CONTROLLER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.nuclear.ReactorBlockEntity> REACTOR =
            register("reactor", new BlockEntityType<>(org.alex_melan.spacereloaded.nuclear.ReactorBlockEntity::new,
                    Set.of(ModBlocks.CONTROL_ROD_DRIVE)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.nuclear.CascadeBlockEntity> CASCADE =
            register("cascade", new BlockEntityType<>(org.alex_melan.spacereloaded.nuclear.CascadeBlockEntity::new,
                    Set.of(ModBlocks.CASCADE_CONTROLLER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.cryo.AirColumnBlockEntity> ASU =
            register("asu", new BlockEntityType<>(org.alex_melan.spacereloaded.cryo.AirColumnBlockEntity::new,
                    Set.of(ModBlocks.ASU_SUMP)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.cryo.AsuCompressorBlockEntity> ASU_COMPRESSOR =
            register("asu_compressor", new BlockEntityType<>(org.alex_melan.spacereloaded.cryo.AsuCompressorBlockEntity::new,
                    Set.of(ModBlocks.ASU_COMPRESSOR)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.metallurgy.ArcFurnaceBlockEntity> EAF =
            register("eaf", new BlockEntityType<>(org.alex_melan.spacereloaded.metallurgy.ArcFurnaceBlockEntity::new,
                    Set.of(ModBlocks.EAF_CONTROLLER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.comms.DsnBlockEntity> DSN =
            register("dsn", new BlockEntityType<>(org.alex_melan.spacereloaded.comms.DsnBlockEntity::new,
                    Set.of(ModBlocks.DSN_CONTROLLER)));

    // --- Жизнь на станции (007) ---
    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity> GAS_TANK =
            register("gas_tank", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.GasTankBlockEntity::new,
                    Set.of(ModBlocks.GAS_TANK)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.AirSeparatorBlockEntity> AIR_SEPARATOR =
            register("air_separator", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.AirSeparatorBlockEntity::new,
                    Set.of(ModBlocks.AIR_SEPARATOR)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.AirlockPumpBlockEntity> AIRLOCK_PUMP =
            register("airlock_pump", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.AirlockPumpBlockEntity::new,
                    Set.of(ModBlocks.AIRLOCK_PUMP)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.Co2ScrubberBlockEntity> CO2_SCRUBBER =
            register("co2_scrubber", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.Co2ScrubberBlockEntity::new,
                    Set.of(ModBlocks.CO2_SCRUBBER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity> HYDROPONIC_TRAY =
            register("hydroponic_tray", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.HydroponicTrayBlockEntity::new,
                    Set.of(ModBlocks.HYDROPONIC_TRAY)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.GrowLampBlockEntity> GROW_LAMP =
            register("grow_lamp", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.GrowLampBlockEntity::new,
                    Set.of(ModBlocks.GROW_LAMP)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.lifesupport.BiomassOxidizerBlockEntity> BIOMASS_OXIDIZER =
            register("biomass_oxidizer", new BlockEntityType<>(org.alex_melan.spacereloaded.lifesupport.BiomassOxidizerBlockEntity::new,
                    Set.of(ModBlocks.BIOMASS_OXIDIZER)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.station.SpinHubBlockEntity> SPIN_HUB =
            register("spin_hub", new BlockEntityType<>(org.alex_melan.spacereloaded.station.SpinHubBlockEntity::new,
                    Set.of(ModBlocks.SPIN_HUB)));
    public static final BlockEntityType<org.alex_melan.spacereloaded.station.DespinMotorBlockEntity> DESPIN_MOTOR =
            register("despin_motor", new BlockEntityType<>(org.alex_melan.spacereloaded.station.DespinMotorBlockEntity::new,
                    Set.of(ModBlocks.DESPIN_MOTOR)));

    public static final BlockEntityType<org.alex_melan.spacereloaded.vehicle.RoverChargerBlockEntity> ROVER_CHARGER =
            register("rover_charger", new BlockEntityType<>(org.alex_melan.spacereloaded.vehicle.RoverChargerBlockEntity::new,
                    Set.of(ModBlocks.ROVER_CHARGER)));

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
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CAPACITOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), MOTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CHEMICAL_REACTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), LITHOGRAPHY_STATION);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), DEPOSITION_REACTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), DIFFUSION_FURNACE);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ETCH_BATH);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CRYSTAL_PULLER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), AIR_SEPARATOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ECLSS_CONTROLLER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), REACTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CASCADE);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), EAF);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), AIRLOCK_PUMP);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), CO2_SCRUBBER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), GROW_LAMP);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), DESPIN_MOTOR);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), ROVER_CHARGER);
        EnergyStorage.SIDED.registerForBlockEntity((be, direction) -> be.energyStorage(), REGOLITH_REACTOR);
        EnergyStorage.SIDED.registerForBlocks((level, pos, state, blockEntity, direction) ->
                org.alex_melan.spacereloaded.industry.RegolithReactorBlockEntity.energyThroughLining(level, pos),
                ModBlocks.REFRACTORY_LINING);

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
