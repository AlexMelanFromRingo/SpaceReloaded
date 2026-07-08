package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModItems;

/**
 * HUD кислорода: когда надета маска — суммарный запас всех баллонов
 * в инвентаре (правый верхний угол). Прочность баллона = кислород.
 */
public class OxygenHud implements HudElement {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "oxygen_hud");

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null
                || !mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK)) {
            return;
        }
        int oxygen = 0;
        int capacity = 0;
        var inventory = mc.player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.OXYGEN_CANISTER)) {
                oxygen += stack.getMaxDamage() - stack.getDamageValue();
                capacity += stack.getMaxDamage();
            }
        }
        Font font = mc.font;
        int width = 92;
        int x = mc.getWindow().getGuiScaledWidth() - width - 8;
        int y = 8;
        double fraction = capacity <= 0 ? 0 : (double) oxygen / capacity;
        int barColor = fraction > 0.5 ? 0xFF57C4C4 : fraction > 0.2 ? 0xFFE0B23C : 0xFFDD4B4B;

        gfx.fill(x - 4, y - 4, x + width + 4, y + 10 + font.lineHeight + 4, 0xB00E1418);
        gfx.fill(x, y, x + width, y + 6, 0xFF23282C);
        gfx.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * Math.clamp(fraction, 0, 1)), y + 5, barColor);
        gfx.text(font, Component.translatable("hud.spacereloaded.oxygen", oxygen, capacity),
                x, y + 9, capacity > 0 ? 0xFFE8EEF0 : 0xFFDD4B4B);
    }
}
