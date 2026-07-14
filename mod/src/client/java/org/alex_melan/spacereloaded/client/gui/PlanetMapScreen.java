package org.alex_melan.spacereloaded.client.gui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.alex_melan.spacereloaded.planet.Navigation;
import org.alex_melan.spacereloaded.planet.PlanetManager;
import org.alex_melan.spacereloaded.planet.TransferWindows;
import org.alex_melan.spacereloaded.network.PlanetMapPayload;
import org.alex_melan.spacereloaded.network.SetDestinationPayload;
import org.alex_melan.spacereloaded.registry.ModRegistries;
import org.alex_melan.spacereloaded.rocket.RocketEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Карта полёта. Схема рисуется из графа переходов датапака, а не из таблицы
 * координат: тело садится на кольцо по числу прыжков от Земли, поэтому чужая
 * планета из аддона появляется на карте сама.
 *
 * <p>Почти всё считается на клиенте по синхронизированному реестру планет.
 * С сервера приходит только спутниковое покрытие: клиент о нём знать не может.
 */
public class PlanetMapScreen extends Screen {

    private static final int BG = 0xE00A0E12;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;
    private static final int WARN = 0xFFDD4B4B;
    private static final int GOOD = 0xFF57C46B;
    private static final int ORBIT = 0xFF1E2A30;

    private static final int BODY_RADIUS = 5;
    private static final int RING_STEP = 46;
    private static final long TICKS_PER_DAY = 24_000L;

    /** Тело на карте: узел графа плюс экранная точка. */
    private record Body(Identifier id, ModRegistries.PlanetProfile profile, int x, int y, int ringRadius) {
    }

    private final PlanetMapPayload data;
    private final List<Body> bodies = new ArrayList<>();
    private Identifier here;
    private Identifier selected;
    private Button engageButton;

    public PlanetMapScreen(PlanetMapPayload data) {
        super(Component.translatable("screen.spacereloaded.map"));
        this.data = data;
    }

    @Override
    protected void init() {
        bodies.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        var access = mc.level.registryAccess();
        List<Identifier> ids = Navigation.planetIds(access);
        here = Navigation.entryIdFor(access, mc.level.dimension().identifier());
        if (selected == null) {
            selected = currentDestination(mc, ids);
        }

        // Корень схемы — Земля: тело, от которого граф расходится наружу
        Identifier root = ids.stream()
                .filter(id -> PlanetManager.profileById(access, id)
                        .map(profile -> profile.dimension().getPath().equals("overworld"))
                        .orElse(false))
                .findFirst().orElse(ids.isEmpty() ? null : ids.getFirst());
        if (root == null) {
            return;
        }

        int centerX = width / 2 - 70;
        int centerY = height / 2;
        // Кольцо = число прыжков от корня; тела одного кольца равномерно по углу
        List<List<Identifier>> rings = new ArrayList<>();
        for (Identifier id : ids) {
            int depth = Navigation.route(access, root, id).size() - 1;
            if (depth < 0) {
                depth = ids.indexOf(id); // тело вне графа: не теряем его, уводим наружу
            }
            while (rings.size() <= depth) {
                rings.add(new ArrayList<>());
            }
            rings.get(depth).add(id);
        }
        for (int depth = 0; depth < rings.size(); depth++) {
            List<Identifier> ring = rings.get(depth);
            for (int i = 0; i < ring.size(); i++) {
                Identifier id = ring.get(i);
                double angle = ring.size() == 1 && depth == 0
                        ? 0
                        : (2 * Math.PI * i / ring.size()) - Math.PI / 2;
                int radius = depth * RING_STEP;
                int x = centerX + (int) Math.round(Math.cos(angle) * radius);
                int y = centerY + (int) Math.round(Math.sin(angle) * radius);
                PlanetManager.profileById(access, id)
                        .ifPresent(profile -> bodies.add(new Body(id, profile, x, y, radius)));
            }
        }

        engageButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.map.engage"),
                        button -> {
                            ClientPlayNetworking.send(new SetDestinationPayload(selected));
                            onClose();
                        })
                .bounds(width / 2 + 20, height / 2 + 68, 130, 20)
                .build());
        addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.cannon.close"), button -> onClose())
                .bounds(width / 2 + 20, height / 2 + 92, 130, 20)
                .build());
        updateEngageButton();
    }

    private Identifier currentDestination(Minecraft mc, List<Identifier> ids) {
        if (mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket && !ids.isEmpty()) {
            return ids.get(Math.floorMod(rocket.clientDestinationIndex(), ids.size()));
        }
        return ids.isEmpty() ? null : ids.getFirst();
    }

    private void updateEngageButton() {
        if (engageButton == null) {
            return;
        }
        engageButton.active = data.pilotingRocket()
                && selected != null
                && !selected.equals(here);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubled) {
        for (Body body : bodies) {
            double dx = event.x() - body.x();
            double dy = event.y() - body.y();
            if (dx * dx + dy * dy <= (BODY_RADIUS + 4) * (BODY_RADIUS + 4)) {
                selected = body.id();
                updateEngageButton();
                return true;
            }
        }
        return super.mouseClicked(event, doubled);
    }

    /** Рисуем в слое фона: виджеты кладутся поверх, иначе панель их закроет. */
    @Override
    public void extractBackground(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(gfx, mouseX, mouseY, partialTick);
        gfx.fill(0, 0, width, height, BG);
        gfx.text(font, getTitle(), 16, 14, ACCENT);

        int centerX = width / 2 - 70;
        int centerY = height / 2;
        // Кольца рисуем по одному разу, а не по разу на тело
        bodies.stream().map(Body::ringRadius).filter(radius -> radius > 0).distinct()
                .forEach(radius -> drawRing(gfx, centerX, centerY, radius));

        Minecraft mc = Minecraft.getInstance();
        var access = mc.level == null ? null : mc.level.registryAccess();
        List<Identifier> route = access == null || here == null || selected == null
                ? List.of() : Navigation.route(access, here, selected);

        for (Body body : bodies) {
            boolean onRoute = route.contains(body.id());
            boolean isHere = body.id().equals(here);
            boolean isSelected = body.id().equals(selected);
            int color = isHere ? GOOD : onRoute ? ACCENT : MUTED;
            gfx.fill(body.x() - BODY_RADIUS, body.y() - BODY_RADIUS,
                    body.x() + BODY_RADIUS, body.y() + BODY_RADIUS, color);
            if (isSelected) {
                gfx.outline(body.x() - BODY_RADIUS - 3, body.y() - BODY_RADIUS - 3,
                        (BODY_RADIUS + 3) * 2, (BODY_RADIUS + 3) * 2, TEXT);
            }
            gfx.text(font, Component.translatable("planet.spacereloaded." + body.id().getPath()),
                    body.x() + BODY_RADIUS + 4, body.y() - font.lineHeight / 2,
                    isSelected ? TEXT : MUTED);
        }

        drawInfoPanel(gfx, route);
    }

    /** Кольцо орбиты: пунктир из точек, дуг у экстрактора нет. */
    private void drawRing(GuiGraphicsExtractor gfx, int centerX, int centerY, int radius) {
        int points = Math.max(48, radius * 2);
        for (int i = 0; i < points; i += 2) {
            double angle = 2 * Math.PI * i / points;
            int x = centerX + (int) Math.round(Math.cos(angle) * radius);
            int y = centerY + (int) Math.round(Math.sin(angle) * radius);
            gfx.fill(x, y, x + 1, y + 1, ORBIT);
        }
    }

    private void drawInfoPanel(GuiGraphicsExtractor gfx, List<Identifier> route) {
        int x = width / 2 + 20;
        int y = height / 2 - 100;
        int panelW = 130;
        gfx.fill(x - 8, y - 8, x + panelW + 8, height / 2 + 60, 0xC0121A20);
        gfx.outline(x - 8, y - 8, panelW + 16, height / 2 + 68 - y, FRAME);

        Body body = bodies.stream().filter(b -> b.id().equals(selected)).findFirst().orElse(null);
        if (body == null) {
            gfx.text(font, Component.translatable("screen.spacereloaded.map.no_selection"), x, y, MUTED);
            return;
        }
        int line = font.lineHeight + 3;
        gfx.text(font, Component.translatable("planet.spacereloaded." + body.id().getPath()),
                x, y, ACCENT);
        y += line + 2;

        var profile = body.profile();
        gfx.text(font, Component.translatable("screen.spacereloaded.map.gravity",
                String.format(Locale.ROOT, "%.2f", profile.gravity() / PlanetManager.EARTH_GRAVITY)),
                x, y, TEXT);
        y += line;
        gfx.text(font, Component.translatable(profile.breathable()
                        ? "screen.spacereloaded.map.breathable"
                        : "screen.spacereloaded.map.airless"),
                x, y, profile.breathable() ? GOOD : WARN);
        y += line;
        gfx.text(font, Component.translatable("screen.spacereloaded.map.temperature",
                String.format(Locale.ROOT, "%.0f", profile.temperature())), x, y, TEXT);
        y += line + 4;

        if (body.id().equals(here)) {
            gfx.text(font, Component.translatable("screen.spacereloaded.map.you_are_here"), x, y, GOOD);
            return;
        }
        if (route.isEmpty()) {
            gfx.text(font, Component.translatable("screen.spacereloaded.map.no_route"), x, y, WARN);
            return;
        }
        gfx.text(font, Component.translatable("screen.spacereloaded.map.hops", route.size() - 1),
                x, y, TEXT);
        y += line;

        // Окно перелёта считается по следующему прыжку: именно он сейчас откроется
        Identifier hop = route.get(1);
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            PlanetManager.profileById(mc.level.registryAccess(), hop).ifPresent(hopProfile -> {
                if (!TransferWindows.hasWindow(hopProfile)) {
                    return;
                }
                long gameTime = mc.level.getGameTime();
                boolean open = TransferWindows.isOpen(gameTime, hopProfile);
                Component window = open
                        ? Component.translatable("screen.spacereloaded.map.window_open")
                        : Component.translatable("screen.spacereloaded.map.window_wait",
                                String.format(Locale.ROOT, "%.1f",
                                        TransferWindows.ticksToOpen(gameTime, hopProfile)
                                                / (double) TICKS_PER_DAY));
                gfx.text(font, window, x, height / 2 + 20, open ? GOOD : WARN);
            });
        }

        if (profile.requiresCoverage()) {
            boolean covered = data.coveredDimensions().contains(
                    mc.level == null ? null : mc.level.dimension().identifier());
            gfx.text(font, Component.translatable(covered
                            ? "screen.spacereloaded.map.coverage_ok"
                            : "screen.spacereloaded.map.coverage_missing"),
                    x, height / 2 + 34, covered ? GOOD : WARN);
        }
        if (!data.pilotingRocket()) {
            gfx.text(font, Component.translatable("screen.spacereloaded.map.not_piloting"),
                    x, height / 2 + 48, MUTED);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
