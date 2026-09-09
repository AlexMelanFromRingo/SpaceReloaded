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


if __name__ == "__main__":
    main()
