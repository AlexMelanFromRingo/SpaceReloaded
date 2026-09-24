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


def main():
    eclss()


if __name__ == "__main__":
    main()
