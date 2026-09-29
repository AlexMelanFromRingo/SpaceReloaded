"""Кинетика (005) и станция (007): валы, шестерни, мотор, маховик, пресс, токарный, муфта, ступицы,
газовые баллоны, гидропоника, лампа роста, ровер, лунный кирпич.

UV моделей (проверено по models/block):
- валы (rotor_*_shaft): элемент 6…10 — бок берёт полосу x 12…19 (px), торец — квадрат 12…19;
- шестерни (rotor_*_gear): верх/низ — вся текстура как лицо колеса (зубья вырезаны геометрией),
  бока зубьев — полоса строк 10…21 во всю ширину;
- колесо ровера: шина и шевроны — боковой диск целиком + полоса x 12…19 (протектор), обод — диск 7…24,
  ступица — 11…20;
- рама ровера: вся текстура — палуба, стойка мачты — столбцы 8…9 / 22…23, борта — строки 16…21;
- пульт ровера: лицевая панель — x 4…27, y 12…31; верх антенны — x 10…31, y 0…17;
- лоток гидропоники: бока — нижняя половина (строки 16…31), дно — вся текстура;
- лампа роста: низ — 2…29 (светодиоды), бока — строки 0…7 (корпус).
"""
import math

from . import tex
from .kit import (AMBER, BLACK, BLUE, BRASS, COPPER, CYAN, DARK_STEEL, GLASS, GREEN, GUNMETAL, HAZARD_YELLOW,
                  O2_BLUE, RED, STEEL, TITANIUM, WHITE_PAINT, WOOD, ALUMINIUM, Tex, _h, mix, shade)

# станочная краска (серо-зелёная, как у токарных станков)
MACHINE_GREEN = [(0x2E, 0x3E, 0x38), (0x40, 0x55, 0x4C), (0x55, 0x6E, 0x63), (0x6E, 0x8A, 0x7E), (0x98, 0xB2, 0xA6)]
# лунный кирпич — спечённый анортозитовый реголит
REGOLITH = [(0x5C, 0x5A, 0x57), (0x74, 0x72, 0x6E), (0x8C, 0x8A, 0x85), (0xA4, 0xA2, 0x9C), (0xBE, 0xBC, 0xB5)]
# никель-железный элемент: никелированная сталь, оливковая окраска корпуса
NIFE = [(0x26, 0x2C, 0x22), (0x36, 0x3E, 0x30), (0x48, 0x52, 0x40), (0x5E, 0x6A, 0x54), (0x80, 0x8C, 0x74)]
# фрикционная накладка
FRICTION = [(0x3C, 0x20, 0x18), (0x58, 0x2E, 0x22), (0x74, 0x40, 0x2E), (0x90, 0x56, 0x3E), (0xAE, 0x74, 0x58)]
# питательный раствор
NUTRIENT = [(0x0A, 0x2A, 0x2C), (0x12, 0x3E, 0x40), (0x1C, 0x56, 0x58), (0x2E, 0x76, 0x74), (0x6A, 0xAE, 0xA6)]
# керамзит в сетчатых стаканах
CLAY = [(0x5A, 0x30, 0x1E), (0x7C, 0x44, 0x2A), (0x9C, 0x5C, 0x3A), (0xBA, 0x78, 0x50), (0xD6, 0x9C, 0x74)]
WHITE_PLASTIC = WHITE_PAINT


# ============================================================== общие приёмы
def radial(t, cx, cy, fn):
    """Проход по всем пикселям с полярными координатами (r, угол) относительно центра."""
    for y in range(t.n):
        for x in range(t.n):
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            fn(x, y, math.hypot(dx, dy), math.atan2(dy, dx))


def lit_k(dx, dy, r, base, k=0.45):
    """Ступень палитры с освещением сверху-слева для точки выпуклого диска."""
    s = -(dx + dy) / (max(r, 1) * 1.414)
    return base + (1 if s > k else -1 if s < -k else 0)


def turned_rings(t, cx, cy, r0, r1, pal, base, step=2, seed=31):
    """Следы точения: концентрические кольца чуть светлее/темнее с освещением сверху-слева."""
    def f(x, y, r, a):
        if r0 <= r < r1:
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            k = lit_k(dx, dy, r, base, 0.55)
            c = pal[max(0, min(4, k))]
            if int(r) % step == 0:
                c = mix(c, pal[max(0, base - 1)], 0.22)
            t.put(x, y, c)
    radial(t, cx, cy, f)


def bolt_circle(t, cx, cy, r, n, pal=STEEL, phase=0.0):
    """n винтов по окружности радиуса r."""
    for i in range(n):
        a = phase + 2 * math.pi * i / n
        x, y = cx + r * math.cos(a), cy + r * math.sin(a)
        t.rivet(int(round(x - 1)), int(round(y - 1)), pal)


def housing(t, pal=STEEL, base=2, seed=41, collars=True):
    """Корпус торца/бока машины: панель + рама, у колонных блоков — посадочные пояса сверху и снизу."""
    t.panel(0, 0, 31, 31, pal, base=base, grain=0.25, seed=seed)
    if collars:
        for y0 in (0, 28):
            t.panel(0, y0, 31, y0 + 3, pal, base=min(4, base + 1), grain=0.1, seed=seed + 1)
        t.ao(0, 4, 31, 27, k=0.18)


def level_gauge(t, level, x0=13, x1=18, y_top=9, y_bot=24, pal_on=GREEN):
    """Вертикальное смотровое окно уровня из 4 секций, заполнение снизу, по центру грани."""
    t.recess(x0 - 1, y_top - 1, x1 + 1, y_bot + 1, BLACK, base=1)
    h = (y_bot - y_top + 1) // 4
    for k in range(4):
        y1 = y_bot - k * h
        y0 = y1 - h + 2
        lit = k < level
        pal = pal_on if lit else GLASS
        t.rect(x0, y0, x1, y1, pal[3] if lit else pal[2])
        t.hline(x0, x1, y0, pal[4] if lit else pal[3])
        t.put(x1, y1, pal[2] if lit else pal[1])
    if level > 0:
        t.glow((x0 + x1 + 1) / 2, y_bot - (level - 0.5) * h, 5, pal_on[4], 0.15)


# ============================================================== валы
@tex("block/steel_shaft")
def steel_shaft(name):
    """Стальной вал: шлифованная сталь вдоль оси, цилиндрическая светотень на полосе 12…19 (её берёт
    модель), шпоночный паз — тёмная продольная линия (видно вращение)."""
    t = Tex()
    t.brushed(STEEL, base=2, horizontal=False, seed=51)
    cyl = [3, 4, 3, 3, 2, 2, 1, 1]
    for i, k in enumerate(cyl):
        for y in range(32):
            c = STEEL[k]
            if _h(12 + i, y // 6, 52) > 0.8:
                c = mix(c, STEEL[min(4, k + 1)], 0.4)
            t.put(12 + i, y, c)
    t.vline(17, 0, 31, STEEL[0])
    t.vline(16, 0, 31, mix(STEEL[1], STEEL[2], 0.5))
    # остальная площадь (патрон токарного, ректенна) — те же полосы шлифовки, края темнее
    for y in range(32):
        t.darken(0, y, 0.12)
        t.darken(31, y, 0.12)
    return t


@tex("block/wooden_shaft")
def wooden_shaft(name):
    """Деревянный вал: волокна вдоль оси, светотень цилиндра на полосе 12…19, тёмная риска-метка."""
    t = Tex()
    for y in range(32):
        for x in range(32):
            h = _h(x, y // 8, 61)
            k = 2 if h < 0.78 else (3 if h < 0.9 else 1)
            t.put(x, y, WOOD[k])
    for x in range(32):
        if _h(x, 0, 62) > 0.86:
            for y in range(32):
                if _h(x, y // 3, 63) > 0.25:
                    t.put(x, y, WOOD[1])
    for i, dk in enumerate([1, 1, 0, 0, 0, -1, -1, -1]):
        for y in range(32):
            c = t.get(12 + i, y)[:3]
            t.put(12 + i, y, shade(c, 0.12 * dk) if dk >= 0 else shade(c, 0.12 * dk))
    t.vline(17, 0, 31, WOOD[0])
    # сучок — по оси, чтобы не спорить с симметрией
    t.disc(15.5, 22.5, 1.6, WOOD, base=1)
    return t


# ============================================================== шестерни
@tex("block/gear")
def gear(name):
    """Лицо зубчатого колеса (геометрия вырезает зубья): венец с фаской, утопленное полотно,
    выпуклая ступица, расточка со шпоночным пазом. Бока зубьев берут строки 10…21 — там ровный венец."""
    t = Tex()
    c = 16.0

    def f(x, y, r, a):
        dx, dy = x + 0.5 - c, y + 0.5 - c
        s_ = -(dx + dy) / (max(r, 1) * 1.414)       # >0 — сторона к свету
        if r >= 12:            # венец
            k = 3
            if r >= 15:
                k = 4 if s_ > 0 else 2               # фаска зубьев
            elif r < 13:
                k = 2 if s_ > 0 else 4               # внутренний уступ венца к полотну
        elif r >= 7:           # полотно утоплено: тень у верхне-левой стенки венца
            k = 1
            if r > 11 and s_ > 0.3:
                k = 0
            elif r < 8 and s_ < -0.3:
                k = 2
        elif r >= 2.5:         # ступица
            k = 3
            if r >= 6:
                k = 4 if s_ > 0 else 1
        else:
            k = None
        if k is not None:
            t.put(x, y, STEEL[k])
    radial(t, c, c, f)
    t.disc(c, c, 2.5, BLACK, base=1, lit=False)
    t.rect(15, 12, 16, 13, BLACK[1])
    return t


@tex("block/gearbox")
def gearbox(name):
    """Корпус редуктора: литой корпус с рёбрами, в центре подшипниковый щит на 6 винтах и латунная муфта."""
    t = Tex()
    t.panel(0, 0, 31, 31, GUNMETAL, base=2, grain=0.3, seed=71)
    t.bevel(2, 2, 29, 29, GUNMETAL, raised=False, base=2)
    for x, y in ((3, 3), (26, 3), (3, 26), (26, 26)):
        t.screw(x, y, STEEL)
    t.disc(16, 16, 10, GUNMETAL, base=3)
    t.ring(16, 16, 9.5, 10.5, GUNMETAL, base=1)
    bolt_circle(t, 16, 16, 8, 6, STEEL, phase=-math.pi / 2)
    t.disc(16, 16, 5.5, STEEL, base=2)
    t.disc(16, 16, 3.5, BLACK, base=1, lit=False)
    # крестовая муфта (узнаваемость старой текстуры)
    t.rect(15, 11, 16, 20, BRASS[3])
    t.rect(11, 15, 20, 16, BRASS[3])
    t.hline(15, 16, 11, BRASS[4])
    t.vline(11, 15, 16, BRASS[4])
    t.hline(11, 20, 16, BRASS[1])
    t.vline(16, 11, 20, BRASS[1])
    return t


# ============================================================== маховик, пресс
@tex("block/flywheel")
def flywheel(name):
    """Маховик: тёмный литой диск со следами точения, белая метка балансировки по радиусу (видно вращение)."""
    t = Tex()
    t.fill(GUNMETAL, base=2, grain=0.15, seed=81)

    def rings(x, y, r, a):
        if int(r) % 3 == 0:
            t.darken(x, y, 0.12)
        # мягкий объём: к свету светлее
        s_ = -((x + 0.5 - 16) + (y + 0.5 - 16)) / 45
        if s_ > 0:
            t.lighten(x, y, 0.15 * s_)
        else:
            t.darken(x, y, -0.2 * s_)
    radial(t, 16, 16, rings)
    t.disc(16, 16, 4.5, STEEL, base=2)
    t.disc(16, 16, 2, BLACK, base=1, lit=False)
    # метка — белая полоса поперёк (и на бортах видна как полоса)
    t.rect(21, 15, 31, 16, WHITE_PAINT[3])
    t.rect(0, 15, 10, 16, WHITE_PAINT[1])
    t.hline(21, 31, 15, WHITE_PAINT[4])
    # пара балансировочных сверловок
    for x, y in ((8, 8), (23, 23)):
        t.disc(x, y, 1.3, BLACK, base=1, lit=False)
    return t


@tex("block/press_ram")
def press_ram(name):
    """Шток и плита ползуна пресса: полированная сталь со следами точения поперёк оси (плитка без швов)."""
    t = Tex()
    t.brushed(STEEL, base=3, horizontal=True, seed=91)
    for y in (0, 8, 16, 24):
        t.hline(0, 31, y, STEEL[2])
        t.hline(0, 31, y + 1, STEEL[4])
    return t


@tex("block/mechanical_press_side")
def mechanical_press_side(name):
    """Бок пресса: две колонны, траверса в сигнальной окраске, между колоннами — направляющая ползуна."""
    t = Tex()
    t.fill(DARK_STEEL, base=1, grain=0.2, seed=101)
    # колонны
    for x0 in (2, 24):
        t.panel(x0, 0, x0 + 5, 31, STEEL, base=2, grain=0.2, seed=102 + x0)
    # траверса
    t.panel(0, 1, 31, 7, AMBER, base=3, grain=0.15, seed=104)
    t.hazard(9, 3, 22, 5, width=2)
    t.bevel(9, 3, 22, 5, AMBER, raised=False, base=3)
    # станина
    t.panel(0, 25, 31, 30, STEEL, base=2, grain=0.2, seed=105)
    for x in (4, 26):
        t.screw(x - 1, 2, STEEL)
        t.screw(x - 1, 26, STEEL)
    # ползун в направляющей
    t.recess(9, 9, 22, 23, DARK_STEEL, base=0)
    t.panel(11, 11, 20, 17, STEEL, base=3, grain=0.1, seed=106)
    t.pipe_v(15, 17, 22, 1, STEEL)
    t.pipe_v(16, 17, 22, 1, STEEL)
    t.ao(9, 9, 22, 23, k=0.2)
    t.hline(0, 31, 31, BLACK[1])
    t.hline(0, 31, 0, STEEL[1])
    return t


@tex("block/mechanical_press_top")
def mechanical_press_top(name):
    """Верх/низ пресса: стальная плита, в центре — расточка под шток, Т-пазы стола."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.25, seed=111)
    t.bevel(2, 2, 29, 29, STEEL, raised=False, base=2)
    for y in (7, 24):
        t.recess(4, y - 1, 27, y + 1, DARK_STEEL, base=1)
    t.ring(16, 16, 5.5, 7.5, STEEL, base=3)
    t.disc(16, 16, 5, DARK_STEEL, base=1, lit=False)
    t.disc(16, 16, 3, STEEL, base=3)
    t.rivets(inset=3, pal=STEEL)
    return t


# ============================================================== мотор, муфта, токарный, ступицы
def motor_side_body(t, body, base, window_pal, seed):
    """Бок мотора: рёбра охлаждения вдоль оси, в центре — смотровое окно на обмотку."""
    housing(t, body, base=base, seed=seed)
    for x in range(3, 29, 3):
        if 10 <= x <= 21:
            continue
        t.vline(x, 5, 26, body[min(4, base + 2)])
        t.vline(x + 1, 5, 26, body[max(0, base - 1)])
    t.recess(10, 6, 21, 25, BLACK, base=1)
    # обмотка: витки поперёк оси (медь)
    for y in range(7, 25):
        k = 3 if y % 2 == 0 else 2
        t.hline(11, 20, y, window_pal[k])
        t.put(11, y, window_pal[4 if y % 2 == 0 else 3])
        t.put(20, y, window_pal[1])
    # пазы статора — тёмные разделители
    for y in (12, 19):
        t.hline(11, 20, y, window_pal[0])
    t.ao(10, 6, 21, 25, k=0.22)


@tex("block/motor_side")
def motor_side(name):
    """Электромотор: стальной оребрённый корпус, окно на медную обмотку (как на старой текстуре)."""
    t = Tex()
    motor_side_body(t, STEEL, 2, COPPER, 121)
    # шильдик на поясе
    t.rect(13, 1, 18, 2, BRASS[3])
    return t


@tex("block/motor_end")
def motor_end(name):
    """Торец мотора (и ступицы ветроколеса): подшипниковый щит, кольцо вентиляционных окон, выход вала."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.25, seed=131)
    t.disc(16, 16, 13.5, STEEL, base=2)
    t.ring(16, 16, 13, 14, STEEL, base=1)
    # вентиляционные окна по окружности
    for i in range(12):
        a = 2 * math.pi * i / 12
        x, y = 16 + 9.5 * math.cos(a), 16 + 9.5 * math.sin(a)
        t.rect(int(x) - 1, int(y) - 1, int(x), int(y), BLACK[1])
        t.put(int(x) - 1, int(y) - 1, BLACK[0])
    t.disc(16, 16, 6.5, STEEL, base=3)
    t.ring(16, 16, 6, 7, STEEL, base=1)
    t.disc(16, 16, 3.5, DARK_STEEL, base=2)
    t.disc(16, 16, 2, STEEL, base=3)
    bolt_circle(t, 16, 16, 14.2, 4, STEEL, phase=math.pi / 4)
    return t


@tex("block/despin_motor_side")
def despin_motor_side(name):
    """Мотор противовращения: белый «космический» корпус, окно обмотки, сигнальные пояса."""
    t = Tex()
    motor_side_body(t, WHITE_PAINT, 2, COPPER, 141)
    for y in (4, 27):
        t.hline(0, 31, y, AMBER[3])
    return t


@tex("block/despin_motor_end")
def despin_motor_end(name):
    """Торец мотора противовращения: опорно-поворотное кольцо с зубчатым венцом (оранжевое),
    внутри — неподвижная платформа с метками."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.15, seed=151)

    def f(x, y, r, a):
        dx, dy = x + 0.5 - 16, y + 0.5 - 16
        if 11 <= r < 14.5:
            teeth = (int((a + math.pi) / (2 * math.pi) * 36) % 2 == 0)
            if r >= 13.5 and not teeth:
                return
            t.put(x, y, AMBER[max(0, min(4, lit_k(dx, dy, r, 3, 0.5)))])
        elif 9.5 <= r < 11:
            t.put(x, y, BLACK[2])
    radial(t, 16, 16, f)
    t.disc(16, 16, 9.5, DARK_STEEL, base=2)
    t.ring(16, 16, 5, 6, AMBER, base=2)
    t.disc(16, 16, 3, STEEL, base=3)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        t.rivet(int(16 + 7.5 * math.cos(a)) - 1, int(16 + 7.5 * math.sin(a)) - 1, STEEL)
    return t


@tex("block/clutch_side")
def clutch_side(name):
    """Бок фрикционной муфты: корпус, в окне — пакет дисков (сталь/накладки) поперёк оси."""
    t = Tex()
    housing(t, STEEL, base=2, seed=161)
    t.recess(4, 9, 27, 22, BLACK, base=1)
    for i, y in enumerate(range(10, 22)):
        pal = FRICTION if i % 3 == 1 else STEEL
        base = 2 if pal is FRICTION else 3
        t.hline(5, 26, y, pal[base])
        t.put(5, y, pal[4])
        t.put(26, y, pal[1])
    t.ao(4, 9, 27, 22, k=0.25)
    # болты по краю окна
    for x in (6, 24):
        t.screw(x, 5, STEEL)
        t.screw(x, 24, STEEL)
    return t


@tex("block/clutch_end")
def clutch_end(name):
    """Торец муфты: нажимной диск, кольцо фрикционной накладки с радиальными канавками, шлицевая ступица."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.25, seed=171)
    t.disc(16, 16, 14, STEEL, base=2)

    def f(x, y, r, a):
        dx, dy = x + 0.5 - 16, y + 0.5 - 16
        if 8 <= r < 12.5:
            groove = abs(((a + math.pi) / (2 * math.pi) * 8) % 1 - 0.5) > 0.44
            k = 0 if groove else lit_k(dx, dy, r, 2, 0.5)
            t.put(x, y, FRICTION[max(0, min(4, k))])
    radial(t, 16, 16, f)
    t.ring(16, 16, 12.5, 13.5, STEEL, base=3)
    t.disc(16, 16, 7.5, STEEL, base=3)
    t.disc(16, 16, 4, DARK_STEEL, base=2)
    # шлицы
    for i in range(6):
        a = 2 * math.pi * i / 6
        t.put(int(16 + 3.2 * math.cos(a)), int(16 + 3.2 * math.sin(a)), STEEL[3])
    t.disc(16, 16, 2, BLACK, base=1, lit=False)
    return t


@tex("block/lathe_side")
def lathe_side(name):
    """Бок токарного станка: станина в станочной краске, призматические направляющие, фартук суппорта
    со штурвалом по центру."""
    t = Tex()
    t.panel(0, 0, 31, 31, MACHINE_GREEN, base=2, grain=0.2, seed=181)
    # направляющие (шлифованная сталь) сверху и снизу
    for y0 in (2, 26):
        t.brushed(STEEL, base=3, x0=0, y0=y0, x1=31, y1=y0 + 3, horizontal=True, seed=182 + y0)
        t.hline(0, 31, y0, STEEL[4])
        t.hline(0, 31, y0 + 3, STEEL[1])
    # фартук
    t.panel(8, 8, 23, 23, MACHINE_GREEN, base=3, grain=0.1, seed=184)
    t.ao(8, 8, 23, 23, k=0.12)
    # штурвал
    t.ring(16, 16, 5, 6.5, STEEL, base=3)
    for i in range(3):
        a = -math.pi / 2 + 2 * math.pi * i / 3
        for s in range(2, 6):
            t.put(int(16 + s * math.cos(a)), int(16 + s * math.sin(a)), STEEL[2])
    t.disc(16, 16, 2, STEEL, base=3)
    return t


@tex("block/lathe_end")
def lathe_end(name):
    """Торец токарного: трёхкулачковый патрон на передней бабке — кулачки ступенями по радиусам."""
    t = Tex()
    t.panel(0, 0, 31, 31, MACHINE_GREEN, base=2, grain=0.2, seed=191)
    t.bevel(2, 2, 29, 29, MACHINE_GREEN, raised=False, base=2)
    t.disc(16, 16, 12, STEEL, base=2)
    t.ring(16, 16, 11.2, 12, STEEL, base=1)
    for i in range(3):
        a = -math.pi / 2 + 2 * math.pi * i / 3
        ca, sa = math.cos(a), math.sin(a)
        # паз под кулачок и сам кулачок: прямоугольник в повёрнутых координатах
        for y in range(32):
            for x in range(32):
                dx, dy = x + 0.5 - 16, y + 0.5 - 16
                u, v = dx * ca + dy * sa, -dx * sa + dy * ca
                if 3.5 <= u <= 11 and abs(v) <= 2.2:
                    t.put(x, y, BLACK[2])
                if 4.5 <= u <= 10 and abs(v) <= 1.5:
                    step = u < 7
                    k = 4 if v < -0.5 else 3 if v < 0.5 else 1
                    t.put(x, y, STEEL[k] if step else STEEL[max(0, k - 1)])
    t.disc(16, 16, 3.2, BLACK, base=1, lit=False)
    t.ring(16, 16, 3.2, 4.2, STEEL, base=3)
    return t


@tex("block/spin_hub_side")
def spin_hub_side(name):
    """Бок ступицы вращения: токосъёмник — медные кольца со щётками, стальные пояса."""
    t = Tex()
    housing(t, STEEL, base=2, seed=201)
    t.recess(0, 8, 31, 23, BLACK, base=1)
    for y in (10, 14, 18):
        t.pipe_h(0, 31, y + 1, 1, COPPER)
    # щётки — симметрично
    for x in (4, 12, 19, 27):
        t.panel(x - 1, 9, x + 1, 22, AMBER, base=3, grain=0, seed=202)
    t.ao(0, 8, 31, 23, k=0.2)
    return t


@tex("block/spin_hub_end")
def spin_hub_end(name):
    """Торец ступицы: колесо со спицами (как станционное кольцо), ступица с подшипником."""
    t = Tex()
    t.panel(0, 0, 31, 31, DARK_STEEL, base=2, grain=0.2, seed=211)
    t.ring(16, 16, 11.5, 14.5, STEEL, base=3)
    t.ring(16, 16, 12.5, 13.5, STEEL, base=2)
    for i in range(8):
        a = 2 * math.pi * i / 8
        for s in range(5, 12):
            x, y = 16 + s * math.cos(a), 16 + s * math.sin(a)
            t.put(int(x), int(y), STEEL[3])
            t.put(int(x + 0.7), int(y + 0.7), STEEL[1])
    t.disc(16, 16, 5.5, STEEL, base=2)
    t.disc(16, 16, 3, DARK_STEEL, base=1)
    t.disc(16, 16, 1.5, STEEL, base=4)
    return t


@tex("block/rim_thruster_front")
def rim_thruster_front(name):
    """Сопло ободного двигателя: срез раструба, темнеющий к критике, в горле — копоть."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.15, seed=221)
    t.disc(16, 16, 13, STEEL, base=2)
    for r, k in ((12, 3), (11, 2), (9.5, 1), (8, 0)):
        t.disc(16, 16, r, GUNMETAL, base=k, lit=True)
    t.disc(16, 16, 6, BLACK, base=1, lit=False)
    t.disc(16, 16, 3.5, FRICTION, base=0, lit=False)
    t.glow(16, 16, 5, FRICTION[2], 0.35)
    t.ring(16, 16, 12.5, 13.5, STEEL, base=3)
    t.rivets(inset=2, pal=STEEL)
    return t


@tex("block/rim_thruster_side")
def rim_thruster_side(name):
    """Бок ободного двигателя: белый корпус, по центру — оранжевая магистраль топлива, лючки."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.15, seed=231)
    t.bevel(2, 2, 29, 29, WHITE_PAINT, raised=False, base=2)
    t.recess(13, 3, 18, 28, DARK_STEEL, base=1)
    t.pipe_v(15, 3, 28, 1, AMBER)
    t.pipe_v(16, 3, 28, 1, AMBER)
    for y in (8, 22):
        t.rect(13, y, 18, y + 1, STEEL[3])
    for x0 in (5, 21):
        t.panel(x0, 12, x0 + 5, 19, WHITE_PAINT, base=3, grain=0, seed=232)
        t.screw(x0 + 1, 14, STEEL)
    return t


# ============================================================== газовые баллоны (ГОСТ 949)
GAS = {
    "none": (STEEL, 2, None, None),
    "oxygen": (O2_BLUE, 2, BLACK, None),                # голубой, надпись чёрная
    "nitrogen": (BLACK, 2, HAZARD_YELLOW, FRICTION),    # чёрный, надпись жёлтая, коричневая полоса
}


@tex(*[f"block/gas_tank_{g}_{i}" for g in ("none", "oxygen", "nitrogen") for i in range(5)])
def gas_tank_side(name):
    """Бок газового баллона: окраска по ГОСТ 949, строка «надписи» вверху по центру, стальные пояса,
    смотровая шкала давления по центру (4 секции снизу вверх)."""
    _, _, gas, lvl = name.split("/")[1].split("_")
    level = int(lvl)
    body, base, text, stripe = GAS[gas]
    t = Tex()
    if gas == "none":
        t.brushed(STEEL, base=2, horizontal=False, seed=241)
    else:
        t.fill(body, base=base, grain=0.05, seed=241)
    # мягкий объём: левее светлее, правее темнее
    for x in range(32):
        k = (15.5 - x) / 15.5
        for y in range(32):
            if k > 0.55:
                t.lighten(x, y, 0.06)
            elif k < -0.6:
                t.darken(x, y, 0.1)
    for y0 in (0, 29):
        t.panel(0, y0, 31, y0 + 2, STEEL, base=3 if y0 == 0 else 2, grain=0.1, seed=242)
    if text:
        # строка надписи из «букв» 2×3 — без реального текста, но читается как маркировка
        for i, x in enumerate(range(7, 25, 3)):
            if _h(i, 1, 243) > 0.15:
                t.rect(x, 5, x + 1, 7, text[3])
                if _h(i, 2, 244) > 0.5:
                    t.put(x + 1, 6, body[base])
    if stripe:
        t.rect(0, 26, 31, 27, stripe[3])
        t.hline(0, 31, 26, stripe[4])
    level_gauge(t, level, 13, 18, 11, 23)
    return t


@tex("block/gas_tank_top")
def gas_tank_top(name):
    """Торец баллона: сферическое днище, защитное кольцо, в центре — латунный вентиль с маховичком."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.2, seed=251)
    t.disc(16, 16, 13.5, STEEL, base=2)
    t.ring(16, 16, 12.5, 14, STEEL, base=3)
    t.ring(16, 16, 7, 8.5, STEEL, base=1)
    t.disc(16, 16, 6, BRASS, base=2)
    for i in range(4):
        a = math.pi / 4 + i * math.pi / 2
        for s in range(2, 6):
            t.put(int(16 + s * math.cos(a)), int(16 + s * math.sin(a)), BRASS[4] if i < 2 else BRASS[1])
    t.ring(16, 16, 4.5, 6, BRASS, base=3)
    t.disc(16, 16, 2, RED, base=2)
    return t


# ============================================================== станция: атмосфера, гидропоника, свет
def fan(t, cx, cy, r, on):
    """Вентилятор в решётчатом кожухе. Выключен — видны 5 лопастей, включён — размытый диск."""
    t.disc(cx, cy, r, BLACK, base=1, lit=False)

    def f(x, y, rr, a):
        if 3 <= rr < r - 0.8:
            dx, dy = x + 0.5 - cx, y + 0.5 - cy
            if on:
                c = mix(STEEL[1], STEEL[2], 0.5)
                if int(rr) % 3 == 0:
                    c = STEEL[2]
                t.put(x, y, c)
            else:
                ph = ((a + rr * 0.12) / (2 * math.pi) * 5) % 1
                if ph < 0.42:
                    t.put(x, y, STEEL[max(0, min(4, lit_k(dx, dy, rr, 2, 0.4)))])
    radial(t, cx, cy, f)
    t.disc(cx, cy, 3, STEEL, base=3)
    t.ring(cx, cy, r - 1, r + 0.5, STEEL, base=3)
    # спицы защитной решётки
    for i in range(4):
        a = i * math.pi / 2
        for s in range(3, int(r)):
            t.put(int(cx + s * math.cos(a)), int(cy + s * math.sin(a)), DARK_STEEL[3])


@tex("block/atmosphere_controller", "block/atmosphere_controller_on")
def atmosphere_controller(name):
    """Контроллер атмосферы: панель с вентилятором циркуляции, щелевые решётки по бокам, сигнальные
    светодиоды сверху; включён — вентилятор размыт, светится голубым, индикаторы зелёные."""
    on = name.endswith("_on")
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.12, seed=261)
    t.bevel(2, 2, 29, 29, WHITE_PAINT, raised=False, base=2)
    t.recess(5, 7, 26, 28, DARK_STEEL, base=1)
    fan(t, 16, 17.5, 9.5, on)
    if on:
        t.glow(16, 17.5, 11, CYAN[4], 0.18)
    # индикаторы
    for x in (10, 15, 20):
        t.led(x, 3, GREEN if on else RED, on=True if on else x == 15)
    # оранжевая полоса «воздушной» магистрали (узнаваемость)
    t.hline(5, 26, 29, AMBER[3])
    return t


@tex("block/hydroponic_tray")
def hydroponic_tray(name):
    """Лоток гидропоники: белый пластик; видимая боковина — нижняя половина: борт с отбортовкой,
    смотровая полоса раствора по центру, дренажные штуцеры."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PLASTIC, base=2, grain=0.1, seed=271)
    # отбортовка верхней кромки видимой части
    t.rect(0, 16, 31, 18, WHITE_PLASTIC[4])
    t.hline(0, 31, 18, WHITE_PLASTIC[1])
    # смотровое окно раствора
    t.recess(6, 21, 25, 26, BLACK, base=1)
    t.rect(7, 23, 24, 25, NUTRIENT[3])
    t.hline(7, 24, 23, NUTRIENT[4])
    t.rect(7, 22, 24, 22, GLASS[2])
    for x in (3, 27):
        t.disc(x + 0.5, 23.5, 1.5, STEEL, base=2)
    t.hline(0, 31, 31, WHITE_PLASTIC[0])
    # дно (верхняя половина видна только снизу) — рёбра
    for y in (4, 10):
        t.hline(2, 29, y, WHITE_PLASTIC[1])
        t.hline(2, 29, y + 1, WHITE_PLASTIC[4])
    return t


@tex("block/hydroponic_tray_top")
def hydroponic_tray_top(name):
    """Верх лотка: крышка с решёткой сетчатых стаканов 4×4 с керамзитом над слоем питательного раствора."""
    t = Tex()
    t.fill(NUTRIENT, base=2, grain=0.3, seed=281)
    for y in range(32):
        for x in range(32):
            if _h(x, y, 282) > 0.93:
                t.put(x, y, NUTRIENT[4])
    t.bevel(0, 0, 31, 31, WHITE_PLASTIC, raised=True, base=2)
    t.bevel(1, 1, 30, 30, WHITE_PLASTIC, raised=False, base=2)
    for j in range(4):
        for i in range(4):
            cx, cy = 4.5 + i * 7.66, 4.5 + j * 7.66
            t.ring(cx, cy, 2.2, 3.2, BLACK, base=2)
            t.blob(cx, cy, 2.1, 2.1, CLAY, base=2, seed=283 + i * 4 + j, rough=0.3)
    return t


@tex("block/grow_lamp", "block/grow_lamp_on")
def grow_lamp(name):
    """Лампа роста: алюминиевый радиатор (бока — строки 0…7) и матрица красных/синих светодиодов
    (низ — 2…29), по центру симметрично."""
    on = name.endswith("_on")
    t = Tex()
    t.brushed(ALUMINIUM, base=2, horizontal=True, seed=291)
    # рёбра радиатора на боках
    for x in range(1, 32, 3):
        t.vline(x, 0, 7, ALUMINIUM[4])
        t.vline(x + 1, 0, 7, ALUMINIUM[1])
    t.hline(0, 31, 7, ALUMINIUM[0])
    # матрица светодиодов
    t.recess(7, 7, 24, 24, BLACK, base=1)
    for j in range(4):
        for i in range(4):
            x, y = 9 + i * 4, 9 + j * 4
            pal = RED if (i + j) % 2 == 0 else BLUE
            if on:
                t.rect(x, y, x + 1, y + 1, pal[4])
                t.put(x + 1, y + 1, pal[3])
            else:
                t.rect(x, y, x + 1, y + 1, pal[1])
                t.put(x, y, pal[2])
    if on:
        t.glow(16, 16, 12, (0xE0, 0x60, 0xE0), 0.3)
    t.rivets(inset=3, pal=ALUMINIUM)
    return t


@tex("block/lunar_bricks")
def lunar_bricks(name):
    """Кирпич из спечённого реголита: ряды по 8 px со сдвигом на полкирпича, тонкий шов, кромки с фаской."""
    t = Tex()
    t.fill(REGOLITH, base=0, grain=0, seed=301)
    for row in range(4):
        y0 = row * 8
        off = 0 if row % 2 == 0 else 8
        for b in range(-1, 2):
            x0 = off + b * 16
            seed = 302 + row * 3 + b
            base = 2 if _h(row, b, 303) < 0.6 else (1 if _h(row, b, 304) < 0.5 else 3)
            for y in range(y0 + 1, y0 + 8):
                for x in range(x0 + 1, x0 + 16):
                    if 0 <= x < 32:
                        h = _h(x // 2, y // 2, seed)
                        k = base + (1 if h > 0.88 else -1 if h < 0.1 else 0)
                        t.put(x, y, REGOLITH[max(0, min(4, k))])
            for x in range(x0 + 1, x0 + 16):
                t.put(x % 32 if 0 <= x < 32 else -1, y0 + 1, REGOLITH[min(4, base + 1)])
                t.put(x if 0 <= x < 32 else -1, y0 + 7, REGOLITH[max(0, base - 1)])
            # поры спекания
            for k in range(3):
                px = x0 + 3 + int(_h(k, row, seed) * 10)
                py = y0 + 3 + int(_h(row, k, seed) * 3)
                t.put(px, py, REGOLITH[0])
    return t


# ============================================================== ровер
@tex("block/rover_body")
def rover_body(name):
    """Шасси ровера (по LRV): алюминиевая палуба, трубчатые лонжероны (столбцы 8…9 и 22…23 — их берёт
    мачта антенны), поперечины."""
    t = Tex()
    t.brushed(ALUMINIUM, base=2, horizontal=False, seed=311)
    for y in (3, 15, 27):
        t.pipe_h(0, 31, y, 1, ALUMINIUM)
    for x in (8, 22):
        t.rect(x, 0, x + 1, 31, ALUMINIUM[3])
        t.vline(x, 0, 31, ALUMINIUM[4])
        t.vline(x + 1, 0, 31, ALUMINIUM[1])
    # борт (строки 16…21): тёмная кромка рамы
    t.hline(0, 31, 20, ALUMINIUM[0])
    for x, y in ((3, 8), (27, 8), (3, 22), (27, 22)):
        t.rivet(x, y, ALUMINIUM)
    return t


@tex("block/rover_seat")
def rover_seat(name):
    """Сиденье ровера: синие нейлоновые ремни, сплетённые на алюминиевой раме (как у LRV)."""
    t = Tex()
    t.fill(DARK_STEEL, base=1, grain=0, seed=321)
    for y in range(32):
        for x in range(32):
            band_h = (y % 6) < 5
            band_v = (x % 6) < 5
            over = ((x // 6) + (y // 6)) % 2 == 0
            if band_h and (over or not band_v):
                k = 3 if y % 6 == 0 else (1 if y % 6 == 4 else 2)
                t.put(x, y, BLUE[k])
            elif band_v:
                k = 3 if x % 6 == 0 else (1 if x % 6 == 4 else 2)
                t.put(x, y, mix(BLUE[k], (0, 0, 0), 0.15))
    return t


@tex("block/rover_panel")
def rover_panel(name):
    """Пульт ровера: тёмная панель; лицевая часть (x 4…27, y 12…31) — три индикатора, две шкалы
    и рукоять; верх — сетка отражателя антенны."""
    t = Tex()
    t.fill(GUNMETAL, base=1, grain=0.15, seed=331)
    for y in range(0, 12):
        for x in range(0, 32):
            if x % 4 == 0 or y % 4 == 0:
                t.put(x, y, GUNMETAL[2])
    t.bevel(4, 12, 27, 31, GUNMETAL, raised=True, base=1)
    for x, pal in ((10, AMBER), (15, GREEN), (20, RED)):
        t.led(x, 15, pal, on=True)
    for cx in (10.5, 21.5):
        t.disc(cx, 22.5, 3.5, BLACK, base=2)
        t.ring(cx, 22.5, 3, 3.8, STEEL, base=2)
        t.put(int(cx), 20, WHITE_PAINT[4])
        t.put(int(cx), 21, WHITE_PAINT[3])
    # Т-образная рукоять управления по центру
    t.rect(15, 20, 16, 27, STEEL[2])
    t.hline(13, 18, 20, STEEL[4])
    t.hline(12, 19, 28, STEEL[3])
    t.hline(12, 19, 29, STEEL[1])
    return t


@tex("block/rover_battery")
def rover_battery(name):
    """Никель-железная батарея ровера: ряд банок в оливковом кожухе (бок — строки 4…15), сверху —
    пробки банок и межэлементные перемычки (строки 16…25)."""
    t = Tex()
    t.fill(NIFE, base=2, grain=0.2, seed=341)
    for x in range(0, 32, 4):
        t.vline(x, 0, 31, NIFE[0])
        t.vline(x + 1, 0, 31, NIFE[4])
        t.vline(x + 3, 0, 31, NIFE[1])
    t.hline(0, 31, 4, NIFE[4])
    t.hline(0, 31, 15, NIFE[0])
    for x in range(1, 32, 4):
        t.disc(x + 1.5, 19.5, 1.3, STEEL, base=3)
        t.put(x + 1, 23, COPPER[3])
        t.put(x + 2, 23, COPPER[2])
        t.put(x + 3, 23, COPPER[1])
    t.hline(1, 30, 22, COPPER[4])
    t.hline(1, 30, 24, COPPER[1])
    return t


@tex("block/rover_charger_side")
def rover_charger_side(name):
    """Бок зарядной стойки: тёмная тумба, по центру — оранжевый кабель-канал, решётки охлаждения."""
    t = Tex()
    housing(t, GUNMETAL, base=2, seed=351)
    t.recess(13, 4, 18, 27, BLACK, base=1)
    t.pipe_v(15, 4, 27, 1, AMBER)
    t.pipe_v(16, 4, 27, 1, AMBER)
    for x0 in (4, 21):
        t.grille(x0, 8, x0 + 6, 23, GUNMETAL, pitch=3)
    return t


@tex("block/rover_charger_top")
def rover_charger_top(name):
    """Верх зарядной стойки: разъём с контактами по центру, индикатор готовности, крепёж."""
    t = Tex()
    t.panel(0, 0, 31, 31, GUNMETAL, base=2, grain=0.2, seed=361)
    t.bevel(2, 2, 29, 29, GUNMETAL, raised=False, base=2)
    t.disc(16, 16, 7, BLACK, base=2)
    t.ring(16, 16, 6.5, 8, AMBER, base=3)
    for dx, dy in ((-2.5, -2), (2.5, -2), (0, 2.5)):
        t.disc(16 + dx, 16 + dy, 1.2, COPPER, base=3)
    t.led(15, 4, GREEN, on=True)
    t.rivets(inset=3, pal=STEEL)
    return t


@tex("block/rover_wheel")
def rover_wheel(name):
    """Шина LRV: сетка из оцинкованной рояльной проволоки (одинаково читается и на боку, и на протекторе)."""
    t = Tex()
    t.fill(DARK_STEEL, base=0, grain=0, seed=371)
    for y in range(32):
        for x in range(32):
            a, b = (x + y) % 4, (x - y) % 4
            if a == 0:
                t.put(x, y, STEEL[3] if ((x - y) // 4) % 2 == 0 else STEEL[2])
            elif b == 0:
                t.put(x, y, STEEL[2] if ((x + y) // 4) % 2 == 0 else STEEL[3])
            elif a == 1 or b == 1:
                t.put(x, y, DARK_STEEL[2])
    return t


@tex("block/rover_wheel_chevron")
def rover_wheel_chevron(name):
    """Титановые шевроны протектора: V-образные грунтозацепы (на полосе x 12…19 и на боку)."""
    t = Tex()
    t.fill(DARK_STEEL, base=1, grain=0, seed=381)
    for y in range(32):
        for x in range(32):
            v = (y + abs(x - 15.5) * 0.8) % 8
            if v < 3:
                k = 4 if v < 1 else (3 if v < 2 else 1)
                t.put(x, y, TITANIUM[k])
    return t


@tex("block/rover_wheel_rim")
def rover_wheel_rim(name):
    """Обод колеса (диск 7…24): алюминиевые кольца с бликом сверху-слева, лёгкие отверстия."""
    t = Tex()
    t.fill(ALUMINIUM, base=1, grain=0.1, seed=391)
    turned_rings(t, 16, 16, 0, 24, ALUMINIUM, 2, step=3, seed=392)
    for i in range(6):
        a = 2 * math.pi * i / 6 + math.pi / 6
        t.disc(16 + 6.5 * math.cos(a), 16 + 6.5 * math.sin(a), 1.2, DARK_STEEL, base=1, lit=False)
    t.ring(16, 16, 8.5, 9.2, ALUMINIUM, base=4)
    return t


@tex("block/rover_wheel_hub")
def rover_wheel_hub(name):
    """Ступица (11…20): мотор-колесо с крышкой и пятью болтами, оранжевый колпак."""
    t = Tex()
    t.fill(GUNMETAL, base=1, grain=0.1, seed=401)
    t.disc(16, 16, 5, GUNMETAL, base=2)
    bolt_circle(t, 16, 16, 3.6, 5, STEEL, phase=-math.pi / 2)
    t.disc(16, 16, 1.8, AMBER, base=3)
    return t
