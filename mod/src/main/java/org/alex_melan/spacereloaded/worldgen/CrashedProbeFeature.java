package org.alex_melan.spacereloaded.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.RandomizableContainer;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.storage.loot.LootTable;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Место крушения зонда прошлых миссий (004, FR-242, D39): неглубокий ударный кратер
 * радиуса 3–4, вокруг — 6–20 обломков (корпус, обшивка, панель, разделитель, бак: они же
 * добыча), в центре — уцелевший контейнер с таблицей добычи {@code chests/crashed_probe}.
 * Боёвки нет — ниша данжа закрыта исследованием (backlog «лунные POI без боёвки»).
 */
public class CrashedProbeFeature extends Feature<NoneFeatureConfiguration> {

    public static final ResourceKey<LootTable> LOOT = ResourceKey.create(Registries.LOOT_TABLE,
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "chests/crashed_probe"));

    public CrashedProbeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState regolith = ModBlocks.MOON_REGOLITH.defaultBlockState();

        int radius = 3 + random.nextInt(2);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                int bowl = (int) Math.round(2 * (1 - (d / radius) * (d / radius)));
                for (int y = top; y > top - bowl; y--) {
                    cursor.set(x, y, z);
                    if (isTerrain(level.getBlockState(cursor))) {
                        level.setBlock(cursor, air, 2);
                    }
                }
                cursor.set(x, top - bowl, z);
                if (isTerrain(level.getBlockState(cursor))) {
                    level.setBlock(cursor, regolith, 2);
                }
            }
        }

        BlockState[] debris = {
                ModBlocks.ROCKET_HULL.defaultBlockState(),
                ModBlocks.HULL_PLATING.defaultBlockState(),
                ModBlocks.SOLAR_PANEL.defaultBlockState(),
                ModBlocks.STAGE_SEPARATOR.defaultBlockState(),
                ModBlocks.FUEL_TANK.defaultBlockState()
        };
        int pieces = 6 + random.nextInt(15);
        for (int i = 0; i < pieces; i++) {
            int x = origin.getX() + random.nextInt(15) - 7;
            int z = origin.getZ() + random.nextInt(15) - 7;
            int y = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);
            cursor.set(x, y, z);
            if (level.getBlockState(cursor).isAir()) {
                level.setBlock(cursor, debris[random.nextInt(debris.length)], 2);
            }
        }

        int cy = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, origin.getX(), origin.getZ());
        BlockPos chest = new BlockPos(origin.getX(), cy, origin.getZ());
        level.setBlock(chest, Blocks.BARREL.defaultBlockState(), 2);
        RandomizableContainer.setBlockEntityLootTable(level, random, chest, LOOT);
        return true;
    }

    private static boolean isTerrain(BlockState state) {
        return state.is(ModBlocks.MOON_STONE) || state.is(ModBlocks.MOON_REGOLITH);
    }
}
