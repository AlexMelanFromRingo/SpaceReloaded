package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.alex_melan.spacereloaded.network.ScanReportPayload;

import java.util.Locale;

/** Скан-отчёт стартового комплекса: те же цифры, но читаемые. */
public class ScanReportScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int BG = 0xE00E1418;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;
    private static final int WARN = 0xFFDD4B4B;
    private static final int GOOD = 0xFF57C46B;

    private final ScanReportPayload report;

    public ScanReportScreen(ScanReportPayload report) {
        super(Component.translatable("screen.spacereloaded.scan"));
        this.report = report;
    }

    private int panelHeight() {
        return 122 + report.warnings().size() * (font.lineHeight + 2);
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.cannon.close"), button -> onClose())
                .bounds((width - 100) / 2, (height + panelHeight()) / 2 - 26, 100, 20)
                .build());
    }

    /** Рисуем в слое фона: виджеты кладутся поверх, иначе панель их закроет. */
    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        int panelH = panelHeight();
        int x = (width - PANEL_W) / 2;
        int y = (height - panelH) / 2;
        gfx.fill(x, y, x + PANEL_W, y + panelH, BG);
        gfx.outline(x, y, PANEL_W, panelH, FRAME);

        gfx.text(font, getTitle(), x + 12, y + 10, ACCENT);
        int line = font.lineHeight + 4;
        int row = y + 10 + line + 4;

        if (!report.error().isEmpty()) {
            gfx.text(font, Component.translatable(report.error(), ""), x + 12, row, WARN);
            return;
        }

        gfx.text(font, Component.translatable("screen.spacereloaded.scan.blocks", report.blocks()),
                x + 12, row, MUTED);
        row += line;
        gfx.text(font, Component.translatable("screen.spacereloaded.scan.mass",
                        fmt(report.massKg()), fmt(report.dryMassKg())), x + 12, row, TEXT);
        row += line;
        gfx.text(font, Component.translatable("screen.spacereloaded.scan.thrust",
                fmt(report.thrustN() / 1000.0)), x + 12, row, TEXT);
        row += line;

        boolean liftsOff = report.twr() > 1.0;
        gfx.text(font, Component.translatable("screen.spacereloaded.scan.twr",
                        String.format(Locale.ROOT, "%.2f", report.twr())),
                x + 12, row, liftsOff ? GOOD : WARN);
        row += line;

        boolean enough = report.requiredDeltaV() <= 0 || report.deltaV() >= report.requiredDeltaV();
        gfx.text(font, Component.translatable("screen.spacereloaded.scan.dv",
                        fmt(report.deltaV()), fmt(report.requiredDeltaV())),
                x + 12, row, enough ? GOOD : WARN);
        row += line + 2;

        Component verdict = !liftsOff
                ? Component.translatable("screen.spacereloaded.scan.verdict.grounded")
                : enough ? Component.translatable("screen.spacereloaded.scan.verdict.ready")
                        : Component.translatable("screen.spacereloaded.scan.verdict.short");
        gfx.text(font, verdict, x + 12, row, liftsOff && enough ? GOOD : WARN);
        row += line;

        for (String warning : report.warnings()) {
            gfx.text(font, Component.translatable("message.spacereloaded.rocket.warning." + warning),
                    x + 12, row, WARN);
            row += font.lineHeight + 2;
        }
    }

    private static String fmt(double value) {
        return String.format(Locale.ROOT, "%.0f", value);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
