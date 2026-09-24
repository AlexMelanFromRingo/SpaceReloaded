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
import net.minecraft.world.inventory.ContainerData;
import org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe;
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

    /** Процессы с побочным продуктом (карботермия → CO, обжиг сподумена, Вёлер → CO, …). */
    private final ChemicalProcess process = new ChemicalProcess(ChemicalRecipe.ELECTRIC_FURNACE);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE, pos, state, 1, 2, false);
    }

    @Override
    public void serverTick(ServerLevel level) {
        if (process.working() || process.find(level, items.get(0)).isPresent()) {
            if (level.getGameTime() % 20 == 0) {
                org.alex_melan.spacereloaded.energy.EnergyUtil.ensureAdjacentCableNetworks(level, getBlockPos());
            }
            process.tick(level, items, new int[] {0}, outputSlotIndices(), energy, 1, oxygen -> oxygen == 0);
            progress = process.progress();
            setChanged();
            return;
        }
        super.serverTick(level);
    }

    @Override
    public ContainerData dataAccess() {
        return furnaceData;
    }

    private final ContainerData furnaceData = new ContainerData() {
        @Override
        public int get(int index) {
            boolean chem = process.working();
            return switch (index) {
                case 0 -> chem ? process.progress() : progress;
                case 1 -> chem ? process.maxProgress() : processingTicks();
                case 2 -> (int) Math.min(Integer.MAX_VALUE, energy.amount);
                case 3 -> (int) Math.min(Integer.MAX_VALUE, energy.capacity);
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return 4;
        }
    };

    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        process.save(output.child("process"));
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        process.load(input.childOrEmpty("process"));
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
        return new SingleInputMachineMenu(ModMenus.ELECTRIC_FURNACE, containerId, playerInventory, this, furnaceData);
    }
}
