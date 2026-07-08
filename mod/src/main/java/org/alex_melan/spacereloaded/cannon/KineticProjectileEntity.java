package org.alex_melan.spacereloaded.cannon;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.ballistics.BallisticIntegrator;
import org.alex_melan.spacereloaded.core.ballistics.ImpactEnergy;
import org.alex_melan.spacereloaded.core.ballistics.ProjectileSpec;
import org.alex_melan.spacereloaded.planet.ModTickets;
import org.alex_melan.spacereloaded.planet.PlanetManager;

/**
 * Кинетический снаряд орбитальной пушки (US7, FR-042/FR-043): вольфрамовый лом,
 * падающий по честной баллистике ядра ({@link BallisticIntegrator}, гравитация
 * измерения + сопротивление). Разрушения — не «сила взрыва», а E = ½mv²
 * с кубическим подобием радиуса кратера ({@link ImpactEnergy}).
 *
 * <p>Траектория проверяется клипом от старой позиции к новой — на ~5 блоках
 * за тик снаряд не туннелирует сквозь перекрытия. Чанки по пути держатся
 * авто-протухающими ticket'ами (принцип V — утечка невозможна).
 */
public class KineticProjectileEntity extends Entity {

    private static final double DT = 0.05;

    private double massKg = 2000;
    private double dragCoeff = 0.01;
    private org.alex_melan.spacereloaded.core.geometry.Vec3d velocity =
            org.alex_melan.spacereloaded.core.geometry.Vec3d.ZERO;
    /** Колонна предупреждения над точкой прицеливания (FR-044). */
    private BlockPos warningPos = BlockPos.ZERO;

    public KineticProjectileEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // коллизии считаем сами клипом
    }

    /** Сервер: параметры выстрела (до addFreshEntity). */
    public void configure(double massKg, double dragCoeff, Vec3 initialVelocity, BlockPos warningPos) {
        this.massKg = massKg;
        this.dragCoeff = dragCoeff;
        this.velocity = new org.alex_melan.spacereloaded.core.geometry.Vec3d(
                initialVelocity.x, initialVelocity.y, initialVelocity.z);
        this.warningPos = warningPos.immutable();
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            return;
        }
        ServerLevel level = (ServerLevel) level();

        Vec3 from = position();
        BallisticIntegrator.State state = BallisticIntegrator.step(
                new BallisticIntegrator.State(
                        new org.alex_melan.spacereloaded.core.geometry.Vec3d(getX(), getY(), getZ()),
                        velocity),
                new ProjectileSpec(massKg, dragCoeff),
                PlanetManager.gravity(level), DT);
        velocity = state.vel();
        Vec3 to = new Vec3(state.pos().x(), state.pos().y(), state.pos().z());

        // Клип траектории: на гиперзвуке нельзя проверять только конечную точку
        BlockHitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            impact(level, hit.getLocation());
            return;
        }

        setPos(to);
        setDeltaMovement(to.subtract(from)); // интерполяция на клиенте
        if (to.y < level.getMinY() - 64) {
            discard();
            return;
        }

        // Предохранитель: вырожденная гравитация/скорость (датапак с g=0) не
        // должна оставить вечный самоподдерживающийся чанклоадер
        if (tickCount > 2400) {
            discard();
            return;
        }
        if (tickCount % 20 == 1) {
            ModTickets.holdStrike(level, blockPosition(), 1);
            ModTickets.holdStrike(level, warningPos, 1);
        }
        // След снаряда + колонна предупреждения над целью (FR-044)
        level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 4, 0.15, 0.6, 0.15, 0.01);
        if (tickCount % 10 == 0) {
            for (int i = 0; i < 14; i += 2) {
                level.sendParticles(ParticleTypes.LAVA,
                        warningPos.getX() + 0.5, warningPos.getY() + 1.0 + i, warningPos.getZ() + 0.5,
                        2, 0.3, 0.5, 0.3, 0.0);
            }
        }
    }

    /**
     * Удар: кратер радиусом из кинетической энергии (сфера, неразрушаемые блоки
     * вроде бедрока не трогаем) + взрыв без блочного урона — звук, урон и
     * отброс сущностей. Инвалидация герметичных зон идёт через миксин
     * sendBlockUpdated автоматически.
     */
    private void impact(ServerLevel level, Vec3 at) {
        double speed = Math.sqrt(velocity.x() * velocity.x()
                + velocity.y() * velocity.y() + velocity.z() * velocity.z());
        double energyJ = ImpactEnergy.kineticEnergyJ(massKg, speed);
        var config = SpaceReloaded.config();
        int radius = (int) Math.min(config.cannonMaxCraterRadius,
                Math.round(ImpactEnergy.craterRadiusBlocks(energyJ) * config.cannonCraterMultiplier));

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
                            || blockState.getBlock().getExplosionResistance()
                                    >= SpaceReloaded.config().cannonMaxBlockResistance) {
                        continue; // бедрок, жидкости и обсидиан-класс выживают
                    }
                    level.setBlock(cursor, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        // Взрыв NONE: блоки уже вынес кратер, взрыв даёт урон/отброс/звук/частицы
        level.explode(this, at.x, at.y, at.z, Math.min(10.0f, radius),
                Level.ExplosionInteraction.NONE);
        level.playSound(null, center, SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS, 8.0f, 0.5f);
        SpaceReloaded.LOGGER.info("Кинетический удар: {} кг на {} м/с = {} МДж, кратер r={}",
                Math.round(massKg), Math.round(speed), Math.round(energyJ / 1.0e6), radius);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false; // вольфрамовый лом не сбить
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("mass_kg", massKg);
        output.putDouble("drag", dragCoeff);
        output.putDouble("vel_x", velocity.x());
        output.putDouble("vel_y", velocity.y());
        output.putDouble("vel_z", velocity.z());
        output.putLong("warning_pos", warningPos.asLong());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        massKg = input.getDoubleOr("mass_kg", 2000);
        dragCoeff = input.getDoubleOr("drag", 0.01);
        velocity = new org.alex_melan.spacereloaded.core.geometry.Vec3d(
                input.getDoubleOr("vel_x", 0),
                input.getDoubleOr("vel_y", -80),
                input.getDoubleOr("vel_z", 0));
        warningPos = BlockPos.of(input.getLongOr("warning_pos", 0L));
    }
}
