package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.industry.LaunchSolution;
import org.alex_melan.spacereloaded.core.industry.LaunchSolution.Reason;
import org.alex_melan.spacereloaded.core.industry.MassDriverBallistics;
import org.alex_melan.spacereloaded.core.industry.RailLayout;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModRegistries;
import org.alex_melan.spacereloaded.registry.ModSounds;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Казённик электромагнитной катапульты (004, US1, D30–D33). Слот 0 — грузовая капсула,
 * 1–26 — груз. Рельс — непрерывная линия катушек от передней грани (FACING), батарея —
 * связная группа конденсаторов, касающаяся казённика. Структура пересобирается по событиям
 * ({@link IndustryStructures}); выстрел по Sneak+ПКМ или переднему фронту редстоуна.
 */
public class MassDriverBreechBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, IndustryStructures.StructureOwner {

    public static final int SLOTS = 27;
    public static final int POD_SLOT = 0;
    /** Радиус носа капсулы для оценки нагрева (Саттон–Грейвс), м. */
    private static final double POD_NOSE_RADIUS = 0.5;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);

    // --- структура (сервер) ---
    private int[] railTiers = new int[0];
    private final List<BlockPos> capacitors = new ArrayList<>();
    private boolean dirty = true;
    private boolean lastRedstone;

    // --- цель ---
    private ResourceKey<Level> targetDimension;
    private BlockPos targetPos;

    // --- анимация (синхронизируется клиенту) ---
    private int railLength;
    private long shotTick = Long.MIN_VALUE / 2;
    private long sledUntil;
    private int chargeByte;
    private long lastChargeSync;

    public MassDriverBreechBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MASS_DRIVER_BREECH, pos, state);
    }

    public Direction facing() {
        return getBlockState().getValue(MassDriverBreechBlock.FACING);
    }

    @Override
    public void markStructureDirty() {
        dirty = true;
    }

    // ---------------- тик ----------------

    public static void serverTick(MassDriverBreechBlockEntity breech, ServerLevel level) {
        if (breech.dirty) {
            breech.dirty = false;
            breech.rescan(level);
        }
        if (level.getGameTime() - breech.lastChargeSync >= 10) {
            breech.lastChargeSync = level.getGameTime();
            int charge = breech.chargeByteNow(level);
            if (Math.abs(charge - breech.chargeByte) >= 8) {
                breech.chargeByte = charge;
                breech.syncToClient();
            }
        }
    }

    /** Пересборка рельса и батареи (по событию). */
    private void rescan(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        Direction dir = facing();
        int max = config.massDriverMaxSections;
        int previous = railTiers.length;
        int[] cells = new int[max + 1];
        for (int i = 0; i <= max; i++) {
            BlockPos cell = getBlockPos().relative(dir, i + 1);
            BlockState state = level.getBlockState(cell);
            if (!(state.getBlock() instanceof CoilBlock coil)) {
                break;
            }
            // Секция чужого рельса (IN_RAIL, но не наша) — обрыв: первый захвативший владеет
            if (state.getValue(CoilBlock.IN_RAIL) && i >= previous) {
                break;
            }
            cells[i] = coil.tier();
        }
        RailLayout rail = RailLayout.fromCells(cells, max);
        railTiers = rail.tiers();
        Direction.Axis axis = dir.getAxis();
        for (int i = 0; i < Math.max(previous, railTiers.length); i++) {
            BlockPos cell = getBlockPos().relative(dir, i + 1);
            BlockState state = level.getBlockState(cell);
            if (!(state.getBlock() instanceof CoilBlock)) {
                continue;
            }
            boolean inRail = i < railTiers.length;
            BlockState wanted = state.setValue(CoilBlock.IN_RAIL, inRail).setValue(CoilBlock.AXIS, axis);
            if (wanted != state) {
                level.setBlock(cell, wanted, Block.UPDATE_CLIENTS);
            }
        }
        scanCapacitors(level);

        Set<BlockPos> claimed = new HashSet<>();
        claimed.add(getBlockPos());
        for (Direction d : Direction.values()) {
            claimed.add(getBlockPos().relative(d));
        }
        for (int i = 0; i <= railTiers.length; i++) {
            claimed.add(getBlockPos().relative(dir, i + 1));
        }
        for (BlockPos cap : capacitors) {
            claimed.add(cap);
            for (Direction d : Direction.values()) {
                claimed.add(cap.relative(d));
            }
        }
        IndustryStructures.claim(level, getBlockPos(), claimed);
        if (railLength != railTiers.length) {
            railLength = railTiers.length;
            syncToClient();
        }
        setChanged();
    }

    /** Связная по граням группа конденсаторов, касающаяся казённика (≤ предела). */
    private void scanCapacitors(ServerLevel level) {
        capacitors.clear();
        int limit = SpaceReloaded.config().capacitorMaxBlocks;
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (Direction d : Direction.values()) {
            BlockPos n = getBlockPos().relative(d);
            if (level.getBlockState(n).is(ModBlocks.CAPACITOR) && seen.add(n)) {
                queue.add(n);
            }
        }
        while (!queue.isEmpty() && capacitors.size() < limit) {
            BlockPos cur = queue.poll();
            capacitors.add(cur);
            for (Direction d : Direction.values()) {
                BlockPos n = cur.relative(d);
                if (level.getBlockState(n).is(ModBlocks.CAPACITOR) && seen.add(n)) {
                    queue.add(n);
                }
            }
        }
    }

    public int railLength() {
        return railTiers.length;
    }

    public int capacitorCount() {
        return capacitors.size();
    }

    public long storedEnergy(ServerLevel level) {
        long sum = 0;
        for (BlockPos cap : capacitors) {
            if (level.getBlockEntity(cap) instanceof CapacitorBlockEntity capacitor) {
                sum += capacitor.stored();
            }
        }
        return sum;
    }

    // ---------------- решение ----------------

    /**
     * Масса капсулы с грузом: сухая + груз по таблице масс предметов (датапак
     * {@code spacereloaded:item_mass}); {@code podKgPerItem} — масштаб таблицы (1.0 — как есть).
     */
    public double podMassKg() {
        SpaceReloadedConfig config = SpaceReloaded.config();
        double cargo = 0;
        if (level != null) {
            for (int slot = 1; slot < SLOTS; slot++) {
                cargo += org.alex_melan.spacereloaded.registry.ItemMasses.massOfStack(level.registryAccess(),
                        items.get(slot));
            }
        }
        return config.podDryMassKg + cargo * config.podKgPerItem;
    }

    /** Полное решение выстрела с первой причиной неготовности (порядок FR-207). */
    public LaunchSolution solve(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        RailLayout rail = new RailLayout(railTiers, false);
        double[] accel = rail.accelerations(config.coilTier1AccelG, config.coilTier2AccelG);
        double vMax = MassDriverBallistics.muzzleVelocity(accel, config.massDriverSectionLengthM);
        double mass = podMassKg();
        Optional<ModRegistries.PlanetProfile> body = IndustryStructures.bodyFor(level);
        if (rail.length() == 0) {
            return new LaunchSolution(vMax, 0, mass, 0, 0, 0, 0, Reason.NO_RAIL, 0);
        }
        if (body.isEmpty() || body.get().bodyRadius() <= 0) {
            return new LaunchSolution(vMax, 0, mass, 0, 0, 0, 0, Reason.NO_RADIUS, 0);
        }
        ModRegistries.PlanetProfile profile = body.get();
        if (targetDimension == null || targetPos == null) {
            return new LaunchSolution(vMax, 0, mass, 0, 0, 0, 0, Reason.NO_TARGET, 0);
        }
        if (!profile.transfer().deltaV().containsKey(targetDimension.identifier())) {
            return new LaunchSolution(vMax, 0, mass, 0, 0, 0, 0, Reason.UNREACHABLE_TARGET, 0);
        }
        double tableDv = profile.transferDeltaVTo(targetDimension.identifier()) * config.transferDeltaVScale;
        double vReq = MassDriverBallistics.requiredVelocity(profile.gravity(), profile.bodyRadius(),
                profile.parkingAltitude(), tableDv);
        double energyJ = MassDriverBallistics.shotEnergyJ(mass, vReq, config.massDriverEfficiency);
        long units = (long) Math.ceil(energyJ / config.massDriverJoulesPerEnergy);
        double density = profile.aero() == null ? 0 : profile.aero().density();
        double q = MassDriverBallistics.dynamicPressure(density, vReq);
        double heat = MassDriverBallistics.heatFlux(density, vReq, POD_NOSE_RADIUS);
        double bestAccel = (rail.bestTier() == 2 ? config.coilTier2AccelG : config.coilTier1AccelG)
                * MassDriverBallistics.G0;
        int missing = MassDriverBallistics.missingSections(vReq, vMax, bestAccel, config.massDriverSectionLengthM);
        LaunchSolution solution = new LaunchSolution(vMax, vReq, mass, energyJ, units, q, heat, Reason.OK, missing);
        if (q > config.podMaxDynamicPressurePa) {
            return solution.withReason(Reason.ATMOSPHERE_PRESSURE);
        }
        if (heat > config.podMaxHeatFluxWm2) {
            return solution.withReason(Reason.ATMOSPHERE_HEAT);
        }
        if (vMax < vReq) {
            return solution.withReason(Reason.RAIL_SHORT);
        }
        if (!items.get(POD_SLOT).is(ModItems.CARGO_POD)) {
            return solution.withReason(Reason.NO_POD);
        }
        if (level.getGameTime() < sledUntil) {
            return solution.withReason(Reason.RECHARGING);
        }
        Direction dir = facing();
        int end = railTiers.length + config.massDriverClearance;
        for (int i = 1; i <= end; i++) {
            if (!level.isLoaded(getBlockPos().relative(dir, i))) {
                return solution.withReason(Reason.RAIL_UNLOADED);
            }
        }
        for (int i = railTiers.length + 1; i <= end; i++) {
            if (!level.getBlockState(getBlockPos().relative(dir, i)).isAir()) {
                return solution.withReason(Reason.MUZZLE_BLOCKED);
            }
        }
        if (storedEnergy(level) < units) {
            return solution.withReason(Reason.NO_ENERGY);
        }
        return solution;
    }

    // ---------------- выстрел ----------------

    /** Выстрел (сервер). {@code player} — стрелок (для сообщений/достижений), может быть null. */
    public LaunchSolution tryFire(ServerLevel level, ServerPlayer player) {
        LaunchSolution solution = solve(level);
        if (!solution.ready()) {
            return solution;
        }
        // Разряд батареи: снимаем по очереди до набора энергии выстрела
        long need = solution.energyUnits();
        for (BlockPos cap : capacitors) {
            if (need <= 0) {
                break;
            }
            if (level.getBlockEntity(cap) instanceof CapacitorBlockEntity capacitor) {
                need -= capacitor.discharge(need);
            }
        }
        ItemStack pod = items.get(POD_SLOT).split(1);
        List<ItemStack> cargo = new ArrayList<>();
        for (int slot = 1; slot < SLOTS; slot++) {
            if (!items.get(slot).isEmpty()) {
                cargo.add(items.get(slot).copy());
                items.set(slot, ItemStack.EMPTY);
            }
        }
        long now = level.getGameTime();
        PodTransitState.get(level.getServer()).enqueue(new PodTransitState.PodTransit(
                java.util.UUID.randomUUID(), pod, cargo, targetDimension, targetPos,
                now + SpaceReloaded.config().podTransitTicks, level.getRandom().nextLong(),
                Optional.ofNullable(player).map(Player::getUUID), false));

        Direction dir = facing();
        BlockPos muzzle = getBlockPos().relative(dir, railTiers.length + 1);
        CargoPodEntity.launch(level, Vec3.atCenterOf(muzzle), dir);
        level.playSound(null, getBlockPos(), ModSounds.MASS_DRIVER_FIRE, SoundSource.BLOCKS, 4.0f, 1.0f);
        level.playSound(null, muzzle, ModSounds.MASS_DRIVER_FIRE, SoundSource.BLOCKS, 4.0f, 1.2f);
        level.playSound(null, getBlockPos(), ModSounds.MASS_DRIVER_SLED, SoundSource.BLOCKS, 1.0f, 1.0f);
        shotTick = now;
        sledUntil = now + SpaceReloaded.config().massDriverSledReturnTicks;
        chargeByte = chargeByteNow(level);
        setChanged();
        syncToClient();
        if (player != null) {
            IndustryAdvancements.award(player, IndustryAdvancements.MASS_DRIVER);
        } else {
            IndustryAdvancements.awardNearby(level, getBlockPos(), 16, IndustryAdvancements.MASS_DRIVER);
        }
        return solution;
    }

    /** Редстоун: выстрел по переднему фронту. */
    public void onRedstone(ServerLevel level, boolean powered) {
        if (powered && !lastRedstone) {
            LaunchSolution result = tryFire(level, null);
            if (!result.ready()) {
                SpaceReloaded.LOGGER.debug("Катапульта {}: отказ по редстоуну {}", getBlockPos(), result.reason());
            }
        }
        lastRedstone = powered;
    }

    /** Компаратор: заряд батареи относительно энергии выстрела, 0–15. */
    public int comparatorSignal(ServerLevel level) {
        LaunchSolution solution = solve(level);
        if (solution.energyUnits() <= 0) {
            return 0;
        }
        return (int) Math.min(15, 15 * storedEnergy(level) / solution.energyUnits());
    }

    private int chargeByteNow(ServerLevel level) {
        LaunchSolution solution = solve(level);
        long need = Math.max(1, solution.energyUnits());
        return (int) Math.min(255, 255 * storedEnergy(level) / need);
    }

    // ---------------- цель ----------------

    /** ПКМ программой, привязанной к ловушке: копирует цель. */
    public Component setTargetFromProgram(ItemStack program) {
        GlobalPos catcher = program.get(org.alex_melan.spacereloaded.registry.ModDataComponents.PROGRAM_CATCHER);
        if (catcher == null) {
            return Component.translatable("message.spacereloaded.mass_driver.program_not_catcher");
        }
        targetDimension = catcher.dimension();
        targetPos = catcher.pos().immutable();
        dirty = true;
        setChanged();
        return Component.translatable("message.spacereloaded.mass_driver.target_set",
                targetPos.toShortString(), targetDimension.identifier().toString());
    }

    // ---------------- отчёт ----------------

    public List<Component> report(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        LaunchSolution s = solve(level);
        List<Component> lines = new ArrayList<>();
        RailLayout rail = new RailLayout(railTiers, false);
        lines.add(Component.translatable("message.spacereloaded.mass_driver.rail",
                rail.length(), rail.count(1), rail.count(2), fmt(s.vMax())));
        if (s.vRequired() > 0) {
            lines.add(Component.translatable("message.spacereloaded.mass_driver.required", fmt(s.vRequired()),
                    fmt(s.massKg()), fmt(s.energyJ() / 1.0e6), s.energyUnits()));
        }
        lines.add(Component.translatable("message.spacereloaded.mass_driver.bank", capacitors.size(),
                storedEnergy(level), (long) capacitors.size() * config.capacitorCapacity));
        if (targetPos != null) {
            lines.add(Component.translatable("message.spacereloaded.mass_driver.target",
                    targetPos.toShortString(), targetDimension.identifier().toString()));
        }
        String key = "message.spacereloaded.mass_driver.reason." + s.reason().name().toLowerCase(Locale.ROOT);
        lines.add(switch (s.reason()) {
            case RAIL_SHORT -> Component.translatable(key, fmt(s.vMax()), fmt(s.vRequired()), s.missingSections());
            case ATMOSPHERE_PRESSURE -> Component.translatable(key, fmt(s.dynamicPressurePa() / 1000),
                    fmt(config.podMaxDynamicPressurePa / 1000));
            case ATMOSPHERE_HEAT -> Component.translatable(key, fmt(s.heatFluxWm2() / 1.0e6),
                    fmt(config.podMaxHeatFluxWm2 / 1.0e6));
            case NO_ENERGY -> Component.translatable(key, s.energyUnits(), storedEnergy(level));
            case RECHARGING -> Component.translatable(key, Math.max(0, (sledUntil - level.getGameTime()) / 20));
            default -> Component.translatable(key);
        });
        return lines;
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, value >= 100 ? "%.0f" : "%.1f", value);
    }

    // ---------------- клиент / анимация ----------------

    public int clientRailLength() {
        return railLength;
    }

    public long shotTick() {
        return shotTick;
    }

    public long sledUntil() {
        return sledUntil;
    }

    public float chargeFraction() {
        return chargeByte / 255.0f;
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    // ---------------- жизненный цикл ----------------

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            IndustryStructures.release(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            // Снимаем визуал рельса и высыпаем содержимое
            Direction dir = state.getValue(MassDriverBreechBlock.FACING);
            for (int i = 0; i < railTiers.length; i++) {
                BlockPos cell = pos.relative(dir, i + 1);
                BlockState coil = serverLevel.getBlockState(cell);
                if (coil.getBlock() instanceof CoilBlock && coil.getValue(CoilBlock.IN_RAIL)) {
                    serverLevel.setBlock(cell, coil.setValue(CoilBlock.IN_RAIL, false), Block.UPDATE_CLIENTS);
                }
            }
            IndustryStructures.release(serverLevel, pos);
        }
        super.preRemoveSideEffects(pos, state);
    }

    // ---------------- контейнер ----------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.mass_driver_breech");
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return ChestMenu.threeRows(containerId, inventory, this);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot != POD_SLOT || stack.is(ModItems.CARGO_POD);
    }

    private static final int[] ALL = java.util.stream.IntStream.range(0, SLOTS).toArray();

    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return true;
    }

    // ---------------- NBT ----------------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putIntArray("rail", railTiers);
        output.putInt("rail_length", railLength);
        output.putLong("shot_tick", shotTick);
        output.putLong("sled_until", sledUntil);
        output.putInt("charge", chargeByte);
        output.putBoolean("redstone", lastRedstone);
        if (targetDimension != null && targetPos != null) {
            output.putString("target_dim", targetDimension.identifier().toString());
            output.putLong("target_pos", targetPos.asLong());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        railTiers = input.getIntArray("rail").orElse(new int[0]);
        railLength = input.getIntOr("rail_length", railTiers.length);
        shotTick = input.getLongOr("shot_tick", Long.MIN_VALUE / 2);
        sledUntil = input.getLongOr("sled_until", 0L);
        chargeByte = input.getIntOr("charge", 0);
        lastRedstone = input.getBooleanOr("redstone", false);
        Optional<String> dim = input.getString("target_dim");
        if (dim.isPresent()) {
            targetDimension = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,
                    net.minecraft.resources.Identifier.parse(dim.get()));
            targetPos = BlockPos.of(input.getLongOr("target_pos", 0L));
        }
        dirty = true;
    }
}
