package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.alex_melan.spacereloaded.network.CannonActionPayload;
import org.alex_melan.spacereloaded.network.CannonStatePayload;

/**
 * Терминал орбитального орудия вместо простыни в чате: заряд, энергия, цель,
 * перезарядка и кнопка пуска. Кнопка гаснет, когда стрелять нечем.
 */
public class CannonTerminalScreen extends Screen {

    private static final int PANEL_W = 240;
    private static final int PANEL_H = 150;
    private static final int BG = 0xE00E1418;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;
    private static final int WARN = 0xFFDD4B4B;
    private static final int GOOD = 0xFF57C46B;

    /**
     * Открытый терминал, если он есть. В 26.2 у Minecraft нет публичного доступа
     * к текущему экрану, а обновлять открытую панель надо: держим ссылку сами и
     * гасим её в removed(), который зовётся при любой смене экрана.
     */
    private static CannonTerminalScreen active;

    private CannonStatePayload state;
    private Button fireButton;

    public static CannonTerminalScreen active() {
        return active;
    }

    public CannonTerminalScreen(CannonStatePayload state) {
        super(Component.translatable("screen.spacereloaded.cannon"));
        this.state = state;
    }

    /** Пришёл свежий снимок с сервера: обновляем панель, не пересоздавая экран. */
    public void update(CannonStatePayload fresh) {
        this.state = fresh;
        if (fireButton != null) {
            fireButton.active = canFire();
        }
    }

    private boolean canFire() {
        return state.rods() > 0
                && state.energy() >= state.energyPerShot()
                && state.cooldownTicks() == 0
                && state.hasTarget();
    }

    private int left() {
        return (width - PANEL_W) / 2;
    }

    private int top() {
        return (height - PANEL_H) / 2;
    }

    @Override
    public void removed() {
        super.removed();
        if (active == this) {
            active = null;
        }
    }

    @Override
    protected void init() {
        active = this;
        fireButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.cannon.fire"),
                        button -> ClientPlayNetworking.send(new CannonActionPayload(
                                state.cannonPos(), state.cannonDim(), CannonActionPayload.FIRE)))
                .bounds(left() + 12, top() + PANEL_H - 30, 100, 20)
                .build());
        fireButton.active = canFire();
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.cannon.close"), button -> onClose())
                .bounds(left() + PANEL_W - 112, top() + PANEL_H - 30, 100, 20)
                .build());
    }

    /** Рисуем в слое фона: виджеты кладутся поверх, иначе панель их закроет. */
    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        int x = left();
        int y = top();
        gfx.fill(x, y, x + PANEL_W, y + PANEL_H, BG);
        gfx.outline(x, y, PANEL_W, PANEL_H, FRAME);

        gfx.text(font, getTitle(), x + 12, y + 10, ACCENT);
        int line = font.lineHeight + 4;
        int row = y + 10 + line + 4;

        gfx.text(font, Component.translatable("screen.spacereloaded.cannon.rods",
                state.rods(), state.maxRods()), x + 12, row, state.rods() > 0 ? TEXT : WARN);
        row += line;

        boolean charged = state.energy() >= state.energyPerShot();
        gfx.text(font, Component.translatable("screen.spacereloaded.cannon.energy",
                state.energy(), state.energyCapacity()), x + 12, row, charged ? TEXT : WARN);
        row += line;
        // Полоса заряда: доля от энергии выстрела, а не от ёмкости
        int barWidth = PANEL_W - 24;
        double charge = state.energyPerShot() <= 0 ? 1
                : Math.min(1.0, (double) state.energy() / state.energyPerShot());
        gfx.fill(x + 12, row, x + 12 + barWidth, row + 5, 0xFF23282C);
        gfx.fill(x + 12, row, x + 12 + (int) (barWidth * charge), row + 5, charged ? GOOD : WARN);
        row += 12;

        Component target = state.hasTarget()
                ? Component.literal(state.target().toShortString() + " @ "
                        + state.targetDim().getPath())
                : Component.translatable("screen.spacereloaded.cannon.no_target");
        gfx.text(font, Component.translatable("screen.spacereloaded.cannon.target", target),
                x + 12, row, state.hasTarget() ? TEXT : MUTED);
        row += line;

        Component reload = state.cooldownTicks() == 0
                ? Component.translatable("screen.spacereloaded.cannon.ready")
                : Component.translatable("screen.spacereloaded.cannon.reloading",
                        String.format("%.1f", state.cooldownTicks() / 20.0));
        gfx.text(font, reload, x + 12, row, state.cooldownTicks() == 0 ? GOOD : MUTED);
        row += line;

        gfx.text(font, Component.translatable("screen.spacereloaded.cannon.hint"),
                x + 12, row, MUTED);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
