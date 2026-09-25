"""Мультиблоки из датапак-шаблонов: клетки, спецификация материалов, изометрический вид.

Формат шаблона: ключевой блок в (0,0,0); "cells": offset [x,y,z] + "block" (id или #тег);
"repeat": {cells, step, min, max, display}. Локальная +z — внутрь от лицевой грани ключа, +y вверх."""
import json

from PIL import Image

from . import res, render, icons

MB_DIR = res.RES / f"data/{res.MOD}/{res.MOD}/multiblock"
OUT_DIR = res.DOCS / "img/mb"



UNFORMED_ON_SITE = {"centrifuge_cascade"}

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

    def line_axis(self):
        """Ось линии структуры (катушки катапульты, ячейки стека) — по шагу повтора."""
        step = (self.repeat or {}).get("step", [0, 0, -1])
        return "xyz"[max(range(3), key=lambda i: abs(step[i]))]

    def block_state(self, block):
        """(модель, x, y) собранного вида: вариант blockstate с formed/in_rail, ключ — лицом наружу
        (facing=north: шаблон строится вглубь по +z), ось — вдоль линии структуры."""
        ns, _, name = block.partition(":")
        bs = res.read_json(ns, f"blockstates/{name}.json")
        if not bs or "variants" not in bs:
            return None
        # собранный вид каскада — полые кожухи под роторы клиентского рендера; на сайте роторов нет,
        # поэтому центрифуги показаны цельными, как их ставит игрок
        formed = "false" if self.id in UNFORMED_ON_SITE else "true"
        want = {"formed": formed, "in_rail": "true", "facing": "north", "lit": "false", "axis": self.line_axis(),
                "powered": "false", "open": "false"}
        best, score = None, -1
        for key, v in bs["variants"].items():
            props = dict(kv.split("=") for kv in key.split(",") if "=" in kv)
            s = sum(1 for k, val in props.items() if want.get(k) == val)
            if s > score:
                best, score = (v[0] if isinstance(v, list) else v), s
        try:
            model = res.resolve_model(best["model"])
        except res.ResourceError:
            return None
        return model, best.get("x", 0), best.get("y", 0)

    def render(self, S=64, SS=2, yrot=0):
        """PNG собранного вида: все грани всех блоков сцены, общий painter-sort.
        yrot поворачивает всю сцену вокруг Y (чтобы длинная структура уходила вглубь)."""
        faces, cache = [], {}
        for c in self.cells:
            # локальные клетки → мир при лице ключа на север (MultiblockTemplate.toWorld): +z — внутрь,
            # локальная +x — вправо от смотрящего на лицо ключа, то есть −x мира
            pos = (-c.pos[0], c.pos[1], c.pos[2])
            if yrot:
                p = render._rot(pos, "y", -yrot, (0, 0, 0))
                pos = tuple(round(v) for v in p)
            if c.shown == "minecraft:air":
                continue
            state = self.block_state(c.shown)
            model, bx, by = state if state else (icons.model_for(c.shown), 0, 0)
            if not model or not model.get("elements"):
                continue
            faces.extend(render.model_faces(model, yrot=yrot + by, xrot=bx, offset=pos, tex_cache=cache))
        img, _ = render.draw_faces(faces, S * SS, pad=4)
        if SS > 1:
            img = img.resize((max(1, img.width // SS), max(1, img.height // SS)), Image.LANCZOS)
        OUT_DIR.mkdir(parents=True, exist_ok=True)
        path = OUT_DIR / f"{self.id}.png"
        img.save(path, optimize=True)
        return f"img/mb/{self.id}.png", img.size


def load_all():
    return {p.stem: Multiblock(p.stem, json.loads(p.read_text())) for p in sorted(MB_DIR.glob("*.json"))}
