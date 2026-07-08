package org.alex_melan.spacereloaded.impact;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.ballistics.BallisticIntegrator;
import org.alex_melan.spacereloaded.core.ballistics.ProjectileSpec;
import org.alex_melan.spacereloaded.core.geometry.Vec3d;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Метеорит (backlog AR/GC → adapt): падает на безатмосферные тела по честной
 * баллистике ядра, оставляет кратер из кинетической энергии ({@link ImpactCrater})
 * и залежь метеоритного железа. Защита — заглубление: сферический кратер не
 * достанет базу под достаточным слоем реголита/обшивки (обсидиан-класс держит).
 * Клип траектории — против туннелирования сквозь перекрытия на скорости.
 */
public class MeteorEntity extends Entity {

    private static final double DT = 0.05;

    private double massKg = 800;
    private Vec3d velocity = Vec3d.ZERO;

    public MeteorEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Сервер: параметры до addFreshEntity. */
    public void configure(double massKg, Vec3 initialVelocity) {
        this.massKg = massKg;
        this.velocity = new Vec3d(initialVelocity.x, initialVelocity.y, initialVelocity.z);
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
                new BallisticIntegrator.State(new Vec3d(getX(), getY(), getZ()), velocity),
                new ProjectileSpec(massKg, SpaceReloaded.config().meteorDragCoeff),
                PlanetManager.gravity(level), DT);
        velocity = state.vel();
        Vec3 to = new Vec3(state.pos().x(), state.pos().y(), state.pos().z());

        BlockHitResult hit = level.clip(new ClipContext(from, to,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, this));
        if (hit.getType() == HitResult.Type.BLOCK) {
            impact(level, hit.getLocation());
            return;
        }
        setPos(to);
        setDeltaMovement(to.subtract(from));
        if (to.y < level.getMinY() - 16 || tickCount > 1200) {
            discard();
            return;
        }
        // Огненный след входа
        level.sendParticles(ParticleTypes.FLAME, getX(), getY(), getZ(), 8, 0.2, 0.6, 0.2, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 3, 0.3, 0.5, 0.3, 0.01);
    }

    private void impact(ServerLevel level, Vec3 at) {
        double speed = Math.sqrt(velocity.x() * velocity.x()
                + velocity.y() * velocity.y() + velocity.z() * velocity.z());
        var config = SpaceReloaded.config();
        int radius = ImpactCrater.carve(level, this, at, massKg, speed,
                config.meteorCraterMultiplier, config.meteorMaxCraterRadius,
                config.meteorMaxBlockResistance);
        // Залежь метеоритного железа в воронке
        int drops = config.meteorIronMin
                + level.getRandom().nextInt(Math.max(1, config.meteorIronMax - config.meteorIronMin + 1));
        for (int i = 0; i < drops; i++) {
            Block.popResource(level, net.minecraft.core.BlockPos.containing(at),
                    new ItemStack(ModItems.METEORIC_IRON));
        }
        SpaceReloaded.LOGGER.info("Метеорит: {} кг на {} м/с, кратер r={}, железа {}",
                Math.round(massKg), Math.round(speed), radius, drops);
        discard();
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source,
                              float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putDouble("mass_kg", massKg);
        output.putDouble("vel_x", velocity.x());
        output.putDouble("vel_y", velocity.y());
        output.putDouble("vel_z", velocity.z());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        massKg = input.getDoubleOr("mass_kg", 800);
        velocity = new Vec3d(input.getDoubleOr("vel_x", 0),
                input.getDoubleOr("vel_y", -40), input.getDoubleOr("vel_z", 0));
    }
}
