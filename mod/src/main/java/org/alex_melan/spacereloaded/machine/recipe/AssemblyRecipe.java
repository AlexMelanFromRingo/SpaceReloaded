package org.alex_melan.spacereloaded.machine.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Рецепт сборочного стола (type spacereloaded:assembly): мульти-входовой
 * бесформенный крафт. Каждый ингредиент потребляет 1 предмет из отдельного
 * слота; посторонние предметы в слотах блокируют рецепт (детерминизм).
 * Ингредиент, нужный дважды, указывается в списке дважды.
 */
public class AssemblyRecipe implements Recipe<AssemblyRecipeInput> {

    public static final MapCodec<AssemblyRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Ingredient.CODEC.listOf().fieldOf("ingredients").forGetter(r -> r.ingredients),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result)
    ).apply(instance, AssemblyRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AssemblyRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeVarInt(recipe.ingredients.size());
                for (Ingredient ingredient : recipe.ingredients) {
                    Ingredient.CONTENTS_STREAM_CODEC.encode(buf, ingredient);
                }
                ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.result);
            },
            buf -> {
                int count = buf.readVarInt();
                List<Ingredient> ingredients = new ArrayList<>(count);
                for (int i = 0; i < count; i++) {
                    ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buf));
                }
                return new AssemblyRecipe(ingredients, ItemStackTemplate.STREAM_CODEC.decode(buf));
            });

    private final List<Ingredient> ingredients;
    private final ItemStackTemplate result;
    private PlacementInfo placementInfo;

    public AssemblyRecipe(List<Ingredient> ingredients, ItemStackTemplate result) {
        this.ingredients = List.copyOf(ingredients);
        this.result = result;
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }

    public ItemStack resultStack() {
        return result.create();
    }

    @Override
    public boolean matches(AssemblyRecipeInput input, Level level) {
        return findAssignment(input) != null;
    }

    /**
     * Биекция «ингредиент → непустой слот»: каждый непустой слот должен быть
     * использован ровно одним ингредиентом. Бэктрекинг; размеры ≤ 5 — дёшево.
     *
     * @return индексы слотов по порядку ингредиентов либо null
     */
    public int[] findAssignment(AssemblyRecipeInput input) {
        List<Integer> nonEmpty = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.getItem(i).isEmpty()) {
                nonEmpty.add(i);
            }
        }
        if (nonEmpty.size() != ingredients.size()) {
            return null;
        }
        int[] assignment = new int[ingredients.size()];
        boolean[] used = new boolean[nonEmpty.size()];
        return backtrack(input, nonEmpty, used, assignment, 0) ? assignment : null;
    }

    private boolean backtrack(AssemblyRecipeInput input, List<Integer> slots,
                              boolean[] used, int[] assignment, int ingredientIndex) {
        if (ingredientIndex == ingredients.size()) {
            return true;
        }
        Ingredient ingredient = ingredients.get(ingredientIndex);
        for (int s = 0; s < slots.size(); s++) {
            if (used[s] || !ingredient.test(input.getItem(slots.get(s)))) {
                continue;
            }
            used[s] = true;
            assignment[ingredientIndex] = slots.get(s);
            if (backtrack(input, slots, used, assignment, ingredientIndex + 1)) {
                return true;
            }
            used[s] = false;
        }
        return false;
    }

    @Override
    public ItemStack assemble(AssemblyRecipeInput input) {
        return result.create();
    }

    @Override
    public boolean showNotification() {
        return true;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<AssemblyRecipe> getSerializer() {
        return ModRecipes.ASSEMBLY_SERIALIZER;
    }

    @Override
    public RecipeType<AssemblyRecipe> getType() {
        return ModRecipes.ASSEMBLY;
    }

    @Override
    public PlacementInfo placementInfo() {
        if (placementInfo == null) {
            placementInfo = PlacementInfo.create(ingredients); // лениво, как в ванили
        }
        return placementInfo;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
