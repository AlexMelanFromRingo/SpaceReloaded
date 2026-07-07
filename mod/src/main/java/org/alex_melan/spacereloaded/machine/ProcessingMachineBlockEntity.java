package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.CableNetworkManager;
import org.alex_melan.spacereloaded.registry.ModTags;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * База станков (T041): инвентарь (BaseContainerBlockEntity + WorldlyContainer —
 * ванильные хопперы и Fabric-трубы работают без доп. кода), энергобуфер
 * TR Energy, прогресс с сохранением, ContainerData для GUI.
 *
 * <p>Слоты: [0..inputSlots-1] — входы, [inputSlots] — выход.
 */
public abstract class ProcessingMachineBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    protected final int inputSlots;
    protected NonNullList<ItemStack> items;
    protected final SimpleEnergyStorage energy;
    protected int progress;

    protected final ContainerData dataAccess = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> processingTicks();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           int inputSlots) {
        this(type, pos, state, inputSlots, false);
    }

    protected ProcessingMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                           int inputSlots, boolean generator) {
        super(type, pos, state);
        this.inputSlots = inputSlots;
        this.items = NonNullList.withSize(inputSlots + 1, ItemStack.EMPTY);
        long capacity = SpaceReloaded.config().generatorBufferCapacity;
        // Потребитель: приём извне без отдачи; генератор — наоборот
        this.energy = generator
                ? new SimpleEnergyStorage(capacity, 0, Long.MAX_VALUE)
                : new SimpleEnergyStorage(capacity, capacity, 0);
    }

    /** Длительность одной операции, тики. */
    protected abstract int processingTicks();

    /** Результат для текущего входа либо {@code ItemStack.EMPTY}, если рецепта нет. */
    protected abstract ItemStack peekResult(ServerLevel level);

    /** Изъять входы одной операции (рецепт гарантированно совпал в этом тике). */
    protected abstract void consumeInputs(ServerLevel level);

    public EnergyStorage energyStorage() {
        return energy;
    }

    public ContainerData dataAccess() {
        return dataAccess;
    }

    private int outputSlot() {
        return inputSlots;
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            for (Direction dir : Direction.values()) {
                BlockPos neighbor = getBlockPos().relative(dir);
                if (level.getBlockState(neighbor).is(ModTags.ENERGY_CONDUIT)) {
                    CableNetworkManager.ensureNetwork(level, neighbor);
                }
            }
        }

        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick;
        ItemStack result = peekResult(level);
        if (!result.isEmpty() && outputFits(result) && energy.amount >= energyPerTick) {
            energy.amount -= energyPerTick;
            progress++;
            if (progress >= processingTicks()) {
                consumeInputs(level);
                insertOutput(result);
                progress = 0;
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2); // остывание без энергии/рецепта
            setChanged();
        }
    }

    private boolean outputFits(ItemStack result) {
        ItemStack existing = items.get(outputSlot());
        if (existing.isEmpty()) {
            return true;
        }
        return ItemStack.isSameItemSameComponents(existing, result)
                && existing.getCount() + result.getCount() <= existing.getMaxStackSize();
    }

    private void insertOutput(ItemStack result) {
        ItemStack existing = items.get(outputSlot());
        if (existing.isEmpty()) {
            items.set(outputSlot(), result.copy());
        } else {
            existing.grow(result.getCount());
        }
    }

    // ---------- Container / BaseContainerBlockEntity ----------

    @Override
    public int getContainerSize() {
        return items.size();
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
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < inputSlots;
    }

    // ---------- WorldlyContainer: хопперы (сверху/сбоку — входы, снизу — выход) ----------

    @Override
    public int[] getSlotsForFace(Direction face) {
        if (face == Direction.DOWN) {
            return new int[]{outputSlot()};
        }
        int[] inputs = new int[inputSlots];
        for (int i = 0; i < inputSlots; i++) {
            inputs[i] = i;
        }
        return inputs;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) {
        return slot == outputSlot();
    }

    @Override
    public boolean stillValid(Player player) {
        return net.minecraft.world.Container.stillValidBlockEntity(this, player);
    }

    // ---------- Персистентность ----------

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putInt("progress", progress);
        output.putLong("energy", energy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(inputSlots + 1, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        progress = input.getIntOr("progress", 0);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
    }
}
