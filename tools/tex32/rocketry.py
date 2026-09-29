"""Ракетная техника: баки, двигатели, корпуса, капсулы, стыковка, стартовый стол, спутники, пушка, ЦУП.

Многие модели берут из текстуры только полосу (двигатели, капсула, ЦУП, пушка, спутники) —
раскладка деталей по строкам/столбцам повторяет uv граней (в пикселях 32 = uv × 2).
"""
import math

from . import tex
from .kit import (AMBER, BLACK, BLUE, BRASS, COPPER, CYAN, DARK_STEEL, GLASS, GOLD_FOIL, GREEN, GUNMETAL,
                  HAZARD_YELLOW, RED, SCREEN, STEEL, TITANIUM, WHITE_PAINT, ALUMINIUM, Tex, _h, mix, shade)

# ------------------------------------------------------------------ свои палитры
# грунт баков из Al-Li (светло-зелёный, как у прежней текстуры)
PRIMER = [(0x6A, 0x7E, 0x62), (0x88, 0x9E, 0x7E), (0xA6, 0xBC, 0x9A), (0xC2, 0xD6, 0xB4), (0xDC, 0xEC, 0xCE)]
# абляционная теплозащита (AVCOAT: коричневая, соты)
ABLATIVE = [(0x2A, 0x19, 0x10), (0x42, 0x29, 0x1A), (0x5A, 0x39, 0x24), (0x74, 0x4E, 0x33), (0x92, 0x68, 0x47)]
# обивка кресла
SEAT = [(0x10, 0x1C, 0x36), (0x18, 0x2A, 0x4E), (0x22, 0x3A, 0x68), (0x34, 0x52, 0x88), (0x5C, 0x7C, 0xB0)]
# ниобиевый насадок сопла (тёмный, с побежалостью)
NIOBIUM = [(0x22, 0x20, 0x24), (0x34, 0x30, 0x36), (0x4A, 0x44, 0x4A), (0x64, 0x5C, 0x62), (0x8A, 0x80, 0x84)]


# ------------------------------------------------------------------ общие хелперы
def plate(t, pal, base=2, grain=None, seed=31, x0=0, y0=0, x1=31, y1=31):
    """Лист с фаской по краю грани (тёмные листы — с более мелким зерном: пятна на них заметнее)."""
    if grain is None:
        grain = 0.12 if pal in (DARK_STEEL, GUNMETAL, BLACK) else 0.2
    t.panel(x0, y0, x1, y1, pal, base=base, grain=grain, seed=seed)


def cyl_v(t, x0, x1, y0, y1, pal, base=2, tubes=0, seed=41):
    """Вертикальный цилиндр (или его развёртка): освещение поперёк, опционально трубки охлаждения
    с шагом tubes px."""
    w = max(1, x1 - x0)
    for x in range(x0, x1 + 1):
        s = (x - x0) / w
        k = base + (1 if 0.12 < s < 0.34 else -1 if s > 0.72 else 0)
        if x == x0:
            k = base
        if x == x1:
            k = base - 2
        k = max(0, min(4, k))
        for y in range(y0, y1 + 1):
            c = pal[k]
            if tubes and (x - x0) % tubes == tubes - 1 and x0 < x < x1:
                c = mix(c, pal[max(0, k - 1)], 0.7)
            if _h(x, y // 3, seed) > 0.93:
                c = mix(c, pal[min(4, k + 1)], 0.4)
            t.put(x, y, c)


def flange_h(t, x0, x1, y, pal):
    """Горизонтальный фланец 2 px: блик сверху, тень снизу."""
    t.hline(x0, x1, y, pal[4])
    t.hline(x0, x1, y + 1, pal[1])


def bolt_row(t, xs, y, pal=STEEL):
    for x in xs:
        t.rivet(x, y, pal)


def sym_xs(*xs):
    """Координаты x и их зеркала относительно центра (под деталь шириной 2)."""
    out = []
    for x in xs:
        out += [x, 30 - x]
    return sorted(set(out))


def hazard_border(t, width=4):
    t.hazard(0, 0, 31, width - 1)
    t.hazard(0, 32 - width, 31, 31)
    t.hazard(0, width, width - 1, 31 - width)
    t.hazard(32 - width, width, 31, 31 - width)
    t.bevel(0, 0, 31, 31, BLACK, raised=True, base=2)
    t.bevel(width, width, 31 - width, 31 - width, BLACK, raised=False, base=2)


def lens(t, cx, cy, r, pal, spec=True):
    """Стекло/линза: тёмная середина, блик сверху-слева."""
    t.disc(cx, cy, r, pal, base=1)
    if spec:
        t.put(int(cx - r * 0.45), int(cy - r * 0.45), pal[4])
        t.put(int(cx - r * 0.45) + 1, int(cy - r * 0.45), pal[3])


# ------------------------------------------------------------------ баки
TANKS = {
    "fuel_tank": (WHITE_PAINT, 2, "paint"),
    "aluminium_fuel_tank": (ALUMINIUM, 2, "bare"),
    "al_li_fuel_tank": (PRIMER, 2, "isogrid"),
}


def tank_body(t, pal, base, kind, seed):
    """Обечайка бака (cube_column: блоки ставятся в столбик): сварные пояса сверху и снизу,
    лёгкая цилиндрическая светотень по краям."""
    if kind == "bare":
        t.brushed(pal, base, 0, 0, 31, 31, horizontal=False, seed=seed)
    else:
        t.fill(pal, base, grain=0.06, seed=seed)
    if kind == "isogrid":
        # вафельный изогрид Al-Li — треугольная сетка рёбер, едва заметная
        for y in range(4, 28):
            for x in range(32):
                if (y - 4) % 8 == 0 or (x + y) % 16 == 0 or (x - y) % 16 == 0:
                    t.blend(x, y, pal[base - 1], 0.35)
    # цилиндр: блик слева, тень справа
    for y in range(32):
        for x, k in ((0, -0.18), (1, -0.1), (4, 0.08), (5, 0.1), (6, 0.08), (29, -0.1), (30, -0.16), (31, -0.24)):
            if k > 0:
                t.lighten(x, y, k)
            else:
                t.darken(x, y, -k)
    # сварные кольцевые пояса
    for y0 in (0, 29):
        for x in range(32):
            t.blend(x, y0, pal[base - 1], 0.6)
            t.blend(x, y0 + 1, pal[min(4, base + 1)], 0.5)
            t.blend(x, y0 + 2, pal[base - 1], 0.35)
    if kind in ("bare", "paint"):
        for x in range(2, 31, 4):
            t.put(x, 1, pal[4])
            t.put(x, 30, pal[4])
            t.put(x + 1, 1, pal[1])
            t.put(x + 1, 30, pal[1])


def tank_gauge(t, level, pal):
    """Смотровое окно уровня по центру грани: кронштейн, стекло, топливо снизу доля level/4,
    риски четвертей с обеих сторон (симметрично)."""
    t.ao(10, 4, 21, 27, k=0.2)
    t.panel(11, 5, 20, 26, STEEL, base=2, grain=0.1, seed=43)
    t.recess(13, 7, 18, 24, GLASS, base=1)
    inner = 16                                   # строки 8…23
    fill = round(inner * level / 4)
    for i in range(fill):
        y = 23 - i
        c = AMBER[3] if i < fill - 1 else AMBER[4]
        t.hline(14, 17, y, c)
        t.put(14, y, mix(c, (255, 255, 255), 0.25))
        t.put(17, y, AMBER[2])
    if fill:
        t.glow(15.5, 24 - fill / 2, 6, AMBER[3], 0.10)
    # блик стекла
    for y in range(8, 23, 1):
        if fill and y > 23 - fill:
            continue
        t.put(14, y, GLASS[3])
    t.put(14, 8, GLASS[4])
    # риски: 0, ¼, ½, ¾, 1
    for q in range(5):
        y = 23 - round(q * (inner - 1) / 4)
        t.put(12, y, STEEL[0])
        t.put(19, y, STEEL[0])
    # болты кронштейна
    t.put(12, 6, STEEL[4])
    t.put(19, 6, STEEL[4])
    t.put(12, 25, STEEL[1])
    t.put(19, 25, STEEL[1])


def _tank_side(name):
    base_name = name.split("/")[1].rsplit("_side", 1)[0]
    pal, base, kind = TANKS[base_name]
    t = Tex()
    tank_body(t, pal, base + 1 if kind == "paint" else base, kind, seed=len(base_name))
    if name[-1].isdigit():
        tank_gauge(t, int(name[-1]), pal)
    else:
        # бак без датчика: промежуточный шпангоут по центру (строки 15–16) с заклёпками
        for x in range(32):
            t.blend(x, 15, pal[0], 0.45)
            t.blend(x, 16, pal[4], 0.5)
        for x in range(2, 31, 4):
            t.put(x, 15, STEEL[3])
            t.put(x + 1, 15, STEEL[1])
    return t


tex(*[f"block/{b}_side_{i}" for b in TANKS for i in range(5)], "block/fuel_tank_side")(_tank_side)


def weld(t, r, pal):
    """Кольцевой сварной шов днища: тёмная линия, под ней (снизу-справа) — светлая."""
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - 16, y + 0.5 - 16
            d = math.hypot(dx, dy)
            if abs(d - r) < 0.55:
                t.blend(x, y, pal[0], 0.55)
            elif abs(d - r - 1) < 0.5:
                t.blend(x, y, pal[4], 0.5)


@tex("block/fuel_tank_top", "block/aluminium_fuel_tank_top", "block/al_li_fuel_tank_top")
def tank_top(name):
    """Днище бака: кольцевые швы, центральный фланец заправочного патрубка на 8 болтах."""
    pal, base, kind = TANKS[name.split("/")[1].rsplit("_top", 1)[0]]
    t = Tex()
    b = base if kind == "paint" else base - 1
    t.fill(pal, b + (1 if kind == "paint" else 0), grain=0.06, seed=47)
    t.bevel(0, 0, 31, 31, pal, raised=True, base=b + 1)
    weld(t, 12.5, pal)
    weld(t, 8, pal)
    t.disc(16, 16, 5.2, STEEL, base=2)
    t.disc(16, 16, 2.6, DARK_STEEL, base=1, lit=False)
    t.put(15, 15, DARK_STEEL[3])
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        x, y = 16 + 4.0 * math.cos(a), 16 + 4.0 * math.sin(a)
        t.put(int(x), int(y), STEEL[4] if y < 16 else STEEL[1])
    return t


# ------------------------------------------------------------------ двигатели
ENGINES = {
    # керосин: медная камера, тёмный ниобиевый насадок, копоть
    "rocket_engine": dict(mount=GUNMETAL, throat=COPPER, bell=NIOBIUM, tint=(0x6A, 0x3A, 0x1C), flame=AMBER),
    # водород: светлая нержавейка, синие трубки охлаждения (RS-25)
    "hydrolox_engine": dict(mount=STEEL, throat=TITANIUM, bell=ALUMINIUM, tint=(0x3C, 0x6E, 0xC4), flame=CYAN),
    # метан: тёмная сталь, синевато-фиолетовая побежалость (Raptor)
    "methalox_engine": dict(mount=GUNMETAL, throat=STEEL, bell=TITANIUM, tint=(0x4A, 0x44, 0x9C), flame=BLUE),
}


@tex("block/rocket_engine_side", "block/hydrolox_engine_side", "block/methalox_engine_side")
def engine_side(name):
    """Развёртка двигателя по uv модели: 0–7 — опорная плита (и её низ), 8–15 (x 10–21) — горловина,
    16–23 (x 6–25) — верх раструба, 24–31 (x 2–29) — срез раструба."""
    e = ENGINES[name.split("/")[1][:-5]]
    t = Tex()
    t.fill(e["mount"], 1, grain=0.2, seed=51)
    # опорная плита
    plate(t, e["mount"], base=2, grain=0.2, seed=52, y1=7)
    bolt_row(t, sym_xs(3, 9), 3, STEEL)
    t.hline(13, 18, 3, e["mount"][0])
    t.hline(13, 18, 4, e["mount"][3])
    # горловина: камера сгорания с фланцами
    cyl_v(t, 10, 21, 8, 15, e["throat"], base=2)
    flange_h(t, 10, 21, 8, e["throat"])
    flange_h(t, 10, 21, 14, e["throat"])
    t.ao(10, 8, 21, 9, k=0.25)
    # раструб: трубки охлаждения, к срезу — цвет побежалости
    cyl_v(t, 6, 25, 16, 23, e["bell"], base=3, tubes=2)
    cyl_v(t, 2, 29, 24, 31, e["bell"], base=2, tubes=2, seed=53)
    for y in range(16, 32):
        k = (y - 16) / 15
        x0, x1 = (6, 25) if y < 24 else (2, 29)
        for x in range(x0, x1 + 1):
            t.blend(x, y, e["tint"], 0.12 + 0.38 * k * k)
    flange_h(t, 6, 25, 16, e["bell"])
    t.hline(2, 29, 24, e["bell"][4])
    t.hline(2, 29, 31, e["bell"][0])
    t.ao(6, 16, 25, 17, k=0.2)
    return t


@tex("block/rocket_engine_bottom", "block/hydrolox_engine_bottom", "block/methalox_engine_bottom")
def engine_bottom(name):
    """Вид в сопло снизу: кромка среза, трубки охлаждения радиально, в глубине — горловина с отсветом."""
    e = ENGINES[name.split("/")[1][:-7]]
    t = Tex()
    t.fill(e["mount"], 1, grain=0.2, seed=54)
    cx = cy = 16
    for y in range(32):
        for x in range(32):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            d = math.hypot(dx, dy)
            if d > 14:
                continue
            # стенка сопла темнеет вглубь
            k = 3 if d > 12.8 else (2 if d > 9 else (1 if d > 5 else 0))
            c = e["bell"][k]
            if d > 12.8 and -(dx + dy) < 0:
                c = e["bell"][2]
            ang = math.atan2(dy, dx)
            if d < 12.8 and int((ang + math.pi) / (2 * math.pi) * 36) % 2 == 0:
                c = mix(c, e["bell"][max(0, k - 1)], 0.55)
            c = mix(c, e["tint"], 0.25 * (1 - d / 14))
            t.put(x, y, c)
    t.ring(cx, cy, 13.2, 14.2, e["bell"], base=3)
    # горловина
    t.disc(cx, cy, 4.2, BLACK, base=1, lit=False)
    t.disc(cx, cy, 2.6, e["flame"], base=2, lit=False)
    t.put(15, 15, e["flame"][4])
    t.put(16, 15, e["flame"][3])
    t.glow(cx, cy, 7, e["flame"][3], 0.35)
    return t


# ------------------------------------------------------------------ корпуса
@tex("block/rocket_hull")
def rocket_hull(name):
    """Обшивка ракеты: белые панели, стрингерные швы, оранжевый пояс снизу (стыкуясь, блоки дают полосы)."""
    t = Tex()
    t.fill(WHITE_PAINT, 3, grain=0.06, seed=61)
    for x in (0, 15, 16, 31):
        pass
    t.vline(15, 0, 31, WHITE_PAINT[1])
    t.vline(16, 0, 31, WHITE_PAINT[4])
    t.hline(0, 31, 0, WHITE_PAINT[4])
    t.hline(0, 31, 31, WHITE_PAINT[1])
    for x in range(3, 31, 4):
        t.put(x, 2, WHITE_PAINT[1])
    # пояс
    for y in range(23, 29):
        t.hline(0, 31, y, AMBER[3] if y not in (23, 28) else (AMBER[4] if y == 23 else AMBER[1]))
    for x in range(3, 31, 4):
        t.put(x, 25, AMBER[2])
        t.put(x, 26, AMBER[2])
    return t


@tex("block/module_hull")
def module_hull(name):
    """Корпус станционного модуля: белые панели противометеорной защиты, швы и ряды заклёпок."""
    t = Tex()
    t.fill(WHITE_PAINT, 3, grain=0.06, seed=62)
    for y0, y1 in ((0, 15), (16, 31)):
        for x0, x1 in ((0, 15), (16, 31)):
            t.bevel(x0, y0, x1, y1, WHITE_PAINT, raised=True, base=2)
            t.rivet(x0 + 2, y0 + 2, STEEL)
            t.rivet(x1 - 3, y0 + 2, STEEL)
            t.rivet(x0 + 2, y1 - 3, STEEL)
            t.rivet(x1 - 3, y1 - 3, STEEL)
    for y in (7, 8, 23, 24):
        for x in range(5, 27):
            if x not in (14, 15, 16, 17):
                t.blend(x, y, WHITE_PAINT[0], 0.15)
    return t


@tex("block/hull_plating")
def hull_plating(name):
    """Броневая обшивка: четыре листа, распорка «Х» под листом читается выпуклыми рёбрами."""
    t = Tex()
    t.fill(STEEL, 2, grain=0.14, seed=63)
    for i in range(3, 29):
        for d in (0, 1):
            t.blend(i + d, i, STEEL[3], 0.45)
            t.blend(31 - i - d, i, STEEL[3], 0.45)
            t.blend(i + d, i + 1, STEEL[1], 0.35)
            t.blend(31 - i - d, i + 1, STEEL[1], 0.35)
    t.bevel(0, 0, 31, 31, STEEL, raised=True, base=2)
    t.bevel(1, 1, 30, 30, STEEL, raised=False, base=2)
    for x in (4, 26):
        for y in (4, 26):
            t.screw(x, y, STEEL)
    for x in (15,):
        t.screw(x, 3, STEEL)
        t.screw(x, 26, STEEL)
        t.screw(3, 15, STEEL)
        t.screw(26, 15, STEEL)
    return t


@tex("block/vent_grate")
def vent_grate(name):
    """Решётка вентиляции: рамка и наклонные ламели."""
    t = Tex()
    plate(t, GUNMETAL, base=2, seed=64)
    t.recess(3, 3, 28, 28, BLACK, base=1)
    for y in range(4, 28, 4):
        t.hline(4, 27, y, GUNMETAL[4])
        t.hline(4, 27, y + 1, GUNMETAL[3])
        t.hline(4, 27, y + 2, GUNMETAL[1])
    t.vline(15, 4, 27, GUNMETAL[2])
    t.vline(16, 4, 27, GUNMETAL[1])
    t.rivets(inset=1, pal=STEEL)
    return t


# ------------------------------------------------------------------ капсулы
@tex("block/command_module")
def command_module(name):
    """Командный модуль (конус из 4 ступеней, каждая берёт свою полосу): 24–31 — нижний пояс с кромкой
    теплозащиты, 16–23 — иллюминаторы, 9–15 — двигатели ориентации, 3–8 — вершина; верх/низ — белый."""
    t = Tex()
    t.fill(WHITE_PAINT, 3, grain=0.06, seed=65)
    # швы ступеней
    for y in (8, 15, 23):
        t.hline(0, 31, y, WHITE_PAINT[1])
        t.hline(0, 31, y + 1, WHITE_PAINT[4])
    # нижний пояс: теплозащита по кромке
    for y in range(29, 32):
        t.hline(0, 31, y, ABLATIVE[2 if y < 31 else 1])
    t.hline(0, 31, 29, ABLATIVE[3])
    bolt_row(t, sym_xs(4, 10), 26, STEEL)
    # иллюминаторы (полоса 17–22, видимая ширина 2–29)
    for x0 in (7, 20):
        t.recess(x0, 17, x0 + 4, 21, BLACK, base=1)
        t.rect(x0 + 1, 18, x0 + 3, 20, GLASS[2])
        t.put(x0 + 1, 18, GLASS[4])
        t.put(x0 + 2, 18, GLASS[3])
    t.recess(13, 17, 18, 22, BLACK, base=1)
    t.rect(14, 18, 17, 21, CYAN[1])
    t.put(14, 18, CYAN[4])
    t.put(15, 18, CYAN[3])
    # двигатели ориентации (полоса 10–14)
    for x in (9, 15, 21):
        t.rect(x, 11, x + 1, 13, DARK_STEEL[1])
        t.put(x, 11, DARK_STEEL[3])
    # вершина: стыковочный пояс (полоса 3–7)
    for x in range(8, 24, 3):
        t.put(x, 5, STEEL[2])
        t.put(x, 6, STEEL[1])
    return t


@tex("block/return_capsule_side")
def return_capsule_side(name):
    """Бок спускаемой капсулы: белый корпус, два иллюминатора, внизу — толстый слой абляционной защиты."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.06, seed=66)
    t.bevel(0, 0, 31, 31, WHITE_PAINT, raised=True, base=2)
    for x0 in (6, 21):
        t.disc(x0 + 2.5, 9.5, 3.4, STEEL, base=2)
        lens(t, x0 + 2.5, 9.5, 2.3, GLASS)
    t.hline(2, 29, 16, WHITE_PAINT[1])
    t.hline(2, 29, 17, WHITE_PAINT[4])
    bolt_row(t, sym_xs(3, 9, 15), 18, STEEL)
    # теплозащита: соты AVCOAT
    honeycomb(t, 0, 22, 31, 31, char=lambda x, y: 0.1 + 0.3 * (y - 22) / 9)
    t.hline(0, 31, 22, ABLATIVE[4])
    t.hline(0, 31, 21, WHITE_PAINT[0])
    return t


@tex("block/return_capsule_top")
def return_capsule_top(name):
    """Верх капсулы: люк с окном, парашютный отсек (крышки по бокам)."""
    t = Tex()
    plate(t, WHITE_PAINT, base=2, grain=0.06, seed=68)
    t.disc(16, 16, 10, STEEL, base=2)
    t.ring(16, 16, 8.5, 10, STEEL, base=3)
    t.disc(16, 16, 5.5, DARK_STEEL, base=2)
    lens(t, 16, 16, 3.8, CYAN)
    for a in range(6):
        ang = a * math.pi / 3
        x, y = 16 + 9.2 * math.cos(ang), 16 + 9.2 * math.sin(ang)
        t.put(int(x), int(y), STEEL[4] if y < 16 else STEEL[1])
    for x0 in (2, 26):
        t.panel(x0, 12, x0 + 3, 19, WHITE_PAINT, base=3)
    return t


def honeycomb(t, x0, y0, x1, y1, char=lambda x, y: 0.0, seed=69):
    """Соты абляционного экрана: ячейки 3×3 со сдвигом рядов, стенки чуть темнее заполнителя,
    тон заполнителя плавает пятнами; char(x, y) — доля обугливания (подмешивание чёрного)."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            row = y // 3
            xs = x + (row % 2)
            h = _h(xs // 6, row // 2, seed) * 0.6 + _h(xs // 3, row, seed + 1) * 0.4
            c = mix(ABLATIVE[2], ABLATIVE[3], h)
            if y % 3 == 0 or xs % 3 == 0:
                c = mix(c, ABLATIVE[0], 0.45)
            elif y % 3 == 1 and xs % 3 == 1:
                c = mix(c, ABLATIVE[4], 0.25)
            t.put(x, y, mix(c, BLACK[0], max(0.0, min(0.8, char(x, y)))))


@tex("block/return_capsule_bottom")
def return_capsule_bottom(name):
    """Лобовой экран: абляционные соты, к центру (точка торможения) обуглены сильнее."""
    t = Tex()
    honeycomb(t, 0, 0, 31, 31, char=lambda x, y: 0.45 - math.hypot(x + 0.5 - 16, y + 0.5 - 16) / 36)
    t.bevel(0, 0, 31, 31, ABLATIVE, raised=True, base=2)
    return t


@tex("block/rocket_seat")
def rocket_seat(name):
    """Ложемент: синяя стёганая обивка (горизонтальные валики), кант по краю; спинка — строки 0–17,
    подушка — 18–31."""
    t = Tex()
    t.fill(SEAT, 2, grain=0.15, seed=70)
    for y0 in range(1, 31, 5):
        for x in range(2, 30):
            t.put(x, y0, SEAT[3])
            t.put(x, y0 + 3, SEAT[1])
            t.put(x, y0 + 4, SEAT[0])
    # шов между спинкой и подушкой
    t.hline(0, 31, 17, SEAT[0])
    t.hline(0, 31, 18, SEAT[4])
    # кант
    t.bevel(0, 0, 31, 31, DARK_STEEL, raised=True, base=2)
    t.vline(1, 1, 30, SEAT[1])
    t.vline(30, 1, 30, SEAT[1])
    # ремни по центру
    for x in (11, 20):
        t.vline(x, 1, 30, BLACK[3])
        t.vline(x + 1, 1, 30, BLACK[2])
    t.rect(10, 21, 22, 23, STEEL[2])
    t.hline(10, 22, 21, STEEL[4])
    t.hline(10, 22, 23, STEEL[1])
    return t


# ------------------------------------------------------------------ стыковка и шлюзы
@tex("block/hermetic_hatch")
def hermetic_hatch(name):
    """Гермолюк: полоса «осторожно» по периметру (её берёт рамка открытого люка), в центре —
    крышка с крестовиной и штурвалом."""
    t = Tex()
    hazard_border(t)
    t.panel(4, 4, 27, 27, STEEL, base=2, seed=71)
    for i in range(6, 26):
        for d in (0, 1):
            t.put(i + d, i, STEEL[3])
            t.put(31 - i - d, i, STEEL[3])
        t.put(i, i + 1, STEEL[1])
        t.put(31 - i, i + 1, STEEL[1])
    t.ring(16, 16, 5, 6.6, DARK_STEEL, base=3)
    for dx, dy in ((0, -1), (0, 1), (-1, 0), (1, 0)):
        for r in range(2, 6):
            t.put(16 + dx * r - (dx < 0), 16 + dy * r - (dy < 0), DARK_STEEL[3])
    t.disc(16, 16, 2.4, STEEL, base=3)
    t.ao(4, 4, 27, 27, k=0.3)
    return t


@tex("block/hermetic_hatch_cycling")
def hermetic_hatch_cycling(name):
    """Люк в цикле шлюзования: жёлтая мигалка в центре."""
    t = hermetic_hatch(name)
    t.disc(16, 16, 5, AMBER, base=3)
    t.disc(16, 16, 3, AMBER, base=4, lit=False)
    t.glow(16, 16, 11, AMBER[4], 0.35)
    return t


def open_frame(t):
    """Рамка 4 px вокруг прозрачного проёма (открытый люк/порт)."""
    hazard_border(t)
    t.img.paste((0, 0, 0, 0), (4, 4, 28, 28))


@tex("block/hermetic_hatch_open")
def hermetic_hatch_open(name):
    t = Tex()
    open_frame(t)
    return t


@tex("block/docking_port_front")
def docking_port_front(name):
    """Андрогинный стыковочный агрегат: кольцо с тремя направляющими лепестками, крышка по центру."""
    t = Tex()
    hazard_border(t)
    t.fill(DARK_STEEL, 2, 4, 4, 27, 27, grain=0.2, seed=72)
    t.ring(16, 16, 9, 12, ALUMINIUM, base=2)
    t.ring(16, 16, 8, 9, DARK_STEEL, base=1)
    for a in (-math.pi / 2, math.pi / 6, 5 * math.pi / 6):
        for r in range(8, 12):
            x, y = 16 + r * math.cos(a), 16 + r * math.sin(a)
            t.rect(int(x) - 1, int(y) - 1, int(x), int(y), ALUMINIUM[4] if r < 10 else ALUMINIUM[3])
    t.disc(16, 16, 6.5, STEEL, base=2)
    t.disc(16, 16, 2, DARK_STEEL, base=1)
    for a in range(4):
        ang = a * math.pi / 2 + math.pi / 4
        t.put(int(16 + 4.5 * math.cos(ang)), int(16 + 4.5 * math.sin(ang)), STEEL[4])
    return t


@tex("block/docking_port_open")
def docking_port_open(name):
    t = Tex()
    open_frame(t)
    return t


@tex("block/docking_clamp_side")
def docking_clamp_side(name):
    """Захват стыковки (колонна): стальные щёки, по центру — полоса «осторожно» с замками."""
    t = Tex()
    plate(t, STEEL, base=2, seed=73)
    t.hazard(0, 13, 31, 18, width=3)
    t.hline(0, 31, 12, STEEL[1])
    t.hline(0, 31, 19, STEEL[4])
    for x in (6, 22):
        t.panel(x, 11, x + 3, 20, DARK_STEEL, base=2)
    t.rivets(inset=3, pal=STEEL)
    t.grille(12, 3, 19, 9, DARK_STEEL, pitch=2)
    t.grille(12, 22, 19, 28, DARK_STEEL, pitch=2)
    return t


@tex("block/docking_clamp_end")
def docking_clamp_end(name):
    """Торец захвата: кольцо с голубым лазерным маяком наведения."""
    t = Tex()
    plate(t, STEEL, base=2, seed=74)
    t.ring(16, 16, 8, 12, DARK_STEEL, base=2)
    t.ring(16, 16, 11, 12.5, STEEL, base=3)
    t.disc(16, 16, 5, BLACK, base=1)
    lens(t, 16, 16, 3, CYAN)
    t.glow(16, 16, 7, CYAN[4], 0.3)
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):
        t.screw(x, y, STEEL)
    return t


@tex("block/hermetic_glass")
def hermetic_glass(name):
    """Гермостекло: полупрозрачное, толстая рамка на винтах, двойной блик по диагонали."""
    t = Tex()
    t.img.paste(GLASS[3] + (60,), (0, 0, 32, 32))
    for i in range(3, 29):
        for d in range(3):
            t.put(i + d - 4, i, GLASS[4], 110 if d != 1 else 150)
        t.put(i + 5, i, GLASS[4], 90)
    t.fill(STEEL, 2, 0, 0, 31, 1, grain=0.2)
    t.fill(STEEL, 2, 0, 30, 31, 31, grain=0.2)
    t.fill(STEEL, 2, 0, 2, 1, 29, grain=0.2)
    t.fill(STEEL, 2, 30, 2, 31, 29, grain=0.2)
    t.bevel(0, 0, 31, 31, STEEL, raised=True, base=2)
    t.bevel(1, 1, 30, 30, STEEL, raised=False, base=2)
    for x in (4, 15, 26):
        t.put(x, 0, STEEL[4])
        t.put(x, 31, STEEL[1])
        t.put(0, x, STEEL[4])
        t.put(31, x, STEEL[1])
    return t


@tex("block/stage_separator_side")
def stage_separator_side(name):
    """Кольцо разделения ступеней: тёмный шпангоут, по центру — ряд пироболтов (оранжевые головки)."""
    t = Tex()
    plate(t, GUNMETAL, base=2, seed=75)
    t.recess(0, 11, 31, 20, DARK_STEEL, base=1)
    for x in range(2, 31, 6):
        t.panel(x, 13, x + 3, 18, AMBER, base=2)
        t.put(x + 1, 15, AMBER[4])
    t.hline(0, 31, 4, GUNMETAL[1])
    t.hline(0, 31, 5, GUNMETAL[3])
    t.hline(0, 31, 26, GUNMETAL[1])
    t.hline(0, 31, 27, GUNMETAL[3])
    return t


@tex("block/stage_separator_end")
def stage_separator_end(name):
    """Торец разделителя: крестовина толкателей, по центру — пружинный толкатель."""
    t = Tex()
    plate(t, GUNMETAL, base=1, seed=76)
    t.rect(13, 1, 18, 30, GUNMETAL[2])
    t.rect(1, 13, 30, 18, GUNMETAL[2])
    t.bevel(13, 1, 18, 30, GUNMETAL, raised=True, base=2)
    t.bevel(1, 13, 30, 18, GUNMETAL, raised=True, base=2)
    t.rect(14, 13, 17, 18, GUNMETAL[2])
    t.panel(11, 11, 20, 20, DARK_STEEL, base=1)
    t.disc(15.5, 15.5, 3, AMBER, base=2)
    for x, y in ((4, 4), (25, 4), (4, 25), (25, 25)):
        t.rivet(x, y, STEEL)
    return t


# ------------------------------------------------------------------ грузы
@tex("block/cargo_hold")
def cargo_hold(name):
    """Грузовой отсек: панель с люком на двух защёлках и рёбрами жёсткости."""
    t = Tex()
    plate(t, STEEL, base=2, seed=77)
    t.recess(5, 7, 26, 24, DARK_STEEL, base=2)
    t.panel(6, 8, 25, 23, STEEL, base=2, seed=78)
    for x in (10, 20):
        t.panel(x, 13, x + 1, 18, AMBER, base=3)
    for y in (3, 27):
        t.hline(4, 27, y, STEEL[1])
        t.hline(4, 27, y + 1, STEEL[3])
    t.rivets(inset=1, pal=STEEL)
    return t


@tex("block/cargo_loader_side")
def cargo_loader_side(name):
    """Погрузчик: вертикальный транспортёр с грузами в центральной шахте, направляющие по бокам."""
    t = Tex()
    plate(t, STEEL, base=2, seed=79)
    t.recess(10, 0, 21, 31, BLACK, base=2)
    for y in range(1, 31, 4):
        t.hline(11, 20, y, DARK_STEEL[3])
    for y0 in (4, 18):
        t.panel(12, y0, 19, y0 + 6, AMBER, base=2)
        t.hline(12, 19, y0 + 3, AMBER[1])
    for x in (7, 23):
        t.vline(x, 0, 31, STEEL[4])
        t.vline(x + 1, 0, 31, STEEL[1])
    t.hazard(0, 28, 5, 31)
    t.hazard(26, 28, 31, 31)
    return t


@tex("block/cargo_loader_top")
def cargo_loader_top(name):
    """Верх погрузчика: приёмный люк с поворотным столом."""
    t = Tex()
    plate(t, STEEL, base=2, seed=80)
    t.ring(16, 16, 9.5, 11.5, DARK_STEEL, base=2)
    t.disc(16, 16, 9, DARK_STEEL, base=1)
    t.panel(12, 12, 19, 19, AMBER, base=2)
    t.hline(12, 19, 15, AMBER[1])
    for x, y in ((3, 3), (26, 3), (3, 26), (26, 26)):
        t.screw(x, y, STEEL)
    return t


@tex("block/cargo_terminal_side")
def cargo_terminal_side(name):
    """Грузовой терминал: транспортёрная лента со стрелкой направления по центру."""
    t = Tex()
    plate(t, STEEL, base=2, seed=81)
    t.recess(2, 11, 29, 20, BLACK, base=2)
    for x in range(3, 29, 3):
        t.vline(x, 12, 19, DARK_STEEL[3])
    # стрелка
    t.rect(6, 14, 20, 17, AMBER[3])
    t.hline(6, 20, 14, AMBER[4])
    for i in range(5):
        t.vline(21 + i, 11 + i, 20 - i, AMBER[3] if i else AMBER[4])
    t.hline(21, 25, 20, AMBER[1])
    t.rivets(inset=3, pal=STEEL)
    return t


@tex("block/cargo_terminal_top")
def cargo_terminal_top(name):
    """Верх терминала: экран манифеста груза."""
    t = Tex()
    plate(t, STEEL, base=2, seed=82)
    t.screen(5, 5, 26, 22, pal=[mix(c, GREEN[i], 0.6) for i, c in enumerate(SCREEN)], rows=5, seed=83)
    for x in (8, 14, 20):
        t.led(x, 25, GREEN if x != 20 else AMBER)
    return t


# ------------------------------------------------------------------ наземное
@tex("block/launch_pad")
def launch_pad(name):
    """Стартовый стол: тёмная плита, сигнальные метки по периметру, газоотводное отверстие по центру."""
    t = Tex()
    plate(t, DARK_STEEL, base=2, seed=84)
    for i in range(2, 30, 4):
        for x, y in ((i, 1), (i, 29), (1, i), (29, i)):
            t.rect(x, y, x + 1, y + 1, HAZARD_YELLOW[3])
    t.recess(10, 10, 21, 21, BLACK, base=1)
    t.panel(12, 12, 19, 19, AMBER, base=2)
    t.recess(14, 14, 17, 17, BLACK, base=1)
    return t


@tex("block/launch_pad_top_formed")
def launch_pad_top_formed(name):
    """Собранный стол: уголки «осторожно», по центру — голубая посадочная окружность."""
    t = Tex()
    plate(t, DARK_STEEL, base=2, seed=85)
    for x0, y0 in ((0, 0), (24, 0), (0, 24), (24, 24)):
        t.hazard(x0, y0, x0 + 7, y0 + 7)
    t.ring(16, 16, 9, 10.5, CYAN, base=3)
    t.glow(16, 16, 13, CYAN[3], 0.15)
    t.disc(16, 16, 2.5, CYAN, base=3)
    return t


@tex("block/launch_pad_side")
def launch_pad_side(name):
    """Тёмная стальная плита (бок стола и днища наземных машин)."""
    t = Tex()
    plate(t, DARK_STEEL, base=2, seed=86)
    t.hline(1, 30, 15, DARK_STEEL[1])
    t.hline(1, 30, 16, DARK_STEEL[3])
    for x in (3, 27):
        for y in (3, 12, 19, 27):
            t.rivet(x, y, GUNMETAL)
    return t


@tex("block/landing_beacon_side")
def landing_beacon_side(name):
    """Посадочный маяк: ряд голубых огней под стеклом по центру."""
    t = Tex()
    plate(t, STEEL, base=2, seed=87)
    t.recess(3, 12, 28, 19, BLACK, base=1)
    for x in (5, 11, 17, 23):
        t.rect(x, 14, x + 3, 17, CYAN[3])
        t.hline(x, x + 3, 14, CYAN[4])
    t.glow(16, 15.5, 14, CYAN[3], 0.2)
    t.hazard(0, 28, 31, 31)
    t.hazard(0, 0, 31, 2)
    return t


@tex("block/landing_beacon_top")
def landing_beacon_top(name):
    """Верх маяка: проблесковый огонь в стальном кольце, метки по углам."""
    t = Tex()
    plate(t, DARK_STEEL, base=2, seed=88)
    for x0, y0 in ((2, 2), (26, 2), (2, 26), (26, 26)):
        t.rect(x0, y0, x0 + 3, y0 + 3, HAZARD_YELLOW[3])
        t.put(x0, y0, HAZARD_YELLOW[4])
    t.ring(16, 16, 7, 9, STEEL, base=2)
    t.disc(16, 16, 6.5, CYAN, base=3)
    t.disc(16, 16, 3, CYAN, base=4, lit=False)
    t.glow(16, 16, 14, CYAN[3], 0.35)
    return t


@tex("block/assembly_pylon")
def assembly_pylon(name):
    """Ферма монтажной башни (видна полоса x 8–23): два пояса и раскосы «Х»."""
    t = Tex()
    t.fill(DARK_STEEL, 1, grain=0.2, seed=89)
    t.rect(8, 0, 23, 31, BLACK[1])
    for x in (8, 22):
        t.rect(x, 0, x + 1, 31, STEEL[2])
        t.vline(x, 0, 31, STEEL[3])
        t.vline(x + 1, 0, 31, STEEL[1])
    for y0 in (0, 16):
        for i in range(16):
            y = y0 + i
            xa = 10 + round(i * 11 / 15)
            xb = 21 - round(i * 11 / 15)
            t.rect(xa, y, xa + 1, y, STEEL[3])
            t.rect(xb - 1, y, xb, y, STEEL[2])
        t.hline(8, 23, y0, STEEL[3])
        t.hline(8, 23, y0 + 1, STEEL[1])
    # на торцах — косынки (верх/низ модели 8–23)
    return t


@tex("block/assembly_pylon_formed")
def assembly_pylon_formed(name):
    """Собранная башня: та же ферма, пояса «осторожно» и голубой огонь готовности."""
    t = assembly_pylon(name)
    t.hazard(8, 12, 23, 15)
    t.hazard(8, 28, 23, 31)
    t.panel(13, 3, 18, 7, DARK_STEEL, base=2)
    t.rect(14, 4, 17, 6, CYAN[3])
    t.hline(14, 17, 4, CYAN[4])
    t.glow(15.5, 5, 6, CYAN[3], 0.3)
    return t


@tex("block/fueling_pump")
def fueling_pump(name):
    """Заправочный насос: вертикальная магистраль, манометр, свёрнутый шланг."""
    t = Tex()
    plate(t, STEEL, base=2, seed=90)
    t.recess(3, 3, 28, 28, DARK_STEEL, base=2)
    t.pipe_v(8, 3, 28, 2, STEEL)
    flange_h(t, 5, 11, 6, STEEL)
    flange_h(t, 5, 11, 24, STEEL)
    # манометр
    t.disc(8, 15.5, 4.2, STEEL, base=3)
    t.disc(8, 15.5, 3, WHITE_PAINT, base=3, lit=False)
    t.put(7, 15, RED[2])
    t.put(8, 14, RED[3])
    t.put(9, 13, RED[3])
    t.put(8, 15, BLACK[2])
    # шланг кольцом справа
    t.ring(20, 15.5, 5.5, 7.8, AMBER, base=2)
    t.pipe_h(11, 13, 15, 1, AMBER)
    t.panel(18, 23, 23, 27, DARK_STEEL, base=3)
    t.rivets(inset=1, pal=STEEL)
    return t


@tex("block/gyroscope")
def gyroscope(name):
    """Силовой гироскоп: кардановы кольца в корпусе, латунный ротор."""
    t = Tex()
    plate(t, STEEL, base=2, seed=91)
    t.recess(3, 3, 28, 28, DARK_STEEL, base=1)
    t.ring(16, 16, 10.5, 12.3, ALUMINIUM, base=2)
    for y in range(4, 28):
        t.put(15, y, ALUMINIUM[3] if abs(y - 16) > 5 else DARK_STEEL[2])
        t.put(16, y, ALUMINIUM[1] if abs(y - 16) > 5 else DARK_STEEL[2])
    t.ring(16, 16, 7, 8.5, STEEL, base=3)
    t.disc(16, 16, 5.5, BRASS, base=2)
    t.ring(16, 16, 2, 3, BRASS, base=1)
    t.disc(16, 16, 1.2, BRASS, base=4, lit=False)
    for x, y in ((4, 4), (25, 4), (4, 25), (25, 25)):
        t.screw(x, y, STEEL)
    return t


@tex("block/interceptor_dish_top")
def interceptor_dish_top(name):
    """Радар перехватчика сверху: параболическое зеркало с облучателем и красной лампой."""
    t = Tex()
    plate(t, DARK_STEEL, base=2, seed=92)
    for y in range(32):
        for x in range(32):
            d = math.hypot(x + 0.5 - 16, y + 0.5 - 16)
            if d <= 13:
                k = 3 if d > 11 else (2 if d > 6 else 1)
                if -(x + y - 32) > 12 and d > 6:
                    k += 1
                t.put(x, y, ALUMINIUM[max(0, min(4, k - 1))])
    t.ring(16, 16, 12.5, 13.5, STEEL, base=2)
    for a in range(3):
        ang = a * 2 * math.pi / 3 - math.pi / 2
        for r in range(4, 12):
            t.put(int(16 + r * math.cos(ang)), int(16 + r * math.sin(ang)), DARK_STEEL[2])
    t.disc(16, 16, 3.5, RED, base=2)
    t.glow(16, 16, 7, RED[4], 0.3)
    return t


@tex("block/interceptor_dish_side")
def interceptor_dish_side(name):
    """Бок радара: опорно-поворотное устройство, шкала азимута, красный огонь заграждения."""
    t = Tex()
    plate(t, STEEL, base=2, seed=93)
    t.recess(4, 4, 27, 27, DARK_STEEL, base=2)
    t.panel(12, 6, 19, 25, STEEL, base=2)
    for y in range(8, 24, 2):
        t.put(15, y, STEEL[1])
        t.put(16, y, STEEL[1])
    t.led(15, 3, RED)
    for x in (6, 24):
        t.grille(x - 1, 8, x + 2, 23, DARK_STEEL, pitch=2)
    t.hazard(0, 29, 31, 31)
    return t


# ------------------------------------------------------------------ ЦУП и телеметрия
@tex("block/mission_control_side")
def mission_control_side(name):
    """Бок пульта ЦУП (видны строки 14–31): козырёк, ряд индикаторов, решётка вентиляции."""
    t = Tex()
    t.fill(GUNMETAL, 2, grain=0.2, seed=94)
    t.bevel(0, 0, 31, 31, GUNMETAL, raised=True, base=2)
    t.hline(0, 31, 14, GUNMETAL[4])
    t.hline(0, 31, 15, GUNMETAL[3])
    t.hline(0, 31, 16, GUNMETAL[1])
    t.recess(4, 18, 27, 21, BLACK, base=1)
    for i, x in enumerate(range(6, 26, 4)):
        t.led(x, 19, GREEN if i != 2 else AMBER)
    t.grille(6, 23, 25, 29, DARK_STEEL, pitch=2)
    t.rivet(2, 18, STEEL)
    t.rivet(28, 18, STEEL)
    t.rivet(2, 28, STEEL)
    t.rivet(28, 28, STEEL)
    return t


@tex("block/mission_control_top")
def mission_control_top(name):
    """Верх ЦУП: строки 0–13 — монитор (его лицевая грань), ниже — столешница с клавиатурой и
    кнопками."""
    t = Tex()
    t.fill(GUNMETAL, 2, grain=0.2, seed=95)
    # монитор
    t.panel(1, 0, 30, 13, BLACK, base=2)
    t.screen(3, 1, 28, 12, rows=4, seed=96)
    for x in range(5, 27, 3):
        t.put(x, 9, SCREEN[4] if x % 2 else SCREEN[3])
    t.put(20, 3, GREEN[4])
    t.put(22, 3, GREEN[4])
    t.bevel(0, 14, 31, 31, GUNMETAL, raised=True, base=2)
    # клавиатура
    t.recess(5, 17, 26, 24, BLACK, base=2)
    for y in (18, 21):
        for x in range(6, 26, 2):
            t.put(x, y, DARK_STEEL[4])
            t.put(x, y + 1, DARK_STEEL[2])
    # кнопки
    for x, pal in ((8, GREEN), (13, AMBER), (18, AMBER), (23, RED)):
        t.led(x - 1, 27, pal)
    return t


def telemetry(name, pal, rows, on=True):
    t = Tex()
    plate(t, GUNMETAL, base=2, seed=97)
    t.screen(3, 3, 28, 25, pal=pal, lit=on, rows=rows, seed=98)
    for x in (8, 15, 22):
        t.led(x, 27, pal if on else GLASS, on=on)
    return t


def _tint(pal):
    return [mix(SCREEN[i], pal[i], 0.75 if i >= 2 else 0.25) for i in range(5)]


@tex("block/telemetry_screen_off")
def telemetry_off(name):
    t = telemetry(name, SCREEN, 0, on=False)
    return t


@tex("block/telemetry_screen_sealed")
def telemetry_sealed(name):
    """Отсек герметичен: зелёные строки, ровная кривая давления."""
    t = telemetry(name, _tint(GREEN), 4)
    for x in range(5, 27):
        t.put(x, 23, GREEN[4])
    return t


@tex("block/telemetry_screen_leak")
def telemetry_leak(name):
    """Утечка: красные строки, падающая кривая давления."""
    t = telemetry(name, _tint(RED), 4)
    for x in range(5, 27):
        t.put(x, 17 + (x - 5) * 6 // 21, RED[4])
    return t


@tex("block/telemetry_screen_side")
def telemetry_side(name):
    t = Tex()
    plate(t, GUNMETAL, base=2, seed=99)
    t.grille(8, 6, 23, 25, DARK_STEEL, pitch=3)
    t.rivets(inset=3, pal=STEEL)
    return t


# ------------------------------------------------------------------ орбитальная пушка
@tex("block/orbital_cannon_side")
def orbital_cannon_side(name):
    """Бок пушки: строки 12–31 — основание (панели, «осторожно» снизу), 4–11 (x 6–25) — погон башни
    с рёбрами, 0–3 — дульный тормоз."""
    t = Tex()
    t.fill(GUNMETAL, 2, grain=0.25, seed=100)
    # основание
    t.bevel(0, 12, 31, 31, GUNMETAL, raised=True, base=2)
    t.hazard(1, 27, 30, 30)
    t.hline(1, 30, 26, GUNMETAL[0])
    t.recess(12, 14, 19, 25, DARK_STEEL, base=1)
    t.grille(13, 15, 18, 24, DARK_STEEL, pitch=2)
    for x in (3, 7, 23, 27):
        t.panel(x - 1, 14, x + 1, 24, GUNMETAL, base=3)
    # погон башни
    t.rect(6, 4, 25, 11, GUNMETAL[2])
    t.bevel(6, 4, 25, 11, GUNMETAL, raised=True, base=2)
    for x in range(8, 25, 3):
        t.vline(x, 5, 10, GUNMETAL[4])
        t.vline(x + 1, 5, 10, GUNMETAL[1])
    # дульный тормоз
    t.rect(0, 0, 31, 3, DARK_STEEL[2])
    t.hline(0, 31, 0, DARK_STEEL[4])
    for x in range(2, 31, 5):
        t.rect(x, 1, x + 1, 2, BLACK[0])
    t.hline(0, 31, 3, DARK_STEEL[0])
    return t


@tex("block/orbital_cannon_top")
def orbital_cannon_top(name):
    """Верх: плита основания с опорным кольцом башни, в центре — канал ствола."""
    t = Tex()
    plate(t, GUNMETAL, base=2, seed=101)
    for x, y in ((2, 2), (27, 2), (2, 27), (27, 27)):
        t.screw(x, y, STEEL)
    t.ring(16, 16, 10, 12.5, DARK_STEEL, base=3)
    for a in range(12):
        ang = a * math.pi / 6
        t.put(int(16 + 11.2 * math.cos(ang)), int(16 + 11.2 * math.sin(ang)), STEEL[4])
    t.disc(16, 16, 9.5, GUNMETAL, base=2)
    t.ring(16, 16, 5, 6.5, STEEL, base=2)
    t.disc(16, 16, 4.6, BLACK, base=0, lit=False)
    t.ring(16, 16, 3.2, 4.6, BLACK, base=2)
    return t


@tex("block/orbital_cannon_bottom")
def orbital_cannon_bottom(name):
    """Днище основания и ствол (ствол берёт полосу x 11–20 и строки 0–3, торцы — центр):
    тёмная сталь с продольными направляющими рельсотрона."""
    t = Tex()
    t.fill(DARK_STEEL, 2, grain=0.2, seed=102)
    for y0 in (0, 16):
        for x0 in (0, 16):
            t.bevel(x0, y0, x0 + 15, y0 + 15, DARK_STEEL, raised=True, base=2)
    # ствол: направляющие вдоль и медные шины
    for x in range(11, 21):
        for y in range(32):
            if x in (11, 20):
                t.put(x, y, DARK_STEEL[1])
            elif x in (13, 18):
                t.put(x, y, COPPER[3])
            elif x in (14, 17):
                t.put(x, y, COPPER[1])
            else:
                t.put(x, y, DARK_STEEL[3] if x == 12 else DARK_STEEL[2])
    t.disc(16, 16, 3.5, BLACK, base=1, lit=False)
    return t


# ------------------------------------------------------------------ спутники
@tex("block/satellite")
def satellite(name):
    """Связной спутник: корпус (x 8–23, строки 10–25) в золотой экранно-вакуумной изоляции,
    мачта антенны (x 15–16, строки 2–9), тарелка (строки 0–1); остальное — стальные рёбра
    (ими же рисуются штанги панелей у других спутников)."""
    t = Tex()
    t.fill(STEEL, 2, grain=0.2, seed=103)
    for y in (7, 15, 23):
        t.hline(0, 31, y, STEEL[4])
        t.hline(0, 31, y + 1, STEEL[1])
    # МЛИ: мятая фольга
    foil(t, GOLD_FOIL, 104, 8, 10, 23, 25)
    t.bevel(8, 10, 23, 25, GOLD_FOIL, raised=True, base=2)
    t.panel(12, 14, 19, 21, BLACK, base=2)
    t.hline(13, 18, 16, CYAN[3])
    t.hline(13, 16, 18, CYAN[2])
    # мачта
    t.rect(15, 0, 16, 9, STEEL[3])
    t.vline(16, 0, 9, STEEL[1])
    t.rect(11, 0, 20, 1, WHITE_PAINT[3])
    t.hline(11, 20, 1, WHITE_PAINT[1])
    return t


def foil(t, pal, seed, x0=0, y0=0, x1=31, y1=31):
    """Экранно-вакуумная изоляция: фольга крупными складками."""
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            a, b = (x + y) // 6, (x - y + 64) // 5
            h = _h(a, b, seed)
            k = 3 if h > 0.62 else (2 if h > 0.22 else 1)
            # складка: светлая кромка на верхне-левой границе грани
            if (x + y) % 6 == 0 and h > 0.3:
                k = min(4, k + 1)
            if (x - y + 64) % 5 == 4:
                k = max(0, k - 1)
            t.put(x, y, pal[k])


@tex("block/hyperspectral_bus")
def hyperspectral_bus(name):
    """Шина гиперспектрального спутника: золотая МЛИ, швы на липучке, панель радиатора
    (видна полоса x 10–21, строки 16–31; верх — центр)."""
    t = Tex()
    foil(t, GOLD_FOIL, 105)
    for x in (9, 22):
        t.vline(x, 0, 31, GOLD_FOIL[0])
    t.hline(0, 31, 16, GOLD_FOIL[4])
    t.hline(0, 31, 17, GOLD_FOIL[1])
    t.panel(12, 20, 19, 28, WHITE_PAINT, base=3, grain=0.1)
    for y in range(22, 27, 2):
        t.hline(13, 18, y, WHITE_PAINT[1])
    return t


@tex("block/imaging_satellite")
def imaging_satellite(name):
    """Корпус спутника съёмки: серебристая МЛИ (строки 14–31, x 10–21), звёздный датчик."""
    t = Tex()
    foil(t, ALUMINIUM, 106)
    t.hline(0, 31, 14, ALUMINIUM[4])
    t.hline(0, 31, 15, ALUMINIUM[1])
    for x in (9, 22):
        t.vline(x, 0, 31, ALUMINIUM[0])
    t.panel(12, 18, 19, 25, BLACK, base=2)
    lens(t, 15.5, 21.5, 2.2, GLASS)
    t.panel(13, 27, 18, 29, GOLD_FOIL, base=3)
    return t


@tex("block/imaging_satellite_lens")
def imaging_satellite_lens(name):
    """Объектив телескопа (верх трубы — центр 12–19): многослойное просветлённое стекло."""
    t = Tex()
    t.fill(BLACK, 2, grain=0.1, seed=107)
    t.ring(16, 16, 12, 15, DARK_STEEL, base=2)
    t.disc(16, 16, 11, BLACK, base=1)
    t.disc(16, 16, 4, BLUE, base=1)
    t.ring(16, 16, 3.2, 4.2, [mix(c, (0x7A, 0x3E, 0x9C), 0.5) for c in BLUE], base=3)
    t.put(14, 14, BLUE[4])
    t.put(15, 14, BLUE[3])
    t.put(14, 15, BLUE[3])
    t.ring(16, 16, 6.5, 7.2, DARK_STEEL, base=1)
    return t


@tex("block/imaging_satellite_tube")
def imaging_satellite_tube(name):
    """Бленда телескопа: чёрная труба с кольцами жёсткости (видна полоса x 12–19)."""
    t = Tex()
    cyl_v(t, 0, 31, 0, 31, BLACK, base=2)
    cyl_v(t, 12, 19, 0, 31, BLACK, base=2)
    for y in (2, 9, 16, 23):
        t.hline(0, 31, y, DARK_STEEL[4])
        t.hline(0, 31, y + 1, DARK_STEEL[2])
        t.hline(0, 31, y + 2, BLACK[0])
    return t


@tex("block/spectrometer_box")
def spectrometer_box(name):
    """Блок спектрометра: белый корпус с охлаждающими ламелями (грани берут разные куски —
    рисунок однородный)."""
    t = Tex()
    t.fill(WHITE_PAINT, 3, grain=0.1, seed=108)
    for y in range(1, 32, 3):
        t.hline(0, 31, y, DARK_STEEL[2])
        t.hline(0, 31, y + 1, WHITE_PAINT[4])
    t.bevel(0, 0, 31, 31, WHITE_PAINT, raised=True, base=2)
    return t
