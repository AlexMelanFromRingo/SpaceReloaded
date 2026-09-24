package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.electronics.Purity;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.machine.ChemicalProcess;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModMenus;

/**
 * Диффузионная печь (006, §1 шаги 7 и 10): кварцевая труба на 1000–1100 °C.
 * <ul>
 *   <li>Окисление (маршрут фаба): Si + O₂ → SiO₂ — кислород из баллона (10 единиц на пластину).</li>
 *   <li>Металлизация (последний уровень): термическое напыление алюминия — межсоединения
 *       микросхем до 1997 года были алюминиевыми; ~1 г на пластину, слиток испарителя (1 кг)
 *       хватает на 1000 пластин.</li>
 *   <li>Диффузия эмиттера (солнечные элементы): свежая пластина без окисления (баллон не
 *       вставлен) + фосфор → n⁺-слой на p-подложке. Монокремний ≥ 6N — монокристаллический
 *       элемент (КПД 21 %), грязнее — время жизни носителей мало, КПД как у мультикремния.
 *       Доза фосфора мала: предмета хватает на 64 пластины.</li>
 * </ul>
 * Слоты: 0 — пластина, 1 — баллон O₂, 2 — алюминий, 3 — фосфор, 4 — выход.
 */
public class DiffusionFurnaceBlockEntity extends WaferStationBlockEntity {

    public static final int OXYGEN_PER_WAFER = 10;
    public static final int ALUMINIUM_GRAMS = 1000;
    public static final int PHOSPHORUS_DOSES = 64;

    private int aluminiumGrams;
    private int phosphorusDoses;

    public DiffusionFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIFFUSION_FURNACE, pos, state, ProcessMenu.Layout.DIFFUSION_FURNACE);
    }

    private boolean oxygenReady() {
        ItemStack canister = items.get(1);
        return canister.is(ModItems.OXYGEN_CANISTER)
                && canister.getMaxDamage() - canister.getDamageValue() >= OXYGEN_PER_WAFER;
    }

    private boolean fresh(ItemStack wafer) {
        return !wafer.has(ModDataComponents.WAFER_STEP) && !wafer.has(ModDataComponents.WAFER_KIND);
    }

    @Override
    protected Job plan(ItemStack wafer) {
        boolean emitterSubstrate = wafer.is(ModItems.MULTICRYSTALLINE_WAFER)
                || (wafer.is(ModItems.SILICON_WAFER) && fresh(wafer) && items.get(1).isEmpty());
        if (emitterSubstrate) {
            return phosphorusDoses > 0 || items.get(3).is(ModItems.PHOSPHORUS)
                    ? new Job("emitter", 160, 16, false) : null;
        }
        WaferProcess.Operation op = WaferKind.next(wafer);
        if (op == WaferProcess.Operation.OXIDIZE && oxygenReady()) {
            return new Job("oxidize", 200, 16, true);
        }
        if (op == WaferProcess.Operation.METALLIZE && (aluminiumGrams > 0 || items.get(2).is(ModItems.ALUMINIUM_INGOT))) {
            return new Job("metallize", 200, 12, true);
        }
        return null;
    }

    @Override
    protected ItemStack preview(Job job, ItemStack wafer) {
        if (job.key().equals("emitter")) {
            boolean mono = wafer.is(ModItems.SILICON_WAFER)
                    && ChemicalProcess.purityOf(wafer) >= Purity.SOLAR_GRADE;
            return new ItemStack(mono ? ModItems.MONO_SOLAR_CELL : ModItems.SOLAR_CELL);
        }
        return advanced(wafer);
    }

    @Override
    protected ItemStack complete(ServerLevel level, Job job, ItemStack wafer) {
        switch (job.key()) {
            case "emitter" -> {
                if (phosphorusDoses <= 0) {
                    items.get(3).shrink(1);
                    phosphorusDoses = PHOSPHORUS_DOSES;
                }
                phosphorusDoses--;
            }
            case "oxidize" -> {
                ItemStack canister = items.get(1);
                canister.setDamageValue(canister.getDamageValue() + OXYGEN_PER_WAFER);
            }
            default -> {
                if (aluminiumGrams <= 0) {
                    items.get(2).shrink(1);
                    aluminiumGrams = ALUMINIUM_GRAMS;
                }
                aluminiumGrams--;
            }
        }
        return preview(job, wafer);
    }

    @Override
    protected int reserve() {
        return aluminiumGrams;
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.DIFFUSION_FURNACE;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("aluminium_g", aluminiumGrams);
        output.putInt("phosphorus", phosphorusDoses);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        aluminiumGrams = input.getIntOr("aluminium_g", 0);
        phosphorusDoses = input.getIntOr("phosphorus", 0);
    }
}
