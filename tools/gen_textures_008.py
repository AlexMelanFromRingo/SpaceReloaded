#!/usr/bin/env python3
"""Текстуры 008 «Тяжёлая индустрия». Запуск: python3 tools/gen_textures_008.py"""
import math

from PIL import Image

from gen_textures import save
from gen_textures_006 import disc, put

# палитра стоек МКС: светлые панели, болты, цветная маркировка модулей
PANEL = (0xD8, 0xD6, 0xCC)
PANEL_DARK = (0xB4, 0xB2, 0xA8)
BOLT = (0x7A, 0x7C, 0x80)
STRIPE = {"ogs_module": (0x3C, 0x8C, 0xD8), "sabatier_module": (0xE0, 0x88, 0x30),
          "cdra_module": (0x6C, 0x6C, 0x74), "wrs_module": (0x3C, 0xA8, 0x6C), "eclss_blank_panel": PANEL_DARK}


def panel(color=PANEL, bolts=True):
    img = Image.new("RGBA", (16, 16), color + (255,))
    for i in range(16):
        put(img, i, 0, tuple(max(0, c - 30) for c in color))
        put(img, i, 15, tuple(max(0, c - 40) for c in color))
        put(img, 0, i, tuple(max(0, c - 30) for c in color))
        put(img, 15, i, tuple(max(0, c - 40) for c in color))
    if bolts:
        for x, y in ((2, 2), (13, 2), (2, 13), (13, 13)):
            put(img, x, y, BOLT)
    return img


def eclss():
    save(panel(), "block/eclss_panel_side.png")
    save(panel(PANEL_DARK), "block/eclss_module_side.png")
    rim = panel((0xC4, 0xC2, 0xB8), bolts=False)
    save(rim, "block/eclss_module_rim.png")
    for on in (False, True):
        img = panel()
        for x in range(3, 13):
            for y in range(3, 9):
                put(img, x, y, (0x14, 0x20, 0x26) if not on else (0x10, 0x30, 0x38))
        if on:
            for x, y, c in ((4, 4, (0x60, 0xE0, 0x80)), (5, 4, (0x60, 0xE0, 0x80)), (4, 6, (0x6F, 0xD5, 0xE8)),
                            (5, 6, (0x6F, 0xD5, 0xE8)), (6, 6, (0x6F, 0xD5, 0xE8)), (8, 5, (0xE0, 0xB2, 0x3C))):
                put(img, x, y, c)
        for i, c in enumerate(((0x60, 0xE0, 0x80), (0xE0, 0xB2, 0x3C), (0xDD, 0x4B, 0x4B))):
            put(img, 4 + i * 2, 11, c if on else (0x50, 0x50, 0x50))
        save(img, "block/eclss_controller_front" + ("_on" if on else "") + ".png")
    frame = Image.new("RGBA", (16, 16), (0x9C, 0x9E, 0xA2, 255))
    for i in range(16):
        for j in range(16):
            if (i + j) % 5 == 0 or (i - j) % 5 == 0:
                put(frame, i, j, (0x70, 0x72, 0x76))
    save(frame, "block/eclss_rack_frame.png")
    formed = panel((0xA8, 0xAA, 0xAE), bolts=False)
    for y in range(16):
        for x in (1, 14):
            put(formed, x, y, (0x78, 0x7A, 0x80))
        if y % 4 == 0:
            put(formed, 2, y, BOLT)
            put(formed, 13, y, BOLT)
    save(formed, "block/eclss_rack_frame_formed.png")
    for m, c in STRIPE.items():
        img = panel()
        for x in range(1, 15):
            for y in range(1, 4):
                put(img, x, y, c)
        save(img, f"block/{m}_front.png")
    # ниши модулей
    ogs = Image.new("RGBA", (16, 16), (0x24, 0x40, 0x5C, 255))  # вода за окном
    for x in (5, 10):
        for y in range(4, 13):
            put(ogs, x, y, (0xB0, 0xB8, 0xC0))  # электроды
    for y in range(2, 5):
        for x in range(2, 14):
            put(ogs, x, y, (0x3A, 0x60, 0x80))
    save(ogs, "block/ogs_module_inner.png")
    sab = Image.new("RGBA", (16, 16), (0x30, 0x26, 0x20, 255))
    for y in range(2, 14, 3):
        for x in range(2, 14):
            put(sab, x, y, (0xC8, 0x78, 0x40))  # медный змеевик
    save(sab, "block/sabatier_module_inner.png")
    cdra = Image.new("RGBA", (16, 16), (0x1C, 0x1E, 0x22, 255))
    disc(cdra, 7.5, 7.5, 7.2, lambda x, y, d: (0x38, 0x3A, 0x40) if int(d) % 2 else (0x26, 0x28, 0x2C))
    save(cdra, "block/cdra_module_inner.png")
    wrs = Image.new("RGBA", (16, 16), (0x26, 0x2C, 0x2A, 255))
    for x in (3, 12):
        for y in range(1, 15):
            put(wrs, x, y, (0x5C, 0xA0, 0x7C))
    for x in range(3, 13):
        put(wrs, x, 13, (0x5C, 0xA0, 0x7C))
    save(wrs, "block/wrs_module_inner.png")
    blade = Image.new("RGBA", (16, 16), (0xC4, 0xC8, 0xCC, 255))
    for x in range(16):
        put(blade, x, 8, (0x9C, 0xA0, 0xA4))
    save(blade, "block/eclss_fan.png")
    steel = Image.new("RGBA", (16, 16), (0x8C, 0x90, 0x96, 255))
    for y in range(0, 16, 3):
        for x in range(16):
            put(steel, x, y, (0xA4, 0xA8, 0xAE))
    save(steel, "block/eclss_piston.png")


def noise_fill(base, var, seed):
    img = Image.new("RGBA", (16, 16), base + (255,))
    for x in range(16):
        for y in range(16):
            h = (x * 73 + y * 151 + seed * 37) % 97 / 97.0
            k = int((h - 0.5) * var)
            put(img, x, y, tuple(max(0, min(255, c + k)) for c in base))
    return img


def ore(name, host, spots, seed):
    img = noise_fill(host, 24, seed)
    for i in range(7):
        x, y = (seed * 5 + i * 7) % 14 + 1, (seed * 3 + i * 5) % 14 + 1
        for dx, dy in ((0, 0), (1, 0), (0, 1)):
            put(img, x + dx, y + dy, spots[(i + dx) % len(spots)])
    save(img, f"block/{name}.png")


def item_icon(name, draw):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw(img)
    save(img, f"item/{name}.png")


def blob(img, color, cx=7.5, cy=8.5, r=5.2, edge=None):
    disc(img, cx, cy, r, lambda x, y, d: tuple(max(0, c - int(d * 9)) for c in color) if d < r - 1 else (edge or tuple(max(0, c - 50) for c in color)))


def reactor():
    save(noise_fill((0x6A, 0x70, 0x78), 14, 3), "block/reactor_steel.png")
    save(noise_fill((0x30, 0x32, 0x36), 10, 5), "block/reactor_dark.png")
    for on in (False, True):
        img = noise_fill((0x6A, 0x70, 0x78), 10, 3)
        for x in range(2, 14):
            for y in range(3, 10):
                put(img, x, y, (0x10, 0x16, 0x1A))
        # шкала хода стержня и лампы режима
        for y in range(4, 9):
            put(img, 4, y, (0xE0, 0xB2, 0x3C) if on else (0x50, 0x50, 0x50))
        for i, c in enumerate(((0x60, 0xE0, 0x80), (0xE0, 0xB2, 0x3C), (0xDD, 0x4B, 0x4B))):
            put(img, 7 + i * 2, 12, c if on else (0x44, 0x44, 0x44))
        save(img, "block/control_rod_drive_front" + ("_on" if on else "") + ".png")
    core = noise_fill((0x4A, 0x4E, 0x54), 12, 7)
    for x in range(16):
        for y in range(16):
            if x in (0, 15) or y in (0, 15):
                put(core, x, y, (0x2E, 0x30, 0x34))
    save(core, "block/reactor_core.png")
    formed = core.copy()
    for cx, cy in ((4, 4), (11, 4), (4, 11), (11, 11), (7, 7)):
        disc(formed, cx + 0.5, cy + 0.5, 1.6, lambda x, y, d: (0x8C, 0x90, 0x96))
    save(formed, "block/reactor_core_formed.png")
    beo = noise_fill((0xE8, 0xE6, 0xDE), 10, 11)
    save(beo, "block/beo_reflector.png")
    beo_f = beo.copy()
    for i in range(16):
        put(beo_f, i, 0, (0xC0, 0xBE, 0xB6))
        put(beo_f, i, 15, (0xC0, 0xBE, 0xB6))
    save(beo_f, "block/beo_reflector_formed.png")
    pipe = noise_fill((0x9A, 0x8E, 0x80), 12, 13)
    for y in range(0, 16, 4):
        for x in range(16):
            put(pipe, x, y, (0x7A, 0x70, 0x66))
    save(pipe, "block/heat_pipe.png")
    save(pipe, "block/heat_pipe_side.png")
    end = noise_fill((0x6A, 0x70, 0x78), 10, 3)
    disc(end, 7.5, 7.5, 2.4, lambda x, y, d: (0x9A, 0x8E, 0x80))
    save(end, "block/heat_pipe_end.png")
    body = noise_fill((0xB8, 0x9C, 0x5C), 12, 17)  # позолоченный корпус вытеснителя
    save(body, "block/stirling_body.png")
    rad = noise_fill((0xC8, 0xCC, 0xD2), 8, 19)
    for x in range(0, 16, 5):
        for y in range(16):
            put(rad, x, y, (0x8A, 0x8E, 0x94))
    save(rad, "block/radiator_panel.png")
    rod = Image.new("RGBA", (16, 16), (0x26, 0x26, 0x2A, 255))
    for y in range(0, 16, 4):
        for x in range(16):
            put(rod, x, y, (0xE0, 0xB2, 0x3C))
    save(rod, "block/control_rod.png")
    # руды
    ore("beryl_ore", (0x80, 0x80, 0x80), [(0x6C, 0xC8, 0xA8), (0x58, 0xB0, 0x94)], 2)
    ore("borax_ore", (0x9C, 0x96, 0x8C), [(0xF4, 0xF2, 0xEC), (0xE0, 0xDE, 0xD6)], 4)
    ore("uraninite_ore", (0x4A, 0x4A, 0x50), [(0x26, 0x24, 0x22), (0x3C, 0x3A, 0x30), (0xC8, 0xC0, 0x40)], 6)
    # предметы цепочки
    items = {"beryl": (0x6C, 0xC8, 0xA8), "beryllium_hydroxide": (0xF0, 0xF0, 0xEA), "beryllium_oxide": (0xF6, 0xF6, 0xF2),
             "sodium": (0xC8, 0xCC, 0xD0), "borax": (0xF2, 0xF0, 0xEA), "boric_acid": (0xFA, 0xFA, 0xF6),
             "boron_carbide_blend": (0x6A, 0x6A, 0x6E), "boron_carbide": (0x30, 0x30, 0x34),
             "zircon": (0xB8, 0x80, 0x5C), "zirconium": (0xA8, 0xAC, 0xB2), "uraninite": (0x2C, 0x2A, 0x28),
             "yellowcake": (0xE8, 0xD0, 0x30), "uranium_dioxide": (0x3A, 0x3A, 0x32), "uranium_tetrafluoride": (0x58, 0xA0, 0x50),
             "fluorine": (0xE8, 0xE8, 0xA0), "uranium_hexafluoride": (0xF0, 0xF0, 0xF0),
             "depleted_uranium_hexafluoride": (0xA0, 0xA0, 0xA4)}
    for name, c in items.items():
        if name in ("fluorine", "uranium_hexafluoride", "depleted_uranium_hexafluoride"):
            def cyl(img, c=c, name=name):
                for x in range(4, 12):
                    for y in range(2, 15):
                        put(img, x, y, (0x88, 0x8C, 0x92) if x in (4, 11) else c)
                for x in range(6, 10):
                    put(img, x, 1, (0x60, 0x64, 0x6A))
                if name == "uranium_hexafluoride":
                    for x in range(5, 11):
                        put(img, x, 7, (0x3C, 0x8C, 0xD8))
            item_icon(name, cyl)
        else:
            item_icon(name, lambda img, c=c: blob(img, c))
    def basket(img):
        for x in range(3, 13):
            for y in range(2, 15):
                put(img, x, y, (0x8C, 0x90, 0x96) if x in (3, 12) or y in (2, 14) else (0x4A, 0x4E, 0x54))
        for x in (5, 7, 9):
            for y in range(4, 13):
                put(img, x + (1 if x == 9 else 0), y, (0x9A, 0xA0, 0x60))
    item_icon("fuel_basket", basket)


def cascade():
    for on in (False, True):
        img = noise_fill((0x6A, 0x70, 0x78), 10, 3)
        for x in range(2, 14):
            for y in range(3, 11):
                put(img, x, y, (0x10, 0x16, 0x1A))
        # мнемосхема каскада: ряд ступеней, горящих при работе
        for i in range(5):
            put(img, 3 + i * 2, 6, (0x60, 0xE0, 0x80) if on else (0x40, 0x40, 0x40))
            put(img, 3 + i * 2, 8, (0x6F, 0xD5, 0xE8) if on else (0x40, 0x40, 0x40))
        save(img, "block/cascade_controller_front" + ("_on" if on else "") + ".png")
    side = noise_fill((0xB8, 0xBC, 0xC4), 10, 23)
    for x in (0, 15):
        for y in range(16):
            put(side, x, y, (0x88, 0x8C, 0x94))
    save(side, "block/centrifuge_side.png")
    end = noise_fill((0x8C, 0x90, 0x96), 8, 29)
    disc(end, 7.5, 7.5, 5, lambda x, y, d: (0x5A, 0x5E, 0x66) if d > 4 else (0x9C, 0xA0, 0xA8))
    save(end, "block/centrifuge_end.png")
    rotor = Image.new("RGBA", (16, 16), (0x2C, 0x2E, 0x34, 255))  # углепластик
    for x in range(16):
        for y in range(16):
            if (x + y) % 4 < 2:
                put(rotor, x, y, (0x3C, 0x3E, 0x46))
    for x in range(0, 16, 8):
        for y in range(16):
            put(rotor, x, y, (0xC8, 0xCC, 0xD4))  # светлая метка — видно вращение
    save(rotor, "block/centrifuge_rotor.png")


def main():
    eclss()
    reactor()
    cascade()


if __name__ == "__main__":
    main()
