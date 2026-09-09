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
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;

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
    private static final EntityDataAccessor<Float> DATA_FUEL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> DATA_DESTINATION =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    // Полёт 2.0: ступени (US1)
    private static final EntityDataAccessor<Integer> DATA_STAGE =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DATA_STAGE_COUNT =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> DATA_STAGE_FUEL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_DELTA_V =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    // Полёт 2.0: ориентация (US2)
    private static final EntityDataAccessor<Float> DATA_CMD_PITCH =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DATA_CMD_ROLL =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HAS_GYRO =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);
    // Полёт 2.0: входной нагрев (US3)
    /** 003 (FR-104): цена следующего хопа — перелёт + посадка при спуске, м/с (0 — нет цели/стоимости). */
    private static final EntityDataAccessor<Float> DATA_TRANSFER_DV =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> DATA_HEATING =
            SynchedEntityData.defineId(RocketEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double DT = 0.05; // серверный тик
    /** Остаток топлива ступени, ниже которого она считается выгоревшей (кг). */
    private static final double STAGE_EMPTY_KG = 0.5;
    private static final double CRASH_SPEED = 15.0; // м/с — жёсткая посадка
    private static final double CAPSULE_CRASH_SPEED = 25.0; // капсула: теплозащита+амортизация

    /**
     * Клиентская интерполяция (26.2): сервер шлёт позицию раз в 2 тика, без
     * сглаживания ракета дёргается, а пассажир прыгает вместе с ней
     * (positionRider берёт координаты аппарата). Обработчик сглаживает
     * позицию между пакетами; ванильный Entity.getInterpolation() = null.
     */
    private final net.minecraft.world.entity.InterpolationHandler interpolation =
            new net.minecraft.world.entity.InterpolationHandler(this);

    private RocketData rocketData;
    private RocketStructure structure;
    private FlightState flight;
    private boolean launched;
    private boolean prevSprint;
    private boolean fuelOutWarned;
    private boolean windowWarned;
    /** 003 (FR-103): предупреждение о нехватке Δv перелёта — один раз за подъём. */
    private boolean transferWarned;
    /** 003 (FR-116): беспилотный маршрут продолжается с промежуточной платформы. */
    private boolean routeContinues;
    /** Тик повторного беспилотного старта (пересадка); 0 — нет. */
    private long relaunchAtTick;
    /** Беспилотный набор высоты до орбиты (запуск спутников/грузов). */
    private boolean autopilot;
    /** Фаза беспилотной посадки после прибытия «снижение» (suicide-burn-lite). */
    private boolean descentMode;
    /** Полётная программа: посадочный маяк (точка прибытия). */
    @org.jetbrains.annotations.Nullable
    private net.minecraft.core.GlobalPos programPad;
    private int programFrequency;
    /** Груз (агрегат всех отсеков; раскладывается по ним при разборе). */
    private final java.util.List<net.minecraft.world.item.ItemStack> cargoItems =
            new java.util.ArrayList<>();
    private int destinationIndex;

    // --- Полёт 2.0: ступени (D11/D15) ---
    /** Раскладка ступеней текущей структуры; пересчитывается в rebuildDerived. */
    private org.alex_melan.spacereloaded.core.rocketry.StageLayout layout;
    /** Активная (нижняя оставшаяся) ступень. */
    private int activeStage;
    /** Топливо по ступеням, кг; {@code flight.propellantKg()} зеркалит активную. */
    private double[] stagePropellant = new double[0];
    /** Кэш активного вида структуры для интегратора; сбрасывается при смене ступени/заправке. */
    private RocketStructure activeView;
    /** Аэродинамическое тело активного вида (кэш вместе с activeView). */
    private org.alex_melan.spacereloaded.core.atmosphere.DragBody activeDrag;
    /** Обломок ступени: без экипажа, двигатели заглушены, предельное время жизни. */
    private boolean debris;
    private int debrisTicks;
    /** Команда ориентации пилота (D14); без пилота — вертикаль; не сохраняется. */
    private org.alex_melan.spacereloaded.core.rocketry.AttitudeCommand attitude =
            org.alex_melan.spacereloaded.core.rocketry.AttitudeCommand.LEVEL;

    // Производные размеры (сервер и клиент)
    private float sizeX = 1;
    private float sizeY = 2;
    private float sizeZ = 1;
    private float comY = 1;
    private long commandLocal;
    private java.util.List<Long> seatLocals = java.util.List.of();

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
        builder.define(DATA_FUEL, 0.0f);
        builder.define(DATA_DESTINATION, 0);
        builder.define(DATA_STAGE, 0);
        builder.define(DATA_STAGE_COUNT, 1);
        builder.define(DATA_STAGE_FUEL, 0.0f);
        builder.define(DATA_DELTA_V, 0.0f);
        builder.define(DATA_CMD_PITCH, 0.0f);
        builder.define(DATA_CMD_ROLL, 0.0f);
        builder.define(DATA_HAS_GYRO, false);
        builder.define(DATA_HEATING, false);
        builder.define(DATA_TRANSFER_DV, 0.0f);
    }

    /** Сервер: установить структуру после сборки (до addFreshEntity); топливо — по ёмкости ступеней. */
    public void setAssembly(RocketData data) {
        setAssembly(data, null);
    }

    /**
     * Сервер: структура + честное топливо по ступеням (суммы баков из скана).
     *
     * @param propellantByStage топливо каждой ступени снизу вверх; null — распределить
     *                          суммарное {@code data.propellantKg()} по ёмкости ступеней
     */
    public void setAssembly(RocketData data, double[] propellantByStage) {
        this.rocketData = data;
        rebuildDerived();
        this.activeStage = 0;
        if (level() instanceof ServerLevel serverLevel) {
            // 003: цель по умолчанию — ближайший переход планеты (раньше индекс 0 =
            // первая планета алфавитного списка, т.е. пояс астероидов: планировщик
            // честно отказывал бы в старте «к поясу» каждому свежесобранному борту)
            destinationIndex = defaultDestinationIndex(serverLevel);
            entityData.set(DATA_DESTINATION, destinationIndex);
        }
        this.stagePropellant = propellantByStage != null && propellantByStage.length == layout.stageCount()
                ? propellantByStage.clone()
                : layout.distributeByCapacity(data.propellantKg());
        this.flight = FlightState.atRest(corePos(), stagePropellant[activeStage]);
        syncStageData();
        Tag tag = RocketData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        entityData.set(DATA_STRUCTURE, (CompoundTag) tag);
    }

    private void rebuildDerived() {
        structure = rocketData.toStructure();
        layout = org.alex_melan.spacereloaded.core.rocketry.StageLayout.of(structure);
        activeView = null;
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
        seatLocals = rocketData.seatLocals();
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

    // ---------- Ступени (Полёт 2.0, US1) ----------

    public int activeStage() {
        return activeStage;
    }

    public int stageCount() {
        return layout == null ? 1 : layout.stageCount();
    }

    /** Топливо ступени, кг (0 для несуществующего индекса). */
    public double stagePropellantKg(int stage) {
        return stage >= 0 && stage < stagePropellant.length ? stagePropellant[stage] : 0;
    }

    /** Обломок ступени (без экипажа, летит по баллистике до удара/утилизации). */
    public boolean isDebris() {
        return debris;
    }

    /** Раскладка ступеней (003: вход планировщика маршрута). */
    public org.alex_melan.spacereloaded.core.rocketry.StageLayout layout() {
        return layout;
    }

    RocketData rocketData() {
        return rocketData;
    }

    FlightState flightState() {
        return flight;
    }

    /** Топливо по ступеням, копия (003: вход планировщика маршрута). */
    public double[] stagePropellantSnapshot() {
        return stagePropellant.clone();
    }

    /** Суммарное топливо всех ступеней, кг. */
    private double totalPropellant() {
        double total = 0;
        for (double kg : stagePropellant) {
            total += kg;
        }
        return total;
    }

    /** Активный вид структуры для интегратора (D11): кэш до смены ступени/заправки. */
    private RocketStructure currentView() {
        if (activeView == null) {
            activeView = layout.activeView(activeStage, stagePropellant);
            activeDrag = activeView.dragBody(org.alex_melan.spacereloaded.SpaceReloaded.config().rocketDragCoefficient);
        }
        return activeView;
    }

    /** Аэродинамическое тело оставшегося стека (габариты активного вида, C_d из конфига). */
    private org.alex_melan.spacereloaded.core.atmosphere.DragBody currentDrag() {
        currentView();
        return activeDrag == null ? org.alex_melan.spacereloaded.core.atmosphere.DragBody.NONE : activeDrag;
    }

    /**
     * Герметичная кабина: командный модуль (или возвратная капсула — тоже role=command)
     * в оставшемся стеке — экипаж в креслах дышит без маски (замечание плейтеста:
     * «персонаж просто задыхается в космосе» — в закрытой капсуле не должен).
     */
    public boolean hasPressurizedCabin() {
        if (rocketData == null || debris) {
            return false;
        }
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (entry.role().equals("command")) {
                return true;
            }
        }
        return false;
    }

    /** Теплозащита стека (FR-083): возвратная капсула в конструкции. */
    private boolean hasReturnCapsule() {
        return rocketData != null && rocketData.blocks().stream().anyMatch(e ->
                e.state().is(org.alex_melan.spacereloaded.registry.ModBlocks.RETURN_CAPSULE));
    }

    /** 003: цена следующего хопа для HUD, м/с. */
    public float clientTransferDeltaV() {
        return entityData.get(DATA_TRANSFER_DV);
    }

    public boolean clientHeating() {
        return entityData.get(DATA_HEATING);
    }

    /** Ступень детали по её локальной позиции (для разборки: чья доля топлива). */
    private int stageOf(long localPos) {
        for (var stage : layout.stages()) {
            for (var part : stage.parts()) {
                if (part.packedPos() == localPos) {
                    return stage.index();
                }
            }
        }
        return activeStage;
    }

    private FlightState withPropellant(FlightState state, double propellantKg) {
        return new FlightState(state.pos(), state.vel(), state.pitch(), state.roll(),
                state.pitchRate(), state.rollRate(), propellantKg);
    }

    /** Синхронизация полей ступеней и топлива на клиент (HUD). */
    private void syncStageData() {
        entityData.set(DATA_STAGE, activeStage);
        entityData.set(DATA_STAGE_COUNT, stageCount());
        entityData.set(DATA_STAGE_FUEL, (float) stagePropellantKg(activeStage));
        entityData.set(DATA_FUEL, (float) totalPropellant());
        // Управление ориентацией есть только при гиродине в оставшемся стеке (FR-071)
        boolean gyro = false;
        if (layout != null) {
            for (int i = activeStage; i < layout.stageCount() && !gyro; i++) {
                for (var part : layout.stage(i).parts()) {
                    if (part.properties().gyroTorqueNm() > 0) {
                        gyro = true;
                        break;
                    }
                }
            }
        }
        entityData.set(DATA_HAS_GYRO, gyro);
    }

    /** Остаток Δv стека (сумма ступеней от активной), м/с — для HUD, раз в 10 тиков. */
    private double remainingDeltaV(double gravity) {
        var report = org.alex_melan.spacereloaded.core.rocketry.StagedPerformance
                .calculate(layout, stagePropellant, gravity > 0 ? gravity : 9.81);
        double total = 0;
        for (int i = activeStage; i < report.stages().size(); i++) {
            total += report.stages().get(i).deltaV();
        }
        return total;
    }

    /**
     * Верхний стек после отделения ступени (вызывается {@link StageSeparation}):
     * новая структура с нормализованными координатами, новая позиция сущности,
     * топливо оставшихся ступеней и приращение скорости от импульса разделения.
     */
    void applyRemainingStack(RocketData remaining, Vec3 newPos, double[] remainingStages,
                             org.alex_melan.spacereloaded.core.geometry.Vec3d deltaVel) {
        this.rocketData = remaining;
        rebuildDerived();
        this.activeStage = 0;
        this.stagePropellant = remainingStages.length == layout.stageCount()
                ? remainingStages.clone() : layout.distributeByCapacity(remaining.propellantKg());
        setPos(newPos.x, newPos.y, newPos.z);
        this.flight = new FlightState(corePos(), flight.vel().add(deltaVel), flight.pitch(), flight.roll(),
                flight.pitchRate(), flight.rollRate(), stagePropellant[0]);
        this.fuelOutWarned = false;
        syncStageData();
        Tag tag = RocketData.CODEC.encodeStart(NbtOps.INSTANCE, remaining).getOrThrow();
        entityData.set(DATA_STRUCTURE, (CompoundTag) tag);
        // Кресла отброшенной ступени ушли вместе с ней: лишние пассажиры — за борт (честно)
        java.util.List<Entity> riders = new java.util.ArrayList<>(getPassengers());
        for (int i = riders.size() - 1; i >= 1 + seatLocals.size(); i--) {
            riders.get(i).stopRiding();
        }
    }

    /** Обломок ступени: летит без экипажа с унаследованной ориентацией и скоростью. */
    void markDebris(org.alex_melan.spacereloaded.core.geometry.Vec3d velocity, double pitch, double roll) {
        this.debris = true;
        this.debrisTicks = 0;
        this.launched = true;
        this.autopilot = false;
        this.descentMode = false;
        entityData.set(DATA_LAUNCHED, true);
        this.flight = new FlightState(corePos(), velocity, pitch, roll, 0, 0, stagePropellant[activeStage]);
        entityData.set(DATA_PITCH, (float) Math.toDegrees(pitch));
        entityData.set(DATA_ROLL, (float) Math.toDegrees(roll));
    }

    /**
     * Команда пилота на отделение ступени (FR-065): только первый пассажир,
     * только в полёте, только при наличии нижней ступени.
     */
    public Component requestStageSeparation(ServerPlayer player) {
        if (getFirstPassenger() != player) {
            return Component.translatable("message.spacereloaded.stage.not_pilot");
        }
        if (!launched || level().isClientSide()) {
            return Component.translatable("message.spacereloaded.stage.not_launched");
        }
        if (activeStage >= stageCount() - 1) {
            return Component.translatable("message.spacereloaded.stage.last");
        }
        return StageSeparation.separate((ServerLevel) level(), this);
    }

    /** Стенд: задать скорость полёта (м/с). */
    public void setFlightVelocity(Vec3 velocity) {
        if (flight != null) {
            flight = new FlightState(flight.pos(),
                    new org.alex_melan.spacereloaded.core.geometry.Vec3d(velocity.x, velocity.y, velocity.z),
                    flight.pitch(), flight.roll(), flight.pitchRate(), flight.rollRate(), flight.propellantKg());
        }
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
    public net.minecraft.world.entity.InterpolationHandler getInterpolation() {
        return interpolation;
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

    /** Тип топлива ракеты — по двигателям (сборка гарантирует единый тип). */
    public String rocketFuelType() {
        if (rocketData == null) {
            return "";
        }
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (entry.role().equals("engine")) {
                return entry.fuel();
            }
        }
        return "";
    }

    /** Припаркована (не в полёте) — можно заправлять/разбирать. */
    public boolean isParked() {
        return !launched && rocketData != null;
    }

    private boolean hasPayload(net.minecraft.world.level.block.Block block) {
        if (rocketData == null) {
            return false;
        }
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (entry.state().is(block)) {
                return true;
            }
        }
        return false;
    }

    /** Есть ли спутниковая полезная нагрузка связи (Phase 12). */
    public boolean hasSatellite() {
        return hasPayload(org.alex_melan.spacereloaded.registry.ModBlocks.SATELLITE);
    }

    /** Есть ли энергоспутник (Phase 14). */
    public boolean hasPowerSatellite() {
        return hasPayload(org.alex_melan.spacereloaded.registry.ModBlocks.POWER_SATELLITE);
    }

    /** Слоты груза: 15 на каждый грузовой отсек в структуре. */
    public int cargoSlots() {
        if (rocketData == null) {
            return 0;
        }
        int holds = 0;
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (entry.state().is(org.alex_melan.spacereloaded.registry.ModBlocks.CARGO_HOLD)) {
                holds++;
            }
        }
        return holds * CargoHoldBlockEntity.SLOTS;
    }

    /** Погрузка: мерж в существующие стеки, затем новые слоты. Возврат — остаток. */
    public net.minecraft.world.item.ItemStack loadCargo(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return stack;
        }
        for (net.minecraft.world.item.ItemStack existing : cargoItems) {
            if (net.minecraft.world.item.ItemStack.isSameItemSameComponents(existing, stack)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room > 0) {
                    int moved = Math.min(room, stack.getCount());
                    existing.grow(moved);
                    stack.shrink(moved);
                    if (stack.isEmpty()) {
                        return net.minecraft.world.item.ItemStack.EMPTY;
                    }
                }
            }
        }
        if (cargoItems.size() < cargoSlots()) {
            cargoItems.add(stack.copy());
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return stack;
    }

    /** Разгрузка: последний стек (LIFO), пустой — если груза нет. */
    public net.minecraft.world.item.ItemStack unloadCargo() {
        if (cargoItems.isEmpty()) {
            return net.minecraft.world.item.ItemStack.EMPTY;
        }
        return cargoItems.remove(cargoItems.size() - 1);
    }

    /** Всего предметов на борту (для ЦУП/тестов). */
    public int cargoCount() {
        return cargoItems.stream().mapToInt(net.minecraft.world.item.ItemStack::getCount).sum();
    }

    /** Сервер: загрузить груз при сборке (из отсеков-BE). */
    public void setCargo(java.util.List<net.minecraft.world.item.ItemStack> items) {
        cargoItems.clear();
        for (net.minecraft.world.item.ItemStack stack : items) {
            if (!stack.isEmpty()) {
                cargoItems.add(stack);
            }
        }
    }

    /**
     * Снимок структуры с ТЕКУЩИМ суммарным топливом (для стыковочных операций).
     * Упрощение (D15): после стыковки/расстыковки топливо перераспределяется
     * по ёмкости ступеней каждой части.
     */
    public RocketData rocketDataForDocking() {
        return new RocketData(rocketData.blocks(), flight == null ? 0 : totalPropellant());
    }

    /** Локальный Y стыковочного узла в ячейке клика (допуск ±1), если есть. */
    private java.util.OptionalInt clampCellAt(Vec3 hitPos) {
        if (rocketData == null) {
            return java.util.OptionalInt.empty();
        }
        int lx = (int) Math.floor(hitPos.x - (getX() - halfX()));
        int ly = (int) Math.floor(hitPos.y - getY());
        int lz = (int) Math.floor(hitPos.z - (getZ() - halfZ()));
        for (RocketData.Entry entry : rocketData.blocks()) {
            if (!entry.role().equals("clamp")) {
                continue;
            }
            if (Math.abs(PackedPos.unpackX(entry.localPos()) - lx) <= 1
                    && Math.abs(PackedPos.unpackY(entry.localPos()) - ly) <= 1
                    && Math.abs(PackedPos.unpackZ(entry.localPos()) - lz) <= 1) {
                return java.util.OptionalInt.of(PackedPos.unpackY(entry.localPos()));
            }
        }
        return java.util.OptionalInt.empty();
    }

    /** Текущий запас топлива всех ступеней, кг. */
    public double propellantKg() {
        return flight == null ? 0 : totalPropellant();
    }

    // --- Клиентские аксессоры для HUD ---

    public float clientFuelKg() {
        return entityData.get(DATA_FUEL);
    }

    public double clientFuelCapacityKg() {
        if (rocketData == null) {
            return 0;
        }
        double capacity = 0;
        for (RocketData.Entry entry : rocketData.blocks()) {
            capacity += entry.capacityKg();
        }
        return capacity;
    }

    public boolean clientLaunched() {
        return entityData.get(DATA_LAUNCHED);
    }

    public int clientDestinationIndex() {
        return entityData.get(DATA_DESTINATION);
    }

    public int clientStage() {
        return entityData.get(DATA_STAGE);
    }

    public int clientStageCount() {
        return entityData.get(DATA_STAGE_COUNT);
    }

    public float clientStageFuelKg() {
        return entityData.get(DATA_STAGE_FUEL);
    }

    /** Остаток Δv стека (сумма ступеней от активной), м/с. */
    public float clientDeltaV() {
        return entityData.get(DATA_DELTA_V);
    }

    public float clientCmdPitchDeg() {
        return entityData.get(DATA_CMD_PITCH);
    }

    public float clientCmdRollDeg() {
        return entityData.get(DATA_CMD_ROLL);
    }

    /** Есть ли гиродин в оставшемся стеке — иначе команды ориентации не действуют. */
    public boolean clientHasGyro() {
        return entityData.get(DATA_HAS_GYRO);
    }

    /**
     * Заправка (рукав/колонна): принять до amountKg, вернуть фактически принятое.
     * Топливо раскладывается по ступеням пропорционально свободной ёмкости
     * (магистраль заполняет все баки, порядок не важен).
     */
    public double refuel(double amountKg) {
        if (launched || structure == null || flight == null) {
            return 0;
        }
        double free = 0;
        for (int i = 0; i < stagePropellant.length; i++) {
            free += Math.max(0, layout.stage(i).propellantCapacityKg() - stagePropellant[i]);
        }
        double accepted = Math.clamp(amountKg, 0, free);
        if (accepted > 0) {
            for (int i = 0; i < stagePropellant.length; i++) {
                double room = Math.max(0, layout.stage(i).propellantCapacityKg() - stagePropellant[i]);
                stagePropellant[i] += accepted * room / free;
            }
            flight = withPropellant(flight, stagePropellant[activeStage]);
            activeView = null;
            syncStageData();
            fuelOutWarned = false;
        }
        return accepted;
    }

    /** Слив (рукав/колонна): отдать до amountKg, пропорционально остаткам ступеней. */
    public double drain(double amountKg) {
        if (launched || flight == null) {
            return 0;
        }
        double total = totalPropellant();
        double drained = Math.clamp(amountKg, 0, total);
        if (drained > 0) {
            for (int i = 0; i < stagePropellant.length; i++) {
                stagePropellant[i] -= drained * stagePropellant[i] / total;
            }
            flight = withPropellant(flight, stagePropellant[activeStage]);
            activeView = null;
            syncStageData();
        }
        return drained;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 hitPos) {
        // Пульт: Sneak+ПКМ по припаркованной ракете — беспилотный старт (спутник)
        if (player.getItemInHand(hand).is(org.alex_melan.spacereloaded.registry.ModItems.TARGETING_DESIGNATOR)
                && player.isSecondaryUseActive()) {
            if (!level().isClientSide() && isParked()
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(launchUnmanned((ServerLevel) level()));
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.SUCCESS;
        }
        // Полётная программа: ПКМ по припаркованной — загрузить маршрут
        if (player.getItemInHand(hand).is(org.alex_melan.spacereloaded.registry.ModItems.FLIGHT_PROGRAM)) {
            if (!level().isClientSide() && isParked()
                    && player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(
                        installProgram((ServerLevel) level(), player.getItemInHand(hand)));
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.SUCCESS;
        }
        // Заправочный рукав: ПКМ — закачать из подключённого бака, sneak+ПКМ — слить
        if (player.getItemInHand(hand).is(org.alex_melan.spacereloaded.registry.ModItems.FUELING_HOSE)) {
            if (!level().isClientSide() && !launched
                    && player instanceof ServerPlayer serverPlayer) {
                if (player.isSecondaryUseActive()) {
                    FuelingHose.drainFromRocket(serverPlayer, (ServerLevel) level(), this);
                } else {
                    FuelingHose.pumpToRocket(serverPlayer, (ServerLevel) level(), this);
                }
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isSecondaryUseActive()) {
            // Sneak+ПКМ: по стыковочному узлу — расстыковка/стыковка (US6),
            // по остальному корпусу — разобрать в блоки
            if (!level().isClientSide() && !launched && rocketData != null) {
                var clampCell = clampCellAt(hitPos);
                if (clampCell.isPresent() && player instanceof ServerPlayer serverPlayer) {
                    int clampY = clampCell.getAsInt();
                    boolean hasLower = rocketData.blocks().stream()
                            .anyMatch(e -> PackedPos.unpackY(e.localPos()) < clampY);
                    Component result = hasLower
                            ? DockingSystem.undock((ServerLevel) level(), this, clampY)
                            : DockingSystem.dock((ServerLevel) level(), this, clampY);
                    serverPlayer.sendSystemMessage(result);
                    return InteractionResult.SUCCESS_SERVER;
                }
                ejectPassengers();
                disassembleInto((ServerLevel) level());
                discard();
                return InteractionResult.SUCCESS_SERVER;
            }
            return InteractionResult.PASS;
        }
        if (!level().isClientSide()) {
            player.startRiding(this); // canAddPassenger ограничит вместимость
            return InteractionResult.SUCCESS_SERVER;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected boolean canAddPassenger(Entity passenger) {
        if (debris) {
            return false; // на падающий обломок не сесть
        }
        return getPassengers().size() < 1 + seatLocals.size(); // модуль = место пилота
    }

    @Override
    protected void positionRider(Entity passenger, Entity.MoveFunction moveFunction) {
        if (!hasPassenger(passenger)) {
            return;
        }
        // Пилот — в командном модуле, остальные — по креслам (детерминированный порядок)
        int index = getPassengers().indexOf(passenger);
        long seat = commandLocal;
        if (index > 0 && index - 1 < seatLocals.size()) {
            seat = seatLocals.get(index - 1);
        }
        double x = getX() + PackedPos.unpackX(seat) - halfX() + 0.5;
        double y = getY() + PackedPos.unpackY(seat) + 0.1;
        double z = getZ() + PackedPos.unpackZ(seat) - halfZ() + 0.5;
        moveFunction.accept(passenger, x, y, z);
    }

    // ---------- Полёт ----------

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            interpolation.interpolate(); // плавное движение аппарата и пассажира
            return;
        }
        if (rocketData == null) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel) level();

        boolean jump = false;
        boolean sprint = false;
        ServerPlayer pilot = getFirstPassenger() instanceof ServerPlayer sp ? sp : null;
        if (pilot != null) {
            Input input = pilot.getLastClientInput();
            jump = input.jump();
            sprint = input.sprint();
            autopilot = false; // ручное управление приоритетнее
        } else if (autopilot && launched) {
            // Набор высоты — полная тяга; посадка — гасить скорость свыше 12 м/с
            jump = !descentMode || flight.vel().y() < -12.0;
        }

        if (!launched) {
            // Выбор цели полёта (спринт циклит список из профиля планеты)
            if (sprint && !prevSprint && pilot != null) {
                cycleDestination(serverLevel, pilot);
            }
            prevSprint = sprint;
            if (jump) {
                ignite(serverLevel);
            }
            // 003: пересадка автопилота — повторный беспилотный старт по сроку (FR-116)
            if (pilot != null) {
                relaunchAtTick = 0; // экипаж на борту — ручной режим
            } else if (relaunchAtTick > 0 && serverLevel.getGameTime() >= relaunchAtTick) {
                LaunchResult relaunch = tryLaunchUnmanned(serverLevel);
                if (!relaunch.launched()) {
                    relaunchAtTick = serverLevel.getGameTime()
                            + org.alex_melan.spacereloaded.SpaceReloaded.config().autopilotRelaunchDelayTicks;
                    org.alex_melan.spacereloaded.SpaceReloaded.LOGGER.info("Пересадка отложена: {}",
                            relaunch.message().getString());
                }
            }
            if (tickCount % 20 == 0) {
                entityData.set(DATA_TRANSFER_DV, (float) nextHopCost(serverLevel));
            }
            return;
        }
        prevSprint = sprint;

        // Обломок ступени (FR-067): тяги нет; предельное время жизни — предохранитель
        if (debris) {
            jump = false;
            if (++debrisTicks > org.alex_melan.spacereloaded.SpaceReloaded.config().stageDebrisMaxTicks) {
                discard();
                return;
            }
        }
        // Автопилот (FR-065): активная ступень выгорела — отделить и лететь дальше
        if (autopilot && pilot == null && activeStage < stageCount() - 1
                && stagePropellant[activeStage] <= STAGE_EMPTY_KG) {
            StageSeparation.separate(serverLevel, this);
            return;
        }

        // Полёт 2.0 (FR-070, D14): наклон относительно взгляда пилота — отрабатывают гиродины.
        // Minecraft: yaw 0 = юг (+Z), рост yaw — по часовой; forward = (−sin, cos), right = (−cos, −sin)
        double tiltX = 0;
        double tiltZ = 0;
        if (pilot != null) {
            Input steer = pilot.getLastClientInput();
            double forward = (steer.forward() ? 1 : 0) - (steer.backward() ? 1 : 0);
            double right = (steer.right() ? 1 : 0) - (steer.left() ? 1 : 0);
            if (forward != 0 || right != 0) {
                double yaw = Math.toRadians(pilot.getYHeadRot());
                tiltX = -Math.sin(yaw) * forward - Math.cos(yaw) * right;
                tiltZ = Math.cos(yaw) * forward - Math.sin(yaw) * right;
            }
        }
        var config = org.alex_melan.spacereloaded.SpaceReloaded.config();
        attitude = pilot == null
                ? org.alex_melan.spacereloaded.core.rocketry.AttitudeCommand.LEVEL
                : attitude.step(tiltX, tiltZ, DT, config.attitudeRateDegPerSec, config.attitudeMaxDeg);
        entityData.set(DATA_CMD_PITCH, (float) attitude.pitchDeg());
        entityData.set(DATA_CMD_ROLL, (float) attitude.rollDeg());

        ControlInput control = new ControlInput(jump ? 1.0 : 0.0, attitude.pitchRad(), attitude.rollRad(), true);
        flight = new FlightState(corePos(), flight.vel(), flight.pitch(), flight.roll(),
                flight.pitchRate(), flight.rollRate(), stagePropellant[activeStage]);
        // Среда: гравитация + атмосфера тела (FR-080); сопротивление по габаритам стека (FR-081)
        FlightEnvironment env = org.alex_melan.spacereloaded.planet.PlanetManager.environment(serverLevel);
        double gravity = env.gravity();
        // D11: интегратор видит активный вид — тяга и баки только активной ступени
        flight = FlightIntegrator.step(currentView(), flight, control, env, DT, currentDrag());
        stagePropellant[activeStage] = flight.propellantKg();

        setPos(flight.pos().x(), flight.pos().y(), flight.pos().z());
        setDeltaMovement(flight.vel().x() * DT, flight.vel().y() * DT, flight.vel().z() * DT);
        entityData.set(DATA_PITCH, (float) Math.toDegrees(flight.pitch()));
        entityData.set(DATA_ROLL, (float) Math.toDegrees(flight.roll()));
        if (Math.abs(entityData.get(DATA_STAGE_FUEL) - flight.propellantKg()) > 1.0) {
            syncStageData();
        }
        if (tickCount % 10 == 0) {
            entityData.set(DATA_DELTA_V, (float) remainingDeltaV(gravity));
            entityData.set(DATA_TRANSFER_DV, (float) nextHopCost(serverLevel));
        }
        // Ниже границы мира (пустота орбиты, потерянный обломок) — утилизация, не вечный объект
        if (getY() < serverLevel.getMinY() - 64) {
            ejectPassengers();
            discard();
            return;
        }

        // T056: полётные ticket'ы (persist + keep-dimension-active) — полёт
        // завершится и без игрока рядом, и после перезапуска сервера
        if (tickCount % 20 == 1) {
            org.alex_melan.spacereloaded.planet.ModTickets.holdStrike(serverLevel, blockPosition(), 1);
        }

        double speedNow = new Vec3(flight.vel().x(), flight.vel().y(), flight.vel().z()).length();

        // Входной нагрев (FR-083): √ρ·v³ выше порога — плазма; без капсулы экипаж горит
        double heatIndex = org.alex_melan.spacereloaded.core.rocketry.Aerothermal
                .heatIndex(env.density(getY()), speedNow);
        boolean heating = heatIndex > config.reentryHeatIndexThreshold;
        if (heating != entityData.get(DATA_HEATING)) {
            entityData.set(DATA_HEATING, heating);
        }
        if (heating) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
                    getX(), getY() + (flight.vel().y() > 0 ? sizeY : 0.0), getZ(),
                    12, halfX() * 0.6, 0.5, halfZ() * 0.6, 0.08);
            if (!hasReturnCapsule() && tickCount % config.reentryHeatIntervalTicks == 0) {
                for (Entity passenger : List.copyOf(getPassengers())) {
                    if (passenger instanceof net.minecraft.world.entity.LivingEntity living) {
                        living.hurtServer(serverLevel,
                                org.alex_melan.spacereloaded.registry.ModDamageTypes.reentryHeat(serverLevel),
                                config.reentryHeatDamage);
                    }
                }
            }
        }
        // T062: топливо кончилось — дальше только честная баллистика (или отделение ступени)
        if (flight.propellantKg() <= STAGE_EMPTY_KG && !fuelOutWarned) {
            fuelOutWarned = true;
            if (pilot != null) {
                pilot.sendOverlayMessage(Component.translatable(activeStage < stageCount() - 1
                        ? "message.spacereloaded.stage.burnout"
                        : "message.spacereloaded.rocket.fuel_out"));
            }
        }
        // Эффекты: факел двигателя при тяге, плазменный след на скорости
        if (jump && flight.propellantKg() > 0) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX(), getY() - 0.2, getZ(), 6, halfX() * 0.4, 0.2, halfZ() * 0.4, 0.02);
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.LARGE_SMOKE,
                    getX(), getY() - 0.5, getZ(), 3, halfX() * 0.5, 0.3, halfZ() * 0.5, 0.02);
        }
        if (speedNow > 40) {
            serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                    getX(), getY() + (flight.vel().y() > 0 ? sizeY : 0.0), getZ(),
                    8, halfX() * 0.5, 0.4, halfZ() * 0.5, 0.05);
        }

        // T055: интерпенетрация ведущих граней корпуса (бок/верх) — крушение
        if (hullCollides(serverLevel)) {
            crashInto(serverLevel);
            return;
        }
        if (flight.vel().y() <= 0 && touchesGround(serverLevel)) {
            land(serverLevel);
            return;
        }

        // Переход между измерениями: набрали высоту перехода профиля (FR-031)
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(serverLevel);
        if (profile.isPresent() && !profile.get().transitionTargets().isEmpty()
                && getY() >= profile.get().transitionAltitude()) {
            var hop = nextHop(serverLevel);
            if (hop != null && transferWindowOpen(serverLevel, hop, pilot)) {
                transition(serverLevel, profile.get(), hop);
            }
        } else if (getY() < (profile.map(pp -> pp.transitionAltitude()).orElse(Integer.MAX_VALUE) - 20)) {
            windowWarned = false; // спустились — предупреждение об окне снова актуально
            transferWarned = false;
        }
    }

    /**
     * Окно Гомана к выбранной цели (Phase 11). Луна/Земля всегда открыты;
     * Марс — только в окне запуска, иначе перелёт не начинается, пилот
     * получает предупреждение со сроком до окна.
     */
    private boolean transferWindowOpen(ServerLevel level,
            net.minecraft.resources.Identifier targetId, ServerPlayer pilot) {
        var target = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, targetId);
        if (target.isEmpty()) {
            return true;
        }
        // Покрытие: беспилотный межпланетный рейс требует спутника на орбите вылета
        boolean unmanned = autopilot && pilot == null;
        if (!org.alex_melan.spacereloaded.network.Logistics.coverageSatisfied(
                level.getServer(), level.dimension(), target.get(), unmanned)) {
            org.alex_melan.spacereloaded.SpaceReloaded.LOGGER.info(
                    "Беспилотный рейс к {} отклонён: нет спутникового покрытия на {}",
                    targetId, level.dimension().identifier());
            return false;
        }
        if (org.alex_melan.spacereloaded.planet.TransferWindows.isOpen(level.getGameTime(), target.get())) {
            return true;
        }
        if (!windowWarned && pilot != null) {
            windowWarned = true;
            long ticks = org.alex_melan.spacereloaded.planet.TransferWindows
                    .ticksToOpen(level.getGameTime(), target.get());
            pilot.sendOverlayMessage(Component.translatable(
                    "message.spacereloaded.rocket.window_closed",
                    Component.translatable("planet.spacereloaded." + targetId.getPath()),
                    ticks / 24000L, (ticks % 24000L) / 1200L));
        }
        return false;
    }

    /**
     * Перенос ракеты с пассажиром в целевое измерение (T056/FR-031/FR-034):
     * координаты масштабируются отношением coordinate_scale (AR-стиль: орбита
     * пространственно связана с точкой старта). Прибытие на орбиту — парковка
     * на автоплатформе (GC-стиль); прибытие в атмосферу — падение с ретро-burn.
     */
    private void transition(ServerLevel from,
            org.alex_melan.spacereloaded.registry.ModRegistries.PlanetProfile fromProfile,
            net.minecraft.resources.Identifier targetId) {
        var targetProfile = org.alex_melan.spacereloaded.planet.PlanetManager
                .profileById(from, targetId);
        if (targetProfile.isEmpty()) {
            return;
        }
        ServerLevel target = from.getServer().getLevel(net.minecraft.resources.ResourceKey.create(
                net.minecraft.core.registries.Registries.DIMENSION, targetProfile.get().dimension()));
        if (target == null) {
            return;
        }
        ServerPlayer pilot = getFirstPassenger() instanceof ServerPlayer sp ? sp : null;
        var config = org.alex_melan.spacereloaded.SpaceReloaded.config();
        // 003 (FR-102/FR-103, D21): списание Δv перелёта по Циолковскому через ступени ДО телепорта
        double transferCost = fromProfile.transferDeltaVTo(targetId) * config.transferDeltaVScale;
        if (transferCost > 0) {
            if (transferWarned) {
                return; // уже предупреждали за этот подъём — считать заново нечего
            }
            var burn = org.alex_melan.spacereloaded.core.rocketry.TransferBurn
                    .apply(layout, stagePropellant, activeStage, transferCost);
            if (!burn.achieved()) {
                transferWarned = true;
                String need = String.format(java.util.Locale.ROOT, "%.0f", transferCost);
                String have = String.format(java.util.Locale.ROOT, "%.0f", remainingDeltaV(9.81));
                if (pilot != null) {
                    pilot.sendOverlayMessage(Component.translatable(
                            "message.spacereloaded.rocket.transfer_short", need, have));
                }
                org.alex_melan.spacereloaded.SpaceReloaded.LOGGER.info(
                        "Перелёт к {} отклонён: нужно {} м/с, есть {} м/с", targetId, need, have);
                return;
            }
            if (burn.activeStage() > activeStage) {
                StageSeparation.dropBurnedStages(this, burn.activeStage(),
                        java.util.Arrays.copyOfRange(burn.propellantKg(), burn.activeStage(),
                                burn.propellantKg().length));
            } else {
                stagePropellant = burn.propellantKg();
                activeView = null;
                flight = withPropellant(flight, stagePropellant[activeStage]);
                syncStageData();
            }
        }
        boolean continueRoute = autopilot && pilot == null
                && !targetId.equals(finalDestination(from));

        double scale = fromProfile.coordinateScale() / targetProfile.get().coordinateScale();
        double targetX = getX() * scale;
        double targetZ = getZ() * scale;
        boolean toOrbit = "platform".equals(targetProfile.get().arrival());

        // Полётная программа: прибытие к посадочному маяку
        boolean padArrival = programPad != null
                && programPad.dimension().equals(target.dimension());
        if (padArrival) {
            targetX = programPad.pos().getX() + 0.5;
            targetZ = programPad.pos().getZ() + 0.5;
        }

        double targetY;
        if (toOrbit) {
            targetY = padArrival ? programPad.pos().getY() + 1
                    : org.alex_melan.spacereloaded.planet.PlanetManager
                            .ensureOrbitPlatform(target, targetX, targetZ);
        } else if (padArrival) {
            targetY = programPad.pos().getY() + config.arrivalHeightM;
        } else {
            targetY = Math.max(180.0, targetProfile.get().transitionAltitude() - 20.0);
        }

        org.alex_melan.spacereloaded.planet.ModTickets.holdAround(from, blockPosition(), 2);
        org.alex_melan.spacereloaded.planet.ModTickets.holdAround(target,
                BlockPos.containing(targetX, targetY, targetZ), 2);

        ejectPassengers();

        double[] savedStages = stagePropellant.clone();
        int savedActive = activeStage;
        Entity moved = teleport(new net.minecraft.world.level.portal.TeleportTransition(
                target, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, 0f, 0f,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));

        if (moved instanceof RocketEntity rocket) {
            rocket.postArrival(toOrbit, savedStages, savedActive, continueRoute);
            // Спутник/энергоспутник: развёртывание на орбите ТОЛЬКО беспилотно
            // (иначе экипаж и груз погибли бы вместе с аппаратом)
            boolean payload = rocket.hasSatellite() || rocket.hasPowerSatellite();
            if (toOrbit && payload && pilot == null) {
                var network = org.alex_melan.spacereloaded.network.SpaceNetworkState.get(target.getServer());
                if (rocket.hasSatellite()) {
                    network.addCoverage(target.dimension());
                }
                if (rocket.hasPowerSatellite()) {
                    network.addPowerSat(target.dimension());
                }
                target.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                        rocket.getX(), rocket.getY() + 1.5, rocket.getZ(), 40, 1.0, 1.0, 1.0, 0.05);
                target.playSound(null, rocket.blockPosition(),
                        net.minecraft.sounds.SoundEvents.BEACON_ACTIVATE,
                        net.minecraft.sounds.SoundSource.BLOCKS, 2.0f, 1.4f);
                rocket.discard();
                return;
            }
            if (pilot != null) {
                pilot.teleportTo(target, targetX, targetY + 1.0, targetZ,
                        java.util.Set.of(), pilot.getYRot(), pilot.getXRot(), false);
                pilot.startRiding(rocket, true, true);
                pilot.sendSystemMessage(Component.translatable(toOrbit
                        ? "message.spacereloaded.rocket.reached_orbit"
                        : "message.spacereloaded.rocket.reentry"));
            }
        }
    }

    /** Настройка после прибытия (вызывается на НОВОМ экземпляре после teleport). */
    private void postArrival(boolean parked, double[] stages, int active, boolean continueRoute) {
        if (layout != null && stages.length == layout.stageCount()) {
            this.stagePropellant = stages.clone();
            this.activeStage = Math.clamp(active, 0, layout.stageCount() - 1);
            this.activeView = null;
        }
        this.flight = new FlightState(corePos(),
                parked ? org.alex_melan.spacereloaded.core.geometry.Vec3d.ZERO
                       : new org.alex_melan.spacereloaded.core.geometry.Vec3d(0, -5, 0),
                0, 0, 0, 0, stagePropellantKg(activeStage));
        this.launched = !parked;
        if (parked) {
            autopilot = false;
            descentMode = false;
            // 003 (FR-116): цель дальше — пересадка через задержку
            this.routeContinues = continueRoute;
            this.relaunchAtTick = continueRoute
                    ? level().getGameTime() + org.alex_melan.spacereloaded.SpaceReloaded.config().autopilotRelaunchDelayTicks
                    : 0;
        } else if (autopilot) {
            descentMode = true; // беспилотная посадка к маяку
        }
        entityData.set(DATA_LAUNCHED, launched);
        entityData.set(DATA_PITCH, 0.0f);
        entityData.set(DATA_ROLL, 0.0f);
        setDeltaMovement(Vec3.ZERO);
        syncStageData();
    }

    /** Загрузка полётной программы: цель + посадочный маяк. */
    public Component installProgram(ServerLevel level, net.minecraft.world.item.ItemStack program) {
        var destination = program.get(
                org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_DESTINATION);
        var pad = program.get(
                org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_PAD);
        if (destination == null && pad == null) {
            return Component.translatable("message.spacereloaded.program.empty");
        }
        int frequency = program.getOrDefault(
                org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_FREQUENCY, 0);
        if (!installRoute(level, destination, pad, frequency)) {
            return Component.translatable("message.spacereloaded.program.unreachable",
                    String.valueOf(destination));
        }
        return Component.translatable("message.spacereloaded.program.installed",
                FlightProgramItem.describe(program));
    }

    /**
     * Маршрут борта (003, терминал и предмет-программа): цель — измерение планеты
     * из реестра (ЛЮБАЯ достижимая по хопам, как у карты полёта), маяк и канал.
     * Индекс цели — по общему списку планет (как {@link #setDestination}); раньше
     * программа писала индекс по transition_targets — другое пространство индексов.
     *
     * @return false, если цель не в реестре или маршрута к ней нет
     */
    public boolean installRoute(ServerLevel level, net.minecraft.resources.Identifier destinationDimension,
                                net.minecraft.core.GlobalPos pad, int frequency) {
        if (destinationDimension != null) {
            var access = level.registryAccess();
            var entry = org.alex_melan.spacereloaded.planet.Navigation.entryIdFor(access, destinationDimension);
            var here = org.alex_melan.spacereloaded.planet.Navigation.entryIdFor(access, level.dimension().identifier());
            if (entry == null || here == null
                    || org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, here, entry) == null) {
                return false;
            }
            setDestination(level, entry);
        }
        this.programPad = pad;
        this.programFrequency = frequency;
        return true;
    }

    /**
     * Беспилотный старт (T-спутник): полная тяга до высоты перехода. Цели
     * с прибытием «снижение» разрешены при заданном посадочном маяке —
     * автопилот выполнит suicide-burn-lite над ним.
     */
    public Component launchUnmanned(ServerLevel level) {
        return tryLaunchUnmanned(level).message();
    }

    /** Результат беспилотного старта: стартовал ли борт и сообщение (успех или причина отказа). */
    public record LaunchResult(boolean launched, Kind kind, Component message) {
        /** Класс исхода — терминал показывает состояние без разбора текста. */
        public enum Kind {
            OK, NO_TARGET, ONLY_ORBIT, TWR, BUDGET, WINDOW, NO_COVERAGE, AUTH
        }
    }

    private static LaunchResult refused(LaunchResult.Kind kind, Component message) {
        return new LaunchResult(false, kind, message);
    }

    /**
     * Беспилотный старт с честной проверкой (003, FR-107): планировщик бюджета
     * маршрута, окно перелёта на момент «сейчас + подъём», спутниковое покрытие —
     * отказ с причиной и цифрами вместо сожжённого впустую топлива.
     */
    public LaunchResult tryLaunchUnmanned(ServerLevel level) {
        if (!isParked() || debris) {
            return refused(LaunchResult.Kind.NO_TARGET,
                    Component.translatable("message.spacereloaded.rocket.autopilot_no_target"));
        }
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        var targetId = nextHop(level);
        if (profile.isEmpty() || targetId == null) {
            return refused(LaunchResult.Kind.NO_TARGET,
                    Component.translatable("message.spacereloaded.rocket.autopilot_no_target"));
        }
        var target = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, targetId);
        boolean descendTarget = target.isPresent() && !"platform".equals(target.get().arrival());
        if (target.isEmpty() || (descendTarget && programPad == null)) {
            return refused(LaunchResult.Kind.ONLY_ORBIT,
                    Component.translatable("message.spacereloaded.rocket.autopilot_only_orbit"));
        }
        if (!canLiftOff()) {
            boolean noPropellant = remainingDeltaV(9.81) <= 0;
            return refused(LaunchResult.Kind.TWR, Component.translatable(noPropellant
                    ? "message.spacereloaded.rocket.warning.NO_USABLE_PROPELLANT"
                    : "message.spacereloaded.rocket.warning.TWR_BELOW_ONE"));
        }
        // 003 (FR-105…FR-107): бюджет всего маршрута — отказ с причиной и цифрами
        var plan = org.alex_melan.spacereloaded.logistics.MissionPlanning.plan(level, this, finalDestination(level));
        if (plan != null && !plan.feasible()) {
            return refused(LaunchResult.Kind.BUDGET,
                    org.alex_melan.spacereloaded.logistics.MissionPlanning.describe(plan));
        }
        // Окно перелёта на момент прибытия на высоту перехода (иначе борт сжёг бы топливо зря)
        if (org.alex_melan.spacereloaded.planet.TransferWindows.hasWindow(target.get())) {
            double ascentS = plan == null ? 0 : plan.report().firstAscentTimeS();
            long at = level.getGameTime() + (Double.isFinite(ascentS) ? Math.round(ascentS * 20) : 0);
            if (!org.alex_melan.spacereloaded.planet.TransferWindows.isOpen(at, target.get())) {
                long ticks = org.alex_melan.spacereloaded.planet.TransferWindows.ticksToOpen(at, target.get());
                return refused(LaunchResult.Kind.WINDOW, Component.translatable("message.spacereloaded.rocket.window_closed",
                        Component.translatable("planet.spacereloaded." + targetId.getPath()),
                        ticks / 24000L, (ticks % 24000L) / 1200L));
            }
        }
        // Покрытие для беспилотного межпланетного рейса — до старта, а не на высоте перехода
        if (!org.alex_melan.spacereloaded.network.Logistics.coverageSatisfied(
                level.getServer(), level.dimension(), target.get(), true)) {
            return refused(LaunchResult.Kind.NO_COVERAGE, Component.translatable("message.spacereloaded.mission.no_coverage",
                    Component.translatable("planet.spacereloaded." + targetId.getPath())));
        }
        // Защищённая маршрутизация: разрешаем адрес доставки (аутентификация/перехват)
        if (programPad != null) {
            var routed = org.alex_melan.spacereloaded.network.SecureRouting.resolve(
                    level.getServer(), programPad, programFrequency);
            if (routed.authFailed()) {
                return refused(LaunchResult.Kind.AUTH,
                        Component.translatable("message.spacereloaded.routing.auth_failed"));
            }
            programPad = routed.destination();
        }
        autopilot = true;
        launched = true;
        fuelOutWarned = false;
        transferWarned = false;
        relaunchAtTick = 0;
        entityData.set(DATA_LAUNCHED, true);
        flight = FlightState.atRest(corePos(), stagePropellantKg(activeStage));
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 3.0f, 0.5f);
        return new LaunchResult(true, LaunchResult.Kind.OK, Component.translatable("message.spacereloaded.rocket.autopilot_started",
                Component.translatable("planet.spacereloaded." + targetId.getPath())));
    }

    /**
     * Цена следующего хопа для HUD (003, FR-104): перелёт по таблице профиля плюс
     * пропульсивная посадка при прибытии спуском (TWR активного вида при гравитации цели).
     * Без симуляции подъёма — дёшево для периодического вызова.
     */
    private double nextHopCost(ServerLevel level) {
        if (layout == null) {
            return 0;
        }
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        var hop = nextHop(level);
        if (profile.isEmpty() || hop == null) {
            return 0;
        }
        var config = org.alex_melan.spacereloaded.SpaceReloaded.config();
        double cost = profile.get().transferDeltaVTo(hop) * config.transferDeltaVScale;
        var target = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, hop);
        if (target.isPresent() && !"platform".equals(target.get().arrival()) && target.get().gravity() > 0) {
            var perf = PerformanceCalculator.calculate(currentView(), target.get().gravity());
            double touchdown = org.alex_melan.spacereloaded.core.rocketry.LandingBudget.touchdownSpeed(
                    org.alex_melan.spacereloaded.logistics.MissionPlanning.ARRIVAL_SPEED_MS,
                    target.get().gravity(), config.arrivalHeightM);
            double landing = org.alex_melan.spacereloaded.core.rocketry.LandingBudget
                    .propulsiveDeltaV(touchdown, perf.twr());
            if (Double.isFinite(landing)) {
                cost += landing;
            }
        }
        return cost;
    }

    /** Активная ступень оторвёт стек от земли и есть чем лететь (TWR > 1, Δv стека > 0). */
    private boolean canLiftOff() {
        var report = org.alex_melan.spacereloaded.core.rocketry.StagedPerformance
                .calculate(layout, stagePropellant, 9.81);
        return report.stages().get(activeStage).twr() > 1.0 && remainingDeltaV(9.81) > 0;
    }

    /**
     * Зажигание (пилот — Прыжок; стенд — напрямую): честная проверка TWR активной
     * ступени и наличия топлива.
     *
     * @return true, если борт перешёл в полёт
     */
    public boolean ignite(ServerLevel level) {
        if (launched || debris || layout == null) {
            return false;
        }
        var report = org.alex_melan.spacereloaded.core.rocketry.StagedPerformance
                .calculate(layout, stagePropellant, 9.81);
        double twr = report.stages().get(activeStage).twr();
        if (twr <= 1.0 || remainingDeltaV(9.81) <= 0) {
            if (getFirstPassenger() instanceof ServerPlayer pilot) {
                pilot.sendOverlayMessage(Component.translatable(
                        twr <= 1.0
                                ? "message.spacereloaded.rocket.warning.TWR_BELOW_ONE"
                                : "message.spacereloaded.rocket.warning.NO_USABLE_PROPELLANT"));
            }
            return false;
        }
        launched = true;
        fuelOutWarned = false;
        entityData.set(DATA_LAUNCHED, true);
        flight = FlightState.atRest(corePos(), stagePropellantKg(activeStage));
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 3.0f, 0.5f);
        return true;
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

    /**
     * T055: проверка интерпенетрации ведущих граней корпуса с миром.
     * Семплируются только блоки грани по направлению скорости (низ —
     * отдельно в {@link #touchesGround}): дёшево и ловит боковой снос
     * в скалу и взлёт в перекрытие.
     */
    private boolean hullCollides(ServerLevel level) {
        var vel = flight.vel();
        boolean px = vel.x() > 2;
        boolean nx = vel.x() < -2;
        boolean pz = vel.z() > 2;
        boolean nz = vel.z() < -2;
        boolean py = vel.y() > 2;
        if (!(px || nx || pz || nz || py)) {
            return false;
        }
        int maxX = (int) sizeX - 1;
        int maxY = (int) sizeY - 1;
        int maxZ = (int) sizeZ - 1;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (RocketData.Entry entry : rocketData.blocks()) {
            int lx = PackedPos.unpackX(entry.localPos());
            int ly = PackedPos.unpackY(entry.localPos());
            int lz = PackedPos.unpackZ(entry.localPos());
            boolean leading = (px && lx == maxX) || (nx && lx == 0)
                    || (pz && lz == maxZ) || (nz && lz == 0)
                    || (py && ly == maxY);
            if (!leading) {
                continue;
            }
            cursor.set(
                    (int) Math.floor(getX() - halfX() + lx + 0.5),
                    (int) Math.floor(getY() + ly + 0.5),
                    (int) Math.floor(getZ() - halfZ() + lz + 0.5));
            BlockState state = level.getBlockState(cursor);
            if (!state.isAir() && !state.getCollisionShape(level, cursor).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Крушение (T055): разрушения из кинетической энергии E = ½mv²
     * (масса — честная стартовая из ядра), через штатный explosion-pipeline
     * (в отличие от кратера пушки — тут именно взрыв конструкции с топливом).
     */
    private void crashInto(ServerLevel level) {
        double speed = new Vec3(flight.vel().x(), flight.vel().y(), flight.vel().z()).length();
        double massKg = PerformanceCalculator.calculate(structure, 9.81).totalMassKg();
        double energyJ = org.alex_melan.spacereloaded.core.ballistics.ImpactEnergy
                .kineticEnergyJ(massKg, speed);
        float power = (float) Math.clamp(org.alex_melan.spacereloaded.core.ballistics.ImpactEnergy
                .craterRadiusBlocks(energyJ), 2.0, 8.0);
        ejectPassengers();
        disassembleInto(level);
        level.explode(this, getX(), getY() + comY, getZ(), power, Level.ExplosionInteraction.BLOCK);
        discard();
    }

    /**
     * Касание земли: мягко — ракета остаётся собранной сущностью (парковка,
     * как в AR/GC); жёстко — разбор с взрывом. Разбор вручную: sneak+ПКМ.
     */
    private void land(ServerLevel level) {
        double impactSpeed = new Vec3(flight.vel().x(), flight.vel().y(), flight.vel().z()).length();
        boolean hasCapsule = hasReturnCapsule();
        if (impactSpeed <= (hasCapsule ? CAPSULE_CRASH_SPEED : CRASH_SPEED)) {
            launched = false;
            autopilot = false;
            descentMode = false;
            debris = false; // мягко севший обломок — обычный припаркованный аппарат (FR-067)
            entityData.set(DATA_LAUNCHED, false);
            entityData.set(DATA_PITCH, 0.0f);
            entityData.set(DATA_ROLL, 0.0f);
            flight = FlightState.atRest(corePos(), stagePropellantKg(activeStage));
            setDeltaMovement(Vec3.ZERO);
            level.playSound(null, blockPosition(), SoundEvents.IRON_DOOR_CLOSE,
                    SoundSource.NEUTRAL, 2.0f, 0.8f);
            return;
        }
        crashInto(level);
    }

    /** Разбор в блоки: остаток топлива честно возвращается в баки (US6). */
    private void disassembleInto(ServerLevel level) {
        int baseX = (int) Math.round(getX() - halfX());
        int baseY = (int) Math.round(getY());
        int baseZ = (int) Math.round(getZ() - halfZ());
        // Доля заправки — своей ступени (баки верхних ступеней могут быть полны при пустой нижней)
        double[] fraction = new double[stagePropellant.length];
        for (int i = 0; i < fraction.length; i++) {
            double capacity = layout.stage(i).propellantCapacityKg();
            fraction[i] = capacity <= 0 ? 0 : Math.clamp(stagePropellant[i] / capacity, 0, 1);
        }
        for (RocketData.Entry entry : rocketData.blocks()) {
            BlockPos target = new BlockPos(
                    baseX + PackedPos.unpackX(entry.localPos()),
                    baseY + PackedPos.unpackY(entry.localPos()),
                    baseZ + PackedPos.unpackZ(entry.localPos()));
            level.setBlock(target, entry.state(), 3);
            if (entry.capacityKg() > 0
                    && level.getBlockEntity(target) instanceof FuelTankBlockEntity tank) {
                int stage = stageOf(entry.localPos());
                double share = stage < fraction.length ? fraction[stage] : 0;
                tank.setPropellant(entry.capacityKg() * share, rocketFuelType());
            }
            if (level.getBlockEntity(target) instanceof CargoHoldBlockEntity hold) {
                for (int slot = 0; slot < hold.getContainerSize() && !cargoItems.isEmpty(); slot++) {
                    if (hold.getItem(slot).isEmpty()) {
                        hold.setItem(slot, cargoItems.remove(cargoItems.size() - 1));
                    }
                }
            }
        }
        // Отсеков не хватило (перегруз после стыковки) — честно высыпать
        for (net.minecraft.world.item.ItemStack stack : cargoItems) {
            net.minecraft.world.level.block.Block.popResource(level,
                    blockPosition(), stack);
        }
        cargoItems.clear();
    }

    /** Финальная цель (id записи планеты) по синхронизированному индексу. */
    private net.minecraft.resources.Identifier finalDestination(ServerLevel level) {
        var ids = org.alex_melan.spacereloaded.planet.Navigation.planetIds(level.registryAccess());
        return ids.isEmpty() ? null : ids.get(Math.floorMod(destinationIndex, ids.size()));
    }

    /** Ближайший хоп к финальной цели, либо null (уже на месте / нет маршрута). */
    private net.minecraft.resources.Identifier nextHop(ServerLevel level) {
        var access = level.registryAccess();
        var from = org.alex_melan.spacereloaded.planet.Navigation.entryIdFor(access, level.dimension().identifier());
        var to = finalDestination(level);
        return org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, from, to);
    }

    /**
     * Прямой выбор цели с карты полёта.
     *
     * @return false, если такой планеты нет или это планета под ногами
     */
    public boolean setDestination(ServerLevel level, net.minecraft.resources.Identifier target) {
        var access = level.registryAccess();
        var ids = org.alex_melan.spacereloaded.planet.Navigation.planetIds(access);
        int index = ids.indexOf(target);
        var here = org.alex_melan.spacereloaded.planet.Navigation
                .entryIdFor(access, level.dimension().identifier());
        if (index < 0 || target.equals(here)) {
            return false;
        }
        destinationIndex = index;
        entityData.set(DATA_DESTINATION, destinationIndex);
        return true;
    }

    /** Индекс первой цели перехода текущей планеты в общем списке планет (0, если нет). */
    private static int defaultDestinationIndex(ServerLevel level) {
        var access = level.registryAccess();
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        if (profile.isEmpty() || profile.get().transitionTargets().isEmpty()) {
            return 0;
        }
        int index = org.alex_melan.spacereloaded.planet.Navigation.planetIds(access)
                .indexOf(profile.get().transitionTargets().get(0));
        return Math.max(0, index);
    }

    /** Циклический выбор цели перехода: ЛЮБАЯ планета реестра, не только сосед. */
    private void cycleDestination(ServerLevel level, ServerPlayer pilot) {
        var access = level.registryAccess();
        var ids = org.alex_melan.spacereloaded.planet.Navigation.planetIds(access);
        var here = org.alex_melan.spacereloaded.planet.Navigation.entryIdFor(access, level.dimension().identifier());
        if (ids.size() <= 1) {
            return;
        }
        // Пропускаем планету, на которой стоим
        for (int step = 0; step < ids.size(); step++) {
            destinationIndex = (destinationIndex + 1) % ids.size();
            if (!ids.get(destinationIndex).equals(here)) {
                break;
            }
        }
        entityData.set(DATA_DESTINATION, destinationIndex);

        var target = ids.get(destinationIndex);
        var hop = org.alex_melan.spacereloaded.planet.Navigation.nextHop(access, here, target);
        Component window = Component.empty();
        if (hop != null) {
            var hopProfile = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, hop);
            if (hopProfile.isPresent()
                    && org.alex_melan.spacereloaded.planet.TransferWindows.hasWindow(hopProfile.get())) {
                boolean open = org.alex_melan.spacereloaded.planet.TransferWindows
                        .isOpen(level.getGameTime(), hopProfile.get());
                window = Component.translatable(open
                        ? "message.spacereloaded.rocket.window_open"
                        : "message.spacereloaded.rocket.window_wait");
            }
        } else {
            window = Component.translatable("message.spacereloaded.rocket.no_route");
        }
        pilot.sendOverlayMessage(Component.translatable("message.spacereloaded.rocket.destination_window",
                Component.translatable("planet.spacereloaded." + target.getPath()), window));
    }


    // ---------- Прочее ----------

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // v1: неразрушима оружием
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        if (rocketData != null) {
            RocketData persisted = new RocketData(rocketData.blocks(), totalPropellant());
            output.store("rocket", RocketData.CODEC, persisted);
            // Полёт 2.0 (FR-068): ступени переживают выгрузку и рестарт
            output.putInt("stage_active", activeStage);
            output.store("stage_propellant", com.mojang.serialization.Codec.DOUBLE.listOf(),
                    java.util.stream.DoubleStream.of(stagePropellant).boxed().toList());
        }
        output.putBoolean("debris", debris);
        output.putInt("debris_ticks", debrisTicks);
        output.putBoolean("launched", launched);
        output.putBoolean("autopilot", autopilot);
        output.putBoolean("descent_mode", descentMode);
        output.store("cargo", net.minecraft.world.item.ItemStack.CODEC.listOf(),
                cargoItems.stream().filter(stack -> !stack.isEmpty()).toList());
        if (programPad != null) {
            output.putString("program_pad_dim", programPad.dimension().identifier().toString());
            output.putLong("program_pad_pos", programPad.pos().asLong());
        }
        output.putInt("program_frequency", programFrequency);
        output.putBoolean("route_continues", routeContinues);
        output.putLong("relaunch_at", relaunchAtTick);
        output.putInt("destination", destinationIndex);
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
            // Ступени: сохранённые остатки либо (старое сохранение) — по ёмкости ступеней
            var stages = input.read("stage_propellant", com.mojang.serialization.Codec.DOUBLE.listOf());
            if (stages.isPresent() && stages.get().size() == layout.stageCount()) {
                this.stagePropellant = stages.get().stream().mapToDouble(Double::doubleValue).toArray();
            } else {
                this.stagePropellant = layout.distributeByCapacity(data.propellantKg());
            }
            this.activeStage = Math.clamp(input.getIntOr("stage_active", 0), 0, layout.stageCount() - 1);
            this.flight = new FlightState(corePos(),
                    new org.alex_melan.spacereloaded.core.geometry.Vec3d(
                            input.getDoubleOr("vel_x", 0),
                            input.getDoubleOr("vel_y", 0),
                            input.getDoubleOr("vel_z", 0)),
                    input.getDoubleOr("pitch_rad", 0),
                    input.getDoubleOr("roll_rad", 0),
                    0, 0, stagePropellant[activeStage]);
            syncStageData();
        });
        this.debris = input.getBooleanOr("debris", false);
        this.debrisTicks = input.getIntOr("debris_ticks", 0);
        this.launched = input.getBooleanOr("launched", false);
        this.autopilot = input.getBooleanOr("autopilot", false);
        this.descentMode = input.getBooleanOr("descent_mode", false);
        cargoItems.clear();
        input.read("cargo", net.minecraft.world.item.ItemStack.CODEC.listOf())
                .ifPresent(list -> list.forEach(stack -> {
                    if (!stack.isEmpty()) {
                        cargoItems.add(stack);
                    }
                }));
        String padDim = input.getStringOr("program_pad_dim", "");
        if (!padDim.isEmpty()) {
            this.programPad = net.minecraft.core.GlobalPos.of(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            net.minecraft.resources.Identifier.parse(padDim)),
                    BlockPos.of(input.getLongOr("program_pad_pos", 0L)));
        }
        this.programFrequency = input.getIntOr("program_frequency", 0);
        this.routeContinues = input.getBooleanOr("route_continues", false);
        this.relaunchAtTick = input.getLongOr("relaunch_at", 0L);
        this.destinationIndex = input.getIntOr("destination", 0);
        entityData.set(DATA_LAUNCHED, launched);
    }
}
