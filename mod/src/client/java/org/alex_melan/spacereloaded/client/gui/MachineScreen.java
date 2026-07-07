package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.machine.MachineMenu;

/**
 * Экран станка (26.2: extract-пайплайн, GuiGraphicsExtractor вместо GuiGraphics).
 * Рисует фон, стрелку прогресса (частичный blit из той же текстуры, регион
 * (176,0) 22×15) и вертикальную шкалу энергии заливкой.
 */
public class MachineScreen extends AbstractContainerScreen<MachineMenu> {

    private static final int ARROW_U = 176;
    private static final int ARROW_V = 0;
    private static final int ARROW_WIDTH = 22;
    private static final int ARROW_HEIGHT = 15;

    private static final int ENERGY_X = 156;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_WIDTH = 8;
    private static final int ENERGY_HEIGHT = 54;
    private static final int ENERGY_COLOR = 0xFFE08830;

    private final Identifier texture;
    private final int arrowX;
    private final int arrowY;

    public MachineScreen(MachineMenu menu, Inventory playerInventory, Component title,
                         Identifier texture, int arrowX, int arrowY) {
        super(menu, playerInventory, title);
        this.texture = texture;
        this.arrowX = arrowX;
        this.arrowY = arrowY;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos, topPos,
                0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        int progressWidth = menu.progress() * ARROW_WIDTH / menu.maxProgress();
        if (progressWidth > 0) {
            gfx.blit(RenderPipelines.GUI_TEXTURED, texture, leftPos + arrowX, topPos + arrowY,
                    (float) ARROW_U, (float) ARROW_V, progressWidth, ARROW_HEIGHT, 256, 256);
        }

        int filled = ENERGY_HEIGHT * menu.energy() / menu.energyCapacity();
        if (filled > 0) {
            gfx.fill(leftPos + ENERGY_X, topPos + ENERGY_Y + (ENERGY_HEIGHT - filled),
                    leftPos + ENERGY_X + ENERGY_WIDTH, topPos + ENERGY_Y + ENERGY_HEIGHT, ENERGY_COLOR);
        }

        // Тултипы: значения энергии на шкале, проценты на стрелке
        if (isHovering(ENERGY_X - 1, ENERGY_Y - 1, ENERGY_WIDTH + 2, ENERGY_HEIGHT + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
        if (isHovering(arrowX, arrowY, ARROW_WIDTH, ARROW_HEIGHT, mouseX, mouseY)) {
            int percent = 100 * menu.progress() / menu.maxProgress();
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.progress",
                    percent), mouseX, mouseY);
        }
    }
}
