package org.alex_melan.spacereloaded.energy;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/** Аккумулятор (FR-010): хранилище + визуальные уровни заряда + GUI. */
public class BatteryBlockEntity extends MachineBlockEntity implements MenuProvider {

    /**
     * Ванильная синхронизация ContainerData режет значения до short (16 бит) —
     * 100000 E не влезает. Пакуем int в пары слотов lo/hi по 16 бит.
     */
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            int energyNow = (int) Math.min(Integer.MAX_VALUE, energy.amount);
            int capacity = (int) Math.min(Integer.MAX_VALUE, energy.capacity);
            return switch (index) {
                case 0 -> energyNow & 0xFFFF;
                case 1 -> energyNow >>> 16;
                case 2 -> capacity & 0xFFFF;
                case 3 -> capacity >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY, pos, state,
                SpaceReloaded.config().batteryCapacity,
                SpaceReloaded.config().batteryMaxTransfer,
                SpaceReloaded.config().batteryMaxTransfer);
    }

    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 != 0) {
            return;
        }
        EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());

        // Визуальная индикация заряда (5 уровней)
        BlockState state = getBlockState();
        if (state.hasProperty(BatteryBlock.CHARGE)) {
            int charge = (int) Math.round(4.0 * energy.amount / Math.max(1, energy.capacity));
            if (state.getValue(BatteryBlock.CHARGE) != charge) {
                level.setBlock(getBlockPos(), state.setValue(BatteryBlock.CHARGE, charge), 3);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.spacereloaded.battery");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new BatteryMenu(containerId, playerInventory, data);
    }
}
