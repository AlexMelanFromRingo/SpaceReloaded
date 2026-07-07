package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.energy.BatteryMenu;

/** Экран аккумулятора: большая шкала заряда + числовое значение. */
public class BatteryScreen extends AbstractContainerScreen<BatteryMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            SpaceReloaded.MOD_ID, "textures/gui/battery.png");

    private static final int BAR_X = 79;
    private static final int BAR_Y = 20;
    private static final int BAR_WIDTH = 18;
    private static final int BAR_HEIGHT = 50;
    private static final int BAR_COLOR = 0xFF60C66C;

    public BatteryScreen(BatteryMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        int filled = BAR_HEIGHT * menu.energy() / menu.energyCapacity();
        if (filled > 0) {
            gfx.fill(leftPos + BAR_X, topPos + BAR_Y + (BAR_HEIGHT - filled),
                    leftPos + BAR_X + BAR_WIDTH, topPos + BAR_Y + BAR_HEIGHT, BAR_COLOR);
        }
        int percent = 100 * menu.energy() / menu.energyCapacity();
        gfx.centeredText(font, Component.literal(percent + "%"),
                leftPos + imageWidth / 2, topPos + 44, 0xFFFFFFFF);

        if (isHovering(BAR_X - 1, BAR_Y - 1, BAR_WIDTH + 2, BAR_HEIGHT + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
    }
}
