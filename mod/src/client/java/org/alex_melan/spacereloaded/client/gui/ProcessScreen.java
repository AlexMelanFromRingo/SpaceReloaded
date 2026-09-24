package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.electronics.ProcessMenu;

/**
 * Экран процессных машин 006 по раскладке меню: фон (слоты нарисованы генератором текстур),
 * стрелка прогресса, шкала энергии, у машин фаба — класс чистоты воздуха ISO 14644-1 и запас
 * реагента внутри машины (заряд ванны, фоторезист, алюминий испарителя).
 */
public class ProcessScreen extends AbstractContainerScreen<ProcessMenu> {

    private static final int ARROW_W = 22;
    private static final int ARROW_H = 15;
    private static final int ENERGY_X = 156;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_W = 8;
    private static final int ENERGY_H = 54;

    private final Identifier texture;

    public ProcessScreen(ProcessMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.texture = Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, menu.layout().texture());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos, 0f, 0f, imageWidth, imageHeight, 256, 256);
        ProcessMenu.Layout layout = menu.layout();
        int progress = menu.progress() * ARROW_W / menu.maxProgress();
        if (progress > 0) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + layout.arrowX, topPos + layout.arrowY,
                    176f, 0f, progress, ARROW_H, 256, 256);
        }
        int filled = ENERGY_H * menu.energy() / menu.energyCapacity();
        if (filled > 0) {
            gfx.fill(leftPos + ENERGY_X, topPos + ENERGY_Y + ENERGY_H - filled, leftPos + ENERGY_X + ENERGY_W,
                    topPos + ENERGY_Y + ENERGY_H, 0xFFE08830);
        }
        if (layout.cleanroom && menu.isoClass() > 0) {
            int iso = menu.isoClass();
            int color = iso <= 5 ? 0xFF3FA7D6 : iso <= 7 ? 0xFFD6A03F : 0xFFC0392B;
            gfx.fill(leftPos + 134, topPos + 62, leftPos + 152, topPos + 72, color);
            gfx.text(font, "ISO" + iso, leftPos + 135, topPos + 63, 0xFFFFFFFF, false);
        }
        if (isHovering(ENERGY_X - 1, ENERGY_Y - 1, ENERGY_W + 2, ENERGY_H + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
        if (isHovering(layout.arrowX, layout.arrowY, ARROW_W, ARROW_H, mouseX, mouseY)) {
            Component tip = Component.translatable("tooltip.spacereloaded.progress",
                    100 * menu.progress() / menu.maxProgress());
            if (menu.reserve() >= 0) {
                tip = Component.translatable("tooltip.spacereloaded.reserve." + layout.name().toLowerCase(java.util.Locale.ROOT),
                        100 * menu.progress() / menu.maxProgress(), menu.reserve());
            }
            gfx.setTooltipForNextFrame(font, tip, mouseX, mouseY);
        }
        if (layout.cleanroom && isHovering(134, 62, 18, 10, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.iso_class",
                    menu.isoClass()), mouseX, mouseY);
        }
    }
}
