package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.network.chat.Component;
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
 * Реактор Сабатье (Phase 11): CO2 + водород (из льда) + энергия → метанокс.
 * Реакция Сабатье CO2 + 4H2 → CH4 + 2H2O; водород берём из льда, как
 * электролизёр. Топливо копится в буфере и перекачивается в соседние баки.
 * Слоты: [0] — CO2, [1] — лёд.
 */
public class SabatierReactorBlockEntity extends ChemMachineBlockEntity {

    private double fuelBuffer;

    public SabatierReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SABATIER_REACTOR, pos, state, 2);
    }

    public double fuelBuffer() {
        return fuelBuffer;
    }

    @Override
    public void serverTick(ServerLevel level) {
        var config = SpaceReloaded.config();
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
            pushFuelToTanks(level);
        }
        ItemStack co2 = items.get(0);
        ItemStack ice = items.get(1);
        boolean canWork = co2.is(ModItems.CARBON_DIOXIDE) && ice.is(ModTags.ELECTROLYZER_INPUT)
                && fuelBuffer + config.sabatierFuelPerOp <= config.sabatierBufferCapacity
                && energy.amount >= config.chemMachineEnergyPerTick;
        if (canWork) {
            energy.amount -= config.chemMachineEnergyPerTick;
            progress++;
            if (progress >= config.sabatierTicks) {
                progress = 0;
                co2.shrink(1);
                ice.shrink(1);
                fuelBuffer += config.sabatierFuelPerOp;
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }
    }

    @Override
    public Component status(ServerLevel level) {
        return Component.translatable("message.spacereloaded.sabatier.status",
                (int) fuelBuffer, (int) SpaceReloaded.config().sabatierBufferCapacity, energy.amount);
    }

    private void pushFuelToTanks(ServerLevel level) {
        if (fuelBuffer <= 0) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (level.getBlockEntity(getBlockPos().relative(dir)) instanceof FuelTankBlockEntity tank) {
                fuelBuffer -= tank.fill(fuelBuffer, "spacereloaded:methalox");
                if (fuelBuffer <= 0) {
                    break;
                }
            }
        }
        setChanged();
    }

    // Ввод CO2/лёд с любой стороны; топливо уходит в баки напрямую (не предмет)
    @Override public int[] getSlotsForFace(Direction side) { return new int[]{0, 1}; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0 ? stack.is(ModItems.CARBON_DIOXIDE) : stack.is(ModTags.ELECTROLYZER_INPUT);
    }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return false; }

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
