package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.registry.ModEntities;

/**
 * Визуальная грузовая капсула (004, FR-210/FR-225): только картинка вылета из среза и
 * прибытия в ловушку. Груза не несёт и не сохраняется — настоящая капсула живёт записью
 * {@link PodTransitState}, поэтому выгрузка/исчезновение визуала ничего не теряет.
 * Скорость визуала условная (реальные 2.5 км/с = 125 блоков за тик — невидимо).
 */
public class CargoPodEntity extends Entity {

    private static final int LIFE_TICKS = 60;
    private int life;

    public CargoPodEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true;
    }

    /** Вылет со среза вдоль оси рельса с подъёмом ~10°. */
    public static void launch(ServerLevel level, Vec3 muzzle, Direction direction) {
        CargoPodEntity pod = new CargoPodEntity(ModEntities.CARGO_POD, level);
        pod.setPos(muzzle);
        Vec3 dir = Vec3.atLowerCornerOf(direction.getUnitVec3i()).add(0, 0.18, 0).normalize();
        pod.setDeltaMovement(dir.scale(4.0));
        level.addFreshEntity(pod);
        level.sendParticles(ParticleTypes.END_ROD, muzzle.x, muzzle.y, muzzle.z, 20, 0.2, 0.2, 0.2, 0.3);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, muzzle.x, muzzle.y, muzzle.z, 40, 0.4, 0.4, 0.4, 0.6);
    }

    /** Прибытие сверху в ловушку (визуал приёма). */
    public static void arrive(ServerLevel level, Vec3 catcher) {
        CargoPodEntity pod = new CargoPodEntity(ModEntities.CARGO_POD, level);
        pod.setPos(catcher.add(0, 30, 0));
        pod.setDeltaMovement(new Vec3(0, -3.0, 0));
        pod.life = LIFE_TICKS - 10;
        level.addFreshEntity(pod);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
    }

    @Override
    public void tick() {
        super.tick();
        Vec3 motion = getDeltaMovement();
        setPos(position().add(motion));
        if (level().isClientSide()) {
            return;
        }
        if (++life >= LIFE_TICKS) {
            discard();
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
    }
}
