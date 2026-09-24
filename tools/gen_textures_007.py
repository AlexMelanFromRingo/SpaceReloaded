#!/usr/bin/env python3
"""Текстуры 007 «Жизнь на станции». Запуск: python3 tools/gen_textures_007.py"""
from PIL import Image

from gen_textures import _frame, _shade, _tint, mod, save, vanilla
from gen_textures_006 import blank, disc, icon, pile, put, slot, BG

STEEL = (0xA8, 0xAC, 0xB4)
# маркировка баллонов (ГОСТ 949): кислород — голубой, азот — чёрный с жёлтой полосой
GAS_COLORS = {"none": (0x9C, 0xA0, 0xA8), "oxygen": (0x4C, 0x9C, 0xE0), "nitrogen": (0x2A, 0x2C, 0x30)}


def items():
    pile("lithium_hydroxide", [(0xF4, 0xF4, 0xF8), (0xE0, 0xE4, 0xE8)], 7)
    img = blank()
    for x in range(4, 12):
        for y in range(2, 15):
            put(img, x, y, _shade(STEEL, 1.2 if x == 5 else 0.75 if x == 11 else 1.0))
    for x in range(5, 11):
        for y in range(5, 12):
            put(img, x, y, (0xF4, 0xF4, 0xF8) if (x + y) % 3 else (0xD8, 0xDC, 0xE0))
    icon("lioh_cartridge", img)
    img = blank()
    for i, (x, y) in enumerate(((5, 5), (9, 4), (7, 8), (4, 10), (10, 9), (8, 12), (11, 13), (5, 13))):
        disc(img, x, y, 1.6, lambda a, b, d: (0xD8, 0xC8, 0x98) if d < 1.1 else (0xB8, 0xA8, 0x78))
    icon("zeolite", img)
    img = blank()
    for x in range(2, 14):
        for y in range(3, 13):
            edge = x in (2, 13) or y in (3, 12)
            put(img, x, y, _shade(STEEL, 0.7) if edge else ((0xD8, 0xC8, 0x98) if (x * 3 + y) % 4 else (0xB8, 0xA8, 0x78)))
    for x in range(3, 13, 3):
        put(img, x, 2, (0xD8, 0x84, 0x50))
    icon("zeolite_bed", img)


def blocks():
    steel = vanilla("block/iron_block.png")
    for gas, color in GAS_COLORS.items():
        for fill in range(5):
            img = _tint(steel, color, 1.3)
            if gas == "nitrogen":
                for x in range(16):
                    put(img, x, 3, (0xE8, 0xC8, 0x30))
            # манометр: шкала заполнения
            for y in range(5, 13):
                put(img, 12, y, (0x20, 0x20, 0x24))
                put(img, 14, y, (0x20, 0x20, 0x24))
            for y in range(12 - 2 * fill, 13):
                if y >= 5:
                    put(img, 13, y, (0x60, 0xE0, 0x70))
            _frame(img, _shade(color, 0.55), 1)
            save(img, f"block/gas_tank_{gas}_{fill}.png")
    top = _tint(steel, (0xB0, 0xB4, 0xBC), 1.2)
    disc(top, 7.5, 7.5, 3, lambda x, y, d: (0x50, 0x54, 0x5C) if d > 1.5 else (0xD8, 0x84, 0x50))
    save(top, "block/gas_tank_top.png")

    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    # воздухоразделитель: «холодный ящик» с инеем и ректификационной колонной
    side = casing.copy()
    for x in range(5, 11):
        for y in range(1, 15):
            put(side, x, y, (0xE8, 0xF0, 0xF8) if (x + y * 3) % 5 else (0xC8, 0xD8, 0xE8))
    for y in range(2, 14, 3):
        for x in range(5, 11):
            put(side, x, y, (0x7A, 0x80, 0x8A))
    save(side, "block/air_separator_side.png")
    top = casing.copy()
    disc(top, 7.5, 7.5, 4, lambda x, y, d: (0xE8, 0xF0, 0xF8) if d > 2 else (0x4C, 0x9C, 0xE0))
    save(top, "block/air_separator_top.png")
    # насос шлюза: пластинчато-роторный насос с патрубком
    side = casing.copy()
    disc(side, 7.5, 8.5, 5, lambda x, y, d: (0x3A, 0x3C, 0x44) if d > 4 else (0x80, 0x84, 0x90) if d > 1.5 else (0x30, 0x30, 0x34))
    for x in range(1, 4):
        for y in range(6, 11):
            put(side, x, y, (0xD8, 0x84, 0x50))
    save(side, "block/airlock_pump_side.png")
    top = casing.copy()
    for x in range(4, 12):
        for y in range(4, 12):
            put(top, x, y, (0x50, 0x54, 0x5C) if (x + y) % 2 else (0x60, 0x64, 0x6C))
    save(top, "block/airlock_pump_top.png")
    # поглотитель CO₂: решётка вентилятора и окно с сорбентом (белеет при работе)
    for lit in (False, True):
        f = casing.copy()
        for x in range(3, 13):
            for y in range(3, 13):
                put(f, x, y, (0x40, 0x42, 0x48) if (x % 2 == 0 or y % 2 == 0) else ((0xF0, 0xF4, 0xF8) if lit else (0xB0, 0xB4, 0xBC)))
        save(f, f"block/co2_scrubber_front{'_on' if lit else ''}.png")
    save(casing, "block/co2_scrubber_side.png")
    top = casing.copy()
    disc(top, 7.5, 7.5, 5, lambda x, y, d: (0x40, 0x42, 0x48) if int(d * 2) % 2 else None)
    save(top, "block/co2_scrubber_top.png")


def greenhouse():
    img = blank()
    for i in range(10):
        x = 3 + i
        for y in range(4 + (i % 3), 14):
            put(img, x, y, (0xE0, 0xC8, 0x70) if (x + y) % 3 else (0xC0, 0xA0, 0x50))
    icon("straw", img)
    steel = vanilla("block/iron_block.png")
    side = _tint(steel, (0xE8, 0xEC, 0xF0), 1.25)
    for x in range(16):
        for y in range(9, 16):
            put(side, x, y, (0xE8, 0xEC, 0xF0) if y > 9 else (0x9C, 0xA0, 0xA8))
    save(side, "block/hydroponic_tray.png")
    top = Image.new("RGBA", (16, 16), (0x3C, 0x78, 0x90, 255))
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                put(top, x, y, (0xE8, 0xEC, 0xF0))
            elif (x * 5 + y * 3) % 7 == 0:
                put(top, x, y, (0x58, 0x98, 0xB0))
            elif x % 4 == 2 and y % 4 == 2:
                put(top, x, y, (0x2A, 0x2A, 0x30))  # отверстия под рассаду
    save(top, "block/hydroponic_tray_top.png")
    for lit in (False, True):
        lamp = Image.new("RGBA", (16, 16), (0x40, 0x42, 0x48, 255))
        for x in range(1, 15):
            for y in range(1, 15):
                if (x + y) % 2 == 0:
                    put(lamp, x, y, (0xE0, 0x40, 0xA0) if lit and x % 3 else (0x50, 0x70, 0xF0) if lit else (0x80, 0x84, 0x90))
        save(lamp, f"block/grow_lamp{'_on' if lit else ''}.png")
    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    for lit in (False, True):
        f = casing.copy()
        disc(f, 7.5, 8, 5, lambda x, y, d: (0x2A, 0x2A, 0x30) if d > 4 else ((0xFF, 0x90, 0x30) if lit else (0x60, 0x50, 0x48)))
        save(f, f"block/biomass_oxidizer_front{'_on' if lit else ''}.png")
    save(casing, "block/biomass_oxidizer_side.png")
    save(casing, "block/biomass_oxidizer_top.png")


def ring():
    steel = vanilla("block/iron_block.png")
    side = steel.copy()
    for x in range(16):
        for y in (4, 11):
            put(side, x, y, (0x50, 0x54, 0x5C))
    for x in range(16):
        for y in range(5, 11):
            put(side, x, y, (0xE0, 0x88, 0x30) if (x // 2) % 2 else (0x2A, 0x2A, 0x30))  # полосы вращающейся части
    save(side, "block/spin_hub_side.png")
    end = steel.copy()
    disc(end, 7.5, 7.5, 7, lambda x, y, d: (0x3A, 0x3C, 0x44) if 6 < d else (0xB8, 0xBC, 0xC4) if d > 3 else (0x70, 0x72, 0x78))
    for i in range(8):
        import math
        a = i * math.pi / 4
        for r in (4, 5):
            put(end, int(7.5 + r * math.cos(a)), int(7.5 + r * math.sin(a)), (0x30, 0x30, 0x34))
    save(end, "block/spin_hub_end.png")
    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    f = casing.copy()
    disc(f, 7.5, 7.5, 5, lambda x, y, d: (0x2A, 0x2A, 0x30) if d > 3.5 else (0x60, 0x40, 0x30) if d > 1.5 else (0x10, 0x10, 0x12))
    save(f, "block/rim_thruster_front.png")
    s2 = casing.copy()
    for y in range(4, 12):
        put(s2, 7, y, (0xD8, 0x84, 0x50))
        put(s2, 8, y, (0xD8, 0x84, 0x50))
    save(s2, "block/rim_thruster_side.png")
    m = mod("block/motor_side.png") if True else casing
    save(m, "block/despin_motor_side.png")
    e = casing.copy()
    disc(e, 7.5, 7.5, 5, lambda x, y, d: (0xE0, 0x88, 0x30) if int(d) % 2 else (0x40, 0x42, 0x48))
    save(e, "block/despin_motor_end.png")


def guis():
    base = mod("gui/machine_single.png")
    arrow = base.crop((72, 35, 72 + 22, 35 + 16))
    img = base.copy()
    for x in range(7, 152):
        for y in range(14, 79):
            img.putpixel((x, y), BG)
    img.paste(arrow, (76, 35))
    slot(img, 50, 35)
    slot(img, 116, 35)
    save(img, "gui/co2_scrubber.png")
    img2 = base.copy()
    for x in range(7, 152):
        for y in range(14, 79):
            img2.putpixel((x, y), BG)
    img2.paste(arrow, (76, 35))
    slot(img2, 56, 35)
    save(img2, "gui/biomass_oxidizer.png")


def main():
    items()
    blocks()
    greenhouse()
    ring()
    guis()


if __name__ == "__main__":
    main()
