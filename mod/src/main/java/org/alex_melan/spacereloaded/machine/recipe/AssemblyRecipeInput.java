package org.alex_melan.spacereloaded.machine.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/** Вход сборочного стола: снимок входных слотов. */
public record AssemblyRecipeInput(List<ItemStack> items) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return items.get(index);
    }

    @Override
    public int size() {
        return items.size();
    }
}
