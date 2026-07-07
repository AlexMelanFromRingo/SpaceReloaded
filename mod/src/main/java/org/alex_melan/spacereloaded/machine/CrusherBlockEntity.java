package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.recipe.CrusherRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModRecipes;

public class CrusherBlockEntity extends ProcessingMachineBlockEntity {

    private final RecipeManager.CachedCheck<SingleRecipeInput, CrusherRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.CRUSHING);

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CRUSHER, pos, state, 1);
    }

    @Override
    protected int processingTicks() {
        return SpaceReloaded.config().crusherTicks;
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        SingleRecipeInput input = new SingleRecipeInput(items.get(0));
        return quickCheck.getRecipeFor(input, level)
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
        items.get(0).shrink(1);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.crusher");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new SingleInputMachineMenu(ModMenus.CRUSHER, containerId, playerInventory, this, dataAccess);
    }
}
