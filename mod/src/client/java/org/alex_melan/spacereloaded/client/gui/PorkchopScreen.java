package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.SpaceReloaded;
import org.alex_melan.spacereloaded.core.orbit.GameCalendar;
import org.alex_melan.spacereloaded.core.orbit.InterplanetaryTransfer;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.planet.TransferCosts;

import java.util.Locale;

/**
 * «Свиная отбивная» (porkchop, 009, US2, FR-722): цена межпланетного плеча на синодический период
 * вперёд. Слева — нижняя кромка Δv(дата отлёта) (лучшее время полёта на каждую дату) с отметками
 * «сегодня» и «минимум»; справа — тепловая карта «дата отлёта × время полёта» с изолиниями через
 * 0.5 км/с. Всё считается на клиенте по синхронизированным профилям — те же формулы, что спишет
 * переход; сетка 64 × 40 решений Ламберта — один раз при открытии.
 */
public class PorkchopScreen extends Screen {

    private static final int BG = 0xE00A0E12;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;
    private static final int GOOD = 0xFF57C46B;
    private static final int COLS = 64;
    private static final int ROWS = 40;
    /** Шкала тепловой карты: от минимума до минимума + 8 км/с. */
    private static final double RANGE_MS = 8000;

    private final Screen parent;
    private final Identifier from;
    private final Identifier to;
    private long now;
    private long span;
    private final double[] edge = new double[COLS];
    private final double[] edgeTof = new double[COLS];
    private final double[][] grid = new double[COLS][ROWS];
    private double tofLo;
    private double tofHi;
    private double min = Double.POSITIVE_INFINITY;
    private int minCol;

    public PorkchopScreen(Screen parent, Identifier from, Identifier to) {
        super(Component.translatable("screen.spacereloaded.porkchop"));
        this.parent = parent;
        this.from = from;
        this.to = to;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("screen.spacereloaded.porkchop.back"),
                b -> Minecraft.getInstance().setScreenAndShow(parent)).bounds(width - 110, height - 28, 90, 20).build());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        var access = mc.level.registryAccess();
        var a = PlanetManager.profileById(access, from).flatMap(TransferCosts::body);
        var b = PlanetManager.profileById(access, to).flatMap(TransferCosts::body);
        var fromProfile = PlanetManager.profileById(access, from);
        if (a.isEmpty() || b.isEmpty() || fromProfile.isEmpty()) {
            return;
        }
        now = mc.level.getGameTime();
        span = Math.max(1, TransferCosts.synodicTicks(access, fromProfile.get(), to));
        double th = InterplanetaryTransfer.hohmannDays(a.get(), b.get());
        tofLo = 0.4 * th;
        tofHi = 1.6 * th;
        double scale = SpaceReloaded.config().transferDeltaVScale;
        min = Double.POSITIVE_INFINITY;
        for (int c = 0; c < COLS; c++) {
            long tick = now + span * c / (COLS - 1);
            var best = TransferCosts.option(access, fromProfile.get(), to, tick);
            edge[c] = best.map(o -> o.totalMs() * scale).orElse(Double.POSITIVE_INFINITY);
            edgeTof[c] = best.map(InterplanetaryTransfer.Option::tofDays).orElse(0.0);
            if (edge[c] < min) {
                min = edge[c];
                minCol = c;
            }
            double day = TransferCosts.day(tick);
            for (int r = 0; r < ROWS; r++) {
                double tof = tofLo + (tofHi - tofLo) * r / (ROWS - 1);
                grid[c][r] = InterplanetaryTransfer.cost(a.get(), b.get(), day, tof).totalMs() * scale;
            }
        }
    }

    private static String kms(double ms) {
        return Double.isFinite(ms) ? String.format(Locale.ROOT, "%.2f", ms / 1000) : "∞";
    }

    private double daysAt(int col) {
        return (double) span * col / (COLS - 1) / GameCalendar.TICKS_PER_GAME_DAY;
    }

    /** Палитра тепловой карты: синий (дёшево) → зелёный → жёлтый → красный (дорого). */
    private static int heat(double f) {
        f = Math.max(0, Math.min(1, f));
        float r, g, b;
        if (f < 1 / 3f) {
            float t = (float) (f * 3);
            r = 0.10f; g = 0.25f + 0.55f * t; b = 0.75f - 0.45f * t;
        } else if (f < 2 / 3f) {
            float t = (float) (f * 3 - 1);
            r = 0.10f + 0.80f * t; g = 0.80f; b = 0.30f - 0.20f * t;
        } else {
            float t = (float) (f * 3 - 2);
            r = 0.90f; g = 0.80f - 0.65f * t; b = 0.10f;
        }
        return 0xFF000000 | (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.fill(0, 0, width, height, BG);
        Component route = Component.translatable("screen.spacereloaded.porkchop.route",
                Component.translatable("planet.spacereloaded." + from.getPath()),
                Component.translatable("planet.spacereloaded." + to.getPath()));
        gfx.text(font, getTitle().copy().append(": ").append(route), 16, 12, ACCENT);
        if (!Double.isFinite(min)) {
            return;
        }
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.summary", kms(edge[0]), kms(min),
                String.format(Locale.ROOT, "%.1f", daysAt(minCol)), String.format(Locale.ROOT, "%.0f", edgeTof[minCol])),
                16, 26, TEXT);

        int top = 48, bottom = height - 44;
        int mid = width / 2;
        drawEdgeChart(gfx, 34, top, mid - 14, bottom);
        drawHeatmap(gfx, mid + 20, top, width - 20, bottom);
    }

    /** Нижняя кромка: Δv(дата) линией, отметки «сегодня» (слева) и «минимум». */
    private void drawEdgeChart(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.outline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, FRAME);
        double top = min + RANGE_MS;
        for (double v = Math.ceil(min / 1000) * 1000; v <= top; v += 1000) {
            int y = y1 - (int) ((v - min) / RANGE_MS * (y1 - y0));
            gfx.fill(x0, y, x1, y + 1, 0xFF1A252A);
            gfx.text(font, String.format(Locale.ROOT, "%.0f", v / 1000), x0 - 12, y - 4, MUTED);
        }
        int prevX = -1, prevY = -1;
        for (int c = 0; c < COLS; c++) {
            int x = x0 + (x1 - x0) * c / (COLS - 1);
            double v = Math.min(top, edge[c]);
            int y = y1 - (int) ((v - min) / RANGE_MS * (y1 - y0));
            if (prevX >= 0) {
                int steps = Math.max(Math.abs(x - prevX), Math.abs(y - prevY));
                for (int k = 0; k <= steps; k++) {
                    int px = prevX + (x - prevX) * k / Math.max(1, steps), py = prevY + (y - prevY) * k / Math.max(1, steps);
                    gfx.fill(px, py, px + 2, py + 2, heat((Math.min(top, edge[c]) - min) / RANGE_MS));
                }
            }
            prevX = x;
            prevY = y;
        }
        int xm = x0 + (x1 - x0) * minCol / (COLS - 1);
        gfx.fill(xm, y0, xm + 1, y1, GOOD);
        gfx.fill(x0, y0, x0 + 1, y1, ACCENT);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.today"), x0 + 3, y0 + 2, ACCENT);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.best"), Math.min(xm + 3, x1 - 40), y0 + 12, GOOD);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.axis_date",
                String.format(Locale.ROOT, "%.1f", daysAt(COLS - 1))), x0, y1 + 4, MUTED);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.axis_dv"), x0 - 12, y0 - 12, MUTED);
    }

    /** Тепловая карта «дата × время полёта» с изолиниями через 0.5 км/с. */
    private void drawHeatmap(GuiGraphicsExtractor gfx, int x0, int y0, int x1, int y1) {
        gfx.outline(x0 - 1, y0 - 1, x1 - x0 + 2, y1 - y0 + 2, FRAME);
        for (int c = 0; c < COLS; c++) {
            int cx0 = x0 + (x1 - x0) * c / COLS, cx1 = x0 + (x1 - x0) * (c + 1) / COLS;
            for (int r = 0; r < ROWS; r++) {
                int cy1 = y1 - (y1 - y0) * r / ROWS, cy0 = y1 - (y1 - y0) * (r + 1) / ROWS;
                double v = grid[c][r];
                int color = Double.isFinite(v) && v < min + RANGE_MS ? heat((v - min) / RANGE_MS) : 0xFF202428;
                gfx.fill(cx0, cy0, cx1, cy1, color);
                // изолиния: смена полосы 0.5 км/с к соседу справа или сверху
                long band = (long) Math.floor(v / 500);
                if (c + 1 < COLS && (long) Math.floor(grid[c + 1][r] / 500) != band && Double.isFinite(v)) {
                    gfx.fill(cx1 - 1, cy0, cx1, cy1, 0x90000000);
                }
                if (r + 1 < ROWS && (long) Math.floor(grid[c][r + 1] / 500) != band && Double.isFinite(v)) {
                    gfx.fill(cx0, cy0, cx1, cy0 + 1, 0x90000000);
                }
            }
        }
        // лучшая точка
        int xm = x0 + (x1 - x0) * minCol / COLS + (x1 - x0) / COLS / 2;
        int ym = y1 - (int) ((edgeTof[minCol] - tofLo) / (tofHi - tofLo) * (y1 - y0));
        gfx.outline(xm - 3, ym - 3, 7, 7, TEXT);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.axis_date",
                String.format(Locale.ROOT, "%.1f", daysAt(COLS - 1))), x0, y1 + 4, MUTED);
        gfx.text(font, Component.translatable("screen.spacereloaded.porkchop.axis_tof",
                String.format(Locale.ROOT, "%.0f", tofLo), String.format(Locale.ROOT, "%.0f", tofHi)), x0, y0 - 12, MUTED);
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreenAndShow(parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
