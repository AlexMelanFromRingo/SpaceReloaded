package org.alex_melan.spacereloaded.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.alex_melan.spacereloaded.core.worldgen.LavaTubeLayout;
import org.alex_melan.spacereloaded.registry.ModBlocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Лавовые трубки Луны (004, FR-240, D38). Размещается один раз на чанк без смещения:
 * перебирает регионы 128×128 вокруг чанка, берёт их детерминированные трубки
 * ({@link LavaTubeLayout#forRegion}) и вырезает ТОЛЬКО клетки своего чанка — куски трубки
 * из соседних чанков сшиваются без общего состояния (тест ядра). Трогает только породу тела.
 */
public class LavaTubeFeature extends Feature<NoneFeatureConfiguration> {

    /** Доля регионов с трубкой (≈ одна трубка на 8×8 чанков). */
    public static final double CHANCE_PER_REGION = 0.85;

    public LavaTubeFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        int chunkMinX = (origin.getX() >> 4) << 4;
        int chunkMinZ = (origin.getZ() >> 4) << 4;
        int regionX = Math.floorDiv(chunkMinX, LavaTubeLayout.REGION);
        int regionZ = Math.floorDiv(chunkMinZ, LavaTubeLayout.REGION);
        int reach = LavaTubeLayout.regionRadius();
        long seed = level.getSeed();

        List<LavaTubeLayout> tubes = new ArrayList<>();
        for (int rx = regionX - reach; rx <= regionX + reach; rx++) {
            for (int rz = regionZ - reach; rz <= regionZ + reach; rz++) {
                Optional<LavaTubeLayout> tube = LavaTubeLayout.forRegion(seed, rx, rz, CHANCE_PER_REGION);
                if (tube.isPresent() && tube.get().touchesChunk(chunkMinX, chunkMinZ)) {
                    tubes.add(tube.get());
                }
            }
        }
        if (tubes.isEmpty()) {
            return false;
        }
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState floor = ModBlocks.MOON_STONE.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        boolean placed = false;
        for (int x = chunkMinX; x < chunkMinX + 16; x++) {
            for (int z = chunkMinZ; z < chunkMinZ + 16; z++) {
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
                for (LavaTubeLayout tube : tubes) {
                    int bottom = surface - (int) Math.ceil(tube.maxCenterDepth() + tube.halfHeight()) - 1;
                    int highestCavity = Integer.MIN_VALUE;
                    for (int y = bottom; y <= surface - 1; y++) {
                        if (!tube.contains(x + 0.5, y + 0.5, z + 0.5, surface)) {
                            continue;
                        }
                        cursor.set(x, y, z);
                        if (isTerrain(level.getBlockState(cursor))) {
                            level.setBlock(cursor, air, 2);
                            placed = true;
                        }
                        highestCavity = Math.max(highestCavity, y);
                        cursor.set(x, y - 1, z);
                        if (level.getBlockState(cursor).is(ModBlocks.MOON_REGOLITH)) {
                            level.setBlock(cursor, floor, 2); // застывший базальт пола
                        }
                    }
                    // Провал-окно: колодец от потолка полости до поверхности
                    if (highestCavity != Integer.MIN_VALUE && tube.isSkylight(x + 0.5, z + 0.5)) {
                        for (int y = highestCavity + 1; y <= surface; y++) {
                            cursor.set(x, y, z);
                            if (isTerrain(level.getBlockState(cursor))) {
                                level.setBlock(cursor, air, 2);
                            }
                        }
                    }
                }
            }
        }
        return placed;
    }

    private static boolean isTerrain(BlockState state) {
        return state.is(ModBlocks.MOON_STONE) || state.is(ModBlocks.MOON_REGOLITH)
                || state.is(ModBlocks.MOON_TITANIUM_ORE);
    }
}
