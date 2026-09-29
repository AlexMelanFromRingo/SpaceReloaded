"""Предметы: детали, электроника, инструменты, скафандр, вёдра топлива, карты и снимки (012).

Все иконки — прозрачный фон, силуэт ~24–28 px по центру, свет сверху-слева, тёмный контур.
Общий приём — маска формы (PIL) + заливка материалом с кромочным светом (solid) или
цилиндрическим (cyl): так любой силуэт получает объём без ручной обводки.
"""
import math

from PIL import Image, ImageDraw

from . import tex
from .kit import (ALUMINIUM, AMBER, BLACK, BLUE, BRASS, CERAMIC, COPPER, CYAN, DARK_STEEL, GLASS, GOLD_FOIL,
                  GREEN, GUNMETAL, O2_BLUE, RED, RUBBER, SCREEN, STEEL, TITANIUM, WHITE_PAINT, WOOD, Tex, _h, mix, shade)

# ------------------------------------------------------------------ палитры группы (тень → блик)
PCB = [(0x0C, 0x33, 0x1A), (0x14, 0x4A, 0x26), (0x1E, 0x64, 0x34), (0x2E, 0x80, 0x46), (0x58, 0xA8, 0x6A)]
FR4 = [(0x6E, 0x66, 0x2E), (0x8A, 0x80, 0x3C), (0xA6, 0x9C, 0x4E), (0xBE, 0xB4, 0x66), (0xD8, 0xD0, 0x8E)]
PAPER = [(0xA8, 0x9C, 0x80), (0xC6, 0xBA, 0x9A), (0xDC, 0xD2, 0xB4), (0xEC, 0xE4, 0xCC), (0xF8, 0xF4, 0xE6)]
QUARTZ = [(0x8E, 0x9C, 0xA6), (0xAE, 0xBA, 0xC2), (0xC8, 0xD2, 0xD8), (0xDE, 0xE6, 0xEA), (0xF4, 0xF8, 0xFA)]
DIE_LOGIC = [(0x3A, 0x3A, 0x5C), (0x52, 0x52, 0x7E), (0x6C, 0x6C, 0x9E), (0x8C, 0x8C, 0xBA), (0xB8, 0xB8, 0xDC)]
DIE_CPU = [(0x1E, 0x22, 0x5E), (0x2C, 0x32, 0x80), (0x40, 0x48, 0xA6), (0x5E, 0x68, 0xC8), (0x9A, 0xA4, 0xEC)]
DIE_RAD = [(0x44, 0x30, 0x5E), (0x5E, 0x44, 0x80), (0x7C, 0x5E, 0xA4), (0x9C, 0x80, 0xC4), (0xC6, 0xB0, 0xE4)]
MONO_CELL = [(0x08, 0x0C, 0x18), (0x10, 0x16, 0x2A), (0x1A, 0x22, 0x3E), (0x2A, 0x36, 0x5A), (0x4E, 0x5E, 0x8E)]
POLY_CELL = [(0x10, 0x22, 0x4E), (0x18, 0x32, 0x6C), (0x24, 0x46, 0x8E), (0x36, 0x5E, 0xB0), (0x6E, 0x92, 0xD4)]
SAPPHIRE = [(0x4E, 0x6A, 0x9A), (0x6A, 0x88, 0xB8), (0x88, 0xA6, 0xD2), (0xA8, 0xC2, 0xE6), (0xD6, 0xE6, 0xF6)]
FERRITE = [(0x16, 0x17, 0x1A), (0x24, 0x25, 0x29), (0x34, 0x35, 0x3A), (0x4A, 0x4B, 0x51), (0x70, 0x72, 0x7A)]
PHENOLIC = [(0x42, 0x28, 0x16), (0x5C, 0x38, 0x1E), (0x76, 0x4A, 0x28), (0x92, 0x60, 0x36), (0xB2, 0x80, 0x52)]
YELLOWCAKE = [(0x5E, 0x5A, 0x1C), (0x7E, 0x78, 0x28), (0x9E, 0x96, 0x38), (0xBC, 0xB4, 0x4E), (0xDA, 0xD4, 0x7A)]
ZEOLITE = [(0x8A, 0x7C, 0x5C), (0xA6, 0x98, 0x76), (0xC0, 0xB2, 0x8E), (0xD6, 0xCA, 0xA8), (0xEC, 0xE2, 0xC8)]
BOOK = [(0x12, 0x26, 0x5C), (0x1A, 0x36, 0x80), (0x24, 0x4A, 0xA6), (0x36, 0x62, 0xC6), (0x70, 0x98, 0xE2)]
SUIT_ORANGE = [(0x8A, 0x3A, 0x0C), (0xB4, 0x50, 0x12), (0xDE, 0x6C, 0x1C), (0xF2, 0x8E, 0x3A), (0xFF, 0xBC, 0x7C)]
MASK_RED = [(0x5E, 0x10, 0x14), (0x82, 0x18, 0x1C), (0xA6, 0x24, 0x26), (0xC4, 0x3C, 0x38), (0xE2, 0x78, 0x6C)]
# топливо в вёдрах — цвета жидкостей 003 (как у блоков-жидкостей)
HYDROLOX = [(0x2E, 0x86, 0xB4), (0x4A, 0xAA, 0xD8), (0x6E, 0xCC, 0xF2), (0x96, 0xE2, 0xFF), (0xD2, 0xF6, 0xFF)]
KEROLOX = [(0x7E, 0x46, 0x12), (0xA6, 0x62, 0x1E), (0xCC, 0x88, 0x3A), (0xE2, 0xA2, 0x4E), (0xF6, 0xCC, 0x86)]
METHALOX = [(0x48, 0x5E, 0x9E), (0x68, 0x80, 0xC0), (0x8C, 0xA6, 0xE0), (0xA6, 0xC0, 0xF4), (0xD4, 0xE2, 0xFF)]
SPECTRUM = [(0xD8, 0x30, 0x30), (0xEE, 0x8A, 0x22), (0xEE, 0xD8, 0x30), (0x4C, 0xC0, 0x4E), (0x30, 0xA8, 0xD8),
            (0x44, 0x5C, 0xD8), (0x92, 0x48, 0xC8)]


# ------------------------------------------------------------------ маски и объём
def mk(fn):
    """Маска 32×32: fn(draw) рисует фигуру значением 255."""
    m = Image.new("L", (32, 32), 0)
    fn(ImageDraw.Draw(m))
    return m


def inside(m, x, y):
    return 0 <= x < 32 and 0 <= y < 32 and m.getpixel((x, y)) > 127


def solid(t, m, pal, base=2, grain=0.2, seed=1, edge=True):
    """Заливка маски материалом: зерно 2×2 и кромочный свет (верх/лево — блик, низ/право — тень)."""
    for y in range(32):
        for x in range(32):
            if not inside(m, x, y):
                continue
            k = base
            h = _h(x // 2, y // 2, seed)
            if h < grain * 0.25:
                k -= 1
            elif h > 1 - grain * 0.25:
                k += 1
            if edge:
                if not inside(m, x, y - 1) or not inside(m, x - 1, y):
                    k = base + 1 if inside(m, x, y - 1) or inside(m, x - 1, y) else base + 2
                elif not inside(m, x, y + 1) or not inside(m, x + 1, y):
                    k = base - 1
            t.put(x, y, pal[max(0, min(4, k))])


def cyl(t, m, pal, x0, x1, base=2, seed=1, grain=0.1):
    """Вертикальный цилиндр в пределах маски: блик на первой трети, тень к правому краю."""
    for y in range(32):
        for x in range(32):
            if not inside(m, x, y):
                continue
            u = (x + 0.5 - x0) / max(1, x1 - x0 + 1)
            k = base
            if u < 0.12:
                k = base - 1
            elif u < 0.38:
                k = base + 1 if not (0.2 < u < 0.3) else base + 2
            elif u > 0.84:
                k = base - 2
            elif u > 0.66:
                k = base - 1
            if _h(x, y // 3, seed) > 1 - grain * 0.3:
                k += 1
            t.put(x, y, pal[max(0, min(4, k))])


def done(t):
    t.outline()
    return t


def ellipse_mask(x0, y0, x1, y1):
    return mk(lambda d: d.ellipse((x0, y0, x1, y1), fill=255))


def rect_mask(x0, y0, x1, y1):
    return mk(lambda d: d.rectangle((x0, y0, x1, y1), fill=255))


def poly_mask(pts):
    return mk(lambda d: d.polygon(pts, fill=255))


def pins_h(t, x0, x1, y, pitch=3, length=2, pal=GOLD_FOIL, down=True):
    """Ряд выводов микросхемы вдоль горизонтали (вниз или вверх от корпуса)."""
    for x in range(x0, x1 + 1, pitch):
        for i in range(length):
            yy = y + i if down else y - i
            t.put(x, yy, pal[3] if i == 0 else pal[2])
            t.put(x + 1, yy, pal[1])


def pins_v(t, y0, y1, x, pitch=3, length=2, pal=GOLD_FOIL, right=True):
    for y in range(y0, y1 + 1, pitch):
        for i in range(length):
            xx = x + i if right else x - i
            t.put(xx, y, pal[3] if i == 0 else pal[2])
            t.put(xx, y + 1, pal[1])


# ------------------------------------------------------------------ банки и пайки
def tin(t, x0=8, x1=23, y0=5, y1=27):
    """Жестяная банка: цилиндр с закатанными ободами сверху и снизу."""
    m = rect_mask(x0, y0 + 2, x1, y1 - 1)
    cyl(t, m, STEEL, x0, x1, base=3, seed=31)
    top = ellipse_mask(x0, y0, x1, y0 + 4)
    solid(t, top, ALUMINIUM, base=2, grain=0.05, edge=False)
    for x in range(x0, x1 + 1):              # закатка: светлый верхний обод, тёмный нижний
        t.put(x, y0 + 4, STEEL[1])
        t.put(x, y1, STEEL[1])
        t.put(x, y1 - 1, STEEL[3] if x < (x0 + x1) // 2 else STEEL[2])
    t.hline(x0 + 3, x1 - 3, y0, ALUMINIUM[4])
    for x in range(x0 + 1, x1):
        if (x - x0) % 3 == 0:
            t.put(x, y0 + 6, STEEL[2])       # рёбра жёсткости
            t.put(x, y1 - 3, STEEL[2])


@tex("item/canned_ration")
def canned_ration(name):
    """Паёк в жестяной банке: бумажная этикетка по центру, ключ-открывашка на крышке."""
    t = Tex()
    tin(t)
    lab = rect_mask(8, 11, 23, 21)
    cyl(t, lab, KEROLOX, 8, 23, base=3, seed=5, grain=0)
    for x in range(8, 24):
        t.put(x, 11, KEROLOX[4] if x < 15 else KEROLOX[3])
        t.put(x, 21, KEROLOX[1])
        t.put(x, 13, RED[2])
        t.put(x, 19, RED[2])
    t.rect(13, 15, 18, 17, PAPER[4])         # поле с маркировкой
    t.hline(14, 17, 16, RED[1])
    t.hline(13, 18, 15, PAPER[3])
    t.put(12, 6, STEEL[0])                   # язычок ключа
    t.hline(12, 16, 7, BRASS[3])
    return done(t)


@tex("item/empty_can")
def empty_can(name):
    """Пустая банка: крышка вскрыта и отогнута, видно тёмное нутро."""
    t = Tex()
    tin(t)
    inner = ellipse_mask(10, 6, 21, 8)
    for y in range(6, 9):
        for x in range(10, 22):
            if inside(inner, x, y):
                t.put(x, y, DARK_STEEL[1] if x > 12 else DARK_STEEL[2])
    lid = poly_mask([(19, 6), (24, 1), (26, 2), (22, 7)])   # отогнутая крышка
    solid(t, lid, ALUMINIUM, base=3, grain=0, seed=4)
    return done(t)


# ------------------------------------------------------------------ грузы и детали ракет
@tex("item/cargo_pod")
def cargo_pod(name):
    """Грузовой контейнер: белая капсула с аркой крыши, люк, тёмный теплозащитный поддон."""
    t = Tex()
    body = mk(lambda d: (d.rectangle((6, 14, 25, 25), fill=255), d.ellipse((6, 4, 25, 24), fill=255)))
    cyl(t, body, WHITE_PAINT, 6, 25, base=3, seed=9)
    base = rect_mask(5, 25, 26, 28)
    solid(t, base, WOOD, base=1, grain=0.3, seed=2)
    t.hline(5, 26, 25, WOOD[3])
    # люк
    t.rect(12, 13, 19, 22, WHITE_PAINT[2])
    t.bevel(12, 13, 19, 22, WHITE_PAINT, raised=False, base=2)
    t.hline(14, 17, 17, STEEL[1])
    t.hline(14, 17, 18, STEEL[3])
    # маркировочная полоса и огни
    for x in range(7, 25):
        if (x // 2) % 2 == 0:
            t.put(x, 11, SUIT_ORANGE[2])
    t.led(8, 20, RED, True)
    t.led(22, 20, GREEN, True)
    t.put(13, 6, WHITE_PAINT[4])
    t.put(12, 7, WHITE_PAINT[4])
    return done(t)


@tex("item/heat_shield")
def heat_shield(name):
    """Теплозащитный экран: абляционные соты по диску, медно-оранжевый силовой обод."""
    t = Tex()
    m = ellipse_mask(3, 3, 28, 28)
    for y in range(32):
        for x in range(32):
            if not inside(m, x, y):
                continue
            # гексагональная сетка тайлов
            q = (x + (3 if (y // 4) % 2 else 0)) % 6
            r = y % 4
            k = 2 if (x // 6 + y // 4) % 3 else 1
            if q == 0 or r == 0:
                k = 0
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            if -(dx + dy) > 10:
                k += 1
            elif dx + dy > 12:
                k -= 1
            t.put(x, y, DARK_STEEL[max(0, min(4, k))])
    t.ring(16, 16, 10.6, 13, COPPER, base=2)
    t.ring(16, 16, 10.6, 11.4, COPPER, base=1)
    for a in range(8):
        ang = a * math.pi / 4
        t.rivet(int(16 + 11.8 * math.cos(ang)) - 1, int(16 + 11.8 * math.sin(ang)) - 1, BRASS)
    t.glow(12, 12, 5, (0x60, 0x66, 0x70), 0.25)
    return done(t)


def bell(t, pal, seed, tubes=True, gaps=False, band=True):
    """Сопло-колокол регенеративного охлаждения: горловина сверху, раструб вниз, трубки вдоль образующей."""
    pts = [(13, 3), (18, 3), (18, 6), (21, 12), (25, 20), (27, 27), (4, 27), (6, 20), (10, 12), (13, 6)]
    m = poly_mask(pts)
    cyl(t, m, pal, 4, 27, base=2, seed=seed, grain=0)
    if tubes:
        for y in range(6, 27):
            # ширина раструба на высоте y
            xs = [x for x in range(32) if inside(m, x, y)]
            if not xs:
                continue
            l, r = xs[0], xs[-1]
            n = 7
            for i in range(1, n):
                if gaps and i in (2, 5) and y < 18:
                    continue
                x = int(round(l + (r - l) * i / n))
                t.put(x, y, pal[0])
                t.put(x - 1, y, mix(pal[2], pal[3], 0.5))
    if band:
        t.rect(12, 2, 19, 5, STEEL[2])            # фланец/коллектор у горловины
        t.bevel(12, 2, 19, 5, STEEL, base=2)
        for y in (26, 27):
            xs = [x for x in range(32) if inside(m, x, y)]
            t.hline(xs[0], xs[-1], y, STEEL[3] if y == 26 else STEEL[1])


@tex("item/regen_nozzle")
def regen_nozzle(name):
    t = Tex()
    bell(t, COPPER, 7)
    return done(t)


@tex("item/incomplete_nozzle")
def incomplete_nozzle(name):
    """Недоделанное сопло: трубки выложены не все, без коллектора и выходного кольца, медь не отполирована."""
    t = Tex()
    dull = [mix(c, (0x50, 0x3A, 0x2C), 0.3) for c in COPPER]
    bell(t, dull, 7, band=False)
    # трубки выложены только на левой половине: справа — оправка-болванка из тёмной стали
    for y in range(3, 28):
        xs = [x for x in range(32) if t.get(x, y)[3]]
        if not xs:
            continue
        cut = xs[0] + int((xs[-1] - xs[0]) * (0.55 + 0.15 * ((y * 7) % 3) / 2))
        for x in range(cut, xs[-1] + 1):
            k = 2 if x < xs[-1] - 1 else 1
            t.put(x, y, DARK_STEEL[k + (1 if x == cut else 0)])
    t.rect(13, 2, 18, 3, dull[1])
    return done(t)


def injector_disc(t, holes):
    """Форсуночная головка: медный диск с фаской; holes — концентрические ряды отверстий."""
    t.disc(16, 16, 12.5, COPPER, base=2)
    t.ring(16, 16, 11.2, 12.5, COPPER, base=3)
    if holes:
        t.disc(16, 16, 2, DARK_STEEL, base=1, lit=False)
        for r, n in ((5.5, 6), (9, 12)):
            for i in range(n):
                a = i * 2 * math.pi / n + (0.26 if n == 12 else 0)
                x, y = int(round(16 + r * math.cos(a) - 0.5)), int(round(16 + r * math.sin(a) - 0.5))
                t.put(x, y, COPPER[0])
                t.put(x + 1, y + 1, COPPER[4])
    else:
        # заготовка: следы резца — концентрические риски
        for r in (4, 7, 10):
            for i in range(24):
                a = i * 2 * math.pi / 24
                t.darken(int(16 + r * math.cos(a)), int(16 + r * math.sin(a)), 0.12)
    t.glow(11, 11, 5, (0xF0, 0xC0, 0x90), 0.3)


@tex("item/injector_plate")
def injector_plate(name):
    t = Tex()
    injector_disc(t, True)
    return done(t)


@tex("item/incomplete_injector")
def incomplete_injector(name):
    t = Tex()
    injector_disc(t, False)
    return done(t)


def volute(t, pal, finished=True):
    """Турбонасос: улитка корпуса, выходной патрубок вправо-вверх, вал по центру."""
    body = ellipse_mask(3, 6, 24, 27)
    solid(t, body, pal, base=2, grain=0.15, seed=12)
    t.ring(13.5, 16.5, 8.5, 10.5, pal, base=3)
    pipe = rect_mask(15, 5, 29, 11)
    solid(t, pipe, pal, base=2, grain=0.1, seed=13)
    t.hline(15, 28, 6, pal[4])
    t.hline(15, 28, 10, pal[0])
    if finished:
        t.rect(27, 3, 29, 13, pal[3])         # фланец патрубка
        t.vline(29, 3, 13, pal[1])
        t.put(28, 4, STEEL[0])
        t.put(28, 12, STEEL[0])
        t.disc(13.5, 16.5, 6, pal, base=1)
        t.disc(13.5, 16.5, 2.5, BRASS, base=2)
        for a in range(6):
            ang = a * math.pi / 3
            t.screw(int(13.5 + 8.2 * math.cos(ang)) - 1, int(16.5 + 8.2 * math.sin(ang)) - 1, STEEL)
    else:
        # крышка не поставлена: видна крыльчатка
        t.disc(13.5, 16.5, 7, DARK_STEEL, base=1, lit=False)
        for i in range(6):
            a = i * math.pi / 3
            for s in range(2, 7):
                x = 13.5 + s * math.cos(a + s * 0.12)
                y = 16.5 + s * math.sin(a + s * 0.12)
                t.put(int(x), int(y), TITANIUM[3])
        t.disc(13.5, 16.5, 1.8, BRASS, base=3)


@tex("item/turbopump")
def turbopump(name):
    t = Tex()
    volute(t, STEEL, True)
    return done(t)


@tex("item/incomplete_turbopump")
def incomplete_turbopump(name):
    t = Tex()
    volute(t, [mix(c, (0x6A, 0x5E, 0x50), 0.25) for c in STEEL], False)
    return done(t)


@tex("item/fuel_basket")
def fuel_basket(name):
    """Топливная корзина реактора: стальной каркас-решётка, внутри три стержня с таблетками урана."""
    t = Tex()
    frame = mk(lambda d: (d.rectangle((6, 3, 25, 28), fill=255)))
    solid(t, frame, STEEL, base=2, grain=0.1, seed=3)
    t.recess(8, 6, 23, 25, DARK_STEEL, base=1)
    for i, x in enumerate((10, 15, 20)):
        m = rect_mask(x, 7, x + 2, 24)
        cyl(t, m, YELLOWCAKE, x, x + 2, base=2, seed=i)
        for y in range(9, 24, 3):
            t.hline(x, x + 2, y, YELLOWCAKE[0])     # стыки таблеток
        t.hline(x, x + 2, 7, STEEL[3])              # концевые заглушки
        t.hline(x, x + 2, 24, STEEL[1])
    for y in (11, 19):                              # дистанционирующие решётки
        t.hline(8, 23, y, STEEL[3])
        t.hline(8, 23, y + 1, STEEL[1])
    t.rect(12, 1, 19, 3, STEEL[3])                  # головка захвата
    t.hline(12, 19, 1, STEEL[4])
    t.hline(14, 17, 2, STEEL[1])
    return done(t)


@tex("item/fueling_hose")
def fueling_hose(name):
    """Заправочный шланг: гибкий шланг дугой, латунный пистолет с синим клапаном."""
    t = Tex()
    pts = [(4 + 20 * s + 5 * math.sin(s * math.pi) * -1, 28 - 20 * s + 5 * math.sin(s * math.pi) * -1)
           for s in [i / 40 for i in range(41)]]
    hose = mk(lambda d: d.line(pts, fill=255, width=4, joint="curve"))
    solid(t, hose, RUBBER, base=2, grain=0.1, seed=6)
    for i in range(3, 40, 4):                        # оплётка
        x, y = pts[i]
        t.put(int(x), int(y), RUBBER[4])
    noz = poly_mask([(22, 7), (26, 3), (29, 6), (25, 10)])
    solid(t, noz, BRASS, base=2, grain=0, seed=1)
    t.rect(27, 2, 29, 4, CYAN[3])
    t.put(27, 2, CYAN[4])
    t.rect(22, 9, 24, 11, BRASS[1])                  # хомут
    t.put(22, 9, BRASS[3])
    return done(t)


# ------------------------------------------------------------------ платы и слоистые материалы
def board(t, pal, x0=3, y0=5, x1=28, y1=26, seed=1):
    m = rect_mask(x0, y0, x1, y1)
    solid(t, m, pal, base=2, grain=0.15, seed=seed)


def traces(t, pal=COPPER):
    """Медные дорожки платы: шина к разъёму, разводка к посадочному месту."""
    c = pal[3]
    for y in (9, 13, 17):
        t.hline(6, 12, y, c)
    t.vline(12, 9, 22, c)
    t.hline(12, 17, 22, c)
    t.hline(20, 25, 10, c)
    t.vline(25, 10, 15, c)
    for x, y in ((6, 9), (6, 13), (6, 17), (20, 10), (25, 15)):
        t.rect(x - 1, y - 1, x, y, pal[4])       # контактные площадки
        t.put(x, y, pal[1])


@tex("item/circuit_board")
def circuit_board(name):
    """Собранная плата: зелёный текстолит, дорожки, микросхема, резисторы, позолоченный разъём."""
    t = Tex()
    board(t, PCB)
    traces(t)
    t.rect(14, 8, 22, 17, BLACK[2])              # микросхема
    t.bevel(14, 8, 22, 17, BLACK, base=2)
    pins_h(t, 15, 21, 18, pitch=2, length=1, pal=STEEL)
    t.put(15, 9, BLACK[4])
    for x in (18, 22):                           # резисторы
        t.rect(x, 21, x + 2, 22, CERAMIC[2])
        t.put(x + 1, 21, RED[2])
    for x in range(5, 27, 2):                    # разъём
        t.vline(x, 24, 26, GOLD_FOIL[3])
        t.put(x, 24, GOLD_FOIL[4])
    t.led(7, 20, GREEN, True)
    return done(t)


@tex("item/incomplete_circuit_board")
def incomplete_circuit_board(name):
    """Незавершённая плата: те же дорожки, но посадочные места пусты, разъём не позолочен."""
    t = Tex()
    board(t, PCB)
    traces(t)
    t.rect(14, 8, 22, 17, PCB[1])
    for x in range(15, 22, 2):
        t.put(x, 8, COPPER[3])
        t.put(x, 17, COPPER[3])
    t.rect(18, 21, 20, 22, PCB[1])
    t.rect(22, 21, 24, 22, PCB[1])
    for x in range(5, 27, 2):
        t.vline(x, 24, 26, COPPER[2])
    return done(t)


def slab(t, top_pal, edge_pal, seed, top_grain=0.1):
    """Лист в лёгкой перспективе: лицевая сторона сверху, торец снизу."""
    top = poly_mask([(7, 6), (29, 6), (25, 22), (3, 22)])
    solid(t, top, top_pal, base=2, grain=top_grain, seed=seed)
    edge = poly_mask([(3, 23), (25, 23), (25, 26), (3, 26)])
    solid(t, edge, edge_pal, base=1, grain=0.05, seed=seed + 1)
    side = poly_mask([(26, 22), (29, 7), (29, 11), (26, 26)])
    solid(t, side, edge_pal, base=0, grain=0, seed=seed)


@tex("item/copper_clad_laminate")
def copper_clad_laminate(name):
    """Фольгированный текстолит: медная фольга сверху, стеклотекстолит в торце."""
    t = Tex()
    slab(t, COPPER, FR4, 21, top_grain=0.05)
    for i in range(3):                               # отражение на фольге
        t.hline(10 + i * 2, 20 + i * 2, 9 + i, COPPER[4])
    t.hline(4, 24, 23, COPPER[2])
    return done(t)


@tex("item/fr4_laminate")
def fr4_laminate(name):
    """FR-4: стеклоткань в эпоксидке — видно переплетение."""
    t = Tex()
    slab(t, FR4, FR4, 23)
    for y in range(7, 22):
        for x in range(4, 29):
            if t.get(x, y)[3] and (x + y) % 4 == 0 and (x // 2 + y // 2) % 2:
                t.darken(x, y, 0.08)
    return done(t)


@tex("item/core_rope_memory")
def core_rope_memory(name):
    """Прошивочная память: гетинаксовая рамка, ряды ферритовых колец, медные провода-«верёвки»."""
    t = Tex()
    m = rect_mask(4, 3, 27, 28)
    solid(t, m, PHENOLIC, base=2, grain=0.2, seed=8)
    t.recess(7, 6, 24, 25, PHENOLIC, base=1)
    for i in range(8):                               # диагональные провода
        x = 7 + i * 2
        for s in range(19):
            t.put(x + s // 2, 6 + s, COPPER[3] if s % 2 else COPPER[2])
    for cy in range(8, 25, 4):
        for cx in range(9, 24, 4):
            t.ring(cx, cy, 0.8, 1.9, FERRITE, base=3)
    for x in (5, 25):
        for y in range(5, 27, 3):
            t.put(x, y, BRASS[3])
    return done(t)


# ------------------------------------------------------------------ кристаллы и фотошаблоны
def die(t, pal, kind):
    """Кристалл: кремний с интерференционным оттенком, золотые площадки по периметру, рисунок топологии."""
    m = rect_mask(4, 4, 27, 27)
    solid(t, m, pal, base=2, grain=0.08, seed=17)
    t.rect(6, 6, 25, 25, pal[1])
    for i in range(6, 26, 3):                        # площадки разварки
        for x, y in ((i, 5), (i, 26), (5, i), (26, i)):
            t.put(x, y, GOLD_FOIL[3])
    if kind == "logic":                              # регулярная матрица вентилей
        for y in range(8, 24, 4):
            for x in range(8, 24, 4):
                t.rect(x, y, x + 2, y + 2, pal[3])
                t.put(x, y, pal[4])
    elif kind == "cpu":                              # кэш (регулярный) + ядро (разнородное)
        for y in range(8, 16, 2):
            t.hline(8, 23, y, pal[3])
        t.rect(8, 18, 14, 23, pal[3])
        t.rect(17, 18, 23, 20, pal[2])
        t.rect(17, 22, 23, 23, pal[4])
        t.put(8, 18, pal[4])
    else:                                            # радстойкий: защитное кольцо и крупные ячейки
        t.bevel(7, 7, 24, 24, pal, base=2)
        for y in (10, 17):
            for x in (10, 17):
                t.rect(x, y, x + 4, y + 4, pal[3])
                t.bevel(x, y, x + 4, y + 4, pal, base=3)
    # радужный блик на кремнии
    for s in range(6):
        t.blend(6 + s, 6, CYAN[4], 0.35)
        t.blend(6, 6 + s, CYAN[4], 0.35)


@tex("item/die_logic")
def die_logic(name):
    t = Tex()
    die(t, DIE_LOGIC, "logic")
    return done(t)


@tex("item/die_microprocessor")
def die_microprocessor(name):
    t = Tex()
    die(t, DIE_CPU, "cpu")
    return done(t)


@tex("item/die_radhard")
def die_radhard(name):
    t = Tex()
    die(t, DIE_RAD, "rad")
    return done(t)


def photomask(t, pal, kind):
    """Фотошаблон: кварцевая пластина с хромовым рисунком (цвет — по изделию, как прежде)."""
    m = rect_mask(3, 3, 28, 28)
    solid(t, m, QUARTZ, base=3, grain=0.05, seed=19)
    t.bevel(5, 5, 26, 26, QUARTZ, raised=False, base=2)
    c, d = pal[2], pal[1]
    if kind == "logic":                              # меандр межсоединений
        for i, x in enumerate(range(8, 24, 4)):
            t.vline(x, 8, 23, c)
            t.vline(x + 1, 8, 23, d)
            y = 8 if i % 2 else 23
            t.hline(x, x + 4, y, c)
        t.vline(24, 8, 23, c)
    elif kind == "cpu":
        for y in range(8, 25, 5):
            t.hline(7, 24, y, c)
            t.hline(7, 24, y + 1, d)
        for x in range(7, 25, 5):
            t.vline(x, 8, 24, c)
    else:
        for y in range(8, 25, 5):
            t.hline(7, 24, y, c)
        for x in range(7, 25, 6):
            t.vline(x, 8, 23, c)
            t.vline(x + 1, 8, 23, d)
        t.rect(14, 14, 17, 17, c)
    for x, y in ((6, 6), (25, 6), (6, 25), (25, 25)):   # метки совмещения
        t.put(x, y, BLACK[2])
    t.hline(4, 27, 4, QUARTZ[4])


@tex("item/photomask_logic")
def photomask_logic(name):
    t = Tex()
    photomask(t, COPPER, "logic")
    return done(t)


@tex("item/photomask_microprocessor")
def photomask_microprocessor(name):
    t = Tex()
    photomask(t, RED, "cpu")
    return done(t)


@tex("item/photomask_radhard")
def photomask_radhard(name):
    t = Tex()
    photomask(t, [(0x3A, 0x10, 0x22), (0x52, 0x16, 0x30), (0x6E, 0x1E, 0x42), (0x8C, 0x2E, 0x58), (0xB0, 0x58, 0x80)],
              "rad")
    return done(t)


@tex("item/diffraction_grating")
def diffraction_grating(name):
    """Дифракционная решётка: штрихи на стекле, радужная дисперсия по диагонали."""
    t = Tex()
    m = rect_mask(5, 3, 26, 28)
    solid(t, m, GLASS, base=3, grain=0, seed=2)
    for y in range(4, 28):
        for x in range(6, 26):
            s = (x + y * 0.6) / 4.2
            c = SPECTRUM[int(s) % len(SPECTRUM)]
            t.put(x, y, mix(c, (0xE8, 0xEE, 0xF4), 0.25) if x % 2 == 0 else shade(c, -0.35))
    t.bevel(5, 3, 26, 28, STEEL, base=2)
    t.bevel(6, 4, 25, 27, STEEL, raised=False, base=2)
    return done(t)


# ------------------------------------------------------------------ микросхемы и корпуса
def dip(t, body, pins=STEEL, x0=6, y0=8, x1=25, y1=23, pitch=3):
    """Корпус DIP/QFP: тело с фаской, выводы сверху и снизу, ключ."""
    m = rect_mask(x0, y0, x1, y1)
    solid(t, m, body, base=2, grain=0.1, seed=4)
    pins_h(t, x0 + 2, x1 - 3, y1 + 1, pitch=pitch, length=3, pal=pins, down=True)
    pins_h(t, x0 + 2, x1 - 3, y0 - 1, pitch=pitch, length=3, pal=pins, down=False)
    t.disc(x0 + 3, y0 + 3, 1.2, body, base=0, lit=False)


@tex("item/logic_chip")
def logic_chip(name):
    """Логическая микросхема: светлый керамический DIP, маркировка."""
    t = Tex()
    dip(t, CERAMIC, pins=ALUMINIUM)
    t.hline(11, 20, 15, BLACK[2])
    t.hline(13, 18, 17, CERAMIC[0])
    return done(t)


@tex("item/microprocessor")
def microprocessor(name):
    """Микропроцессор: чёрный корпус, позолоченная крышка кристалла, золотые выводы."""
    t = Tex()
    dip(t, BLACK, pins=GOLD_FOIL, x0=4, x1=27, y0=7, y1=24, pitch=2)
    t.rect(11, 12, 20, 19, GOLD_FOIL[2])
    t.bevel(11, 12, 20, 19, GOLD_FOIL, base=2)
    t.hline(13, 18, 15, GOLD_FOIL[1])
    return done(t)


@tex("item/radhard_processor")
def radhard_processor(name):
    """Радстойкий процессор: керамика тёмно-красная (как прежде), золотая крышка, выводы по четырём сторонам."""
    t = Tex()
    m = rect_mask(6, 6, 25, 25)
    solid(t, m, MASK_RED, base=1, grain=0.12, seed=5)
    pins_h(t, 8, 23, 26, pitch=3, length=3, pal=GOLD_FOIL, down=True)
    pins_h(t, 8, 23, 5, pitch=3, length=3, pal=GOLD_FOIL, down=False)
    pins_v(t, 8, 23, 26, pitch=3, length=3, pal=GOLD_FOIL, right=True)
    pins_v(t, 8, 23, 5, pitch=3, length=3, pal=GOLD_FOIL, right=False)
    t.rect(10, 10, 21, 21, GOLD_FOIL[2])
    t.bevel(10, 10, 21, 21, GOLD_FOIL, base=2)
    t.hline(12, 19, 15, GOLD_FOIL[1])
    t.put(8, 8, MASK_RED[4])
    return done(t)


@tex("item/ceramic_package")
def ceramic_package(name):
    """Керамический корпус: белое основание, золотая крышка по центру, контактные ламели по краям."""
    t = Tex()
    m = rect_mask(4, 4, 27, 27)
    solid(t, m, CERAMIC, base=3, grain=0.1, seed=6)
    for i in range(7, 25, 3):
        for x, y in ((i, 4), (i, 27), (4, i), (27, i)):
            t.put(x, y, GOLD_FOIL[3])
    t.recess(9, 9, 22, 22, CERAMIC, base=2)
    t.rect(11, 11, 20, 20, GOLD_FOIL[2])
    t.bevel(11, 11, 20, 20, GOLD_FOIL, base=2)
    t.put(6, 6, GOLD_FOIL[4])
    return done(t)


@tex("item/image_sensor")
def image_sensor(name):
    """ПЗС-матрица: керамический корпус с выводами, окно, под ним фиолетово-синяя светочувствительная область."""
    t = Tex()
    dip(t, BLACK, pins=GOLD_FOIL, x0=4, x1=27, y0=8, y1=23, pitch=3)
    t.recess(7, 11, 24, 20, BLACK, base=1)
    for y in range(12, 20):
        for x in range(8, 24):
            t.put(x, y, DIE_CPU[2] if (x + y) % 2 else DIE_CPU[3])
    for x in range(8, 24):
        t.blend(x, 12, CYAN[4], 0.5)
    t.hline(9, 14, 13, DIE_RAD[4])
    return done(t)


@tex("item/pbs_detector")
def pbs_detector(name):
    """ИК-фоторезистор PbS: металлический корпус ТО с окном, три вывода вниз."""
    t = Tex()
    for x in (12, 16, 20):
        t.vline(x, 20, 29, STEEL[3])
        t.vline(x + 1, 20, 29, STEEL[1])
    m = rect_mask(7, 12, 25, 21)
    cyl(t, m, BRASS, 7, 25, base=2, seed=3)
    top = ellipse_mask(7, 4, 25, 16)
    solid(t, top, BRASS, base=3, grain=0, seed=2)
    t.disc(16, 10, 4.5, BLACK, base=1, lit=False)
    t.rect(14, 8, 17, 11, DIE_RAD[1])
    t.put(14, 8, CYAN[4])
    t.hline(6, 26, 21, BRASS[1])
    return done(t)


@tex("item/ferrite_core")
def ferrite_core(name):
    """Ферритовое кольцо: матово-чёрный тороид с прошитыми медными проводами."""
    t = Tex()
    t.ring(16, 16, 5, 12, FERRITE, base=2)
    t.ring(16, 16, 5, 6.2, FERRITE, base=1)
    for x in range(3, 29):                       # провод X
        if not (11 <= x <= 20):
            t.put(x, 16, COPPER[3])
            t.put(x, 17, COPPER[1])
    for i in range(5, 27):                       # провод по диагонали
        if not (11 <= i <= 20):
            t.put(i, i, COPPER[3])
    t.glow(11, 10, 4, (0x90, 0x94, 0xA0), 0.25)
    return done(t)


@tex("item/relay")
def relay(name):
    """Реле: катушка медного провода на ярме, якорь с контактами, чёрное основание."""
    t = Tex()
    b = rect_mask(4, 22, 27, 27)
    solid(t, b, BLACK, base=2, grain=0.1, seed=3)
    coil = rect_mask(7, 9, 18, 21)
    cyl(t, coil, COPPER, 7, 18, base=2, seed=2, grain=0)
    for y in range(10, 21, 2):
        t.hline(7, 18, y, COPPER[1])
    t.rect(6, 7, 19, 8, STEEL[3])                # ярмо
    t.rect(20, 7, 22, 21, STEEL[2])
    t.vline(20, 7, 21, STEEL[4])
    t.rect(10, 4, 24, 5, STEEL[3])               # якорь
    t.hline(10, 24, 4, STEEL[4])
    for x in (24, 26):                            # контактные пружины
        t.vline(x, 6, 21, BRASS[3])
    t.put(25, 6, GOLD_FOIL[4])
    for x in (7, 13, 19, 25):
        t.vline(x, 28, 29, STEEL[2])
    return done(t)


@tex("item/relay_logic")
def relay_logic(name):
    """Релейный логический блок: плата с тремя рядами миниатюрных реле."""
    t = Tex()
    m = rect_mask(3, 4, 28, 27)
    solid(t, m, DARK_STEEL, base=2, grain=0.1, seed=4)
    t.bevel(3, 4, 28, 27, DARK_STEEL, base=2)
    for row, y in enumerate((7, 14, 21)):
        for x in (6, 13, 20):
            t.rect(x, y, x + 5, y + 4, RED[2])
            t.bevel(x, y, x + 5, y + 4, RED, base=2)
            t.hline(x + 1, x + 4, y + 2, COPPER[3])
            t.led(x + 6, y, AMBER if (x + row) % 2 else GREEN, True)
    return done(t)


# ------------------------------------------------------------------ приборы и электроника
@tex("item/flight_computer")
def flight_computer(name):
    """Бортовой компьютер (как DSKY): серый корпус, зелёный цифровой индикатор, клавиатура."""
    t = Tex()
    m = rect_mask(3, 3, 28, 28)
    solid(t, m, GUNMETAL, base=2, grain=0.15, seed=5)
    t.rivets(inset=4, pal=STEEL)
    t.screen(7, 5, 24, 14, SCREEN, lit=False)
    for i, y in enumerate((7, 10, 12)):          # разряды индикатора
        for x in range(9, 23, 3):
            if _h(x, y, i) > 0.25:
                t.hline(x, x + 1, y, GREEN[4] if y == 7 else GREEN[3])
    for y in (17, 21, 25):
        for x in (7, 12, 17, 22):
            t.rect(x, y, x + 3, y + 2, WHITE_PAINT[2] if (x, y) != (22, 25) else AMBER[3])
            t.hline(x, x + 3, y, WHITE_PAINT[4] if (x, y) != (22, 25) else AMBER[4])
            t.hline(x, x + 3, y + 2, GUNMETAL[0])
    return done(t)


@tex("item/flight_program")
def flight_program(name):
    """Полётная программа: перфокарта со срезанным углом, колонки пробивок."""
    t = Tex()
    m = poly_mask([(3, 11), (8, 5), (28, 5), (28, 26), (3, 26)])
    solid(t, m, PAPER, base=3, grain=0.08, seed=7)
    for x in range(6, 27, 2):
        t.put(x, 8, BOOK[3])
    for y in range(11, 25, 2):
        for x in range(5, 27, 2):
            if _h(x, y, 42) > 0.62:
                t.put(x, y, BLACK[1])
                t.put(x, y + 1, PAPER[1])
    t.vline(27, 6, 26, PAPER[1])
    return done(t)


@tex("item/frequency_key")
def frequency_key(name):
    """Частотный ключ: латунная ключ-карта с кодовым чипом, светодиод, зубцы разъёма."""
    t = Tex()
    m = mk(lambda d: (d.rounded_rectangle((3, 7, 24, 25), 3, fill=255), d.rectangle((24, 11, 29, 21), fill=255)))
    solid(t, m, BRASS, base=2, grain=0.1, seed=3)
    t.disc(8, 16, 2.2, BRASS, base=0, lit=False)   # отверстие под кольцо
    t.put(8, 16, (0, 0, 0, 0))
    t.put(7, 16, (0, 0, 0, 0))
    t.put(8, 15, (0, 0, 0, 0))
    t.put(7, 15, (0, 0, 0, 0))
    t.rect(12, 12, 19, 19, BLACK[2])
    t.bevel(12, 12, 19, 19, BLACK, base=2)
    pins_v(t, 13, 18, 20, pitch=2, length=1, pal=GOLD_FOIL)
    t.led(14, 9, CYAN, True)
    for y in range(12, 21, 2):                     # контакты разъёма
        t.hline(25, 29, y, GOLD_FOIL[3])
    return done(t)


@tex("item/ground_radar")
def ground_radar(name):
    """Георадар: салазки-антенна, корпус с индикатором, оранжевая ручка-буксир."""
    t = Tex()
    h = mk(lambda d: d.line([(6, 13), (10, 5), (22, 5), (26, 13)], fill=255, width=3))
    solid(t, h, SUIT_ORANGE, base=2, grain=0, seed=1)
    m = rect_mask(3, 13, 28, 23)
    solid(t, m, STEEL, base=2, grain=0.12, seed=4)
    t.screen(6, 15, 16, 20, SCREEN, rows=2)
    t.led(20, 16, GREEN, True)
    t.led(23, 16, AMBER, False)
    sk = poly_mask([(2, 24), (29, 24), (27, 27), (4, 27)])
    solid(t, sk, RUBBER, base=2, grain=0.05, seed=2)
    return done(t)


@tex("item/ir_spectrometer")
def ir_spectrometer(name):
    """ИК-спектрометр: корпус с окном спектра, входная трубка-коллиматор сверху."""
    t = Tex()
    tube = rect_mask(14, 2, 17, 10)
    cyl(t, tube, STEEL, 14, 17, base=2)
    t.rect(13, 2, 18, 3, BLACK[3])
    m = rect_mask(3, 9, 28, 27)
    solid(t, m, GUNMETAL, base=2, grain=0.12, seed=8)
    t.recess(6, 14, 25, 20, BLACK, base=1)
    for i in range(18):
        c = SPECTRUM[int(i * len(SPECTRUM) / 18)]
        t.vline(7 + i, 15, 19, c)
        t.put(7 + i, 15, shade(c, 0.4))
    t.led(6, 23, GREEN, True)
    t.hline(10, 25, 24, GUNMETAL[1])
    t.rivets(inset=4, pal=STEEL, n=None)
    return done(t)


@tex("item/leak_scanner")
def leak_scanner(name):
    """Течеискатель: ручной прибор со щупом, экраном и индикаторами."""
    t = Tex()
    t.vline(16, 2, 9, STEEL[3])
    t.vline(17, 2, 9, STEEL[1])
    t.rect(15, 1, 18, 2, CYAN[3])
    m = mk(lambda d: d.rounded_rectangle((8, 9, 24, 29), 3, fill=255))
    solid(t, m, GUNMETAL, base=2, grain=0.1, seed=3)
    t.screen(11, 12, 21, 18, SCREEN, rows=2)
    t.led(11, 21, RED, True)
    t.led(15, 21, AMBER, False)
    t.led(19, 21, GREEN, True)
    t.grille(11, 24, 21, 27, DARK_STEEL, pitch=2)
    return done(t)


@tex("item/targeting_designator")
def targeting_designator(name):
    """Лазерный целеуказатель: корпус-бинокль, красный объектив лазера, дальномерный экран."""
    t = Tex()
    m = mk(lambda d: d.rounded_rectangle((5, 8, 26, 26), 3, fill=255))
    solid(t, m, GUNMETAL, base=2, grain=0.12, seed=5)
    for cx in (11, 21):                            # объективы
        t.disc(cx, 13, 3.5, BLACK, base=2)
        t.disc(cx, 13, 2, GLASS, base=3)
    t.disc(16, 5, 2.5, RED, base=3)                # излучатель
    t.rect(15, 6, 17, 8, STEEL[2])
    t.glow(16, 5, 5, RED[4], 0.35)
    t.screen(9, 18, 22, 23, SCREEN, rows=2)
    t.put(19, 20, RED[4])
    return done(t)


@tex("item/nife_battery")
def nife_battery(name):
    """Никель-железный аккумулятор: стальной рифлёный корпус, клеммы «+» и «−»."""
    t = Tex()
    m = rect_mask(5, 9, 26, 28)
    solid(t, m, STEEL, base=2, grain=0.1, seed=6)
    for x in range(8, 25, 3):                     # рифление банок
        t.vline(x, 11, 26, STEEL[1])
        t.vline(x + 1, 11, 26, STEEL[3])
    t.rect(5, 7, 26, 9, DARK_STEEL[3])
    t.hline(5, 26, 7, DARK_STEEL[4])
    for cx, pal in ((10, RED), (21, BLACK)):
        t.rect(cx - 1, 3, cx + 2, 7, pal[2])
        t.hline(cx - 1, cx + 2, 3, pal[4])
    t.hline(9, 12, 5, RED[4])                     # «+»
    t.vline(10, 4, 6, RED[4])
    t.hline(20, 23, 5, WHITE_PAINT[3])            # «−»
    return done(t)


@tex("item/lioh_cartridge")
def lioh_cartridge(name):
    """Патрон LiOH: белый цилиндр с окном гранул, серые торцевые крышки."""
    t = Tex()
    body = rect_mask(9, 5, 22, 27)
    cyl(t, body, WHITE_PAINT, 9, 22, base=3, seed=4)
    for y0 in (3, 25):
        cap = rect_mask(8, y0, 23, y0 + 3)
        cyl(t, cap, STEEL, 8, 23, base=2, seed=1)
    t.vline(15, 1, 3, STEEL[3])
    t.vline(16, 1, 3, STEEL[1])
    t.recess(12, 10, 19, 21, GLASS, base=2)
    for y in range(11, 21):
        for x in range(13, 19):
            t.put(x, y, WHITE_PAINT[4] if _h(x, y, 7) > 0.5 else WHITE_PAINT[1])
    t.hline(9, 22, 23, O2_BLUE[2])
    return done(t)


@tex("item/oxygen_mask")
def oxygen_mask(name):
    """Кислородная маска: прозрачный щиток, резиновая чашка, оранжевый клапан, ремни."""
    t = Tex()
    for y in (12, 13):                            # ремни
        t.hline(2, 29, y, RUBBER[3] if y == 12 else RUBBER[1])
    cup = mk(lambda d: d.chord((5, 3, 26, 29), 0, 360, fill=255))
    solid(t, cup, STEEL, base=3, grain=0.1, seed=4)
    visor = mk(lambda d: d.ellipse((8, 5, 23, 16), fill=255))
    for y in range(32):
        for x in range(32):
            if inside(visor, x, y):
                t.put(x, y, CYAN[3] if y < 8 else CYAN[2] if y < 13 else CYAN[1])
    t.hline(11, 15, 6, CYAN[4])
    t.put(10, 7, CYAN[4])
    t.disc(15.5, 22.5, 4, SUIT_ORANGE, base=2)
    t.grille(13, 21, 18, 24, DARK_STEEL, pitch=2)
    return done(t)


@tex("item/quartz_crucible")
def quartz_crucible(name):
    """Кварцевый тигель: полупрозрачная белая чаша, видно внутреннюю стенку."""
    t = Tex()
    m = mk(lambda d: (d.rectangle((4, 9, 27, 18), fill=255), d.chord((4, 6, 27, 29), 0, 180, fill=255)))
    cyl(t, m, QUARTZ, 4, 27, base=3, seed=3)
    inner = ellipse_mask(5, 5, 26, 12)
    solid(t, inner, QUARTZ, base=1, grain=0.05, edge=False)
    rim = mk(lambda d: d.ellipse((4, 4, 27, 13), outline=255, width=1))
    for y in range(32):
        for x in range(32):
            if inside(rim, x, y):
                t.put(x, y, QUARTZ[4] if y < 9 else QUARTZ[2])
    for y in range(14, 24):
        t.put(9, y, QUARTZ[4])
    return done(t)


@tex("item/telescope_mirror")
def telescope_mirror(name):
    """Зеркало телескопа: посеребрённый диск, толстая стеклянная кромка, блик и отражение."""
    t = Tex()
    t.disc(16, 16, 13, GLASS, base=3)
    t.ring(16, 16, 12, 13, QUARTZ, base=1)
    t.disc(16, 15, 11.5, ALUMINIUM, base=2)
    for y in range(32):                           # отражение неба: светлая полоса по диагонали
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 15
            if math.hypot(dx, dy) < 11 and abs(dx + dy + 4) < 3:
                t.blend(x, y, (0xFF, 0xFF, 0xFF), 0.55 if abs(dx + dy + 4) < 1.2 else 0.3)
            elif math.hypot(dx, dy) < 11 and dx + dy > 8:
                t.darken(x, y, 0.12)
    t.disc(16, 15, 1.5, ALUMINIUM, base=1, lit=False)   # центральная метка
    t.glow(11, 10, 4, (0xFF, 0xFF, 0xFF), 0.4)
    return done(t)


@tex("item/zeolite_bed")
def zeolite_bed(name):
    """Цеолитовый слой: стальная кассета, засыпанная гранулами-шариками."""
    t = Tex()
    m = rect_mask(3, 4, 28, 27)
    solid(t, m, STEEL, base=2, grain=0.1, seed=4)
    t.recess(6, 7, 25, 24, ZEOLITE, base=1)
    for y in range(8, 24, 2):
        for x in range(7 + (y // 2) % 2, 25, 2):
            t.put(x, y, ZEOLITE[3])
            t.put(x + 1, y + 1, ZEOLITE[1] if x + 1 < 25 else ZEOLITE[2])
            if _h(x, y, 9) > 0.8:
                t.put(x, y, ZEOLITE[4])
    t.rivets(inset=4, pal=STEEL)
    return done(t)


# ------------------------------------------------------------------ солнечные элементы и пластины
@tex("item/mono_solar_cell")
def mono_solar_cell(name):
    """Монокристаллический элемент: почти чёрный псевдоквадрат со срезанными углами, 3 шины и тонкие «пальцы»."""
    t = Tex()
    m = poly_mask([(7, 3), (24, 3), (28, 7), (28, 24), (24, 28), (7, 28), (3, 24), (3, 7)])
    solid(t, m, MONO_CELL, base=2, grain=0.05, seed=2)
    for y in range(5, 27, 3):
        t.hline(4, 27, y, MONO_CELL[3])
    for x in (9, 15, 21):
        t.vline(x, 3, 28, ALUMINIUM[3])
        t.vline(x + 1, 3, 28, ALUMINIUM[1])
    t.glow(9, 8, 6, (0x6E, 0x80, 0xB8), 0.3)
    return done(t)


@tex("item/solar_cell")
def solar_cell(name):
    """Поликристаллический элемент: синие ячейки мозаикой, серебряная сетка и рамка."""
    t = Tex()
    m = rect_mask(3, 3, 28, 28)
    for y in range(3, 29):
        for x in range(3, 29):
            k = 2 + (1 if _h(x // 3, y // 3, 5) > 0.7 else -1 if _h(x // 3, y // 3, 5) < 0.25 else 0)
            t.put(x, y, POLY_CELL[k])
    for i in (3, 11, 20, 28):
        t.hline(3, 28, i, ALUMINIUM[3])
        t.vline(i, 3, 28, ALUMINIUM[2])
    t.hline(3, 28, 3, ALUMINIUM[4])
    t.vline(3, 3, 28, ALUMINIUM[4])
    for i in (7, 15, 24):
        t.hline(4, 27, i, POLY_CELL[4])
    return done(t)


def voronoi(x, y, seed, n=24):
    """Номер ближайшего зерна (детерминированные центры) — кристаллиты мультикремния."""
    best, bi = 1e9, 0
    for i in range(n):
        cx, cy = 3 + 26 * _h(i, 1, seed), 3 + 26 * _h(i, 2, seed)
        d = (x - cx) ** 2 + (y - cy) ** 2
        if d < best:
            best, bi = d, i
    return bi


@tex("item/multicrystalline_wafer")
def multicrystalline_wafer(name):
    """Мультикристаллическая пластина: квадрат с видимыми зёрнами разного оттенка."""
    t = Tex()
    for y in range(4, 28):
        for x in range(4, 28):
            g = voronoi(x, y, 21)
            k = 2 + int(_h(g, 3, 21) * 2.99)
            k = min(4, k)
            t.put(x, y, POLY_CELL[k])
            if voronoi(x + 1, y, 21) != g or voronoi(x, y + 1, 21) != g:
                t.put(x, y, POLY_CELL[max(0, k - 1)])
    t.bevel(4, 4, 27, 27, POLY_CELL, base=2)
    return done(t)


@tex("item/sos_wafer")
def sos_wafer(name):
    """Кремний на сапфире: круглая пластина, прозрачно-голубой сапфир с базовым срезом и радужным бликом."""
    t = Tex()
    m = mk(lambda d: (d.ellipse((3, 3, 28, 28), fill=255), d.rectangle((0, 27, 31, 31), fill=0)))
    solid(t, m, SAPPHIRE, base=2, grain=0.05, seed=3)
    for i in range(-10, 11):
        for w in range(4):
            x, y = 16 + i - w, 16 + i + w - 4
            if inside(m, x, y):
                t.blend(x, y, SAPPHIRE[4], 0.3)
    for y in range(8, 24, 4):                     # сетка кристаллов на слое кремния
        for x in range(8, 24, 4):
            if inside(m, x, y) and inside(m, x + 2, y + 2):
                t.rect(x, y, x + 2, y + 2, DIE_LOGIC[2])
                t.put(x, y, DIE_LOGIC[4])
    return done(t)


# ------------------------------------------------------------------ вёдра топлива
def bucket(t, liquid):
    """Ведро по ванильному силуэту в 32 px: железные стенки, дужка, зеркало жидкости."""
    body = poly_mask([(5, 9), (26, 9), (23, 28), (8, 28)])
    cyl(t, body, STEEL, 5, 26, base=3, seed=11)
    for x in range(6, 26):
        t.put(x, 13, STEEL[2])                    # обжимной пояс
        t.put(x, 14, STEEL[4] if x < 14 else STEEL[3])
    t.hline(9, 22, 27, STEEL[1])
    rim = ellipse_mask(4, 5, 27, 12)
    solid(t, rim, STEEL, base=3, grain=0, edge=False)
    surf = ellipse_mask(6, 6, 25, 11)
    for y in range(32):
        for x in range(32):
            if inside(surf, x, y):
                t.put(x, y, liquid[3] if y > 7 else liquid[4])
    t.hline(10, 16, 7, liquid[4])
    t.hline(9, 22, 10, liquid[2])
    h = mk(lambda d: d.arc((5, 1, 26, 18), 190, 350, fill=255, width=1))   # дужка
    for y in range(32):
        for x in range(32):
            if inside(h, x, y) and t.get(x, y)[3] == 0:
                t.put(x, y, STEEL[2])
    t.put(4, 8, STEEL[4])
    t.put(27, 8, STEEL[1])


@tex("item/hydrolox_bucket")
def hydrolox_bucket(name):
    t = Tex()
    bucket(t, HYDROLOX)
    return done(t)


@tex("item/kerolox_bucket")
def kerolox_bucket(name):
    t = Tex()
    bucket(t, KEROLOX)
    return done(t)


@tex("item/methalox_bucket")
def methalox_bucket(name):
    t = Tex()
    bucket(t, METHALOX)
    return done(t)


# ------------------------------------------------------------------ инструменты и документы
@tex("item/engineer_hammer")
def engineer_hammer(name):
    """Инженерный молот (handheld): рукоять по диагонали, стальной боёк сверху справа."""
    t = Tex()
    handle = mk(lambda d: d.line([(4, 28), (19, 13)], fill=255, width=3))
    solid(t, handle, WOOD, base=2, grain=0.2, seed=4)
    for i in range(3):                             # обмотка рукояти
        t.put(6 + i * 2, 25 - i * 2, RUBBER[3])
        t.put(7 + i * 2, 25 - i * 2, RUBBER[1])
    head = poly_mask([(13, 11), (22, 2), (29, 9), (20, 18)])
    solid(t, head, STEEL, base=2, grain=0.1, seed=5)
    face = poly_mask([(22, 2), (24, 1), (30, 7), (29, 9)])
    solid(t, face, STEEL, base=3, grain=0, seed=1)
    t.rect(17, 9, 19, 11, SUIT_ORANGE[2])          # клин-метка
    t.put(17, 9, SUIT_ORANGE[4])
    return done(t)


@tex("item/engineer_manual")
def engineer_manual(name):
    """Справочник инженера: синий переплёт, оранжевые полосы, обрез страниц справа."""
    t = Tex()
    pages = rect_mask(8, 4, 27, 28)
    solid(t, pages, PAPER, base=3, grain=0, seed=1)
    for y in range(5, 28, 2):
        t.hline(24, 27, y, PAPER[1])
    cover = rect_mask(5, 3, 24, 27)
    solid(t, cover, BOOK, base=2, grain=0.12, seed=3)
    t.rect(5, 3, 7, 27, BOOK[1])                    # корешок
    t.vline(7, 3, 27, BOOK[0])
    for y in (8, 11):
        t.hline(10, 21, y, SUIT_ORANGE[3])
        t.hline(10, 21, y + 1, SUIT_ORANGE[1])
    t.rect(11, 16, 20, 21, WHITE_PAINT[3])          # шильдик
    t.hline(12, 19, 18, BOOK[1])
    t.hline(12, 17, 19, BOOK[2])
    return done(t)


@tex("item/mineral_map")
def mineral_map(name):
    """Минералогическая карта: лист с изолиниями рельефа и цветными пятнами находок, легенда внизу."""
    t = Tex()
    m = rect_mask(3, 3, 28, 28)
    solid(t, m, PAPER, base=3, grain=0.05, seed=2)
    for y in range(4, 23):
        for x in range(4, 28):
            v = math.sin(x * 0.35) * 3 + math.cos(y * 0.4 + x * 0.1) * 3 + (x - 16) ** 2 / 40
            if abs(v - round(v / 2.5) * 2.5) < 0.18:
                t.put(x, y, PHENOLIC[3])
    for (x, y), c in (((9, 9), RED), ((20, 7), BLUE), ((15, 15), GREEN), ((23, 17), AMBER), ((7, 18), CYAN)):
        t.disc(x, y, 1.6, c, base=3)
    t.hline(4, 27, 23, PAPER[1])
    for i, c in enumerate((RED, BLUE, GREEN, AMBER, CYAN)):
        t.rect(5 + i * 5, 25, 7 + i * 5, 26, c[3])
    t.vline(16, 3, 28, PAPER[2])                    # сгиб
    return done(t)


@tex("item/orbital_image")
def orbital_image(name):
    """Орбитальный снимок: фотоотпечаток в белом поле — кратеры и гряды поверхности."""
    t = Tex()
    m = rect_mask(3, 3, 28, 28)
    solid(t, m, PAPER, base=4, grain=0, seed=1)
    for y in range(5, 27):
        for x in range(5, 27):
            v = 0.5 + 0.25 * math.sin(x * 0.5 + y * 0.2) + 0.2 * (_h(x // 2, y // 2, 4) - 0.5)
            k = 1 + int(v * 3)
            t.put(x, y, STEEL[max(0, min(4, k))])
    for cx, cy, r in ((12, 12, 4), (21, 20, 3), (20, 9, 2)):     # кратеры: тень слева, вал справа
        t.disc(cx, cy, r, STEEL, base=1, lit=False)
        t.ring(cx, cy, r - 0.5, r + 0.8, STEEL, base=3)
        t.put(cx + r - 1, cy, STEEL[4])
    t.put(26, 5, RED[3])                             # метка кадра
    return done(t)


@tex("item/radargram")
def radargram(name):
    """Радарограмма: распечатка с тёмным полем, слои грунта и гипербола отражения от объекта."""
    t = Tex()
    m = rect_mask(3, 3, 28, 28)
    solid(t, m, PAPER, base=3, grain=0, seed=1)
    t.rect(5, 5, 26, 24, BLACK[1])
    for y in range(6, 24, 3):                        # слои
        for x in range(5, 27):
            if _h(x // 3, y, 5) > 0.3:
                t.put(x, y + int(math.sin(x * 0.3) * 0.8), BLACK[3])
    for x in range(6, 26):                           # гипербола
        y = int(9 + math.sqrt(4 + ((x - 15.5) / 1.8) ** 2) * 1.6)
        if y < 24:
            t.put(x, y, WHITE_PAINT[4])
            if y + 1 < 24:
                t.put(x, y + 1, WHITE_PAINT[1])
    t.hline(5, 26, 26, BLACK[2])
    for x in range(5, 27, 3):
        t.put(x, 25, BLACK[2])
    return done(t)


# ------------------------------------------------------------------ ровер
@tex("item/rover_chassis")
def rover_chassis(name):
    """Шасси ровера: алюминиевая рама-платформа, четыре узла подвески с тёмными ступицами."""
    t = Tex()
    deck = poly_mask([(5, 11), (27, 11), (29, 17), (3, 17)])
    solid(t, deck, ALUMINIUM, base=2, grain=0.1, seed=2)
    t.hline(4, 28, 13, ALUMINIUM[1])
    t.hline(5, 27, 11, ALUMINIUM[4])
    t.rect(3, 17, 29, 19, ALUMINIUM[1])
    for x in (4, 13, 19, 27):                        # стойки подвески
        t.vline(x, 19, 23, BLUE[2])
        t.vline(x + 1, 19, 23, BLUE[1])
    for cx in (5, 26):
        t.disc(cx, 25, 3.5, RUBBER, base=2)
        t.disc(cx, 25, 1.4, SUIT_ORANGE, base=2)
    for x in range(8, 25, 4):
        t.rivet(x, 14, STEEL)
    return done(t)


@tex("item/rover_wheel")
def rover_wheel(name):
    """Колесо ровера: сетчатая шина с шевронами грунтозацепов, оранжевая ступица."""
    t = Tex()
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            d = math.hypot(dx, dy)
            if 6 < d <= 13.5:
                mesh = (x + y) % 3 == 0 or (x - y) % 3 == 0
                k = 2 if mesh else 1
                if -(dx + dy) > 9:
                    k += 1
                elif dx + dy > 10:
                    k -= 1
                t.put(x, y, TITANIUM[max(0, min(4, k))])
    for i in range(12):                               # шевроны
        a = i * math.pi / 6
        for r in (11.5, 12.5):
            t.put(int(16 + r * math.cos(a)), int(16 + r * math.sin(a)), TITANIUM[4])
    t.ring(16, 16, 12.8, 13.8, TITANIUM, base=3)
    for i in range(6):                                # спицы
        a = i * math.pi / 3
        for r in range(4, 7):
            t.put(int(16 + r * math.cos(a)), int(16 + r * math.sin(a)), STEEL[3])
    t.disc(16, 16, 4, SUIT_ORANGE, base=2)
    t.disc(16, 16, 1.5, STEEL, base=2)
    return done(t)


# ------------------------------------------------------------------ скафандр
@tex("item/space_suit_chestplate")
def space_suit_chestplate(name):
    """Верх скафандра: белая оболочка, оранжевые вставки, панель системы жизнеобеспечения с индикаторами."""
    t = Tex()
    m = poly_mask([(3, 5), (11, 3), (20, 3), (28, 5), (28, 15), (24, 15), (24, 28), (7, 28), (7, 15), (3, 15)])
    solid(t, m, WHITE_PAINT, base=3, grain=0.15, seed=6)
    neck = ellipse_mask(11, 0, 20, 7)
    for y in range(32):
        for x in range(32):
            if inside(neck, x, y) and t.get(x, y)[3]:
                t.put(x, y, (0, 0, 0, 0))
    t.ring(15.5, 3, 4.5, 5.5, STEEL, base=2)        # шейное кольцо
    for x in range(3, 7):
        t.vline(x, 12, 15, SUIT_ORANGE[2])          # манжеты
    for x in range(25, 29):
        t.vline(x, 12, 15, SUIT_ORANGE[1])
    t.rect(11, 12, 20, 21, GUNMETAL[2])             # панель СЖО
    t.bevel(11, 12, 20, 21, GUNMETAL, base=2)
    t.led(13, 14, CYAN, True)
    t.led(17, 14, SUIT_ORANGE, True)
    t.hline(13, 18, 18, GUNMETAL[4])
    t.vline(15, 8, 11, STEEL[2])                    # шланг
    t.hline(7, 24, 25, SUIT_ORANGE[2])
    t.hline(7, 24, 26, SUIT_ORANGE[1])
    return done(t)


@tex("item/space_suit_leggings")
def space_suit_leggings(name):
    """Низ скафандра: белые штанины с гофрой коленей и оранжевым поясом."""
    t = Tex()
    m = mk(lambda d: (d.rectangle((6, 3, 25, 9), fill=255), d.rectangle((6, 9, 14, 29), fill=255),
                      d.rectangle((17, 9, 25, 29), fill=255)))
    solid(t, m, WHITE_PAINT, base=3, grain=0.15, seed=7)
    t.rect(6, 3, 25, 5, SUIT_ORANGE[2])
    t.hline(6, 25, 3, SUIT_ORANGE[4])
    t.rect(14, 4, 17, 5, STEEL[3])                  # пряжка
    for y in (17, 19, 21):                          # гофра коленей
        for x0 in (6, 17):
            t.hline(x0, x0 + 8, y, WHITE_PAINT[1])
    for x0 in (6, 17):
        t.hline(x0, x0 + 8, 27, STEEL[2])           # кольцо стыковки с ботинком
        t.hline(x0, x0 + 8, 28, STEEL[1])
    return done(t)


@tex("item/space_suit_boots")
def space_suit_boots(name):
    """Ботинки скафандра: белые голенища, серые ребристые подошвы, оранжевые ремни."""
    t = Tex()
    for x0 in (3, 17):
        m = mk(lambda d: (d.rectangle((x0 + 1, 10, x0 + 8, 24), fill=255), d.rectangle((x0 + 1, 18, x0 + 12, 24), fill=255)))
        solid(t, m, WHITE_PAINT, base=3, grain=0.15, seed=x0)
        sole = rect_mask(x0, 25, x0 + 12, 27)
        solid(t, sole, RUBBER, base=2, grain=0, seed=1)
        for x in range(x0 + 1, x0 + 12, 2):
            t.put(x, 27, RUBBER[4])
        t.hline(x0 + 1, x0 + 8, 10, STEEL[3])      # стыковочное кольцо
        t.hline(x0 + 1, x0 + 8, 11, STEEL[1])
        t.hline(x0 + 1, x0 + 8, 16, SUIT_ORANGE[2])
        t.put(x0 + 10, 20, SUIT_ORANGE[3])
    return done(t)


# ------------------------------------------------------------------ броня скафандра (развёртки 128×64)
# Раскладка UV ванильной брони 64×32, всё ×2. Грани коробки (x, y, w, h) в единицах 64×32:
# голова (0, 0): верх (8,0,8,8), низ (16,0,8,8), право (0,8,8,8), перед (8,8,8,8), лево (16,8,8,8), зад (24,8,8,8);
# нога (0, 16), тело (16, 16), рука (40, 16) — та же схема для коробок 4×12×4 и 8×12×4.
ARMOR_DIR = ("..", "..", "mod", "src", "main", "resources", "assets", "spacereloaded", "textures", "entity",
             "equipment")


def box_faces(u, v, w, h, d):
    """Грани коробки w×h×d с началом UV (u, v), в пикселях 128×64: {имя: (x0, y0, x1, y1)} включительно."""
    s = 2
    f = {
        "top": (u + d, v, w, d), "bottom": (u + d + w, v, w, d),
        "right": (u, v + d, d, h), "front": (u + d, v + d, w, h),
        "left": (u + d + w, v + d, d, h), "back": (u + 2 * d + w, v + d, w, h),
    }
    return {k: (x * s, y * s, (x + ww) * s - 1, (y + hh) * s - 1) for k, (x, y, ww, hh) in f.items()}


def suit_fill(t, r, seed, base=3):
    x0, y0, x1, y1 = r
    t.fill(WHITE_PAINT, base, x0, y0, x1, y1, grain=0.2, seed=seed)
    t.vline(x0, y0, y1, WHITE_PAINT[4])            # кромка ткани у ребра коробки
    t.vline(x1, y0, y1, WHITE_PAINT[1])


def band(t, r, y, h, pal, base=2):
    """Полоса поперёк грани на строках y…y+h−1 (пиксели 128×64): блик сверху, тень снизу."""
    x0, _, x1, _ = r
    t.rect(x0, y, x1, y + h - 1, pal[base])
    t.hline(x0, x1, y, pal[min(4, base + 1)])
    t.hline(x0, x1, y + h - 1, pal[max(0, base - 1)])


def corrugation(t, r, y0, y1):
    """Гофра сустава: чередование бликов и теней."""
    x0, _, x1, _ = r
    for y in range(y0, y1 + 1):
        t.hline(x0, x1, y, WHITE_PAINT[4] if (y - y0) % 2 == 0 else WHITE_PAINT[1])


def armor_suit():
    """Слой humanoid: торс с панелью СЖО и ранцем, рукава с гофрой локтя и перчатками, ботинки."""
    t = Tex(n=128)
    body = box_faces(16, 16, 8, 12, 4)
    for k in ("front", "back", "right", "left", "top"):
        suit_fill(t, body[k], seed=31 + len(k))
    x0, y0, x1, y1 = body["bottom"]
    t.fill(STEEL, 2, x0, y0, x1, y1, grain=0.1, seed=3)
    # плечи: шейное кольцо на верхней грани
    x0, y0, x1, y1 = body["top"]
    t.rect(x0 + 4, y0 + 2, x1 - 4, y1 - 1, STEEL[2])
    t.bevel(x0 + 4, y0 + 2, x1 - 4, y1 - 1, STEEL, base=2)
    t.rect(x0 + 6, y0 + 3, x1 - 6, y1 - 2, DARK_STEEL[1])
    for k in ("front", "back", "right", "left"):
        r = body[k]
        band(t, r, 56, 2, SUIT_ORANGE)              # оранжевая опояска
        band(t, r, 60, 4, STEEL)                    # поясной подшипник
        t.hline(r[0], r[2], 40, WHITE_PAINT[4])
    # перед: панель управления СЖО (DCM)
    x0, y0, x1, y1 = body["front"]
    t.rect(x0 + 3, 44, x1 - 3, 53, GUNMETAL[2])
    t.bevel(x0 + 3, 44, x1 - 3, 53, GUNMETAL, base=2)
    t.screen(x0 + 5, 46, x0 + 10, 50, SCREEN, rows=2)
    t.led(x1 - 7, 46, CYAN, True)
    t.led(x1 - 7, 50, SUIT_ORANGE, True)
    t.ao(x0 + 3, 54, x1 - 3, 55, k=0.2)
    for x in (x0 + 5, x1 - 5):                      # шланги к ранцу
        t.vline(x, 41, 43, STEEL[2])
    # зад: ранец PLSS — жёсткий кожух, решётка теплообменника, бирка
    x0, y0, x1, y1 = body["back"]
    t.panel(x0 + 2, 41, x1 - 2, 55, WHITE_PAINT, base=2, grain=0.1, seed=9)
    t.grille(x0 + 5, 44, x1 - 5, 49, DARK_STEEL, pitch=2)
    t.rect(x0 + 5, 51, x0 + 8, 53, SUIT_ORANGE[2])
    t.screw(x0 + 3, 42, STEEL)
    t.screw(x1 - 5, 42, STEEL)
    # бока: шов
    for k in ("right", "left"):
        x0, y0, x1, y1 = body[k]
        t.vline((x0 + x1) // 2, 42, 55, WHITE_PAINT[1])

    arm = box_faces(40, 16, 4, 12, 4)
    for k in ("front", "back", "right", "left", "top"):
        suit_fill(t, arm[k], seed=41 + len(k))
    x0, y0, x1, y1 = arm["top"]
    t.ring((x0 + x1 + 1) / 2, (y0 + y1 + 1) / 2, 2, 4, STEEL, base=2)
    x0, y0, x1, y1 = arm["bottom"]
    t.fill(WHITE_PAINT, 1, x0, y0, x1, y1, grain=0.1, seed=5)   # ладонь перчатки
    t.rect(x0 + 2, y0 + 2, x1 - 2, y1 - 2, RUBBER[3])
    for k in ("front", "back", "right", "left"):
        r = arm[k]
        band(t, r, 46, 2, STEEL)                    # плечевой подшипник
        corrugation(t, r, 51, 54)                   # локоть
        band(t, r, 56, 2, SUIT_ORANGE)              # манжета
        band(t, r, 58, 2, STEEL)                    # кольцо перчатки
        t.fill(WHITE_PAINT, 1, r[0], 60, r[2], 63, grain=0.1, seed=6)
        for x in range(r[0] + 1, r[2], 2):          # пальцы
            t.put(x, 63, WHITE_PAINT[0])

    # ботинки: нижняя часть ноги (слой humanoid для слота ног), выше — прозрачно
    leg = box_faces(0, 16, 4, 12, 4)
    for k in ("front", "back", "right", "left"):
        x0, y0, x1, y1 = leg[k]
        suit_fill(t, (x0, 50, x1, 63), seed=51 + len(k))
        band(t, leg[k], 50, 2, STEEL)               # кольцо стыковки со штаниной
        band(t, leg[k], 55, 2, SUIT_ORANGE)         # ремень
        band(t, leg[k], 61, 3, RUBBER)              # рант подошвы
    x0, y0, x1, y1 = leg["bottom"]
    t.fill(RUBBER, 2, x0, y0, x1, y1, grain=0.1, seed=7)
    for y in range(y0 + 1, y1, 2):                  # протектор
        t.hline(x0 + 1, x1 - 1, y, RUBBER[4])
    x0, y0, x1, y1 = leg["front"]
    t.rect(x0 + 2, 57, x1 - 2, 59, WHITE_PAINT[4])  # носок
    return t


def armor_leggings():
    """Слой humanoid_leggings: белые штанины с гофрой колен, оранжевый пояс и поясной подшипник."""
    t = Tex(n=128)
    leg = box_faces(0, 16, 4, 12, 4)
    for k in ("front", "back", "right", "left", "top"):
        suit_fill(t, leg[k], seed=61 + len(k))
    for k in ("front", "back", "right", "left"):
        r = leg[k]
        band(t, r, 41, 2, STEEL)                    # бедренный подшипник
        corrugation(t, r, 49, 53)                   # колено
        band(t, r, 58, 2, STEEL)                    # кольцо к ботинку
    x0, y0, x1, y1 = leg["front"]
    t.vline((x0 + x1) // 2, 43, 48, WHITE_PAINT[1])
    t.rect(x0 + 2, 44, x1 - 2, 46, SUIT_ORANGE[2])  # карман-метка
    body = box_faces(16, 16, 8, 12, 4)
    for k in ("front", "back", "right", "left"):
        r = body[k]
        suit_fill(t, (r[0], 52, r[2], 63), seed=71 + len(k))
        band(t, r, 52, 3, SUIT_ORANGE)              # пояс
        band(t, r, 60, 2, STEEL)
    x0, y0, x1, y1 = body["front"]
    t.rect((x0 + x1) // 2 - 2, 52, (x0 + x1) // 2 + 1, 54, STEEL[3])   # пряжка
    t.put((x0 + x1) // 2 - 2, 52, STEEL[4])
    return t


def armor_mask():
    """Слой humanoid маски: голубой щиток на глазах, чашка с оранжевым клапаном, резиновый ремень вокруг головы."""
    t = Tex(n=128)
    head = box_faces(0, 0, 8, 8, 8)
    for k in ("right", "left", "back"):
        x0, y0, x1, y1 = head[k]
        band(t, head[k], 22, 3, RUBBER)             # ремень
        for x in range(x0 + 1, x1, 3):
            t.put(x, 23, RUBBER[4])
    for k, edge in (("right", "hi"), ("left", "lo")):   # крепление щитка у лицевой кромки
        x0, y0, x1, y1 = head[k]
        xs = (x1 - 3, x1) if k == "right" else (x0, x0 + 3)
        t.rect(xs[0], 20, xs[1], 26, STEEL[2])
        t.bevel(xs[0], 20, xs[1], 26, STEEL, base=2)
    x0, y0, x1, y1 = head["back"]
    t.rect((x0 + x1) // 2 - 2, 21, (x0 + x1) // 2 + 1, 25, STEEL[3])   # пряжка
    x0, y0, x1, y1 = head["front"]
    # щиток
    t.rect(x0, 20, x1, 26, STEEL[2])
    t.bevel(x0, 20, x1, 26, STEEL, base=2)
    for y in range(21, 26):
        t.hline(x0 + 1, x1 - 1, y, CYAN[3] if y < 23 else CYAN[2])
    t.hline(x0 + 2, x0 + 6, 21, CYAN[4])
    t.vline((x0 + x1) // 2, 21, 25, STEEL[2])        # перемычка
    # чашка маски
    t.rect(x0 + 3, 27, x1 - 3, 31, STEEL[3])
    t.bevel(x0 + 3, 27, x1 - 3, 31, STEEL, base=3)
    t.disc((x0 + x1 + 1) / 2, 29.5, 2.2, SUIT_ORANGE, base=2)
    t.put((x0 + x1) // 2, 29, DARK_STEEL[1])
    return t


def write_armor():
    import os
    root = os.path.join(os.path.dirname(__file__), *ARMOR_DIR)
    out = {"humanoid/space_suit.png": armor_suit(), "humanoid/oxygen_mask.png": armor_mask(),
           "humanoid_leggings/space_suit.png": armor_leggings()}
    for rel, t in out.items():
        t.img.crop((0, 0, 128, 64)).save(os.path.join(root, rel))
        print("записано:", rel)


if __name__ == "__main__":
    import sys
    if "armor" in sys.argv[1:]:
        write_armor()
