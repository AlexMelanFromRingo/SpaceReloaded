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

    /** 9 входов (005): сложные узлы — двигатели из деталей, электроника — требуют больше 5 ингредиентов. */
    public static final int INPUT_SLOTS = 9;
    /** До 005 входов было 5 и выход лежал в слоте 5 — миграция старых сохранений. */
    private static final int LEGACY_INPUT_SLOTS = 5;

    private final RecipeManager.CachedCheck<AssemblyRecipeInput, AssemblyRecipe> quickCheck =
            RecipeManager.createCheck(ModRecipes.ASSEMBLY);

    public AssemblyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ASSEMBLY_TABLE, pos, state, INPUT_SLOTS);
    }

    private AssemblyRecipeInput currentInput() {
        return new AssemblyRecipeInput(List.copyOf(items.subList(0, INPUT_SLOTS)));
    }

    @Override
    public void serverTick(ServerLevel level) {
        super.serverTick(level);
        // 005: «Допуск» — двигатель уровня ≥ 9 из точных деталей
        if (level.getGameTime() % 20 == 0) {
            ItemStack out = items.get(INPUT_SLOTS);
            var props = out.get(net.minecraft.core.component.DataComponents.BLOCK_STATE);
            if (props != null) {
                Integer quality = props.get(org.alex_melan.spacereloaded.rocket.EngineBlock.QUALITY);
                if (quality != null && quality >= 9) {
                    org.alex_melan.spacereloaded.industry.IndustryAdvancements.awardNearby(level, getBlockPos(), 16,
                            org.alex_melan.spacereloaded.industry.IndustryAdvancements.TOLERANCE);
                }
            }
        }
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

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("input_slots", INPUT_SLOTS);
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        if (input.getIntOr("input_slots", LEGACY_INPUT_SLOTS) == LEGACY_INPUT_SLOTS) {
            // Старый выход (слот 5) переезжает в новый выход, чтобы не стать входом
            ItemStack legacyOutput = items.get(LEGACY_INPUT_SLOTS);
            if (!legacyOutput.isEmpty() && items.get(INPUT_SLOTS).isEmpty()) {
                items.set(INPUT_SLOTS, legacyOutput);
                items.set(LEGACY_INPUT_SLOTS, ItemStack.EMPTY);
            }
        }
    }
}
