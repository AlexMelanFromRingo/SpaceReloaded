package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.machine.recipe.ChemicalRecipe;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModMenus;

/**
 * Химический реактор (006, FR-410, D60) — мокрая химия: серная кислота (контактный процесс),
 * HF из флюорита, выщелачивание глинозёма, криолит, хлорид лития, эпоксид, фоторезист, рассол.
 * Слоты 0–2 — реагенты, 3–5 — продукты; исполнитель — {@link ChemicalProcess} (рецепты {@code spacereloaded:chemical}
 * с {@code machine = chemical_reactor}).
 */
public class ChemicalReactorBlockEntity extends ProcessMachineBlockEntity {

    private final ChemicalProcess process = new ChemicalProcess(ChemicalRecipe.CHEMICAL_REACTOR);

    public ChemicalReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CHEMICAL_REACTOR, pos, state, ProcessMenu.Layout.CHEMICAL_REACTOR, 40_000, 800);
    }

    @Override
    protected void tick(ServerLevel level) {
        int before = process.progress();
        process.tick(level, items, layout.inputs(), layout.outputs(), energy, 1, oxygen -> oxygen == 0);
        if (process.progress() != before) {
            setChanged();
        }
    }

    @Override
    protected int progress() {
        return process.progress();
    }

    @Override
    protected int maxProgress() {
        return process.maxProgress();
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.CHEMICAL_REACTOR;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        process.save(output);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        process.load(input);
    }
}
