package org.alex_melan.spacereloaded;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import org.alex_melan.spacereloaded.command.SpaceReloadedCommands;
import org.alex_melan.spacereloaded.energy.CableNetworkManager;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModCreativeTab;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModRecipes;
import org.alex_melan.spacereloaded.registry.ModRegistries;
import org.alex_melan.spacereloaded.registry.ModWorldgen;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.rocket.RocketInteractions;
import org.alex_melan.spacereloaded.sealing.VacuumHazard;
import org.alex_melan.spacereloaded.sealing.ZoneManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpaceReloaded implements ModInitializer {
	public static final String MOD_ID = "spacereloaded";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static SpaceReloadedConfig config;

	public static SpaceReloadedConfig config() {
		if (config == null) {
			config = SpaceReloadedConfig.load(FabricLoader.getInstance().getConfigDir());
		}
		return config;
	}

	@Override
	public void onInitialize() {
		LOGGER.info("SpaceReloaded: инициализация космической программы");
		config();

		net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry.clientboundPlay().register(
				org.alex_melan.spacereloaded.network.VacuumStatePayload.TYPE,
				org.alex_melan.spacereloaded.network.VacuumStatePayload.CODEC);
		ModBlocks.init();
		ModDataComponents.init();
		ModItems.init();
		ModBlockEntities.init();
		ModCreativeTab.init();
		ModRecipes.init();
		ModMenus.init();
		ModRegistries.init();
		ModWorldgen.init();
		ModEntities.init();
		org.alex_melan.spacereloaded.planet.ModTickets.init();

		// Инвалидация зон по событиям (T024). Изменения складываются в отложенную
		// очередь и обрабатываются в конце тика: к этому моменту BlockState уже
		// обновлён (в т.ч. после установки блока и открытия двери).
		PlayerBlockBreakEvents.AFTER.register((level, player, pos, state, blockEntity) -> {
			if (level instanceof ServerLevel serverLevel) {
				ZoneManager.markBlockChanged(serverLevel, pos);
				CableNetworkManager.markBlockChanged(serverLevel, pos);
			}
		});
		UseBlockCallback.EVENT.register((player, level, hand, hitResult) -> {
			BlockPos usePos = hitResult.getBlockPos();
			// Заправочный рукав по баку — подключение
			if (player.getItemInHand(hand).is(org.alex_melan.spacereloaded.registry.ModItems.FUELING_HOSE)
					&& level.getBlockState(usePos).is(ModBlocks.FUEL_TANK)) {
				if (level instanceof ServerLevel serverLevel
						&& player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					org.alex_melan.spacereloaded.rocket.FuelingHose.link(serverPlayer, serverLevel, usePos);
					return InteractionResult.SUCCESS_SERVER;
				}
				return InteractionResult.SUCCESS;
			}
			// Подсказка: сборка теперь со стартовой площадки (ПКМ по пилону)
			if (!player.isSecondaryUseActive()
					&& level.getBlockState(usePos).is(ModBlocks.COMMAND_MODULE)) {
				if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
					serverPlayer.sendSystemMessage(net.minecraft.network.chat.Component
							.translatable("message.spacereloaded.assembly.use_pylon"));
				}
				return InteractionResult.SUCCESS_SERVER;
			}
			if (level instanceof ServerLevel serverLevel) {
				BlockPos pos = hitResult.getBlockPos();
				// Клик мог открыть дверь на месте либо поставить блок в соседнюю ячейку
				ZoneManager.markBlockChanged(serverLevel, pos);
				ZoneManager.markBlockChanged(serverLevel, pos.relative(hitResult.getDirection()));
				CableNetworkManager.markBlockChanged(serverLevel, pos);
				CableNetworkManager.markBlockChanged(serverLevel, pos.relative(hitResult.getDirection()));
			}
			return InteractionResult.PASS;
		});
		// TODO T024: взрывы — Fabric-события взрыва в 26.2 нет, потребуется mixin

		ServerTickEvents.END_LEVEL_TICK.register(level -> {
			ZoneManager.processDeferred(level);
			CableNetworkManager.processDirty(level);
			CableNetworkManager.tick(level);
			VacuumHazard.tick(level);
			org.alex_melan.spacereloaded.planet.PlanetEffects.tick(level);
		});
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			ZoneManager.shutdown();
			CableNetworkManager.clearAll();
			org.alex_melan.spacereloaded.rocket.FuelingHose.clearAll();
			org.alex_melan.spacereloaded.sealing.VacuumHazard.clearAll();
		});

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				SpaceReloadedCommands.register(dispatcher));
	}
}
