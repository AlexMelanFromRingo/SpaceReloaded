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


if __name__ == "__main__":
    main()
