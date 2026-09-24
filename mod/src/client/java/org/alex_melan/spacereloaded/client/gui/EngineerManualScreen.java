package org.alex_melan.spacereloaded.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.alex_melan.spacereloaded.core.multiblock.MultiblockTemplate;
import org.alex_melan.spacereloaded.multiblock.MultiblockTemplates;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Руководство инженера (005, FR-325, D58): оглавление слева, справа — текст страницы; у страниц
 * мультиблоков — послойная схема иконками блоков (слой переключается кнопками) и список материалов.
 * Шаблоны берутся из синхронизированного реестра — схемы аддонов появляются сами.
 */
public class EngineerManualScreen extends Screen {

    private static final int PANEL_W = 380;
    private static final int PANEL_H = 230;
    private static final int BG = 0xE00E1418;
    private static final int FRAME = 0xFF2A3A40;
    private static final int ACCENT = 0xFF6FD5E8;
    private static final int TEXT = 0xFFE8EEF0;
    private static final int MUTED = 0xFF9AA8AE;

    /** Страница: текстовая или шаблон. */
    private record Page(String key, MultiblockTemplates.TemplateEntry template) {
    }

    private final List<Page> pages = new ArrayList<>();
    private int page;
    private int layer;
    /** Первая видимая страница в списке (список прокручивается: страниц больше, чем помещается). */
    private int scroll;
    private static final int VISIBLE = 8;

    public EngineerManualScreen() {
        super(Component.translatable("screen.spacereloaded.manual"));
    }

    @Override
    protected void init() {
        pages.clear();
        for (String key : List.of("intro", "transmission", "machines", "parts", "cargo_mass")) {
            pages.add(new Page(key, null));
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            level.registryAccess().lookupOrThrow(MultiblockTemplates.MULTIBLOCKS).listElements()
                    .forEach(ref -> pages.add(new Page(ref.key().identifier().getPath(), ref.value())));
        }
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        scroll = Math.max(0, Math.min(scroll, Math.max(0, pages.size() - VISIBLE)));
        for (int i = scroll; i < Math.min(pages.size(), scroll + VISIBLE); i++) {
            int index = i;
            addRenderableWidget(Button.builder(pageTitle(pages.get(i)), b -> {
                        page = index;
                        layer = 0;
                    })
                    .bounds(left + 6, top + 22 + (i - scroll) * 21, 108, 19).build());
        }
        if (pages.size() > VISIBLE) {
            addRenderableWidget(Button.builder(Component.literal("▲"), b -> scrollList(-1))
                    .bounds(left + 6, top + 22 + VISIBLE * 21, 53, 18).build());
            addRenderableWidget(Button.builder(Component.literal("▼"), b -> scrollList(1))
                    .bounds(left + 61, top + 22 + VISIBLE * 21, 53, 18).build());
        }
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> layer++)
                .bounds(left + PANEL_W - 30, top + 22, 22, 18).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> layer--)
                .bounds(left + PANEL_W - 30, top + 42, 22, 18).build());
    }

    private void scrollList(int delta) {
        int next = Math.max(0, Math.min(scroll + delta, Math.max(0, pages.size() - VISIBLE)));
        if (next != scroll) {
            scroll = next;
            rebuildWidgets();
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int left = (width - PANEL_W) / 2;
        if (mouseX < left + 118 && scrollY != 0) {
            scrollList(scrollY > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private Component pageTitle(Page p) {
        return Component.translatable("manual.spacereloaded." + p.key() + ".title");
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        int left = (width - PANEL_W) / 2;
        int top = (height - PANEL_H) / 2;
        gfx.fill(left - 1, top - 1, left + PANEL_W + 1, top + PANEL_H + 1, FRAME);
        gfx.fill(left, top, left + PANEL_W, top + PANEL_H, BG);
        gfx.text(font, getTitle(), left + 8, top + 8, ACCENT, false);
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
        if (pages.isEmpty()) {
            return;
        }
        Page p = pages.get(Math.max(0, Math.min(page, pages.size() - 1)));
        int x = left + 122;
        int y = top + 22;
        int textWidth = PANEL_W - 122 - 40;
        gfx.text(font, pageTitle(p), x, y, ACCENT, false);
        y += 14;
        Component body = Component.translatable("manual.spacereloaded." + p.key() + ".text");
        if (p.template() == null) {
            gfx.textWithWordWrap(font, body, x, y, textWidth, TEXT, false);
            return;
        }
        gfx.textWithWordWrap(font, body, x, y, textWidth, MUTED, false);
        drawTemplate(gfx, p.template(), x, top + 96, textWidth);
    }

    /** Послойная схема и материалы. */
    private void drawTemplate(GuiGraphicsExtractor gfx, MultiblockTemplates.TemplateEntry template, int x, int y,
                              int width) {
        List<MultiblockTemplate.Cell> cells = template.displayCells();
        int minY = cells.stream().mapToInt(MultiblockTemplate.Cell::y).min().orElse(0);
        int maxY = cells.stream().mapToInt(MultiblockTemplate.Cell::y).max().orElse(0);
        int minX = cells.stream().mapToInt(MultiblockTemplate.Cell::x).min().orElse(0);
        int minZ = cells.stream().mapToInt(MultiblockTemplate.Cell::z).min().orElse(0);
        int layers = maxY - minY + 1;
        layer = Math.floorMod(layer, layers);
        int showY = minY + layer;
        gfx.text(font, Component.translatable("screen.spacereloaded.manual.layer", layer + 1, layers), x, y, TEXT,
                false);
        int gy = y + 12;
        int size = 17;
        for (MultiblockTemplate.Cell cell : cells) {
            if (cell.y() != showY) {
                continue;
            }
            int cx = x + (cell.x() - minX) * size;
            int cz = gy + (cell.z() - minZ) * size;
            if (cx > x + width - 16 || cz > gy + 110) {
                continue;
            }
            gfx.fill(cx - 1, cz - 1, cx + 17, cz + 17, 0x40FFFFFF);
            gfx.item(iconOf(cell.matcher()), cx, cz);
        }
        // Материалы
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (MultiblockTemplate.Cell cell : cells) {
            if (!"minecraft:air".equals(cell.matcher())) {
                counts.merge(cell.matcher(), 1, Integer::sum);
            }
        }
        int my = gy;
        int mx = x + Math.min(width - 100, 7 * size + 10);
        for (var e : counts.entrySet()) {
            gfx.item(iconOf(e.getKey()), mx, my);
            gfx.text(font, Component.literal("× " + e.getValue()), mx + 18, my + 4, TEXT, false);
            my += 18;
        }
    }

    private static ItemStack iconOf(String matcher) {
        if (matcher.startsWith("#")) {
            var tag = TagKey.create(Registries.BLOCK, Identifier.parse(matcher.substring(1)));
            for (var holder : BuiltInRegistries.BLOCK.getTagOrEmpty(tag)) {
                return new ItemStack(holder.value().asItem());
            }
            return new ItemStack(Items.BARRIER);
        }
        if ("minecraft:air".equals(matcher)) {
            return new ItemStack(Items.GLASS_PANE);
        }
        return new ItemStack(BuiltInRegistries.BLOCK.getValue(Identifier.parse(matcher)).asItem());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
