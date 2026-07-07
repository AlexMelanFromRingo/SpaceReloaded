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
import org.alex_melan.spacereloaded.registry.ModRecipes;

/**
 * Рецепт электропечи: пыль/шихта → слиток (type spacereloaded:electric_smelting).
 * Осознанно НЕ совместим с ванильной печью — цепочку нельзя обойти (FR-012).
 */
public class ElectricFurnaceRecipe extends SingleItemRecipe {

    public static final MapCodec<ElectricFurnaceRecipe> MAP_CODEC =
            SingleItemRecipe.simpleMapCodec(ElectricFurnaceRecipe::new);
    public static final StreamCodec<RegistryFriendlyByteBuf, ElectricFurnaceRecipe> STREAM_CODEC =
            SingleItemRecipe.simpleStreamCodec(ElectricFurnaceRecipe::new);

    public ElectricFurnaceRecipe(Recipe.CommonInfo commonInfo, Ingredient input, ItemStackTemplate result) {
        super(commonInfo, input, result);
    }

    public ItemStack resultStack() {
        return result().create();
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<ElectricFurnaceRecipe> getSerializer() {
        return ModRecipes.ELECTRIC_SMELTING_SERIALIZER;
    }

    @Override
    public RecipeType<ElectricFurnaceRecipe> getType() {
        return ModRecipes.ELECTRIC_SMELTING;
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return RecipeBookCategories.CRAFTING_MISC;
    }
}
