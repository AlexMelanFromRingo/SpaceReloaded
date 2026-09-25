#!/usr/bin/env python3
"""Текстуры 009 «Навигация». Запуск: python3 tools/gen_textures_009.py"""
from PIL import Image

from gen_textures import save
from gen_textures_006 import put
from gen_textures_008 import blob, item_icon, noise_fill, ore

GOLD_FOIL = (0xD4, 0xA8, 0x40)
STEEL = (0x9A, 0xA0, 0xA8)
DARK = (0x30, 0x34, 0x3A)


def blocks():
    # галенит: свинцово-серые кубики с металлическим блеском
    ore("galena_ore", (0x80, 0x80, 0x80), [(0x5A, 0x60, 0x6C), (0xB4, 0xBC, 0xC8), (0x46, 0x4C, 0x56)], 9)
    # шина спутника в золотистой экранно-вакуумной изоляции со складками
    bus = noise_fill(GOLD_FOIL, 40, 5)
    for i in range(16):
        put(bus, i, (i * 5) % 16, (0xF0, 0xCC, 0x60))
        put(bus, (i * 7) % 16, i, (0xA8, 0x80, 0x28))
    save(bus, "block/hyperspectral_bus.png")
    # короб спектрометра: тёмный корпус, белый радиатор приёмника PbS
    box = Image.new("RGBA", (16, 16), DARK + (255,))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                put(box, x, y, STEEL)
            elif 3 <= x <= 12 and 3 <= y <= 12 and y % 2 == 1:
                put(box, x, y, (0xE6, 0xE8, 0xEA))
    save(box, "block/spectrometer_box.png")


def items():
    item_icon("galena", lambda img: blob(img, (0x70, 0x78, 0x84), edge=(0x40, 0x44, 0x4C)))

    def detector(img):
        for x in range(3, 13):
            for y in range(4, 12):
                put(img, x, y, (0xC8, 0xB0, 0x80) if x in (3, 12) or y in (4, 11) else (0x3A, 0x3E, 0x46))
        for x in range(6, 10):
            for y in range(6, 10):
                put(img, x, y, (0x20, 0x22, 0x28))
        for x in (5, 10):
            for y in range(12, 15):
                put(img, x, y, (0xC8, 0x80, 0x40))
    item_icon("pbs_detector", detector)

    def grating(img):
        for x in range(2, 14):
            for y in range(3, 13):
                # штрихи решётки разлагают свет в спектр
                hue = [(0xE0, 0x40, 0x40), (0xE0, 0xA0, 0x30), (0xD0, 0xD0, 0x40), (0x50, 0xC0, 0x50),
                       (0x40, 0x80, 0xE0), (0x80, 0x50, 0xD0)][(x + y) // 3 % 6]
                put(img, x, y, hue if x % 2 else (0xC8, 0xCC, 0xD0))
    item_icon("diffraction_grating", grating)

    def spectrometer(img):
        for x in range(2, 14):
            for y in range(4, 13):
                put(img, x, y, STEEL if x in (2, 13) or y in (4, 12) else DARK)
        for x in range(4, 12):
            put(img, x, 8, [(0xE0, 0x40, 0x40), (0xE0, 0xA0, 0x30), (0xD0, 0xD0, 0x40), (0x50, 0xC0, 0x50),
                            (0x40, 0x80, 0xE0), (0x80, 0x50, 0xD0), (0x60, 0x30, 0x60), (0x40, 0x20, 0x30)][x - 4])
        for y in range(1, 4):
            put(img, 7, y, STEEL)
            put(img, 8, y, STEEL)
    item_icon("ir_spectrometer", spectrometer)

    def mineral_map(img):
        for x in range(1, 15):
            for y in range(1, 15):
                put(img, x, y, (0x8C, 0x70, 0x48) if x in (1, 14) or y in (1, 14) else (0xE8, 0xDC, 0xB8))
        for x in range(3, 13):
            put(img, x, 12, [(0x60, 0xA0, 0xE0), (0x50, 0xA0, 0x50), (0xC0, 0x40, 0x30), (0x80, 0x50, 0x30),
                             (0xD0, 0xC0, 0x40), (0x70, 0x40, 0x90), (0xF0, 0xF0, 0xF0), (0x70, 0x70, 0x74),
                             (0x60, 0xA0, 0xE0), (0x50, 0xA0, 0x50)][x - 3])
    item_icon("mineral_map", mineral_map)

    def radar(img):
        for x in range(2, 14):
            for y in range(9, 13):
                put(img, x, y, STEEL if y in (9, 12) else DARK)
        # «бабочка» антенны
        for k in range(4):
            for x in range(3 + k, 7 - k // 2):
                put(img, x, 5 + k, (0xC8, 0x80, 0x40))
                put(img, 15 - x, 5 + k, (0xC8, 0x80, 0x40))
        put(img, 7, 10, (0x60, 0xE0, 0x80))
    item_icon("ground_radar", radar)

    def radargram(img):
        for x in range(1, 15):
            for y in range(1, 15):
                put(img, x, y, (0x8C, 0x70, 0x48) if x in (1, 14) or y in (1, 14) else (0x20, 0x22, 0x26))
        for x in range(2, 14):
            # гипербола эха над пустотой и полоса кровли
            y = 5 + int(((x - 7.5) ** 2) / 9)
            put(img, x, min(12, y), (0xF0, 0xF0, 0xF0))
            put(img, x, 11, (0x90, 0x94, 0x9A))
    item_icon("radargram", radargram)


def main():
    blocks()
    items()


if __name__ == "__main__":
    main()
