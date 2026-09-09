package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
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
        int width = 170;
        int line = font.lineHeight + 2;
        boolean multiStage = rocket.clientStageCount() > 1;
        boolean heating = launched && rocket.clientHeating();
        // 003 (FR-104): цена следующего хопа против остатка Δv стека
        double transferCost = rocket.clientTransferDeltaV();
        boolean showTransfer = transferCost > 0;
        int lines = 5 + (multiStage ? 1 : 0) + (launched ? 1 : 0) + (heating ? 1 : 0) + (showTransfer ? 1 : 0);
        int height = 8 + 10 + 4 + line * lines + 4;

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
        // Полёт 2.0: ступень, её топливо и остаток Δv стека
        if (multiStage) {
            gfx.text(font, Component.translatable("hud.spacereloaded.rocket.stage",
                    rocket.clientStage() + 1, rocket.clientStageCount(),
                    String.format("%.0f", rocket.clientStageFuelKg()),
                    String.format("%.0f", rocket.clientDeltaV())), x, y, ACCENT);
            y += line;
        }
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
        if (showTransfer) {
            double have = rocket.clientDeltaV();
            gfx.text(font, Component.translatable("hud.spacereloaded.rocket.transfer",
                    String.format("%.0f", transferCost), String.format("%.0f", have)),
                    x, y, transferCost > have ? 0xFFDD4B4B : TEXT);
            y += line;
        }
        // Полёт 2.0 (FR-074): фактический и командуемый наклон; без гиродинов — пояснение
        if (launched) {
            double actual = Math.hypot(rocket.pitchDeg(), rocket.rollDeg());
            double commanded = Math.hypot(rocket.clientCmdPitchDeg(), rocket.clientCmdRollDeg());
            if (commanded > 0.5 && !rocket.clientHasGyro()) {
                gfx.text(font, Component.translatable("hud.spacereloaded.rocket.no_gyro"), x, y, 0xFFDD4B4B);
            } else {
                gfx.text(font, Component.translatable("hud.spacereloaded.rocket.attitude",
                        String.format("%.0f", actual), String.format("%.0f", commanded)), x, y, TEXT);
            }
            y += line;
        }
        gfx.text(font, Component.translatable(launched
                ? "hud.spacereloaded.rocket.hint_flight"
                : "hud.spacereloaded.rocket.hint_parked"), x, y, MUTED);
    }

    /** Имя финальной цели: индекс по полному списку планет (как на сервере). */
    private Component destinationName(Minecraft mc, RocketEntity rocket) {
        var ids = org.alex_melan.spacereloaded.planet.Navigation.planetIds(mc.level.registryAccess());
        if (ids.isEmpty()) {
            return Component.translatable("hud.spacereloaded.rocket.no_destination");
        }
        Identifier target = ids.get(Math.floorMod(rocket.clientDestinationIndex(), ids.size()));
        return Component.translatable("planet.spacereloaded." + target.getPath());
    }

}
