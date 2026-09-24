package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.kinetics.Flywheel;
import org.alex_melan.spacereloaded.core.kinetics.NodeLoad;
import org.alex_melan.spacereloaded.industry.IndustryAdvancements;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

import java.util.Locale;

/**
 * Маховик (005, FR-310): инерция сплошного стального диска; запас ½Iω². Сверх предельной
 * скорости (окружное напряжение > допускаемого) — разрыв: взрыв с мощностью по запасённой
 * энергии (TNT-эквивалент 4.184 МДж, радиус ∝ ∛E, потолок и разрушение блоков — конфиг).
 */
public class FlywheelBlockEntity extends KineticBlockEntity {

    /** Энергия одного «TNT» для перевода в мощность взрыва, Дж. */
    private static final double TNT_JOULES = 4.184e6;

    public FlywheelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLYWHEEL, pos, state);
    }

    @Override
    public NodeLoad load(ServerLevel level) {
        return new NodeLoad(0, 0, SpaceReloaded.config().kineticFrictionTorqueNm,
                SpaceReloaded.config().flywheelInertia);
    }

    /** Запасённая энергия, Дж. */
    public double storedJoules() {
        return Flywheel.energy(SpaceReloaded.config().flywheelInertia, omega);
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        if (level.getGameTime() % 40 == 0
                && storedJoules() / SpaceReloaded.config().massDriverJoulesPerEnergy >= 1000) {
            IndustryAdvancements.awardNearby(level, getBlockPos(), 16, IndustryAdvancements.FLYWHEEL);
        }
    }

    /** Разрыв маховика. */
    public void burst(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        double energyJ = storedJoules();
        float power = (float) Math.min(config.flywheelBurstMaxRadius, 4 * Math.cbrt(energyJ / TNT_JOULES));
        BlockPos pos = getBlockPos();
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, power,
                config.flywheelBurstBreaksBlocks ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        SpaceReloaded.LOGGER.info("Маховик {} разорван: {} МДж, мощность взрыва {}", pos,
                String.format(Locale.ROOT, "%.1f", energyJ / 1e6), power);
    }

    @Override
    protected void extraReport(ServerPlayer player) {
        player.sendSystemMessage(Component.translatable("message.spacereloaded.kinetic.flywheel",
                String.format(Locale.ROOT, "%.0f", storedJoules() / SpaceReloaded.config().massDriverJoulesPerEnergy),
                String.format(Locale.ROOT, "%.0f",
                        KineticBlockEntity.toRpm(SpaceReloaded.config().flywheelMaxOmega))));
    }
}
