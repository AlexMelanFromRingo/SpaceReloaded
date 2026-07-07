package org.alex_melan.spacereloaded.registry;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.recipe.AssemblyRecipe;
import org.alex_melan.spacereloaded.machine.recipe.CrusherRecipe;
import org.alex_melan.spacereloaded.machine.recipe.ElectricFurnaceRecipe;

public final class ModRecipes {

    public static final RecipeType<CrusherRecipe> CRUSHING = registerType("crushing");
    public static final RecipeType<ElectricFurnaceRecipe> ELECTRIC_SMELTING = registerType("electric_smelting");
    public static final RecipeType<AssemblyRecipe> ASSEMBLY = registerType("assembly");

    public static final RecipeSerializer<CrusherRecipe> CRUSHING_SERIALIZER =
            registerSerializer("crushing", new RecipeSerializer<>(CrusherRecipe.MAP_CODEC, CrusherRecipe.STREAM_CODEC));
    public static final RecipeSerializer<ElectricFurnaceRecipe> ELECTRIC_SMELTING_SERIALIZER =
            registerSerializer("electric_smelting",
                    new RecipeSerializer<>(ElectricFurnaceRecipe.MAP_CODEC, ElectricFurnaceRecipe.STREAM_CODEC));
    public static final RecipeSerializer<AssemblyRecipe> ASSEMBLY_SERIALIZER =
            registerSerializer("assembly", new RecipeSerializer<>(AssemblyRecipe.MAP_CODEC, AssemblyRecipe.STREAM_CODEC));

    private static <T extends Recipe<?>> RecipeType<T> registerType(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name);
        return Registry.register(BuiltInRegistries.RECIPE_TYPE, id, new RecipeType<T>() {
            @Override
            public String toString() {
                return id.toString();
            }
        });
    }

    private static <T extends Recipe<?>> RecipeSerializer<T> registerSerializer(String name,
                                                                                RecipeSerializer<T> serializer) {
        return Registry.register(BuiltInRegistries.RECIPE_SERIALIZER,
                Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, name), serializer);
    }

    public static void init() {
    }

    private ModRecipes() {
    }
}
