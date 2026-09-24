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
 * Реактор осаждения Сименса (006, FR-412): колпак над U-образными стержнями кремния, нагретыми
 * током до 1100 °C; SiHCl₃ + H₂ → Si + 3HCl — поликремний нарастает на стержнях, чистота
 * наследуется от ректифицированного трихлорсилана. Водород — из льда встроенным электролизом
 * (как у реактора Сабатье); HCl возвращается в цикл (выход 95 % — дробная часть копится).
 * Эпитаксия кремния на сапфир (кремний-на-сапфире, T3) — та же реакция на сапфировой подложке.
 * Самый энергоёмкий шаг цепочки: ≈ 70 кВт·ч на килограмм. Слоты: 0 — ТХС, 1 — лёд, 2 — подложка,
 * 3–4 — продукты.
 */
public class DepositionReactorBlockEntity extends ProcessMachineBlockEntity {

    private final ChemicalProcess process = new ChemicalProcess(ChemicalRecipe.DEPOSITION_REACTOR);

    public DepositionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DEPOSITION_REACTOR, pos, state, ProcessMenu.Layout.DEPOSITION_REACTOR, 60_000, 1200);
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
        return ModMenus.DEPOSITION_REACTOR;
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
