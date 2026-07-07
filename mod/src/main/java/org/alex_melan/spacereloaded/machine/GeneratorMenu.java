package org.alex_melan.spacereloaded.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.registry.ModMenus;

/** Меню угольного генератора: слот топлива (80,35), пламя, шкала энергии. */
public class GeneratorMenu extends AbstractContainerMenu {

    private final Container container;
    private final ContainerData data; // 0 burnTime, 1 burnDuration, 2 energy, 3 cap

    public GeneratorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.COAL_GENERATOR, containerId);
        this.container = container;
        this.data = data;
        checkContainerDataCount(data, 4);

        addSlot(new Slot(container, 0, 80, 35));
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public GeneratorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(2), new SimpleContainerData(4));
    }

    public int burnTime() {
        return data.get(0);
    }

    public int burnDuration() {
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
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
