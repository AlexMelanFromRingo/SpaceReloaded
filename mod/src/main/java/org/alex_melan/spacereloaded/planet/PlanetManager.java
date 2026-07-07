package org.alex_melan.spacereloaded.planet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModRegistries;

import java.util.Optional;

/**
 * Доступ к профилям небесных тел (FR-030): гравитация, дыхание, солнце,
 * переходы. Профили — датапак-реестр; без записи применяются земные дефолты.
 */
public final class PlanetManager {

    public static final double EARTH_GRAVITY = 9.81;
    /** Высота платформы прибытия на орбиту. */
    public static final int ORBIT_PLATFORM_Y = 100;

    private PlanetManager() {
    }

    private static Registry<ModRegistries.PlanetProfile> registry(ServerLevel level) {
        return level.registryAccess().lookupOrThrow(ModRegistries.PLANETS);
    }

    /** Профиль измерения либо empty (обычный мир — земные условия). */
    public static Optional<ModRegistries.PlanetProfile> profileFor(ServerLevel level) {
        Identifier dimensionId = level.dimension().identifier();
        for (ModRegistries.PlanetProfile profile : registry(level)) {
            if (profile.dimension().equals(dimensionId)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }

    /** Профиль по id записи реестра (для transition_target). */
    public static Optional<ModRegistries.PlanetProfile> profileById(ServerLevel level, Identifier id) {
        Registry<ModRegistries.PlanetProfile> registry = registry(level);
        return Optional.ofNullable(registry.getValue(id));
    }

    public static double gravity(ServerLevel level) {
        return profileFor(level).map(ModRegistries.PlanetProfile::gravity).orElse(EARTH_GRAVITY);
    }

    public static boolean isBreathable(ServerLevel level) {
        return profileFor(level).map(ModRegistries.PlanetProfile::breathable).orElse(true);
    }

    public static double solarEfficiency(ServerLevel level) {
        return profileFor(level).map(ModRegistries.PlanetProfile::solarEfficiency).orElse(1.0);
    }

    /**
     * Стартовая платформа на орбите (механика Galacticraft): первое прибытие
     * на участок создаёт площадку 9×9, дальше игрок достраивает станцию сам.
     *
     * @return Y верха платформы (куда ставить ракету)
     */
    public static int ensureOrbitPlatform(ServerLevel level, double x, double z) {
        BlockPos center = BlockPos.containing(x, ORBIT_PLATFORM_Y - 1, z);
        BlockState existing = level.getBlockState(center);
        if (existing.isAir()) {
            for (int dx = -4; dx <= 4; dx++) {
                for (int dz = -4; dz <= 4; dz++) {
                    BlockPos pos = center.offset(dx, 0, dz);
                    if (level.getBlockState(pos).isAir()) {
                        level.setBlock(pos, ModBlocks.HULL_PLATING.defaultBlockState(), 3);
                    }
                }
            }
        }
        return ORBIT_PLATFORM_Y;
    }
}
