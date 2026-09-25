package org.alex_melan.spacereloaded.kinetics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.config.SpaceReloadedConfig;
import org.alex_melan.spacereloaded.core.industry.MachiningTolerance;
import org.alex_melan.spacereloaded.machine.recipe.MachiningRecipe;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.Locale;
import java.util.Optional;

/**
 * База станков от вала (005, D54/D56): вход и выход (воронки: сверху/сбоку — вход, снизу — выход),
 * рабочее окно оборотов, накопление среднего отклонения скорости за операцию и перенос допуска
 * в результат: полуфабрикат — шаг и Σδ², деталь — качество (или брак — металлическая пыль).
 */
public abstract class KineticMachineBlockEntity<R extends MachiningRecipe> extends KineticBlockEntity
        implements WorldlyContainer {

    protected NonNullList<ItemStack> items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
    protected double deviationSum;
    protected int deviationSamples;
    private final RecipeManager.CachedCheck<SingleRecipeInput, R> check;

    protected KineticMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                                        RecipeType<R> recipeType) {
        super(type, pos, state);
        this.check = RecipeManager.createCheck(recipeType);
    }

    /** Число слотов: 0 — вход, 1 — выход; наследники добавляют свои (тигель, пыль пропила). */
    protected int slotCount() {
        return 2;
    }

    protected abstract double nominalOmega();

    protected abstract double minOmega();

    protected abstract double maxOmega();

    protected abstract double machineDeltaUm();

    protected Optional<RecipeHolder<R>> currentRecipe(ServerLevel level) {
        ItemStack input = items.get(0);
        if (input.isEmpty()) {
            return Optional.empty();
        }
        return check.getRecipeFor(new SingleRecipeInput(input), level);
    }

    protected boolean inWindow() {
        double w = Math.abs(omega);
        return w >= minOmega() && w <= maxOmega();
    }

    protected boolean outputFits(ItemStack result) {
        ItemStack out = items.get(1);
        return out.isEmpty() || (ItemStack.isSameItemSameComponents(out, result)
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    protected void sampleDeviation() {
        deviationSum += MachiningTolerance.deviation(omega, nominalOmega());
        deviationSamples++;
    }

    /** Завершение операции: изъять вход, выдать результат с допуском. */
    protected void finishOperation(ServerLevel level, R recipe) {
        SpaceReloadedConfig config = SpaceReloaded.config();
        ItemStack input = items.get(0);
        ItemStack result = recipe.assemble(new SingleRecipeInput(input));
        double meanDeviation = deviationSamples == 0 ? 0 : deviationSum / deviationSamples;
        double base = recipe.data().deltaUm() >= 0 ? recipe.data().deltaUm() : machineDeltaUm();
        double delta = MachiningTolerance.operationDelta(base, meanDeviation, config.toleranceSpeedFactor);
        double sumSq = MachiningTolerance.accumulate(MachiningRecipe.deltaSqOf(input), delta);
        if (recipe.data().balance()) {
            sumSq = MachiningTolerance.balance(sumSq);
        }
        if (ModItems.isEnginePart(result.getItem())) {
            if (MachiningTolerance.isScrap(sumSq, config.toleranceScrapUm)) {
                result = new ItemStack(ModItems.IRON_DUST, 2); // брак — в переплавку
            } else {
                result.set(ModDataComponents.PART_QUALITY,
                        (float) MachiningTolerance.quality(sumSq, config.toleranceScrapUm));
            }
        } else if (ModItems.isIntermediatePart(result.getItem())) {
            result.set(ModDataComponents.MACHINING_STEP, recipe.data().step() + 1);
            result.set(ModDataComponents.MACHINING_DELTA_SQ, (float) sumSq);
        }
        // 006 (D68): материал колеса турбины идёт с заготовкой через всю цепочку станков
        Integer superalloy = input.get(ModDataComponents.TURBINE_SUPERALLOY);
        if (superalloy != null && !result.isEmpty() && !result.is(ModItems.IRON_DUST)) {
            result.set(ModDataComponents.TURBINE_SUPERALLOY, superalloy);
        }
        input.shrink(1);
        ItemStack out = items.get(1);
        if (out.isEmpty()) {
            items.set(1, result);
        } else if (ItemStack.isSameItemSameComponents(out, result)) {
            out.grow(result.getCount());
        } else {
            Block.popResource(level, getBlockPos().above(), result);
        }
        deviationSum = 0;
        deviationSamples = 0;
        setChanged();
    }

    // ---------------- взаимодействие ----------------

    /** ПКМ предметом — положить во вход. */
    public boolean insert(ItemStack stack) {
        ItemStack in = items.get(0);
        if (in.isEmpty()) {
            items.set(0, stack.split(stack.getCount()));
            setChanged();
            return true;
        }
        if (ItemStack.isSameItemSameComponents(in, stack) && in.getCount() < in.getMaxStackSize()) {
            int move = Math.min(stack.getCount(), in.getMaxStackSize() - in.getCount());
            in.grow(move);
            stack.shrink(move);
            setChanged();
            return true;
        }
        return false;
    }

    /** Sneak+ПКМ пустой рукой — забрать выход (или вход, если выхода нет). */
    public ItemStack takeOutput() {
        int slot = items.get(1).isEmpty() ? 0 : 1;
        ItemStack taken = items.get(slot);
        items.set(slot, ItemStack.EMPTY);
        setChanged();
        return taken;
    }

    @Override
    protected void extraReport(java.util.List<Component> lines) {
        lines.add(Component.translatable("message.spacereloaded.kinetic.machine",
                String.format(Locale.ROOT, "%.0f", toRpm(minOmega())),
                String.format(Locale.ROOT, "%.0f", toRpm(maxOmega())),
                items.get(0).isEmpty() ? Component.literal("—") : items.get(0).getHoverName(),
                items.get(1).isEmpty() ? Component.literal("—") : items.get(1).getHoverName()));
    }

    // ---------------- контейнер ----------------

    @Override
    public int getContainerSize() {
        return slotCount();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack r = ContainerHelper.removeItem(items, slot, amount);
        if (!r.isEmpty()) {
            setChanged();
        }
        return r;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return side == Direction.DOWN ? new int[] {1} : new int[] {0};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 0;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return slot == 1;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null && !level.isClientSide()) {
            for (ItemStack stack : items) {
                if (!stack.isEmpty()) {
                    Block.popResource(level, pos, stack);
                }
            }
            items.clear();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.putDouble("dev_sum", deviationSum);
        output.putInt("dev_n", deviationSamples);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(slotCount(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        deviationSum = input.getDoubleOr("dev_sum", 0);
        deviationSamples = input.getIntOr("dev_n", 0);
    }
}
