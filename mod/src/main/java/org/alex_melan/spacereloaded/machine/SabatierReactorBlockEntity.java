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
 *
 * <p>Режим газ-твёрдое (006, процессные рецепты {@code machine = sabatier_reactor}): тот же
 * каталитический реактор с подогревом проводит прямой синтез трихлорсилана
 * (Si + 3HCl → SiHCl₃ + H₂, кипящий слой 300 °C) и процесс Монда (Ni + 4CO → Ni(CO)₄ при 50 °C,
 * разложение при 200 °C — чистый никель из метеоритного железа, CO возвращается). Слоты [2–4] — продукты.
 */
public class SabatierReactorBlockEntity extends ChemMachineBlockEntity implements net.minecraft.world.MenuProvider {

    private final net.minecraft.world.inventory.ContainerData data = new net.minecraft.world.inventory.ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> process.working() ? process.maxProgress() : SpaceReloaded.config().sabatierTicks;
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                case 5 -> (int) fuelBuffer;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return org.alex_melan.spacereloaded.electronics.ProcessMenu.DATA;
        }
    };

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.spacereloaded.sabatier_reactor");
    }

    @Override
    public net.minecraft.world.inventory.AbstractContainerMenu createMenu(int id, net.minecraft.world.entity.player.Inventory inventory,
                                                                          net.minecraft.world.entity.player.Player player) {
        return new org.alex_melan.spacereloaded.electronics.ProcessMenu(org.alex_melan.spacereloaded.registry.ModMenus.SABATIER_REACTOR,
                org.alex_melan.spacereloaded.electronics.ProcessMenu.Layout.SABATIER_REACTOR, id, inventory, this, data);
    }

    private double fuelBuffer;

    public SabatierReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SABATIER_REACTOR, pos, state, 5);
    }

    private final ChemicalProcess process = new ChemicalProcess(org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe.SABATIER_REACTOR);
    private static final int[] IN = {0, 1};
    private static final int[] OUT = {2, 3, 4};

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
        if (!(co2.is(ModItems.CARBON_DIOXIDE) && ice.is(ModTags.ELECTROLYZER_INPUT))
                && (process.working() || process.find(level, co2, ice).isPresent())) {
            int before = process.progress();
            process.tick(level, items, IN, OUT, energy, 1, oxygen -> oxygen == 0);
            progress = process.progress();
            if (progress != before) {
                setChanged();
            }
            return;
        }
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
                org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 16,
                        org.alex_melan.spacereloaded.industry.IndustryAdvancements.METHALOX);
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

    // Ввод сверху/сбоку, продукты режима газ-твёрдое — снизу; метанокс уходит в баки напрямую
    @Override public int[] getSlotsForFace(Direction side) { return side == Direction.DOWN ? OUT : IN; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return slot < 2; }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot >= 2; }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putDouble("fuel_buffer", fuelBuffer);
        process.save(output.child("process"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelBuffer = input.getDoubleOr("fuel_buffer", 0);
        process.load(input.childOrEmpty("process"));
    }
}
