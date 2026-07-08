package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.registry.ModRegistries;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.List;

/**
 * Бортовой HUD пилота ракеты (замена сообщений в чат): топливо, скорость,
 * высота, ориентация, цель перелёта. Данные — synched-поля сущности;
 * имя цели — из синхронизированного datapack-реестра планет.
 */
public class RocketHud implements HudElement {

    public static final Identifier ID =
            Identifier.fromNamespaceAndPath(SpaceReloaded.MOD_ID, "rocket_hud");

    private static final int PANEL_BG = 0xB00E1418;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null
                || !(mc.player.getVehicle() instanceof RocketEntity rocket)) {
            return;
        }
        Font font = mc.font;
        boolean launched = rocket.clientLaunched();

        double fuel = rocket.clientFuelKg();
        double capacity = rocket.clientFuelCapacityKg();
        double fraction = capacity <= 0 ? 0 : Math.clamp(fuel / capacity, 0, 1);
        double speed = rocket.getDeltaMovement().length() * 20.0;
        double verticalSpeed = rocket.getDeltaMovement().y * 20.0;

        int x = 8;
        int y = 8;
        int width = 150;
        int line = font.lineHeight + 2;
        int height = 8 + 10 + 4 + line * 5 + 4;

        gfx.fill(x - 4, y - 4, x + width + 4, y + height, PANEL_BG);
        gfx.text(font, Component.translatable(launched
                ? "hud.spacereloaded.rocket.flight"
                : "hud.spacereloaded.rocket.parked"), x, y, ACCENT);
        y += font.lineHeight + 3;

        // Шкала топлива: цвет от запаса
        int barColor = fraction > 0.5 ? 0xFF57C46B : fraction > 0.2 ? 0xFFE0B23C : 0xFFDD4B4B;
        gfx.fill(x, y, x + width, y + 8, 0xFF23282C);
        gfx.fill(x + 1, y + 1, x + 1 + (int) ((width - 2) * fraction), y + 7, barColor);
        y += 12;

        gfx.text(font, Component.translatable("hud.spacereloaded.rocket.fuel",
                String.format("%.0f", fuel), String.format("%.0f", capacity)), x, y, TEXT);
        y += line;
        gfx.text(font, Component.translatable("hud.spacereloaded.rocket.speed",
                String.format("%.1f", speed), String.format("%+.1f", verticalSpeed)), x, y, TEXT);
        y += line;
        gfx.text(font, Component.translatable("hud.spacereloaded.rocket.altitude",
                String.format("%.0f", rocket.getY()),
                String.format("%.1f", rocket.pitchDeg()), String.format("%.1f", rocket.rollDeg())),
                x, y, TEXT);
        y += line;
        gfx.text(font, Component.translatable("hud.spacereloaded.rocket.destination",
                destinationName(mc, rocket)), x, y, TEXT);
        y += line;
        gfx.text(font, Component.translatable(launched
                ? "hud.spacereloaded.rocket.hint_flight"
                : "hud.spacereloaded.rocket.hint_parked"), x, y, MUTED);
    }

    /** Имя цели перелёта из синхронизированного реестра планет. */
    private Component destinationName(Minecraft mc, RocketEntity rocket) {
        Identifier here = mc.level.dimension().identifier();
        for (ModRegistries.PlanetProfile profile
                : mc.level.registryAccess().lookupOrThrow(ModRegistries.PLANETS)) {
            if (!profile.dimension().equals(here)) {
                continue;
            }
            List<Identifier> targets = profile.transitionTargets();
            if (targets.isEmpty()) {
                break;
            }
            Identifier target = targets.get(
                    Math.floorMod(rocket.clientDestinationIndex(), targets.size()));
            return Component.translatable("planet.spacereloaded." + target.getPath());
        }
        return Component.translatable("hud.spacereloaded.rocket.no_destination");
    }
}
