#!/usr/bin/env python3
"""
Генератор производных текстур SpaceReloaded.

Руды собираются композицией: маска рудных вкраплений снимается как разница
ванильной породы и уже нарисованной земной руды, затем накладывается на породу
другого тела. Так лунный титан выглядит титаном, но лежит в лунном камне.

Жидкости и вёдра рисуются из ванильных образцов, чтобы попадать в палитру игры.

Запуск: python3 tools/gen_textures.py
"""
import io
import os
import random
import zipfile

from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "mod/src/main/resources/assets/spacereloaded/textures")
CLIENT_JAR = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/26.2/minecraft-client-only.jar")

# Цвет топлива: керолокс — керосиновый янтарь, гидролокс — переохлаждённый
# голубой, метанокс — бледная синь сжиженного метана.
FUELS = {
    "kerolox": (0xC9, 0x8A, 0x3C),
    "hydrolox": (0x6F, 0xC8, 0xE8),
    "methalox": (0x8C, 0xA8, 0xD8),
}


def vanilla(path):
    with zipfile.ZipFile(CLIENT_JAR) as jar:
        with jar.open(f"assets/minecraft/textures/{path}") as handle:
            return Image.open(io.BytesIO(handle.read())).convert("RGBA")


def mod(path):
    return Image.open(os.path.join(ASSETS, path)).convert("RGBA")


def save(image, path):
    full = os.path.join(ASSETS, path)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    image.save(full)
    print("  ", path)


def ore_mask(base, ore, threshold=28):
    """Пиксели руды, заметно отличающиеся от породы-носителя."""
    mask = Image.new("L", ore.size, 0)
    for x in range(ore.width):
        for y in range(ore.height):
            br, bg, bb, _ = base.getpixel((x, y))
            orr, og, ob, oa = ore.getpixel((x, y))
            delta = abs(br - orr) + abs(bg - og) + abs(bb - ob)
            if oa > 0 and delta > threshold:
                mask.putpixel((x, y), 255)
    return mask


def transplant_ore(host, base, ore, name):
    mask = ore_mask(base, ore)
    out = host.copy()
    out.paste(ore, (0, 0), mask)
    save(out, f"block/{name}.png")


def fluid_still(color, name):
    """Спокойная поверхность: лёгкая рябь по яркости, кадры анимации по вертикали."""
    rng = random.Random(hash(name) & 0xFFFF)
    frames = 8
    image = Image.new("RGBA", (16, 16 * frames))
    for frame in range(frames):
        for y in range(16):
            for x in range(16):
                wave = 0.06 * ((x + y + frame * 2) % 5) - 0.12
                noise = rng.uniform(-0.04, 0.04)
                factor = 1.0 + wave + noise
                pixel = tuple(min(255, max(0, int(c * factor))) for c in color)
                image.putpixel((x, frame * 16 + y), pixel + (255,))
    save(image, f"block/{name}_still.png")
    write_mcmeta(f"block/{name}_still.png.mcmeta", frames=frames, frametime=3)


def fluid_flow(color, name):
    """Течение: продольные полосы, кадры со сдвигом. Ширина кадра 32 по канону."""
    frames = 16
    image = Image.new("RGBA", (32, 32 * frames))
    dark = tuple(int(c * 0.72) for c in color)
    for frame in range(frames):
        for y in range(32):
            for x in range(32):
                band = ((y + frame * 2) // 2 + x // 8) % 3
                pixel = color if band else dark
                image.putpixel((x, frame * 32 + y), pixel + (255,))
    save(image, f"block/{name}_flow.png")
    write_mcmeta(f"block/{name}_flow.png.mcmeta", frames=frames, frametime=2)


def write_mcmeta(path, frames, frametime):
    full = os.path.join(ASSETS, path)
    with open(full, "w", encoding="utf-8") as handle:
        handle.write('{\n\t"animation": {\n\t\t"frametime": %d\n\t}\n}\n' % frametime)
    print("  ", path)


def fuel_bucket(color, name):
    """Ванильное ведро воды, синева перекрашена в цвет топлива."""
    water = vanilla("item/water_bucket.png")
    out = water.copy()
    for x in range(out.width):
        for y in range(out.height):
            r, g, b, a = out.getpixel((x, y))
            if a == 0:
                continue
            # Жидкость в ванильном ведре синяя: синий заметно преобладает
            if b > r + 24 and b > g + 8:
                shade = b / 255.0
                out.putpixel((x, y), tuple(
                    min(255, int(c * (0.55 + 0.65 * shade))) for c in color) + (a,))
    save(out, f"item/{name}_bucket.png")


def heat_shield():
    """Абляционный экран: тёмные соты с раскалённой кромкой."""
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    for x in range(16):
        for y in range(16):
            dx, dy = x - 7.5, y - 7.5
            radius = (dx * dx + dy * dy) ** 0.5
            if radius > 7.6:
                continue
            if radius > 6.4:
                image.putpixel((x, y), (0xC4, 0x5A, 0x1E, 255))
            elif (x // 2 + y // 2) % 2 == 0:
                image.putpixel((x, y), (0x2B, 0x2A, 0x2E, 255))
            else:
                image.putpixel((x, y), (0x3A, 0x38, 0x3E, 255))
    save(image, "item/heat_shield.png")


def stage_separator():
    """Разделитель ступеней: тёмное стальное кольцо с оранжевыми пироболтами,
    торец — силовая плита с крестовиной и центральным проёмом."""
    steel = (0x4A, 0x4E, 0x55)
    light = (0x6C, 0x72, 0x7A)
    dark = (0x2E, 0x31, 0x36)
    bolt = (0xE0, 0x7A, 0x1E)
    rng = random.Random(0x5EA)
    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = light if 6 <= y <= 9 else steel
            n = rng.randint(-8, 8)
            side.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    for y in (5, 10):
        for x in range(16):
            side.putpixel((x, y), dark + (255,))
    for bx in (2, 6, 10, 14):
        for dy in (7, 8):
            for dx in (0, 1):
                side.putpixel(((bx + dx) % 16, dy), bolt + (255,))
    save(side, "block/stage_separator_side.png")

    end = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            n = rng.randint(-6, 6)
            end.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in steel) + (255,))
    for i in range(16):
        for c in (7, 8):
            end.putpixel((i, c), light + (255,))
            end.putpixel((c, i), light + (255,))
    for y in range(5, 11):
        for x in range(5, 11):
            end.putpixel((x, y), dark + (255,))
    for x, y in ((1, 1), (14, 1), (1, 14), (14, 14)):
        end.putpixel((x, y), bolt + (255,))
    save(end, "block/stage_separator_end.png")


def cargo_terminal():
    """Грузовой терминал (003): тёмная стальная стойка с оранжевой полосой линии
    и зелёным экраном состояния на верхней грани."""
    steel = (0x3E, 0x43, 0x4A)
    light = (0x5C, 0x63, 0x6B)
    stripe = (0xE0, 0x7A, 0x1E)
    screen = (0x2E, 0xC4, 0x6B)
    screen_dark = (0x14, 0x3A, 0x26)
    rng = random.Random(0xCA60)
    side = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = light if y in (0, 15) or x in (0, 15) else steel
            n = rng.randint(-6, 6)
            side.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    for x in range(2, 14):
        for y in (6, 7):
            side.putpixel((x, y), stripe + (255,))
    for x, y in ((11, 5), (12, 6), (12, 7), (11, 8)):
        side.putpixel((x, y), (0xFF, 0xD0, 0x80, 255))
    save(side, "block/cargo_terminal_side.png")

    top = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            base = light if y in (0, 15) or x in (0, 15) else steel
            n = rng.randint(-6, 6)
            top.putpixel((x, y), tuple(max(0, min(255, c + n)) for c in base) + (255,))
    for y in range(3, 13):
        for x in range(3, 13):
            top.putpixel((x, y), screen_dark + (255,))
    for x in range(4, 12):
        top.putpixel((x, 5), screen + (255,))
        top.putpixel((x, 8), screen + (255,))
    for x in range(4, 9):
        top.putpixel((x, 11), screen + (255,))
    save(top, "block/cargo_terminal_top.png")


def docking_port():
    """Стыковочный порт (003): лицевая грань — люк с направляющим кольцом захвата."""
    ring = (0xD8, 0xB4, 0x5A)
    dark = (0x2A, 0x2D, 0x31)
    for source, name in (("block/hermetic_hatch.png", "block/docking_port_front.png"),
                         ("block/hermetic_hatch_open.png", "block/docking_port_open.png")):
        image = mod(source).copy()
        for i in range(16):
            for c in (1, 14):
                image.putpixel((i, c), ring + (255,))
                image.putpixel((c, i), ring + (255,))
        for x, y in ((0, 0), (15, 0), (0, 15), (15, 15)):
            image.putpixel((x, y), dark + (255,))
        save(image, name)


def module_hull():
    """Обшивка модуля (003): стенка бака в раме обшивки — «этот модуль был ракетой»."""
    tank = mod("block/fuel_tank_side.png")
    frame = mod("block/hull_plating.png")
    image = tank.copy()
    for i in range(16):
        for c in (0, 15):
            image.putpixel((i, c), frame.getpixel((i, c)))
            image.putpixel((c, i), frame.getpixel((c, i)))
    save(image, "block/module_hull.png")


def mars_ice():
    """Марсианский лёд (003): красный песчаник с прожилками грунтового льда."""
    base = vanilla("block/red_sandstone.png").copy()
    ice = (0xBF, 0xE3, 0xF2)
    ice_dark = (0x8C, 0xC1, 0xD9)
    rng = random.Random(0x3A25)
    for _ in range(5):
        x, y = rng.randint(1, 14), rng.randint(1, 14)
        for step in range(rng.randint(3, 6)):
            base.putpixel((x % 16, y % 16), (ice if step % 2 == 0 else ice_dark) + (255,))
            dx, dy = rng.choice(((1, 0), (0, 1), (1, 1), (-1, 1)))
            x, y = (x + dx) % 16, (y + dy) % 16
    save(base, "block/mars_ice.png")


# ---------------------------------------------------------------------------
# Лунная индустрия (004): катапульта, ловушка масс, реголитовый реактор
# ---------------------------------------------------------------------------

def _shade(color, factor):
    r, g, b = color[:3]
    return (max(0, min(255, int(r * factor))), max(0, min(255, int(g * factor))),
            max(0, min(255, int(b * factor))), 255)


def _frame(image, color, width=1):
    for i in range(16):
        for w in range(width):
            for xy in ((i, w), (i, 15 - w), (w, i), (15 - w, i)):
                image.putpixel(xy, color)


def coil(name, band, band_dark, core):
    """Секция рельса: стальной сердечник, обмотка полосами поперёк оси, торец — кольцо."""
    steel = vanilla("block/iron_block.png")
    side = steel.copy()
    for x in range(16):
        for y in range(2, 14):
            if x % 4 in (1, 2):
                side.putpixel((x, y), band if (x + y) % 3 else band_dark)
            elif x % 4 == 3:
                side.putpixel((x, y), band_dark)
    _frame(side, _shade(steel.getpixel((8, 8)), 0.55))
    save(side, f"block/{name}_side.png")
    end = steel.copy()
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if 4.5 <= d <= 7.2:
                end.putpixel((x, y), band if int(d * 2) % 2 else band_dark)
            elif d < 3:
                end.putpixel((x, y), core)
    save(end, f"block/{name}_end.png")
    # Верх секции в составе рельса: направляющая для салазок вдоль оси
    top = side.copy()
    rail = _shade(steel.getpixel((8, 8)), 0.35)
    for x in range(16):
        for y in (6, 9):
            top.putpixel((x, y), rail)
        for y in (7, 8):
            top.putpixel((x, y), _shade(steel.getpixel((8, 8)), 1.15))
    save(top, f"block/{name}_rail.png")


def mass_driver_breech():
    base = vanilla("block/netherite_block.png")
    side = base.copy()
    _frame(side, (0x2A, 0x2A, 0x30, 255), 2)
    for y in range(5, 11):
        side.putpixel((3, y), (0xE0, 0x88, 0x30, 255))
        side.putpixel((12, y), (0xE0, 0x88, 0x30, 255))
    save(side, "block/mass_driver_breech_side.png")
    front = base.copy()
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 3.2:
                front.putpixel((x, y), (0x08, 0x0A, 0x10, 255))
            elif d < 5.2:
                front.putpixel((x, y), (0x55, 0xC8, 0xFF, 255) if (x + y) % 2 else (0x2C, 0x7A, 0xB8, 255))
    _frame(front, (0x2A, 0x2A, 0x30, 255), 2)
    save(front, "block/mass_driver_breech_front.png")
    top = base.copy()
    for x in range(3, 13):
        top.putpixel((x, 7), (0x7A, 0x7A, 0x80, 255))
        top.putpixel((x, 8), (0x7A, 0x7A, 0x80, 255))
    _frame(top, (0x2A, 0x2A, 0x30, 255), 1)
    save(top, "block/mass_driver_breech_top.png")


def capacitor():
    base = vanilla("block/iron_block.png")
    side = base.copy()
    for cell in range(3):
        x0 = 1 + cell * 5
        for x in range(x0, x0 + 4):
            for y in range(3, 14):
                side.putpixel((x, y), (0x2C, 0x5C, 0xC8, 255) if x in (x0, x0 + 3) else (0x3A, 0x7B, 0xFF, 255))
        side.putpixel((x0 + 1, 2), (0xC8, 0x90, 0x50, 255))
        side.putpixel((x0 + 2, 2), (0xC8, 0x90, 0x50, 255))
    save(side, "block/capacitor_side.png")
    top = base.copy()
    for x, y in ((4, 4), (11, 4), (4, 11), (11, 11)):
        for dx in range(2):
            for dy in range(2):
                top.putpixel((x + dx, y + dy), (0xC8, 0x90, 0x50, 255))
    save(top, "block/capacitor_top.png")


def mass_driver_sled():
    steel = vanilla("block/iron_block.png").copy()
    for x in range(16):
        steel.putpixel((x, 0), (0xE0, 0x88, 0x30, 255))
        steel.putpixel((x, 15), (0xE0, 0x88, 0x30, 255))
    save(steel, "block/mass_driver_sled.png")


def mass_catcher():
    base = vanilla("block/iron_block.png")
    top = base.copy()
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 7.4 and int(d) % 3 == 0:
                top.putpixel((x, y), (0xD0, 0x40, 0x30, 255))
    save(top, "block/mass_catcher_top.png")
    side = base.copy()
    _frame(side, (0x55, 0x55, 0x5A, 255), 1)
    for x in range(2, 14):
        side.putpixel((x, 12), (0xD0, 0x40, 0x30, 255))
    save(side, "block/mass_catcher_side.png")


def catcher_net():
    image = Image.new("RGBA", (16, 16), (0x22, 0x24, 0x28, 255))
    fiber = (0x5A, 0x5E, 0x66, 255)
    knot = (0x9A, 0x9E, 0xA8, 255)
    for i in range(16):
        for j in range(16):
            if i % 4 == 0 or j % 4 == 0:
                image.putpixel((i, j), knot if (i % 4 == 0 and j % 4 == 0) else fiber)
    save(image, "block/catcher_net.png")


def regolith_reactor():
    lining = mod("block/refractory_lining.png") if os.path.exists(
        os.path.join(ASSETS, "block/refractory_lining.png")) else vanilla("block/deepslate_bricks.png")
    for lit in (False, True):
        front = vanilla("block/blast_furnace_side.png").copy()
        for x in range(4, 12):
            for y in range(5, 11):
                if lit:
                    heat = 1.0 - abs(y - 8) / 4.0
                    front.putpixel((x, y), (0xFF, int(0x80 + 0x60 * heat), int(0x20 + 0x40 * heat), 255))
                else:
                    front.putpixel((x, y), (0x18, 0x1A, 0x22, 255))
        for x in range(3, 13):
            front.putpixel((x, 4), (0x70, 0x70, 0x78, 255))
            front.putpixel((x, 11), (0x70, 0x70, 0x78, 255))
        for y in range(4, 12):
            front.putpixel((3, y), (0x70, 0x70, 0x78, 255))
            front.putpixel((12, y), (0x70, 0x70, 0x78, 255))
        save(front, "block/regolith_reactor_front_lit.png" if lit else "block/regolith_reactor_front.png")
    save(lining.copy(), "block/regolith_reactor_side.png")


def refractory_lining():
    bricks = vanilla("block/stone_bricks.png")
    tint = (0xC9, 0xB8, 0x9A)
    out = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            r, g, b, a = bricks.getpixel((x, y))
            lum = (r + g + b) / (3 * 255)
            out.putpixel((x, y), (int(tint[0] * lum * 1.2), int(tint[1] * lum * 1.2), int(tint[2] * lum * 1.2), 255))
    save(out, "block/refractory_lining.png")


def lunar_bricks():
    bricks = vanilla("block/stone_bricks.png")
    moon = mod("block/moon_stone.png")
    out = Image.new("RGBA", (16, 16))
    for x in range(16):
        for y in range(16):
            br, bg, bb, _ = bricks.getpixel((x, y))
            mr, mg, mb, _ = moon.getpixel((x, y))
            lum = (br + bg + bb) / (3 * 150.0)
            out.putpixel((x, y), (min(255, int(mr * lum)), min(255, int(mg * lum)), min(255, int(mb * lum)), 255))
    save(out, "block/lunar_bricks.png")


def sintered_regolith():
    base = mod("block/moon_regolith.png").copy()
    rng = random.Random(0x5117)
    for x in range(16):
        for y in range(16):
            r, g, b, a = base.getpixel((x, y))
            f = 0.72 + rng.random() * 0.12
            base.putpixel((x, y), (int(r * f), int(g * f), int(b * f * 1.05), 255))
    for _ in range(10):
        x, y = rng.randint(0, 15), rng.randint(0, 15)
        base.putpixel((x, y), (0x9A, 0xA0, 0xB0, 255))  # стекловидные включения
    save(base, "block/sintered_regolith.png")


def cargo_pod_item():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    hull = (0xB8, 0xBC, 0xC4, 255)
    dark = (0x6A, 0x6E, 0x78, 255)
    shield = (0x5A, 0x3A, 0x28, 255)
    for y in range(2, 14):
        half = 3 + min(y - 2, 3)
        for x in range(8 - half, 8 + half):
            image.putpixel((x, y), hull if x not in (8 - half, 8 + half - 1) else dark)
    for x in range(2, 14):
        image.putpixel((x, 13), shield)
        image.putpixel((x, 14), shield)
    save(image, "item/cargo_pod.png")


def slag_item():
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    rng = random.Random(0x51A6)
    for _ in range(3):
        cx, cy, r = rng.randint(4, 11), rng.randint(5, 11), rng.randint(3, 4)
        for x in range(16):
            for y in range(16):
                if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                    v = rng.randint(0x30, 0x55)
                    image.putpixel((x, y), (v, v - 6, v - 10, 255))
    save(image, "item/slag.png")


def regolith_reactor_gui():
    """GUI реактора: фон электролизёра + три выходных слота (80/98/116, 58)."""
    gui = mod("gui/electrolyzer.png").copy()
    for i in range(3):
        x0, y0 = 79 + i * 18, 57
        for x in range(x0, x0 + 18):
            for y in range(y0, y0 + 18):
                edge_dark = x == x0 or y == y0
                edge_light = x == x0 + 17 or y == y0 + 17
                color = (0x37, 0x37, 0x37, 255) if edge_dark else (0xFF, 0xFF, 0xFF, 255) if edge_light \
                    else (0x8B, 0x8B, 0x8B, 255)
                gui.putpixel((x, y), color)
    save(gui, "gui/regolith_reactor.png")


def lunar_industry():
    coil("steel_coil", (0xC8, 0x7A, 0x48, 255), (0x8C, 0x4E, 0x2A, 255), (0x50, 0x50, 0x58, 255))
    coil("superconducting_coil", (0x7F, 0xD8, 0xF0, 255), (0x3A, 0x8C, 0xB8, 255), (0xE8, 0xF6, 0xFF, 255))
    mass_driver_breech()
    capacitor()
    mass_driver_sled()
    mass_catcher()
    catcher_net()
    refractory_lining()
    regolith_reactor()
    lunar_bricks()
    sintered_regolith()
    cargo_pod_item()
    slag_item()
    regolith_reactor_gui()


# ---------------------------------------------------------------------------
# Инженерия (005): валы, шестерни, машины, детали
# ---------------------------------------------------------------------------

def _tint(image, tint, gain=1.0):
    out = Image.new("RGBA", image.size)
    for x in range(image.width):
        for y in range(image.height):
            r, g, b, a = image.getpixel((x, y))
            lum = (r + g + b) / (3 * 255)
            out.putpixel((x, y), (min(255, int(tint[0] * lum * gain)), min(255, int(tint[1] * lum * gain)),
                                  min(255, int(tint[2] * lum * gain)), a))
    return out


def _icon(pixels_fn, name):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    pixels_fn(image)
    save(image, f"item/{name}.png")


def engineering():
    iron = vanilla("block/iron_block.png")
    save(_tint(vanilla("block/stripped_oak_log.png"), (0xC8, 0x9A, 0x60), 1.1), "block/wooden_shaft.png")
    shaft = iron.copy()
    for y in range(16):
        for x in (3, 12):
            shaft.putpixel((x, y), (0x70, 0x72, 0x78, 255))
    save(shaft, "block/steel_shaft.png")
    gear = _tint(vanilla("block/copper_block.png"), (0xD8, 0xB0, 0x60), 1.05)  # латунь
    save(gear, "block/gear.png")
    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    gearbox = casing.copy()
    for i in range(4, 12):
        gearbox.putpixel((i, 7), (0xD8, 0xB0, 0x60, 255))
        gearbox.putpixel((7, i), (0xD8, 0xB0, 0x60, 255))
    save(gearbox, "block/gearbox.png")
    clutch_side = casing.copy()
    for x in range(16):
        for y in (5, 10):
            clutch_side.putpixel((x, y), (0x8A, 0x40, 0x30, 255))
    save(clutch_side, "block/clutch_side.png")
    clutch_end = casing.copy()
    for x in range(6, 10):
        for y in range(6, 10):
            clutch_end.putpixel((x, y), (0x70, 0x72, 0x78, 255))
    save(clutch_end, "block/clutch_end.png")
    motor_side = iron.copy()
    for x in range(2, 14):
        for y in range(4, 12):
            motor_side.putpixel((x, y), (0xB8, 0x6A, 0x30, 255) if (x % 2) else (0x8C, 0x4E, 0x2A, 255))
    _frame(motor_side, (0x3A, 0x3C, 0x44, 255), 1)
    save(motor_side, "block/motor_side.png")
    motor_end = iron.copy()
    for x in range(16):
        for y in range(16):
            d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
            if d < 2.2:
                motor_end.putpixel((x, y), (0x70, 0x72, 0x78, 255))
            elif 4 < d < 6.5 and (x + y) % 3 == 0:
                motor_end.putpixel((x, y), (0x30, 0x32, 0x38, 255))
    save(motor_end, "block/motor_end.png")
    fly = vanilla("block/netherite_block.png").copy()
    for x in range(16):
        fly.putpixel((x, 7), (0x90, 0x92, 0x98, 255))
    save(fly, "block/flywheel.png")
    press_side = iron.copy()
    for y in range(3, 13):
        press_side.putpixel((2, y), (0x3A, 0x3C, 0x44, 255))
        press_side.putpixel((13, y), (0x3A, 0x3C, 0x44, 255))
    for x in range(2, 14):
        press_side.putpixel((x, 3), (0xE0, 0x88, 0x30, 255))
    save(press_side, "block/mechanical_press_side.png")
    press_top = iron.copy()
    _frame(press_top, (0x3A, 0x3C, 0x44, 255), 2)
    save(press_top, "block/mechanical_press_top.png")
    ram = vanilla("block/netherite_block.png").copy()
    save(ram, "block/press_ram.png")
    lathe_side = casing.copy()
    for x in range(1, 15):
        lathe_side.putpixel((x, 11), (0x3A, 0x3C, 0x44, 255))
    for x in range(3, 8):
        for y in range(5, 10):
            lathe_side.putpixel((x, y), (0x90, 0x92, 0x98, 255))
    save(lathe_side, "block/lathe_side.png")
    lathe_end = casing.copy()
    for x in range(5, 11):
        for y in range(5, 11):
            lathe_end.putpixel((x, y), (0x90, 0x92, 0x98, 255))
    save(lathe_end, "block/lathe_end.png")

    def plate(color):
        def draw(img):
            for x in range(2, 14):
                for y in range(5, 11):
                    shade = 1.15 if y == 5 else (0.75 if y == 10 else 1.0)
                    img.putpixel((x, y), tuple(min(255, int(c * shade)) for c in color) + (255,))
        return draw
    _icon(plate((0xA8, 0xAC, 0xB4)), "steel_plate")
    _icon(plate((0xD8, 0x84, 0x50)), "copper_plate")
    _icon(plate((0xB8, 0xC8, 0xD8)), "titanium_alloy_plate")

    def turbopump(img, done):
        body = (0xB8, 0xBC, 0xC4, 255) if done else (0x88, 0x8C, 0x94, 255)
        for x in range(16):
            for y in range(16):
                d = ((x - 7.5) ** 2 + (y - 8.5) ** 2) ** 0.5
                if d < 5.5:
                    img.putpixel((x, y), body)
                if d < 2:
                    img.putpixel((x, y), (0x40, 0x42, 0x48, 255))
        for x in range(12, 16):
            img.putpixel((x, 4), body)
            img.putpixel((x, 5), body)
    _icon(lambda i: turbopump(i, True), "turbopump")
    _icon(lambda i: turbopump(i, False), "incomplete_turbopump")

    def injector(img, done):
        c = (0xD8, 0x84, 0x50, 255) if done else (0xA0, 0x60, 0x38, 255)
        for x in range(16):
            for y in range(16):
                d = ((x - 7.5) ** 2 + (y - 7.5) ** 2) ** 0.5
                if d < 6.5:
                    img.putpixel((x, y), c)
                    if done and (x % 3 == 1 and y % 3 == 1) and d < 5.5:
                        img.putpixel((x, y), (0x30, 0x20, 0x18, 255))
    _icon(lambda i: injector(i, True), "injector_plate")
    _icon(lambda i: injector(i, False), "incomplete_injector")

    def nozzle(img, done):
        c = (0xD8, 0x84, 0x50, 255) if done else (0xA0, 0x60, 0x38, 255)
        for y in range(2, 15):
            half = 2 + (y - 2) // 2
            for x in range(8 - half, 8 + half):
                img.putpixel((x, y), c if (x - (8 - half)) % 3 else (0x70, 0x40, 0x28, 255))
    _icon(lambda i: nozzle(i, True), "regen_nozzle")
    _icon(lambda i: nozzle(i, False), "incomplete_nozzle")


def engineering_multiblocks():
    iron = vanilla("block/iron_block.png")
    cell = iron.copy()
    for x in range(3, 13):
        for y in range(3, 13):
            cell.putpixel((x, y), (0x3A, 0x7B, 0xC8, 255) if (y > 5) else (0xB0, 0xD8, 0xF0, 255))
    for x in range(16):
        cell.putpixel((x, 1), (0xD8, 0xB0, 0x60, 255))
        cell.putpixel((x, 14), (0xD8, 0xB0, 0x60, 255))
    save(cell, "block/electrolysis_cell.png")
    tray = iron.copy()
    _frame(tray, (0x5A, 0x5E, 0x66, 255), 1)
    for x in range(2, 14, 3):
        for y in range(2, 14, 3):
            tray.putpixel((x, y), (0x20, 0x22, 0x28, 255))
    save(tray, "block/distillation_tray.png")
    tray_side = iron.copy()
    for x in range(16):
        tray_side.putpixel((x, 7), (0x5A, 0x5E, 0x66, 255))
        tray_side.putpixel((x, 8), (0x5A, 0x5E, 0x66, 255))
    for x in range(5, 11):
        for y in range(10, 14):
            tray_side.putpixel((x, y), (0xC9, 0x8A, 0x3C, 255))  # окошко с керосиновой фракцией
    save(tray_side, "block/distillation_tray_side.png")

    def hammer(img):
        for i in range(3, 14):
            img.putpixel((i, 15 - i), (0x8C, 0x5E, 0x34, 255))
            img.putpixel((i + 1, 15 - i), (0x6A, 0x44, 0x24, 255))
        for x in range(7, 15):
            for y in range(1, 5):
                img.putpixel((x, y), (0xA8, 0xAC, 0xB4, 255) if y > 1 else (0xD0, 0xD4, 0xDC, 255))
    _icon(hammer, "engineer_hammer")

    def manual(img):
        for x in range(3, 13):
            for y in range(2, 15):
                img.putpixel((x, y), (0x2C, 0x5C, 0xC8, 255))
        for y in range(2, 15):
            img.putpixel((3, y), (0x1A, 0x3A, 0x80, 255))
        for x in range(5, 11):
            img.putpixel((x, 5), (0xE0, 0x88, 0x30, 255))
            img.putpixel((x, 7), (0xD8, 0xD8, 0xE0, 255))
            img.putpixel((x, 9), (0xD8, 0xD8, 0xE0, 255))
    _icon(manual, "engineer_manual")


def formed_multiblocks():
    """Облик сформированных мультиблоков (005): шина стека, обечайка колонны, оболочка реактора."""
    cell = mod("block/electrolysis_cell.png").copy()
    for x in range(16):
        for y in range(0, 3):
            cell.putpixel((x, y), (0xE8, 0xC0, 0x68, 255) if y == 1 else (0xB0, 0x88, 0x40, 255))
    for x in range(4, 12):
        for y in range(6, 12):
            cell.putpixel((x, y), (0x55, 0xC8, 0xFF, 255) if (x + y) % 3 else (0x9C, 0xE4, 0xFF, 255))
    save(cell, "block/electrolysis_cell_formed.png")
    top = Image.new("RGBA", (16, 16), (0xB0, 0x88, 0x40, 255))
    for x in range(16):
        for y in (6, 7, 8, 9):
            top.putpixel((x, y), (0xE8, 0xC0, 0x68, 255))
    save(top, "block/electrolysis_cell_formed_top.png")
    shell = vanilla("block/iron_block.png").copy()
    for y in range(16):
        for x in (0, 15):
            shell.putpixel((x, y), (0x6A, 0x6E, 0x78, 255))
    for x in range(16):
        shell.putpixel((x, 0), (0x5A, 0x5E, 0x66, 255))
    for x in range(3, 13, 3):
        shell.putpixel((x, 2), (0x40, 0x42, 0x48, 255))
    for x in range(6, 10):
        for y in range(8, 12):
            shell.putpixel((x, y), (0xC9, 0x8A, 0x3C, 255))
    save(shell, "block/distillation_column.png")
    lining = mod("block/refractory_lining.png")
    plate = _tint(vanilla("block/iron_block.png"), (0xC0, 0xB0, 0x98), 1.0)
    for x in range(16):
        for y in range(16):
            if x in (1, 14) and y in (1, 14):
                plate.putpixel((x, y), (0x50, 0x48, 0x40, 255))
    for x in range(16):
        plate.putpixel((x, 7), (0xE0, 0x70, 0x28, 255))
    save(plate, "block/refractory_lining_formed.png")


def main():
    print("руды:")
    transplant_ore(mod("block/moon_stone.png"), vanilla("block/stone.png"),
                   mod("block/titanium_ore.png"), "moon_titanium_ore")
    transplant_ore(vanilla("block/red_sandstone.png"), vanilla("block/deepslate.png"),
                   mod("block/deepslate_tungsten_ore.png"), "mars_tungsten_ore")

    print("жидкости:")
    for name, color in FUELS.items():
        fluid_still(color, name)
        fluid_flow(color, name)

    print("предметы:")
    for name, color in FUELS.items():
        fuel_bucket(color, name)
    heat_shield()
    stage_separator()
    cargo_terminal()
    docking_port()
    module_hull()
    mars_ice()
    print("лунная индустрия:")
    lunar_industry()
    print("инженерия:")
    engineering()
    engineering_multiblocks()
    formed_multiblocks()


if __name__ == "__main__":
    main()
