package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.recipe.ElectricFurnaceRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModRecipes;

public class ElectricFurnaceBlockEntity extends ProcessingMachineBlockEntity {

    private final RecipeManager.CachedCheck<SingleRecipeInput, ElectricFurnaceRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.ELECTRIC_SMELTING);
    /** Фолбэк: обычные печные рецепты — электропечь умеет всё, что ваниль, но быстрее. */
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> vanillaCheck =
            RecipeManager.createCheck(RecipeType.SMELTING);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state, 1);
    }

    @Override
    protected int processingTicks() {
        return SpaceReloaded.config().electricFurnaceTicks;
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        SingleRecipeInput input = new SingleRecipeInput(items.get(0));
        ItemStack own = quickCheck.getRecipeFor(input, level)
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
        if (!own.isEmpty()) {
            return own;
        }
        return vanillaCheck.getRecipeFor(input, level)
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
        items.get(0).shrink(1);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.electric_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new SingleInputMachineMenu(ModMenus.ELECTRIC_FURNACE, containerId, playerInventory, this, dataAccess);
    }
}
