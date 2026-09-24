package org.alex_melan.spacereloaded.industry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.industry.ReactorShell;
import org.alex_melan.spacereloaded.core.industry.RegolithYield;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModBlocks;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModSounds;
import org.alex_melan.spacereloaded.registry.ModTags;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.List;

/**
 * Реголитовый реактор — электролиз расплава реголита (MRE, 004, US3, D36). Контроллер —
 * центр боковой грани оболочки 3×3×3 из огнеупорной футеровки (FACING наружу), центр куба —
 * воздух. Слоты: 0 — сырьё, 1 — баллон, 2 — железная пыль, 3 — титановая пыль, 4 — шлак.
 * Кислород — в баллон (уменьшение износа = зарядка, как у электролизёра), остаток — в буфер.
 *
 * <p>Электролиз других расплавов (006, процессные рецепты {@code machine = regolith_reactor}):
 * глинозём — два честных пути: прямой электролиз оксидного расплава на инертном аноде (MOE,
 * ≈ 20 кВт·ч/кг, выделяется O₂ — лунный путь) или Холл–Эру: глинозём растворён в криолитовой
 * ванне, анод угольный и сгорает (2Al₂O₃ + 3C → 4Al + 3CO₂, 0.45 кг C на кг Al, ≈ 14 кВт·ч/кг —
 * энергия ×0.7, кислорода нет). Криолит и уголь, положенные во вход, уходят в ванну и анод;
 * потери фторидов ~2 % массы алюминия — одной порции криолита хватает на 50 кг. анортозит лунных нагорий (Al + Si + O₂ + шлак CaO),
 * хлорид лития (2LiCl → 2Li + Cl₂; хлор уходит в скруббер). Продукты — в слоты 2–4.
 */
public class RegolithReactorBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, IndustryStructures.StructureOwner {

    public static final int SLOT_INPUT = 0;
    public static final int SLOT_CANISTER = 1;
    public static final int SLOT_IRON = 2;
    public static final int SLOT_TITANIUM = 3;
    public static final int SLOT_SLAG = 4;
    public static final int SLOTS = 5;

    private static final long ENERGY_CAPACITY = 20_000L;
    private static final long ENERGY_MAX_INSERT = 200L;

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private final SimpleEnergyStorage energy = new SimpleEnergyStorage(ENERGY_CAPACITY, ENERGY_MAX_INSERT, 0);
    private final org.alex_melan.spacereloaded.machine.ChemicalProcess melt =
            new org.alex_melan.spacereloaded.machine.ChemicalProcess(
                    org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe.REGOLITH_REACTOR);
    private static final int[] MELT_OUT = {SLOT_IRON, SLOT_TITANIUM, SLOT_SLAG};
    /** Криолитовая ванна (кг алюминия, на который её хватит) и угольный анод (кг углерода). */
    private double bathCapacity;
    private double anodeCarbon;
    public static final double ALUMINIUM_PER_CRYOLITE = 50;
    public static final double CARBON_PER_ALUMINIUM = 0.45;
    public static final double HALL_HEROULT_ENERGY = 0.7;

    private static boolean anodeCarbon(ItemStack stack) {
        return stack.is(net.minecraft.world.item.Items.COAL) || stack.is(net.minecraft.world.item.Items.CHARCOAL)
                || stack.is(ModItems.COAL_DUST);
    }

    /** Криолит и уголь во входе — в ванну и анод. */
    private void absorbBathAndAnode() {
        ItemStack input = items.get(SLOT_INPUT);
        if (input.is(ModItems.CRYOLITE)) {
            bathCapacity += ALUMINIUM_PER_CRYOLITE * input.getCount();
            items.set(SLOT_INPUT, ItemStack.EMPTY);
            setChanged();
        } else if (anodeCarbon(input)) {
            anodeCarbon += input.getCount();
            items.set(SLOT_INPUT, ItemStack.EMPTY);
            setChanged();
        }
    }

    /** Режим Холла–Эру: ванна есть и анода хватит на порцию. */
    private boolean hallHeroult() {
        return bathCapacity >= 1 && anodeCarbon >= CARBON_PER_ALUMINIUM && items.get(SLOT_INPUT).is(ModItems.ALUMINA);
    }
    private int progress;
    private int oxygenBuffer;
    private boolean formed;
    private int badIndex = -1;
    private boolean dirty = true;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> melt.working() ? melt.maxProgress() : SpaceReloaded.config().reactorCycleTicks;
                case 2 -> (int) energy.amount;
                case 3 -> (int) energy.capacity;
                case 4 -> oxygenBuffer;
                case 5 -> SpaceReloaded.config().reactorOxygenBuffer;
                case 6 -> formed ? 1 : 0;
                case 7 -> badIndex;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 8;
        }
    };

    public RegolithReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REGOLITH_REACTOR, pos, state);
    }

    public EnergyStorage energyStorage() {
        return energy;
    }

    @Override
    public void markStructureDirty() {
        dirty = true;
    }

    public boolean formed() {
        return formed;
    }

    public int oxygenBuffer() {
        return oxygenBuffer;
    }

    /** Криолитовая ванна, кг алюминия (006). */
    public double bathCapacity() {
        return bathCapacity;
    }

    /** Угольный анод, кг (006). */
    public double anodeCarbon() {
        return anodeCarbon;
    }

    /** Центр куба — за контроллером (FACING смотрит наружу). */
    private BlockPos cubeCenter() {
        return getBlockPos().relative(getBlockState().getValue(RegolithReactorBlock.FACING).getOpposite());
    }

    private void rescan(ServerLevel level) {
        BlockPos origin = cubeCenter().offset(-1, -1, -1);
        ReactorShell.Cell[] cells = new ReactorShell.Cell[27];
        List<BlockPos> claimed = new ArrayList<>(27);
        int controllerIndex = -1;
        for (int y = 0; y < 3; y++) {
            for (int z = 0; z < 3; z++) {
                for (int x = 0; x < 3; x++) {
                    BlockPos cell = origin.offset(x, y, z);
                    claimed.add(cell);
                    int index = ReactorShell.index(x, y, z);
                    BlockState state = level.getBlockState(cell);
                    if (cell.equals(getBlockPos())) {
                        cells[index] = ReactorShell.Cell.CONTROLLER;
                        controllerIndex = index;
                    } else if (state.is(ModBlocks.REFRACTORY_LINING)) {
                        cells[index] = ReactorShell.Cell.REFRACTORY;
                    } else if (state.isAir()) {
                        cells[index] = ReactorShell.Cell.AIR;
                    } else {
                        cells[index] = ReactorShell.Cell.OTHER;
                    }
                }
            }
        }
        ReactorShell.Result result = ReactorShell.validate(cells, controllerIndex);
        boolean wasFormed = formed;
        formed = result.formed();
        badIndex = result.badIndex();
        if (!formed && wasFormed) {
            progress = 0; // разборка во время работы: цикл сброшен, сырьё не списывалось
        }
        IndustryStructures.claim(level, getBlockPos(), claimed);
        org.alex_melan.spacereloaded.multiblock.FormableBlock.apply(level, claimed, formed);
        setChanged();
    }

    /** Смещение ошибочной клетки для отчёта (или null). */
    public BlockPos badCell() {
        if (badIndex < 0) {
            return null;
        }
        int x = badIndex % 3;
        int z = (badIndex / 3) % 3;
        int y = badIndex / 9;
        return cubeCenter().offset(x - 1, y - 1, z - 1);
    }

    public static void serverTick(RegolithReactorBlockEntity reactor, ServerLevel level) {
        if (reactor.dirty) {
            reactor.dirty = false;
            reactor.rescan(level);
        }
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, reactor.getBlockPos());
        }
        reactor.drainBufferToCanister();
        boolean running = reactor.work(level);
        BlockState state = reactor.getBlockState();
        if (state.getValue(RegolithReactorBlock.LIT) != running) {
            level.setBlock(reactor.getBlockPos(), state.setValue(RegolithReactorBlock.LIT, running), Block.UPDATE_ALL);
        }
        if (running && level.getGameTime() % 60 == 0) {
            level.playSound(null, reactor.getBlockPos(), ModSounds.REGOLITH_REACTOR_HUM, SoundSource.BLOCKS, 0.8f, 1.0f);
        }
    }

    private boolean work(ServerLevel level) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        ItemStack input = items.get(SLOT_INPUT);
        absorbBathAndAnode();
        input = items.get(SLOT_INPUT);
        if (formed && !input.is(ModTags.REGOLITH_REACTOR_INPUT)
                && (melt.working() || melt.find(level, input).isPresent())) {
            boolean hh = hallHeroult();
            melt.setEnergyFactor(hh ? HALL_HEROULT_ENERGY : 1.0);
            var done = melt.tick(level, items, new int[] {SLOT_INPUT}, MELT_OUT, energy, 1,
                    oxygen -> hh || oxygenBuffer + oxygen <= config.reactorOxygenBuffer + canisterRoom());
            progress = melt.progress();
            if (done != null && hh) {
                double aluminium = 0;
                for (var product : done.recipe().outputs()) {
                    if (product.result().item().value() == ModItems.ALUMINIUM_INGOT) {
                        aluminium += product.expected() * done.batches();
                    }
                }
                anodeCarbon = Math.max(0, anodeCarbon - aluminium * CARBON_PER_ALUMINIUM);
                bathCapacity = Math.max(0, bathCapacity - aluminium);
            } else if (done != null) {
                oxygenBuffer += done.recipe().oxygen() * done.batches();
                drainBufferToCanister();
                oxygenBuffer = Math.min(oxygenBuffer, config.reactorOxygenBuffer);
            }
            setChanged();
            return melt.working() || done != null;
        }
        long perTick = (config.reactorEnergyPerCycle + config.reactorCycleTicks - 1) / config.reactorCycleTicks;
        boolean canWork = formed && input.is(ModTags.REGOLITH_REACTOR_INPUT)
                && energy.amount >= perTick
                && fits(SLOT_IRON, ModItems.IRON_DUST) && fits(SLOT_TITANIUM, ModItems.TITANIUM_DUST)
                && fits(SLOT_SLAG, ModItems.SLAG)
                && oxygenBuffer + config.reactorOxygenPerBlock <= config.reactorOxygenBuffer + canisterRoom();
        if (!canWork) {
            if (progress > 0 && !formed) {
                progress = 0;
            }
            return false;
        }
        energy.amount -= perTick;
        progress++;
        if (progress >= config.reactorCycleTicks) {
            progress = 0;
            input.shrink(1);
            RegolithYield yield = new RegolithYield(config.reactorOxygenPerBlock, 1, config.reactorTitaniumChance, 1);
            RegolithYield.Output out = yield.roll(new java.util.SplittableRandom(level.getRandom().nextLong()));
            oxygenBuffer += out.oxygen();
            drainBufferToCanister();
            oxygenBuffer = Math.min(oxygenBuffer, config.reactorOxygenBuffer);
            grow(SLOT_IRON, new ItemStack(ModItems.IRON_DUST, out.ironDust()));
            if (out.titaniumDust() > 0) {
                grow(SLOT_TITANIUM, new ItemStack(ModItems.TITANIUM_DUST, out.titaniumDust()));
            }
            grow(SLOT_SLAG, new ItemStack(ModItems.SLAG, out.slag()));
            IndustryAdvancements.awardNearby(level, getBlockPos(), 16, IndustryAdvancements.LUNAR_AIR);
        }
        setChanged();
        return true;
    }

    private int canisterRoom() {
        ItemStack canister = items.get(SLOT_CANISTER);
        return canister.is(ModItems.OXYGEN_CANISTER) ? canister.getDamageValue() : 0;
    }

    private void drainBufferToCanister() {
        ItemStack canister = items.get(SLOT_CANISTER);
        if (oxygenBuffer <= 0 || !canister.is(ModItems.OXYGEN_CANISTER) || canister.getDamageValue() <= 0) {
            return;
        }
        int move = Math.min(oxygenBuffer, canister.getDamageValue());
        canister.setDamageValue(canister.getDamageValue() - move);
        oxygenBuffer -= move;
        setChanged();
    }

    private boolean fits(int slot, net.minecraft.world.item.Item item) {
        ItemStack existing = items.get(slot);
        return existing.isEmpty() || (existing.is(item) && existing.getCount() < existing.getMaxStackSize());
    }

    private void grow(int slot, ItemStack stack) {
        ItemStack existing = items.get(slot);
        if (existing.isEmpty()) {
            items.set(slot, stack);
        } else {
            existing.grow(stack.getCount());
        }
    }

    public List<Component> report() {
        List<Component> lines = new ArrayList<>();
        if (formed) {
            lines.add(Component.translatable("message.spacereloaded.regolith_reactor.formed"));
            lines.add(Component.translatable("message.spacereloaded.regolith_reactor.bath",
                    String.format(java.util.Locale.ROOT, "%.0f", bathCapacity),
                    String.format(java.util.Locale.ROOT, "%.1f", anodeCarbon)));
        } else {
            BlockPos bad = badCell();
            lines.add(Component.translatable("message.spacereloaded.regolith_reactor.not_formed",
                    bad == null ? "?" : bad.toShortString()));
        }
        return lines;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos center = pos.relative(state.getValue(RegolithReactorBlock.FACING).getOpposite());
            java.util.List<BlockPos> cube = new java.util.ArrayList<>(27);
            BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))
                    .forEach(p -> cube.add(p.immutable()));
            org.alex_melan.spacereloaded.multiblock.FormableBlock.apply(serverLevel, cube, false);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            IndustryStructures.release(serverLevel, getBlockPos());
        }
        super.setRemoved();
    }

    // ---------------- контейнер ----------------

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.regolith_reactor");
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
        return new RegolithReactorMenu(containerId, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_INPUT -> stack.is(ModTags.REGOLITH_REACTOR_INPUT) || stack.is(ModTags.MOLTEN_SALT_FEED)
                    || stack.is(ModItems.CRYOLITE) || anodeCarbon(stack);
            case SLOT_CANISTER -> stack.is(ModItems.OXYGEN_CANISTER);
            default -> false;
        };
    }

    private static final int[] ALL_SLOTS = {SLOT_INPUT, SLOT_CANISTER, SLOT_IRON, SLOT_TITANIUM, SLOT_SLAG};

    /**
     * Наружу у контроллера смотрит только фасад (остальные грани — внутри оболочки), поэтому
     * воронки работают через любую грань: сырьё и баллоны фильтруются по типу, наружу уходят
     * продукты и заряженные баллоны.
     */
    @Override
    public int[] getSlotsForFace(Direction side) {
        return ALL_SLOTS;
    }

    /**
     * Энерговвод оболочки (в духе мультиблоков Immersive Engineering): клетка футеровки
     * отдаёт кабелю энергохранилище контроллера, в чей куб 3×3×3 она входит.
     */
    public static EnergyStorage energyThroughLining(net.minecraft.world.level.Level level, BlockPos lining) {
        for (BlockPos candidate : BlockPos.betweenClosed(lining.offset(-2, -2, -2), lining.offset(2, 2, 2))) {
            if (level.getBlockEntity(candidate) instanceof RegolithReactorBlockEntity reactor && reactor.formed()) {
                BlockPos center = reactor.cubeCenter();
                if (Math.abs(lining.getX() - center.getX()) <= 1 && Math.abs(lining.getY() - center.getY()) <= 1
                        && Math.abs(lining.getZ() - center.getZ()) <= 1) {
                    return reactor.energy;
                }
            }
        }
        return null;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (slot == SLOT_CANISTER) {
            return stack.getDamageValue() == 0; // заряженный баллон уходит воронкой
        }
        return slot >= SLOT_IRON;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putInt("oxygen", oxygenBuffer);
        output.putLong("energy", energy.amount);
        melt.save(output.child("melt"));
        output.putDouble("bath", bathCapacity);
        output.putDouble("anode", anodeCarbon);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        oxygenBuffer = input.getIntOr("oxygen", 0);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
        melt.load(input.childOrEmpty("melt"));
        bathCapacity = input.getDoubleOr("bath", 0);
        anodeCarbon = input.getDoubleOr("anode", 0);
        dirty = true;
    }
}
