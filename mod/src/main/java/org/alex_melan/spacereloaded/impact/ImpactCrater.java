package org.alex_melan.spacereloaded.impact;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.core.ballistics.ImpactEnergy;

/**
 * Кинетический кратер (общий для орбитальной пушки и метеоритов): единственное
 * место с правилами разрушения. Радиус — из E=½mv² с кубическим подобием ядра;
 * бедрок (destroySpeed&lt;0), жидкости и блоки взрывостойкости ≥ порога выживают
 * (обсидиановый бункер и вода — честная защита от удара, в т.ч. заглубление).
 * Взрыв NONE поверх кратера даёт урон/отброс/звук/частицы, не трогая блоки.
 */
public final class ImpactCrater {

    private ImpactCrater() {
    }

    /**
     * @param maxResistance порог взрывостойкости: блоки ≥ него выживают (задаёт вызывающий)
     * @return фактический радиус кратера в блоках (0, если энергии не хватило)
     */
    public static int carve(ServerLevel level, Entity source, Vec3 at,
                            double massKg, double speedMs,
                            double craterMultiplier, int maxRadius, double maxResistance) {
        double energyJ = ImpactEnergy.kineticEnergyJ(massKg, speedMs);
        int radius = (int) Math.min(maxRadius,
                Math.round(ImpactEnergy.craterRadiusBlocks(energyJ) * craterMultiplier));

        BlockPos center = BlockPos.containing(at);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        int radiusSq = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radiusSq) {
                        continue;
                    }
                    cursor.setWithOffset(center, dx, dy, dz);
                    BlockState blockState = level.getBlockState(cursor);
                    if (blockState.isAir()
                            || blockState.getDestroySpeed(level, cursor) < 0
                            || !blockState.getFluidState().isEmpty()
                            || blockState.getBlock().getExplosionResistance() >= maxResistance) {
                        continue; // бедрок, жидкости, обсидиан-класс — держат удар
                    }
                    level.setBlock(cursor, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        level.explode(source, at.x, at.y, at.z, Math.min(10.0f, radius),
                Level.ExplosionInteraction.NONE);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 8.0f, 0.5f);
        return radius;
    }
}
