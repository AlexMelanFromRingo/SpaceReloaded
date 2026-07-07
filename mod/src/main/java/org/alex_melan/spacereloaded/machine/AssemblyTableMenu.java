package org.alex_melan.spacereloaded.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import org.alex_melan.spacereloaded.registry.ModMenus;

/** Меню сборочного стола: 5 входов (8..80, 35) → стрелка → выход (134,35). */
public class AssemblyTableMenu extends MachineMenu {

    public AssemblyTableMenu(int containerId, Inventory playerInventory,
                             Container container, ContainerData data) {
        super(ModMenus.ASSEMBLY_TABLE, containerId, playerInventory, container, data,
                AssemblyTableBlockEntity.INPUT_SLOTS);
    }

    public AssemblyTableMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory,
                new SimpleContainer(AssemblyTableBlockEntity.INPUT_SLOTS + 1), new SimpleContainerData(4));
    }

    @Override
    protected void addMachineSlots() {
        for (int i = 0; i < AssemblyTableBlockEntity.INPUT_SLOTS; i++) {
            addSlot(new Slot(container, i, 8 + i * 18, 35));
        }
        addSlot(new OutputSlot(container, AssemblyTableBlockEntity.INPUT_SLOTS, 134, 35));
    }
}
