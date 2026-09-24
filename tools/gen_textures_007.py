#!/usr/bin/env python3
"""Текстуры 007 «Жизнь на станции». Запуск: python3 tools/gen_textures_007.py"""
import math

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


def rover():
    alu = _tint(vanilla("block/iron_block.png"), (0xE0, 0xE4, 0xEA), 1.3)
    frame = alu.copy()
    for i in range(0, 16, 4):
        for j in range(16):
            put(frame, i, j, (0x9C, 0xA0, 0xA8))
    save(frame, "block/rover_body.png")
    seat = Image.new("RGBA", (16, 16), (0x50, 0x68, 0x90, 255))
    for x in range(16):
        for y in range(0, 16, 3):
            put(seat, x, y, (0x40, 0x54, 0x74))
    save(seat, "block/rover_seat.png")
    panel = Image.new("RGBA", (16, 16), (0x2A, 0x2C, 0x30, 255))
    for x, y, c in ((3, 4, (0xE0, 0x88, 0x30)), (7, 4, (0x60, 0xE0, 0x70)), (11, 4, (0xE0, 0x40, 0x40))):
        for dx in range(2):
            for dy in range(2):
                put(panel, x + dx, y + dy, c)
    for x in range(2, 14):
        put(panel, x, 10, (0x90, 0x94, 0x9C))
    save(panel, "block/rover_panel.png")
    tire = Image.new("RGBA", (16, 16), (0x70, 0x74, 0x7C, 255))
    for x in range(16):
        for y in range(16):
            if (x + y) % 4 == 0 or (x - y) % 4 == 0:
                put(tire, x, y, (0xB8, 0xBC, 0xC4))  # сетка из стальной проволоки
    save(tire, "block/rover_wheel.png")
    # крышка мотор-редуктора: анодированный корпус, шесть болтов по окружности, вал в центре
    hub = Image.new("RGBA", (16, 16), (0x3A, 0x3E, 0x46, 255))
    disc(hub, 7.5, 7.5, 4.2, lambda x, y, d: (0x5A, 0x60, 0x6A) if d > 3.4 else (0x48, 0x4E, 0x58))
    for k in range(6):
        a = k * math.pi / 3
        put(hub, round(7.5 + 2.6 * math.cos(a)), round(7.5 + 2.6 * math.sin(a)), (0xC8, 0xCC, 0xD2))
    disc(hub, 7.5, 7.5, 1.2, lambda x, y, d: (0xD8, 0x84, 0x50))
    save(hub, "block/rover_wheel_hub.png")
    # обод: шлифованный алюминий с концентрическими рисками
    rim = Image.new("RGBA", (16, 16), (0xB8, 0xBC, 0xC4, 255))
    for x in range(16):
        for y in range(16):
            d = math.hypot(x - 7.5, y - 7.5)
            v = 0xB8 + int(10 * math.sin(d * 2.4))
            put(rim, x, y, (v, v + 4, v + 12))
    save(rim, "block/rover_wheel_rim.png")
    # шевроны протектора: титан с тёмной кромкой
    chev = Image.new("RGBA", (16, 16), (0x9A, 0x94, 0x8C, 255))
    for x in range(16):
        for y in range(16):
            if (x + abs(y - 8)) % 6 < 2:
                put(chev, x, y, (0xC4, 0xBE, 0xB4))
            elif (x + abs(y - 8)) % 6 == 5:
                put(chev, x, y, (0x6A, 0x66, 0x60))
    save(chev, "block/rover_wheel_chevron.png")
    for name, color in (("plus", (0xC8, 0x3A, 0x32)), ("minus", (0x2A, 0x2C, 0x30))):
        term = Image.new("RGBA", (16, 16), color + (255,))
        for x in range(16):
            put(term, x, 0, tuple(min(255, c + 40) for c in color))
        save(term, f"block/battery_terminal_{name}.png")
    bat = Image.new("RGBA", (16, 16), (0x3C, 0x40, 0x3C, 255))
    for x in range(1, 15, 3):
        for y in range(2, 14):
            put(bat, x, y, (0x70, 0x78, 0x70))
    save(bat, "block/rover_battery.png")
    casing = vanilla("block/smooth_stone.png").copy()
    _frame(casing, (0x55, 0x58, 0x60, 255), 1)
    side = casing.copy()
    for y in range(3, 13):
        put(side, 7, y, (0xE0, 0x88, 0x30))
        put(side, 8, y, (0xE0, 0x88, 0x30))
    save(side, "block/rover_charger_side.png")
    top = casing.copy()
    disc(top, 7.5, 7.5, 3, lambda x, y, d: (0x60, 0xE0, 0x70) if d < 1.5 else (0x30, 0x30, 0x34))
    save(top, "block/rover_charger_top.png")
    # предметы
    img = blank()
    for x in range(1, 15):
        for y in range(6, 10):
            put(img, x, y, (0xE0, 0xE4, 0xEA) if y in (6, 9) else (0x9C, 0xA0, 0xA8))
    for x in (3, 11):
        for y in range(3, 6):
            put(img, x, y, (0x50, 0x68, 0x90))
            put(img, x + 1, y, (0x50, 0x68, 0x90))
    icon("rover_chassis", img)
    img = blank()
    disc(img, 7.5, 7.5, 6.5, lambda x, y, d: (0xB8, 0xBC, 0xC4) if (x + y) % 3 == 0 else (0x70, 0x74, 0x7C) if d > 2 else (0xD8, 0x84, 0x50))
    icon("rover_wheel", img)
    img = blank()
    for x in range(2, 14):
        for y in range(4, 14):
            put(img, x, y, (0x3C, 0x40, 0x3C) if (x - 2) % 3 else (0x70, 0x78, 0x70))
    for x in (4, 11):
        put(img, x, 3, (0xE0, 0x40, 0x40) if x == 4 else (0x30, 0x30, 0x34))
    icon("nife_battery", img)


def imaging():
    # ЭВТИ: золотистая каптоновая плёнка с морщинами
    foil = Image.new("RGBA", (16, 16), (0xD8, 0xA8, 0x40, 255))
    for x in range(16):
        for y in range(16):
            w = (x * 7 + y * 3) % 11
            if w < 2:
                put(foil, x, y, (0xF0, 0xCC, 0x70))
            elif w > 8:
                put(foil, x, y, (0xA8, 0x7C, 0x28))
    save(foil, "block/imaging_satellite.png")
    tube = Image.new("RGBA", (16, 16), (0x30, 0x32, 0x38, 255))
    for y in range(0, 16, 4):
        for x in range(16):
            put(tube, x, y, (0x44, 0x46, 0x4E))
    save(tube, "block/imaging_satellite_tube.png")
    lens = Image.new("RGBA", (16, 16), (0x30, 0x32, 0x38, 255))
    disc(lens, 7.5, 7.5, 6.5, lambda x, y, d: (0x10, 0x14, 0x28) if d > 2.2 else (0x60, 0x64, 0x70))  # вторичное зеркало
    put(lens, 5, 5, (0x80, 0x90, 0xC0))
    save(lens, "block/imaging_satellite_lens.png")
    img = blank()
    disc(img, 7.5, 7.5, 6.5, lambda x, y, d: (0xC8, 0xD0, 0xDC) if d < 5.5 else (0x70, 0x74, 0x7C))
    for x, y in ((5, 5), (6, 5), (5, 6)):
        put(img, x, y, (0xF4, 0xF8, 0xFF))
    icon("telescope_mirror", img)
    img = blank()
    for x in range(2, 14):
        for y in range(5, 11):
            put(img, x, y, (0xE8, 0xE0, 0xD0) if y in (5, 10) else (0x3A, 0x30, 0x60))
    for x in range(3, 13):
        put(img, x, 7, (0x70, 0x60, 0xC0))
        put(img, x, 8, (0x70, 0x60, 0xC0))
    for x in range(3, 13, 2):
        put(img, x, 4, (0xD0, 0xA0, 0x40))
        put(img, x, 11, (0xD0, 0xA0, 0x40))
    icon("image_sensor", img)
    img = blank()
    for x in range(1, 15):
        for y in range(1, 15):
            edge = x in (1, 14) or y in (1, 14)
            relief = (x * 5 + y * 3 + (x * y) % 7) % 9
            put(img, x, y, (0xE8, 0xE0, 0xC8) if edge else (0x90, 0x8C, 0x84) if relief < 3 else (0xB4, 0xB0, 0xA8))
    disc(img, 9.5, 6.5, 2.2, lambda x, y, d: (0x60, 0x5C, 0x58) if d < 1.4 else (0xD0, 0xCC, 0xC4))  # кратер
    icon("orbital_image", img)


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
    imaging()
    items()
    blocks()
    greenhouse()
    ring()
    rover()
    guis()


if __name__ == "__main__":
    main()
