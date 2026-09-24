#!/usr/bin/env python3
"""Колесо ровера и предметы деталей ровера как цельные 3D-тела (007). Вызывается из gen_007.py.

Колесо LRV (Ø 81 см, ширина 23 см): плетёная сетчатая шина из стальной проволоки, титановые шевроны
протектора, утопленный обод, мотор-редуктор в ступице. Профиль в плоскости колеса (Z–Y) растеризуется
на сетку 0.5 px и сливается в непересекающиеся объёмы; толщина вдоль оси X своя у каждой части.
Грани — только на границе тела (tools/check_zfight.py не находит совпадающих плоскостей)."""
import json
import math
import os

MODELS = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                      "spacereloaded", "models")
NS = "spacereloaded"
EMPTY = 0
# часть: (x1, x2, текстура боков, текстура торца)
HUB, RIM, TIRE, CHEVRON = 1, 2, 3, 4
PARTS = {
    HUB: (5.5, 10.5, "hub", "hub"),          # мотор-редуктор выступает за обод
    RIM: (6.5, 9.5, "rim", "rim"),           # облегчённый обод утоплен
    TIRE: (6.0, 10.0, "tire", "tire"),       # сетчатая шина на всю ширину
    CHEVRON: (6.4, 9.6, "chevron", "chevron"),  # шевроны протектора — поверх сетки, чуть уже шины
}
R_HUB, R_RIM, R_TIRE, R_TREAD = 2.2, 4.4, 7.3, 7.9
CHEVRONS = 24


def part_at(z, y):
    dz, dy = z - 8, y - 8
    r = math.hypot(dz, dy)
    if r < R_HUB:
        return HUB
    if r < R_RIM:
        return RIM
    if r < R_TIRE:
        return TIRE
    if r < R_TREAD:
        phase = (math.atan2(dy, dz) / (2 * math.pi) * CHEVRONS) % 1.0
        return CHEVRON if abs(phase - 0.5) < 0.2 else EMPTY
    return EMPTY


def rectangles(grid, n):
    used = [[False] * n for _ in range(n)]
    out = []
    for j in range(n):
        i = 0
        while i < n:
            c = grid[j][i]
            if c == EMPTY or used[j][i]:
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
            out.append((c, i, j, i2, j2))
            i = i2 + 1
    return out


def exposed(grid, n, c, cells):
    x1, x2 = PARTS[c][:2]
    for (i, j) in cells:
        if not (0 <= i < n and 0 <= j < n) or grid[j][i] == EMPTY:
            return True
        o1, o2 = PARTS[grid[j][i]][:2]
        if o1 > x1 or o2 < x2:
            return True
    return False


def wheel_model(cell=0.5):
    n = round(16 / cell)
    # grid[j][i]: i — ось Z, j — ось Y (сверху вниз по сетке = вниз по Y)
    grid = [[part_at((i + 0.5) * cell, 16 - (j + 0.5) * cell) for i in range(n)] for j in range(n)]
    elements = []
    for c, i1, j1, i2, j2 in rectangles(grid, n):
        z1, z2 = i1 * cell, (i2 + 1) * cell
        y2, y1 = 16 - j1 * cell, 16 - (j2 + 1) * cell
        x1, x2, side, edge = PARTS[c]
        # UV боков: колесо целиком в текстуре 16×16 (круглая текстура обода/ступицы ложится по месту)
        faces = {"east": {"uv": [16 - z2, 16 - y2, 16 - z1, 16 - y1], "texture": f"#{side}"},
                 "west": {"uv": [z1, 16 - y2, z2, 16 - y1], "texture": f"#{side}"}}
        sides = {"up": [(i, j1 - 1) for i in range(i1, i2 + 1)],
                 "down": [(i, j2 + 1) for i in range(i1, i2 + 1)],
                 "north": [(i1 - 1, j) for j in range(j1, j2 + 1)],
                 "south": [(i2 + 1, j) for j in range(j1, j2 + 1)]}
        for face, cells in sides.items():
            if not exposed(grid, n, c, cells):
                continue
            if face in ("up", "down"):
                uv = [x1, z1, x2, z2]
            else:
                uv = [x1, 16 - y2, x2, 16 - y1]
            faces[face] = {"uv": uv, "texture": f"#{edge}"}
        elements.append({"from": [x1, y1, z1], "to": [x2, y2, z2], "faces": faces})
    return {"parent": "minecraft:block/block",
            "textures": {"particle": f"{NS}:block/rover_wheel", "tire": f"{NS}:block/rover_wheel",
                         "hub": f"{NS}:block/rover_wheel_hub", "rim": f"{NS}:block/rover_wheel_rim",
                         "chevron": f"{NS}:block/rover_wheel_chevron"},
            "display": {"gui": {"rotation": [30, 135, 0], "scale": [0.8, 0.8, 0.8]},
                        "fixed": {"rotation": [0, 90, 0], "scale": [0.9, 0.9, 0.9]},
                        "ground": {"translation": [0, 2, 0], "scale": [0.4, 0.4, 0.4]},
                        "thirdperson_righthand": {"rotation": [75, 135, 0], "translation": [0, 2.5, 0],
                                                  "scale": [0.45, 0.45, 0.45]},
                        "firstperson_righthand": {"rotation": [0, 135, 0], "scale": [0.5, 0.5, 0.5]}},
            "elements": elements}


def box(f, t, tex, skip=()):
    return {"from": f, "to": t, "faces": {x: {"texture": tex} for x in ("up", "down", "north", "south", "east", "west")
                                          if x not in skip}}


def battery_model():
    """Ni–Fe батарея: корпус из банок (рёбра между банками — отдельные объёмы поверх крышки),
    клеммы «+» и «−», ручка для переноски."""
    els = [box([2, 0, 3], [14, 10, 13], "#case")]
    for x in (4.5, 7.5, 10.5):
        els.append(box([x - 0.5, 10, 3.5], [x + 0.5, 10.5, 12.5], "#rib", skip=("down",)))
    els.append(box([3.5, 10, 5], [5.5, 12, 7], "#plus", skip=("down",)))
    els.append(box([10.5, 10, 5], [12.5, 12, 7], "#minus", skip=("down",)))
    els.append(box([6.5, 12.5, 9.25], [9.5, 13.5, 10.75], "#rib", skip=("east", "west")))
    els.append(box([5.5, 10, 9.25], [6.5, 13.5, 10.75], "#rib", skip=("down",)))
    els.append(box([9.5, 10, 9.25], [10.5, 13.5, 10.75], "#rib", skip=("down",)))
    return {"parent": "minecraft:block/block",
            "textures": {"particle": f"{NS}:block/rover_battery", "case": f"{NS}:block/rover_battery",
                         "rib": f"{NS}:block/rover_wheel_rim", "plus": f"{NS}:block/battery_terminal_plus",
                         "minus": f"{NS}:block/battery_terminal_minus"},
            "elements": els}


def chassis_item():
    """Предмет шасси — модель корпуса ровера, уменьшенная под слот (корпус шире блока в 2 раза)."""
    return {"parent": f"{NS}:block/rover_body_model",
            "display": {"gui": {"rotation": [30, 225, 0], "translation": [0, -1, 0], "scale": [0.3, 0.3, 0.3]},
                        "fixed": {"rotation": [0, 180, 0], "scale": [0.35, 0.35, 0.35]},
                        "ground": {"translation": [0, 2, 0], "scale": [0.15, 0.15, 0.15]},
                        "thirdperson_righthand": {"rotation": [75, 45, 0], "translation": [0, 2.5, 0],
                                                  "scale": [0.2, 0.2, 0.2]},
                        "firstperson_righthand": {"rotation": [0, 45, 0], "scale": [0.2, 0.2, 0.2]}}}


def write(rel, obj):
    path = os.path.join(MODELS, rel)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent="\t")
        f.write("\n")


def main():
    wheel = wheel_model()
    write("block/rover_wheel_model.json", wheel)
    write("item/rover_wheel.json", {"parent": f"{NS}:block/rover_wheel_model"})
    write("block/nife_battery_item.json", battery_model())
    write("item/nife_battery.json", {"parent": f"{NS}:block/nife_battery_item"})
    write("item/rover_chassis.json", chassis_item())
    print(f"   rover_wheel_model: {len(wheel['elements'])} объёмов")


if __name__ == "__main__":
    main()
