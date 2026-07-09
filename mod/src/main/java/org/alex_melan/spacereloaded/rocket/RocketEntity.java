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

    private static final double DT = 0.05; // серверный тик
    private static final double CRASH_SPEED = 15.0; // м/с — жёсткая посадка
    private static final double CAPSULE_CRASH_SPEED = 25.0; // капсула: теплозащита+амортизация

    private RocketData rocketData;
    private RocketStructure structure;
    private FlightState flight;
    private boolean launched;
    private boolean prevSprint;
    private boolean fuelOutWarned;
    private boolean windowWarned;
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
    }

    /** Сервер: установить структуру после сборки (до addFreshEntity). */
    public void setAssembly(RocketData data) {
        this.rocketData = data;
        rebuildDerived();
        this.flight = FlightState.atRest(corePos(), data.propellantKg());
        entityData.set(DATA_FUEL, (float) data.propellantKg());
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

    /** Снимок структуры с ТЕКУЩИМ топливом (для стыковочных операций). */
    public RocketData rocketDataForDocking() {
        return new RocketData(rocketData.blocks(), flight == null ? 0 : flight.propellantKg());
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

    /** Текущий запас топлива, кг. */
    public double propellantKg() {
        return flight == null ? 0 : flight.propellantKg();
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

    /** Заправка (рукав): принять до amountKg, вернуть фактически принятое. */
    public double refuel(double amountKg) {
        if (launched || structure == null || flight == null) {
            return 0;
        }
        double capacity = structure.totalPropellantCapacityKg();
        double accepted = Math.clamp(amountKg, 0, Math.max(0, capacity - flight.propellantKg()));
        if (accepted > 0) {
            flight = new FlightState(flight.pos(), flight.vel(), flight.pitch(), flight.roll(),
                    flight.pitchRate(), flight.rollRate(), flight.propellantKg() + accepted);
            entityData.set(DATA_FUEL, (float) flight.propellantKg());
            fuelOutWarned = false;
        }
        return accepted;
    }

    /** Слив (рукав): отдать до amountKg. */
    public double drain(double amountKg) {
        if (launched || flight == null) {
            return 0;
        }
        double drained = Math.clamp(amountKg, 0, flight.propellantKg());
        if (drained > 0) {
            flight = new FlightState(flight.pos(), flight.vel(), flight.pitch(), flight.roll(),
                    flight.pitchRate(), flight.rollRate(), flight.propellantKg() - drained);
            entityData.set(DATA_FUEL, (float) flight.propellantKg());
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
                serverPlayer.sendSystemMessage(startAutopilot((ServerLevel) level()));
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
        if (level().isClientSide() || rocketData == null) {
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
                tryIgnite(serverLevel);
            }
            return;
        }
        prevSprint = sprint;

        ControlInput control = new ControlInput(jump ? 1.0 : 0.0, 0, 0, true);
        flight = new FlightState(corePos(), flight.vel(), flight.pitch(), flight.roll(),
                flight.pitchRate(), flight.rollRate(), flight.propellantKg());
        double gravity = org.alex_melan.spacereloaded.planet.PlanetManager.gravity(serverLevel);
        flight = FlightIntegrator.step(structure, flight, control,
                new FlightEnvironment(gravity), DT);

        setPos(flight.pos().x(), flight.pos().y(), flight.pos().z());
        setDeltaMovement(flight.vel().x() * DT, flight.vel().y() * DT, flight.vel().z() * DT);
        entityData.set(DATA_PITCH, (float) Math.toDegrees(flight.pitch()));
        entityData.set(DATA_ROLL, (float) Math.toDegrees(flight.roll()));
        if (Math.abs(entityData.get(DATA_FUEL) - flight.propellantKg()) > 1.0) {
            entityData.set(DATA_FUEL, (float) flight.propellantKg());
        }

        // T056: полётные ticket'ы (persist + keep-dimension-active) — полёт
        // завершится и без игрока рядом, и после перезапуска сервера
        if (tickCount % 20 == 1) {
            org.alex_melan.spacereloaded.planet.ModTickets.holdStrike(serverLevel, blockPosition(), 1);
        }

        double speedNow = new Vec3(flight.vel().x(), flight.vel().y(), flight.vel().z()).length();
        // T062: топливо кончилось — дальше только честная баллистика
        if (flight.propellantKg() <= 0.5 && !fuelOutWarned) {
            fuelOutWarned = true;
            if (pilot != null) {
                pilot.sendOverlayMessage(Component.translatable("message.spacereloaded.rocket.fuel_out"));
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
            if (transferWindowOpen(serverLevel, profile.get(), pilot)) {
                transition(serverLevel, profile.get());
            }
        } else if (getY() < (profile.map(pp -> pp.transitionAltitude()).orElse(Integer.MAX_VALUE) - 20)) {
            windowWarned = false; // спустились — предупреждение об окне снова актуально
        }
    }

    /**
     * Окно Гомана к выбранной цели (Phase 11). Луна/Земля всегда открыты;
     * Марс — только в окне запуска, иначе перелёт не начинается, пилот
     * получает предупреждение со сроком до окна.
     */
    private boolean transferWindowOpen(ServerLevel level,
            org.alex_melan.spacereloaded.registry.ModRegistries.PlanetProfile fromProfile,
            ServerPlayer pilot) {
        var targets = fromProfile.transitionTargets();
        var targetId = targets.get(Math.floorMod(destinationIndex, targets.size()));
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
    private void transition(ServerLevel from, org.alex_melan.spacereloaded.registry.ModRegistries.PlanetProfile fromProfile) {
        var targets = fromProfile.transitionTargets();
        var targetId = targets.get(Math.floorMod(destinationIndex, targets.size()));
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
            targetY = programPad.pos().getY() + 180.0;
        } else {
            targetY = Math.max(180.0, targetProfile.get().transitionAltitude() - 20.0);
        }

        org.alex_melan.spacereloaded.planet.ModTickets.holdAround(from, blockPosition(), 2);
        org.alex_melan.spacereloaded.planet.ModTickets.holdAround(target,
                BlockPos.containing(targetX, targetY, targetZ), 2);

        ServerPlayer pilot = getFirstPassenger() instanceof ServerPlayer sp ? sp : null;
        ejectPassengers();

        double savedPropellant = flight.propellantKg();
        Entity moved = teleport(new net.minecraft.world.level.portal.TeleportTransition(
                target, new Vec3(targetX, targetY, targetZ), Vec3.ZERO, 0f, 0f,
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));

        if (moved instanceof RocketEntity rocket) {
            rocket.postArrival(toOrbit, savedPropellant);
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
    private void postArrival(boolean parked, double propellantKg) {
        this.flight = new FlightState(corePos(),
                parked ? org.alex_melan.spacereloaded.core.geometry.Vec3d.ZERO
                       : new org.alex_melan.spacereloaded.core.geometry.Vec3d(0, -5, 0),
                0, 0, 0, 0, propellantKg);
        this.launched = !parked;
        if (parked) {
            autopilot = false;
            descentMode = false;
        } else if (autopilot) {
            descentMode = true; // беспилотная посадка к маяку
        }
        entityData.set(DATA_LAUNCHED, launched);
        entityData.set(DATA_PITCH, 0.0f);
        entityData.set(DATA_ROLL, 0.0f);
        setDeltaMovement(Vec3.ZERO);
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
        if (destination != null) {
            var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
            int index = -1;
            if (profile.isPresent()) {
                var targets = profile.get().transitionTargets();
                for (int i = 0; i < targets.size(); i++) {
                    var target = org.alex_melan.spacereloaded.planet.PlanetManager
                            .profileById(level, targets.get(i));
                    if (target.isPresent() && target.get().dimension().equals(destination)) {
                        index = i;
                        break;
                    }
                }
            }
            if (index < 0) {
                return Component.translatable("message.spacereloaded.program.unreachable",
                        destination.toString());
            }
            destinationIndex = index;
            entityData.set(DATA_DESTINATION, destinationIndex);
        }
        this.programPad = pad;
        this.programFrequency = program.getOrDefault(
                org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_FREQUENCY, 0);
        return Component.translatable("message.spacereloaded.program.installed",
                FlightProgramItem.describe(program));
    }

    /**
     * Беспилотный старт (T-спутник): полная тяга до высоты перехода. Цели
     * с прибытием «снижение» разрешены при заданном посадочном маяке —
     * автопилот выполнит suicide-burn-lite над ним.
     */
    private Component startAutopilot(ServerLevel level) {
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        if (profile.isEmpty() || profile.get().transitionTargets().isEmpty()) {
            return Component.translatable("message.spacereloaded.rocket.autopilot_no_target");
        }
        var targetId = profile.get().transitionTargets()
                .get(Math.floorMod(destinationIndex, profile.get().transitionTargets().size()));
        var target = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, targetId);
        boolean descendTarget = target.isPresent() && !"platform".equals(target.get().arrival());
        if (target.isEmpty() || (descendTarget && programPad == null)) {
            return Component.translatable("message.spacereloaded.rocket.autopilot_only_orbit");
        }
        RocketPerformance performance = PerformanceCalculator.calculate(structure, 9.81);
        if (performance.twr() <= 1.0 || performance.deltaV() <= 0) {
            return Component.translatable("message.spacereloaded.rocket.warning.TWR_BELOW_ONE");
        }
        // Защищённая маршрутизация: разрешаем адрес доставки (аутентификация/перехват)
        if (programPad != null) {
            var routed = org.alex_melan.spacereloaded.network.SecureRouting.resolve(
                    level.getServer(), programPad, programFrequency);
            if (routed.authFailed()) {
                return Component.translatable("message.spacereloaded.routing.auth_failed");
            }
            programPad = routed.destination();
        }
        autopilot = true;
        launched = true;
        entityData.set(DATA_LAUNCHED, true);
        flight = FlightState.atRest(corePos(), flight.propellantKg());
        level.playSound(null, blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.NEUTRAL, 3.0f, 0.5f);
        return Component.translatable("message.spacereloaded.rocket.autopilot_started",
                Component.translatable("planet.spacereloaded." + targetId.getPath()));
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
        boolean hasCapsule = rocketData.blocks().stream().anyMatch(e ->
                e.state().is(org.alex_melan.spacereloaded.registry.ModBlocks.RETURN_CAPSULE));
        if (impactSpeed <= (hasCapsule ? CAPSULE_CRASH_SPEED : CRASH_SPEED)) {
            launched = false;
            autopilot = false;
            descentMode = false;
            entityData.set(DATA_LAUNCHED, false);
            entityData.set(DATA_PITCH, 0.0f);
            entityData.set(DATA_ROLL, 0.0f);
            flight = FlightState.atRest(corePos(), flight.propellantKg());
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
        double totalCapacity = 0;
        for (RocketData.Entry entry : rocketData.blocks()) {
            totalCapacity += entry.capacityKg();
        }
        double fraction = totalCapacity <= 0 ? 0
                : Math.clamp(flight.propellantKg() / totalCapacity, 0, 1);
        for (RocketData.Entry entry : rocketData.blocks()) {
            BlockPos target = new BlockPos(
                    baseX + PackedPos.unpackX(entry.localPos()),
                    baseY + PackedPos.unpackY(entry.localPos()),
                    baseZ + PackedPos.unpackZ(entry.localPos()));
            level.setBlock(target, entry.state(), 3);
            if (entry.capacityKg() > 0
                    && level.getBlockEntity(target) instanceof FuelTankBlockEntity tank) {
                tank.setPropellant(entry.capacityKg() * fraction, rocketFuelType());
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

    /** Циклический выбор цели перехода (список из профиля планеты). */
    private void cycleDestination(ServerLevel level, ServerPlayer pilot) {
        var profile = org.alex_melan.spacereloaded.planet.PlanetManager.profileFor(level);
        if (profile.isEmpty() || profile.get().transitionTargets().size() <= 1) {
            return;
        }
        destinationIndex = (destinationIndex + 1) % profile.get().transitionTargets().size();
        entityData.set(DATA_DESTINATION, destinationIndex);
        var target = profile.get().transitionTargets().get(destinationIndex);
        var targetProfile = org.alex_melan.spacereloaded.planet.PlanetManager.profileById(level, target);
        Component window = Component.empty();
        if (targetProfile.isPresent()
                && org.alex_melan.spacereloaded.planet.TransferWindows.hasWindow(targetProfile.get())) {
            boolean open = org.alex_melan.spacereloaded.planet.TransferWindows
                    .isOpen(level.getGameTime(), targetProfile.get());
            window = Component.translatable(open
                    ? "message.spacereloaded.rocket.window_open"
                    : "message.spacereloaded.rocket.window_wait");
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
            RocketData persisted = new RocketData(rocketData.blocks(), flight.propellantKg());
            output.store("rocket", RocketData.CODEC, persisted);
        }
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
        this.destinationIndex = input.getIntOr("destination", 0);
        entityData.set(DATA_LAUNCHED, launched);
    }
}
