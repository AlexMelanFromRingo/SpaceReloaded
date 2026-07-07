package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.core.geometry.PackedPos;
import org.alex_melan.spacereloaded.core.rocketry.ControlInput;
import org.alex_melan.spacereloaded.core.rocketry.FlightEnvironment;
import org.alex_melan.spacereloaded.core.rocketry.FlightIntegrator;
import org.alex_melan.spacereloaded.core.rocketry.FlightState;
import org.alex_melan.spacereloaded.core.rocketry.PerformanceCalculator;
import org.alex_melan.spacereloaded.core.rocketry.RocketPerformance;
import org.alex_melan.spacereloaded.core.rocketry.RocketStructure;
import org.alex_melan.spacereloaded.registry.ModEntities;

import java.util.List;

/**
 * Ракета-сущность (T052/T054): хранит структуру из блоков, летает серверной
 * физикой ядра ({@link FlightIntegrator} — Мещерский, моменты, гиродины).
 * Управление: ванильный ввод пассажира (jump = тяга). Структура синхронизируется
 * на клиент через synched CompoundTag (приходит вместе со спавном).
 */
public class RocketEntity extends Entity {

    private static final EntityDataAccessor<CompoundTag> DATA_STRUCTURE =
            SynchedEntityData.defineId(RocketEntity.class, ModEntities.COMPOUND_TAG_SERIALIZER);
    private static final EntityDataAccessor<Float> DATA_PITCH =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_ROLL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_LAUNCHED =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double DT = 0.05; // серверный тик
    private static final double CRASH_SPEED = 15.0; // м/с — жёсткая посадка

    private RocketData rocketData;
    private RocketStructure structure;
    private FlightState flight;
    private boolean launched;

    // Производные размеры (сервер и клиент)
    private float sizeX = 1;
    private float sizeY = 2;
    private float sizeZ = 1;
    private float comY = 1;
    private long commandLocal;

    public RocketEntity(EntityType<?> type, Level level) {
        super(type, level);
        this.noPhysics = true; // физика своя; ванильные коллизии не мешают
    }

    // ---------- Данные ----------

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_STRUCTURE, new CompoundTag());
        builder.define(DATA_PITCH, 0.0f);
        builder.define(DATA_ROLL, 0.0f);
        builder.define(DATA_LAUNCHED, false);
    }

    /** Сервер: установить структуру после сборки (до addFreshEntity). */
    public void setAssembly(RocketData data) {
        this.rocketData = data;
        rebuildDerived();
        this.flight = FlightState.atRest(corePos(), data.propellantKg());
        Tag tag = RocketData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        entityData.set(DATA_STRUCTURE, (CompoundTag) tag);
    }

    private void rebuildDerived() {
        structure = rocketData.toStructure();
        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;
        for (RocketData.Entry entry : rocketData.blocks()) {
            maxX = Math.max(maxX, PackedPos.unpackX(entry.localPos()));
            maxY = Math.max(maxY, PackedPos.unpackY(entry.localPos()));
            maxZ = Math.max(maxZ, PackedPos.unpackZ(entry.localPos()));
        }
        sizeX = maxX + 1;
        sizeY = maxY + 1;
        sizeZ = maxZ + 1;
        commandLocal = rocketData.commandLocalPos();
        comY = (float) PerformanceCalculator.calculate(structure, 9.81).centerOfMass().y();
        setBoundingBox(makeBoundingBox(position()));
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> accessor) {
        super.onSyncedDataUpdated(accessor);
        if (DATA_STRUCTURE.equals(accessor) && level().isClientSide()) {
            CompoundTag tag = entityData.get(DATA_STRUCTURE);
            if (!tag.isEmpty()) {
                RocketData.CODEC.parse(NbtOps.INSTANCE, tag).result().ifPresent(data -> {
                    this.rocketData = data;
                    rebuildDerived();
                });
            }
        }
    }

    /** Клиентский рендер: блоки структуры. */
    public List<RocketData.Entry> clientBlocks() {
        return rocketData == null ? List.of() : rocketData.blocks();
    }

    public float pitchDeg() {
        return entityData.get(DATA_PITCH);
    }

    public float rollDeg() {
        return entityData.get(DATA_ROLL);
    }

    public float halfX() {
        return sizeX / 2f;
    }

    public float halfZ() {
        return sizeZ / 2f;
    }

    public float comY() {
        return comY;
    }

    // ---------- Геометрия ----------

    @Override
    protected AABB makeBoundingBox(Vec3 pos) {
        if (rocketData == null) {
            return super.makeBoundingBox(pos);
        }
        return new AABB(pos.x - sizeX / 2.0, pos.y, pos.z - sizeZ / 2.0,
                pos.x + sizeX / 2.0, pos.y + sizeY, pos.z + sizeZ / 2.0);
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public boolean canBeCollidedWith(Entity entity) {
        return true; // по ракете можно ходить, как по лодке
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    // ---------- Посадка пилота ----------

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        if (player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide() && !isVehicle()) {
            player.startRiding(this);
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        double x = getX() + PackedPos.unpackX(commandLocal) - halfX() + 0.5;
        double y = getY() + PackedPos.unpackY(commandLocal) + 0.1;
        double z = getZ() + PackedPos.unpackZ(commandLocal) - halfZ() + 0.5;
        moveFunction.accept(passenger, x, y, z);
    }

    // ---------- Полёт ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide() || rocketData == null) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level();

        boolean jump = false;
        if (getFirstPassenger() instanceof ServerPlayer pilot) {
            Input input = pilot.getLastClientInput();
            jump = input.jump();
        }

        if (!launched) {
            if (jump) {
                tryIgnite(serverLevel);
            }
            return;
        }

        ControlInput control = new ControlInput(jump ? 1.0 : 0.0, 0, 0, true);
        flight = new FlightState(corePos(), flight.vel(), flight.pitch(), flight.roll(),
                flight.pitchRate(), flight.rollRate(), flight.propellantKg());
        flight = FlightIntegrator.step(structure, flight, control, FlightEnvironment.EARTH, DT);

        setPos(flight.pos().x(), flight.pos().y(), flight.pos().z());
        setDeltaMovement(flight.vel().x() * DT, flight.vel().y() * DT, flight.vel().z() * DT);
        entityData.set(DATA_PITCH, (float) Math.toDegrees(flight.pitch()));
        entityData.set(DATA_ROLL, (float) Math.toDegrees(flight.roll()));

        if (flight.vel().y() <= 0 && touchesGround(serverLevel)) {
            land(serverLevel);
        }
    }

    private void tryIgnite(ServerLevel level) {
        RocketPerformance performance = PerformanceCalculator.calculate(structure, 9.81);
        if (performance.twr() <= 1.0 || performance.deltaV() <= 0) {
            if (getFirstPassenger() instanceof ServerPlayer pilot) {
                pilot.sendOverlayMessage(Component.translatable(
                        performance.twr() <= 1.0
                                ? "message.spacereloaded.rocket.warning.TWR_BELOW_ONE"
                                : "message.spacereloaded.rocket.warning.NO_USABLE_PROPELLANT"));
            }
            return;
        }
        launched = true;
        entityData.set(DATA_LAUNCHED, true);
        flight = FlightState.atRest(corePos(), flight.propellantKg());
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 3.0f, 0.5f);
    }

    private org.alex_melan.spacereloaded.core.geometry.Vec3d corePos() {
        return new org.alex_melan.spacereloaded.core.geometry.Vec3d(getX(), getY(), getZ());
    }

    /** Контакт с землёй: под любым блоком нижнего яруса — коллизия. */
    private boolean touchesGround(ServerLevel level) {
        double baseY = getY() - 0.06;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (PackedPos.unpackY(entry.localPos()) != 0) {
                continue;
            }
            cursor.set(
                    (int) Math.floor(getX() - halfX() + PackedPos.unpackX(entry.localPos()) + 0.5),
                    (int) Math.floor(baseY),
                    (int) Math.floor(getZ() - halfZ() + PackedPos.unpackZ(entry.localPos()) + 0.5));
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /** Посадка/разборка (T052): блоки возвращаются в мир; жёсткий удар — взрыв. */
    private void land(ServerLevel level) {
        double impactSpeed = new Vec3(flight.vel().x(), flight.vel().y(), flight.vel().z()).length();
        ejectPassengers();

        int baseX = (int) Math.round(getX() - halfX());
        int baseY = (int) Math.round(getY());
        int baseZ = (int) Math.round(getZ() - halfZ());
        for (RocketData.Entry entry : rocketData.blocks()) {
            BlockPos target = new BlockPos(
                    baseX + PackedPos.unpackX(entry.localPos()),
                    baseY + PackedPos.unpackY(entry.localPos()),
                    baseZ + PackedPos.unpackZ(entry.localPos()));
            level.setBlock(target, entry.state(), 3);
        }
        if (impactSpeed > CRASH_SPEED) {
            float power = (float) Math.min(4.0, impactSpeed / 5.0);
            level.explode(this, getX(), getY(), getZ(), power, Level.ExplosionInteraction.BLOCK);
        }
        level.playSound(null, blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 2.0f, 0.6f);
        discard();
    }

    // ---------- Прочее ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // v1: неразрушима оружием
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (rocketData != null) {
            RocketData persisted = new RocketData(rocketData.blocks(), flight.propellantKg());
            output.store("rocket", RocketData.CODEC, persisted);
        }
        output.putBoolean("launched", launched);
        output.putDouble("vel_x", flight == null ? 0 : flight.vel().x());
        output.putDouble("vel_y", flight == null ? 0 : flight.vel().y());
        output.putDouble("vel_z", flight == null ? 0 : flight.vel().z());
        output.putDouble("pitch_rad", flight == null ? 0 : flight.pitch());
        output.putDouble("roll_rad", flight == null ? 0 : flight.roll());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        input.read("rocket", RocketData.CODEC).ifPresent(data -> {
            this.rocketData = data;
            rebuildDerived();
            Tag tag = RocketData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
            entityData.set(DATA_STRUCTURE, (CompoundTag) tag);
            this.flight = new FlightState(corePos(),
                    new org.alex_melan.spacereloaded.core.geometry.Vec3d(
                            input.getDoubleOr("vel_x", 0),
                            input.getDoubleOr("vel_y", 0),
                            input.getDoubleOr("vel_z", 0)),
                    input.getDoubleOr("pitch_rad", 0),
                    input.getDoubleOr("roll_rad", 0),
                    0, 0, data.propellantKg());
        });
        this.launched = input.getBooleanOr("launched", false);
        entityData.set(DATA_LAUNCHED, launched);
    }
}
