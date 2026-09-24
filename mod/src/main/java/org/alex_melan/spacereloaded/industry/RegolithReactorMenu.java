package org.alex_melan.spacereloaded.industry;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModTags;

/**
 * Меню реголитового реактора: сырьё (44,26), баллон (44,48), выходы Fe/Ti/шлак (80/98/116, 58),
 * шкалы энергии и буфера O₂, признак формирования.
 * Данные: 0 progress, 1 max, 2 energy, 3 ecap, 4 o2, 5 o2cap, 6 formed, 7 badIndex.
 */
public class RegolithReactorMenu extends AbstractContainerMenu {

    public static final int DATA_COUNT = 8;

    private final Container container;
    private final ContainerData data;

    public RegolithReactorMenu(int containerId, Inventory playerInventory, Container container, ContainerData data) {
        super(ModMenus.REGOLITH_REACTOR, containerId);
        this.container = container;
        this.data = data;
        checkContainerDataCount(data, DATA_COUNT);
        addSlot(new Slot(container, RegolithReactorBlockEntity.SLOT_INPUT, 44, 26) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModTags.REGOLITH_REACTOR_INPUT);
            }
        });
        addSlot(new Slot(container, RegolithReactorBlockEntity.SLOT_CANISTER, 44, 48) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.is(ModItems.OXYGEN_CANISTER);
            }
        });
        for (int i = 0; i < 3; i++) {
            addSlot(new Slot(container, RegolithReactorBlockEntity.SLOT_IRON + i, 80 + i * 18, 58) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
        }
        addStandardInventorySlots(playerInventory, 8, 84);
        addDataSlots(data);
    }

    public RegolithReactorMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(RegolithReactorBlockEntity.SLOTS),
                new SimpleContainerData(DATA_COUNT));
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

    public int oxygen() {
        return data.get(4);
    }

    public int oxygenCapacity() {
        return Math.max(1, data.get(5));
    }

    public boolean formed() {
        return data.get(6) != 0;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = RegolithReactorBlockEntity.SLOTS;
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            int target = stack.is(ModItems.OXYGEN_CANISTER) ? RegolithReactorBlockEntity.SLOT_CANISTER
                    : RegolithReactorBlockEntity.SLOT_INPUT;
            if (!moveItemStackTo(stack, target, target + 1, false)) {
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
