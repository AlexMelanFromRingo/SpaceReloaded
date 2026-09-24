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
                case 1 -> SpaceReloaded.config().reactorCycleTicks;
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
        } else {
            BlockPos bad = badCell();
            lines.add(Component.translatable("message.spacereloaded.regolith_reactor.not_formed",
                    bad == null ? "?" : bad.toShortString()));
        }
        return lines;
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
            case SLOT_INPUT -> stack.is(ModTags.REGOLITH_REACTOR_INPUT);
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
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        oxygenBuffer = input.getIntOr("oxygen", 0);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
        dirty = true;
    }
}
