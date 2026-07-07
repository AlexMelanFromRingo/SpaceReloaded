package org.alex_melan.spacereloaded.machine.recipe;

import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategories;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import org.alex_melan.spacereloaded.registry.ModRecipes;

/** Рецепт дробилки: руда/сырьё → пыль (JSON: data/spacereloaded/recipe/, type spacereloaded:crushing). */
public class CrusherRecipe extends SingleItemRecipe {

    public static final MapCodec<CrusherRecipe> MAP_CODEC = SingleItemRecipe.simpleMapCodec(CrusherRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, CrusherRecipe> STREAM_CODEC =
            SingleItemRecipe.simpleStreamCodec(CrusherRecipe::new);

    public CrusherRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    /** Для дампа рецептов и подсказок. */
    public ItemStack resultStack() {
        return result().create();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<CrusherRecipe> getSerializer() {
        return ModRecipes.CRUSHING_SERIALIZER;
    }

    @Override
    public RecipeType<CrusherRecipe> getType() {
        return ModRecipes.CRUSHING;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }

    public static SingleRecipeInput input(ItemStack stack) {
        return new SingleRecipeInput(stack);
    }
}
