"""Мультиблоки из датапак-шаблонов: клетки, спецификация материалов, изометрический вид.

Формат шаблона: ключевой блок в (0,0,0); "cells": offset [x,y,z] + "block" (id или #тег);
"repeat": {cells, step, min, max, display}. Локальная +z — внутрь от лицевой грани ключа, +y вверх."""
import json

from PIL import Image

from . import res, render, icons

MB_DIR = res.RES / f"data/{res.MOD}/{res.MOD}/multiblock"
OUT_DIR = res.DOCS / "img/mb"



UNFORMED_ON_SITE = {"centrifuge_cascade"}
# Показательная тарелка антенны: шаблон содержит только приёмник и опору, панели игрок кладёт сам
# любым плоским пятном в плоскости +2 (до 1000). На схеме — круг r² ≤ 12 (37 панелей, Ø ≈ 6.9 м).
DEMO_DISH = {"deep_space_antenna": 12}
DISH_PANEL = f"{res.MOD}:dish_panel"
DISH_MAX = 1000

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
        r2 = DEMO_DISH.get(mid)
        if r2:
            n = int(r2 ** 0.5) + 1
            for dx in range(-n, n + 1):
                for dz in range(-n, n + 1):
                    if dx * dx + dz * dz <= r2:
                        self.cells.append(Cell((dx, 2, dz), DISH_PANEL, "demo"))
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
            elif b == DISH_PANEL and self.id in DEMO_DISH:
                rng = (1, DISH_MAX)
            out.append((b, n, rng))
        return out

    def line_axis(self):
        """Ось линии структуры (катушки катапульты, ячейки стека) — по шагу повтора."""
        step = (self.repeat or {}).get("step", [0, 0, -1])
        return "xyz"[max(range(3), key=lambda i: abs(step[i]))]

    def block_state(self, block, formed=None):
        """(модель, x, y) собранного вида: вариант blockstate с formed/in_rail, ключ — лицом наружу
        (facing=north: шаблон строится вглубь по +z), ось — вдоль линии структуры."""
        ns, _, name = block.partition(":")
        bs = res.read_json(ns, f"blockstates/{name}.json")
        if not bs or "variants" not in bs:
            return None
        # собранный вид каскада — полые кожухи под роторы клиентского рендера; на сайте роторов нет,
        # поэтому центрифуги показаны цельными, как их ставит игрок
        if formed is None:
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

    def export3d(self):
        """Сцена для 3D-просмотрщика: те же варианты блокстейтов и то же зеркалирование x, что у изометрии."""
        from . import models3d
        parts = []
        for c in self.cells:
            if c.shown == "minecraft:air" or c.role == "demo":
                continue
            state = self.block_state(c.shown)
            model, bx, by = state if state else (icons.model_for(c.shown), 0, 0)
            path = models3d.export_model(model) if model else None
            if path:
                parts.append({"m": path, "p": [-c.pos[0], c.pos[1], c.pos[2]], "x": bx, "y": by})
        demo = [c for c in self.cells if c.role == "demo"]
        if demo:
            parts.extend(self._dish_parts(demo, models3d))
        return models3d.export_scene(self.id, parts) if parts else None

    @staticmethod
    def _dish_parts(panels, models3d):
        """Собранная тарелка, как рисует DsnRenderer (в зените): сплошная оболочка по параболоиду
        y = r²/4f (F/D = 0.4, D по площади панелей) — вершины в углах клеток общие, тыл и кромка;
        четыре растяжки от кромки и облучатель в фокусе."""
        import math
        pivot, lift, shell = 1 + 12.5 / 16, 0.3, 1.5 / 16
        d = 2 * math.sqrt(len(panels) / math.pi)
        f = 0.4 * d
        base = pivot + lift
        cells = {(-c.pos[0], c.pos[2]) for c in panels}

        def surf(x, z, back=False):
            return [x + 0.5, base + (x * x + z * z) / (4 * f) - (shell if back else 0), z + 0.5]

        def shade(nx, ny, nz):
            ln = math.sqrt(nx * nx + ny * ny + nz * nz) or 1
            nx, ny, nz = nx / ln, ny / ln, nz / ln
            return round(nx * nx * 0.6 + ny * ny * (1 if ny > 0 else 0.5) + nz * nz * 0.8, 3)

        quads = []
        for cx, cz in sorted(cells):
            x0, x1, z0, z1 = cx - 0.5, cx + 0.5, cz - 0.5, cz + 0.5
            s_top = shade(-cx / (2 * f), 1, -cz / (2 * f))
            quads.append({"p": [surf(x0, z1), surf(x1, z1), surf(x1, z0), surf(x0, z0)],
                          "uv": [[0, 0], [1, 0], [1, 1], [0, 1]], "s": s_top})
            quads.append({"p": [surf(x0, z1, True), surf(x0, z0, True), surf(x1, z0, True), surf(x1, z1, True)],
                          "uv": [[0, 0], [0, 1], [1, 1], [1, 0]], "s": round(s_top * 0.7, 3)})
            for (nx, nz), (ax, az, bx, bz) in (((-1, 0), (x0, z0, x0, z1)), ((1, 0), (x1, z1, x1, z0)),
                                              ((0, -1), (x1, z0, x0, z0)), ((0, 1), (x0, z1, x1, z1))):
                if (cx + nx, cz + nz) in cells:
                    continue
                quads.append({"p": [surf(ax, az, True), surf(bx, bz, True), surf(bx, bz), surf(ax, az)],
                              "uv": [[0, 1], [1, 1], [1, 0.9], [0, 0.9]], "s": shade(nx, 0, nz)})
        tex = models3d.texture_path(f"{res.MOD}:block/dish_panel")
        out = [{"mesh": {"t": tex, "quads": quads}}]
        # растяжки: брус 1.5 px от кромки к облучателю
        rim = math.sqrt(len(panels) / math.pi) * 0.8
        rim_y = base + rim * rim / (4 * f)
        top = [0.5, base + f - 0.3, 0.5]
        bars = []
        w = 0.75 / 16
        for ax, az in ((rim, 0), (-rim, 0), (0, rim), (0, -rim)):
            a = [ax + 0.5, rim_y, az + 0.5]
            dvec = [top[i] - a[i] for i in range(3)]
            ln = math.sqrt(sum(v * v for v in dvec))
            dvec = [v / ln for v in dvec]
            up = [0, 1, 0] if abs(dvec[1]) < 0.9 else [1, 0, 0]
            e1 = [dvec[1] * up[2] - dvec[2] * up[1], dvec[2] * up[0] - dvec[0] * up[2], dvec[0] * up[1] - dvec[1] * up[0]]
            l1 = math.sqrt(sum(v * v for v in e1)); e1 = [v / l1 * w for v in e1]
            e2 = [dvec[1] * e1[2] - dvec[2] * e1[1], dvec[2] * e1[0] - dvec[0] * e1[2], dvec[0] * e1[1] - dvec[1] * e1[0]]
            l2 = math.sqrt(sum(v * v for v in e2)); e2 = [v / l2 * w for v in e2]
            sides = [e1, e2, [-v for v in e1], [-v for v in e2]]
            for k in range(4):
                s0, s1 = sides[k], sides[(k + 1) % 4]
                bars.append({"p": [[a[i] + s0[i] for i in range(3)], [top[i] + s0[i] for i in range(3)],
                                   [top[i] + s1[i] for i in range(3)], [a[i] + s1[i] for i in range(3)]],
                             "uv": [[0, 0], [0, 1], [0.1, 1], [0.1, 0]], "s": 0.8})
        out.append({"mesh": {"t": models3d.texture_path(f"{res.MOD}:block/dsn_mount"), "quads": bars}})
        horn = models3d.export_model(res.resolve_model(f"{res.MOD}:block/feed_horn"))
        if horn:
            # раструбом вниз к зеркалу: поворот X на 180°, низ — в фокусе
            out.append({"m": horn, "p": [0, base + f - 1, 0], "m3": [1, 0, 0, 0, -1, 0, 0, 0, -1]})
        return out

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
            if c.shown == "minecraft:air" or c.role == "demo":
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
