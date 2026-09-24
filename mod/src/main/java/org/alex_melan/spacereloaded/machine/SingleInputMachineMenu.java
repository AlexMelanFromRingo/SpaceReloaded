package org.alex_melan.spacereloaded.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;

/** Меню дробилки/электропечи: вход (44,35) → стрелка → выход (116,35). */
public class SingleInputMachineMenu extends MachineMenu {

    /** Серверный конструктор. */
    public SingleInputMachineMenu(MenuType<?> type, int containerId, Inventory playerInventory,
                                  Container container, ContainerData data) {
        super(type, containerId, playerInventory, container, data, 1);
    }

    /** Клиентский конструктор (фабрика MenuType). */
    public static SingleInputMachineMenu client(MenuType<?> type, int containerId, Inventory playerInventory) {
        return client(type, containerId, playerInventory, 2);
    }

    /** Клиентский конструктор машины с побочным выходом (электропечь 006: 3 слота). */
    public static SingleInputMachineMenu client(MenuType<?> type, int containerId, Inventory playerInventory,
                                                int size) {
        return new SingleInputMachineMenu(type, containerId, playerInventory,
                new SimpleContainer(size), new SimpleContainerData(4));
    }

    @Override
    protected void addMachineSlots() {
        addSlot(new Slot(container, 0, 44, 35));
        addSlot(new OutputSlot(container, 1, 116, 35));
        if (container.getContainerSize() > 2) {
            addSlot(new OutputSlot(container, 2, 116, 57)); // побочный продукт (CO карботермии и т. п.)
        }
    }
}
