package org.alex_melan.spacereloaded.machine.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

import java.util.List;

/**
 * Вход процессного рецепта: машина-исполнитель и до трёх реагентных слотов. Машина входит в
 * сопоставление — один тип рецепта обслуживает весь химический парк (реактор, печь, Сабатье,
 * электролизёр, реактор осаждения, реголитовый реактор), и рецепт одной машины не сработает
 * в другой.
 */
public record ChemicalRecipeInput(String machine, List<ItemStack> stacks) implements RecipeInput {

    public ChemicalRecipeInput(String machine, ItemStack... stacks) {
        this(machine, List.of(stacks));
    }

    @Override
    public ItemStack getItem(int index) {
        return index < stacks.size() ? stacks.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return stacks.size();
    }
}
