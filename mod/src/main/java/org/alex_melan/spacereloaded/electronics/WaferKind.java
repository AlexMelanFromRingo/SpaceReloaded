package org.alex_melan.spacereloaded.electronics;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.registry.ModDataComponents;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * Изделие фаба (006, §2.1 дизайна): число уровней шаблона M, площадь кристалла A, предмет
 * кристалла, фотошаблон и подложка. Радстойкий процессор — кремний-на-сапфире: изолирующая
 * подложка не даёт частице собрать заряд с объёма кремния (защёлкивание, SEU).
 */
public enum WaferKind {
    LOGIC(5, 0.25, false),
    MICROPROCESSOR(8, 0.5, false),
    RADHARD(12, 1.0, true);

    public static final double WAFER_DIAMETER_CM = 5;

    public final int masks;
    public final double areaCm2;
    public final boolean sapphire;

    WaferKind(int masks, double areaCm2, boolean sapphire) {
        this.masks = masks;
        this.areaCm2 = areaCm2;
        this.sapphire = sapphire;
    }

    public Item die() {
        return switch (this) {
            case LOGIC -> ModItems.DIE_LOGIC;
            case MICROPROCESSOR -> ModItems.DIE_MICROPROCESSOR;
            case RADHARD -> ModItems.DIE_RADHARD;
        };
    }

    public Item mask() {
        return switch (this) {
            case LOGIC -> ModItems.PHOTOMASK_LOGIC;
            case MICROPROCESSOR -> ModItems.PHOTOMASK_MICROPROCESSOR;
            case RADHARD -> ModItems.PHOTOMASK_RADHARD;
        };
    }

    public static WaferKind byIndex(int index) {
        WaferKind[] values = values();
        return values[Math.max(0, Math.min(values.length - 1, index))];
    }

    public static WaferKind byMask(ItemStack mask) {
        for (WaferKind kind : values()) {
            if (mask.is(kind.mask())) {
                return kind;
            }
        }
        return null;
    }

    /** Пластина фаба: кремниевая или кремний-на-сапфире. */
    public static boolean isWafer(ItemStack stack) {
        return stack.is(ModItems.SILICON_WAFER) || stack.is(ModItems.SOS_WAFER);
    }

    /** Следующая операция маршрута пластины. */
    public static WaferProcess.Operation next(ItemStack wafer) {
        if (!isWafer(wafer)) {
            return null;
        }
        int step = wafer.getOrDefault(ModDataComponents.WAFER_STEP, 0);
        Integer kind = wafer.get(ModDataComponents.WAFER_KIND);
        // до первого экспонирования тип не выбран: первая операция — окисление при любом M ≥ 2
        int masks = kind == null ? Integer.MAX_VALUE / WaferProcess.OPERATIONS_PER_LEVEL : byIndex(kind).masks;
        return WaferProcess.next(step, masks);
    }
}
