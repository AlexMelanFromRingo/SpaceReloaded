#!/usr/bin/env python3
"""Кадры README и сайта: из стенда (SR_ONLY=testReadmeShots) в docs/img, с обрезкой и сжатием.

Кадры сняты без HUD и чата (1600×900). Запуск после стенда: python3 tools/readme_shots.py"""
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[1]
SRC = REPO / "mod/build/run/clientGameTest/screenshots"
DST = REPO / "docs/img"
# имя: область обрезки (left, top, right, bottom) или None — кадр целиком
SHOTS = {
    "station": None,
    "rover": (160, 120, 1440, 840),
    "gears": (300, 100, 1300, 700),
    "orbital-image": (520, 0, 1080, 440),
    "creative-tab": (505, 160, 1095, 730),
    # 008: кадры снимают стенды машин (SR_ONLY=testEclssRack,…,testDsnAntenna)
    "eclss": None,
    "reactor": (100, 80, 1500, 868),
    "cascade": (200, 150, 1400, 825),
    "air-column": (200, 0, 1400, 900),
    "arc-furnace": (300, 100, 1500, 775),
    "arc-furnace-tap": (300, 100, 1500, 775),
    "dsn": (400, 150, 1600, 825),
}
# коллаж «Тяжёлая индустрия» для руководства: 3 × 2 кадра 533 × 300
COLLAGE = ("industry", ["eclss", "reactor", "cascade", "air-column", "arc-furnace-tap", "dsn"], (533, 300))


def main():
    for name, box in SHOTS.items():
        src = SRC / f"readme_{name}.png"
        if not src.exists():  # кадры снимают разные стенды — переносим то, что снято этим прогоном
            continue
        im = Image.open(src).convert("RGB")
        if box:
            im = im.crop(box)
        out = DST / f"{name}.png"
        im.save(out, optimize=True)
        print(f"   {out.relative_to(REPO)}: {im.size[0]}×{im.size[1]}, {out.stat().st_size // 1024} КБ")
    name, parts, (w, h) = COLLAGE
    sheet = Image.new("RGB", (3 * w, 2 * h))
    for i, part in enumerate(parts):
        im = Image.open(DST / f"{part}.png").convert("RGB")
        scale = max(w / im.width, h / im.height)  # заполнение ячейки с обрезкой по центру
        im = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)
        left, top = (im.width - w) // 2, (im.height - h) // 2
        sheet.paste(im.crop((left, top, left + w, top + h)), ((i % 3) * w, (i // 3) * h))
    out = DST / f"{name}.png"
    sheet.save(out, optimize=True)
    print(f"   {out.relative_to(REPO)}: {sheet.size[0]}×{sheet.size[1]}, {out.stat().st_size // 1024} КБ")


if __name__ == "__main__":
    main()
