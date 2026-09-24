package org.alex_melan.spacereloaded.machine;

import com.mojang.serialization.Codec;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.electronics.Purity;
import org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe;
import org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipeInput;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModRecipes;
import team.reborn.energy.api.base.SimpleEnergyStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.DoublePredicate;

/**
 * Исполнитель процессных рецептов (006, D60) — общий для всего химического парка. Машина отдаёт
 * слоты реагентов и продуктов, энергобуфер и число параллельных реакций (ячейки стека 005);
 * исполнитель ищет рецепт своей машины, фиксирует партию на старте цикла (досыпать реагент на
 * последнем тике нельзя), списывает энергию равномерно и выдаёт продукты атомарно: реакция
 * стартует, только если все продукты целиком помещаются — в стек той же партии или в пустой слот,
 * с разбиением по нескольким слотам.
 *
 * <p>Партии материалов с чистотой смешиваются честно: масса примесей сохраняется
 * ({@link Purity#blend}), поэтому поликремний разной чистоты складывается в один слот, а N9
 * стопки — средневзвешенная доля примесей. Дробный выход (возврат HCl 95 %) копится по предмету
 * и сохраняется в машине.
 */
public final class ChemicalProcess {

    /** Завершённая реакция: рецепт и число параллельных реакций. */
    public record Finished(ChemicalRecipe recipe, int batches) {
    }

    private static final Codec<Map<String, Double>> FRACTIONS_CODEC = Codec.unboundedMap(Codec.STRING, Codec.DOUBLE);

    private final String machine;
    private final RecipeManager.CachedCheck<ChemicalRecipeInput, ChemicalRecipe> check =
            RecipeManager.createCheck(ModRecipes.CHEMICAL);
    private final Map<String, Double> fractions = new HashMap<>();
    private Object current;
    private int progress;
    private int maxProgress = 1;
    private int batches;
    private double energyFactor = 1.0;

    public ChemicalProcess(String machine) {
        this.machine = machine;
    }

    /**
     * Множитель энергии реакции для режима машины (Холл–Эру в криолите — ниже напряжение
     * разложения, чем у прямого электролиза оксида на инертном аноде).
     */
    public void setEnergyFactor(double factor) {
        this.energyFactor = factor;
    }

    public int progress() {
        return progress;
    }

    public int maxProgress() {
        return maxProgress;
    }

    public boolean working() {
        return progress > 0;
    }

    public Optional<RecipeHolder<ChemicalRecipe>> find(ServerLevel level, ItemStack... stacks) {
        return check.getRecipeFor(new ChemicalRecipeInput(machine, stacks), level);
    }

    /**
     * Один тик процесса.
     *
     * @param in          слоты реагентов (1–3)
     * @param out         слоты продуктов
     * @param maxBatches  предел параллельных реакций (1 — одиночная машина)
     * @param oxygenFits  принимает ли машина кислород реакции (единиц на все партии)
     * @return завершённая реакция или {@code null}
     */
    public Finished tick(ServerLevel level, NonNullList<ItemStack> items, int[] in, int[] out,
                         SimpleEnergyStorage energy, int maxBatches, DoublePredicate oxygenFits) {
        List<ItemStack> stacks = new ArrayList<>(in.length);
        for (int slot : in) {
            stacks.add(items.get(slot));
        }
        ChemicalRecipeInput input = new ChemicalRecipeInput(machine, stacks);
        Optional<RecipeHolder<ChemicalRecipe>> holder = check.getRecipeFor(input, level);
        if (holder.isEmpty() || holder.get().value().outputs().size() > out.length) {
            return reset();
        }
        ChemicalRecipe recipe = holder.get().value();
        int[] mapping = recipe.assignment(input).orElseThrow();
        int[] slots = new int[mapping.length];
        for (int i = 0; i < mapping.length; i++) {
            slots[i] = in[mapping[i]];
        }
        int available = maxBatches;
        for (int i = 0; i < recipe.inputs().size(); i++) {
            available = Math.min(available, items.get(slots[i]).getCount() / recipe.inputs().get(i).count());
        }
        if (progress > 0 && current == null) {
            current = holder.get().id(); // после загрузки мира — продолжить тот же цикл
        }
        if (progress > 0 && (!holder.get().id().equals(current) || available < batches)) {
            reset();
        }
        if (progress == 0) {
            current = holder.get().id();
            batches = available;
            while (batches > 0 && !(fits(items, out, plan(recipe, items.get(slots[0]), batches, true))
                    && oxygenFits.test(recipe.oxygen() * batches))) {
                batches--; // стек не помещается целиком — меньше ячеек в работе, а не простой
            }
            if (batches == 0) {
                return null;
            }
        } else if (!fits(items, out, plan(recipe, items.get(slots[0]), batches, true))
                || !oxygenFits.test(recipe.oxygen() * batches)) {
            return null; // выход заняли посреди цикла — пауза без потери прогресса
        }
        maxProgress = recipe.ticks();
        long energyTotal = (long) Math.ceil(recipe.energy() * energyFactor);
        long perTick = (energyTotal + recipe.ticks() - 1) / recipe.ticks() * batches;
        if (energy.amount < perTick) {
            return null;
        }
        energy.amount -= perTick;
        progress++;
        if (progress < recipe.ticks()) {
            return null;
        }
        List<ItemStack> products = plan(recipe, items.get(slots[0]), batches, false);
        for (int i = 0; i < recipe.inputs().size(); i++) {
            items.get(slots[i]).shrink(recipe.inputs().get(i).count() * batches);
        }
        for (ItemStack product : products) {
            distribute(items, out, product);
        }
        Finished done = new Finished(recipe, batches);
        progress = 0;
        return done;
    }

    private Finished reset() {
        progress = 0;
        current = null;
        return null;
    }

    /**
     * Продукты партии с чистотой. {@code worstCase} — для проверки места: дробный выход
     * округляется вверх, накопитель не меняется.
     */
    private List<ItemStack> plan(ChemicalRecipe recipe, ItemStack firstReagent, int batches, boolean worstCase) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = 0; i < recipe.outputs().size(); i++) {
            ChemicalRecipe.Product product = recipe.outputs().get(i);
            ItemStack stack = product.result().create();
            String key = BuiltInRegistries.ITEM.getKey(stack.getItem()) + "#" + i;
            double total = fractions.getOrDefault(key, 0.0) + product.expected() * batches;
            int count = worstCase ? (int) Math.ceil(total - 1e-9) : (int) Math.floor(total + 1e-9);
            if (!worstCase) {
                fractions.put(key, Math.max(0, total - count));
            }
            if (count <= 0) {
                continue;
            }
            stack.setCount(count);
            if (i == 0 && recipe.purity() >= 0) {
                stack.set(ModDataComponents.PURITY, (float) recipe.purity());
            } else if (i == 0 && recipe.keepPurity()) {
                stack.set(ModDataComponents.PURITY, (float) purityOf(firstReagent));
            }
            result.add(stack);
        }
        return result;
    }

    // ---------------- партии ----------------

    /** Чистота партии; без компонента — худший случай, технический кремний. */
    public static double purityOf(ItemStack stack) {
        Float purity = stack.get(ModDataComponents.PURITY);
        return purity == null ? Purity.METALLURGICAL : purity;
    }

    /** Складываются ли партии: тот же предмет и компоненты, чистота может различаться. */
    public static boolean canStack(ItemStack existing, ItemStack add) {
        if (existing.isEmpty() || add.isEmpty() || !ItemStack.isSameItem(existing, add)) {
            return false;
        }
        Float a = existing.get(ModDataComponents.PURITY);
        Float b = add.get(ModDataComponents.PURITY);
        if ((a == null) != (b == null)) {
            return false;
        }
        if (a == null) {
            return ItemStack.isSameItemSameComponents(existing, add);
        }
        ItemStack probe = add.copy();
        probe.set(ModDataComponents.PURITY, a);
        return ItemStack.isSameItemSameComponents(existing, probe);
    }

    /** Добавить партию в стопку: количество растёт, чистота — по массе примесей. */
    public static void merge(ItemStack existing, ItemStack add, int count) {
        Float a = existing.get(ModDataComponents.PURITY);
        Float b = add.get(ModDataComponents.PURITY);
        if (a != null && b != null) {
            existing.set(ModDataComponents.PURITY,
                    (float) Purity.blend(a, existing.getCount(), b, count));
        }
        existing.grow(count);
    }

    /** Помещаются ли все продукты целиком (симуляция на копиях). */
    public static boolean fits(NonNullList<ItemStack> items, int[] out, List<ItemStack> products) {
        NonNullList<ItemStack> sim = NonNullList.withSize(items.size(), ItemStack.EMPTY);
        for (int slot : out) {
            sim.set(slot, items.get(slot).copy());
        }
        for (ItemStack product : products) {
            if (!distribute(sim, out, product.copy())) {
                return false;
            }
        }
        return true;
    }

    /** Разложить продукт: сначала в стопки той же партии, затем в пустые слоты. */
    public static boolean distribute(NonNullList<ItemStack> items, int[] out, ItemStack product) {
        int left = product.getCount();
        for (int slot : out) {
            ItemStack existing = items.get(slot);
            if (left > 0 && canStack(existing, product)) {
                int move = Math.min(left, existing.getMaxStackSize() - existing.getCount());
                if (move > 0) {
                    merge(existing, product, move);
                    left -= move;
                }
            }
        }
        for (int slot : out) {
            if (left > 0 && items.get(slot).isEmpty()) {
                int move = Math.min(left, product.getMaxStackSize());
                items.set(slot, product.copyWithCount(move));
                left -= move;
            }
        }
        return left == 0;
    }

    // ---------------- сохранение ----------------

    public void save(ValueOutput output) {
        output.putInt("progress", progress);
        output.putInt("batches", batches);
        output.store("fractions", FRACTIONS_CODEC, new HashMap<>(fractions));
    }

    public void load(ValueInput input) {
        progress = input.getIntOr("progress", 0);
        batches = input.getIntOr("batches", 0);
        fractions.clear();
        input.read("fractions", FRACTIONS_CODEC).ifPresent(fractions::putAll);
        current = null;
        if (progress > 0 && batches <= 0) {
            progress = 0;
        }
    }
}
