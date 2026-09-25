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
import org.alex_melan.spacereloaded.planet.TransferCosts;
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
 * координат: тело встаёт в колонку по числу прыжков от Земли, поэтому чужая
 * планета из аддона появляется на карте сама.
 *
 * <p>Почти всё считается на клиенте по синхронизированному реестру планет.
 * С сервера приходит только спутниковое покрытие: клиент о нём знать не может.
 *
 * <p>009 (US2): карта Δv — на каждом ребре цена «туда / обратно» на сегодня (межпланетные плечи —
 * по Ламберту, как спишет переход), в панели — минимум периода и дни до него, сумма маршрута против
 * Δv стека; «Окна…» открывает «свиную отбивную» следующего межпланетного плеча.
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
    private static final long TICKS_PER_DAY = 24_000L;

    /** Тело на карте: узел графа, экранная точка и число прыжков от Земли. */
    private record Body(Identifier id, ModRegistries.PlanetProfile profile, int x, int y, int depth) {
    }

    private final PlanetMapPayload data;
    private final List<Body> bodies = new ArrayList<>();
    private Identifier here;
    private Identifier selected;
    private Button engageButton;
    private Button windowsButton;

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

        // 009: схема-дерево («карта метро» Δv): колонка = число прыжков от корня, тела колонки —
        // столбиком по центру; рёбра не пересекают чужие тела, цена читается у каждого
        int left = 40;
        int right = width / 2 - 24;
        int centerY = height / 2;
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
        int columns = Math.max(1, rings.size() - 1);
        for (int depth = 0; depth < rings.size(); depth++) {
            List<Identifier> ring = rings.get(depth);
            final int column = depth;
            int x = left + (right - left) * depth / columns;
            for (int i = 0; i < ring.size(); i++) {
                Identifier id = ring.get(i);
                int y = centerY + (int) Math.round((i - (ring.size() - 1) / 2.0) * 56);
                PlanetManager.profileById(access, id)
                        .ifPresent(profile -> bodies.add(new Body(id, profile, x, y, column)));
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
        windowsButton = addRenderableWidget(Button.builder(
                        Component.translatable("screen.spacereloaded.map.windows"), button -> openPorkchop())
                .bounds(width / 2 + 20, height / 2 + 116, 130, 20)
                .build());
        updateEngageButton();
    }

    private Identifier currentDestination(Minecraft mc, List<Identifier> ids) {
        if (mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket && !ids.isEmpty()) {
            return ids.get(Math.floorMod(rocket.clientDestinationIndex(), ids.size()));
        }
        return ids.isEmpty() ? null : ids.getFirst();
    }

    /** Первое межпланетное плечо маршрута к выбранной цели: {откуда, куда} или null. */
    private Identifier[] celestialLeg() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || here == null || selected == null) {
            return null;
        }
        var access = mc.level.registryAccess();
        List<Identifier> route = Navigation.route(access, here, selected);
        for (int i = 0; i + 1 < route.size(); i++) {
            var from = PlanetManager.profileById(access, route.get(i));
            if (from.isPresent() && TransferCosts.celestial(access, from.get(), route.get(i + 1))) {
                return new Identifier[] {route.get(i), route.get(i + 1)};
            }
        }
        return null;
    }

    /** Выбор цели (стенд и кадры README). */
    public void select(Identifier id) {
        selected = id;
        updateEngageButton();
    }

    public void openPorkchop() {
        Identifier[] leg = celestialLeg();
        if (leg != null) {
            Minecraft.getInstance().setScreenAndShow(new PorkchopScreen(this, leg[0], leg[1]));
        }
    }

    private void updateEngageButton() {
        if (windowsButton != null) {
            windowsButton.active = celestialLeg() != null;
        }
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


        Minecraft mc = Minecraft.getInstance();
        var access = mc.level == null ? null : mc.level.registryAccess();
        List<Identifier> route = access == null || here == null || selected == null
                ? List.of() : Navigation.route(access, here, selected);

        if (access != null && mc.level != null) {
            drawEdges(gfx, access, mc.level.getGameTime(), route);
        }
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
            Component name = Component.translatable("planet.spacereloaded." + body.id().getPath());
            gfx.text(font, name, body.x() - font.width(name) / 2, body.y() + BODY_RADIUS + 4, isSelected ? TEXT : MUTED);
        }

        drawInfoPanel(gfx, route);
    }

    /**
     * Рёбра графа переходов пунктиром и цена на сегодня, км/с: «наружу / обратно» — от тела ближе к
     * Земле к дальнему; подпись у внутренней трети ребра, чтобы не наезжать на имена тел.
     */
    private void drawEdges(GuiGraphicsExtractor gfx, net.minecraft.core.RegistryAccess access, long tick,
                           List<Identifier> route) {
        java.util.Set<String> drawn = new java.util.HashSet<>();
        for (Body p : bodies) {
            for (Identifier target : p.profile().transitionTargets()) {
                Body q = bodies.stream().filter(o -> o.id().equals(target)).findFirst().orElse(null);
                if (q == null) {
                    continue;
                }
                Body a = p.depth() <= q.depth() ? p : q;   // внутреннее тело
                Body b = a == p ? q : p;
                if (!drawn.add(a.id() + ">" + b.id())) {
                    continue;
                }
                boolean onRoute = route.contains(a.id()) && route.contains(b.id());
                int color = onRoute ? 0xFF3C7F8C : 0xFF26343A;
                int n = Math.max(8, (int) (Math.hypot(b.x() - a.x(), b.y() - a.y()) / 3));
                for (int k = 1; k < n; k += 2) {
                    int x = a.x() + (b.x() - a.x()) * k / n, y = a.y() + (b.y() - a.y()) * k / n;
                    gfx.fill(x, y, x + 1, y + 1, color);
                }
                double out = a.profile().transitionTargets().contains(b.id())
                        ? TransferCosts.cost(access, a.profile(), b.id(), tick) : Double.NaN;
                double back = b.profile().transitionTargets().contains(a.id())
                        ? TransferCosts.cost(access, b.profile(), a.id(), tick) : Double.NaN;
                String label = (Double.isNaN(out) ? "—" : kms(out)) + " / " + (Double.isNaN(back) ? "—" : kms(back));
                int mx = (a.x() + b.x()) / 2, my = (a.y() + b.y()) / 2;
                gfx.text(font, label, mx - font.width(label) / 2, my - font.lineHeight - 2, onRoute ? ACCENT : MUTED);
            }
        }
    }

    private String minKey;
    private double[] minValue;

    /** Минимум периода — перебор сотен дат; на экране пересчитывается раз в игровой час. */
    private double[] minimumCached(net.minecraft.core.RegistryAccess access, ModRegistries.PlanetProfile from,
                                   Identifier[] leg, long tick) {
        String key = leg[0] + ">" + leg[1] + "@" + tick / 1000;
        if (!key.equals(minKey)) {
            minValue = TransferCosts.minimumInPeriod(access, from, leg[1], tick);
            minKey = key;
        }
        return minValue;
    }

    private static String kms(double ms) {
        return Double.isFinite(ms) ? String.format(Locale.ROOT, "%.1f", ms / 1000) : "∞";
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

        // 009: цена маршрута сегодня против Δv стека; межпланетное плечо — сегодня и минимум периода
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            var access = mc.level.registryAccess();
            long tick = mc.level.getGameTime();
            double total = 0;
            for (int i = 0; i + 1 < route.size(); i++) {
                var from = PlanetManager.profileById(access, route.get(i));
                if (from.isPresent()) {
                    total += TransferCosts.cost(access, from.get(), route.get(i + 1), tick);
                }
            }
            double have = mc.player != null && mc.player.getVehicle() instanceof RocketEntity rocket ? rocket.clientDeltaV() : Double.NaN;
            gfx.text(font, Component.translatable("screen.spacereloaded.map.route_cost", kms(total)), x, y,
                    Double.isFinite(have) && have < total ? WARN : TEXT);
            y += line;
            if (Double.isFinite(have)) {
                gfx.text(font, Component.translatable("screen.spacereloaded.map.stack_dv", kms(have)), x, y,
                        have < total ? WARN : GOOD);
                y += line;
            }
            Identifier[] leg = celestialLeg();
            if (leg != null) {
                var from = PlanetManager.profileById(access, leg[0]).orElseThrow();
                double today = TransferCosts.cost(access, from, leg[1], tick);
                double[] min = minimumCached(access, from, leg, tick);
                gfx.text(font, Component.translatable("screen.spacereloaded.map.leg_today", kms(today)), x, y, TEXT);
                y += line;
                gfx.text(font, Component.translatable("screen.spacereloaded.map.leg_min", kms(min[1]),
                        String.format(Locale.ROOT, "%.1f", (min[0] - tick) / (double) TICKS_PER_DAY)), x, y,
                        today <= min[1] * 1.02 ? GOOD : MUTED);
            }
        }

        // Датапак-тело без орбиты: окно-расписание по следующему прыжку (003)
        Identifier hop = route.get(1);
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
