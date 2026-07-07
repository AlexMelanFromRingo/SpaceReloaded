package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.RefineryMenu;

/** Экран перегонного куба: прогресс + шкалы энергии (оранж) и топлива (голубая). */
public class RefineryScreen extends AbstractContainerScreen<RefineryMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            SpaceReloaded.MOD_ID, "textures/gui/refinery.png");

    public RefineryScreen(RefineryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        int progressWidth = menu.progress() * 22 / menu.maxProgress();
        if (progressWidth > 0) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos + 72, topPos + 35,
                    176.0f, 0.0f, progressWidth, 15, 256, 256);
        }
        int energyFill = 54 * menu.energy() / menu.energyCapacity();
        if (energyFill > 0) {
            gfx.fill(leftPos + 156, topPos + 17 + (54 - energyFill),
                    leftPos + 164, topPos + 71, 0xFFE08830);
        }
        int fuelFill = 54 * menu.fuel() / menu.fuelCapacity();
        if (fuelFill > 0) {
            gfx.fill(leftPos + 134, topPos + 17 + (54 - fuelFill),
                    leftPos + 142, topPos + 71, 0xFF60C4DC);
        }
        if (isHovering(155, 16, 10, 56, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
        if (isHovering(133, 16, 10, 56, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.fuel",
                    menu.fuel(), menu.fuelCapacity()), mouseX, mouseY);
        }
    }
}
