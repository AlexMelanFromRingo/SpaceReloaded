package org.alex_melan.spacereloaded.client;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.client.gui.BatteryScreen;
import org.alex_melan.spacereloaded.client.gui.GeneratorScreen;
import org.alex_melan.spacereloaded.client.gui.MachineScreen;
import org.alex_melan.spacereloaded.machine.MachineMenu;
import org.alex_melan.spacereloaded.registry.ModMenus;

public class SpaceReloadedClient implements ClientModInitializer {

	private static final Identifier SINGLE_GUI = Identifier.fromNamespaceAndPath(
			SpaceReloaded.MOD_ID, "textures/gui/machine_single.png");
	private static final Identifier ASSEMBLY_GUI = Identifier.fromNamespaceAndPath(
			SpaceReloaded.MOD_ID, "textures/gui/machine_assembly.png");

	@Override
	public void onInitializeClient() {
		// MenuScreens.register вскрыт classtweaker'ом fabric-menu-api-v1;
		// параметры типов явно: экран объявлен над базовым MachineMenu
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.CRUSHER, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, SINGLE_GUI, 72, 35));
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.ELECTRIC_FURNACE, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, SINGLE_GUI, 72, 35));
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.ASSEMBLY_TABLE, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, ASSEMBLY_GUI, 105, 35));
		MenuScreens.register(ModMenus.COAL_GENERATOR, GeneratorScreen::new);
		MenuScreens.register(ModMenus.BATTERY, BatteryScreen::new);
	}
}
