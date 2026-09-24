package org.alex_melan.spacereloaded.electronics;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.alex_melan.spacereloaded.core.electronics.DieYield;
import org.alex_melan.spacereloaded.core.electronics.WaferProcess;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

import java.util.Locale;
import java.util.function.Consumer;

/** Предмет технологической цепочки 006: подсказка — чистота (N), слои и дефекты пластины, тир программы. */
public class MaterialItem extends Item {

    public MaterialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Float purity = stack.get(ModDataComponents.PURITY);
        if (purity != null) {
            tooltip.accept(Component.translatable("tooltip.spacereloaded.purity",
                    String.format(Locale.ROOT, "%.2f", purity)).withStyle(ChatFormatting.AQUA));
        }
        if (!WaferKind.isWafer(stack)) {
            return;
        }
        Integer kindIndex = stack.get(ModDataComponents.WAFER_KIND);
        int step = stack.getOrDefault(ModDataComponents.WAFER_STEP, 0);
        var next = WaferKind.next(stack);
        if (kindIndex == null) {
            tooltip.accept(Component.translatable("tooltip.spacereloaded.wafer.blank",
                    Component.translatable("wafer_op.spacereloaded." + next.name().toLowerCase(Locale.ROOT)))
                    .withStyle(ChatFormatting.GRAY));
            return;
        }
        WaferKind kind = WaferKind.byIndex(kindIndex);
        float defects = stack.getOrDefault(ModDataComponents.WAFER_DEFECTS, 0f);
        double total = defects + DieYield.impurityDefects(org.alex_melan.spacereloaded.machine.ChemicalProcess.purityOf(stack));
        tooltip.accept(Component.translatable("tooltip.spacereloaded.wafer",
                Component.translatable(kind.die().getDescriptionId()),
                Math.min(kind.masks, step / WaferProcess.OPERATIONS_PER_LEVEL + 1), kind.masks,
                Component.translatable("wafer_op.spacereloaded." + next.name().toLowerCase(Locale.ROOT)))
                .withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.spacereloaded.wafer.yield",
                String.format(Locale.ROOT, "%.3f", total),
                String.format(Locale.ROOT, "%.0f", 100 * DieYield.yield(total, kind.areaCm2)))
                .withStyle(ChatFormatting.DARK_AQUA));
    }
}
