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
}


def main():
    for name, box in SHOTS.items():
        im = Image.open(SRC / f"readme_{name}.png").convert("RGB")
        if box:
            im = im.crop(box)
        out = DST / f"{name}.png"
        im.save(out, optimize=True)
        print(f"   {out.relative_to(REPO)}: {im.size[0]}×{im.size[1]}, {out.stat().st_size // 1024} КБ")


if __name__ == "__main__":
    main()
