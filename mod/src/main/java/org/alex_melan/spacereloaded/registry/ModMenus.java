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
                    SingleInputMachineMenu.client(ModMenus.ELECTRIC_FURNACE, containerId, inventory),
                    FeatureFlags.VANILLA_SET));

    public static final MenuType<AssemblyTableMenu> ASSEMBLY_TABLE = register("assembly_table",
            new MenuType<>(AssemblyTableMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<GeneratorMenu> COAL_GENERATOR = register("coal_generator",
            new MenuType<>(GeneratorMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<BatteryMenu> BATTERY = register("battery",
            new MenuType<>(BatteryMenu::new, FeatureFlags.VANILLA_SET));

    public static final MenuType<ElectrolyzerMenu> ELECTROLYZER = register("electrolyzer",
            new MenuType<>(ElectrolyzerMenu::new, FeatureFlags.VANILLA_SET));

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
