#!/usr/bin/env python3
"""Поиск z-fighting в блочных моделях: пары граней с одной нормалью в одной плоскости и общей площадью.

Проверяет каждую модель отдельно и связки моделей, которые рендер рисует вместе (вал + деталь на валу).
Учитывает поворот элементов (любой оси и угла). Запуск: python3 tools/check_zfight.py [модель...]
"""
import glob
import json
import math
import os
import sys

MODELS = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                      "spacereloaded", "models", "block")
# что рендер рисует одновременно (KineticRenderer: вал + деталь)
COMBOS = [("rotor_steel_shaft", "rotor_small_gear"), ("rotor_steel_shaft", "rotor_large_gear"),
          ("rotor_steel_shaft", "rotor_flywheel")]
NORMALS = {"up": (0, 1, 0), "down": (0, -1, 0), "north": (0, 0, -1), "south": (0, 0, 1),
           "east": (1, 0, 0), "west": (-1, 0, 0)}
EPS = 1e-4


def rotate(p, rot):
    if not rot:
        return p
    a = math.radians(rot["angle"])
    o = rot["origin"]
    x, y, z = p[0] - o[0], p[1] - o[1], p[2] - o[2]
    c, s = math.cos(a), math.sin(a)
    if rot["axis"] == "x":
        y, z = y * c - z * s, y * s + z * c
    elif rot["axis"] == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + o[0], y + o[1], z + o[2])


def faces(element):
    (x1, y1, z1), (x2, y2, z2) = element["from"], element["to"]
    quads = {"up": [(x1, y2, z1), (x2, y2, z1), (x2, y2, z2), (x1, y2, z2)],
             "down": [(x1, y1, z1), (x2, y1, z1), (x2, y1, z2), (x1, y1, z2)],
             "north": [(x1, y1, z1), (x2, y1, z1), (x2, y2, z1), (x1, y2, z1)],
             "south": [(x1, y1, z2), (x2, y1, z2), (x2, y2, z2), (x1, y2, z2)],
             "east": [(x2, y1, z1), (x2, y2, z1), (x2, y2, z2), (x2, y1, z2)],
             "west": [(x1, y1, z1), (x1, y2, z1), (x1, y2, z2), (x1, y1, z2)]}
    rot = element.get("rotation")
    for name in element.get("faces", {}):
        pts = [rotate(p, rot) for p in quads[name]]
        n = rotate(NORMALS[name], {**rot, "origin": [0, 0, 0]}) if rot else NORMALS[name]
        yield name, pts, n


def basis(n):
    a = (1, 0, 0) if abs(n[0]) < 0.9 else (0, 1, 0)
    u = cross(n, a)
    lu = math.sqrt(dot(u, u))
    u = tuple(c / lu for c in u)
    return u, cross(n, u)


def dot(a, b):
    return sum(x * y for x, y in zip(a, b))


def cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def area(poly):
    return abs(sum(poly[i][0] * poly[i - 1][1] - poly[i - 1][0] * poly[i][1] for i in range(len(poly)))) / 2


def clip(subject, clipper):
    """Сазерленд–Ходжман: пересечение выпуклых многоугольников (оба против часовой после ориентации)."""
    def orient(p):
        s = sum(p[i][0] * p[i - 1][1] - p[i - 1][0] * p[i][1] for i in range(len(p)))
        return p if s < 0 else p[::-1]
    subject, clipper = orient(subject), orient(clipper)
    out = subject
    for i in range(len(clipper)):
        a, b = clipper[i - 1], clipper[i]
        inp, out = out, []
        if not inp:
            break
        def inside(p):
            return (b[0] - a[0]) * (p[1] - a[1]) - (b[1] - a[1]) * (p[0] - a[0]) >= -EPS
        for j in range(len(inp)):
            p, q = inp[j - 1], inp[j]
            if inside(q):
                if not inside(p):
                    out.append(intersect(p, q, a, b))
                out.append(q)
            elif inside(p):
                out.append(intersect(p, q, a, b))
    return out


def intersect(p, q, a, b):
    dx1, dy1 = q[0] - p[0], q[1] - p[1]
    dx2, dy2 = b[0] - a[0], b[1] - a[1]
    den = dx1 * dy2 - dy1 * dx2
    t = ((a[0] - p[0]) * dy2 - (a[1] - p[1]) * dx2) / den
    return (p[0] + t * dx1, p[1] + t * dy1)


def check(elements, label):
    fs = []
    for i, e in enumerate(elements):
        for name, pts, n in faces(e):
            fs.append((i, name, pts, n))
    bad = []
    for a in range(len(fs)):
        for b in range(a + 1, len(fs)):
            ia, na, pa, nn = fs[a]
            ib, nb, pb, mm = fs[b]
            if ia == ib or dot(nn, mm) < 1 - 1e-6 or abs(dot(nn, pa[0]) - dot(nn, pb[0])) > EPS:
                continue
            u, v = basis(nn)
            A = [(dot(p, u), dot(p, v)) for p in pa]
            B = [(dot(p, u), dot(p, v)) for p in pb]
            inter = clip(A, B)
            if len(inter) >= 3 and area(inter) > 1e-3:
                bad.append(f"{label}: элементы {ia}.{na} и {ib}.{nb} — {area(inter):.2f} пикс²")
    return bad


def load(name):
    with open(os.path.join(MODELS, name + ".json"), encoding="utf-8") as f:
        return json.load(f).get("elements", [])


def main():
    names = sys.argv[1:] or sorted(os.path.basename(p)[:-5] for p in glob.glob(os.path.join(MODELS, "*.json")))
    bad = []
    for n in names:
        bad += check(load(n), n)
    if not sys.argv[1:]:
        for combo in COMBOS:
            elements = []
            for n in combo:
                elements += load(n)
            bad += check(elements, " + ".join(combo))
    print("\n".join(bad) if bad else "z-fighting не найден")
    return 1 if bad else 0


if __name__ == "__main__":
    sys.exit(main())
