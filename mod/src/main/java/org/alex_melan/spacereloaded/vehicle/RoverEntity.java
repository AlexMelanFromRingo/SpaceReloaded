package org.alex_melan.spacereloaded.vehicle;

import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.InterpolationHandler;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.core.vehicle.Terramechanics;
import org.alex_melan.spacereloaded.lifesupport.EnergyScale;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.registry.ItemMasses;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.Locale;

/**
 * Электрический ровер (007, FR-521…FR-523, D78) по образцу LRV: собирается на месте из деталей —
 * шасси, четыре мотор-колеса (4 × 190 Вт), никель-железная батарея (8.7 кВт·ч, как у LRV; Ni–Fe
 * вместо Ag–Zn — серебра в моде нет, батарея в 6 раз тяжелее — честная цена), 1–2 места.
 * <p>
 * Серверная физика (как ракета 002): масса — детали по таблице масс + экипаж в скафандрах (180 кг)
 * + груз; вес на колесо при g тела; сопротивление и сцепление — механика Беккера грунта тела;
 * тяга ограничена мощностью (F = η·P/v) и сцеплением; поворот — боковое ускорение не больше
 * μ·g (μ = tg φ), иначе занос; ступень в блок — средний уклон атан(1/2.3 м колёсной базы) не
 * больше предельного склона при 60 % пробуксовки. Расход — F·v/η + 50 Вт электроники.
 */
public class RoverEntity extends Entity {

    public static final double MOTOR_W = 4 * 190;
    public static final double EFFICIENCY = 0.7;
    public static final double BATTERY_KWH = 8.7;
    public static final double WHEELBASE = 2.29;
    public static final double CREW_KG = 180;
    public static final double IDLE_W = 50;
    public static final Terramechanics.Wheel WHEEL = Terramechanics.Wheel.LRV;

    private static final EntityDataAccessor<Integer> WHEELS = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> BATTERY = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> WHEEL_ANGLE = SynchedEntityData.defineId(RoverEntity.class, EntityDataSerializers.FLOAT);

    private final InterpolationHandler interpolation = new InterpolationHandler(this);
    private double speed;
    private double charge;
    private double odometer;
    private boolean stepAllowed = true;
    /** Только для стенда: «газ» без пилота. */
    private boolean testForward;

    public void testSetup(int wheels, double chargeE, boolean forward) {
        entityData.set(WHEELS, wheels);
        entityData.set(BATTERY, true);
        charge = chargeE;
        testForward = forward;
    }

    public void testForward(boolean forward) {
        testForward = forward;
    }

    public RoverEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(WHEELS, 0);
        builder.define(BATTERY, false);
        builder.define(WHEEL_ANGLE, 0f);
    }

    public int wheels() {
        return entityData.get(WHEELS);
    }

    public boolean hasBattery() {
        return entityData.get(BATTERY);
    }

    public float wheelAngle() {
        return entityData.get(WHEEL_ANGLE);
    }

    public double speed() {
        return speed;
    }

    public double charge() {
        return charge;
    }

    public double odometer() {
        return odometer;
    }

    public static double capacityE() {
        return BATTERY_KWH * EnergyScale.E_PER_KWH;
    }

    public void charge(double e) {
        charge = Math.min(capacityE(), charge + e);
    }

    @Override
    public InterpolationHandler getInterpolation() {
        return interpolation;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public float maxUpStep() {
        return stepAllowed ? 1.0f : 0.0f;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        return getPassengers().size() < 2;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction move) {
        int index = getPassengers().indexOf(passenger);
        double side = index == 0 ? -0.45 : 0.45;
        Vec3 offset = new Vec3(side, 0.55, -0.2).yRot((float) Math.toRadians(-getYRot()));
        move.accept(passenger, getX() + offset.x, getY() + offset.y, getZ() + offset.z);
    }

    /** Масса ровера, кг: детали, экипаж, груз. */
    public double mass() {
        var access = level().registryAccess();
        double m = ItemMasses.massOf(access, new ItemStack(ModItems.ROVER_CHASSIS))
                + wheels() * ItemMasses.massOf(access, new ItemStack(ModItems.ROVER_WHEEL));
        if (hasBattery()) {
            m += ItemMasses.massOf(access, new ItemStack(ModItems.NIFE_BATTERY));
        }
        return m + getPassengers().size() * CREW_KG;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        ItemStack stack = player.getItemInHand(hand);
        if (level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.ROVER_WHEEL) && wheels() < 4) {
            entityData.set(WHEELS, wheels() + 1);
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (stack.is(ModItems.NIFE_BATTERY) && !hasBattery()) {
            entityData.set(BATTERY, true);
            charge = stack.getOrDefault(ModDataComponents.ROVER_CHARGE, 0f);
            stack.consume(1, player);
            return InteractionResult.SUCCESS_SERVER;
        }
        if (player.isSecondaryUseActive() && player instanceof ServerPlayer sp) {
            sp.sendSystemMessage(Component.translatable("message.spacereloaded.rover.report", wheels(),
                    hasBattery() ? String.format(Locale.ROOT, "%.1f", 100 * charge / capacityE()) : "—",
                    String.format(Locale.ROOT, "%.0f", mass()),
                    String.format(Locale.ROOT, "%.1f", speed * 3.6),
                    String.format(Locale.ROOT, "%.2f", odometer / 1000)));
            return InteractionResult.SUCCESS_SERVER;
        }
        if (wheels() == 4 && hasBattery() && player.startRiding(this)) {
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.PASS;
    }

    /** Удар с Sneak — разборка на детали (заряд батареи сохраняется). */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isSecondaryUseActive()) {
            ejectPassengers();
            spawnAtLocation(level, new ItemStack(ModItems.ROVER_CHASSIS));
            if (wheels() > 0) {
                spawnAtLocation(level, new ItemStack(ModItems.ROVER_WHEEL, wheels()));
            }
            if (hasBattery()) {
                ItemStack battery = new ItemStack(ModItems.NIFE_BATTERY);
                battery.set(ModDataComponents.ROVER_CHARGE, (float) charge);
                spawnAtLocation(level, battery);
            }
            discard();
            return true;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate();
            return;
        }
        ServerLevel level = (ServerLevel) level();
        double dt = 0.05;
        double g = PlanetManager.gravity(level);
        Vec3 motion = getDeltaMovement();
        // гравитация каждый тик (как у ванильных сущностей): на земле столкновение гасит её, onGround не мигает
        double vy = (onGround() ? 0 : motion.y) - g * dt * dt;
        double m = mass();
        double wheelLoad = m * g / 4;
        Terramechanics.Soil soil = Soils.under(level, blockPosition().below());
        double resistance = soil == null ? 0.015 * m * g : 4 * Terramechanics.compactionN(soil, WHEEL, wheelLoad);
        double traction = soil == null ? 0.7 * m * g : 4 * Terramechanics.tractionN(soil, WHEEL, wheelLoad, 0.5);
        double mu = soil == null ? 0.7 : Math.tan(Math.toRadians(soil.phiDeg()));
        double maxSlope = soil == null ? 35 : Terramechanics.maxSlopeDeg(soil, WHEEL, wheelLoad, 0.6);
        stepAllowed = maxSlope >= Math.toDegrees(Math.atan(1 / WHEELBASE));

        Input input = getFirstPassenger() instanceof ServerPlayer pilot ? pilot.getLastClientInput()
                : testForward ? new Input(true, false, false, false, false, false, false) : Input.EMPTY;
        boolean powered = wheels() == 4 && hasBattery() && charge > 0 && onGround();
        double drive = 0;
        if (powered && (input.forward() || input.backward())) {
            double f = Math.min(EFFICIENCY * MOTOR_W / Math.max(0.3, Math.abs(speed)), traction);
            drive = input.forward() ? f : -f;
        }
        double net = drive - Math.signum(speed) * resistance;
        double accel = net / m;
        double next = speed + accel * dt;
        if (drive == 0 && Math.signum(next) != Math.signum(speed)) {
            next = 0; // остановился: сопротивление не толкает назад
        }
        speed = onGround() ? next : speed;
        // поворот: руль задаёт желаемую угловую скорость, боковое ускорение ≤ μ·g
        double turn = (input.left() ? 1 : 0) - (input.right() ? 1 : 0);
        if (turn != 0 && Math.abs(speed) > 0.05) {
            double wanted = turn * Math.min(Math.abs(speed) / 3.0, 1.2); // радиус ≥ 3 м (LRV)
            double limit = mu * g / Math.abs(speed);
            double yawRate = Math.signum(wanted) * Math.min(Math.abs(wanted), limit);
            setYRot(getYRot() - (float) Math.toDegrees(yawRate * dt) * (float) Math.signum(speed));
        }
        // энергия: механическая мощность / КПД + электроника
        if (hasBattery()) {
            double watts = Math.abs(drive * speed) / EFFICIENCY + (getPassengers().isEmpty() ? 0 : IDLE_W);
            charge = Math.max(0, charge - EnergyScale.fromJoules(watts * dt));
        }
        double yaw = Math.toRadians(getYRot());
        double vx = -Math.sin(yaw) * speed * dt;
        double vz = Math.cos(yaw) * speed * dt;
        setDeltaMovement(vx, vy, vz);
        Vec3 before = position();
        move(MoverType.SELF, getDeltaMovement());
        double moved = position().subtract(before).horizontalDistance();
        if (moved < Math.abs(speed * dt) * 0.5 && Math.abs(speed) > 0.1) {
            speed *= 0.5; // упёрся: ступень круче предельного склона или стена
        }
        odometer += moved;
        entityData.set(WHEEL_ANGLE, (float) ((wheelAngle() + Math.signum(speed) * moved / (WHEEL.diameterCm() / 200.0)) % (2 * Math.PI)));
        if (odometer >= 1000 && getFirstPassenger() instanceof ServerPlayer pilot
                && !level.dimension().equals(Level.OVERWORLD)) {
            org.alex_melan.spacereloaded.industry.IndustryAdvancements.award(pilot,
                    org.alex_melan.spacereloaded.industry.IndustryAdvancements.FIRST_TRACK);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.putInt("wheels", wheels());
        output.putBoolean("battery", hasBattery());
        output.putDouble("charge", charge);
        output.putDouble("odometer", odometer);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        entityData.set(WHEELS, input.getIntOr("wheels", 0));
        entityData.set(BATTERY, input.getBooleanOr("battery", false));
        charge = input.getDoubleOr("charge", 0);
        odometer = input.getDoubleOr("odometer", 0);
    }
}
