package org.alex_melan.spacereloaded.electronics;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModTags;

import java.util.List;

/**
 * Меню процессных машин 006 по раскладке: входы, выходы, стрелка, шкала энергии, класс чистоты
 * комнаты (для машин фаба). ContainerData: 0 прогресс, 1 длительность, 2 энергия, 3 ёмкость,
 * 4 класс ISO (0 — не показывать), 5 запас реагента (единиц, −1 — нет).
 */
public class ProcessMenu extends AbstractContainerMenu {

    public static final int DATA = 6;

    public record SlotSpec(int index, int x, int y, boolean output) {
        static SlotSpec in(int index, int x, int y) {
            return new SlotSpec(index, x, y, false);
        }

        static SlotSpec out(int index, int x, int y) {
            return new SlotSpec(index, x, y, true);
        }
    }

    /** Раскладки машин. */
    public enum Layout {
        CHEMICAL_REACTOR(72, 35, false, SlotSpec.in(0, 35, 17), SlotSpec.in(1, 35, 35), SlotSpec.in(2, 35, 53),
                SlotSpec.out(3, 116, 17), SlotSpec.out(4, 116, 35), SlotSpec.out(5, 116, 53)),
        SABATIER_REACTOR(72, 35, false, SlotSpec.in(0, 35, 26), SlotSpec.in(1, 35, 48),
                SlotSpec.out(2, 116, 17), SlotSpec.out(3, 116, 35), SlotSpec.out(4, 116, 53)),
        DEPOSITION_REACTOR(76, 36, false, SlotSpec.in(0, 26, 26), SlotSpec.in(1, 26, 48), SlotSpec.in(2, 50, 37),
                SlotSpec.out(3, 116, 26), SlotSpec.out(4, 116, 48)),
        DIFFUSION_FURNACE(76, 35, true, SlotSpec.in(0, 26, 35), SlotSpec.in(1, 50, 17), SlotSpec.in(2, 50, 35),
                SlotSpec.in(3, 50, 53), SlotSpec.out(4, 116, 35)),
        LITHOGRAPHY_STATION(76, 35, true, SlotSpec.in(0, 26, 35), SlotSpec.in(1, 50, 26), SlotSpec.in(2, 50, 48),
                SlotSpec.out(3, 116, 35)),
        BIOMASS_OXIDIZER(76, 35, false, SlotSpec.in(0, 56, 35)),
        CO2_SCRUBBER(76, 35, false, SlotSpec.in(0, 50, 35), SlotSpec.out(1, 116, 35)),
        ETCH_BATH(76, 35, true, SlotSpec.in(0, 26, 35), SlotSpec.in(1, 50, 35), SlotSpec.out(2, 116, 35));

        public final int arrowX;
        public final int arrowY;
        public final boolean cleanroom;
        public final List<SlotSpec> slots;

        Layout(int arrowX, int arrowY, boolean cleanroom, SlotSpec... slots) {
            this.arrowX = arrowX;
            this.arrowY = arrowY;
            this.cleanroom = cleanroom;
            this.slots = List.of(slots);
        }

        public int size() {
            return slots.size();
        }

        public int[] inputs() {
            return slots.stream().filter(s -> !s.output()).mapToInt(SlotSpec::index).toArray();
        }

        public int[] outputs() {
            return slots.stream().filter(SlotSpec::output).mapToInt(SlotSpec::index).toArray();
        }

        /** Что можно положить в слот (общая проверка сервера и клиента). */
        public boolean accepts(int index, ItemStack stack) {
            for (SlotSpec spec : slots) {
                if (spec.index() == index && spec.output()) {
                    return false;
                }
            }
            return switch (this) {
                case CHEMICAL_REACTOR, SABATIER_REACTOR -> true;
                case DEPOSITION_REACTOR -> switch (index) {
                    case 0 -> stack.is(ModItems.TRICHLOROSILANE);
                    case 1 -> stack.is(ModTags.ELECTROLYZER_INPUT);
                    default -> stack.is(ModItems.SAPPHIRE_WAFER);
                };
                case DIFFUSION_FURNACE -> switch (index) {
                    case 0 -> WaferKind.isWafer(stack) || stack.is(ModItems.MULTICRYSTALLINE_WAFER);
                    case 1 -> stack.is(ModItems.OXYGEN_CANISTER);
                    case 2 -> stack.is(ModItems.ALUMINIUM_INGOT);
                    default -> stack.is(ModItems.PHOSPHORUS);
                };
                case LITHOGRAPHY_STATION -> switch (index) {
                    case 0 -> WaferKind.isWafer(stack);
                    case 1 -> WaferKind.byMask(stack) != null;
                    default -> stack.is(ModItems.PHOTORESIST);
                };
                case BIOMASS_OXIDIZER -> stack.is(ModTags.BIOMASS);
                case CO2_SCRUBBER -> stack.is(ModItems.LIOH_CARTRIDGE) || stack.is(ModItems.ZEOLITE_BED);
                case ETCH_BATH -> index == 0 ? WaferKind.isWafer(stack)
                        : stack.is(ModItems.HYDROFLUORIC_ACID) || stack.is(ModItems.CAUSTIC_SODA);
            };
        }

        public String texture() {
            return "textures/gui/" + name().toLowerCase(java.util.Locale.ROOT) + ".png";
        }
    }

    private final Layout layout;
    private final Container container;
    private final ContainerData data;

    public ProcessMenu(MenuType<?> type, Layout layout, int id, Inventory inventory, Container container,
                       ContainerData data) {
        super(type, id);
        this.layout = layout;
        this.container = container;
        this.data = data;
        checkContainerSize(container, layout.size());
        checkContainerDataCount(data, DATA);
        for (SlotSpec spec : layout.slots) {
            addSlot(spec.output() ? new OutSlot(container, spec.index(), spec.x(), spec.y())
                    : new Slot(container, spec.index(), spec.x(), spec.y()) {
                        @Override
                        public boolean mayPlace(ItemStack stack) {
                            return layout.accepts(spec.index(), stack);
                        }
                    });
        }
        addStandardInventorySlots(inventory, 8, 84);
        addDataSlots(data);
    }

    /** Клиентская фабрика. */
    public static ProcessMenu client(MenuType<?> type, Layout layout, int id, Inventory inventory) {
        return new ProcessMenu(type, layout, id, inventory, new SimpleContainer(layout.size()),
                new SimpleContainerData(DATA));
    }

    private static final class OutSlot extends Slot {
        OutSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    public Layout layout() {
        return layout;
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

    /** Класс ISO воздуха у машины фаба (0 — не показывать). */
    public int isoClass() {
        return data.get(4);
    }

    /** Запас реагента в машине (заряд ванны, доза), −1 — нет. */
    public int reserve() {
        return data.get(5);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machine = layout.size();
        if (index < machine) {
            if (!moveItemStackTo(stack, machine, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            for (int i = 0; i < machine && !stack.isEmpty(); i++) {
                Slot target = slots.get(i);
                if (target.mayPlace(stack) && moveItemStackTo(stack, i, i + 1, false)) {
                    moved = true;
                }
            }
            if (!moved) {
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
