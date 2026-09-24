package org.alex_melan.spacereloaded.registry;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;

public final class ModCreativeTab {

    public static final CreativeModeTab TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            ResourceKey.create(Registries.CREATIVE_MODE_TAB,
                    Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "main")),
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.spacereloaded"))
                    .icon(() -> new ItemStack(ModBlocks.ATMOSPHERE_CONTROLLER))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.MOON_TITANIUM_ORE);
                        output.accept(ModBlocks.MARS_TUNGSTEN_ORE);
                        output.accept(ModItems.HEAT_SHIELD);
                        for (var propellant : org.alex_melan.spacereloaded.fluid.ModFluids.all()) {
                            output.accept(propellant.bucket());
                        }
                        output.accept(ModBlocks.HULL_PLATING);
                        output.accept(ModBlocks.HERMETIC_GLASS);
                        output.accept(ModBlocks.HERMETIC_HATCH);
                        output.accept(ModBlocks.ATMOSPHERE_CONTROLLER);
                        output.accept(ModBlocks.SOLAR_PANEL);
                        output.accept(ModBlocks.RTG);
                        output.accept(ModBlocks.COAL_GENERATOR);
                        output.accept(ModBlocks.BATTERY);
                        output.accept(ModBlocks.ENERGY_CABLE);
                        output.accept(ModBlocks.CREATIVE_POWER);
                        output.accept(ModItems.OXYGEN_MASK);
                        output.accept(ModItems.OXYGEN_CANISTER);
                        output.accept(ModItems.LEAK_SCANNER);
                        output.accept(ModItems.EMPTY_CAN);
                        output.accept(ModItems.CANNED_RATION);
                        output.accept(ModItems.SPACE_SUIT_CHESTPLATE);
                        output.accept(ModItems.SPACE_SUIT_LEGGINGS);
                        output.accept(ModItems.SPACE_SUIT_BOOTS);
                        output.accept(ModBlocks.ELECTROLYZER);
                        output.accept(ModBlocks.REFINERY);
                        output.accept(ModBlocks.ATMOSPHERIC_COLLECTOR);
                        output.accept(ModBlocks.SABATIER_REACTOR);
                        output.accept(ModBlocks.OIL_SHALE);
                        output.accept(ModItems.FUELING_HOSE);
                        output.accept(ModBlocks.MOON_REGOLITH);
                        output.accept(ModBlocks.MOON_STONE);
                        output.accept(ModBlocks.MOON_ICE);
                        output.accept(ModBlocks.CRUSHER);
                        output.accept(ModBlocks.ELECTRIC_FURNACE);
                        output.accept(ModBlocks.ASSEMBLY_TABLE);
                        output.accept(ModBlocks.TITANIUM_ORE);
                        output.accept(ModBlocks.DEEPSLATE_TITANIUM_ORE);
                        output.accept(ModBlocks.DEEPSLATE_TUNGSTEN_ORE);
                        output.accept(ModItems.CARBON_DIOXIDE);
                        output.accept(ModItems.METEORIC_IRON);
                        output.accept(ModItems.RAW_TITANIUM);
                        output.accept(ModItems.RAW_TUNGSTEN);
                        output.accept(ModItems.TITANIUM_DUST);
                        output.accept(ModItems.TUNGSTEN_DUST);
                        output.accept(ModItems.IRON_DUST);
                        output.accept(ModItems.COAL_DUST);
                        output.accept(ModItems.STEEL_BLEND);
                        output.accept(ModItems.TITANIUM_INGOT);
                        output.accept(ModItems.TUNGSTEN_INGOT);
                        output.accept(ModItems.STEEL_INGOT);
                        output.accept(ModItems.TITANIUM_ALLOY_INGOT);
                        output.accept(ModItems.CARBON_FIBER);
                        output.accept(ModBlocks.ROCKET_HULL);
                        output.accept(ModBlocks.FUEL_TANK);
                        output.accept(ModBlocks.ROCKET_ENGINE);
                        output.accept(ModBlocks.COMMAND_MODULE);
                        output.accept(ModBlocks.GYROSCOPE);
                        output.accept(ModBlocks.HYDROLOX_ENGINE);
                        output.accept(ModBlocks.METHALOX_ENGINE);
                        output.accept(ModBlocks.FUELING_PUMP);
                        output.accept(ModBlocks.ROCKET_SEAT);
                        output.accept(ModBlocks.DOCKING_CLAMP);
                        output.accept(ModBlocks.STAGE_SEPARATOR);
                        output.accept(ModBlocks.SATELLITE);
                        output.accept(ModBlocks.POWER_SATELLITE);
                        output.accept(ModBlocks.RECTENNA);
                        output.accept(ModBlocks.ASTEROID_STONE);
                        output.accept(ModBlocks.RETURN_CAPSULE);
                        output.accept(ModBlocks.LANDING_BEACON);
                        output.accept(ModBlocks.VENT_GRATE);
                        output.accept(ModBlocks.CARGO_HOLD);
                        output.accept(ModBlocks.CARGO_LOADER);
                        output.accept(ModBlocks.CARGO_TERMINAL);
                        output.accept(ModBlocks.DOCKING_PORT);
                        output.accept(ModBlocks.MODULE_HULL);
                        output.accept(ModBlocks.MARS_ICE);
                        output.accept(ModBlocks.MISSION_CONTROL);
                        output.accept(ModBlocks.TELEMETRY_SCREEN);
                        output.accept(ModItems.FLIGHT_PROGRAM);
                        output.accept(ModItems.FREQUENCY_KEY);
                        output.accept(ModBlocks.INTERCEPTOR_DISH);
                        output.accept(ModBlocks.LAUNCH_PAD);
                        output.accept(ModBlocks.ASSEMBLY_PYLON);
                        output.accept(ModBlocks.ORBITAL_CANNON);
                        output.accept(ModItems.TUNGSTEN_ROD);
                        output.accept(ModItems.TARGETING_DESIGNATOR);
                        output.accept(ModBlocks.MASS_DRIVER_BREECH);
                        output.accept(ModBlocks.STEEL_COIL);
                        output.accept(ModBlocks.SUPERCONDUCTING_COIL);
                        output.accept(ModBlocks.CAPACITOR);
                        output.accept(ModItems.CARGO_POD);
                        output.accept(ModBlocks.MASS_CATCHER);
                        output.accept(ModBlocks.CATCHER_NET);
                        output.accept(ModBlocks.REGOLITH_REACTOR);
                        output.accept(ModBlocks.REFRACTORY_LINING);
                        output.accept(ModItems.SLAG);
                        output.accept(ModBlocks.SINTERED_REGOLITH);
                        output.accept(ModBlocks.LUNAR_BRICKS);
                        output.accept(ModBlocks.WOODEN_SHAFT);
                        output.accept(ModBlocks.STEEL_SHAFT);
                        output.accept(ModBlocks.SMALL_GEAR);
                        output.accept(ModBlocks.LARGE_GEAR);
                        output.accept(ModBlocks.GEARBOX);
                        output.accept(ModBlocks.CLUTCH);
                        output.accept(ModBlocks.MOTOR);
                        output.accept(ModBlocks.FLYWHEEL);
                        output.accept(ModBlocks.MECHANICAL_PRESS);
                        output.accept(ModBlocks.LATHE);
                        output.accept(ModBlocks.WIND_HUB);
                        output.accept(ModBlocks.SAIL);
                        output.accept(ModBlocks.ELECTROLYSIS_CELL);
                        output.accept(ModBlocks.DISTILLATION_TRAY);
                        output.accept(ModItems.ENGINEER_HAMMER);
                        output.accept(ModItems.ENGINEER_MANUAL);
                        output.accept(ModItems.STEEL_PLATE);
                        output.accept(ModItems.COPPER_PLATE);
                        output.accept(ModItems.TITANIUM_ALLOY_PLATE);
                        output.accept(ModItems.TURBOPUMP);
                        output.accept(ModItems.INJECTOR_PLATE);
                        output.accept(ModItems.REGEN_NOZZLE);
                        // 006: материалы и электроника
                        for (var block : new net.minecraft.world.level.block.Block[] {
                                ModBlocks.HALITE_ORE, ModBlocks.FLUORITE_ORE, ModBlocks.SPODUMENE_ORE, ModBlocks.ANORTHOSITE,
                                ModBlocks.CHEMICAL_REACTOR, ModBlocks.DEPOSITION_REACTOR, ModBlocks.CRYSTAL_PULLER,
                                ModBlocks.WAFER_SAW, ModBlocks.FAN_FILTER_UNIT, ModBlocks.DIFFUSION_FURNACE,
                                ModBlocks.LITHOGRAPHY_STATION, ModBlocks.ETCH_BATH, ModBlocks.MONO_SOLAR_PANEL,
                                ModBlocks.ALUMINIUM_FUEL_TANK, ModBlocks.AL_LI_FUEL_TANK,
                                ModBlocks.GAS_TANK, ModBlocks.AIR_SEPARATOR, ModBlocks.CO2_SCRUBBER, ModBlocks.AIRLOCK_PUMP,
                                ModBlocks.HYDROPONIC_TRAY, ModBlocks.GROW_LAMP, ModBlocks.BIOMASS_OXIDIZER}) {
                            output.accept(block);
                        }
                        for (var item : new net.minecraft.world.item.Item[] {
                                ModItems.COPPER_WIRE, ModItems.RELAY, ModItems.RELAY_LOGIC, ModItems.FERRITE_CORE,
                                ModItems.CORE_ROPE_MEMORY, ModItems.ROCK_SALT, ModItems.BRINE, ModItems.HYDROGEN_CHLORIDE,
                                ModItems.CAUSTIC_SODA, ModItems.SULFURIC_ACID, ModItems.FLUORITE, ModItems.HYDROFLUORIC_ACID,
                                ModItems.GYPSUM, ModItems.SILICON_BLEND, ModItems.METALLURGICAL_SILICON, ModItems.CARBON_MONOXIDE,
                                ModItems.TRICHLOROSILANE, ModItems.POLYSILICON, ModItems.SILICON_DUST, ModItems.PHOSPHATE_BLEND,
                                ModItems.PHOSPHORUS, ModItems.QUARTZ_CRUCIBLE, ModItems.SILICON_BOULE,
                                ModItems.MULTICRYSTALLINE_SILICON, ModItems.SILICON_WAFER, ModItems.MULTICRYSTALLINE_WAFER,
                                ModItems.SOLAR_CELL, ModItems.MONO_SOLAR_CELL, ModItems.SAPPHIRE, ModItems.SAPPHIRE_WAFER,
                                ModItems.SOS_WAFER, ModItems.PHOTORESIST, ModItems.PHOTOMASK_LOGIC,
                                ModItems.PHOTOMASK_MICROPROCESSOR, ModItems.PHOTOMASK_RADHARD, ModItems.DIE_LOGIC,
                                ModItems.DIE_MICROPROCESSOR, ModItems.DIE_RADHARD, ModItems.CERAMIC_PACKAGE, ModItems.LOGIC_CHIP,
                                ModItems.MICROPROCESSOR, ModItems.RADHARD_PROCESSOR, ModItems.GLASS_FIBER, ModItems.EPOXY_RESIN,
                                ModItems.FR4_LAMINATE, ModItems.COPPER_CLAD_LAMINATE, ModItems.INCOMPLETE_CIRCUIT_BOARD,
                                ModItems.CIRCUIT_BOARD, ModItems.FLIGHT_COMPUTER, ModItems.ALUMINA, ModItems.CRYOLITE,
                                ModItems.ALUMINIUM_INGOT, ModItems.AL_CU_BLEND, ModItems.ALUMINIUM_COPPER_INGOT,
                                ModItems.SPODUMENE, ModItems.BETA_SPODUMENE, ModItems.LITHIUM_CHLORIDE, ModItems.LITHIUM_INGOT,
                                ModItems.AL_LI_BLEND, ModItems.ALUMINIUM_LITHIUM_INGOT, ModItems.NICKEL_INGOT,
                                ModItems.SUPERALLOY_BLEND, ModItems.NICKEL_SUPERALLOY_INGOT,
                                ModItems.LITHIUM_HYDROXIDE, ModItems.LIOH_CARTRIDGE, ModItems.ZEOLITE, ModItems.ZEOLITE_BED, ModItems.STRAW}) {
                            output.accept(item);
                        }
                    })
                    .build());

    public static void init() {
    }

    private ModCreativeTab() {
    }
}
