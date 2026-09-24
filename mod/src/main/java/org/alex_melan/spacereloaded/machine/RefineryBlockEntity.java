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
 *
 * <p>Ректификация трихлорсилана (006, FR-413, D61): примеси BCl₃ (12.6 °C) и PCl₃ (76 °C) кипят
 * далеко от SiHCl₃ (31.8 °C); по Фенске каждая теоретическая тарелка делит примесь на α ≈ 2.5 —
 * чистота растёт на 0.4·K «девятки» за проход ({@link org.alex_melan.spacereloaded.core.electronics.Purity}).
 * Куб без колонны — одна ступень равновесия. Слоты: 0 — сланец или ТХС, 1 — сера, 2 — очищенный ТХС.
 */
public class RefineryBlockEntity extends ProcessingMachineBlockEntity
        implements org.alex_melan.spacereloaded.industry.IndustryStructures.StructureOwner {

    /** Ректификационная колонна (005, FR-323): тарелки столбом над кубом, формирует молот. */
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

    public boolean hammer(ServerLevel level, net.minecraft.server.level.ServerPlayer player) {
        boolean ok = structure.hammer(level, getBlockPos(), face(), player);
        setChanged();
        return ok;
    }

    /** Выход керолокса на единицу сланца: Y(K) по числу тарелок сформированной колонны. */
    public double fuelPerOperation() {
        double base = SpaceReloaded.config().refineryFuelPerOp;
        return structure.formed()
                ? org.alex_melan.spacereloaded.core.industry.ColumnYield.yield(structure.repeats(), base)
                : base;
    }

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
        super(ModBlockEntities.REFINERY, pos, state, 1, 2, false);
    }

    /** Теоретических ступеней: тарелки сформированной колонны или одна ступень куба. */
    public int stages() {
        return structure.formed() ? structure.repeats() : 1;
    }

    /** Проход ТХС через колонну: один предмет за цикл, чистота по Фенске. */
    private boolean distillSilane(ServerLevel level) {
        ItemStack input = items.get(0);
        if (!input.is(org.alex_melan.spacereloaded.registry.ModItems.TRICHLOROSILANE)) {
            return false;
        }
        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick;
        ItemStack product = input.copyWithCount(1);
        product.set(org.alex_melan.spacereloaded.registry.ModDataComponents.PURITY,
                (float) org.alex_melan.spacereloaded.core.electronics.Purity.afterColumn(
                        ChemicalProcess.purityOf(input), stages()));
        if (!ChemicalProcess.fits(items, new int[] {2}, java.util.List.of(product)) || energy.amount < energyPerTick) {
            if (progress > 0) {
                progress = Math.max(0, progress - 2);
                setChanged();
            }
            return true;
        }
        energy.amount -= energyPerTick;
        if (++progress >= processingTicks()) {
            progress = 0;
            input.shrink(1);
            ChemicalProcess.distribute(items, new int[] {2}, product);
            if (product.getOrDefault(org.alex_melan.spacereloaded.registry.ModDataComponents.PURITY, 0f)
                    >= org.alex_melan.spacereloaded.core.electronics.Purity.ELECTRONIC_GRADE) {
                org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 16,
                        org.alex_melan.spacereloaded.industry.IndustryAdvancements.NINE_NINES);
            }
            if (structure.formed()) {
                net.minecraft.core.BlockPos top = getBlockPos().above(structure.repeats() + 1);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, top.getX() + 0.5,
                        top.getY() + 0.1, top.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
            }
        }
        setChanged();
        return true;
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
        if (distillSilane(level)) {
            return;
        }
        long energyPerTick = SpaceReloaded.config().machineEnergyPerTick;
        ItemStack input = items.get(0);
        ItemStack sulfur = items.get(1);
        boolean canWork = input.is(ModTags.REFINERY_INPUT)
                && (sulfur.isEmpty() || (sulfur.is(net.minecraft.world.item.Items.SULFUR)
                        && sulfur.getCount() < sulfur.getMaxStackSize())) // сера не теряется — куб ждёт
                && fuelBuffer + fuelPerOperation() <= FUEL_BUFFER_CAPACITY
                && energy.amount >= energyPerTick;

        if (canWork) {
            energy.amount -= energyPerTick;
            progress++;
            if (progress >= processingTicks()) {
                progress = 0;
                input.shrink(1);
                fuelBuffer += fuelPerOperation();
                if (structure.formed()) {
                    net.minecraft.core.BlockPos top = getBlockPos().above(structure.repeats() + 1);
                    level.sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, top.getX() + 0.5,
                            top.getY() + 0.1, top.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
                }
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
        structure.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuelBuffer = input.getDoubleOr("fuel_buffer", 0);
        structure.load(input);
        claimed = false;
    }
}
