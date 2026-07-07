package org.alex_melan.spacereloaded;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpaceReloaded implements ModInitializer {
	public static final String MOD_ID = "spacereloaded";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("SpaceReloaded: инициализация космической программы");
	}
}
