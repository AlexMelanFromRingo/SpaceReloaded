"""Предметы-материалы: слитки, пластины, пыли и смеси, минералы и кристаллы, химикаты в таре,
газовые баллоны, пластины кремния, волокна, стержни (012). Семейства рисуются общими функциями,
вещества различаются палитрой; цвета — от прежних текстур 16×16."""
import math

from . import tex
from .kit import (BRASS, COPPER, DARK_STEEL, O2_BLUE, RUBBER, STEEL, WOOD, Tex, _h, mix, shade)

OUT = (0x14, 0x16, 0x1A)


def pal(c, cool=(0x1C, 0x1A, 0x2E), warm=(0xFF, 0xFA, 0xEC)):
    """Палитра из 5 ступеней от основного цвета: тени уходят в холодный тёмный, блики — в тёплый белый."""
    return [mix(c, cool, 0.55), mix(c, cool, 0.28), tuple(c), mix(c, warm, 0.3), mix(c, warm, 0.62)]


def H(c):
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def finish(t):
    t.outline(OUT)
    return t


# ================================================================== слитки и пластины
def ingot(p, seed=17):
    """Слиток: верхняя грань — трапеция (уклон литейной формы), передняя грань сужается книзу, клеймо плавки."""
    t = Tex()
    top, mid, bot = 8, 17, 23
    for y in range(top, mid):
        k = (y - top) / (mid - top - 1)
        x0, x1 = round(10 - 5 * k), round(21 + 5 * k)
        for x in range(x0, x1 + 1):
            c = p[3]
            if _h(x // 3, y, seed) > 0.82:
                c = mix(p[3], p[4], 0.5)
            t.put(x, y, c)
        t.put(x0, y, p[4])
        t.put(x1, y, p[2])
    t.hline(10, 21, top, p[4])
    for y in range(mid, bot + 1):
        k = (y - mid) / (bot - mid)
        x0, x1 = round(5 + 2 * k), round(26 - 2 * k)
        for x in range(x0, x1 + 1):
            u = (x - x0) / max(1, x1 - x0)
            c = p[2] if u < 0.78 else p[1]
            if _h(x // 4, y, seed + 1) > 0.86:
                c = mix(c, p[3], 0.4)
            t.put(x, y, c)
        t.put(x0, y, p[3])
    t.hline(5, 26, mid, p[4])
    t.hline(7, 24, bot, p[0])
    # клеймо плавки на верхней грани
    t.hline(14, 17, 11, p[1])
    t.hline(15, 18, 12, p[4])
    t.put(6, mid, (255, 255, 255))
    return finish(t)


def plate(p, seed=31):
    """Пластина в изометрии: ромб верхней грани с шлифовкой, толщина 2 px, отверстия по углам."""
    t = Tex()
    cx, cy, a, b = 16, 14, 14, 7
    th = 3
    for y in range(0, 32):
        for x in range(0, 32):
            dx, dy = abs(x + 0.5 - cx) / a, abs(y + 0.5 - cy) / b
            if dx + dy <= 1:
                # шлифовка вдоль диагонали, мягкий градиент света
                g = (x - y) * 0.5
                c = p[3] if (x + 0.5 - cx) / a + (y + 0.5 - cy) / b < 0 else p[2]
                if _h(int(x + y * 2) // 2, 0, seed) > 0.78:
                    c = mix(c, p[4], 0.45)
                t.put(x, y, c)
    # толщина: нижние кромки ромба сдвинуты вниз
    for x in range(cx - a, cx + a + 1):
        for y in range(31, -1, -1):
            if t.get(x, y)[3] and y >= cy:
                for k in range(1, th):
                    t.put(x, y + k, p[1] if x < cx else p[0])
                break
    # верхние кромки — блик
    for x in range(cx - a, cx + a + 1):
        for y in range(0, 32):
            if t.get(x, y)[3]:
                t.put(x, y, p[4])
                break
    for hx, hy in ((cx - 9, cy), (cx + 9, cy), (cx, cy - 4), (cx, cy + 4)):
        t.put(hx, hy, p[0])
        t.put(hx + 1, hy, p[1])
    return finish(t)


# ================================================================== пыли и смеси
def heap(p, p2=None, frac=0.38, seed=41, coarse=False):
    """Кучка порошка: округлый конус, свет слева, зернистость по пикселю; смесь — вкрапления второй палитры."""
    t = Tex()
    top, bot, cx = 8, 26, 15.5
    for y in range(top, bot + 1):
        h = (y - top + 0.8) / (bot - top)
        w = 2.5 + 11.5 * h ** 0.55
        if y >= bot - 1:
            w -= (y - bot + 2) * 1.2
        for x in range(0, 32):
            s = (x + 0.5 - cx) / w
            if abs(s) > 1:
                continue
            # грубое затенение как у конуса: левая сторона и макушка светлее
            k = 2
            if s < -0.3 or (y < top + 4 and s < 0.3):
                k = 3
            if s > 0.45 or y >= bot - 1:
                k = 1
            if s < -0.75 and y > top + 5:
                k = 2
            hh = _h(x // (2 if coarse else 1), y // (2 if coarse else 1), seed)
            if hh > 0.86:
                k += 1
            elif hh < 0.12:
                k -= 1
            pp = p
            if p2 is not None and _h(x, y, seed + 7) < frac:
                pp = p2
            t.put(x, y, pp[max(0, min(4, k))])
    t.put(15, top, p[4])
    t.put(16, top, p[3])
    return finish(t)


# ================================================================== минералы и кристаллы
def lump(p, seed=51, R=11.5, cx=16, cy=17, t=None, rough=0.22, pores=0.0, flecks=None, sheen=False):
    """Гранёный комок: неровный силуэт, грани — ячейки Вороного, яркость грани по её «нормали» к свету."""
    own = t is None
    t = t or Tex()
    rnd = lambda i: _h(i, seed, 3)
    seeds = [(cx + (rnd(i) - 0.5) * R * 1.6, cy + (rnd(i + 20) - 0.5) * R * 1.3) for i in range(9)]
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            ang = math.atan2(dy, dx)
            b = int((ang + math.pi) / (2 * math.pi) * 7)
            f = ((ang + math.pi) / (2 * math.pi) * 7) - b
            r = R * (1 + rough * ((1 - f) * (_h(b % 7, 2, seed) - 0.5) + f * (_h((b + 1) % 7, 2, seed) - 0.5)))
            ry = r * (0.86 if dy > 0 else 0.8)
            if math.hypot(dx, dy * r / ry) > r:
                continue
            i = min(range(len(seeds)), key=lambda j: (x - seeds[j][0]) ** 2 + (y - seeds[j][1]) ** 2)
            sx, sy = seeds[i][0] - cx, seeds[i][1] - cy
            light = -(sx + sy) / (R * 1.2) + (rnd(i + 40) - 0.5) * 0.7
            k = 2 + (1 if light > 0.25 else -1 if light < -0.3 else 0)
            # рёбра граней
            d1 = sorted((x - q[0]) ** 2 + (y - q[1]) ** 2 for q in seeds)
            if math.sqrt(d1[1]) - math.sqrt(d1[0]) < 0.7:
                k = k + 1 if light > 0 else k - 1
            if math.hypot(dx, dy * r / ry) > r - 1.1:
                k = 0 if not own else k - 1
            c = p[max(0, min(4, k))]
            if pores and _h(x // 2, y // 2, seed + 9) < pores:
                c = p[0]
                if _h(x, y, seed + 9) > 0.5:
                    c = mix(p[0], (0, 0, 0), 0.3)
            if flecks and _h(x, y, seed + 13) > 0.9:
                c = flecks[3] if light > -0.2 else flecks[2]
            t.put(x, y, c)
    if sheen:
        for x, y in ((cx - 5, cy - 6), (cx - 4, cy - 6), (cx - 6, cy - 5)):
            t.put(x, y, p[4])
    return finish(t) if own else t


def lumps(p, seed=51, **kw):
    """Горсть кусков, как ванильное «сырьё»: большой кусок спереди, два поменьше сзади."""
    t = Tex()
    lump(p, seed + 1, R=7, cx=21, cy=13, t=t, **kw)
    lump(p, seed + 2, R=6.5, cx=10, cy=14, t=t, **kw)
    lump(p, seed, R=10, cx=15, cy=19, t=t, **kw)
    return finish(t)


def crystal(t, bx, by, ang, L, w, p, tip=1.7):
    """Шестигранная призма с пирамидальной вершиной: ось из (bx, by) под углом ang (0 — вверх)."""
    ux, uy = math.sin(ang), -math.cos(ang)
    tl = w * tip
    for y in range(32):
        for x in range(32):
            rx, ry = x + 0.5 - bx, y + 0.5 - by
            a = rx * ux + ry * uy
            s = rx * -uy + ry * ux
            if a < 0 or a > L:
                continue
            lim = w if a <= L - tl else w * (L - a) / tl
            if abs(s) > lim:
                continue
            # грани призмы: левая (к свету) светлая, средняя, правая тёмная
            k = 3 if s < -w * 0.34 else (1 if s > w * 0.34 else 2)
            if a > L - tl:
                k = min(4, k + 1)
            if abs(abs(s) - w * 0.34) < 0.5:
                k = min(4, k + 1) if s < 0 else k
            if abs(s) > lim - 0.9 or a < 1:
                k = 0
            t.put(x, y, p[k])


def cube(t, cx, cy, s, p):
    """Изометрический куб (кубическая сингония): верх — ромб, левая грань светлая, правая тёмная, тёмные рёбра
    по силуэту, чтобы соседние кубы читались раздельно."""
    m = {}
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if abs(dx) / s + abs(dy) / (s / 2) <= 1:
                m[(x, y)] = p[4] if dy < -s / 5 and dx < s / 3 else p[3]
            elif abs(dx) <= s and 0 < dy <= s + s / 2 - abs(dx) / 2 and dy >= -s / 2 + abs(dx) / 2:
                m[(x, y)] = p[2] if dx < 0 else p[1]
    for (x, y), c in m.items():
        edge = any((x + dx, y + dy) not in m for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
        t.put(x, y, p[0] if edge and (y > cy or x > cx) else (p[4] if edge else c))
    t.vline(int(cx), int(cy + s / 2) + 1, int(cy + s * 1.5) - 1, p[0])


def cubes(p, seed=61):
    t = Tex()
    cube(t, 21, 8, 7, p)
    cube(t, 10, 11, 7, p)
    cube(t, 19, 17, 7, p)
    return finish(t)


def cluster(p, spec, seed=71):
    t = Tex()
    for bx, by, ang, L, w in spec:
        crystal(t, bx, by, ang, L, w, p)
    return finish(t)


CLUSTER = [(11, 29, -0.5, 18, 3.4), (22, 29, 0.55, 17, 3.2), (16, 30, 0.05, 25, 4.2), (9, 30, -1.0, 11, 2.6), (24, 30, 1.0, 10, 2.4)]


# ================================================================== тара
GLASS_P = pal((0xB4, 0xCC, 0xDC))
HDPE = pal((0xE6, 0xE6, 0xDE))


def bottle(liquid=None, body=None, cap=None, label=None, level=0.62, amber=False):
    """Реактивная бутыль: пробка/крышка, горло, плечи, корпус с этикеткой. liquid — видимая жидкость в стекле;
    body — непрозрачный пластик (HDPE/PTFE), тогда жидкость не видна."""
    t = Tex()
    cap = cap or RUBBER
    glass = pal((0x9C, 0x6A, 0x2E)) if amber else GLASS_P
    x0, x1, y0, y1 = 9, 22, 12, 29
    for y in range(4, y1 + 1):
        if y < 8:
            a, b = 12, 19
        elif y < 10:
            a, b = 13, 18
        elif y < y0:
            a, b = 11 - (y - 10), 20 + (y - 10)
        else:
            a, b = x0, x1
            if y == y1:
                a, b = x0 + 1, x1 - 1
        for x in range(a, b + 1):
            u = (x - a) / max(1, b - a)
            if y < 8:
                k = 3 if u < 0.3 else (1 if u > 0.75 else 2)
                t.put(x, y, cap[k] if cap is not RUBBER else cap[k + 1 if k < 4 else 4])
                continue
            if body is not None:
                k = 4 if u < 0.15 else 3 if u < 0.4 else (1 if u > 0.8 else 2)
                t.put(x, y, body[k])
                continue
            lvl = y1 - (y1 - y0) * level
            if liquid is not None and y >= lvl:
                k = 3 if u < 0.25 else (1 if u > 0.78 else 2)
                if y < lvl + 1:
                    k = 4
                t.put(x, y, liquid[k])
            else:
                k = 3 if u < 0.3 else (1 if u > 0.8 else 2)
                t.put(x, y, glass[k])
    # блик стекла слева
    if body is None:
        t.vline(x0 + 2, y0 + 1, y1 - 3, (0xF4, 0xFA, 0xFF))
        t.put(x0 + 2, y0 - 1, glass[4])
    t.hline(12, 19, 7, (0x10, 0x10, 0x12))
    if label is not None:
        ly0, ly1 = 17, 24
        for y in range(ly0, ly1 + 1):
            for x in range(x0 + 1, x1):
                t.put(x, y, (0xF2, 0xEE, 0xE2) if x < x1 - 2 else (0xC8, 0xC2, 0xB4))
        t.rect(x0 + 1, ly0, x1 - 1, ly0 + 2, label[2])
        t.hline(x0 + 1, x1 - 1, ly0, label[3])
        t.hline(x0 + 3, x1 - 5, ly0 + 4, (0x60, 0x60, 0x66))
        t.hline(x0 + 3, x1 - 7, ly0 + 6, (0x8A, 0x8A, 0x90))
    return finish(t)


def flask(liquid, level=0.55):
    """Круглодонная колба (рассол): корковая пробка, горло, шар с жидкостью и мениском."""
    t = Tex()
    cx, cy, r = 16, 20, 9.5
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            inside = d <= r or (13 <= x <= 18 and 6 <= y <= cy)
            if not inside:
                continue
            u = dx / r
            if y >= cy - r + 2 * r * (1 - level) and d <= r - 0.8:
                k = 3 if u < -0.45 else (1 if u > 0.5 else 2)
                if abs(y - (cy - r + 2 * r * (1 - level))) < 1:
                    k = 4
                t.put(x, y, liquid[k])
            else:
                k = 3 if u < -0.4 else (1 if u > 0.5 else 2)
                if d > r - 1:
                    k = 1
                t.put(x, y, GLASS_P[k])
    t.rect(13, 3, 18, 6, WOOD[3])
    t.hline(13, 18, 3, WOOD[4])
    t.vline(18, 3, 6, WOOD[1])
    t.rect(12, 7, 19, 7, GLASS_P[3])
    for x, y in ((10, 17), (10, 18), (11, 16), (12, 15)):
        t.put(x, y, (0xF8, 0xFC, 0xFF))
    return finish(t)


def cylinder(body, band=None, band2=None, valve=BRASS, y0=8, seed=81):
    """Газовый баллон: вертикальный цилиндр со сферическим верхом, вентиль с маховичком,
    окраска корпуса и кольцевая полоса по маркировке газа."""
    t = Tex()
    x0, x1, y1 = 10, 21, 29
    w = x1 - x0
    for y in range(y0, y1 + 1):
        dome = y - y0
        if dome < 3:
            a, b = x0 + (3 - dome), x1 - (3 - dome)
        else:
            a, b = x0, x1
        for x in range(a, b + 1):
            u = (x - x0) / w
            k = 4 if 0.12 < u < 0.26 else 3 if u < 0.42 else (1 if u > 0.8 else 2)
            if u <= 0.05:
                k = 2
            p = body
            if band is not None and 14 <= y <= 17:
                p = band
            if band2 is not None and 19 <= y <= 20:
                p = band2
            t.put(x, y, p[k])
    t.hline(x0 + 1, x1 - 1, y1, body[0])
    # горловина и вентиль
    t.rect(14, 5, 17, 7, valve[2])
    t.put(14, 5, valve[4])
    t.vline(17, 5, 7, valve[1])
    t.rect(13, 3, 18, 4, valve[3])
    t.hline(13, 18, 3, valve[4])
    t.rect(19, 5, 21, 6, valve[2])
    t.put(21, 6, valve[1])
    # маховичок вентиля
    t.hline(12, 19, 2, DARK_STEEL[3])
    return finish(t)


def drum(body, band=None, seed=91):
    """Горизонтальный транспортный цилиндр UF₆ (типы 30B/48Y): рёбра-юбки на концах, клапан сверху."""
    t = Tex()
    x0, x1, yc, r = 3, 28, 17, 8
    for y in range(yc - r, yc + r + 1):
        v = (y - (yc - r)) / (2 * r)
        k = 4 if 0.14 < v < 0.28 else 3 if v < 0.42 else (1 if v > 0.78 else 2)
        for x in range(x0, x1 + 1):
            e = min(x - x0, x1 - x)
            if e < 2 and abs(y - yc) > r - 2 + e:
                continue
            p = body
            if band is not None and 13 <= x <= 16:
                p = band
            kk = k
            if x in (x0 + 3, x1 - 3):
                kk = max(0, k - 1)
            if x in (x0 + 2, x1 - 4):
                kk = min(4, k + 1)
            t.put(x, y, p[kk])
    # юбки
    for x in (x0, x0 + 1, x1 - 1, x1):
        t.vline(x, yc - r + 1, yc + r - 1, body[1] if x > 16 else body[2])
    t.rect(22, yc - r - 2, 24, yc - r - 1, BRASS[2])
    t.put(22, yc - r - 2, BRASS[4])
    # опорные салазки
    t.rect(7, yc + r + 1, 10, yc + r + 2, DARK_STEEL[2])
    t.rect(21, yc + r + 1, 24, yc + r + 2, DARK_STEEL[2])
    return finish(t)


# ================================================================== пластины кремния
def wafer(p, glint, square=False, seed=101, grains=None):
    """Пластина: диск с базовым срезом (или псевдоквадрат мультикремния), торец 1 px, зеркальный блик по диагонали."""
    t = Tex()
    cx, cy, r = 16, 15, 12.5
    cells = [(3 + 26 * _h(i, 1, seed), 3 + 26 * _h(i, 2, seed)) for i in range(14)]
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if square:
                inside = abs(dx) <= 12 and abs(dy) <= 12 and abs(dx) + abs(dy) <= 21
            else:
                inside = math.hypot(dx, dy) <= r and dy <= 11
            if not inside:
                continue
            k = 2
            diag = (dx + dy) / 2
            if -6 < diag + 3 < -2:
                k = 3
            if -4 < diag + 3 < -3:
                k = 4
            if 5 < diag < 7:
                k = 3
            c = p[k]
            if grains is not None:
                i = min(range(len(cells)), key=lambda j: (x - cells[j][0]) ** 2 + (y - cells[j][1]) ** 2)
                c = grains[1 + int(_h(i, 5, seed) * 3)]
                if -6 < diag + 3 < -2:
                    c = mix(c, grains[4], 0.35)
            t.put(x, y, c)
    # торец
    for x in range(32):
        for y in range(31, -1, -1):
            if t.get(x, y)[3]:
                t.put(x, y + 1, p[0])
                break
    for x, y in glint:
        t.put(x, y, (0xFF, 0xFF, 0xFF))
    return finish(t)


# ================================================================== волокна
def cloth(p, twill=True, seed=111):
    """Ткань: квадратный отрез с загнутым углом, саржевое (или полотняное) переплетение 2×2."""
    t = Tex()
    for y in range(4, 28):
        for x in range(4, 28):
            if x - 4 + (27 - y) > 42:
                continue
            if twill:
                ph = ((x + y) // 2) % 2
                c = p[3] if ph == 0 else p[1]
                if (x + y) % 2 == 0:
                    c = mix(c, p[2], 0.5)
            else:
                ph = (x // 2 + y // 2) % 2
                c = p[3] if ph == 0 else p[1]
                if (x + y) % 2 == 1:
                    c = p[4] if ph == 0 else p[2]
            t.put(x, y, c)
    # загнутый угол справа-внизу
    for i in range(6):
        for j in range(i + 1):
            t.put(27 - 5 + i, 27 - j, p[4] if j == i else p[2])
    t.hline(4, 26, 4, p[4])
    t.vline(4, 4, 27, p[4])
    return finish(t)


# ================================================================== стержни
def rod(t, x0, y0, x1, y1, r, p, caps=True):
    """Цилиндр между точками: цилиндрическое освещение, светлый верхний торец."""
    L = math.hypot(x1 - x0, y1 - y0)
    ux, uy = (x1 - x0) / L, (y1 - y0) / L
    for y in range(32):
        for x in range(32):
            rx, ry = x + 0.5 - x0, y + 0.5 - y0
            a = rx * ux + ry * uy
            s = -rx * uy + ry * ux
            # s < 0 — сторона к свету при оси «вниз-влево → вверх-вправо»
            if -0.5 <= a <= L + 0.5 and abs(s) <= r:
                v = s / r
                sgn = 1 if (-uy) + ux < 0 else -1
                v *= sgn
                k = 4 if -0.75 < v < -0.4 else 3 if v < -0.1 else (1 if v > 0.55 else 2)
                if abs(s) > r - 0.7:
                    k = max(0, k - 1)
                if caps and a > L - 0.8:
                    k = 3
                t.put(x, y, p[k])


# ================================================================== цвета веществ
C = {n: pal(H(c)) for n, c in {
    "aluminium": "c8ccd4", "al_li": "c6dae6", "al_cu": "e2c4a4", "lithium": "e4e2d4", "nickel": "c0bb98",
    "superalloy": "8a8e98", "steel": "9ba3ad", "titanium": "c4ccd8", "ti_alloy": "b4c2d8", "tungsten": "5a6070",
    "copper": "c87844", "zirconium": "8a8e95",
}.items()}
WHITE = pal(H("d8d8d2"))
SNOW = pal(H("e4e8ec"))


@tex("item/aluminium_ingot")
def _(n): return ingot(C["aluminium"])


@tex("item/aluminium_lithium_ingot")
def _(n): return ingot(C["al_li"])


@tex("item/aluminium_copper_ingot")
def _(n): return ingot(C["al_cu"])


@tex("item/lithium_ingot")
def _(n): return ingot(C["lithium"])


@tex("item/nickel_ingot")
def _(n): return ingot(C["nickel"])


@tex("item/nickel_superalloy_ingot")
def _(n): return ingot(C["superalloy"])


@tex("item/steel_ingot")
def _(n): return ingot(C["steel"])


@tex("item/titanium_ingot")
def _(n): return ingot(C["titanium"])


@tex("item/titanium_alloy_ingot")
def _(n): return ingot(C["ti_alloy"])


@tex("item/tungsten_ingot")
def _(n): return ingot(C["tungsten"])


@tex("item/copper_plate")
def _(n): return plate(COPPER)


@tex("item/steel_plate")
def _(n): return plate(C["steel"])


@tex("item/titanium_alloy_plate")
def _(n): return plate(C["ti_alloy"])


# --- пыли (цвет вещества)
DUST = {
    "alumina": "e8e8e4", "iron_dust": "b89e84", "silicon_dust": "7c8498", "titanium_dust": "a6aebc",
    "tungsten_dust": "4e5462", "coal_dust": "34343a", "lithium_chloride": "e6eaee", "lithium_hydroxide": "eef0f2",
    "gypsum": "ece6d6", "beta_spodumene": "e8c8d2", "boric_acid": "dcdcd6", "beryllium_hydroxide": "cfcfc8",
    "beryllium_oxide": "d8d8d2", "yellowcake": "c8ae10", "uranium_tetrafluoride": "3a8a30",
}
for _n, _c in DUST.items():
    tex("item/" + _n)(lambda n, _c=_c, _s=len(_n): heap(pal(H(_c)), seed=40 + _s,
                                                          coarse=n.endswith(("tetrafluoride", "yellowcake"))))

BLEND = {
    "al_cu_blend": ("d4d8e0", "c87848", 0.4), "al_li_blend": ("c6cad2", "ecead8", 0.45),
    "boron_carbide_blend": ("3c3c42", "9098a4", 0.3), "phosphate_blend": ("e6e0cc", "3a3a3e", 0.35),
    "silicon_blend": ("d8c49a", "34343a", 0.45), "steel_blend": ("7a7c84", "2e2e34", 0.35),
    "superalloy_blend": ("dfe2ea", "a8a48c", 0.4),
}
for _n, (_a, _b, _f) in BLEND.items():
    tex("item/" + _n)(lambda n, _a=_a, _b=_b, _f=_f: heap(pal(H(_a)), pal(H(_b)), _f, seed=60 + len(n)))


# --- минералы
@tex("item/galena")
def _(n): return cubes(pal(H("5a6272")))


@tex("item/rock_salt")
def _(n): return cubes(pal(H("e8d8d8")))


@tex("item/fluorite")
def _(n): return cubes(pal(H("8a56bc")))


@tex("item/cryolite")
def _(n): return cluster(pal(H("d0d0d6")), CLUSTER)


@tex("item/borax")
def _(n): return cluster(pal(H("dcdad2")), [(12, 29, -0.35, 20, 3.8), (21, 29, 0.45, 17, 3.6), (16, 30, 0.08, 14, 3.4)])


@tex("item/sapphire")
def _(n): return cluster(pal(H("3052c8")), CLUSTER)


@tex("item/beryl")
def _(n): return cluster(pal(H("3c9c7a")), [(15, 30, 0.35, 27, 5.0), (10, 29, -0.45, 15, 3.2)])


@tex("item/spodumene")
def _(n): return cluster(pal(H("b88ca2")), [(9, 28, 0.75, 26, 4.2)])


@tex("item/zircon")
def _(n): return cluster(pal(H("9a6038")), [(12, 30, -0.25, 26, 4.6), (22, 30, 0.45, 19, 4.0)])


@tex("item/polysilicon")
def _(n): return lumps(pal(H("8c98b2")), seed=5, rough=0.35, sheen=True)


@tex("item/metallurgical_silicon")
def _(n): return lumps(pal(H("6a7282")), seed=8, rough=0.3, sheen=True)


@tex("item/uraninite")
def _(n): return lump(pal(H("262220")), seed=11, sheen=True)


@tex("item/boron_carbide")
def _(n): return lumps(pal(H("24242c")), seed=14, rough=0.4, sheen=True)


@tex("item/slag")
def _(n): return lump(pal(H("5a524c")), seed=17, pores=0.16)


@tex("item/zirconium")
def _(n): return lump(pal(H("8a8e95")), seed=19, pores=0.12, rough=0.3)


@tex("item/meteoric_iron")
def _(n): return lump(pal(H("3a3432")), seed=23, flecks=pal(H("a8acb4")))


@tex("item/raw_titanium")
def _(n): return lumps(pal(H("a2aab8")), seed=29, rough=0.3, flecks=pal(H("dde2ea")))


@tex("item/raw_tungsten")
def _(n): return lumps(pal(H("4e5464")), seed=31, rough=0.3, flecks=pal(H("8a92a6")))


@tex("item/sodium")
def _(n):
    """Натрий: срез металла — светлый блестящий, на гранях белая корка гидроксида."""
    t = Tex()
    cube(t, 16, 12, 10, pal(H("c8ccd0")))
    for y in range(32):
        for x in range(32):
            c = t.get(x, y)
            if c[3] and _h(x, y, 7) > 0.8:
                t.put(x, y, mix(c[:3], (0xF2, 0xF2, 0xEE), 0.6))
    return finish(t)


@tex("item/zeolite")
def _(n):
    """Цеолит-молекулярное сито: гранулы-шарики."""
    t = Tex()
    p = pal(H("d4c492"))
    for cx, cy, r in ((11, 12, 4.2), (20, 11, 4.0), (15, 19, 4.5), (24, 19, 3.8), (8, 21, 3.8), (19, 26, 3.6), (11, 27, 3.2)):
        t.disc(cx, cy, r, p)
    return finish(t)


@tex("item/uranium_dioxide")
def _(n):
    """UO₂: спечённые топливные таблетки — короткие цилиндры с фаской."""
    t = Tex()
    p = pal(H("2c2a24"))
    for bx, by in ((6, 18), (14, 12), (20, 20)):
        for y in range(by, by + 9):
            for x in range(bx, bx + 8):
                u = (x - bx) / 7
                t.put(x, y, p[3] if u < 0.3 else p[1] if u > 0.75 else p[2])
        for x in range(bx, bx + 8):
            t.put(x, by, p[4])
            t.put(x, by + 1, p[3])
        t.put(bx, by, p[3])
    return finish(t)


@tex("item/phosphorus")
def _(n):
    """Белый фосфор — восковая палочка."""
    t = Tex()
    rod(t, 7, 26, 25, 7, 3.6, pal(H("d6cc96")))
    return finish(t)


# --- химикаты в таре
@tex("item/sulfuric_acid")
def _(n): return bottle(liquid=pal(H("d8c850")), label=pal(H("c83a2a")))


@tex("item/hydrofluoric_acid")
def _(n):
    """HF растворяет стекло — только в пластиковой (PTFE/ПЭ) бутыли."""
    return bottle(body=HDPE, cap=pal(H("3c9a4e")), label=pal(H("3c9a4e")))


@tex("item/photoresist")
def _(n):
    """Фоторезист светочувствителен — тёмное янтарное стекло."""
    return bottle(liquid=pal(H("d88a2e")), amber=True, label=pal(H("e0a030")), level=0.7)


@tex("item/epoxy_resin")
def _(n): return bottle(liquid=pal(H("c8a860")), label=pal(H("7a6a4a")), level=0.75)


@tex("item/caustic_soda")
def _(n):
    """NaOH разъедает стекло при хранении — пластиковая банка."""
    return bottle(body=HDPE, cap=pal(H("4a74b8")), label=pal(H("4a74b8")))


@tex("item/brine")
def _(n): return flask(pal(H("6c9cd0")))


# --- газы (ГОСТ 949 по возможности, узнаваемость прежних цветов)
GREY = pal(H("8a9098"))


@tex("item/oxygen_canister")
def _(n): return cylinder(O2_BLUE)


@tex("item/argon_canister")
def _(n): return cylinder(pal(H("3c8c5c")), band=GREY)


@tex("item/carbon_dioxide")
def _(n): return cylinder(GREY, band=pal(H("6ea0dc")))


@tex("item/carbon_monoxide")
def _(n): return cylinder(GREY, band=pal(H("d0c840")))


@tex("item/fluorine")
def _(n): return cylinder(pal(H("dcdc98")), band=pal(H("8a9098")))


@tex("item/hydrogen_chloride")
def _(n): return cylinder(pal(H("9aa890")), band=pal(H("40a040")))


@tex("item/trichlorosilane")
def _(n): return cylinder(pal(H("8c96b0")), band=pal(H("5a7ad8")))


@tex("item/uranium_hexafluoride")
def _(n): return drum(pal(H("e6e6e2")), band=pal(H("3c8cd8")))


@tex("item/depleted_uranium_hexafluoride")
def _(n): return drum(pal(H("8e9298")))


# --- пластины кремния и сапфира
@tex("item/silicon_wafer")
def _(n): return wafer(pal(H("7a82a0")), glint=((9, 8), (10, 7)))


@tex("item/sapphire_wafer")
def _(n): return wafer(pal(H("a4b8d0")), glint=((9, 8), (10, 7), (8, 9)))


@tex("item/multicrystalline_silicon")
def _(n):
    return wafer(pal(H("3a5090")), glint=((7, 7),), square=True, grains=pal(H("4262a8")))


# --- волокна
@tex("item/carbon_fiber")
def _(n): return cloth(pal(H("34343e")), twill=True)


@tex("item/glass_fiber")
def _(n): return cloth(pal(H("c8d6da")), twill=False)


@tex("item/straw")
def _(n):
    """Сноп соломы, перевязанный жгутом."""
    t = Tex()
    p = pal(H("d8bc62"))
    for i, off in enumerate(range(-6, 7, 2)):
        rod(t, 8 + off, 28, 22 + off, 4, 1.2, p if i % 2 else pal(H("c8a850")), caps=False)
    for y in range(14, 19):
        for x in range(8, 26):
            if t.get(x, y)[3]:
                t.put(x, y, WOOD[2] if y not in (14, 18) else WOOD[1])
    return finish(t)


# --- стержни и провод
@tex("item/graphite_electrode")
def _(n):
    """Графитовый электрод ДСП: цилиндр с резьбовым ниппельным гнездом на торце."""
    t = Tex()
    p = pal(H("3a3a42"))
    rod(t, 7, 27, 24, 6, 4.5, p)
    for k in range(3, 20, 3):
        x, y = 7 + (24 - 7) * k / 21, 27 - 21 * k / 21
        t.put(int(x), int(y), p[1])
    t.disc(22.6, 7.6, 2.2, pal(H("16161a")), lit=False)
    return finish(t)


@tex("item/electrode_blank")
def _(n):
    t = Tex()
    rod(t, 7, 27, 24, 6, 4.5, pal(H("4a4a52")))
    return finish(t)


@tex("item/tungsten_rod")
def _(n):
    t = Tex()
    rod(t, 5, 28, 27, 4, 1.8, pal(H("767080")))
    return finish(t)


@tex("item/silicon_boule")
def _(n):
    """Монокристалл Чохральского: цилиндр с коническим «плечом» у затравки и хвостом внизу, кольца роста."""
    t = Tex()
    p = pal(H("7c8496"))
    for y in range(3, 30):
        if y < 9:
            w = 1 + (y - 3) * 1.1
        elif y > 26:
            w = 7 - (y - 26) * 2
        else:
            w = 7.5
        for x in range(32):
            u = (x + 0.5 - 16) / w
            if abs(u) <= 1:
                k = 4 if -0.7 < u < -0.4 else 3 if u < -0.1 else (1 if u > 0.55 else 2)
                if y % 5 == 0 and y > 9:
                    k = max(0, k - 1)
                t.put(x, y, p[k])
    t.rect(15, 1, 16, 3, p[3])
    return finish(t)


@tex("item/copper_wire")
def _(n):
    """Моток медного провода на катушке: щёки катушки и витки."""
    t = Tex()
    for y in range(10, 23):
        for x in range(8, 24):
            v = (y - 10) / 12
            k = 4 if 0.12 < v < 0.25 else 3 if v < 0.4 else (1 if v > 0.78 else 2)
            if x % 2 == 0:
                k = max(0, k - 1)
            t.put(x, y, COPPER[k])
    for x0 in (4, 24):
        for y in range(6, 27):
            for x in range(x0, x0 + 4):
                u = (x - x0) / 3
                t.put(x, y, WOOD[3] if u < 0.4 else WOOD[2] if u < 0.8 else WOOD[1])
        t.hline(x0, x0 + 3, 6, WOOD[4])
        t.hline(x0, x0 + 3, 26, WOOD[0])
    # хвост провода
    for i in range(6):
        t.put(22 - i, 22 + i // 2 + 1, COPPER[3])
    return finish(t)
