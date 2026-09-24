"""Изометрический рендер блочных моделей Minecraft средствами PIL.

Проекция как в инвентаре игры: камера сверху-северо-запада, видны грани
up (верх), north (слева) и west (справа), затенение 1.0 / 0.8 / 0.6.
Каждая грань элемента модели проецируется в параллелограмм, текстура
натягивается аффинно (NEAREST), грани сортируются по глубине (painter).
Поддерживаются элементы с поворотом, uv, rotation граней, поворот блока по Y."""
import math

from PIL import Image, ImageDraw

from . import res

VIEW = (-1.0, 1.0, -1.0)  # направление на зрителя

# углы грани: (u1,v1)-угол, (u2,v1)-угол, (u1,v2)-угол; x1..z2 -> индексы в (from,to)
FACE_CORNERS = {
    "up":    (("x1", "y2", "z1"), ("x2", "y2", "z1"), ("x1", "y2", "z2")),
    "down":  (("x1", "y1", "z2"), ("x2", "y1", "z2"), ("x1", "y1", "z1")),
    "north": (("x2", "y2", "z1"), ("x1", "y2", "z1"), ("x2", "y1", "z1")),
    "south": (("x1", "y2", "z2"), ("x2", "y2", "z2"), ("x1", "y1", "z2")),
    "west":  (("x1", "y2", "z1"), ("x1", "y2", "z2"), ("x1", "y1", "z1")),
    "east":  (("x2", "y2", "z2"), ("x2", "y2", "z1"), ("x2", "y1", "z2")),
}


def default_uv(face, f, t):
    x1, y1, z1 = f
    x2, y2, z2 = t
    return {
        "up": [x1, z1, x2, z2], "down": [x1, 16 - z2, x2, 16 - z1],
        "north": [16 - x2, 16 - y2, 16 - x1, 16 - y1], "south": [x1, 16 - y2, x2, 16 - y1],
        "west": [z1, 16 - y2, z2, 16 - y1], "east": [16 - z2, 16 - y2, 16 - z1, 16 - y1],
    }[face]


def _rot(p, axis, ang, origin):
    a = math.radians(ang)
    c, s = math.cos(a), math.sin(a)
    x, y, z = (p[i] - origin[i] for i in range(3))
    if axis == "x":
        y, z = y * c - z * s, y * s + z * c
    elif axis == "y":
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + origin[0], y + origin[1], z + origin[2])


def _sub(a, b):
    return tuple(a[i] - b[i] for i in range(3))


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _dot(a, b):
    return sum(a[i] * b[i] for i in range(3))


class Face:
    __slots__ = ("o", "u", "v", "img", "shade", "depth")


def model_faces(model, yrot=0, offset=(0, 0, 0), tex_cache=None, xrot=0):
    """Все видимые грани модели в мировых координатах (единица = блок). xrot/yrot — повороты
    варианта blockstate (как в игре: сначала вокруг X, потом вокруг Y) и сцены."""
    tex_cache = {} if tex_cache is None else tex_cache
    out = []
    for el in model["elements"] or []:
        f, t = el["from"], el["to"]
        coords = {"x1": f[0], "y1": f[1], "z1": f[2], "x2": t[0], "y2": t[1], "z2": t[2]}
        rot = el.get("rotation")
        for fname, fd in el.get("faces", {}).items():
            tex_id = res.texture_ref(model["textures"], fd.get("texture"))
            if not tex_id:
                continue
            if tex_id not in tex_cache:
                tex_cache[tex_id] = res.load_texture(tex_id)
            tex = tex_cache[tex_id]
            if tex is None:
                continue
            pts = [tuple(coords[k] for k in c) for c in FACE_CORNERS[fname]]
            if rot:
                pts = [_rot(p, rot.get("axis", "y"), rot.get("angle", 0), rot.get("origin", [8, 8, 8])) for p in pts]
            if xrot:
                pts = [_rot(p, "x", -xrot, (8, 8, 8)) for p in pts]
            if yrot:
                pts = [_rot(p, "y", -yrot, (8, 8, 8)) for p in pts]
            pts = [tuple(p[i] / 16 + offset[i] for i in range(3)) for p in pts]
            o, pu, pv = pts
            n = _cross(_sub(pv, o), _sub(pu, o))  # наружная нормаль (правило обхода граней выше)
            if _dot(n, VIEW) <= 1e-9:
                continue
            ln = math.sqrt(_dot(n, n)) or 1
            nx, ny, nz = (c / ln for c in n)
            shade = 1.0 if el.get("shade") is False else (
                nx * nx * 0.6 + ny * ny * (1.0 if ny > 0 else 0.5) + nz * nz * 0.8)
            uv = fd.get("uv") or default_uv(fname, f, t)
            img = _crop(tex, uv, fd.get("rotation", 0))
            face = Face()
            face.o, face.u, face.v = o, _sub(pu, o), _sub(pv, o)
            face.img, face.shade = img, shade
            center = tuple(o[i] + (face.u[i] + face.v[i]) / 2 for i in range(3))
            face.depth = _dot(center, VIEW)
            out.append(face)
    return out


def _crop(tex, uv, rotation):
    tw, th = tex.size
    u1, v1, u2, v2 = (uv[0] * tw / 16, uv[1] * th / 16, uv[2] * tw / 16, uv[3] * th / 16)
    l, r = sorted((u1, u2))
    tp, b = sorted((v1, v2))
    l, tp = int(math.floor(l + 1e-6)), int(math.floor(tp + 1e-6))
    r, b = max(int(math.ceil(r - 1e-6)), l + 1), max(int(math.ceil(b - 1e-6)), tp + 1)
    img = tex.crop((l, tp, r, b))
    if u1 > u2:
        img = img.transpose(Image.FLIP_LEFT_RIGHT)
    if v1 > v2:
        img = img.transpose(Image.FLIP_TOP_BOTTOM)
    if rotation:
        img = img.rotate(-rotation, expand=True)
    return img


def project(p, S):
    x, y, z = p
    return ((z - x) * S / 2, -(x + z) * S / 4 - y * S / 2)


def _shade(img, k):
    if k >= 0.999:
        return img
    r, g, b, a = img.split()
    lut = [min(255, int(i * k + 0.5)) for i in range(256)]
    return Image.merge("RGBA", (r.point(lut), g.point(lut), b.point(lut), a))


def draw_faces(faces, S, pad=2):
    """Рисует грани на холсте по размеру сцены. Возвращает (Image, origin_px)."""
    if not faces:
        return None, (0, 0)
    faces = sorted(faces, key=lambda f: f.depth)
    pts = []
    for f in faces:
        for p in (f.o, tuple(f.o[i] + f.u[i] for i in range(3)), tuple(f.o[i] + f.v[i] for i in range(3)),
                  tuple(f.o[i] + f.u[i] + f.v[i] for i in range(3))):
            pts.append(project(p, S))
    minx = min(p[0] for p in pts) - pad
    miny = min(p[1] for p in pts) - pad
    W = int(math.ceil(max(p[0] for p in pts) - minx + pad))
    H = int(math.ceil(max(p[1] for p in pts) - miny + pad))
    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    for f in faces:
        O = project(f.o, S)
        U = project(tuple(f.o[i] + f.u[i] for i in range(3)), S)
        V = project(tuple(f.o[i] + f.v[i] for i in range(3)), S)
        O = (O[0] - minx, O[1] - miny)
        U = (U[0] - minx - O[0], U[1] - miny - O[1])
        V = (V[0] - minx - O[0], V[1] - miny - O[1])
        det = U[0] * V[1] - V[0] * U[1]
        if abs(det) < 1e-6:
            continue
        quad = [O, (O[0] + U[0], O[1] + U[1]), (O[0] + U[0] + V[0], O[1] + U[1] + V[1]), (O[0] + V[0], O[1] + V[1])]
        bx0 = max(0, int(math.floor(min(q[0] for q in quad))) - 1)
        by0 = max(0, int(math.floor(min(q[1] for q in quad))) - 1)
        bx1 = min(W, int(math.ceil(max(q[0] for q in quad))) + 1)
        by1 = min(H, int(math.ceil(max(q[1] for q in quad))) + 1)
        if bx1 <= bx0 or by1 <= by0:
            continue
        img = _shade(f.img, f.shade)
        w, h = img.size
        # 1px «поля» с повтором края: убирает швы на стыках граней
        padded = Image.new("RGBA", (w + 2, h + 2))
        padded.paste(img, (1, 1))
        padded.paste(img.crop((0, 0, w, 1)), (1, 0)); padded.paste(img.crop((0, h - 1, w, h)), (1, h + 1))
        padded.paste(padded.crop((1, 0, 2, h + 2)), (0, 0)); padded.paste(padded.crop((w, 0, w + 1, h + 2)), (w + 1, 0))
        ia, ib = V[1] / det, -V[0] / det
        ic, id_ = -U[1] / det, U[0] / det
        a, b = w * ia, w * ib
        d, e = h * ic, h * id_
        ox, oy = O[0] - bx0, O[1] - by0
        data = (a, b, 1 - a * ox - b * oy, d, e, 1 - d * ox - e * oy)
        patch = padded.transform((bx1 - bx0, by1 - by0), Image.AFFINE, data, resample=Image.NEAREST)
        mask = Image.new("L", patch.size, 0)
        ImageDraw.Draw(mask).polygon([(q[0] - bx0, q[1] - by0) for q in _grow(quad, 0.35)], fill=255)
        pa = patch.getchannel("A")
        patch.putalpha(Image.composite(pa, mask, mask))
        canvas.alpha_composite(patch, (bx0, by0))
    return canvas, (-minx, -miny)


def _grow(quad, r):
    cx = sum(q[0] for q in quad) / 4
    cy = sum(q[1] for q in quad) / 4
    out = []
    for x, y in quad:
        dx, dy = x - cx, y - cy
        ln = math.hypot(dx, dy) or 1
        out.append((x + dx / ln * r, y + dy / ln * r))
    return out


def fit_square(img, size, ref, fill=0.94, max_up=2.2):
    """Обрезает по альфе и вписывает в квадрат size; ref — размер полного куба на холсте.
    Мелкие модели (кабель, вал) увеличиваются, но не больше чем в max_up раз."""
    bbox = img.getchannel("A").getbbox()
    if not bbox:
        return Image.new("RGBA", (size, size))
    img = img.crop(bbox)
    w, h = img.size
    k = min(size * fill / w, size * fill / h)
    k = min(k, max_up * size / ref)
    nw, nh = max(1, round(w * k)), max(1, round(h * k))
    img = img.resize((nw, nh), Image.LANCZOS)
    out = Image.new("RGBA", (size, size))
    out.alpha_composite(img, ((size - nw) // 2, (size - nh) // 2))
    return out


def flat_layers(model):
    """item/generated: слои layer0..N поверх друг друга (родной размер)."""
    layers = []
    i = 0
    while f"layer{i}" in model["textures"]:
        tid = res.texture_ref(model["textures"], model["textures"][f"layer{i}"])
        im = res.load_texture(tid) if tid else None
        if im is not None:
            layers.append(im)
        i += 1
    if not layers:
        return None
    base = Image.new("RGBA", layers[0].size)
    for im in layers:
        if im.size != base.size:
            im = im.resize(base.size, Image.NEAREST)
        base.alpha_composite(im)
    return base


# синтетическая модель сундука (special-рендер ванили): коробка + крышка + замок
def _box(fr, to, uvs):
    return {"from": fr, "to": to, "faces": {k: {"texture": "#c", "uv": [c / 4 for c in v]} for k, v in uvs.items()}}


CHEST_MODEL = {
    "textures": {"c": "minecraft:entity/chest/normal"},
    "elements": [
        _box([1, 0, 1], [15, 10, 15], {"north": [14, 43, 28, 33], "west": [0, 43, 14, 33], "east": [28, 43, 42, 33]}),
        _box([1, 9, 1], [15, 14, 15], {"north": [14, 19, 28, 14], "west": [0, 19, 14, 14], "east": [28, 19, 42, 14],
                                        "up": [14, 0, 28, 14]}),
        _box([7, 7, 0], [9, 11, 1], {"north": [1, 5, 3, 1], "west": [0, 5, 1, 1], "up": [1, 0, 3, 1]}),
    ],
}
