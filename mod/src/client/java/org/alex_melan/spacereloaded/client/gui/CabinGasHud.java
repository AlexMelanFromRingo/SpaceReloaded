package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.lifesupport.Metabolism;
import org.alex_melan.spacereloaded.network.CabinGasPayload;
import org.alex_melan.spacereloaded.registry.ModItems;

import java.util.Locale;

/**
 * HUD газа кабины (007): давление, pO₂ и pCO₂ зоны, в которой стоит игрок; цвет — по порогам
 * NASA (pCO₂ 0.53 / 1 кПа, pO₂ 19.5 / 16 кПа). Под индикатором кислорода маски, если она надета.
 */
public class CabinGasHud implements HudElement {

    public static final Identifier ID = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "cabin_gas_hud");

    private static CabinGasPayload last;
    private static long receivedMs;

    public static void update(CabinGasPayload payload) {
        last = payload.pressure() < 0 ? null : payload;
        receivedMs = System.currentTimeMillis();
    }

    /** Последний газ зоны (null — вне зоны). */
    public static CabinGasPayload current() {
        return last;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        CabinGasPayload gas = last;
        if (mc.player == null || gas == null || System.currentTimeMillis() - receivedMs > 3000) {
            return;
        }
        var font = mc.font;
        Component line = Component.translatable("hud.spacereloaded.cabin",
                String.format(Locale.ROOT, "%.1f", gas.pressure()),
                String.format(Locale.ROOT, "%.1f", gas.pO2()),
                String.format(Locale.ROOT, "%.2f", gas.pCo2()));
        int width = font.width(line);
        int x = mc.getWindow().getGuiScaledWidth() - width - 8;
        int y = mc.player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.OXYGEN_MASK) ? 8 + 10 + font.lineHeight + 12 : 8;
        int color = gas.pCo2() >= Metabolism.CO2_LEVELS[1] || gas.pO2() < Metabolism.O2_LEVELS[1] ? 0xFFDD4B4B
                : gas.pCo2() >= Metabolism.CO2_LEVELS[0] || gas.pO2() < Metabolism.O2_LEVELS[0] ? 0xFFE0B23C : 0xFF8FE0A8;
        gfx.fill(x - 4, y - 4, x + width + 4, y + font.lineHeight + 3, 0xB00E1418);
        gfx.text(font, line, x, y, color);
    }
}
