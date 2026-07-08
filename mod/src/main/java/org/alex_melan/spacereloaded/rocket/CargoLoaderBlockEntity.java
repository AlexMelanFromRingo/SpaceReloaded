package org.alex_melan.spacereloaded.rocket;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Погрузчик (backlog: loaders/unloaders): раз в 10 тиков обслуживает
 * припаркованную ракету в зоне 17×48×17 над собой. ПОГРУЗКА: перекладывает
 * стеки из соседних контейнеров (сундук/хоппер вплотную) в грузовые отсеки
 * борта; РАЗГРУЗКА: наоборот. Вместе с посадочным маяком и заправочной
 * колонной образует полноценный докинг-пэд.
 */
public class CargoLoaderBlockEntity extends BlockEntity {

    public enum Mode {
        LOAD, UNLOAD, OFF;

        Mode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    private Mode mode = Mode.LOAD;

    public CargoLoaderBlockEntity(BlockPos pos, BlockState state) {
        super(org.alex_melan.spacereloaded.registry.ModBlockEntities.CARGO_LOADER, pos, state);
    }

    public Mode mode() {
        return mode;
    }

    public Mode cycleMode() {
        mode = mode.next();
        setChanged();
        return mode;
    }

    public static void serverTick(CargoLoaderBlockEntity loader, ServerLevel level) {
        if (loader.mode == Mode.OFF || level.getGameTime() % 10 != 0) {
            return;
        }
        BlockPos pos = loader.getBlockPos();
        List<RocketEntity> rockets = level.getEntities(
                EntityTypeTest.forClass(RocketEntity.class),
                new AABB(pos.getX() - 8, pos.getY(), pos.getZ() - 8,
                        pos.getX() + 9, pos.getY() + 48, pos.getZ() + 9),
                RocketEntity::isParked);
        if (rockets.isEmpty()) {
            return;
        }
        RocketEntity rocket = rockets.get(0);
        if (rocket.cargoSlots() <= 0) {
            return;
        }
        if (loader.mode == Mode.LOAD) {
            loadOneStack(level, pos, rocket);
        } else {
            unloadOneStack(level, pos, rocket);
        }
    }

    /** Один стек за операцию: из первого непустого соседнего контейнера в борт. */
    private static void loadOneStack(ServerLevel level, BlockPos pos, RocketEntity rocket) {
        for (Direction dir : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(dir)) instanceof Container container)) {
                continue;
            }
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                ItemStack leftover = rocket.loadCargo(stack.copy());
                if (leftover.getCount() != stack.getCount()) {
                    container.setItem(slot, leftover);
                    container.setChanged();
                    return;
                }
            }
        }
    }

    /** Один стек за операцию: из борта в первый соседний контейнер со свободным местом. */
    private static void unloadOneStack(ServerLevel level, BlockPos pos, RocketEntity rocket) {
        ItemStack stack = rocket.unloadCargo();
        if (stack.isEmpty()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!(level.getBlockEntity(pos.relative(dir)) instanceof Container container)) {
                continue;
            }
            stack = insertInto(container, stack);
            if (stack.isEmpty()) {
                return;
            }
        }
        if (!stack.isEmpty()) {
            rocket.loadCargo(stack); // некуда — вернуть в борт
        }
    }

    private static ItemStack insertInto(Container container, ItemStack stack) {
        for (int slot = 0; slot < container.getContainerSize() && !stack.isEmpty(); slot++) {
            ItemStack existing = container.getItem(slot);
            if (existing.isEmpty()) {
                container.setItem(slot, stack);
                container.setChanged();
                return ItemStack.EMPTY;
            }
            if (ItemStack.isSameItemSameComponents(existing, stack)) {
                int room = existing.getMaxStackSize() - existing.getCount();
                if (room > 0) {
                    int moved = Math.min(room, stack.getCount());
                    existing.grow(moved);
                    stack.shrink(moved);
                    container.setChanged();
                }
            }
        }
        return stack;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("mode", mode.name());
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        try {
            mode = Mode.valueOf(input.getStringOr("mode", Mode.LOAD.name()));
        } catch (IllegalArgumentException e) {
            mode = Mode.LOAD;
        }
    }
}
