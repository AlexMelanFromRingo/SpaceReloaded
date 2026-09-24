"""Мультиблоки из датапак-шаблонов: клетки, спецификация материалов, изометрический вид.

Формат шаблона: ключевой блок в (0,0,0); "cells": offset [x,y,z] + "block" (id или #тег);
"repeat": {cells, step, min, max, display}. Локальная +z — внутрь от лицевой грани ключа, +y вверх."""
import json

from PIL import Image

from . import res, render, icons

MB_DIR = res.RES / f"data/{res.MOD}/{res.MOD}/multiblock"
OUT_DIR = res.DOCS / "img/mb"


class Cell:
    def __init__(self, pos, block, role):
        self.pos, self.block, self.role = tuple(pos), block, role  # role: key | fixed | repeat

    @property
    def candidates(self):
        return res.tag_values(self.block, "block") if self.block.startswith("#") else [self.block]

    @property
    def shown(self):
        return self.candidates[0]


class Multiblock:
    def __init__(self, mid, data):
        self.id, self.data = mid, data
        self.key = data["key"]
        self.cells = [Cell((0, 0, 0), self.key, "key")]
        for c in data.get("cells", []):
            self.cells.append(Cell(c["offset"], c["block"], "fixed"))
        rep = data.get("repeat")
        self.repeat = rep
        if rep:
            step = rep["step"]
            for k in range(rep.get("display", rep.get("min", 1))):
                for c in rep["cells"]:
                    o = c["offset"]
                    self.cells.append(Cell([o[i] + step[i] * k for i in range(3)], c["block"], "repeat"))
        xs, ys, zs = zip(*(c.pos for c in self.cells))
        self.min = (min(xs), min(ys), min(zs))
        self.max = (max(xs), max(ys), max(zs))

    @property
    def size(self):
        return tuple(self.max[i] - self.min[i] + 1 for i in range(3))

    def layers(self):
        """[(y, {(x,z): Cell})] снизу вверх."""
        out = []
        for y in range(self.min[1], self.max[1] + 1):
            out.append((y, {(c.pos[0], c.pos[2]): c for c in self.cells if c.pos[1] == y}))
        return out

    def bom(self):
        """[(block, count_shown, (min,max) | None, candidates)] без воздуха; воздух отдельно."""
        rows, order = {}, []
        rep = self.repeat or {}
        per_rep = {}
        for c in rep.get("cells", []):
            per_rep[c["block"]] = per_rep.get(c["block"], 0) + 1
        for c in self.cells:
            if c.block not in rows:
                rows[c.block] = 0; order.append(c.block)
            rows[c.block] += 1
        out = []
        for b in order:
            n = rows[b]
            rng = None
            if b in per_rep:
                base = n - per_rep[b] * rep.get("display", 0)
                rng = (base + per_rep[b] * rep["min"], base + per_rep[b] * rep["max"])
            out.append((b, n, rng))
        return out

    def render(self, S=64, SS=2, yrot=0):
        """PNG собранного вида: все грани всех блоков сцены, общий painter-sort.
        yrot поворачивает всю сцену вокруг Y (чтобы длинная структура уходила вглубь)."""
        faces, cache = [], {}
        for c in self.cells:
            pos = c.pos
            if yrot:
                p = render._rot(pos, "y", -yrot, (0, 0, 0))
                pos = tuple(round(v) for v in p)
            if c.shown == "minecraft:air":
                continue
            model = icons.model_for(c.shown)
            if not model or not model.get("elements"):
                continue
            faces.extend(render.model_faces(model, yrot=yrot, offset=pos, tex_cache=cache))
        img, _ = render.draw_faces(faces, S * SS, pad=4)
        if SS > 1:
            img = img.resize((max(1, img.width // SS), max(1, img.height // SS)), Image.LANCZOS)
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / f"{self.id}.png"
        img.save(path, optimize=True)
        return f"img/mb/{self.id}.png", img.size


def load_all():
    return {p.stem: Multiblock(p.stem, json.loads(p.read_text())) for p in sorted(MB_DIR.glob("*.json"))}
