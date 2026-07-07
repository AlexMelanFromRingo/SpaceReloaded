package org.alex_melan.spacereloaded.energy;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.registry.ModMenus;

/**
 * Меню аккумулятора: индикация заряда. Значения пакуются в пары слотов lo/hi —
 * ванильная синхронизация ContainerData обрезает данные до 16 бит.
 */
public class BatteryMenu extends AbstractContainerMenu {

    private final ContainerData data;

    public BatteryMenu(int containerId, Inventory playerInventory, ContainerData data) {
        super(ModMenus.BATTERY, containerId);
        this.data = data;
        checkContainerDataCount(data, 4);
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public BatteryMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainerData(4));
    }

    public int energy() {
        return (data.get(1) << 16) | (data.get(0) & 0xFFFF);
    }

    public int energyCapacity() {
        return Math.max(1, (data.get(3) << 16) | (data.get(2) & 0xFFFF));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY; // слотов машины нет
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
