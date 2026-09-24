package org.alex_melan.spacereloaded.electronics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.registry.ModBlockEntities;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;
import org.alex_melan.spacereloaded.registry.ModMenus;

/**
 * Станция литографии (006, §1 шаг 8): нанесение фоторезиста центрифугой, контактное
 * экспонирование через фотошаблон, проявление. Первый шаблон задаёт изделие пластины;
 * радстойкий шаблон — только на кремний-на-сапфире. Контактная печать изнашивает шаблон
 * (50 экспозиций). Флакона фоторезиста хватает на 16 пластин. Слоты: 0 — пластина,
 * 1 — шаблон, 2 — фоторезист, 3 — выход.
 */
public class LithographyStationBlockEntity extends WaferStationBlockEntity {

    public static final int COATS_PER_RESIST = 16;

    private int coats;

    public LithographyStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LITHOGRAPHY_STATION, pos, state, ProcessMenu.Layout.LITHOGRAPHY_STATION);
    }

    @Override
    protected Job plan(ItemStack wafer) {
        WaferKind mask = WaferKind.byMask(items.get(1));
        if (mask == null || WaferKind.next(wafer) != WaferProcess.Operation.EXPOSE
                || (coats <= 0 && !items.get(2).is(ModItems.PHOTORESIST))) {
            return null;
        }
        Integer kind = wafer.get(ModDataComponents.WAFER_KIND);
        if (kind != null && kind != mask.ordinal()) {
            return null; // пластина начата другим шаблоном
        }
        if (mask.sapphire && !wafer.is(ModItems.SOS_WAFER)) {
            return null;
        }
        return new Job("expose", 100, 4, true);
    }

    @Override
    protected ItemStack preview(Job job, ItemStack wafer) {
        ItemStack next = advanced(wafer);
        next.set(ModDataComponents.WAFER_KIND, WaferKind.byMask(items.get(1)).ordinal());
        return next;
    }

    @Override
    protected ItemStack complete(ServerLevel level, Job job, ItemStack wafer) {
        ItemStack result = preview(job, wafer);
        if (coats <= 0) {
            items.get(2).shrink(1);
            coats = COATS_PER_RESIST;
        }
        coats--;
        ItemStack mask = items.get(1);
        mask.setDamageValue(mask.getDamageValue() + 1);
        if (mask.getDamageValue() >= mask.getMaxDamage()) {
            items.set(1, ItemStack.EMPTY);
        }
        return result;
    }

    @Override
    protected int reserve() {
        return coats;
    }

    @Override
    protected MenuType<ProcessMenu> menuType() {
        return ModMenus.LITHOGRAPHY_STATION;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("coats", coats);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        coats = input.getIntOr("coats", 0);
    }
}
