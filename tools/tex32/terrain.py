"""Порода, реголит, лёд, руды и жидкое топливо (012). Все текстуры бесшовные: шум периодический с периодом 32,
детали рисуются с переносом через край; рамок и сеток нет. Руда = та же порода, что базовый камень, плюс минерал.

Рельеф — поле высот h(x, y) ∈ [0, 1], квантованное в ступени палитры; кромки подсвечиваются по разности высот
со соседом сверху-слева (свет сверху-слева, как во всём наборе)."""
import math

from . import tex
from .kit import N, Tex, _h, mix, shade

# ------------------------------------------------------------------ палитры пород (тень → блик)
STONE = [(0x5C, 0x5C, 0x5E), (0x6C, 0x6C, 0x6E), (0x7C, 0x7C, 0x7E), (0x8C, 0x8C, 0x8E), (0xA2, 0xA2, 0xA4)]
DEEPSLATE = [(0x2E, 0x2E, 0x34), (0x3A, 0x3A, 0x40), (0x47, 0x47, 0x4D), (0x55, 0x55, 0x5B), (0x68, 0x68, 0x6E)]
RED_SANDSTONE = [(0x86, 0x42, 0x12), (0xA2, 0x52, 0x18), (0xB8, 0x62, 0x20), (0xC8, 0x72, 0x2C), (0xDA, 0x8A, 0x42)]
MOON_STONE = [(0x46, 0x46, 0x4C), (0x54, 0x54, 0x5A), (0x62, 0x62, 0x68), (0x70, 0x70, 0x76), (0x86, 0x86, 0x8C)]
REGOLITH = [(0x80, 0x80, 0x86), (0x93, 0x93, 0x98), (0xA3, 0xA3, 0xA8), (0xB2, 0xB2, 0xB6), (0xC6, 0xC6, 0xCA)]
ANORTHOSITE = [(0x5E, 0x5E, 0x62), (0x6C, 0x6C, 0x70), (0x7A, 0x7A, 0x7E), (0x88, 0x88, 0x8C), (0x9C, 0x9C, 0xA0)]
PLAGIOCLASE = [(0xB8, 0xB8, 0xBE), (0xCC, 0xCC, 0xD2), (0xDE, 0xDE, 0xE4), (0xEC, 0xEC, 0xF2), (0xFA, 0xFA, 0xFE)]
ASTEROID = [(0x26, 0x22, 0x28), (0x32, 0x2E, 0x34), (0x3E, 0x3A, 0x40), (0x4A, 0x46, 0x4C), (0x5C, 0x58, 0x5E)]
CHONDRULE = [(0x5A, 0x54, 0x54), (0x70, 0x6A, 0x68), (0x86, 0x80, 0x7C), (0x9C, 0x96, 0x90), (0xB6, 0xB0, 0xA8)]
SHALE = [(0x34, 0x34, 0x39), (0x40, 0x40, 0x45), (0x4B, 0x4B, 0x50), (0x56, 0x56, 0x5B), (0x64, 0x64, 0x69)]
KEROGEN = [(0x12, 0x10, 0x14), (0x1A, 0x18, 0x1C), (0x22, 0x20, 0x25), (0x2C, 0x2A, 0x2F), (0x3A, 0x38, 0x3E)]
SINTER = [(0x70, 0x72, 0x7E), (0x80, 0x84, 0x90), (0x8E, 0x92, 0xA0), (0x9C, 0xA2, 0xB0), (0xB4, 0xBA, 0xC8)]
ICE = [(0x8C, 0xB8, 0xD0), (0xA4, 0xCC, 0xE0), (0xB8, 0xDA, 0xEA), (0xCC, 0xE6, 0xF2), (0xEC, 0xF8, 0xFC)]
MARS_DUST = [(0x8E, 0x44, 0x12), (0xAA, 0x56, 0x18), (0xC0, 0x68, 0x22), (0xD2, 0x78, 0x2E), (0xE2, 0x92, 0x4A)]

# ------------------------------------------------------------------ палитры минералов
BERYL = [(0x1C, 0x5E, 0x4E), (0x2A, 0x80, 0x68), (0x40, 0xA4, 0x86), (0x62, 0xC8, 0xA6), (0xB0, 0xF0, 0xDA)]
BORAX = [(0x9C, 0x96, 0x86), (0xBA, 0xB4, 0xA4), (0xD4, 0xD0, 0xC2), (0xE8, 0xE6, 0xDC), (0xFA, 0xFA, 0xF4)]
EVAPORITE = [(0x86, 0x80, 0x74), (0x94, 0x8E, 0x82), (0xA2, 0x9C, 0x90), (0xB0, 0xAA, 0x9E), (0xC0, 0xBA, 0xAE)]
FLUORITE = [(0x4C, 0x22, 0x7C), (0x6C, 0x34, 0xA8), (0x90, 0x52, 0xCE), (0xB0, 0x78, 0xEA), (0xDA, 0xB8, 0xFA)]
GALENA = [(0x38, 0x3C, 0x46), (0x52, 0x58, 0x64), (0x70, 0x78, 0x86), (0x96, 0x9E, 0xAC), (0xD8, 0xDE, 0xE8)]
HALITE = [(0xC0, 0x92, 0x94), (0xD8, 0xAA, 0xAA), (0xEA, 0xC2, 0xC0), (0xF6, 0xDA, 0xD6), (0xFF, 0xF2, 0xEE)]
SPODUMENE = [(0x9C, 0x5A, 0x7E), (0xC0, 0x78, 0x9E), (0xDC, 0x98, 0xBC), (0xF0, 0xBC, 0xD6), (0xFE, 0xE4, 0xF0)]
ILMENITE = [(0x2C, 0x2C, 0x32), (0x4A, 0x4C, 0x54), (0x7A, 0x7E, 0x88), (0xAE, 0xB2, 0xBC), (0xEC, 0xEE, 0xF4)]
WOLFRAMITE = [(0x1C, 0x1C, 0x22), (0x2C, 0x2E, 0x38), (0x48, 0x4E, 0x5E), (0x6E, 0x78, 0x8E), (0xA8, 0xB4, 0xCC)]
PITCHBLENDE = [(0x10, 0x10, 0x10), (0x1C, 0x1C, 0x1A), (0x2A, 0x2A, 0x26), (0x3E, 0x3E, 0x36), (0x5E, 0x5C, 0x4E)]
AUTUNITE = [(0x6E, 0x74, 0x10), (0x96, 0x9E, 0x18), (0xC0, 0xC8, 0x2A), (0xDC, 0xE6, 0x4C), (0xF4, 0xFA, 0x9A)]

# ------------------------------------------------------------------ жидкое топливо
HYDROLOX = [(0x2E, 0x7C, 0x9C), (0x46, 0xA0, 0xC2), (0x62, 0xBE, 0xDF), (0x7E, 0xD4, 0xF0), (0xC4, 0xF0, 0xFC)]
KEROLOX = [(0x7A, 0x44, 0x14), (0xA4, 0x62, 0x22), (0xC8, 0x80, 0x34), (0xE0, 0x9C, 0x48), (0xF6, 0xCC, 0x86)]
METHALOX = [(0x3E, 0x52, 0x80), (0x5A, 0x72, 0xA4), (0x78, 0x94, 0xC6), (0x92, 0xAE, 0xDC), (0xCC, 0xDC, 0xF6)]


# ------------------------------------------------------------------ периодический шум
def _smooth(t):
    return t * t * (3 - 2 * t)


def vnoise(x, y, cx, cy, seed, n=N):
    """Значение шума в точке (x, y) при решётке с шагом cx × cy px; период по обеим осям — n px."""
    nx, ny = n // cx, n // cy
    gx, gy = x / cx, y / cy
    i0, j0 = int(math.floor(gx)), int(math.floor(gy))
    fx, fy = _smooth(gx - i0), _smooth(gy - j0)

    def v(i, j):
        return _h(i % nx, j % ny, seed)
    a = v(i0, j0) + (v(i0 + 1, j0) - v(i0, j0)) * fx
    b = v(i0, j0 + 1) + (v(i0 + 1, j0 + 1) - v(i0, j0 + 1)) * fx
    return a + (b - a) * fy


def field(seed, octaves, dx=0, dy=0, n=N):
    """Поле высот n×n (по умолчанию 32): сумма октав (cx, cy, вес), нормированная в [0, 1]. dx, dy — сдвиг."""
    f = [[0.0] * n for _ in range(n)]
    for y in range(n):
        for x in range(n):
            s = w = 0.0
            for k, (cx, cy, wt) in enumerate(octaves):
                s += wt * vnoise(x + dx, y + dy, cx, cy, seed * 31 + k, n)
                w += wt
            f[y][x] = s / w
    lo = min(min(r) for r in f)
    hi = max(max(r) for r in f)
    return [[(v - lo) / (hi - lo + 1e-9) for v in r] for r in f]


def relief(t, pal, h, cuts=(0.18, 0.42, 0.66, 0.88), emboss=0.9):
    """Раскраска поля высот ступенями палитры + подсветка кромок (свет сверху-слева)."""
    lvl = [[sum(1 for c in cuts if h[y][x] > c) for x in range(N)] for y in range(N)]
    for y in range(N):
        for x in range(N):
            k = lvl[y][x]
            up = lvl[(y - 1) % N][(x - 1) % N]
            if emboss and k > up:
                k = min(4, k + 1) if _h(x, y, 91) < emboss else k
            elif emboss and k < up:
                k = max(0, k - 1) if _h(x, y, 92) < emboss else k
            t.put(x, y, pal[max(0, min(4, k))])
    return lvl


def wput(t, x, y, c):
    t.put(int(x) % N, int(y) % N, c)


def wdark(t, x, y, k):
    t.darken(int(x) % N, int(y) % N, k)


# ------------------------------------------------------------------ породы
def stone_base(pal, seed):
    """Ванильный по духу камень: крупные мягкие пятна с короткими горизонтальными прожилками."""
    t = Tex()
    h = field(seed, [(16, 16, 0.45), (8, 4, 0.35), (4, 2, 0.2)])
    relief(t, pal, h, cuts=(0.2, 0.4, 0.62, 0.86), emboss=0.8)
    return t


def deepslate_base(pal, seed):
    """Глубинный сланец: вертикальная слоистость (вытянутый по Y шум) и тёмные щели между слоями."""
    t = Tex()
    h = field(seed, [(8, 32, 0.5), (4, 16, 0.3), (2, 4, 0.2)])
    lvl = relief(t, pal, h, cuts=(0.22, 0.44, 0.66, 0.86), emboss=0.6)
    # тонкие вертикальные трещины сланцеватости
    for x in range(N):
        if _h(x, 0, seed + 5) < 0.14:
            y0 = int(_h(x, 1, seed + 5) * N)
            for d in range(4 + int(_h(x, 2, seed + 5) * 8)):
                wdark(t, x, y0 + d, 0.28)
    return t


def red_sandstone_base(seed):
    """Красный песчаник Марса: горизонтальная косая слоистость и мелкое песчаное зерно."""
    t = Tex()
    h = field(seed, [(32, 8, 0.45), (16, 4, 0.35), (4, 2, 0.2)])
    relief(t, RED_SANDSTONE, h, cuts=(0.2, 0.42, 0.64, 0.86), emboss=0.5)
    for y in range(N):
        for x in range(N):
            r = _h(x, y, seed + 7)
            if r < 0.06:
                t.darken(x, y, 0.1)
            elif r > 0.95:
                t.lighten(x, y, 0.1)
    return t


def moon_stone_base(seed=41):
    """Лунный базальт: мелкозернистый, с редкими пузырьковыми порами (везикулами)."""
    t = Tex()
    h = field(seed, [(16, 16, 0.4), (8, 8, 0.35), (2, 2, 0.25)])
    relief(t, MOON_STONE, h, cuts=(0.22, 0.44, 0.64, 0.86), emboss=0.7)
    for i in range(7):
        px, py = int(_h(i, 1, seed) * N), int(_h(i, 2, seed) * N)
        wput(t, px, py, MOON_STONE[0])
        if _h(i, 3, seed) > 0.5:
            wput(t, px + 1, py, MOON_STONE[0])
        wput(t, px + 1, py + 1, MOON_STONE[4])
    return t


# ------------------------------------------------------------------ минералы
def _mask_rim(t, mask, k=0.3):
    """Тёмная кайма вокруг вкраплений: минерал сидит в породе, а не наклеен сверху."""
    for (x, y) in list(mask):
        for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1), (1, 1)):
            p = ((x + dx) % N, (y + dy) % N)
            if p not in mask:
                t.darken(p[0], p[1], k if (dx, dy) in ((1, 0), (0, 1), (1, 1)) else k * 0.6)


def prisms(t, pal, spots, seed, rim=0.3):
    """Призматические кристаллы: spots — [(x, y, длина, ширина, угол°)]. Грань к свету светлее, торец — блик."""
    mask = {}
    for (cx, cy, ln, wd, ang) in spots:
        a = math.radians(ang)
        ux, uy = math.cos(a), math.sin(a)
        vx, vy = -uy, ux
        # нормаль ширины, повёрнутая к свету (сверху-слева)
        if vx + vy > 0:
            vx, vy = -vx, -vy
        r = int(ln / 2 + wd) + 1
        for yy in range(-r, r + 1):
            for xx in range(-r, r + 1):
                u = xx * ux + yy * uy
                v = xx * vx + yy * vy
                if abs(u) <= ln / 2 and abs(v) <= wd / 2:
                    s = v / (wd / 2 + 1e-9)          # +1 — сторона к свету
                    k = 3 if s > 0.34 else 2 if s > -0.34 else 1
                    if u < -ln / 2 + 1.2 and ux * -1 + uy * -1 > 0 or u > ln / 2 - 1.2 and ux + uy < 0:
                        k = 4                          # торец, смотрящий к свету
                    mask[((cx + xx) % N, (cy + yy) % N)] = pal[k]
    _mask_rim(t, mask, rim)
    for (x, y), c in mask.items():
        t.put(x, y, c)
    return mask


def cubes(t, pal, spots, rim=0.3):
    """Кубические кристаллы (флюорит, галенит, галит) в кабинетной проекции: передняя грань — квадрат,
    сверху — светлая грань с бликом по кромке, справа — теневая. spots — [(x, y, s)]: грань s×s, глубина d = s//2."""
    mask = {}
    for (cx, cy, s) in spots:
        d = max(1, s // 2)
        x0, y0 = cx - s // 2, cy - s // 2
        for j in range(d):                         # верхняя грань: сдвиг вправо на уровень j от переда
            off = d - j
            for i in range(s):
                mask[((x0 + i + off) % N, (y0 - off) % N)] = pal[4] if (j == 0 or i == 0) else pal[3]
        for j in range(d):                         # правая грань
            off = d - j
            for i in range(s):
                mask[((x0 + s - 1 + off) % N, (y0 + i - off) % N)] = pal[1] if i else pal[3]
        for yy in range(s):                        # передняя грань
            for xx in range(s):
                k = 2
                if xx == 0 or yy == 0:
                    k = 3
                if yy == s - 1 or xx == s - 1:
                    k = 1
                mask[((x0 + xx) % N, (y0 + yy) % N)] = pal[k]
    _mask_rim(t, mask, rim)
    for (x, y), c in mask.items():
        t.put(x, y, c)
    return mask


def nuggets(t, pal, spots, seed, rim=0.35):
    """Натёчные/зернистые массы (настуран, ильменит): неровный круг, освещённый сверху-слева.
    spots — [(x, y, радиус)]."""
    mask = {}
    for i, (cx, cy, r) in enumerate(spots):
        R = int(r) + 2
        for yy in range(-R, R + 1):
            for xx in range(-R, R + 1):
                ang = math.atan2(yy, xx)
                edge = r * (1 + 0.28 * (_h(int((ang + 4) * 2.2), i, seed) - 0.5))
                d = math.hypot(xx + 0.5, yy + 0.5)
                if d <= edge:
                    s = -(xx + yy) / (1.414 * max(1.0, edge))
                    k = 2 + (1 if s > 0.3 else -1 if s < -0.3 else 0)
                    if d > edge - 1:
                        k -= 1
                    if s > 0.45 and d < edge - 1:
                        k = 4
                    mask[((cx + xx) % N, (cy + yy) % N)] = pal[max(0, min(4, k))]
    _mask_rim(t, mask, rim)
    for (x, y), c in mask.items():
        t.put(x, y, c)
    return mask


def halo(t, pal, spots, radius, seed, k=0.5):
    """Ореол изменённой породы вокруг вкраплений (корка эвапорита, окисление) — мягкое пятно цвета pal."""
    for (cx, cy) in spots:
        for yy in range(-radius, radius + 1):
            for xx in range(-radius, radius + 1):
                d = math.hypot(xx, yy) / radius
                if d < 1 and _h(cx + xx, cy + yy, seed) < (1 - d) * 1.3:
                    x, y = (cx + xx) % N, (cy + yy) % N
                    t.blend(x, y, pal[2 if _h(x, y, seed + 1) > 0.3 else 1], k * (1 - d * 0.6))


# ------------------------------------------------------------------ породы — текстуры
@tex("block/moon_stone")
def moon_stone(_):
    return moon_stone_base()


@tex("block/moon_regolith")
def moon_regolith(_):
    """Реголит: рыхлая пыль мельче пикселя (зерно 1 px) с редкими обломками и следами микрометеоритов."""
    t = Tex()
    h = field(53, [(16, 16, 0.35), (8, 8, 0.25), (1, 1, 0.4)])
    relief(t, REGOLITH, h, cuts=(0.24, 0.44, 0.62, 0.84), emboss=0.25)
    # обломки брекчии: тёмные угловатые камешки с бликом
    for i in range(6):
        px, py = int(_h(i, 11, 53) * N), int(_h(i, 12, 53) * N)
        s = 1 + int(_h(i, 13, 53) * 2)
        for yy in range(s):
            for xx in range(s + 1):
                wput(t, px + xx, py + yy, REGOLITH[1] if yy or xx else REGOLITH[3])
        for xx in range(s + 1):
            wput(t, px + xx + 1, py + s, REGOLITH[0])
    # стеклянные бусинки импактного стекла
    for i in range(3):
        wput(t, _h(i, 21, 53) * N, _h(i, 22, 53) * N, (0x6A, 0x5E, 0x52))
    return t


@tex("block/anorthosite")
def anorthosite(_):
    """Анортозит лунных материков: серая основа с белыми лейстами плагиоклаза."""
    t = Tex()
    h = field(61, [(16, 16, 0.4), (8, 8, 0.35), (2, 2, 0.25)])
    relief(t, ANORTHOSITE, h, cuts=(0.22, 0.44, 0.64, 0.86), emboss=0.6)
    spots = []
    for i in range(12):
        spots.append((int(_h(i, 1, 61) * N), int(_h(i, 2, 61) * N), 3 + int(_h(i, 3, 61) * 4),
                      1.6 + _h(i, 5, 61) * 0.8, int(_h(i, 4, 61) * 180)))
    prisms(t, PLAGIOCLASE, spots, 61, rim=0.18)
    return t


@tex("block/asteroid_stone")
def asteroid_stone(_):
    """Хондрит: тёмная матрица, округлые хондры со светлой кромкой и зёрна металла (Fe-Ni)."""
    t = Tex()
    h = field(71, [(16, 16, 0.4), (4, 4, 0.35), (2, 2, 0.25)])
    relief(t, ASTEROID, h, cuts=(0.22, 0.44, 0.64, 0.86), emboss=0.5)
    ch = [(5, 6, 2.6), (19, 4, 1.8), (27, 15, 2.4), (11, 20, 2.2), (22, 26, 1.6), (3, 28, 1.4), (15, 12, 1.3)]
    nuggets(t, CHONDRULE, [(x, y, r) for x, y, r in ch], 71, rim=0.25)
    for i, (mx, my) in enumerate([(9, 11), (24, 9), (17, 29), (30, 23), (6, 16), (26, 31)]):
        wput(t, mx, my, GALENA[3])
        wput(t, mx + 1, my, GALENA[2] if i % 2 else GALENA[4])
    # голубые зёрна оливина
    for (x, y) in [(14, 6), (29, 3), (8, 26)]:
        wput(t, x, y, (0x7C, 0x9A, 0x8C))
    return t


@tex("block/oil_shale")
def oil_shale(_):
    """Горючий сланец: тонкая горизонтальная слоистость, чёрные прослои керогена."""
    t = Tex()
    h = field(81, [(32, 32, 0.3), (32, 4, 0.45), (8, 2, 0.25)])
    relief(t, SHALE, h, cuts=(0.22, 0.44, 0.64, 0.86), emboss=0.4)
    for band in (5, 13, 22, 28):
        for x in range(N):
            y = band + int(round(2.2 * math.sin(2 * math.pi * x / N + band)))
            if _h(x // 3, band, 81) < 0.82:
                wput(t, x, y, KEROGEN[2 if _h(x, band, 82) > 0.4 else 1])
                if _h(x // 2, band, 83) < 0.5:
                    wput(t, x, y + 1, KEROGEN[3])
                wput(t, x, y - 1, shade(SHALE[3], 0.05))
    return t


@tex("block/sintered_regolith")
def sintered_regolith(_):
    """Спечённый реголит: оплавленная стекловидная масса, пузыри-поры и голубоватый стеклянный блеск."""
    t = Tex()
    h = field(91, [(16, 16, 0.45), (8, 8, 0.35), (2, 2, 0.2)])
    relief(t, SINTER, h, cuts=(0.2, 0.42, 0.64, 0.86), emboss=0.7)
    pores = [(4, 5, 1.6), (15, 3, 1.1), (25, 8, 1.9), (9, 15, 1.2), (20, 18, 1.5), (29, 22, 1.0),
             (5, 26, 1.8), (16, 28, 1.1), (26, 29, 1.3), (12, 9, 0.8)]
    for (cx, cy, r) in pores:
        R = int(r) + 1
        for yy in range(-R, R + 1):
            for xx in range(-R, R + 1):
                d = math.hypot(xx, yy)
                if d <= r:
                    wput(t, cx + xx, cy + yy, shade(SINTER[0], -0.3) if xx + yy < 1 else SINTER[0])
                elif d <= r + 1:
                    # кромка поры: утопленное — светлая низ-право, тёмная верх-лево
                    if xx + yy > 0:
                        wput(t, cx + xx, cy + yy, SINTER[4])
    return t


@tex("block/moon_ice")
def moon_ice(_):
    """Лёд лунных полярных кратеров: голубоватая толща, пузырьки воздуха и трещины с бликом."""
    t = Tex()
    h = field(101, [(32, 32, 0.4), (16, 16, 0.35), (8, 8, 0.25)])
    relief(t, ICE, h, cuts=(0.18, 0.4, 0.64, 0.88), emboss=0.0)
    # трещины-плоскости: тёмная линия + светлая сторона к свету, с переносом через край
    for (x0, y0, dx, dy, ln) in [(2, 30, 1, -1, 22), (18, 10, 1, 0.5, 14), (6, 8, 0.5, 1, 9)]:
        for s in range(ln):
            x, y = x0 + dx * s, y0 + dy * s
            wput(t, x, y, ICE[0])
            wput(t, x - 1, y, ICE[4])
    for (bx, by) in [(8, 20), (24, 25), (13, 4), (28, 17), (4, 13), (21, 30)]:
        wput(t, bx, by, ICE[4])
        wput(t, bx + 1, by + 1, ICE[1])
    return t


@tex("block/mars_ice")
def mars_ice(_):
    """Марсианский ледяной пласт в песчанике: линзы грязного льда под ржавой пылью."""
    t = red_sandstone_base(111)
    lens = field(113, [(16, 8, 0.6), (8, 4, 0.4)])
    for y in range(N):
        for x in range(N):
            v = lens[y][x]
            if v > 0.62:
                k = 2 + (1 if v > 0.78 else 0) + (1 if v > 0.9 else 0)
                t.put(x, y, mix(ICE[min(4, k)], MARS_DUST[2], 0.06))
            elif v > 0.56:
                t.put(x, y, mix(ICE[0], MARS_DUST[1], 0.45))
    # кромки линз: свет сверху-слева
    for y in range(N):
        for x in range(N):
            a, b = lens[y][x], lens[(y - 1) % N][(x - 1) % N]
            if a > 0.62 >= b:
                t.put(x, y, ICE[4])
            elif b > 0.62 >= a:
                t.darken(x, y, 0.2)
    return t


# ------------------------------------------------------------------ руды
def _spots(seed, n, margin=3):
    """Центры вкраплений: равномернее, чем чистый случай (по сетке с дрожанием)."""
    out = []
    g = int(math.ceil(math.sqrt(n)))
    cells = [(i, j) for j in range(g) for i in range(g)]
    cells.sort(key=lambda c: _h(c[0], c[1], seed))
    for (i, j) in cells[:n]:
        step = N / g
        out.append((int(i * step + margin + _h(i, j, seed + 1) * (step - 2 * margin)),
                    int(j * step + margin + _h(i, j, seed + 2) * (step - 2 * margin))))
    return out


@tex("block/beryl_ore")
def beryl_ore(_):
    """Берилл: шестигранные призмы цвета морской волны в камне (пегматит)."""
    t = stone_base(STONE, 121)
    sp = _spots(121, 5)
    prisms(t, BERYL, [(x, y, 6 + int(_h(x, y, 1) * 3), 3, 60 + int(_h(x, y, 2) * 60)) for x, y in sp], 121)
    return t


@tex("block/borax_ore")
def borax_ore(_):
    """Бура: белые призмы в корке эвапорита на камне."""
    t = stone_base(STONE, 131)
    sp = _spots(131, 5)
    halo(t, EVAPORITE, sp, 5, 131, 0.7)
    prisms(t, BORAX, [(x, y, 4 + int(_h(x, y, 3) * 3), 2.4, int(_h(x, y, 4) * 180)) for x, y in sp], 131, rim=0.22)
    return t


@tex("block/fluorite_ore")
def fluorite_ore(_):
    """Флюорит: фиолетовые кубы (спайность по октаэдру, габитус — куб)."""
    t = stone_base(STONE, 141)
    sp = _spots(141, 6)
    cubes(t, FLUORITE, [(x, y, 3 + int(_h(x, y, 5) * 3)) for x, y in sp])
    return t


@tex("block/galena_ore")
def galena_ore(_):
    """Галенит PbS: свинцово-серые кубы с металлическим бликом."""
    t = stone_base(STONE, 151)
    sp = _spots(151, 6)
    cubes(t, GALENA, [(x, y, 3 + int(_h(x, y, 6) * 2)) for x, y in sp], rim=0.35)
    return t


@tex("block/halite_ore")
def halite_ore(_):
    """Каменная соль: розовато-белые кубы галита сростками."""
    t = stone_base(STONE, 161)
    sp = _spots(161, 5)
    cl = []
    for x, y in sp:
        cl.append((x, y, 4))
        cl.append((x + 4, y + 3, 3))
    cubes(t, HALITE, cl, rim=0.25)
    return t


@tex("block/spodumene_ore")
def spodumene_ore(_):
    """Сподумен (кунцит): розово-сиреневые длинные призмы в пегматите."""
    t = stone_base(STONE, 171)
    sp = _spots(171, 4)
    prisms(t, SPODUMENE, [(x, y, 8 + int(_h(x, y, 7) * 3), 3.2, 20 + int(_h(x, y, 8) * 50)) for x, y in sp], 171)
    return t


def _ilmenite(t, seed, n=6):
    sp = _spots(seed, n)
    prisms(t, ILMENITE, [(x, y, 5 + int(_h(x, y, 9) * 4), 3, int(_h(x, y, 10) * 180)) for x, y in sp], seed,
           rim=0.35)


@tex("block/titanium_ore")
def titanium_ore(_):
    """Ильменит FeTiO₃: чёрные с металлическим блеском пластинки в камне."""
    t = stone_base(STONE, 181)
    _ilmenite(t, 181)
    return t


@tex("block/deepslate_titanium_ore")
def deepslate_titanium_ore(_):
    t = deepslate_base(DEEPSLATE, 191)
    _ilmenite(t, 191)
    return t


@tex("block/moon_titanium_ore")
def moon_titanium_ore(_):
    """Лунный базальт, богатый ильменитом (моря): те же пластинки на лунном камне."""
    t = moon_stone_base(201)
    _ilmenite(t, 201, 7)
    return t


def _wolframite(t, seed, n=5):
    sp = _spots(seed, n)
    prisms(t, WOLFRAMITE, [(x, y, 6 + int(_h(x, y, 11) * 3), 3, 70 + int(_h(x, y, 12) * 40)) for x, y in sp], seed,
           rim=0.3)


@tex("block/deepslate_tungsten_ore")
def deepslate_tungsten_ore(_):
    """Вольфрамит: тёмные таблитчатые кристаллы с синевато-стальным бликом."""
    t = deepslate_base(DEEPSLATE, 211)
    _wolframite(t, 211)
    return t


@tex("block/mars_tungsten_ore")
def mars_tungsten_ore(_):
    """Вольфрамит в марсианском песчанике: кристаллы в ореоле окисленной породы."""
    t = red_sandstone_base(221)
    sp = _spots(221, 5)
    halo(t, MARS_DUST, sp, 4, 221, 0.35)
    for (x, y) in sp:
        halo(t, [shade(c, -0.35) for c in RED_SANDSTONE], [(x + 1, y + 1)], 3, 222, 0.4)
    prisms(t, WOLFRAMITE, [(x, y, 6 + int(_h(x, y, 11) * 3), 3, 70 + int(_h(x, y, 12) * 40)) for x, y in sp], 221,
           rim=0.3)
    return t


@tex("block/uraninite_ore")
def uraninite_ore(_):
    """Настуран (уранинит): чёрные почковидные массы с жёлто-зелёной коркой вторичных урановых слюдок."""
    t = deepslate_base(DEEPSLATE, 231)
    sp = _spots(231, 5)
    halo(t, AUTUNITE, sp, 5, 231, 0.8)
    nuggets(t, PITCHBLENDE, [(x, y, 2.2 + _h(x, y, 13) * 1.2) for x, y in sp], 231, rim=0.3)
    for i, (x, y) in enumerate(sp):
        # чешуйки отенита на кромке
        wput(t, x + 3, y - 1, AUTUNITE[4])
        wput(t, x - 2, y + 3, AUTUNITE[3])
    return t


# ------------------------------------------------------------------ жидкое топливо
STILL_FRAMES = 32
FLOW_FRAMES = 16


def _liquid_still(pal, seed):
    """Стоячая жидкость: два поля ряби дрейфуют навстречу (сдвиг на целые px, за цикл — ровно 32 px),
    их сумма даёт бегущие блики. Цикл замкнут, края бесшовны."""
    frames = []
    for f in range(STILL_FRAMES):
        a = field(seed, [(16, 16, 0.6), (8, 8, 0.4)], dx=f, dy=0)
        b = field(seed + 1, [(16, 16, 0.6), (8, 8, 0.4)], dx=0, dy=-f)
        t = Tex()
        for y in range(N):
            for x in range(N):
                v = 0.5 * (a[y][x] + b[y][x])
                # гребни ряби — там, где поля почти равны
                crest = 1 - abs(a[y][x] - b[y][x]) * 3.2
                k = 1 + (1 if v > 0.42 else 0) + (1 if v > 0.62 else 0)
                c = pal[k]
                if crest > 0.72:
                    c = mix(c, pal[4], 0.55 if crest > 0.88 else 0.3)
                t.put(x, y, c)
        frames.append(t)
    return frames


def _liquid_flow(pal, seed):
    """Текущая жидкость: вытянутые вдоль течения струи. Кадр 64×64 — игра кладёт на грань его четверть,
    как у ванильной воды, поэтому плотность детали та же, что у стоячей (32 px на блок). Сдвиг вниз на 4 px
    за кадр: цикл 16 кадров = 64 px, скорость течения прежняя (2 блока за 32 тика)."""
    n = 2 * N
    frames = []
    for f in range(FLOW_FRAMES):
        h = field(seed, [(8, 32, 0.45), (4, 16, 0.35), (2, 8, 0.2)], dx=0, dy=-4 * f, n=n)
        t = Tex(n=n)
        for y in range(n):
            for x in range(n):
                v = h[y][x]
                k = 1 + (1 if v > 0.38 else 0) + (1 if v > 0.64 else 0)
                c = pal[k]
                if v > 0.86:
                    c = mix(pal[3], pal[4], 0.6)
                elif v < 0.12:
                    c = mix(pal[0], pal[1], 0.5)
                t.put(x, y, c)
        frames.append(t)
    return frames


@tex("block/hydrolox_still")
def hydrolox_still(_):
    return _liquid_still(HYDROLOX, 301)


@tex("block/hydrolox_flow")
def hydrolox_flow(_):
    return _liquid_flow(HYDROLOX, 311)


@tex("block/kerolox_still")
def kerolox_still(_):
    return _liquid_still(KEROLOX, 321)


@tex("block/kerolox_flow")
def kerolox_flow(_):
    return _liquid_flow(KEROLOX, 331)


@tex("block/methalox_still")
def methalox_still(_):
    return _liquid_still(METHALOX, 341)


@tex("block/methalox_flow")
def methalox_flow(_):
    return _liquid_flow(METHALOX, 351)
