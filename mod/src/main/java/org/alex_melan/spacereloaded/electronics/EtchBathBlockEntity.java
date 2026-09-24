package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModMenus;

/**
 * Травильная ванна (006, §1 шаг 9): окна в SiO₂ по рисунку фоторезиста вскрываются только
 * плавиковой кислотой (SiO₂ + 6HF → H₂SiF₆ + 2H₂O); алюминиевая разводка последнего уровня
 * травится щёлочью (2Al + 2NaOH + 6H₂O → 2Na[Al(OH)₄] + 3H₂). Заправки ванны хватает на
 * 16 пластин; смена травителя сливает остаток. Слоты: 0 — пластина, 1 — травитель, 2 — выход.
 */
public class EtchBathBlockEntity extends WaferStationBlockEntity {

    public static final int WAFERS_PER_CHARGE = 16;

    private Item bath = Items.AIR;
    private int charge;

    public EtchBathBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ETCH_BATH, pos, state, ProcessMenu.Layout.ETCH_BATH);
    }

    private static Item etchantFor(WaferProcess.Operation op) {
        return switch (op) {
            case ETCH_OXIDE -> ModItems.HYDROFLUORIC_ACID;
            case ETCH_METAL -> ModItems.CAUSTIC_SODA;
            default -> null;
        };
    }

    @Override
    protected Job plan(ItemStack wafer) {
        WaferProcess.Operation op = WaferKind.next(wafer);
        Item etchant = op == null ? null : etchantFor(op);
        if (etchant == null) {
            return null;
        }
        boolean charged = bath == etchant && charge > 0;
        return charged || items.get(1).is(etchant) ? new Job(op.name(), 100, 2, true) : null;
    }

    @Override
    protected ItemStack preview(Job job, ItemStack wafer) {
        return advanced(wafer);
    }

    @Override
    protected ItemStack complete(ServerLevel level, Job job, ItemStack wafer) {
        Item etchant = etchantFor(WaferProcess.Operation.valueOf(job.key()));
        if (bath != etchant || charge <= 0) {
            items.get(1).shrink(1); // слить остаток, заправить свежим
            bath = etchant;
            charge = WAFERS_PER_CHARGE;
        }
        charge--;
        return preview(job, wafer);
    }

    @Override
    protected int reserve() {
        return charge;
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.ETCH_BATH;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("bath", BuiltInRegistries.ITEM.getKey(bath).toString());
        output.putInt("charge", charge);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        Identifier id = Identifier.tryParse(input.getStringOr("bath", "minecraft:air"));
        bath = id == null ? Items.AIR : BuiltInRegistries.ITEM.getValue(id);
        charge = input.getIntOr("charge", 0);
    }
}
