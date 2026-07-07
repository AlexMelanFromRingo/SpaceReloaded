package org.alex_melan.spacereloaded.registry;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.alex_melan.spacereloaded.SpaceReloaded;

/**
 * Генерация руд (T040): configured/placed features лежат датапаком в
 * data/spacereloaded/worldgen/, сюда — только привязка к биомам Оверворлда.
 */
public final class ModWorldgen {

    public static final ResourceKey<PlacedFeature> ORE_TITANIUM = placed("ore_titanium");
    public static final ResourceKey<PlacedFeature> ORE_TUNGSTEN = placed("ore_tungsten");
    public static final ResourceKey<PlacedFeature> OIL_SHALE = placed("oil_shale");

    private static ResourceKey<PlacedFeature> placed(String name) {
        return ResourceKey.create(Registries.PLACED_FEATURE,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name));
    }

    public static void init() {
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TITANIUM);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, ORE_TUNGSTEN);
        BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.UNDERGROUND_ORES, OIL_SHALE);
    }

    private ModWorldgen() {
    }
}
