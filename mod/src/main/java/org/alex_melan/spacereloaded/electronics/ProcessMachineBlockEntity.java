package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.machine.MachineActivity;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.base.SimpleEnergyStorage;

/**
 * База процессных машин 006: инвентарь по раскладке {@link ProcessMenu.Layout} (воронки: сверху и
 * сбоку — входы с фильтром раскладки, снизу — выходы), энергобуфер TR Energy, ContainerData меню.
 * Логику тика задают наследники.
 */
public abstract class ProcessMachineBlockEntity extends BaseContainerBlockEntity
        implements WorldlyContainer, MachineActivity.Source {

    protected final ProcessMenu.Layout layout;
    protected NonNullList<ItemStack> items;
    protected final SimpleEnergyStorage energy;
    private int activeHold;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress();
                case 1 -> maxProgress();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                case 4 -> isoClass();
                case 5 -> reserve();
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return ProcessMenu.DATA;
        }
    };

    protected ProcessMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        ProcessMenu.Layout layout, long capacity, long maxInsert) {
        super(type, pos, state);
        this.layout = layout;
        this.items = NonNullList.withSize(layout.size(), ItemStack.EMPTY);
        this.energy = new SimpleEnergyStorage(capacity, maxInsert, 0);
    }

    public EnergyStorage energyStorage() {
        return energy;
    }

    public final void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
        }
        tick(level);
    }

    protected abstract void tick(ServerLevel level);

    protected abstract int progress();

    protected abstract int maxProgress();

    /** Класс ISO воздуха у машины (0 — не показывать). */
    protected int isoClass() {
        return 0;
    }

    /** Запас реагента внутри машины, −1 — нет. */
    protected int reserve() {
        return -1;
    }

    protected abstract MenuType<ProcessMenu> menuType();

    @Override
    public boolean isWorking() {
        return progress() > 0;
    }

    @Override
    public int activeHold() {
        return activeHold;
    }

    @Override
    public void setActiveHold(int hold) {
        activeHold = hold;
    }

    // ---------------- контейнер ----------------

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
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
        return new ProcessMenu(menuType(), layout, containerId, inventory, this, data);
    }

    @Override
    public int getContainerSize() {
        return layout.size();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return layout.accepts(slot, stack);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? layout.outputs() : layout.inputs();
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        for (int out : layout.outputs()) {
            if (out == slot) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putLong("energy", energy.amount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(layout.size(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        energy.amount = Math.min(energy.capacity, input.getLongOr("energy", 0));
    }
}
