package org.alex_melan.spacereloaded.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import org.alex_melan.spacereloaded.client.gui.RocketHud;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.client.gui.BatteryScreen;
import org.alex_melan.spacereloaded.client.gui.ElectrolyzerScreen;
import org.alex_melan.spacereloaded.client.gui.RefineryScreen;
import org.alex_melan.spacereloaded.client.gui.GeneratorScreen;
import org.alex_melan.spacereloaded.client.gui.MachineScreen;
import org.alex_melan.spacereloaded.machine.MachineMenu;
import org.alex_melan.spacereloaded.client.render.RocketRenderer;
import org.alex_melan.spacereloaded.registry.ModEntities;
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
		MenuScreens.register(ModMenus.ELECTROLYZER, ElectrolyzerScreen::new);
		MenuScreens.register(ModMenus.REFINERY, RefineryScreen::new);

		// Топливо-жидкости: текстуры уже окрашены, тинт нейтральный
		for (var propellant : org.alex_melan.spacereloaded.fluid.ModFluids.all()) {
			String name = propellant.fuelId().substring(propellant.fuelId().indexOf(':') + 1);
			var model = new net.minecraft.client.renderer.block.FluidModel.Unbaked(
					new net.minecraft.client.resources.model.sprite.Material(
							Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "block/" + name + "_still")),
					new net.minecraft.client.resources.model.sprite.Material(
							Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "block/" + name + "_flow")),
					null,
					net.minecraft.client.color.block.BlockTintSources.constant(0xFFFFFF));
			net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
					propellant.flowing(), propellant.source(), model);
			net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry
					.setBlockTransparency(propellant.block(), true);
		}

		EntityRendererRegistry.register(ModEntities.ROCKET, RocketRenderer::new);
		EntityRendererRegistry.register(ModEntities.KINETIC_PROJECTILE,
				org.alex_melan.spacereloaded.client.render.KineticProjectileRenderer::new);
		EntityRendererRegistry.register(ModEntities.METEOR,
				org.alex_melan.spacereloaded.client.render.MeteorRenderer::new);

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.VacuumStatePayload.TYPE,
				(payload, context) -> VacuumAmbience.setExposed(payload.exposed()));

		// Терминал орудия: первый пакет открывает экран, следующие его обновляют
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.CannonStatePayload.TYPE,
				(payload, context) -> {
					var open = org.alex_melan.spacereloaded.client.gui.CannonTerminalScreen.active();
					if (open != null) {
						open.update(payload);
					} else {
						context.client().setScreenAndShow(
								new org.alex_melan.spacereloaded.client.gui.CannonTerminalScreen(payload));
					}
				});
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.ScanReportPayload.TYPE,
				(payload, context) -> context.client().setScreenAndShow(
						new org.alex_melan.spacereloaded.client.gui.ScanReportScreen(payload)));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.PlanetMapPayload.TYPE,
				(payload, context) -> context.client().setScreenAndShow(
						new org.alex_melan.spacereloaded.client.gui.PlanetMapScreen(payload)));

		registerPlanetMapKey();
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> VacuumAmbience.setExposed(false));

		HudElementRegistry.addLast(RocketHud.ID, new RocketHud());
		HudElementRegistry.addLast(org.alex_melan.spacereloaded.client.gui.OxygenHud.ID,
				new org.alex_melan.spacereloaded.client.gui.OxygenHud());
	}

	/** Клавиша карты полёта (по умолчанию M): работает где угодно, не только в ракете. */
	private void registerPlanetMapKey() {
		net.minecraft.client.KeyMapping key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
				.registerKeyMapping(new net.minecraft.client.KeyMapping(
						"key.spacereloaded.planet_map",
						com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
						org.lwjgl.glfw.GLFW.GLFW_KEY_M,
						net.minecraft.client.KeyMapping.Category.GAMEPLAY));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (key.consumeClick()) {
				if (client.player != null) {
					// Карту рисует сервер-ответ: покрытие спутников клиенту неизвестно
					net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
							new org.alex_melan.spacereloaded.network.OpenPlanetMapPayload(
									client.player.getVehicle()
											instanceof org.alex_melan.spacereloaded.rocket.RocketEntity));
				}
			}
		});
	}
}
