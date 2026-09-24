package org.alex_melan.spacereloaded.machine.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import org.alex_melan.spacereloaded.registry.ModRecipes;

import java.util.List;
import java.util.Optional;

/**
 * Процессный рецепт (006, FR-410, D60): машина-исполнитель ({@code machine}), до двух реагентов
 * (с количеством), до трёх продуктов с выходом ({@code yield} — доля от количества, дробная часть
 * копится в машине: возврат HCl в цикле Сименса 95 %), энергия (E, масштаб κ химии 005: честны
 * соотношения цен процессов), время и кислород ({@code oxygen}, единиц баллона — для электролиза
 * оксидных расплавов). Чистота кремния: {@code purity} ≥ 0 — задать продукту №1,
 * {@code keep_purity} — перенести с реагента №1. Реакции — данные: карботермия в печи,
 * хлор-щелочной электролиз в стеке, трихлорсилан и Монд в Сабатье, осаждение Сименса, мокрая
 * химия в реакторе (серная кислота, HF, глинозём, криолит, хлорид лития), Холл–Эру в MRE.
 */
public class ChemicalRecipe implements Recipe<ChemicalRecipeInput> {

    /** Реагент: ингредиент и количество. */
    public record Reagent(Ingredient ingredient, int count) {
        public static final Codec<Reagent> CODEC = RecordCodecBuilder.create(i -> i.group(
                Ingredient.CODEC.fieldOf("item").forGetter(Reagent::ingredient),
                Codec.intRange(1, 64).optionalFieldOf("count", 1).forGetter(Reagent::count)
        ).apply(i, Reagent::new));
        static final StreamCodec<RegistryFriendlyByteBuf, Reagent> STREAM = StreamCodec.composite(
                Ingredient.CONTENTS_STREAM_CODEC, Reagent::ingredient, ByteBufCodecs.VAR_INT, Reagent::count, Reagent::new);

        public boolean test(ItemStack stack) {
            return ingredient.test(stack) && stack.getCount() >= count;
        }
    }

    /** Продукт: стек и выход (доля от количества, 0 < yield ≤ 1). */
    public record Product(ItemStackTemplate result, double yield) {
        public static final Codec<Product> CODEC = RecordCodecBuilder.create(i -> i.group(
                ItemStackTemplate.MAP_CODEC.forGetter(Product::result),
                Codec.doubleRange(1e-6, 1.0).optionalFieldOf("yield", 1.0).forGetter(Product::yield)
        ).apply(i, Product::new));
        static final StreamCodec<RegistryFriendlyByteBuf, Product> STREAM = StreamCodec.composite(
                ItemStackTemplate.STREAM_CODEC, Product::result, ByteBufCodecs.DOUBLE, Product::yield, Product::new);

        /** Ожидаемое количество за реакцию. */
        public double expected() {
            return result.count() * yield;
        }
    }

    /** Машины-исполнители (значение поля {@code machine}). */
    public static final String CHEMICAL_REACTOR = "chemical_reactor";
    public static final String ELECTRIC_FURNACE = "electric_furnace";
    public static final String SABATIER_REACTOR = "sabatier_reactor";
    public static final String ELECTROLYZER = "electrolyzer";
    public static final String DEPOSITION_REACTOR = "deposition_reactor";
    public static final String REGOLITH_REACTOR = "regolith_reactor";

    public static final MapCodec<ChemicalRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.STRING.optionalFieldOf("machine", CHEMICAL_REACTOR).forGetter(r -> r.machine),
            Reagent.CODEC.listOf(1, 3).fieldOf("inputs").forGetter(r -> r.inputs),
            Product.CODEC.listOf(1, 3).fieldOf("outputs").forGetter(r -> r.outputs),
            Codec.LONG.optionalFieldOf("energy", 1600L).forGetter(r -> r.energy),
            Codec.INT.optionalFieldOf("ticks", 200).forGetter(r -> r.ticks),
            Codec.DOUBLE.optionalFieldOf("purity", -1.0).forGetter(r -> r.purity),
            Codec.BOOL.optionalFieldOf("keep_purity", false).forGetter(r -> r.keepPurity),
            Codec.INT.optionalFieldOf("oxygen", 0).forGetter(r -> r.oxygen)
    ).apply(i, ChemicalRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ChemicalRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, r) -> {
                ByteBufCodecs.STRING_UTF8.encode(buf, r.machine);
                Reagent.STREAM.apply(ByteBufCodecs.list()).encode(buf, r.inputs);
                Product.STREAM.apply(ByteBufCodecs.list()).encode(buf, r.outputs);
                ByteBufCodecs.VAR_LONG.encode(buf, r.energy);
                ByteBufCodecs.VAR_INT.encode(buf, r.ticks);
                ByteBufCodecs.DOUBLE.encode(buf, r.purity);
                ByteBufCodecs.BOOL.encode(buf, r.keepPurity);
                ByteBufCodecs.VAR_INT.encode(buf, r.oxygen);
            },
            buf -> new ChemicalRecipe(ByteBufCodecs.STRING_UTF8.decode(buf),
                    Reagent.STREAM.apply(ByteBufCodecs.list()).decode(buf),
                    Product.STREAM.apply(ByteBufCodecs.list()).decode(buf),
                    ByteBufCodecs.VAR_LONG.decode(buf), ByteBufCodecs.VAR_INT.decode(buf),
                    ByteBufCodecs.DOUBLE.decode(buf), ByteBufCodecs.BOOL.decode(buf), ByteBufCodecs.VAR_INT.decode(buf)));

    private final String machine;
    private final List<Reagent> inputs;
    private final List<Product> outputs;
    private final long energy;
    private final int ticks;
    private final double purity;
    private final boolean keepPurity;
    private final int oxygen;
    private PlacementInfo placementInfo;

    public ChemicalRecipe(String machine, List<Reagent> inputs, List<Product> outputs, long energy, int ticks,
                          double purity, boolean keepPurity, int oxygen) {
        this.machine = machine;
        this.inputs = List.copyOf(inputs);
        this.outputs = List.copyOf(outputs);
        this.energy = energy;
        this.ticks = Math.max(1, ticks);
        this.purity = purity;
        this.keepPurity = keepPurity;
        this.oxygen = Math.max(0, oxygen);
    }

    public String machine() {
        return machine;
    }

    public List<Reagent> inputs() {
        return inputs;
    }

    public List<Product> outputs() {
        return outputs;
    }

    public int oxygen() {
        return oxygen;
    }

    public long energy() {
        return energy;
    }

    public int ticks() {
        return ticks;
    }

    public double purity() {
        return purity;
    }

    public boolean keepPurity() {
        return keepPurity;
    }

    /**
     * Соответствие реагентов слотам: {@code slots[i]} — слот реагента i (слоты можно заполнять в
     * любом порядке); empty — рецепт не подходит. Лишние занятые слоты рецепт не пускают:
     * реакция с посторонним веществом в реакторе — не та реакция.
     */
    public Optional<int[]> assignment(ChemicalRecipeInput input) {
        int n = input.size();
        int occupied = 0;
        for (int s = 0; s < n; s++) {
            if (!input.getItem(s).isEmpty()) {
                occupied++;
            }
        }
        if (occupied != inputs.size()) {
            return Optional.empty();
        }
        int[] slots = new int[inputs.size()];
        return assign(input, 0, new boolean[n], slots) ? Optional.of(slots) : Optional.empty();
    }

    private boolean assign(ChemicalRecipeInput input, int reagent, boolean[] used, int[] slots) {
        if (reagent == inputs.size()) {
            return true;
        }
        for (int s = 0; s < input.size(); s++) {
            if (!used[s] && inputs.get(reagent).test(input.getItem(s))) {
                used[s] = true;
                slots[reagent] = s;
                if (assign(input, reagent + 1, used, slots)) {
                    return true;
                }
                used[s] = false;
            }
        }
        return false;
    }

    @Override
    public boolean matches(ChemicalRecipeInput input, Level level) {
        return machine.equals(input.machine()) && assignment(input).isPresent();
    }

    @Override
    public ItemStack assemble(ChemicalRecipeInput input) {
        return outputs.get(0).result().create();
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<ChemicalRecipe> getSerializer() {
        return ModRecipes.CHEMICAL_SERIALIZER;
    }

    @Override
    public RecipeType<ChemicalRecipe> getType() {
        return ModRecipes.CHEMICAL;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(inputs.stream().map(Reagent::ingredient).toList());
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
