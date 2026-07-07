package org.alex_melan.spacereloaded.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * База меню станков: входные слоты + выход + инвентарь игрока + ContainerData
 * (0 — прогресс, 1 — длительность, 2 — энергия, 3 — ёмкость).
 */
public abstract class MachineMenu extends AbstractContainerMenu {

    protected final Container container;
    protected final ContainerData data;
    protected final int inputSlots;

    protected MachineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                          Container container, ContainerData data, int inputSlots) {
        super(type, containerId);
        this.container = container;
        this.data = data;
        this.inputSlots = inputSlots;
        checkContainerSize(container, inputSlots + 1);
        checkContainerDataCount(data, 4);

        addMachineSlots();
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    /** Наследники размещают входы и выход. */
    protected abstract void addMachineSlots();

    /** Выходной слот: класть нельзя, только забирать. */
    protected class OutputSlot extends Slot {
        public OutputSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    public int progress() {
        return data.get(0);
    }

    public int maxProgress() {
        return Math.max(1, data.get(1));
    }

    public int energy() {
        return data.get(2);
    }

    public int energyCapacity() {
        return Math.max(1, data.get(3));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = inputSlots + 1;

        if (index < machineSlots) {
            // Из машины — в инвентарь игрока
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            // Из инвентаря — во входные слоты
            if (!moveItemStackTo(stack, 0, inputSlots, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }
}
