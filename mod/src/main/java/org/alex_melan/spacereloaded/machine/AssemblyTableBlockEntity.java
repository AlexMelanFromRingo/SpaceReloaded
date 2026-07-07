package org.alex_melan.spacereloaded.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.state.BlockState;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.recipe.AssemblyRecipe;
import org.alex_melan.spacereloaded.machine.recipe.AssemblyRecipeInput;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;
import org.alex_melan.spacereloaded.registry.ModRecipes;

import java.util.List;

public class AssemblyTableBlockEntity extends ProcessingMachineBlockEntity {

    public static final int INPUT_SLOTS = 5;

    private final RecipeManager.CachedCheck<AssemblyRecipeInput, AssemblyRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.ASSEMBLY);

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_TABLE, pos, state, INPUT_SLOTS);
    }

    private AssemblyRecipeInput currentInput() {
        return new AssemblyRecipeInput(List.copyOf(items.subList(0, INPUT_SLOTS)));
    }

    @Override
    protected int processingTicks() {
        return SpaceReloaded.config().assemblyTicks;
    }

    @Override
    protected ItemStack peekResult(ServerLevel level) {
        AssemblyRecipeInput input = currentInput();
        return quickCheck.getRecipeFor(input, level)
                .map(holder -> holder.value().assemble(input))
                .orElse(ItemStack.EMPTY);
    }

    @Override
    protected void consumeInputs(ServerLevel level) {
        AssemblyRecipeInput input = currentInput();
        var holder = quickCheck.getRecipeFor(input, level).orElse(null);
        if (holder == null) {
            return;
        }
        int[] assignment = holder.value().findAssignment(input);
        if (assignment == null) {
            return;
        }
        for (int slot : assignment) {
            items.get(slot).shrink(1);
        }
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.spacereloaded.assembly_table");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory playerInventory) {
        return new AssemblyTableMenu(containerId, playerInventory, this, dataAccess);
    }
}
