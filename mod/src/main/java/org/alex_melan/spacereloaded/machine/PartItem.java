package org.alex_melan.spacereloaded.machine;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.alex_melan.spacereloaded.registry.ModDataComponents;

import java.util.Locale;
import java.util.function.Consumer;

/** Полуфабрикат/деталь двигателя (005, FR-330): тултип — шаг и допуск или качество. */
public class PartItem extends Item {

    public PartItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        Integer step = stack.get(ModDataComponents.MACHINING_STEP);
        Float deltaSq = stack.get(ModDataComponents.MACHINING_DELTA_SQ);
        Float quality = stack.get(ModDataComponents.PART_QUALITY);
        if (step != null) {
            tooltip.accept(Component.translatable("tooltip.spacereloaded.part.step", step,
                    String.format(Locale.ROOT, "%.1f", deltaSq == null ? 0 : Math.sqrt(deltaSq)))
                    .withStyle(ChatFormatting.GRAY));
        }
        if (quality != null) {
            tooltip.accept(Component.translatable("tooltip.spacereloaded.part.quality",
                    String.format(Locale.ROOT, "%.2f", quality)).withStyle(ChatFormatting.AQUA));
        }
    }
}
