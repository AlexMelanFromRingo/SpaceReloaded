package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import org.alex_melan.spacereloaded.registry.ModTags;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;

/**
 * Перегонный куб (земная ветка топлива): нефтеносный сланец + энергия →
 * топливо во внутренний буфер → соседние баки. Выход выше электролизёра —
 * нефть энергетически выгоднее льда.
 */
public class RefineryBlockEntity extends ProcessingMachineBlockEntity {

    public static final double FUEL_BUFFER_CAPACITY = 500.0;

    private double fuelBuffer;

    private final ContainerData refineryData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> processingTicks();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                case 4 -> (int) fuelBuffer;
                case 5 -> (int) FUEL_BUFFER_CAPACITY;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            if (index == 0) {
                progress = value;
            } else if (index == 4) {
                fuelBuffer = value;
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    public RefineryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REFINERY, pos, state, 1);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
            pushFuelToTanks(level);
        }
        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick;
        ItemStack input = items.get(0);
        boolean canWork = input.is(ModTags.REFINERY_INPUT)
                && fuelBuffer + SpaceReloaded.config().refineryFuelPerOp <= FUEL_BUFFER_CAPACITY
                && energy.amount >= energyPerTick;

        if (canWork) {
            energy.amount -= energyPerTick;
            progress++;
            if (progress >= processingTicks()) {
                progress = 0;
                input.shrink(1);
                fuelBuffer += SpaceReloaded.config().refineryFuelPerOp;
                // Побочный продукт перегонки — сера (в композиты и порох)
                ItemStack byproduct = items.get(1);
                if (byproduct.isEmpty()) {
                    items.set(1, new ItemStack(net.minecraft.world.item.Items.SULFUR));
                } else if (byproduct.is(net.minecraft.world.item.Items.SULFUR)
                        && byproduct.getCount() < byproduct.getMaxStackSize()) {
                    byproduct.grow(1);
                }
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }
    }

    private void pushFuelToTanks(ServerLevel level) {
        if (fuelBuffer <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(dir)) instanceof FuelTankBlockEntity tank) {
                fuelBuffer -= tank.fill(fuelBuffer, "spacereloaded:kerolox");
                if (fuelBuffer <= 0) {
                    break;
                }
            }
        }
        setChanged();
    }

    @Override
    protected int processingTicks() {
        return SpaceReloaded.config().refineryTicks;
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        return ItemStack.EMPTY;
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
    }

    public ContainerData refineryData() {
        return refineryData;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.refinery");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new RefineryMenu(containerId, playerInventory, this, refineryData);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("fuel_buffer", fuelBuffer);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelBuffer = input.getDoubleOr("fuel_buffer", 0);
    }
}
