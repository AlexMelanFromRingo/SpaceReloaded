package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.ElectrolyzerMenu;

/** Экран электролизёра: прогресс, энергия (оранжевая), топливо (голубая). */
public class ElectrolyzerScreen extends AbstractContainerScreen<ElectrolyzerMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            SpaceReloaded.MOD_ID, "textures/gui/electrolyzer.png");

    private static final int ARROW_X = 76;
    private static final int ARROW_Y = 35;
    private static final int ARROW_W = 22;
    private static final int ARROW_H = 15;

    private static final int ENERGY_X = 156;
    private static final int FUEL_X = 134;
    private static final int BAR_Y = 17;
    private static final int BAR_W = 8;
    private static final int BAR_H = 54;

    public ElectrolyzerScreen(ElectrolyzerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        int progressWidth = menu.progress() * ARROW_W / menu.maxProgress();
        if (progressWidth > 0) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + ARROW_X, topPos + ARROW_Y,
                    176.0f, 0.0f, progressWidth, ARROW_H, 256, 256);
        }
        int energyFill = BAR_H * menu.energy() / menu.energyCapacity();
        if (energyFill > 0) {
            gfx.fill(leftPos + ENERGY_X, topPos + BAR_Y + (BAR_H - energyFill),
                    leftPos + ENERGY_X + BAR_W, topPos + BAR_Y + BAR_H, 0xFFE08830);
        }
        int fuelFill = BAR_H * menu.fuel() / menu.fuelCapacity();
        if (fuelFill > 0) {
            gfx.fill(leftPos + FUEL_X, topPos + BAR_Y + (BAR_H - fuelFill),
                    leftPos + FUEL_X + BAR_W, topPos + BAR_Y + BAR_H, 0xFF60C4DC);
        }

        if (isHovering(ENERGY_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
        if (isHovering(FUEL_X - 1, BAR_Y - 1, BAR_W + 2, BAR_H + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.fuel",
                    menu.fuel(), menu.fuelCapacity()), mouseX, mouseY);
        }
    }
}
