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
 * Слоты: [0] — лёд или рассол, [1] — баллон, [2–3] — продукты хлор-щелочного режима.
 *
 * <p>Хлор-щелочной режим (006): рассол в ячейках — 2NaCl + 2H₂O → Cl₂ + H₂ + 2NaOH, хлор и
 * водород сжигаются в HCl на выходе (печь синтеза хлороводорода). Ячейки стека работают
 * параллельно (Фарадей: N ячеек — N-кратный выход при N-кратной энергии), рецепты — процессные
 * ({@code machine = electrolyzer}).
 */
public class ElectrolyzerBlockEntity extends ProcessingMachineBlockEntity
        implements org.alex_melan.spacereloaded.industry.IndustryStructures.StructureOwner {

    /** Электролизный стек (005, FR-322): ячейки за задней гранью, формирует молот. */
    private final org.alex_melan.spacereloaded.multiblock.FormedStructure structure =
            new org.alex_melan.spacereloaded.multiblock.FormedStructure();
    private boolean claimed;

    @Override
    public void markStructureDirty() {
        structure.markDirty();
    }

    public org.alex_melan.spacereloaded.multiblock.FormedStructure structure() {
        return structure;
    }

    @Override
    public void preRemoveSideEffects(net.minecraft.core.BlockPos pos, BlockState state) {
        if (level instanceof ServerLevel serverLevel) {
            structure.dismantle(serverLevel, pos, state.getValue(ProcessingMachineBlock.FACING), state.getBlock());
        }
        super.preRemoveSideEffects(pos, state);
    }

    private net.minecraft.core.Direction face() {
        return getBlockState().getValue(ProcessingMachineBlock.FACING);
    }

    /** Удар молотом (005). */
    public boolean hammer(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        if (ok && structure.repeats() >= 7) {
            org.alex_melan.spacereloaded.industry.IndustryAdvancements.award(player,
                    org.alex_melan.spacereloaded.industry.IndustryAdvancements.STACK);
        }
        setChanged();
        return ok;
    }

    /** Сколько единиц льда перерабатывает цикл (N ячеек + сам электролизёр). */
    private int unitsPerCycle(ItemStack ice) {
        return org.alex_melan.spacereloaded.core.industry.ElectrolysisStack.unitsPerCycle(structure.repeats(),
                ice.getCount());
    }

    public static final double FUEL_BUFFER_CAPACITY = 500.0;

    private double fuelBuffer;

    private final ContainerData electrolyzerData = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> chlorAlkali.working() ? chlorAlkali.maxProgress() : processingTicks();
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
        super(ModBlockEntities.ELECTROLYZER, pos, state, 2, 2, false);
    }

    private final ChemicalProcess chlorAlkali = new ChemicalProcess(org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe.ELECTROLYZER);

    /** Хлор-щелочной режим: в ячейках рассол (процессный рецепт электролизёра). */
    private boolean processMode(ServerLevel level) {
        return chlorAlkali.working() || chlorAlkali.find(level, items.get(0)).isPresent();
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (!claimed) {
            claimed = true;
            structure.reclaim(level, getBlockPos(), face());
        }
        if (structure.revalidate(level, getBlockPos(), face())) {
            setChanged();
        }
        if (level.getGameTime() % 20 == 0) {
            EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
            pushFuelToTanks(level);
        }
        if (processMode(level)) {
            int before = chlorAlkali.progress();
            ChemicalProcess.Finished done = chlorAlkali.tick(level, items, new int[] {0}, outputSlotIndices(), energy,
                    structure.repeats() + 1, oxygen -> oxygen == 0);
            progress = chlorAlkali.progress();
            if (done != null && structure.formed()) {
                workParticles(level);
            }
            if (progress != before) {
                setChanged();
            }
            return;
        }
        ItemStack ice = items.get(0);
        int units = Math.max(1, unitsPerCycle(ice));
        // Фарадей: N ячеек последовательно — N-кратный выход при N-кратной энергии (энергия/кг постоянна)
        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick * units;
        boolean canWork = ice.is(ModTags.ELECTROLYZER_INPUT)
                && fuelBuffer + SpaceReloaded.config().electrolyzerFuelPerOp * units <= FUEL_BUFFER_CAPACITY * units
                && energy.amount >= energyPerTick;

        if (canWork) {
            energy.amount -= energyPerTick;
            progress++;
            if (progress >= processingTicks()) {
                progress = 0;
                ice.shrink(units);
                fuelBuffer += SpaceReloaded.config().electrolyzerFuelPerOp * units;
                for (int i = 0; i < units; i++) {
                    chargeCanister();
                }
                if (structure.formed()) {
                    workParticles(level);
                }
            }
            setChanged();
        } else if (progress > 0) {
            progress = Math.max(0, progress - 2);
            setChanged();
        }
    }

    /** Пузыри над ячейками работающего стека. */
    private void workParticles(ServerLevel level) {
        net.minecraft.core.Direction back = face().getOpposite();
        for (int i = 1; i <= structure.repeats(); i++) {
            net.minecraft.core.BlockPos cell = getBlockPos().relative(back, i);
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_POP, cell.getX() + 0.5,
                    cell.getY() + 1.05, cell.getZ() + 0.5, 4, 0.25, 0.05, 0.25, 0.01);
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
        structure.save(output);
        chlorAlkali.save(output.child("chlor_alkali"));
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelBuffer = input.getDoubleOr("fuel_buffer", 0);
        structure.load(input);
        chlorAlkali.load(input.childOrEmpty("chlor_alkali"));
        claimed = false;
    }
}
