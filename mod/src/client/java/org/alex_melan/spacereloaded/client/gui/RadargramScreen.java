package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.alex_melan.spacereloaded.core.survey.GroundRadar;
import org.alex_melan.spacereloaded.survey.GroundRadarService;
import org.alex_melan.spacereloaded.survey.RadargramData;

import java.util.Locale;

/**
 * Радарограмма (009, US4, FR-742): по горизонтали — путь ровера (трасса на 0.5 м), по вертикали —
 * время двойного пробега, переведённое в глубину по ε верхнего слоя. Яркие полосы — границы сред
 * (кровля льда, рудная жила, потолок лавовой трубки); тёмное — среда без отражений или сигнал
 * погас в затухании.
 */
public class RadargramScreen extends Screen {

    private static final int BG = 0xE00A0E12;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int MUTED = 0xFF9AA8AE;

    private final RadargramData data;

    public RadargramScreen(RadargramData data) {
        super(Component.translatable("screen.spacereloaded.radargram"));
        this.data = data;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.spacereloaded.porkchop.back"), b -> onClose())
                .bounds(width - 110, height - 28, 90, 20).build());
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.fill(0, 0, width, height, BG);
        int traces = Math.max(1, data.traces());
        double lengthM = traces * GroundRadarService.STEP_M;
        double depthM = GroundRadar.depthAt(data.samples() - 1, data.epsilon());
        gfx.text(font, getTitle(), 16, 12, ACCENT);
        gfx.text(font, Component.translatable("screen.spacereloaded.radargram.info",
                String.format(Locale.ROOT, "%.1f", lengthM), String.format(Locale.ROOT, "%.1f", data.epsilon()),
                String.format(Locale.ROOT, "%.0f", depthM)), 16, 26, MUTED);
        int x0 = 48, y0 = 44, x1 = width - 24, y1 = height - 44;
        gfx.outline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, FRAME);
        for (int t = 0; t < data.traces(); t++) {
            int cx0 = x0 + (x1 - x0) * t / traces, cx1 = Math.max(cx0 + 1, x0 + (x1 - x0) * (t + 1) / traces);
            for (int k = 0; k < data.samples(); k++) {
                int cy0 = y0 + (y1 - y0) * k / data.samples(), cy1 = Math.max(cy0 + 1, y0 + (y1 - y0) * (k + 1) / data.samples());
                int v = (int) Math.round(Math.sqrt(data.at(t, k)) * 255);   // корень — видны слабые эхо
                if (v > 0) {
                    gfx.fill(cx0, cy0, cx1, cy1, 0xFF000000 | (v << 16) | (v << 8) | Math.min(255, v + 20));
                }
            }
        }
        // шкала глубины через 5 м
        for (double d = 0; d <= depthM; d += 5) {
            int y = y0 + (int) ((y1 - y0) * d / depthM);
            gfx.fill(x0 - 4, y, x0, y + 1, MUTED);
            gfx.text(font, String.format(Locale.ROOT, "%.0f", d), x0 - 8 - font.width(String.format(Locale.ROOT, "%.0f", d)),
                    y - 4, MUTED);
        }
        gfx.text(font, Component.translatable("screen.spacereloaded.radargram.axis"), x0, y1 + 4, MUTED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
