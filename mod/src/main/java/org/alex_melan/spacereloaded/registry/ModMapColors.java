package org.alex_melan.spacereloaded.registry;

import net.minecraft.world.level.material.MapColor;

import java.util.Map;

/**
 * Цвет блоков мода на картах и орбитальных снимках — «вид сверху»: ближайший цвет палитры к среднему
 * цвету верхних граней модели. Сгенерировано tools/gen_map_colors.py, руками не править.
 */
public final class ModMapColors {

    private static final Map<String, MapColor> COLORS = Map.ofEntries(
        Map.entry("air_separator", MapColor.METAL),
        Map.entry("airlock_pump", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("al_li_fuel_tank", MapColor.TERRACOTTA_CYAN),
        Map.entry("aluminium_fuel_tank", MapColor.STONE),
        Map.entry("anorthosite", MapColor.STONE),
        Map.entry("assembly_pylon", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("assembly_table", MapColor.DEEPSLATE),
        Map.entry("asteroid_stone", MapColor.COLOR_GRAY),
        Map.entry("atmosphere_controller", MapColor.STONE),
        Map.entry("atmospheric_collector", MapColor.STONE),
        Map.entry("battery", MapColor.COLOR_GRAY),
        Map.entry("capacitor", MapColor.WOOL),
        Map.entry("cargo_hold", MapColor.DEEPSLATE),
        Map.entry("cargo_loader", MapColor.COLOR_GRAY),
        Map.entry("cargo_terminal", MapColor.COLOR_GRAY),
        Map.entry("catcher_net", MapColor.COLOR_GRAY),
        Map.entry("clutch", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("coal_generator", MapColor.STONE),
        Map.entry("command_module", MapColor.WOOL),
        Map.entry("creative_power", MapColor.TERRACOTTA_LIGHT_GRAY),
        Map.entry("crusher", MapColor.STONE),
        Map.entry("crystal_puller", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("deepslate_titanium_ore", MapColor.TERRACOTTA_CYAN),
        Map.entry("deepslate_tungsten_ore", MapColor.COLOR_GRAY),
        Map.entry("despin_motor", MapColor.STONE),
        Map.entry("distillation_tray", MapColor.WOOL),
        Map.entry("docking_clamp", MapColor.DEEPSLATE),
        Map.entry("electric_furnace", MapColor.STONE),
        Map.entry("electrolysis_cell", MapColor.CLAY),
        Map.entry("electrolyzer", MapColor.STONE),
        Map.entry("energy_cable", MapColor.PODZOL),
        Map.entry("fan_filter_unit", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("fluorite_ore", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("fuel_tank", MapColor.STONE),
        Map.entry("fueling_pump", MapColor.STONE),
        Map.entry("gas_tank", MapColor.CLAY),
        Map.entry("gearbox", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("grow_lamp", MapColor.DEEPSLATE),
        Map.entry("gyroscope", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("halite_ore", MapColor.TERRACOTTA_WHITE),
        Map.entry("hermetic_hatch", MapColor.PODZOL),
        Map.entry("hull_plating", MapColor.STONE),
        Map.entry("hydrolox_engine", MapColor.STONE),
        Map.entry("hydroponic_tray", MapColor.COLOR_CYAN),
        Map.entry("imaging_satellite", MapColor.STONE),
        Map.entry("interceptor_dish", MapColor.TERRACOTTA_CYAN),
        Map.entry("landing_beacon", MapColor.TERRACOTTA_CYAN),
        Map.entry("lathe", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("launch_pad", MapColor.COLOR_GRAY),
        Map.entry("lunar_bricks", MapColor.COLOR_GRAY),
        Map.entry("mars_ice", MapColor.TERRACOTTA_YELLOW),
        Map.entry("mars_tungsten_ore", MapColor.PODZOL),
        Map.entry("mass_catcher", MapColor.TERRACOTTA_WHITE),
        Map.entry("mass_driver_breech", MapColor.COLOR_GRAY),
        Map.entry("mechanical_press", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("methalox_engine", MapColor.STONE),
        Map.entry("mission_control", MapColor.TERRACOTTA_CYAN),
        Map.entry("module_hull", MapColor.CLAY),
        Map.entry("mono_solar_panel", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("moon_ice", MapColor.WOOL),
        Map.entry("moon_regolith", MapColor.METAL),
        Map.entry("moon_stone", MapColor.DEEPSLATE),
        Map.entry("moon_titanium_ore", MapColor.STONE),
        Map.entry("motor", MapColor.WOOL),
        Map.entry("oil_shale", MapColor.COLOR_GRAY),
        Map.entry("orbital_cannon", MapColor.DEEPSLATE),
        Map.entry("power_satellite", MapColor.GLOW_LICHEN),
        Map.entry("rectenna", MapColor.WOOL),
        Map.entry("refinery", MapColor.STONE),
        Map.entry("refractory_lining", MapColor.DEEPSLATE),
        Map.entry("regolith_reactor", MapColor.DEEPSLATE),
        Map.entry("return_capsule", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("rim_thruster", MapColor.STONE),
        Map.entry("rocket_engine", MapColor.STONE),
        Map.entry("rocket_hull", MapColor.WOOL),
        Map.entry("rocket_seat", MapColor.TERRACOTTA_BLUE),
        Map.entry("rover_charger", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("rtg", MapColor.DIRT),
        Map.entry("sabatier_reactor", MapColor.STONE),
        Map.entry("sail", MapColor.QUARTZ),
        Map.entry("satellite", MapColor.GLOW_LICHEN),
        Map.entry("sintered_regolith", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("solar_panel", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("spin_hub", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("spodumene_ore", MapColor.METAL),
        Map.entry("stage_separator", MapColor.COLOR_GRAY),
        Map.entry("steel_coil", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("superconducting_coil", MapColor.CLAY),
        Map.entry("telemetry_screen", MapColor.STONE),
        Map.entry("titanium_ore", MapColor.TERRACOTTA_LIGHT_BLUE),
        Map.entry("vent_grate", MapColor.COLOR_GRAY),
        Map.entry("wafer_saw", MapColor.COLOR_LIGHT_GRAY),
        Map.entry("wind_hub", MapColor.WOOL));

    private ModMapColors() {
    }

    /** Цвет блока на карте; NONE — прозрачен сверху (стекло) или невидимая модель. */
    public static MapColor of(String block) {
        return COLORS.getOrDefault(block, MapColor.NONE);
    }
}
