package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.machine.GeneratorMenu;

/** Экран угольного генератора: пламя горения + шкала энергии. */
public class GeneratorScreen extends AbstractContainerScreen<GeneratorMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(
            SpaceReloaded.MOD_ID, "textures/gui/generator.png");

    private static final int FLAME_U = 176;
    private static final int FLAME_V = 16;
    private static final int FLAME_SIZE = 14;
    private static final int FLAME_X = 81;
    private static final int FLAME_Y = 18;

    private static final int ENERGY_X = 156;
    private static final int ENERGY_Y = 17;
    private static final int ENERGY_WIDTH = 8;
    private static final int ENERGY_HEIGHT = 54;
    private static final int ENERGY_COLOR = 0xFFE08830;

    public GeneratorScreen(GeneratorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, leftPos, topPos,
                0.0f, 0.0f, imageWidth, imageHeight, 256, 256);

        // Пламя выгорает снизу вверх, как у ванильной печи
        if (menu.burnTime() > 0) {
            int height = Math.max(1, FLAME_SIZE * menu.burnTime() / menu.burnDuration());
            gfx.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                    leftPos + FLAME_X, topPos + FLAME_Y + (FLAME_SIZE - height),
                    (float) FLAME_U, (float) (FLAME_V + (FLAME_SIZE - height)),
                    FLAME_SIZE, height, 256, 256);
        }

        int filled = ENERGY_HEIGHT * menu.energy() / menu.energyCapacity();
        if (filled > 0) {
            gfx.fill(leftPos + ENERGY_X, topPos + ENERGY_Y + (ENERGY_HEIGHT - filled),
                    leftPos + ENERGY_X + ENERGY_WIDTH, topPos + ENERGY_Y + ENERGY_HEIGHT, ENERGY_COLOR);
        }
        if (isHovering(ENERGY_X - 1, ENERGY_Y - 1, ENERGY_WIDTH + 2, ENERGY_HEIGHT + 2, mouseX, mouseY)) {
            gfx.setTooltipForNextFrame(font, Component.translatable("tooltip.spacereloaded.energy",
                    menu.energy(), menu.energyCapacity()), mouseX, mouseY);
        }
    }
}
