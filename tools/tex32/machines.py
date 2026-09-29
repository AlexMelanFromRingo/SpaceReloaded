"""Машины 004–006: химия, электролиз, печи, полупроводниковая линия, масс-драйвер, катушки.
Пары *_front / *_front_on рисуются одной функцией: во включённом состоянии — свечение, огонь, экран и
зелёный индикатор по центру верхней кромки."""
import math

from . import tex
from .kit import (AMBER, ALUMINIUM, BLACK, BLUE, BRASS, CERAMIC, COPPER, CYAN, DARK_STEEL, GLASS, GREEN,
                  GUNMETAL, O2_BLUE, RED, STEEL, TITANIUM, WHITE_PAINT, Tex, shade)

# ------------------------------------------------------------------ палитры группы
ACID = [(0x3E, 0x6A, 0x58), (0x5C, 0x92, 0x7A), (0x84, 0xBC, 0x9E), (0xAE, 0xDC, 0xC0), (0xDC, 0xF6, 0xE6)]
WATER = [(0x12, 0x36, 0x6A), (0x1C, 0x52, 0x94), (0x2C, 0x70, 0xBC), (0x5A, 0x9C, 0xDC), (0xA8, 0xD4, 0xF4)]
REACT_GREEN = [(0x12, 0x30, 0x22), (0x1C, 0x4C, 0x34), (0x2A, 0x6C, 0x48), (0x4C, 0xA0, 0x68), (0x9A, 0xE0, 0xAA)]
FIRE = [(0x7A, 0x1E, 0x06), (0xC2, 0x42, 0x0A), (0xF0, 0x7C, 0x16), (0xFF, 0xB8, 0x3A), (0xFF, 0xEC, 0xA8)]
PHOTO_YELLOW = [(0x7A, 0x62, 0x10), (0xA8, 0x8A, 0x1A), (0xD4, 0xB4, 0x2A), (0xF0, 0xD8, 0x4A), (0xFF, 0xF2, 0x9C)]
UV = [(0x2E, 0x14, 0x52), (0x48, 0x22, 0x80), (0x6C, 0x3A, 0xB4), (0x9C, 0x6A, 0xE0), (0xD2, 0xB4, 0xFA)]
OIL = [(0x1E, 0x10, 0x06), (0x36, 0x1E, 0x0C), (0x56, 0x32, 0x14), (0x7A, 0x4C, 0x20), (0xA8, 0x74, 0x36)]
FIRECLAY = [(0x5E, 0x4C, 0x3A), (0x7C, 0x66, 0x4E), (0x9A, 0x82, 0x66), (0xB6, 0x9E, 0x80), (0xD2, 0xBE, 0xA0)]
LUNAR = [(0x4E, 0x4A, 0x44), (0x66, 0x61, 0x59), (0x7E, 0x78, 0x6E), (0x96, 0x90, 0x85), (0xB2, 0xAC, 0xA0)]
SC_BLUE = [(0x1E, 0x5A, 0x86), (0x2E, 0x84, 0xB6), (0x4E, 0xAE, 0xDA), (0x86, 0xD2, 0xF0), (0xD0, 0xF2, 0xFC)]
HOT = [(0x6A, 0x1A, 0x08), (0xB4, 0x3A, 0x0C), (0xE8, 0x70, 0x18), (0xFF, 0xB0, 0x40), (0xFF, 0xF0, 0xC0)]


# ------------------------------------------------------------------ общие детали
def shell(t, pal=STEEL, base=2, seed=31, rivets=True):
    """Корпус машины: выпуклый обод 2 px, внутренняя кромка утоплена, заклёпки по углам."""
    t.panel(0, 0, 31, 31, pal, base=base, grain=0.25, seed=seed)
    t.bevel(2, 2, 29, 29, pal, raised=False, base=base)
    if rivets:
        t.rivets(inset=3, pal=pal if pal is not WHITE_PAINT else STEEL)


def status(t, on):
    """Индикатор работы по центру верхней кромки (зелёный — работает, тёмный — нет)."""
    t.rect(13, 1, 18, 4, BLACK[1])
    t.hline(13, 18, 1, BLACK[0])
    t.led(14, 2, GREEN, on)
    t.led(16, 2, AMBER, False)
    if on:
        t.glow(15, 3, 4, GREEN[4], 0.25)


def window(t, x0, y0, x1, y1, pal, base=2, seed=41):
    """Смотровое окно в утопленной раме: заливка, диагональный блик по стеклу."""
    t.recess(x0 - 1, y0 - 1, x1 + 1, y1 + 1, DARK_STEEL, base=1)
    t.fill(pal, base, x0, y0, x1, y1, grain=0.2, seed=seed)
    for i in range(3):
        for k in range(min(x1 - x0, y1 - y0)):
            x, y = x0 + 2 + i + k, y0 + k
            if x <= x1 and y <= y1 and k < 5:
                t.lighten(x, y, 0.18)


def bubbles(t, x0, y0, x1, y1, pal, seed=5, n=9):
    """Пузырьки 1–2 px, светлые с тёмной кромкой снизу."""
    for i in range(n):
        h1 = (i * 7919 + seed * 104729) % 997 / 997
        h2 = (i * 6271 + seed * 7331) % 991 / 991
        x = x0 + 1 + int(h1 * (x1 - x0 - 2))
        y = y0 + 1 + int(h2 * (y1 - y0 - 2))
        t.put(x, y, pal[4])
        if i % 3 == 0:
            t.put(x + 1, y, pal[3])
            t.put(x, y + 1, pal[3])
            t.put(x + 1, y + 1, pal[2])


def flange_band(t, y0, y1, pal=STEEL):
    """Горизонтальный фланец/бандаж по всей ширине с болтами."""
    t.panel(0, y0, 31, y1, pal, base=2, grain=0.1)
    for x in range(3, 30, 6):
        t.rivet(x, (y0 + y1) // 2, pal)


def cyl_shade(t, x0, x1, y0, y1, k=0.22):
    """Цилиндр вдоль вертикали: блик слева от центра, тень к правому краю."""
    w = x1 - x0
    for x in range(x0, x1 + 1):
        u = (x - x0) / max(1, w) * 2 - 1
        s = 0.35 - u
        for y in range(y0, y1 + 1):
            if s > 0.6:
                t.lighten(x, y, k * 0.6)
            elif u > 0.55:
                t.darken(x, y, k * (u - 0.4))


# ================================================================== химия и газ
@tex("block/air_separator_side")
def air_separator_side(name):
    """Колонна воздухоразделения: белый цилиндр с инеем, по центру — мерное стекло жидкого O₂, сварные швы."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.15, seed=51)
    cyl_shade(t, 0, 31, 0, 31)
    for y in (7, 23):
        t.hline(0, 31, y, WHITE_PAINT[0])
        t.hline(0, 31, y + 1, WHITE_PAINT[4])
    # мерное стекло
    t.recess(13, 3, 18, 28, DARK_STEEL, base=1)
    t.rect(14, 4, 17, 11, GLASS[2])
    t.rect(14, 12, 17, 27, O2_BLUE[3])
    t.vline(14, 12, 27, O2_BLUE[4])
    t.vline(17, 12, 27, O2_BLUE[1])
    t.hline(14, 17, 12, O2_BLUE[4])
    for y in (4, 27):
        t.hline(12, 19, y - 2 if y == 4 else y + 2, STEEL[3])
    # иней пятнами
    for x, y in ((4, 3), (25, 12), (6, 18), (27, 27), (3, 29), (24, 4)):
        t.put(x, y, (0xF4, 0xFA, 0xFF))
        t.put(x + 1, y, (0xE4, 0xF0, 0xFA))
    return t


@tex("block/air_separator_top")
def air_separator_top(name):
    """Торец колонны: белая крышка, по центру — патрубок с голубым фланцем (кислород)."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.12, seed=52)
    t.ring(15.5, 15.5, 11, 13, WHITE_PAINT, base=1)
    t.disc(15.5, 15.5, 8, O2_BLUE, base=2)
    t.disc(15.5, 15.5, 4.5, DARK_STEEL, base=1, lit=False)
    t.disc(15.5, 15.5, 2.5, BLACK, base=0, lit=False)
    for a in range(8):
        x = 15.5 + 6.3 * math.cos(a * math.pi / 4)
        y = 15.5 + 6.3 * math.sin(a * math.pi / 4)
        t.put(int(x), int(y), STEEL[4])
    return t


@tex("block/airlock_pump_side")
def airlock_pump_side(name):
    """Насос шлюза: улитка насоса по центру, патрубки влево и вправо, основание снизу."""
    t = Tex()
    shell(t, seed=53)
    t.pipe_h(3, 28, 16, 3, COPPER)
    for x in (4, 26):
        t.panel(x - 1, 11, x + 2, 21, STEEL, base=3)
    t.disc(15.5, 16, 9, GUNMETAL, base=2)
    t.ring(15.5, 16, 7, 9, DARK_STEEL, base=2)
    t.disc(15.5, 16, 4, STEEL, base=2)
    t.disc(15.5, 16, 1.6, BLACK, base=1, lit=False)
    return t


@tex("block/airlock_pump_top")
def airlock_pump_top(name):
    """Верх насоса: решётка забора воздуха в утопленной рамке."""
    t = Tex()
    shell(t, seed=54)
    t.recess(6, 6, 25, 25, DARK_STEEL, base=1)
    t.grille(8, 8, 23, 23, GUNMETAL, pitch=3)
    return t


@tex("block/assembly_table_top", "block/assembly_table_top_on")
def assembly_table_top(name):
    """Сборочный стол: плита с Т-пазами, угловые упоры; включён — разметка подсвечена."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, GUNMETAL, seed=55, rivets=False)
    t.fill(GUNMETAL, 2, 3, 3, 28, 28, grain=0.15, seed=56)
    for p in (10, 21):
        t.hline(3, 28, p, DARK_STEEL[0])
        t.hline(3, 28, p + 1, GUNMETAL[4])
        t.vline(p, 3, 28, DARK_STEEL[0])
        t.vline(p + 1, 3, 28, GUNMETAL[4])
    mark = CYAN if on else AMBER
    for x, y in ((4, 4), (26, 4), (4, 26), (26, 26)):
        t.rect(x, y, x + 1, y + 1, mark[3])
        t.put(x, y, mark[4])
    for x, y in ((15, 15),):
        t.rect(x - 1, y, x + 2, y + 1, mark[3] if on else AMBER[2])
        t.rect(x, y - 1, x + 1, y + 2, mark[3] if on else AMBER[2])
    if on:
        for p in (10, 21):
            for q in range(3, 29):
                t.blend(q, p, CYAN[3], 0.6)
                t.blend(p, q, CYAN[3], 0.6)
        t.glow(15.5, 15.5, 8, CYAN[4], 0.2)
    status(t, on)
    return t


@tex("block/atmospheric_collector_front", "block/atmospheric_collector_front_on")
def atmospheric_collector_front(name):
    """Коллектор атмосферы: круглый заборник с сеткой; включён — внутри вращается подсвеченная крыльчатка."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=57)
    t.disc(15.5, 16.5, 12, STEEL, base=3)
    t.disc(15.5, 16.5, 10, BLACK, base=1, lit=False)
    # лопасти
    for a in range(6):
        ang = a * math.pi / 3 + (0.3 if on else 0)
        for r in range(2, 9):
            x = 15.5 + r * math.cos(ang + r * 0.08)
            y = 16.5 + r * math.sin(ang + r * 0.08)
            t.put(int(x), int(y), (BLUE if on else GUNMETAL)[3])
    t.disc(15.5, 16.5, 2.2, STEEL, base=3)
    # сетка поверх
    for y in range(7, 27):
        for x in range(6, 26):
            if math.hypot(x + 0.5 - 15.5, y + 0.5 - 16.5) <= 9.6 and ((x + y) % 3 == 0 or (x - y) % 3 == 0):
                t.blend(x, y, STEEL[2], 0.55)
    if on:
        t.glow(15.5, 16.5, 10, BLUE[4], 0.3)
    status(t, on)
    return t


@tex("block/biomass_oxidizer_front", "block/biomass_oxidizer_front_on")
def biomass_oxidizer_front(name):
    """Окислитель биомассы: круглая топочная дверца со смотровым окном и ручкой; включён — огонь."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=58)
    t.disc(15.5, 16, 12, GUNMETAL, base=2)
    t.ring(15.5, 16, 10, 12, DARK_STEEL, base=2)
    for a in range(8):
        x = 15.5 + 11 * math.cos(a * math.pi / 4 + math.pi / 8)
        y = 16 + 11 * math.sin(a * math.pi / 4 + math.pi / 8)
        t.put(int(x), int(y), STEEL[4])
    t.disc(15.5, 16, 7, BLACK, base=1, lit=False)
    if on:
        t.disc(15.5, 16, 6.5, FIRE, base=2, lit=False)
        for y in range(11, 22):
            for x in range(10, 22):
                d = math.hypot(x + 0.5 - 15.5, y + 0.5 - 17)
                if d < 6 and (y > 16 or ((x * 3 + y) % 5 == 0)):
                    t.put(x, y, FIRE[3] if d < 3.5 else FIRE[2])
        t.glow(15.5, 17, 5, FIRE[4], 0.5)
    else:
        for y in range(18, 22):
            for x in range(11, 21):
                if math.hypot(x + 0.5 - 15.5, y + 0.5 - 16) < 6.5:
                    t.put(x, y, OIL[1] if (x + y) % 3 else OIL[2])
    t.hline(9, 22, 9, STEEL[3])
    t.rect(13, 27, 18, 28, STEEL[3])
    t.hline(13, 18, 28, STEEL[1])
    status(t, on)
    return t


@tex("block/biomass_oxidizer_side")
def biomass_oxidizer_side(name):
    """Бок окислителя: утопленная панель с жалюзи обдува сверху и снизу."""
    t = Tex()
    shell(t, seed=59)
    t.recess(5, 5, 26, 26, STEEL, base=1)
    t.grille(7, 7, 24, 13, DARK_STEEL, pitch=2)
    t.grille(7, 18, 24, 24, DARK_STEEL, pitch=2)
    return t


@tex("block/biomass_oxidizer_top")
def biomass_oxidizer_top(name):
    """Верх окислителя: дымоход с кольцом-фланцем."""
    t = Tex()
    shell(t, seed=60)
    t.disc(15.5, 15.5, 9, STEEL, base=3)
    t.ring(15.5, 15.5, 7, 9, STEEL, base=2)
    t.disc(15.5, 15.5, 6, BLACK, base=1, lit=False)
    t.disc(15.5, 15.5, 3, BLACK, base=0, lit=False)
    return t


@tex("block/chem_machine_side")
def chem_machine_side(name):
    """Общий бок химмашин: утопленная панель, технологический трубопровод поперёк, фланцы."""
    t = Tex()
    shell(t, seed=61)
    t.recess(5, 5, 26, 26, STEEL, base=1)
    t.ao(6, 6, 25, 25, 0.2)
    t.pipe_h(3, 28, 11, 2, STEEL)
    t.pipe_h(3, 28, 21, 2, COPPER)
    for x in (9, 22):
        t.rect(x, 8, x + 1, 14, STEEL[4])
        t.vline(x + 1, 8, 14, STEEL[1])
        t.rect(x, 18, x + 1, 24, COPPER[4])
        t.vline(x + 1, 18, 24, COPPER[1])
    return t


@tex("block/chem_machine_top")
def chem_machine_top(name):
    """Общий верх химмашин: люк-крышка на болтах."""
    t = Tex()
    shell(t, seed=62)
    t.disc(15.5, 15.5, 10, STEEL, base=2)
    t.ring(15.5, 15.5, 8.5, 10, STEEL, base=3)
    for a in range(8):
        t.screw(int(15.5 + 8.9 * math.cos(a * math.pi / 4)) - 1, int(15.5 + 8.9 * math.sin(a * math.pi / 4)) - 1)
    t.disc(15.5, 15.5, 5, GUNMETAL, base=2)
    t.rect(11, 15, 20, 16, STEEL[4])
    t.hline(11, 20, 16, STEEL[1])
    return t


@tex("block/chemical_reactor_front", "block/chemical_reactor_front_on")
def chemical_reactor_front(name):
    """Химреактор: смотровое окно с зелёным раствором и мешалкой; включён — раствор светится и кипит."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, WHITE_PAINT, seed=63)
    pal = REACT_GREEN
    window(t, 7, 6, 24, 25, pal, base=3 if on else 1, seed=64)
    t.hline(7, 24, 9, shade(pal[4 if on else 2], 0.1))
    # вал мешалки и лопасти
    t.vline(15, 6, 21, STEEL[3])
    t.vline(16, 6, 21, STEEL[1])
    t.rect(11, 21, 20, 22, STEEL[2])
    t.hline(11, 20, 21, STEEL[4])
    if on:
        bubbles(t, 7, 10, 24, 25, pal, seed=3, n=12)
        t.glow(15.5, 17, 9, pal[4], 0.22)
    status(t, on)
    return t


@tex("block/chemical_reactor_side")
def chemical_reactor_side(name):
    """Бок реактора: белый кожух с рубашкой охлаждения — патрубки сверху и снизу."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=65)
    t.recess(5, 5, 26, 26, WHITE_PAINT, base=1)
    cyl_shade(t, 6, 25, 6, 25, 0.15)
    for y in (9, 22):
        t.hline(6, 25, y, WHITE_PAINT[0])
        t.hline(6, 25, y + 1, WHITE_PAINT[4])
    for x in (9.5, 22.5):
        t.disc(x, 16, 3, STEEL, base=2)
        t.disc(x, 16, 1.4, BLACK, base=1, lit=False)
    return t


@tex("block/chemical_reactor_top")
def chemical_reactor_top(name):
    """Верх реактора: привод мешалки по центру и загрузочные штуцеры по краям."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=66)
    t.disc(15.5, 15.5, 9.5, WHITE_PAINT, base=3)
    t.disc(15.5, 15.5, 6, DARK_STEEL, base=2)
    t.disc(15.5, 15.5, 2.5, STEEL, base=3)
    for x, y in ((7, 7), (24, 7), (7, 24), (24, 24)):
        t.disc(x, y, 2.3, STEEL, base=2)
        t.put(int(x), int(y), BLACK[1])
    return t


@tex("block/co2_scrubber_front", "block/co2_scrubber_front_on")
def co2_scrubber_front(name):
    """Поглотитель CO₂: перфорированная панель картриджа; включён — картридж светится (реакция) + индикатор."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=67)
    t.recess(5, 6, 26, 27, DARK_STEEL, base=1)
    t.fill(WHITE_PAINT if on else CERAMIC, 1, 6, 7, 25, 26, grain=0.2, seed=68)
    for y in range(8, 26, 3):
        for x in range(8, 25, 3):
            t.rect(x, y, x + 1, y + 1, BLACK[1])
            t.put(x + 1, y + 1, (WHITE_PAINT if on else CERAMIC)[4])
    if on:
        t.glow(15.5, 16.5, 10, CYAN[4], 0.18)
    status(t, on)
    return t


@tex("block/co2_scrubber_side")
def co2_scrubber_side(name):
    """Бок поглотителя: кассета картриджа на защёлках."""
    t = Tex()
    shell(t, seed=69)
    t.panel(6, 5, 25, 26, STEEL, base=3, grain=0.15, seed=70)
    t.ao(6, 5, 25, 26, 0.15)
    for y in (10, 21):
        t.rect(13, y - 1, 18, y + 1, DARK_STEEL[2])
        t.hline(13, 18, y - 1, DARK_STEEL[4])
    return t


@tex("block/co2_scrubber_top")
def co2_scrubber_top(name):
    """Верх поглотителя: вытяжной вентилятор под решёткой."""
    t = Tex()
    shell(t, seed=71)
    t.disc(15.5, 15.5, 11, DARK_STEEL, base=1, lit=False)
    for a in range(5):
        ang = a * 2 * math.pi / 5
        for r in range(3, 10):
            for w in (0, 0.18):
                x = 15.5 + r * math.cos(ang + w + r * 0.07)
                y = 15.5 + r * math.sin(ang + w + r * 0.07)
                t.put(int(x), int(y), GUNMETAL[3] if w == 0 else GUNMETAL[2])
    t.disc(15.5, 15.5, 3, STEEL, base=3)
    t.hline(4, 27, 15, STEEL[3])
    t.vline(15, 4, 27, STEEL[3])
    t.hline(4, 27, 16, STEEL[1])
    t.vline(16, 4, 27, STEEL[1])
    return t


@tex("block/crusher_front", "block/crusher_front_on")
def crusher_front(name):
    """Дробилка: пасть с двумя рядами зубьев валков; включён — искры и пыль в зеве."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=72)
    t.hazard(4, 25, 27, 27)
    t.recess(5, 8, 26, 23, BLACK, base=0)
    for x in range(6, 26, 4):
        for k in range(3):
            t.hline(x + k, x + 3 - k, 9 + k, STEEL[4 - k])
            t.hline(x + k, x + 3 - k, 22 - k, STEEL[2 - k // 2])
    t.pipe_h(6, 25, 10, 0, DARK_STEEL)
    if on:
        for x, y in ((10, 15), (14, 17), (19, 14), (22, 17), (12, 13), (17, 16)):
            t.put(x, y, AMBER[4])
        t.glow(15.5, 15.5, 7, AMBER[3], 0.25)
        for x in range(7, 25, 2):
            t.put(x, 19 + (x % 3) // 2, CERAMIC[1])
    status(t, on)
    return t


@tex("block/crystal_puller_end")
def crystal_puller_end(name):
    """Торец установки Чохральского: крышка камеры и смотровой иллюминатор с раскалённым тиглем."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.12, seed=73)
    t.ring(15.5, 15.5, 11, 13, STEEL, base=2)
    t.disc(15.5, 15.5, 10.5, WHITE_PAINT, base=3)
    t.disc(15.5, 15.5, 6.5, STEEL, base=2)
    t.disc(15.5, 15.5, 5, BLACK, base=1, lit=False)
    t.disc(15.5, 15.5, 3.2, HOT, base=3, lit=False)
    t.glow(15.5, 15.5, 5, HOT[4], 0.5)
    for a in range(6):
        t.rivet(int(15.5 + 12 * math.cos(a * math.pi / 3)) - 1, int(15.5 + 12 * math.sin(a * math.pi / 3)) - 1)
    return t


@tex("block/crystal_puller_side")
def crystal_puller_side(name):
    """Бок установки: вертикальная щель-окно, в ней — вытягиваемая серая буля кремния на затравке."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.12, seed=74)
    cyl_shade(t, 0, 31, 0, 31, 0.18)
    for y in (3, 28):
        t.hline(0, 31, y, WHITE_PAINT[0])
        t.hline(0, 31, y + 1, WHITE_PAINT[4])
    t.recess(10, 5, 21, 26, BLACK, base=1)
    t.vline(15, 6, 12, STEEL[3])
    for y in range(12, 25):
        w = min(4, (y - 12) // 2 + 1) if y < 22 else 4 - (y - 22)
        for x in range(16 - w, 16 + w):
            u = (x - (16 - w)) / max(1, 2 * w - 1)
            t.put(x, y, TITANIUM[4] if u < 0.25 else TITANIUM[2] if u < 0.7 else TITANIUM[1])
    t.hline(11, 20, 25, HOT[3])
    t.glow(15.5, 25, 5, HOT[3], 0.4)
    return t


@tex("block/deposition_reactor_front", "block/deposition_reactor_front_on")
def deposition_reactor_front(name):
    """Реактор Сименса: колпак с U-образным стержнем; включён — стержень раскалён."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=75)
    t.recess(5, 4, 26, 28, BLACK, base=1)
    # стеклянный колпак — дуга сверху
    for x in range(6, 26):
        dx = (x + 0.5 - 15.5) / 10
        y = int(6 + 4 * dx * dx)
        t.put(x, y, GLASS[4])
    pal = HOT if on else STEEL
    for x in (11, 20):
        t.rect(x, 11, x + 1, 25, pal[3] if on else pal[2])
        t.vline(x, 11, 25, pal[4] if on else pal[3])
    t.rect(11, 9, 21, 10, pal[3] if on else pal[2])
    t.hline(11, 21, 9, pal[4] if on else pal[3])
    t.rect(8, 26, 23, 27, STEEL[2])
    t.hline(8, 23, 26, STEEL[4])
    if on:
        t.glow(15.5, 17, 11, HOT[3], 0.28)
    status(t, on)
    return t


@tex("block/deposition_reactor_side")
def deposition_reactor_side(name):
    """Бок реактора: плита с вертикальными рёбрами охлаждения."""
    t = Tex()
    shell(t, seed=76)
    t.recess(5, 5, 26, 26, STEEL, base=1)
    for x in range(7, 26, 4):
        t.rect(x, 6, x + 1, 25, STEEL[3])
        t.vline(x, 6, 25, STEEL[4])
        t.vline(x + 2, 6, 25, STEEL[0])
    return t


@tex("block/deposition_reactor_top")
def deposition_reactor_top(name):
    """Верх реактора: белая крышка колпака, газовый ввод трихлорсилана по центру."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.2, seed=77)
    t.disc(15.5, 15.5, 12, WHITE_PAINT, base=2)
    t.ring(15.5, 15.5, 10.5, 12, WHITE_PAINT, base=3)
    t.disc(15.5, 15.5, 5, STEEL, base=2)
    t.disc(15.5, 15.5, 2.5, BLACK, base=1, lit=False)
    t.rivets(inset=2)
    return t


@tex("block/diffusion_furnace_front", "block/diffusion_furnace_front_on")
def diffusion_furnace_front(name):
    """Диффузионная печь: круглый загрузочный фланец с тремя кварцевыми трубами; включён — трубы светятся."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=78)
    t.disc(15.5, 16, 12, GUNMETAL, base=2)
    t.ring(15.5, 16, 10, 12, STEEL, base=2)
    t.disc(15.5, 16, 9, BLACK, base=1, lit=False)
    for x in (10, 15, 20):
        pal = HOT if on else CERAMIC
        t.disc(x + 0.5, 16, 2.4, pal, base=3 if on else 2)
        t.put(x, 16, (HOT[4] if on else BLACK[2]))
    if on:
        t.glow(15.5, 16, 9, HOT[3], 0.35)
    status(t, on)
    return t


@tex("block/diffusion_furnace_side")
def diffusion_furnace_side(name):
    """Бок печи: теплоизолированный кожух, три зоны нагрева — оранжевые метки зон."""
    t = Tex()
    shell(t, seed=79)
    t.recess(5, 5, 26, 26, WHITE_PAINT, base=2)
    for y in (10, 16, 22):
        t.hline(6, 25, y, WHITE_PAINT[0])
    for y in (7, 13, 19):
        t.rect(14, y, 17, y + 1, AMBER[3])
        t.hline(14, 17, y, AMBER[4])
    return t


@tex("block/diffusion_furnace_top")
def diffusion_furnace_top(name):
    """Верх печи: гладкий белый кожух с вытяжной решёткой по центру."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=80)
    t.grille(8, 11, 23, 20, STEEL, pitch=3)
    return t


# ------------------------------------------------------------------ ректификация
@tex("block/distillation_column")
def distillation_column(name):
    """Колонна ректификации (бок собранной тарелки): белая обечайка 12/16 по центру со смотровым стеклом;
    строки 0–3 и 28–31 — кромки фланцев тарелки (модель берёт их на торцы фланцев)."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.12, seed=81)
    cyl_shade(t, 4, 27, 0, 31, 0.2)
    # кромки фланцев
    for y0 in (0, 28):
        t.panel(0, y0, 31, y0 + 3, STEEL, base=2, grain=0.1)
        for x in range(3, 30, 6):
            t.put(x, y0 + 1, STEEL[4])
            t.put(x, y0 + 2, STEEL[1])
    # смотровое стекло с уровнем флегмы
    t.recess(12, 8, 19, 23, DARK_STEEL, base=1)
    t.rect(13, 9, 18, 15, GLASS[2])
    t.rect(13, 16, 18, 22, AMBER[2])
    t.hline(13, 18, 16, AMBER[4])
    for x in (10, 21):
        t.screw(x - 1, 8)
        t.screw(x - 1, 21)
    return t


@tex("block/distillation_tray")
def distillation_tray(name):
    """Ситчатая тарелка (вид сверху): перфорированный лист, сливной карман, болты фланца по кругу."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.2, seed=82)
    t.panel(3, 3, 28, 28, STEEL, base=3, grain=0.1, seed=83, raised=False)
    for y in range(6, 26, 3):
        for x in range(6 + (y // 3) % 2, 26, 3):
            t.put(x, y, BLACK[1])
            t.put(x + 1, y + 1, STEEL[4])
    # сливной карман
    t.recess(5, 22, 26, 26, DARK_STEEL, base=1)
    t.hline(5, 26, 21, STEEL[4])
    for x, y in ((1, 1), (15, 0), (29, 1), (1, 15), (29, 15), (1, 29), (15, 29), (29, 29)):
        t.rivet(x, y)
    return t


@tex("block/distillation_tray_side")
def distillation_tray_side(name):
    """Бок одиночной тарелки: белая обечайка, фланцевый пояс по центру и шильдик."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.12, seed=84)
    cyl_shade(t, 0, 31, 0, 31, 0.2)
    flange_band(t, 13, 18)
    t.panel(11, 22, 20, 27, AMBER, base=2, grain=0.05)
    t.hline(13, 18, 24, AMBER[0])
    t.hline(13, 16, 25, AMBER[0])
    return t


# ------------------------------------------------------------------ печи и электролиз
@tex("block/electric_furnace_front", "block/electric_furnace_front_on")
def electric_furnace_front(name):
    """Электропечь: дверца со смотровым окном сверху и спиралью нагревателя в камере; включён — накал."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=85)
    t.panel(5, 5, 26, 27, GUNMETAL, base=2)
    t.recess(8, 8, 23, 13, GLASS, base=1)
    t.hline(9, 14, 9, GLASS[4])
    # камера и спираль
    t.recess(8, 16, 23, 24, BLACK, base=0)
    pal = FIRE if on else STEEL
    for x in range(9, 23):
        y = 20 + (1 if (x // 2) % 2 else -1)
        t.put(x, y, pal[3])
        t.put(x, y + 1, pal[2] if not on else pal[1])
    if on:
        t.glow(15.5, 20.5, 8, FIRE[3], 0.45)
        for x in range(9, 23):
            t.blend(x, 12, FIRE[3], 0.3)
    t.rect(12, 14, 19, 14, STEEL[4])
    status(t, on)
    return t


def cell_body(t, pal, base, seed):
    """Корпус электролизёра: белая рама, латунные шины сверху и снизу, окно ванны."""
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.1, seed=seed)
    for y0 in (1, 27):
        t.panel(1, y0, 30, y0 + 3, BRASS, base=2, grain=0.05)
    t.recess(3, 6, 28, 25, DARK_STEEL, base=1)
    t.fill(pal, base, 4, 7, 27, 24, grain=0.15, seed=seed + 1)
    t.hline(4, 27, 9, pal[min(4, base + 1)])
    t.hline(4, 27, 7, GLASS[2])
    t.hline(4, 27, 8, GLASS[3])


@tex("block/electrolysis_cell")
def electrolysis_cell(name):
    """Ячейка электролиза (одиночная): ванна с водой, электроды — тёмные пластины."""
    t = Tex()
    cell_body(t, WATER, 2, 86)
    for x in (9, 21):
        t.rect(x, 7, x + 1, 22, GUNMETAL[2])
        t.vline(x, 7, 22, GUNMETAL[4])
    return t


@tex("block/electrolysis_cell_formed")
def electrolysis_cell_formed(name):
    """Собранная ячейка: вода светлее, у электродов — струйки пузырей газа."""
    t = Tex()
    cell_body(t, WATER, 3, 87)
    for x in (9, 21):
        t.rect(x, 7, x + 1, 22, GUNMETAL[2])
        t.vline(x, 7, 22, GUNMETAL[4])
        for y in range(10, 23, 3):
            t.put(x - 2 + (y // 3) % 2, y, WATER[4])
            t.put(x + 3 - (y // 3) % 2, y + 1, WATER[4])
    return t


@tex("block/electrolysis_cell_formed_top")
def electrolysis_cell_formed_top(name):
    """Верх собранной ячейки: медная шина с выводами электродов и газоотводы."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.1, seed=88)
    t.panel(2, 12, 29, 19, COPPER, base=2, grain=0.1)
    for x in (7, 15, 23):
        t.disc(x + 0.5, 15.5, 2.3, BRASS, base=3)
    for x in (7, 23):
        t.disc(x + 0.5, 6, 2.5, STEEL, base=2)
        t.put(x, 6, BLACK[1])
        t.disc(x + 0.5, 25.5, 2.5, STEEL, base=2)
        t.put(x, 25, BLACK[1])
    return t


@tex("block/electrolyzer_front", "block/electrolyzer_front_on")
def electrolyzer_front(name):
    """Электролизёр: окно ванны; выключен — тёмная вода, включён — голубая с поднимающимися пузырями."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=89)
    window(t, 7, 6, 24, 26, WATER, base=2 if on else 0, seed=90)
    for x in (10, 21):
        t.rect(x, 9, x + 1, 26, GUNMETAL[2])
        t.vline(x, 9, 26, GUNMETAL[3])
    t.hline(7, 24, 8, WATER[3] if on else WATER[1])
    bubbles(t, 7, 9, 24, 26, WATER if on else GLASS, seed=7, n=12 if on else 5)
    status(t, on)
    return t


@tex("block/etch_bath_front", "block/etch_bath_front_on")
def etch_bath_front(name):
    """Ванна травления: окно с бледно-зелёной кислотой и кассетой пластин; включён — пузыри."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, WHITE_PAINT, seed=91)
    window(t, 6, 8, 25, 25, ACID, base=2 if on else 1, seed=92)
    t.hline(6, 25, 10, ACID[4] if on else ACID[3])
    for x in range(10, 23, 3):
        t.vline(x, 12, 23, TITANIUM[3])
    t.hline(9, 23, 23, STEEL[2])
    if on:
        bubbles(t, 6, 11, 25, 25, ACID, seed=9, n=10)
    status(t, on)
    return t


@tex("block/etch_bath_side")
def etch_bath_side(name):
    """Бок ванны: белый корпус из полипропилена, сливной штуцер снизу по центру."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=93)
    t.recess(5, 5, 26, 26, WHITE_PAINT, base=2)
    t.hline(6, 25, 10, ACID[2])
    t.disc(15.5, 22, 3, STEEL, base=2)
    t.disc(15.5, 22, 1.3, BLACK, base=1, lit=False)
    return t


@tex("block/etch_bath_top")
def etch_bath_top(name):
    """Верх ванны: открытое зеркало кислоты в белом бортике, кассета с пластинами."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.1, seed=94)
    t.recess(4, 4, 27, 27, WHITE_PAINT, base=1)
    t.fill(ACID, 2, 5, 5, 26, 26, grain=0.2, seed=95)
    for y in range(9, 23, 3):
        t.hline(9, 22, y, TITANIUM[3])
        t.hline(9, 22, y + 1, ACID[1])
    for x, y in ((6, 6), (24, 8), (7, 24)):
        t.put(x, y, ACID[4])
    return t


@tex("block/fan_filter_unit")
def fan_filter_unit(name):
    """Фильтровентиляционный модуль: HEPA-фильтр гофрами в белой рамке, центральный лючок."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.08, seed=96)
    t.recess(3, 3, 28, 28, WHITE_PAINT, base=1)
    for x in range(4, 28):
        c = WHITE_PAINT[3] if x % 3 == 0 else WHITE_PAINT[1] if x % 3 == 2 else WHITE_PAINT[2]
        t.vline(x, 4, 27, c)
    for y in (10, 21):
        t.hline(4, 27, y, STEEL[2])
        t.hline(4, 27, y + 1, STEEL[4])
    return t


# ------------------------------------------------------------------ литография
@tex("block/lithography_station_front", "block/lithography_station_front_on")
def lithography_station_front(name):
    """Литография: окно жёлтой зоны (фоторезист безопасен при жёлтом свете) над столом пластины;
    включён — экспонирование УФ, фиолетовое свечение над пластиной."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, WHITE_PAINT, seed=97)
    window(t, 6, 6, 25, 15, PHOTO_YELLOW, base=2, seed=98)
    # стол и пластина
    t.panel(6, 22, 25, 26, GUNMETAL, base=2)
    t.disc(15.5, 20.5, 4.5, TITANIUM if not on else UV, base=3)
    t.rect(10, 17, 21, 18, UV[3] if on else DARK_STEEL[2])
    if on:
        t.glow(15.5, 19, 7, UV[4], 0.4)
    status(t, on)
    return t


@tex("block/lithography_station_side")
def lithography_station_side(name):
    """Бок литографа: белый кожух с термостабилизирующим воздуховодом."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=99)
    t.grille(7, 7, 24, 12, STEEL, pitch=2)
    t.recess(7, 16, 24, 25, WHITE_PAINT, base=2)
    t.hline(8, 23, 20, WHITE_PAINT[0])
    return t


@tex("block/lithography_station_top")
def lithography_station_top(name):
    """Верх литографа: корпус осветителя с жёлтым защитным стеклом."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=100)
    t.recess(7, 7, 24, 24, DARK_STEEL, base=1)
    t.fill(PHOTO_YELLOW, 3, 8, 8, 23, 23, grain=0.15, seed=101)
    t.bevel(8, 8, 23, 23, PHOTO_YELLOW, raised=True, base=3)
    t.disc(15.5, 15.5, 3.5, PHOTO_YELLOW, base=2)
    return t


# ------------------------------------------------------------------ масс-драйвер
@tex("block/mass_catcher_side")
def mass_catcher_side(name):
    """Бок ловушки: белый корпус, красная сигнальная полоса по центру, амортизаторы."""
    t = Tex()
    shell(t, WHITE_PAINT, seed=102)
    t.panel(2, 21, 29, 24, RED, base=2, grain=0.05)
    for x in (8, 23):
        t.pipe_v(x, 5, 18, 2, STEEL)
        t.rect(x - 3, 17, x + 3, 19, GUNMETAL[2])
    return t


@tex("block/mass_catcher_top")
def mass_catcher_top(name):
    """Верх/низ ловушки: мишень из красно-белых колец (точка приёма)."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.08, seed=103)
    for r, pal in ((13, RED), (10.5, WHITE_PAINT), (8, RED), (5.5, WHITE_PAINT), (3, RED)):
        t.disc(15.5, 15.5, r, pal, base=2)
    t.rivets(inset=1)
    return t


@tex("block/mass_driver_breech_front")
def mass_driver_breech_front(name):
    """Казённик масс-драйвера (вид по стволу): тёмное жерло в кольце катушки со сверхпроводником."""
    t = Tex()
    shell(t, GUNMETAL, seed=104)
    t.disc(15.5, 15.5, 12, DARK_STEEL, base=2)
    t.ring(15.5, 15.5, 8, 11, SC_BLUE, base=2)
    for a in range(16):
        ang = a * math.pi / 8
        t.put(int(15.5 + 9.5 * math.cos(ang)), int(15.5 + 9.5 * math.sin(ang)), SC_BLUE[4])
    t.disc(15.5, 15.5, 7, BLACK, base=0, lit=False)
    t.disc(15.5, 15.5, 3, BLACK, base=0, lit=False)
    t.glow(15.5, 15.5, 12, SC_BLUE[3], 0.15)
    return t


@tex("block/mass_driver_breech_side")
def mass_driver_breech_side(name):
    """Бок казённика: тёмный корпус, пара медных направляющих шин вдоль ствола."""
    t = Tex()
    shell(t, GUNMETAL, seed=105)
    t.recess(4, 5, 27, 26, DARK_STEEL, base=1)
    for y in (10, 21):
        t.pipe_h(3, 28, y, 1, COPPER)
    for x in range(6, 27, 5):
        t.rect(x, 14, x + 2, 17, DARK_STEEL[3])
        t.hline(x, x + 2, 14, DARK_STEEL[4])
    return t


@tex("block/mass_driver_breech_top")
def mass_driver_breech_top(name):
    """Верх казённика: загрузочный лоток-щель по оси ствола."""
    t = Tex()
    shell(t, GUNMETAL, seed=106)
    t.recess(5, 12, 26, 19, BLACK, base=1)
    t.hline(6, 25, 15, STEEL[2])
    t.hline(6, 25, 16, STEEL[3])
    for x in (5, 26):
        t.rect(x - 1, 10, x, 21, STEEL[3])
    return t


@tex("block/mass_driver_sled")
def mass_driver_sled(name):
    """Салазки (элемент 14×4×12): строки 4–27 — верх с магнитными башмаками, строки 24–31 — борт
    с оранжевой полосой (модель берёт борт с низа текстуры)."""
    t = Tex()
    t.fill(ALUMINIUM, 2, grain=0.12, seed=107)
    # верх
    t.panel(2, 4, 29, 23, ALUMINIUM, base=3, grain=0.1, seed=108)
    for x in (6, 21):
        t.panel(x, 7, x + 4, 20, SC_BLUE, base=2, grain=0.05)
    t.disc(15.5, 13.5, 3, STEEL, base=2)
    # борт
    t.panel(0, 24, 31, 31, ALUMINIUM, base=2, grain=0.1, seed=109)
    t.hline(0, 31, 27, AMBER[3])
    t.hline(0, 31, 28, AMBER[2])
    t.hline(0, 31, 0, AMBER[3])
    t.hline(0, 31, 1, AMBER[2])
    return t


# ------------------------------------------------------------------ нефтепереработка и Сабатье
@tex("block/refinery_front", "block/refinery_front_on")
def refinery_front(name):
    """Установка перегонки: колонна со смотровыми метками уровня и манометр; включён — нефть в стекле, огни."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=110)
    t.recess(8, 5, 23, 27, BLACK, base=1)
    t.rect(9, 6, 22, 26, OIL[2] if on else DARK_STEEL[1])
    for y in range(8, 26, 4):
        t.hline(9, 22, y, BLACK[0])
        t.led(11, y + 1, AMBER, on)
        t.led(19, y + 1, AMBER, on)
    if on:
        for y in range(6, 27):
            for x in range(13, 18):
                t.put(x, y, OIL[3] if y > 12 else OIL[4])
        t.glow(15.5, 17, 8, AMBER[3], 0.18)
    # манометры по бокам
    for x in (4.5, 26.5):
        t.disc(x, 9, 2.3, WHITE_PAINT, base=3)
        t.put(int(x), 8, RED[3])
    status(t, on)
    return t


@tex("block/sabatier_reactor_front", "block/sabatier_reactor_front_on")
def sabatier_reactor_front(name):
    """Реактор Сабатье: пучок трубок с никелевым катализатором; включён — трубки нагреты (~300 °C)."""
    on = name.endswith("_on")
    t = Tex()
    shell(t, seed=111)
    t.recess(5, 5, 26, 27, BLACK, base=1)
    for i, x in enumerate(range(8, 25, 4)):
        pal = HOT if on else (BLUE if i % 2 == 0 else STEEL)
        t.pipe_v(x, 7, 25, 1, pal)
    t.hline(6, 25, 6, STEEL[3])
    t.hline(6, 25, 26, STEEL[1])
    if on:
        t.glow(15.5, 16, 11, HOT[3], 0.2)
    status(t, on)
    return t


# ------------------------------------------------------------------ огнеупор и регoлит
def bricks(t, pal, seed, hot=False):
    """Кладка огнеупорного кирпича 16×8 со смещением рядов, шов тёмный, кромки кирпичей освещены."""
    t.fill(pal, 2, grain=0.3, seed=seed)
    for row in range(4):
        y0 = row * 8
        t.hline(0, 31, y0, pal[0])
        off = 0 if row % 2 == 0 else 8
        for x in range(off, 32, 16):
            t.vline(x, y0, y0 + 7, pal[0])
            t.vline(x + 1, y0 + 1, y0 + 7, pal[3])
        t.hline(0, 31, y0 + 1, pal[3])
        t.hline(0, 31, y0 + 7, pal[1])
    if hot:
        for row in range(4):
            for x in range(32):
                t.blend(x, row * 8, HOT[3], 0.7)


@tex("block/refractory_lining")
def refractory_lining(name):
    """Огнеупорная футеровка: шамотный кирпич."""
    t = Tex()
    bricks(t, FIRECLAY, 112)
    return t


@tex("block/refractory_lining_formed")
def refractory_lining_formed(name):
    """Футеровка в собранной печи: швы прокалены до свечения."""
    t = Tex()
    bricks(t, FIRECLAY, 112, hot=True)
    return t


@tex("block/regolith_reactor_front", "block/regolith_reactor_front_lit")
def regolith_reactor_front(name):
    """Реактор расплава реголита: каменный портал, стальная заслонка; горит — внутри жёлтый расплав."""
    on = name.endswith("_lit")
    t = Tex()
    bricks(t, LUNAR, 113)
    t.panel(4, 5, 27, 27, STEEL, base=2)
    t.recess(7, 8, 24, 24, BLACK, base=0)
    if on:
        for y in range(9, 24):
            k = 4 if y < 13 else 3 if y < 18 else 2
            t.hline(8, 23, y, HOT[k])
        t.glow(15.5, 16, 10, HOT[4], 0.4)
    else:
        t.rect(8, 20, 23, 23, LUNAR[1])
        t.hline(8, 23, 20, LUNAR[2])
    t.hline(4, 27, 5, STEEL[4])
    return t


@tex("block/regolith_reactor_side")
def regolith_reactor_side(name):
    """Бок реактора: кладка из спечённого лунного камня."""
    t = Tex()
    bricks(t, LUNAR, 114)
    return t


# ------------------------------------------------------------------ катушки масс-драйвера
def coil_side(t, wire, seed):
    """Намотка поперёк оси: витки провода, по краям (торцы оси) — стальные щёки."""
    # ось катушки — вдоль u (east/west — торцы), поэтому витки идут поперёк: вертикальные полосы
    for x in range(3, 29):
        k = 3 if x % 2 == 0 else 1
        for y in range(32):
            kk = k
            if y % 11 == 0:
                kk = 0          # межслойная изоляция
            elif y % 11 == 1:
                kk = min(4, k + 1)
            t.put(x, y, wire[kk])
    for x0 in (0, 29):
        t.panel(x0, 0, x0 + 2, 31, STEEL, base=2, grain=0.1)


def coil_end(t, wire, seed):
    """Торец катушки: кольцо намотки на стальной щеке, в центре — проход для салазок."""
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.2, seed=seed)
    t.ring(15.5, 15.5, 6, 13, wire, base=2)
    for r in range(7, 13, 2):
        for a in range(64):
            ang = a * math.pi / 32
            t.blend(int(15.5 + r * math.cos(ang)), int(15.5 + r * math.sin(ang)), wire[0], 0.35)
    t.disc(15.5, 15.5, 5.5, BLACK, base=1, lit=False)
    t.ring(15.5, 15.5, 5, 6, STEEL, base=3)
    t.rivets(inset=1)


def coil_rail(t, wire, seed):
    """Катушка в направляющей (вид сверху): витки и стальная рельса поперёк по центру."""
    coil_side(t, wire, seed)
    t.rect(0, 14, 31, 17, STEEL[3])
    t.hline(0, 31, 14, STEEL[4])
    t.hline(0, 31, 17, STEEL[1])


@tex("block/steel_coil_side")
def steel_coil_side(name):
    t = Tex()
    coil_side(t, COPPER, 115)
    return t


@tex("block/steel_coil_end")
def steel_coil_end(name):
    t = Tex()
    coil_end(t, COPPER, 116)
    return t


@tex("block/steel_coil_rail")
def steel_coil_rail(name):
    t = Tex()
    coil_rail(t, COPPER, 117)
    return t


@tex("block/superconducting_coil_side")
def superconducting_coil_side(name):
    """Сверхпроводящая катушка: ленточный провод в голубом криостате."""
    t = Tex()
    coil_side(t, SC_BLUE, 118)
    return t


@tex("block/superconducting_coil_end")
def superconducting_coil_end(name):
    t = Tex()
    coil_end(t, SC_BLUE, 119)
    t.glow(15.5, 15.5, 13, SC_BLUE[4], 0.15)
    return t


@tex("block/superconducting_coil_rail")
def superconducting_coil_rail(name):
    t = Tex()
    coil_rail(t, SC_BLUE, 120)
    return t


@tex("block/catcher_net")
def catcher_net(name):
    """Улавливающая сеть: стальной трос ячейкой 8 px, узлы-зажимы, за сетью — тёмный проём."""
    t = Tex()
    t.fill(DARK_STEEL, 0, grain=0.2, seed=121)
    for p in range(3, 32, 8):
        t.hline(0, 31, p, STEEL[3])
        t.hline(0, 31, p + 1, STEEL[1])
        t.vline(p, 0, 31, STEEL[3])
        t.vline(p + 1, 0, 31, STEEL[1])
    for x in range(3, 32, 8):
        for y in range(3, 32, 8):
            t.rect(x - 1, y - 1, x + 2, y + 2, GUNMETAL[3])
            t.put(x - 1, y - 1, GUNMETAL[4])
            t.put(x + 2, y + 2, GUNMETAL[1])
    return t


# ------------------------------------------------------------------ пила
@tex("block/wafer_saw_end")
def wafer_saw_end(name):
    """Торец пилы: алмазный диск с внутренней кромкой реза (ID-пила), ступица по центру."""
    t = Tex()
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.2, seed=122)
    t.disc(15.5, 15.5, 12.5, ALUMINIUM, base=2)
    t.ring(15.5, 15.5, 11.5, 12.5, ALUMINIUM, base=4)
    t.ring(15.5, 15.5, 5, 6.2, TITANIUM, base=3)
    t.disc(15.5, 15.5, 5, BLACK, base=1, lit=False)
    t.disc(15.5, 15.5, 2.2, STEEL, base=3)
    t.rivets(inset=1)
    return t


@tex("block/wafer_saw_side")
def wafer_saw_side(name):
    """Бок пилы: кожух с горизонтальной щелью, в щели виден диск и подача охлаждающей жидкости."""
    t = Tex()
    shell(t, seed=123)
    t.recess(4, 12, 27, 19, BLACK, base=1)
    t.hline(5, 26, 15, ALUMINIUM[4])
    t.hline(5, 26, 16, ALUMINIUM[2])
    for x in (8, 15, 22):
        t.put(x, 14, CYAN[3])
        t.put(x, 17, CYAN[2])
    t.grille(8, 5, 23, 9, STEEL, pitch=2)
    t.grille(8, 22, 23, 26, STEEL, pitch=2)
    return t
