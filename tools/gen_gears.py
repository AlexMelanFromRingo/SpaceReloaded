#!/usr/bin/env python3
"""Шестерни как цельные 3D-тела (005, модели ротора). Запуск: python3 tools/gen_gears.py

Профиль шестерни задаётся в полярных координатах (окружность впадин, трапеция зуба: вершина 36 %
шага, боковины по 14 %) и растеризуется на сетку; клетки жадно сливаются в прямоугольные объёмы,
которые НЕ пересекаются. Как у настоящей литой шестерни — три толщины: обод с зубьями на полную
ширину венца, облегчённый диск тоньше, ступица толще; в ступице сквозное отверстие под вал
(вал рисуется отдельной моделью — ни одна грань не совпадает с его гранями). Грани рисуются только
там, где тело граничит с пустотой или с более тонкой частью: внутренних граней нет, совпадающих
плоскостей тоже (проверка — tools/check_zfight.py).
"""
import json
import math
import os

MODELS = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                      "spacereloaded", "models", "block")
NS = "spacereloaded"
CENTER = 8.0
BORE = 2.0      # полуширина отверстия под вал (вал 6..10)
HUB = 3.6       # радиус ступицы (стенка вокруг вала ≥ 0.8 px у углов)
# толщина по оси Y: (низ, верх)
RIM_Y = (6.5, 9.5)
WEB_Y = (7.0, 9.0)
HUB_Y = (5.0, 11.0)
EMPTY, BORE_C, WEB, RIM, HUBC = 0, 1, 2, 3, 4
THICK = {WEB: WEB_Y, RIM: RIM_Y, HUBC: HUB_Y}


def tooth(phase):
    """Доля высоты зуба при фазе шага 0..1: трапеция (вершина 36 %, боковины по 14 %)."""
    top, flank = 0.36, 0.14
    d = abs(phase - 0.5)
    if d <= top / 2:
        return 1.0
    if d <= top / 2 + flank:
        return 1.0 - (d - top / 2) / flank
    return 0.0


def classify(x, z, teeth, root, tip, rim_w):
    dx, dz = x - CENTER, z - CENTER
    if max(abs(dx), abs(dz)) < BORE:
        return BORE_C  # квадратное отверстие по сечению вала
    r = math.hypot(dx, dz)
    if r < HUB:
        return HUBC
    phase = (math.atan2(dz, dx) / (2 * math.pi) * teeth) % 1.0
    if r < root + (tip - root) * tooth(phase):
        return RIM if r >= root - rim_w else WEB
    return EMPTY


def rasterize(teeth, root, tip, rim_w, cell, lo, hi):
    n = round((hi - lo) / cell)
    grid = [[classify(lo + (i + 0.5) * cell, lo + (j + 0.5) * cell, teeth, root, tip, rim_w)
             for i in range(n)] for j in range(n)]
    return grid, n


def rectangles(grid, n):
    """Жадное слияние клеток одного класса в прямоугольники (не пересекаются, покрывают всё)."""
    used = [[False] * n for _ in range(n)]
    rects = []
    for j in range(n):
        i = 0
        while i < n:
            c = grid[j][i]
            if c in (EMPTY, BORE_C) or used[j][i]:
                i += 1
                continue
            i2 = i
            while i2 + 1 < n and grid[j][i2 + 1] == c and not used[j][i2 + 1]:
                i2 += 1
            j2 = j
            while j2 + 1 < n and all(grid[j2 + 1][k] == c and not used[j2 + 1][k] for k in range(i, i2 + 1)):
                j2 += 1
            for jj in range(j, j2 + 1):
                for k in range(i, i2 + 1):
                    used[jj][k] = True
            rects.append((c, i, j, i2, j2))
            i = i2 + 1
    return rects


def exposed(grid, n, c, cells):
    """Есть ли на стороне клетки, граничащие с пустотой или с более тонкой частью (отверстие — нет:
    его занимает вал)."""
    lo, hi = THICK[c]
    for (i, j) in cells:
        if not (0 <= i < n and 0 <= j < n):
            return True
        other = grid[j][i]
        if other == BORE_C:
            continue
        if other == EMPTY:
            return True
        olo, ohi = THICK[other]
        if olo > lo or ohi < hi:
            return True
    return False


def model(name, teeth, root, tip, rim_w, cell, lo, hi):
    grid, n = rasterize(teeth, root, tip, rim_w, cell, lo, hi)
    span = hi - lo
    s = 16.0 / span  # развёртка текстуры 16×16 на весь диаметр

    def u(v):
        return round((v - lo) * s, 4)

    elements = []
    for c, i1, j1, i2, j2 in rectangles(grid, n):
        x1, x2 = lo + i1 * cell, lo + (i2 + 1) * cell
        z1, z2 = lo + j1 * cell, lo + (j2 + 1) * cell
        y1, y2 = THICK[c]
        vy1, vy2 = 16 - y2, 16 - y1
        faces = {"up": {"uv": [u(x1), u(z1), u(x2), u(z2)], "texture": "#all"},
                 "down": {"uv": [u(x1), u(z2), u(x2), u(z1)], "texture": "#all"}}
        sides = {"north": [(i, j1 - 1) for i in range(i1, i2 + 1)],
                 "south": [(i, j2 + 1) for i in range(i1, i2 + 1)],
                 "west": [(i1 - 1, j) for j in range(j1, j2 + 1)],
                 "east": [(i2 + 1, j) for j in range(j1, j2 + 1)]}
        for face, cells in sides.items():
            if not exposed(grid, n, c, cells):
                continue
            if face in ("north", "south"):
                uv = [u(x1), vy1, u(x2), vy2]
            else:
                uv = [u(z1), vy1, u(z2), vy2]
            faces[face] = {"uv": uv, "texture": "#all"}
        elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
    obj = {"parent": "minecraft:block/block",
           "textures": {"particle": f"{NS}:block/gear", "all": f"{NS}:block/gear"},
           "elements": elements}
    with open(os.path.join(MODELS, name + ".json"), "w", encoding="utf-8") as f:
        json.dump(obj, f, indent="\t")
        f.write("\n")
    print(f"   {name}: {teeth} зубьев, {len(elements)} объёмов")


def main():
    # Межосевые на сетке: малая–малая по грани 16 px, малая–большая по диагонали 16√2 = 22.6 px.
    # Размеры подобраны так, чтобы зубья заходили во впадины соседа (1.8 и 2.3 px) и не упирались
    # вершиной в дно впадины; с фазовым сдвигом рендера (π/16, π/32) перекрытия нет на всём обороте.
    model("rotor_small_gear", 8, 6.6, 8.9, 1.2, 0.5, -1.0, 17.0)
    model("rotor_large_gear", 16, 13.6, 16.0, 1.5, 0.5, -8.0, 24.0)


if __name__ == "__main__":
    main()
