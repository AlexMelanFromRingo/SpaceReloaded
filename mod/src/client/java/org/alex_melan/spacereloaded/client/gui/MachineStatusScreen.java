package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.alex_melan.spacereloaded.network.MachineActionPayload;
import org.alex_melan.spacereloaded.network.MachineStatusPayload;

/**
 * Экран мультиблока 008 (FR-603): строки величин, шкалы и кнопки действий от сервера; раз в секунду
 * экран просит свежий снимок, пока открыт — числа живые, как на пульте.
 */
public class MachineStatusScreen extends Screen {

    private static final int W = 300;
    private static final int BG = 0xE00E1418;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;

    private static MachineStatusScreen active;
    private MachineStatusPayload data;
    private int ticks;

    public static MachineStatusScreen active() {
        return active;
    }

    public MachineStatusScreen(MachineStatusPayload data) {
        super(data.title());
        this.data = data;
    }

    /** Открыт ли экран этой машины (тогда пакет обновляет его, а не открывает новый). */
    public boolean shows(MachineStatusPayload other) {
        return data.pos().equals(other.pos());
    }

    public void update(MachineStatusPayload next) {
        boolean relayout = next.actions().size() != data.actions().size();
        data = next;
        if (relayout) {
            rebuildWidgets();
        }
    }

    private int height() {
        return 34 + data.lines().size() * 11 + data.gauges().size() * 16 + (data.actions().isEmpty() ? 0 : 28);
    }

    @Override
    protected void init() {
        active = this;
        int left = (width - W) / 2;
        int top = (height - height()) / 2;
        int y = top + height() - 24;
        int n = data.actions().size();
        if (n == 0) {
            return;
        }
        int bw = Math.min(96, (W - 16 - (n - 1) * 4) / n);
        for (int i = 0; i < n; i++) {
            var action = data.actions().get(i);
            addRenderableWidget(Button.builder(action.label(), b -> ClientPlayNetworking.send(
                            new MachineActionPayload(data.pos(), action.id(), action.value())))
                    .bounds(left + 8 + i * (bw + 4), y, bw, 18).build());
        }
    }

    @Override
    public void tick() {
        if (++ticks % 20 == 0) {
            ClientPlayNetworking.send(new MachineActionPayload(data.pos(), MachineActionPayload.REFRESH, 0));
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int h = height();
        int left = (width - W) / 2;
        int top = (height - h) / 2;
        gfx.fill(left - 1, top - 1, left + W + 1, top + h + 1, FRAME);
        gfx.fill(left, top, left + W, top + h, BG);
        gfx.text(font, data.title(), left + 8, top + 8, ACCENT, false);
        int y = top + 24;
        for (Component line : data.lines()) {
            gfx.text(font, line, left + 8, y, TEXT, false);
            y += 11;
        }
        for (var g : data.gauges()) {
            gfx.text(font, g.label(), left + 8, y, MUTED, false);
            int bx = left + 120;
            int bw = W - 128;
            gfx.fill(bx, y + 1, bx + bw, y + 8, 0xFF23282C);
            gfx.fill(bx + 1, y + 2, bx + 1 + (int) ((bw - 2) * Math.max(0, Math.min(1, g.fraction()))), y + 7,
                    0xFF000000 | g.rgb());
            y += 16;
        }
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public void removed() {
        if (active == this) {
            active = null;
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
