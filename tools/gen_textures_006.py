#!/usr/bin/env python3
"""Текстуры 006 «Материалы и электроника»: предметы цепочки кремния, металлы, реагенты, изделия
фаба; руды и блоки машин; окна процессных машин. Вспомогательные функции — из gen_textures.py.
Запуск: python3 tools/gen_textures_006.py
"""
import math
import random

from PIL import Image

from gen_textures import _frame, _shade, _tint, mod, save, transplant_ore, vanilla

# ---------------------------------------------------------------------------
# Вспомогательные
# ---------------------------------------------------------------------------


def blank():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))


def put(img, x, y, color):
    if 0 <= x < 16 and 0 <= y < 16:
        img.putpixel((x, y), color if len(color) == 4 else tuple(color) + (255,))


def icon(name, img):
    save(img, f"item/{name}.png")


def tinted(path, color, gain=1.0):
    return _tint(vanilla(path), color, gain)


def ingot(name, color, gain=1.25):
    icon(name, tinted("item/iron_ingot.png", color, gain))


def dust(name, color, gain=1.1):
    icon(name, tinted("item/sugar.png", color, gain))


def crystal(name, color, gain=1.2):
    icon(name, tinted("item/quartz.png", color, gain))


def shard(name, color, gain=1.2):
    icon(name, tinted("item/amethyst_shard.png", color, gain))


def bottle(name, liquid):
    """Бутыль с реагентом: ванильная склянка, жидкость своего цвета."""
    glass = vanilla("item/potion.png")
    overlay = _tint(vanilla("item/potion_overlay.png"), liquid, 1.25)
    out = glass.copy()
    out.alpha_composite(overlay)
    icon(name, out)


def cylinder(name, color, band):
    """Газовый баллон: корпус, вентиль, цветная полоса маркировки."""
    img = blank()
    for y in range(3, 15):
        for x in range(5, 11):
            shade = 1.2 if x == 6 else (0.7 if x == 10 else 1.0)
            put(img, x, y, _shade(color, shade))
    for y in (6, 7):
        for x in range(5, 11):
            put(img, x, y, band)
    for x in range(6, 10):
        put(img, x, 2, _shade(color, 0.8))
    put(img, 7, 1, (0x40, 0x40, 0x44))
    put(img, 8, 1, (0x40, 0x40, 0x44))
    icon(name, img)


def disc(img, cx, cy, r, color_fn):
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - cx, y - cy)
            if d <= r:
                c = color_fn(x, y, d)
                if c:
                    put(img, x, y, c)


def wafer(name, base, grid=None, flat=True, square=False):
    """Пластина Ø50 мм: зеркальная поверхность, базовый срез, сетка кристаллов."""
    img = blank()

    def color(x, y, d):
        if flat and y >= 14:
            return None
        sheen = 1.25 if (x - y) in (-3, -2, 5, 6) else 1.0
        c = _shade(base, sheen * (1.05 - d / 30))
        if grid and 1 <= x <= 14 and (x % 3 == 0 or y % 3 == 0):
            c = _shade(grid, 1.0)
        return c
    if square:
        for x in range(2, 14):
            for y in range(2, 14):
                c = color(x, y, 3)
                put(img, x, y, c if (x + y) % 5 else _shade(base, 0.85))
    else:
        disc(img, 7.5, 7.5, 7, color)
    icon(name, img)


def pile(name, colors, seed):
    rng = random.Random(seed)
    img = blank()
    for y in range(6, 15):
        half = (y - 5) * 0.8
        for x in range(int(8 - half), int(8 + half) + 1):
            put(img, x, y, _shade(rng.choice(colors), rng.uniform(0.8, 1.15)))
    icon(name, img)


def chip(name, body, mark):
    img = blank()
    for x in range(3, 13):
        for y in range(4, 12):
            put(img, x, y, body)
    for x in range(4, 12, 2):
        put(img, x, 3, (0xD0, 0xD0, 0xD8))
        put(img, x, 12, (0xD0, 0xD0, 0xD8))
    put(img, 4, 5, mark)
    for x in range(6, 11):
        put(img, x, 7, _shade(mark, 0.8))
    icon(name, img)


def die(name, color):
    img = blank()
    for x in range(4, 12):
        for y in range(4, 12):
            put(img, x, y, _shade(color, 1.2 if (x * 7 + y * 3) % 5 == 0 else 1.0))
    for i in range(4, 12):
        put(img, i, 4, (0xE0, 0xC0, 0x60))
        put(img, 4, i, (0xE0, 0xC0, 0x60))
    icon(name, img)


# ---------------------------------------------------------------------------
# Предметы
# ---------------------------------------------------------------------------

SI_GREY = (0x7C, 0x84, 0x96)
SI_WAFER = (0x86, 0x8E, 0xAE)


def items():
    # нулевой тир
    img = blank()
    for i in range(16):
        a = i / 16 * 2 * math.pi * 2.5
        for r in (4.5, 5.5):
            put(img, int(8 + r * math.cos(a)), int(8 + r * math.sin(a) * 0.55), (0xD8, 0x84, 0x50))
    for y in range(3, 13):
        put(img, 3, y, (0xB0, 0x68, 0x38))
    icon("copper_wire", img)
    img = blank()
    for x in range(3, 13):
        for y in range(5, 13):
            put(img, x, y, (0x3A, 0x3C, 0x44) if x in (3, 12) or y in (5, 12) else (0x58, 0x5C, 0x66))
    for x in range(5, 11):
        for y in range(7, 11):
            put(img, x, y, (0xD8, 0x84, 0x50) if y % 2 else (0xA0, 0x60, 0x38))
    put(img, 5, 4, (0xC0, 0x30, 0x20))
    put(img, 10, 4, (0xC0, 0x30, 0x20))
    icon("relay", img)
    img = blank()
    for x in range(1, 15):
        for y in range(3, 13):
            put(img, x, y, (0x44, 0x48, 0x50))
    for x in range(2, 14, 3):
        for y in range(4, 12, 3):
            put(img, x, y, (0xD8, 0x84, 0x50))
            put(img, x + 1, y, (0xC0, 0x30, 0x20))
    icon("relay_logic", img)
    img = blank()
    disc(img, 7.5, 7.5, 5, lambda x, y, d: (0x2C, 0x2C, 0x30) if d > 2.5 else None)
    disc(img, 7.5, 7.5, 5, lambda x, y, d: (0x50, 0x50, 0x58) if 4.2 < d < 5 and x < 8 else None)
    icon("ferrite_core", img)
    img = blank()
    for x in range(1, 15):
        for y in range(2, 14):
            put(img, x, y, (0x60, 0x48, 0x30) if x in (1, 14) else (0x90, 0x78, 0x58))
    for x in range(3, 14, 3):
        for y in range(4, 13, 3):
            put(img, x, y, (0x2C, 0x2C, 0x30))
    for y in range(3, 13):
        put(img, 5 + (y % 4), y, (0xD8, 0x84, 0x50))
    icon("core_rope_memory", img)

    # кремний
    pile("silicon_blend", [(0xD8, 0xC8, 0x98), (0x30, 0x30, 0x34), (0xE0, 0xD4, 0xA8)], 1)
    crystal("metallurgical_silicon", (0x70, 0x78, 0x88), 1.3)
    crystal("polysilicon", (0x98, 0xA4, 0xC0), 1.35)
    dust("silicon_dust", (0x7C, 0x84, 0x96))
    img = blank()
    for x in range(5, 11):
        for y in range(1, 15):
            shade = 1.3 if x == 6 else (0.7 if x == 10 else 1.0)
            put(img, x, y, _shade(SI_GREY, shade))
    for x in range(6, 10):
        put(img, x, 0, _shade(SI_GREY, 0.8))
        put(img, x, 15, _shade(SI_GREY, 0.8))
    icon("silicon_boule", img)
    img = blank()
    for x in range(2, 14):
        for y in range(4, 12):
            put(img, x, y, _shade((0x3A, 0x50, 0x90), 1.0 + 0.3 * (((x // 3) + (y // 2)) % 3 == 0)))
    icon("multicrystalline_silicon", img)
    wafer("silicon_wafer", SI_WAFER)
    wafer("multicrystalline_wafer", (0x40, 0x58, 0x9C), square=True)
    wafer("sapphire_wafer", (0xB8, 0xD0, 0xE8), flat=False)
    wafer("sos_wafer", (0x8E, 0xA0, 0xC8), grid=None, flat=True)
    img = blank()
    for x in range(2, 14):
        for y in range(2, 14):
            put(img, x, y, (0x28, 0x3C, 0x80) if (x - 2) % 4 and (y - 2) % 4 else (0xC8, 0xC8, 0xD0))
    icon("solar_cell", img)
    img = blank()
    for x in range(2, 14):
        for y in range(2, 14):
            corner = (x in (2, 13)) and (y in (2, 13))
            put(img, x, y, (0, 0, 0, 0) if corner else ((0x12, 0x14, 0x1C) if x not in (5, 10) else (0xC8, 0xC8, 0xD0)))
    icon("mono_solar_cell", img)
    cylinder("carbon_monoxide", (0x7A, 0x7E, 0x88), (0xD0, 0xD0, 0x40))
    cylinder("hydrogen_chloride", (0x9A, 0xA8, 0x90), (0x40, 0xA0, 0x40))
    cylinder("trichlorosilane", (0x8C, 0x96, 0xB0), (0x60, 0x80, 0xE0))
    bottle("brine", (0xC8, 0xE0, 0xF0))
    bottle("caustic_soda", (0xF0, 0xF0, 0xE8))
    bottle("sulfuric_acid", (0xE8, 0xD8, 0x60))
    bottle("hydrofluoric_acid", (0xC0, 0xF0, 0xC8))
    bottle("photoresist", (0xE0, 0x90, 0x30))
    bottle("epoxy_resin", (0xD8, 0xB8, 0x70))
    pile("phosphate_blend", [(0xF0, 0xF0, 0xE8), (0xD8, 0xC8, 0x98), (0x30, 0x30, 0x34)], 2)
    shard("phosphorus", (0xF0, 0xE8, 0xB0), 1.1)
    img = blank()
    for y in range(5, 14):
        half = 6 - max(0, y - 10)
        for x in range(8 - half, 8 + half):
            put(img, x, y, (0xF0, 0xF0, 0xF4) if y > 5 else (0xC8, 0xC8, 0xD0))
    for x in range(4, 12):
        put(img, x, 6, (0x9C, 0x9C, 0xA8))
    icon("quartz_crucible", img)

    # фаб
    for name, pattern in (("photomask_logic", (0xA0, 0x40, 0x20)), ("photomask_microprocessor", (0x80, 0x30, 0x28)),
                          ("photomask_radhard", (0x60, 0x20, 0x40))):
        img = blank()
        for x in range(2, 14):
            for y in range(2, 14):
                put(img, x, y, (0xD8, 0xE8, 0xF0, 200))
        for x in range(3, 13):
            for y in range(3, 13):
                if (x * 3 + y) % (4 if "logic" in name else 3) == 0 or (x % 4 == 0):
                    put(img, x, y, pattern)
        icon(name, img)
    die("die_logic", (0x70, 0x80, 0xA8))
    die("die_microprocessor", (0x68, 0x70, 0xB8))
    die("die_radhard", (0x90, 0x88, 0xB0))
    img = blank()
    for x in range(3, 13):
        for y in range(4, 12):
            put(img, x, y, (0xE8, 0xE4, 0xD8))
    for x in range(6, 10):
        for y in range(6, 10):
            put(img, x, y, (0xD0, 0xA8, 0x40))
    icon("ceramic_package", img)
    chip("logic_chip", (0xE8, 0xE4, 0xD8), (0x30, 0x30, 0x34))
    chip("microprocessor", (0x30, 0x30, 0x34), (0xD0, 0xA8, 0x40))
    chip("radhard_processor", (0x8C, 0x30, 0x28), (0xD0, 0xA8, 0x40))
    # платы
    img = blank()
    for i in range(12):
        for x in range(2 + i % 2, 14, 2):
            put(img, x, 2 + i, (0xE8, 0xF0, 0xF0))
    icon("glass_fiber", img)
    for name, base, copper in (("fr4_laminate", (0xB8, 0xB0, 0x60), False),
                               ("copper_clad_laminate", (0xB8, 0xB0, 0x60), True),
                               ("incomplete_circuit_board", (0x2E, 0x7A, 0x3A), True),
                               ("circuit_board", (0x2E, 0x7A, 0x3A), True)):
        img = blank()
        for x in range(2, 14):
            for y in range(3, 13):
                put(img, x, y, base)
        if copper and name == "copper_clad_laminate":
            for x in range(2, 14):
                for y in range(3, 11):
                    put(img, x, y, (0xD8, 0x84, 0x50))
        if name.endswith("circuit_board"):
            for x in range(3, 13):
                put(img, x, 6, (0xD0, 0xA8, 0x40))
            for y in range(4, 12):
                put(img, 9, y, (0xD0, 0xA8, 0x40))
            if name == "circuit_board":
                for x in range(4, 7):
                    for y in range(8, 11):
                        put(img, x, y, (0x20, 0x20, 0x24))
        icon(name, img)
    img = blank()
    for x in range(1, 15):
        for y in range(3, 14):
            put(img, x, y, (0x50, 0x54, 0x5C) if x in (1, 14) or y in (3, 13) else (0x2E, 0x7A, 0x3A))
    for x in range(3, 8):
        for y in range(5, 9):
            put(img, x, y, (0x30, 0x30, 0x34))
    for x in range(9, 13):
        for y in range(5, 12, 2):
            put(img, x, y, (0xE8, 0xE4, 0xD8))
    for x in range(3, 8):
        put(img, x, 11, (0x90, 0x78, 0x58))
    icon("flight_computer", img)

    # минералы, металлы
    crystal("fluorite", (0x9A, 0x60, 0xD0), 1.3)
    crystal("cryolite", (0xF0, 0xF0, 0xF4), 1.1)
    dust("alumina", (0xF4, 0xF4, 0xF0))
    shard("spodumene", (0xE0, 0xB0, 0xC8), 1.2)
    dust("beta_spodumene", (0xE8, 0xD0, 0xD8))
    dust("rock_salt", (0xF0, 0xE0, 0xE0), 1.15)
    dust("gypsum", (0xE8, 0xE4, 0xD8))
    dust("lithium_chloride", (0xF0, 0xF4, 0xF8))
    crystal("sapphire", (0x30, 0x50, 0xC8), 1.4)
    ingot("aluminium_ingot", (0xD8, 0xDC, 0xE4), 1.35)
    ingot("aluminium_copper_ingot", (0xE0, 0xC8, 0xB0), 1.3)
    ingot("aluminium_lithium_ingot", (0xC8, 0xD8, 0xE0), 1.3)
    ingot("lithium_ingot", (0xE8, 0xE8, 0xE0), 1.3)
    ingot("nickel_ingot", (0xC8, 0xC4, 0xA8), 1.25)
    ingot("nickel_superalloy_ingot", (0x9C, 0xA0, 0xA8), 1.15)
    pile("al_cu_blend", [(0xD8, 0xDC, 0xE4), (0xD8, 0x84, 0x50)], 3)
    pile("al_li_blend", [(0xD8, 0xDC, 0xE4), (0xE8, 0xE8, 0xE0), (0xD8, 0x84, 0x50)], 4)
    pile("superalloy_blend", [(0xC8, 0xC4, 0xA8), (0xD8, 0xDC, 0xE4), (0xB8, 0xC8, 0xD8)], 5)


# ---------------------------------------------------------------------------
# Блоки
# ---------------------------------------------------------------------------

def ore_tint(name, color):
    """Руда в камне: маска вкраплений железной руды, перекрашенная в цвет минерала."""
    stone = vanilla("block/stone.png")
    ore = vanilla("block/iron_ore.png")
    colored = ore.copy()
    for x in range(16):
        for y in range(16):
            sr_, sg, sb, _ = stone.getpixel((x, y))
            r, g, b, a = ore.getpixel((x, y))
            if abs(sr_ - r) + abs(sg - g) + abs(sb - b) > 28:
                lum = (r + g + b) / (3 * 255)
                colored.putpixel((x, y), tuple(min(255, int(c * (0.55 + lum))) for c in color) + (a,))
    save(colored, f"block/{name}.png")


def machine_face(base, draw):
    img = base.copy()
    draw(img)
    return img


def blocks():
    ore_tint("halite_ore", (0xE8, 0xC0, 0xC0))
    ore_tint("fluorite_ore", (0x9A, 0x60, 0xD0))
    ore_tint("spodumene_ore", (0xE0, 0xB0, 0xC8))
    moon = mod("block/moon_stone.png")
    rng = random.Random(0xA17)
    anorth = _tint(moon, (0xE8, 0xE8, 0xEC), 1.25)
    for _ in range(22):
        put(anorth, rng.randrange(16), rng.randrange(16), (0xF8, 0xF8, 0xFC))
    save(anorth, "block/anorthosite.png")

    steel = vanilla("block/iron_block.png")
    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    side = casing.copy()
    top = steel.copy()
    _frame(top, (0x55, 0x58, 0x60, 255), 1)

    def window(img, color, x0=4, y0=4, x1=12, y1=10):
        for x in range(x0, x1):
            for y in range(y0, y1):
                put(img, x, y, color if (x + y) % 4 else _shade(color, 1.25))
        for x in range(x0 - 1, x1 + 1):
            put(img, x, y0 - 1, (0x3A, 0x3C, 0x44))
            put(img, x, y1, (0x3A, 0x3C, 0x44))

    # химический реактор — стеклянный реакционный сосуд
    for lit in (False, True):
        f = side.copy()
        window(f, (0x60, 0xB0, 0x70) if lit else (0x40, 0x60, 0x50), 4, 3, 12, 12)
        save(f, f"block/chemical_reactor_front{'_on' if lit else ''}.png")
    save(side, "block/chemical_reactor_side.png")
    t = top.copy()
    disc(t, 7.5, 7.5, 3, lambda x, y, d: (0x3A, 0x3C, 0x44))
    save(t, "block/chemical_reactor_top.png")
    # реактор осаждения — кварцевый колокол, стержни светятся при 1100 °C
    for lit in (False, True):
        f = side.copy()
        window(f, (0x50, 0x48, 0x48), 3, 2, 13, 14)
        for x in (6, 9):
            for y in range(4, 13):
                put(f, x, y, (0xFF, 0xB0, 0x40) if lit else (0x80, 0x84, 0x90))
        for x in range(6, 10):
            put(f, x, 4, (0xFF, 0xB0, 0x40) if lit else (0x80, 0x84, 0x90))
        save(f, f"block/deposition_reactor_front{'_on' if lit else ''}.png")
    save(side, "block/deposition_reactor_side.png")
    t = top.copy()
    disc(t, 7.5, 7.5, 5, lambda x, y, d: (0xE0, 0xE8, 0xF0) if d > 3.5 else (0x9C, 0xA8, 0xB8))
    save(t, "block/deposition_reactor_top.png")
    # диффузионная печь — кварцевая труба в кожухе, лодочка с пластинами
    for lit in (False, True):
        f = side.copy()
        disc(f, 7.5, 7.5, 6, lambda x, y, d: (0x2A, 0x2A, 0x30) if d > 4.6 else
             ((0xFF, 0x80, 0x30) if lit else (0x60, 0x50, 0x48)))
        for x in range(5, 11, 2):
            for y in range(6, 10):
                put(f, x, y, (0xC0, 0xC8, 0xE0))
        save(f, f"block/diffusion_furnace_front{'_on' if lit else ''}.png")
    s = side.copy()
    for x in range(16):
        put(s, x, 3, (0xE0, 0x70, 0x28))
        put(s, x, 12, (0xE0, 0x70, 0x28))
    save(s, "block/diffusion_furnace_side.png")
    save(top, "block/diffusion_furnace_top.png")
    # литография — жёлтый свет чистой зоны, стол с шаблоном, УФ-лампа при работе
    for lit in (False, True):
        f = side.copy()
        window(f, (0xE8, 0xC8, 0x40), 3, 3, 13, 8)
        for x in range(4, 12):
            for y in range(10, 13):
                put(f, x, y, (0xB0, 0x80, 0xF0) if lit else (0x60, 0x64, 0x70))
        save(f, f"block/lithography_station_front{'_on' if lit else ''}.png")
    save(side, "block/lithography_station_side.png")
    t = top.copy()
    for x in range(4, 12):
        for y in range(4, 12):
            put(t, x, y, (0xE8, 0xC8, 0x40))
    save(t, "block/lithography_station_top.png")
    # травильная ванна — фторопластовая ванна, пузыри при работе
    for lit in (False, True):
        f = side.copy()
        for x in range(2, 14):
            for y in range(5, 14):
                put(f, x, y, (0xE8, 0xE8, 0xE4) if x in (2, 13) or y == 13 else (0xB0, 0xE0, 0xC0))
        if lit:
            for x, y in ((5, 7), (8, 9), (10, 6), (6, 11)):
                put(f, x, y, (0xF0, 0xFF, 0xF4))
        save(f, f"block/etch_bath_front{'_on' if lit else ''}.png")
    save(side, "block/etch_bath_side.png")
    t = top.copy()
    for x in range(2, 14):
        for y in range(2, 14):
            put(t, x, y, (0xB0, 0xE0, 0xC0) if 3 <= x <= 12 and 3 <= y <= 12 else (0xE8, 0xE8, 0xE4))
    save(t, "block/etch_bath_top.png")
    # кинетические: установка Чохральского и пила
    s = casing.copy()
    for x in range(5, 11):
        for y in range(2, 14):
            put(s, x, y, (0x2A, 0x2A, 0x30) if x in (5, 10) else (0x80, 0x88, 0x98))
    save(s, "block/crystal_puller_side.png")
    e = casing.copy()
    disc(e, 7.5, 7.5, 4, lambda x, y, d: (0xFF, 0x90, 0x40) if d < 2.5 else (0xE0, 0xE8, 0xF0))
    save(e, "block/crystal_puller_end.png")
    s = casing.copy()
    for x in range(1, 15):
        put(s, x, 8, (0xC0, 0xC8, 0xD8))
    for x in range(3, 13):
        put(s, x, 7, (0x90, 0x98, 0xA8))
        put(s, x, 9, (0x90, 0x98, 0xA8))
    save(s, "block/wafer_saw_side.png")
    e = casing.copy()
    disc(e, 7.5, 7.5, 6, lambda x, y, d: (0xE0, 0xE8, 0xF0) if 5 < d else ((0x30, 0x30, 0x34) if d < 3 else None))
    save(e, "block/wafer_saw_end.png")
    # фильтровентиляционный модуль — гофрированный HEPA за решёткой
    ffu = steel.copy()
    for x in range(1, 15):
        for y in range(1, 15):
            put(ffu, x, y, (0xF0, 0xF0, 0xEC) if x % 2 else (0xD0, 0xD0, 0xCC))
    for i in range(1, 15, 4):
        for j in range(16):
            put(ffu, i, j, (0x70, 0x74, 0x7C))
            put(ffu, j, i, (0x70, 0x74, 0x7C))
    _frame(ffu, (0x55, 0x58, 0x60, 255), 1)
    save(ffu, "block/fan_filter_unit.png")
    # баки из алюминиевых сплавов — светлее титана; Al-Li — зелёный грунт (как на стыках SLWT)
    for name, tint_color in (("aluminium_fuel_tank", (0xE4, 0xE8, 0xEC)), ("al_li_fuel_tank", (0xC0, 0xD8, 0xB8))):
        for lvl in range(5):
            src = mod(f"block/fuel_tank_side_{lvl}.png")
            out = src.copy()
            for x in range(16):
                for y in range(16):
                    r, g, b, a = src.getpixel((x, y))
                    # смотровое стекло с топливом (цветное) не трогаем, корпус перекрашиваем
                    if max(r, g, b) - min(r, g, b) < 24:
                        lum = (r + g + b) / (3 * 255)
                        out.putpixel((x, y), tuple(min(255, int(c * (0.35 + 0.8 * lum))) for c in tint_color) + (a,))
            save(out, f"block/{name}_side_{lvl}.png")
        save(_tint(mod("block/fuel_tank_top.png"), tint_color, 1.05), f"block/{name}_top.png")
    # монокристаллическая панель: чёрные псевдоквадраты, серебряные шины
    panel = Image.new("RGBA", (16, 16), (0x10, 0x12, 0x1A, 255))
    for x in range(16):
        for y in range(16):
            if x % 8 == 0 or y % 8 == 0:
                put(panel, x, y, (0x90, 0x94, 0xA0))
            elif x % 4 == 0:
                put(panel, x, y, (0xB8, 0xBC, 0xC8))
            elif (x % 8 in (1, 7)) and (y % 8 in (1, 7)):
                put(panel, x, y, (0x90, 0x94, 0xA0))
    save(panel, "block/mono_solar_panel_top.png")


# ---------------------------------------------------------------------------
# Окна
# ---------------------------------------------------------------------------

BG = (0xC6, 0xC6, 0xC6, 255)
LAYOUTS = {
    "chemical_reactor": ((72, 35), [(35, 17), (35, 35), (35, 53)], [(116, 17), (116, 35), (116, 53)]),
    "sabatier_reactor": ((72, 35), [(35, 26), (35, 48)], [(116, 17), (116, 35), (116, 53)]),
    "deposition_reactor": ((76, 36), [(26, 26), (26, 48), (50, 37)], [(116, 26), (116, 48)]),
    "diffusion_furnace": ((76, 35), [(26, 35), (50, 17), (50, 35), (50, 53)], [(116, 35)]),
    "lithography_station": ((76, 35), [(26, 35), (50, 26), (50, 48)], [(116, 35)]),
    "etch_bath": ((76, 35), [(26, 35), (50, 35)], [(116, 35)]),
}


def slot(img, x, y):
    x0, y0 = x - 1, y - 1
    for i in range(18):
        for j in range(18):
            dark = i == 0 or j == 0
            light = i == 17 or j == 17
            img.putpixel((x0 + i, y0 + j), (0x37, 0x37, 0x37, 255) if dark else
                         (0xFF, 0xFF, 0xFF, 255) if light else (0x8B, 0x8B, 0x8B, 255))


def guis():
    base = mod("gui/machine_single.png")
    arrow = base.crop((72, 35, 72 + 22, 35 + 16))
    for name, ((ax, ay), ins, outs) in LAYOUTS.items():
        img = base.copy()
        for x in range(7, 152):
            for y in range(14, 79):
                img.putpixel((x, y), BG)
        img.paste(arrow, (ax, ay))
        for x, y in ins + outs:
            slot(img, x, y)
        save(img, f"gui/{name}.png")
    furnace = base.copy()
    slot(furnace, 116, 57)
    save(furnace, "gui/machine_furnace.png")
    electro = mod("gui/electrolyzer.png").copy()
    slot(electro, 116, 26)
    slot(electro, 116, 48)
    save(electro, "gui/electrolyzer_chlor.png")
    refinery = mod("gui/refinery.png").copy()
    slot(refinery, 116, 57)
    save(refinery, "gui/refinery_silane.png")


def main():
    print("предметы 006:")
    items()
    print("блоки 006:")
    blocks()
    print("окна 006:")
    guis()


if __name__ == "__main__":
    main()
