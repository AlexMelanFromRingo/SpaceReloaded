package org.alex_melan.spacereloaded.sealing;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

/** Кислородный баллон: запас в тултипе (прочность = кислород). */
public class OxygenCanisterItem extends Item {

    public OxygenCanisterItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context,
                                TooltipDisplay display, Consumer<Component> output,
                                TooltipFlag flag) {
        super.appendHoverText(stack, context, display, output, flag);
        int max = stack.getMaxDamage();
        output.accept(Component.translatable("tooltip.spacereloaded.oxygen_canister",
                max - stack.getDamageValue(), max).withStyle(ChatFormatting.AQUA));
    }
}
