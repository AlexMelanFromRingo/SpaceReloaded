package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.BatteryMenu;
import org.alex_melan.spacereloaded.machine.AssemblyTableMenu;
import org.alex_melan.spacereloaded.machine.ElectrolyzerMenu;
import org.alex_melan.spacereloaded.machine.GeneratorMenu;
import org.alex_melan.spacereloaded.machine.RefineryMenu;
import org.alex_melan.spacereloaded.machine.SingleInputMachineMenu;

/**
 * Типы меню. Конструктор MenuType вскрыт classtweaker'ом fabric-menu-api-v1
 * (в 26.2 он заменил fabric-screen-handler-api-v1).
 */
public final class ModMenus {

    public static final MenuType<SingleInputMachineMenu> CRUSHER = register("crusher",
            new MenuType<>((containerId, inventory) ->
                    SingleInputMachineMenu.client(ModMenus.CRUSHER, containerId, inventory),
                    FeatureFlags.VANILLA_SET));

    public static final MenuType<SingleInputMachineMenu> ELECTRIC_FURNACE = register("electric_furnace",
            new MenuType<>((containerId, inventory) ->
                    SingleInputMachineMenu.client(ModMenus.ELECTRIC_FURNACE, containerId, inventory, 3),
                    FeatureFlags.VANILLA_SET));

    public static final MenuType<AssemblyTableMenu> ASSEMBLY_TABLE = register("assembly_table",
            new MenuType<>(AssemblyTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<GeneratorMenu> COAL_GENERATOR = register("coal_generator",
            new MenuType<>(GeneratorMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<BatteryMenu> BATTERY = register("battery",
            new MenuType<>(BatteryMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<ElectrolyzerMenu> ELECTROLYZER = register("electrolyzer",
            new MenuType<>(ElectrolyzerMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<RefineryMenu> REFINERY = register("refinery",
            new MenuType<>(RefineryMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<org.alex_melan.spacereloaded.industry.RegolithReactorMenu> REGOLITH_REACTOR =
            register("regolith_reactor", new MenuType<>(
                    org.alex_melan.spacereloaded.industry.RegolithReactorMenu::new, FeatureFlags.VANILLA_SET));

    // --- 006: процессные машины (одно меню по раскладке) ---
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> CHEMICAL_REACTOR =
            process("chemical_reactor", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.CHEMICAL_REACTOR);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> SABATIER_REACTOR =
            process("sabatier_reactor", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.SABATIER_REACTOR);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> DEPOSITION_REACTOR =
            process("deposition_reactor", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.DEPOSITION_REACTOR);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> DIFFUSION_FURNACE =
            process("diffusion_furnace", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.DIFFUSION_FURNACE);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> LITHOGRAPHY_STATION =
            process("lithography_station", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.LITHOGRAPHY_STATION);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> BIOMASS_OXIDIZER =
            process("biomass_oxidizer", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.BIOMASS_OXIDIZER);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> CO2_SCRUBBER =
            process("co2_scrubber", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.CO2_SCRUBBER);
    public static final MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> ETCH_BATH =
            process("etch_bath", org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.ETCH_BATH);

    private static MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu> process(
            String name, org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout layout) {
        MenuType<org.alex_melan.spacereloaded.electronics.ProcessMenu>[] self = new MenuType[1];
        self[0] = register(name, new MenuType<>((id, inventory) ->
                org.alex_melan.spacereloaded.electronics.ProcessMenu.client(self[0], layout, id, inventory),
                FeatureFlags.VANILLA_SET));
        return self[0];
    }

    private static <T extends net.minecraft.world.inventory.AbstractContainerMenu> MenuType<T> register(
            String name, MenuType<T> type) {
        return Registry.register(BuiltInRegistries.MENU,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name), type);
    }

    public static void init() {
    }

    private ModMenus() {
    }
}
