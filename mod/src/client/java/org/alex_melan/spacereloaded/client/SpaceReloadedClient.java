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
	/** Электропечь 006: второй выход — побочный продукт (CO карботермии, шлак Вёлера). */
	private static final Identifier FURNACE_GUI = Identifier.fromNamespaceAndPath(
			SpaceReloaded.MOD_ID, "textures/gui/machine_furnace.png");

	@Override
	public void onInitializeClient() {
		// MenuScreens.register вскрыт classtweaker'ом fabric-menu-api-v1;
		// параметры типов явно: экран объявлен над базовым MachineMenu
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.CRUSHER, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, SINGLE_GUI, 72, 35));
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.ELECTRIC_FURNACE, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, FURNACE_GUI, 72, 35));
		MenuScreens.<MachineMenu, MachineScreen>register(ModMenus.ASSEMBLY_TABLE, (menu, inventory, title) ->
				new MachineScreen(menu, inventory, title, ASSEMBLY_GUI, 105, 35));
		MenuScreens.register(ModMenus.COAL_GENERATOR, GeneratorScreen::new);
		MenuScreens.register(ModMenus.BATTERY, BatteryScreen::new);
		MenuScreens.register(ModMenus.ELECTROLYZER, ElectrolyzerScreen::new);
		MenuScreens.register(ModMenus.REFINERY, RefineryScreen::new);
		MenuScreens.register(ModMenus.REGOLITH_REACTOR,
				org.alex_melan.spacereloaded.client.gui.RegolithReactorScreen::new);
		// 006: процессные машины — одно окно по раскладке меню
		for (var type : java.util.List.of(ModMenus.CHEMICAL_REACTOR, ModMenus.SABATIER_REACTOR, ModMenus.DEPOSITION_REACTOR,
				ModMenus.DIFFUSION_FURNACE, ModMenus.LITHOGRAPHY_STATION, ModMenus.ETCH_BATH, ModMenus.CO2_SCRUBBER, ModMenus.BIOMASS_OXIDIZER)) {
			MenuScreens.register(type, org.alex_melan.spacereloaded.client.gui.ProcessScreen::new);
		}

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
		EntityRendererRegistry.register(ModEntities.ROVER, org.alex_melan.spacereloaded.client.render.RoverRenderer::new);
		// 004: анимации мультиблоков и визуальная капсула
		EntityRendererRegistry.register(ModEntities.CARGO_POD,
				org.alex_melan.spacereloaded.client.render.CargoPodRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.MASS_DRIVER_BREECH,
				org.alex_melan.spacereloaded.client.render.MassDriverRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.ECLSS_CONTROLLER,
				org.alex_melan.spacereloaded.client.render.EclssRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.REACTOR,
				org.alex_melan.spacereloaded.client.render.ReactorRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.CASCADE,
				org.alex_melan.spacereloaded.client.render.CascadeRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.REGOLITH_REACTOR,
				org.alex_melan.spacereloaded.client.render.RegolithReactorRenderer::new);
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(
				org.alex_melan.spacereloaded.registry.ModBlockEntities.SOLAR_PANEL,
				org.alex_melan.spacereloaded.client.render.SolarPanelRenderer::new);
		// 005: вращение механики
		for (var type : java.util.List.of(org.alex_melan.spacereloaded.registry.ModBlockEntities.KINETIC,
				org.alex_melan.spacereloaded.registry.ModBlockEntities.MOTOR,
				org.alex_melan.spacereloaded.registry.ModBlockEntities.FLYWHEEL,
				org.alex_melan.spacereloaded.registry.ModBlockEntities.PRESS,
				org.alex_melan.spacereloaded.registry.ModBlockEntities.LATHE,
				org.alex_melan.spacereloaded.registry.ModBlockEntities.WIND_HUB)) {
			registerKinetic(type);
		}
		EntityRendererRegistry.register(ModEntities.KINETIC_PROJECTILE,
				org.alex_melan.spacereloaded.client.render.KineticProjectileRenderer::new);
		EntityRendererRegistry.register(ModEntities.METEOR,
				org.alex_melan.spacereloaded.client.render.MeteorRenderer::new);

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.VacuumStatePayload.TYPE,
				(payload, context) -> VacuumAmbience.setExposed(payload.exposed()));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.SpinSkyPayload.TYPE,
				(payload, context) -> SpinSky.set(payload.axis(), payload.omega()));
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.MachineStatusPayload.TYPE,
				(payload, context) -> {
					var open = org.alex_melan.spacereloaded.client.gui.MachineStatusScreen.active();
					if (open != null && open.shows(payload)) {
						open.update(payload);
					} else {
						context.client().setScreenAndShow(new org.alex_melan.spacereloaded.client.gui.MachineStatusScreen(payload));
					}
				});
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.network.CabinGasPayload.TYPE,
				(payload, context) -> org.alex_melan.spacereloaded.client.gui.CabinGasHud.update(payload));

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

		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.registerGlobalReceiver(
				org.alex_melan.spacereloaded.multiblock.OpenManualPayload.TYPE,
				(payload, context) -> context.client().setScreenAndShow(
						new org.alex_melan.spacereloaded.client.gui.EngineerManualScreen()));
		registerPlanetMapKey();
		registerStageKey();
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
				(handler, client) -> VacuumAmbience.setExposed(false));

		HudElementRegistry.addLast(RocketHud.ID, new RocketHud());
		HudElementRegistry.addLast(org.alex_melan.spacereloaded.client.gui.OxygenHud.ID,
				new org.alex_melan.spacereloaded.client.gui.OxygenHud());
		HudElementRegistry.addLast(org.alex_melan.spacereloaded.client.gui.CabinGasHud.ID,
				new org.alex_melan.spacereloaded.client.gui.CabinGasHud());
	}

	/**
	 * Клавиша отделения ступени (по умолчанию X, Полёт 2.0): шлёт пустой C2S-пакет,
	 * только если игрок сидит в ракете; право пилота проверяет сервер.
	 */
	private void registerStageKey() {
		net.minecraft.client.KeyMapping key = net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper
				.registerKeyMapping(new net.minecraft.client.KeyMapping(
						"key.spacereloaded.stage",
						com.mojang.blaze3d.platform.InputConstants.Type.KEYSYM,
						org.lwjgl.glfw.GLFW.GLFW_KEY_X,
						net.minecraft.client.KeyMapping.Category.GAMEPLAY));
		net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (key.consumeClick()) {
				if (client.player != null
						&& client.player.getVehicle() instanceof org.alex_melan.spacereloaded.rocket.RocketEntity) {
					net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
							new org.alex_melan.spacereloaded.network.StageSeparatePayload());
				}
			}
		});
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

	private static <T extends org.alex_melan.spacereloaded.kinetics.KineticBlockEntity> void registerKinetic(
			net.minecraft.world.level.block.entity.BlockEntityType<T> type) {
		net.fabricmc.fabric.api.client.rendering.v1.BlockEntityRendererRegistry.register(type,
				context -> new org.alex_melan.spacereloaded.client.render.KineticRenderer<T>(context));
	}
}
