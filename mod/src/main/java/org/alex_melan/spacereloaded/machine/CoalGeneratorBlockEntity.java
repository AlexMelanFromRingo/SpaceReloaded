package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.EnergyUtil;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;

/**
 * Угольный генератор — стартовая энергетика: жжёт любое печное топливо
 * (данные из ванильного FuelValues) и выталкивает энергию соседям/в сеть.
 */
public class CoalGeneratorBlockEntity extends ProcessingMachineBlockEntity {

    private int burnTime;
    private int burnDuration;

    private final ContainerData generatorData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> burnTime;
                case 1 -> Math.max(1, burnDuration);
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                burnTime = value;
            } else if (index == 1) {
                burnDuration = value;
            }
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    public CoalGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COAL_GENERATOR, pos, state, 1, true);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
        }
        boolean changed = false;
        if (burnTime > 0) {
            burnTime--;
            energy.amount = Math.min(energy.capacity,
                    energy.amount + SpaceReloaded.config().coalGeneratorEnergyPerTick);
            changed = true;
        }
        if (burnTime == 0 && energy.amount < energy.capacity) {
            ItemStack fuel = items.get(0);
            int duration = fuel.isEmpty() ? 0 : level.fuelValues().burnDuration(fuel);
            if (duration > 0) {
                burnDuration = duration;
                burnTime = duration;
                fuel.shrink(1);
                changed = true;
            }
        }
        if (energy.amount > 0) {
            EnergyUtil.pushToNeighbors(level, getBlockPos(), energy);
        }
        if (changed) {
            setChanged();
        }
    }

    @Override
    protected int processingTicks() {
        return Math.max(1, burnDuration);
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        return ItemStack.EMPTY; // генератор ничего не крафтит
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
    }

    public ContainerData generatorData() {
        return generatorData;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.coal_generator");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new GeneratorMenu(containerId, playerInventory, this, generatorData);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("burn_time", burnTime);
        output.putInt("burn_duration", burnDuration);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        burnTime = input.getIntOr("burn_time", 0);
        burnDuration = input.getIntOr("burn_duration", 0);
    }
}
