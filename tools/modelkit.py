#!/usr/bin/env python3
"""Инструменты цельных моделей (008): объёмы без общих плоскостей, UV в пределах текстуры, варианты
блокстейтов. Все модели 008 строятся здесь, затем проверяются tools/check_zfight.py."""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources")
ASSETS = os.path.join(ROOT, "assets", "spacereloaded")
NS = "spacereloaded"
FACES = ("up", "down", "north", "south", "east", "west")
ROT = {"north": 0, "east": 90, "south": 180, "west": 270}


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent="\t", ensure_ascii=False)
        f.write("\n")


def default_uv(face, f, t):
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {"up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1],
            "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
            "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1]}[face]


def fit_uv(face, f, t):
    """UV по умолчанию, вписанные в 0..16 (элементы крупнее блока иначе берут пиксели атласа)."""
    uv = default_uv(face, f, t)
    out = []
    for a, b in ((uv[0], uv[2]), (uv[1], uv[3])):
        lo, hi = min(a, b), max(a, b)
        if lo < 0 or hi > 16:
            span = hi - lo
            k = 16 / span if span > 16 else 1
            base = 0 if span > 16 else max(0, min(16 - span, lo % 16))
            a, b = base + (a - lo) * k, base + (b - lo) * k
        out.append((round(a, 4), round(b, 4)))
    return [out[0][0], out[1][0], out[0][1], out[1][1]]


def box(f, t, tex, skip=(), over=None, rot=None, shade=True):
    """Объём from..to; tex — текстура граней ("#ключ"), over — замена по граням, skip — не рисовать."""
    over = over or {}
    faces = {x: {"texture": over.get(x, tex), "uv": fit_uv(x, f, t)} for x in FACES if x not in skip}
    e = {"from": list(f), "to": list(t), "faces": faces}
    if rot:
        e["rotation"] = rot
    if not shade:
        e["shade"] = False
    return e


def model(name, textures, elements, parent="minecraft:block/block", display=None):
    textures = dict(textures)
    textures.setdefault("particle", next(iter(textures.values())))
    obj = {"parent": parent, "textures": textures, "elements": elements}
    if display:
        obj["display"] = display
    write(os.path.join(ASSETS, "models", "block", name + ".json"), obj)


def plain(name, parent, textures):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), {"parent": parent, "textures": textures})


def blockstate(name, variants):
    write(os.path.join(ASSETS, "blockstates", name + ".json"), {"variants": variants})


def facing_variants(model_for):
    """Варианты facing × formed × active: model_for(formed, active) -> id модели; модель смотрит на север."""
    out = {}
    for facing, y in ROT.items():
        for formed in ("false", "true"):
            for active in ("false", "true"):
                v = {"model": model_for(formed == "true", active == "true")}
                if y:
                    v["y"] = y
                out[f"active={active},facing={facing},formed={formed}"] = v
    return out


def facing_formed_variants(model_for):
    out = {}
    for facing, y in ROT.items():
        for formed in ("false", "true"):
            v = {"model": model_for(formed == "true")}
            if y:
                v["y"] = y
            out[f"facing={facing},formed={formed}"] = v
    return out


def item(name, model_ref):
    write(os.path.join(ASSETS, "items", name + ".json"), {"model": {"type": "minecraft:model", "model": model_ref}})


def sr(name):
    return name if ":" in name else f"{NS}:{name}"
