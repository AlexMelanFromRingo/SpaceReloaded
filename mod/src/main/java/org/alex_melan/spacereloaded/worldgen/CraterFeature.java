package org.alex_melan.spacereloaded.worldgen;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import org.alex_melan.spacereloaded.registry.ModBlocks;

/**
 * Ударный кратер как элемент рельефа безатмосферного тела: параболическая чаша,
 * вал выброса по краю и дно, засыпанное реголитом.
 *
 * <p>Радиус ограничен {@value #MAX_RADIUS}: фича пишет блоки за границу своего
 * чанка, а генератор разрешает запись только в соседние (радиус 1 чанк).
 * Кратер шире примерно 14 блоков от точки размещения уронил бы генерацию
 * исключением «setBlock in a far chunk».
 */
public class CraterFeature extends Feature<NoneFeatureConfiguration> {

    /** Предел безопасной записи за пределы чанка размещения. */
    private static final int MAX_RADIUS = 12;
    /** Ширина кольца вала за кромкой чаши. */
    private static final int RIM_WIDTH = 2;

    public CraterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos origin = context.origin();

        int radius = 4 + random.nextInt(MAX_RADIUS - 3);
        // Чаша заметно мельче своей ширины: реальные кратеры — блюдца, не колодцы
        int depth = Math.max(2, Math.round(radius * 0.45f));
        int rimHeight = Math.max(1, radius / 6);

        BlockState regolith = ModBlocks.MOON_REGOLITH.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        int outer = radius + RIM_WIDTH;
        boolean placed = false;
        for (int dx = -outer; dx <= outer; dx++) {
            for (int dz = -outer; dz <= outer; dz++) {
                double distance = Math.sqrt(dx * dx + dz * dz);
                if (distance > outer) {
                    continue;
                }
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                int surface = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z);

                if (distance <= radius) {
                    // Параболическая чаша: глубже к центру, сходит на нет к кромке
                    double normalized = distance / radius;
                    int bowl = (int) Math.round(depth * (1.0 - normalized * normalized));
                    if (bowl <= 0) {
                        continue;
                    }
                    for (int y = surface; y > surface - bowl; y--) {
                        cursor.set(x, y, z);
                        if (isTerrain(level, cursor)) {
                            level.setBlock(cursor, air, 2);
                            placed = true;
                        }
                    }
                    cursor.set(x, surface - bowl, z);
                    if (isTerrain(level, cursor)) {
                        level.setBlock(cursor, regolith, 2); // дно засыпано выбросом
                    }
                } else {
                    // Вал: выброшенная порода, тонким кольцом и тем ниже, чем дальше
                    double falloff = 1.0 - (distance - radius) / RIM_WIDTH;
                    int rise = (int) Math.round(rimHeight * falloff);
                    for (int y = surface + 1; y <= surface + rise; y++) {
                        cursor.set(x, y, z);
                        if (level.getBlockState(cursor).isAir()) {
                            level.setBlock(cursor, regolith, 2);
                            placed = true;
                        }
                    }
                }
            }
        }
        return placed;
    }

    /** Копаем только породу тела: лёд, постройки и воздух не трогаем. */
    private static boolean isTerrain(WorldGenLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(ModBlocks.MOON_STONE) || state.is(ModBlocks.MOON_REGOLITH);
    }
}
