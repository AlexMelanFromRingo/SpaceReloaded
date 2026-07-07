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
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModTags;
import org.alex_melan.spacereloaded.rocket.FuelTankBlockEntity;

/**
 * Электролизёр (US6 ISRU): лёд + энергия → топливо (гидролокс) во внутренний
 * буфер (перекачивается в соседние баки) + кислород (заряжает баллоны).
 * Слоты: [0] — лёд, [1] — баллон, [2] — служебный выход (не используется).
 */
public class ElectrolyzerBlockEntity extends ProcessingMachineBlockEntity {

    public static final double FUEL_BUFFER_CAPACITY = 500.0;

    private double fuelBuffer;

    private final ContainerData electrolyzerData = new ContainerData() {
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

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTROLYZER, pos, state, 2);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
            pushFuelToTanks(level);
        }
        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick;
        ItemStack ice = items.get(0);
        boolean canWork = ice.is(ModTags.ELECTROLYZER_INPUT)
                && fuelBuffer + SpaceReloaded.config().electrolyzerFuelPerOp <= FUEL_BUFFER_CAPACITY
                && energy.amount >= energyPerTick;

        if (canWork) {
            energy.amount -= energyPerTick;
            progress++;
            if (progress >= processingTicks()) {
                progress = 0;
                ice.shrink(1);
                fuelBuffer += SpaceReloaded.config().electrolyzerFuelPerOp;
                chargeCanister();
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }
    }

    /** Кислород электролиза — в баллон (уменьшение damage = зарядка). */
    private void chargeCanister() {
        ItemStack canister = items.get(1);
        if (canister.is(ModItems.OXYGEN_CANISTER) && canister.getDamageValue() > 0) {
            canister.setDamageValue(Math.max(0,
                    canister.getDamageValue() - SpaceReloaded.config().electrolyzerOxygenPerOp));
        }
    }

    /** Перекачка буфера в соседние баки (шланги/трубы — следующий срез). */
    private void pushFuelToTanks(ServerLevel level) {
        if (fuelBuffer <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(dir)) instanceof FuelTankBlockEntity tank) {
                fuelBuffer -= tank.fill(fuelBuffer, "spacereloaded:hydrolox");
                if (fuelBuffer <= 0) {
                    break;
                }
            }
        }
        setChanged();
    }

    @Override
    protected int processingTicks() {
        return SpaceReloaded.config().electrolyzerTicks;
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        return ItemStack.EMPTY; // работает не через рецепты предметов
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
    }

    public ContainerData electrolyzerData() {
        return electrolyzerData;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.electrolyzer");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new ElectrolyzerMenu(containerId, playerInventory, this, electrolyzerData);
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
