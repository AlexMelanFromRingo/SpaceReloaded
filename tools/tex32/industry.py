"""Тяжёлая индустрия 008: стойка ECLSS, реактор деления, каскад центрифуг, ВРУ, дуговая печь, антенна DSN.

Многие детали мультиблоков (gen_008.py) берут из текстуры лишь полосы или клочки по мировым координатам
(uv = положение детали в блоке), поэтому «материальные» текстуры (корпус печи, свод, опора антенны, графит,
поршни, ротор) рисуются однородными и бесшовными — без рамок, которые бы резались посередине детали.
Лицевые грани блоков-контроллеров и модулей — полноценные панели с рамой.
"""
import math

from . import tex
from .kit import (ALUMINIUM, AMBER, BLACK, BLUE, CERAMIC, CYAN, DARK_STEEL, GLASS, GOLD_FOIL, GREEN, GUNMETAL,
                  RED, SCREEN, STEEL, WHITE_PAINT, Tex, _h, mix, shade)

# ------------------------------------------------------------------ палитры группы
# кремовая эмаль стоек МКС
CREAM = [(0x9A, 0x96, 0x8A), (0xB8, 0xB4, 0xA8), (0xD0, 0xCC, 0xC0), (0xE2, 0xDF, 0xD5), (0xF4, 0xF2, 0xEA)]
# синяя эмаль холодного блока ВРУ
COLDBOX = [(0x18, 0x2A, 0x4C), (0x22, 0x3A, 0x66), (0x2E, 0x4E, 0x80), (0x42, 0x66, 0x9A), (0x6C, 0x8E, 0xBE)]
# зелёная эмаль компрессора
OLIVE = [(0x2C, 0x3E, 0x26), (0x3E, 0x54, 0x34), (0x52, 0x6C, 0x44), (0x68, 0x86, 0x56), (0x90, 0xAC, 0x7A)]
# тёмно-зелёная эмаль печного трансформатора и пульта ДСП
FURNACE_GREEN = [(0x18, 0x28, 0x1C), (0x24, 0x38, 0x28), (0x32, 0x4C, 0x36), (0x46, 0x62, 0x48), (0x66, 0x84, 0x66)]
# обгоревшая сталь кожуха печи
SCORCHED = [(0x36, 0x2A, 0x22), (0x4C, 0x3C, 0x31), (0x62, 0x4E, 0x41), (0x7A, 0x64, 0x54), (0x98, 0x82, 0x70)]
# графит — матово-чёрный с серебристым отливом
GRAPHITE = [(0x18, 0x19, 0x1B), (0x22, 0x23, 0x26), (0x2E, 0x30, 0x33), (0x3E, 0x40, 0x44), (0x5C, 0x5E, 0x63)]
# тепловая труба (натрий в нержавейке, окалина — соломенный цвет)
STRAW = [(0x5A, 0x50, 0x42), (0x76, 0x6A, 0x58), (0x92, 0x84, 0x6E), (0xAE, 0xA0, 0x88), (0xCC, 0xC0, 0xA8)]
# углепластик ротора центрифуги
CARBON = [(0x10, 0x11, 0x13), (0x1A, 0x1C, 0x1F), (0x26, 0x28, 0x2C), (0x36, 0x39, 0x3E), (0x58, 0x5C, 0x62)]
# акценты модулей ECLSS: OGS — синий, Сабатье — оранжевый, WRS — зелёный, CDRA — серый
ORANGE = [(0x6A, 0x30, 0x0C), (0x9A, 0x48, 0x12), (0xCC, 0x68, 0x1C), (0xEC, 0x8C, 0x34), (0xFF, 0xBE, 0x7A)]


def seamless(t, pal, base=2, grain=0.3, seed=1):
    """Однородный материал без рамки (для деталей, которые берут из текстуры клочок)."""
    t.fill(pal, base, grain=grain, seed=seed)


def cylinder_x(t, pal, cx, r, y0=0, y1=31):
    """Цилиндр вдоль Y шириной 2r вокруг cx (для полос, которые модель берёт у стержней и труб)."""
    for x in range(int(cx - r), int(cx + r)):
        u = (x + 0.5 - cx) / r        # −1 слева … +1 справа
        k = 3 if u < -0.45 else (1 if u > 0.5 else 2)
        if u < -0.8:
            k = 2
        if u > 0.85:
            k = 0
        t.vline(x, y0, y1, pal[k])


def std_frame(t, pal, base=2, seed=31, grain=0.2):
    """Рама лицевой панели: выпуклый обод 2 px, внутри — утопленный кант."""
    t.panel(0, 0, 31, 31, pal, base=base, grain=grain, seed=seed)
    t.bevel(2, 2, 29, 29, pal, raised=False, base=base)


# ================================================================== ECLSS
def cream_panel(t, seed=41):
    """Кремовая панель стойки с винтами по углам."""
    std_frame(t, CREAM, base=2, seed=seed, grain=0.12)
    for x, y in ((4, 4), (25, 4), (4, 25), (25, 25)):
        t.screw(x, y, STEEL)


@tex("block/eclss_panel_side", "block/eclss_module_side")
def eclss_side(name):
    """Бок стойки/модуля: кремовая эмаль, винты по углам, неглубокая выштамповка по центру."""
    t = Tex()
    cream_panel(t, seed=43 if "module" in name else 41)
    t.bevel(9, 9, 22, 22, CREAM, raised=True, base=2)
    t.ao(10, 10, 21, 21, k=0.06)
    return t


@tex("block/eclss_module_rim")
def eclss_module_rim(name):
    """Обод выдвижного модуля — берётся полосами 2–3 px по краю: кромка кремовая чуть темнее, бесшовная."""
    t = Tex()
    seamless(t, CREAM, base=1, grain=0.15, seed=45)
    # лёгкая шлифовка вдоль кромки
    for y in range(0, 32, 4):
        t.hline(0, 31, y, mix(CREAM[1], CREAM[2], 0.5))
    return t


@tex("block/eclss_rack_frame")
def eclss_rack_frame(name):
    """Каркас стойки (разобранный): алюминиевый профиль с сеткой облегчающих отверстий."""
    t = Tex()
    std_frame(t, ALUMINIUM, base=2, seed=47)
    for i in range(4):
        for j in range(4):
            x, y = 5 + i * 6, 5 + j * 6
            t.recess(x, y, x + 3, y + 3, DARK_STEEL, base=2)
    return t


@tex("block/eclss_rack_frame_formed")
def eclss_rack_frame_formed(name):
    """Каркас в собранной стойке: две стойки с перфорацией сиденья по бокам, между ними утопленная панель."""
    t = Tex()
    t.fill(ALUMINIUM, 2, grain=0.12, seed=49)
    t.recess(8, 0, 23, 31, CREAM, base=1)
    t.ao(8, 0, 23, 31, k=0.18, width=2)
    for x0 in (0, 24):
        t.panel(x0, 0, x0 + 7, 31, ALUMINIUM, base=2, grain=0.1, seed=50)
        for y in range(2, 31, 5):
            t.rect(x0 + 3, y, x0 + 4, y + 2, DARK_STEEL[1])
            t.put(x0 + 3, y, DARK_STEEL[0])
            t.put(x0 + 4, y + 2, ALUMINIUM[4])
    return t


def module_front(accent, seed):
    """Лицевая панель модуля: кремовая панель, цветная шапка-маркировка, две ручки, винты."""
    t = Tex()
    cream_panel(t, seed=seed)
    t.panel(3, 3, 28, 8, accent, base=2, grain=0.1, seed=seed + 1)
    t.hline(3, 28, 9, CREAM[0])
    # ручки (симметрично) — скобы с тенью
    for x0 in (6, 21):
        t.rect(x0, 13, x0 + 4, 14, STEEL[3])
        t.rect(x0, 15, x0, 24, STEEL[3])
        t.rect(x0 + 4, 15, x0 + 4, 24, STEEL[1])
        t.rect(x0, 25, x0 + 4, 26, STEEL[1])
        t.put(x0, 13, STEEL[4])
        t.hline(x0 + 1, x0 + 3, 15, CREAM[0])
    # по центру — табличка и индикатор
    t.recess(12, 13, 19, 20, BLACK, base=2)
    t.hline(13, 18, 15, CREAM[3])
    t.hline(13, 16, 17, CREAM[2])
    t.led(15, 23, accent)
    return t


@tex("block/ogs_module_front")
def ogs_front(name):
    return module_front(BLUE, 51)


@tex("block/sabatier_module_front")
def sabatier_front(name):
    return module_front(ORANGE, 53)


@tex("block/wrs_module_front")
def wrs_front(name):
    return module_front(GREEN, 55)


@tex("block/cdra_module_front")
def cdra_front(name):
    return module_front(STEEL, 57)


@tex("block/eclss_blank_panel_front")
def eclss_blank(name):
    """Заглушка пустого места в стойке: серая шапка, гладкая панель, ручки-захваты сверху."""
    t = Tex()
    cream_panel(t, seed=59)
    t.panel(3, 3, 28, 7, STEEL, base=2, grain=0.1)
    t.bevel(8, 11, 23, 25, CREAM, raised=True, base=2)
    for x0 in (11, 18):
        t.rect(x0, 9, x0 + 2, 9, STEEL[1])
    return t


def inner_box(t, seed):
    """Внутренность открытого модуля: тёмная ниша с тенью от обода."""
    t.fill(GUNMETAL, 1, grain=0.15, seed=seed)
    t.ao(0, 0, 31, 31, k=0.35, width=3)


@tex("block/ogs_module_inner")
def ogs_inner(name):
    """OGS: электролизная стопка (синие ячейки) на стойках, трубки O₂ и H₂ вниз."""
    t = Tex()
    inner_box(t, 61)
    t.panel(5, 5, 26, 14, BLUE, base=2, grain=0.1, seed=62)
    for x in range(7, 26, 3):
        t.vline(x, 6, 13, BLUE[0])
    t.rect(5, 5, 26, 5, BLUE[4])
    for cx in (10, 21):
        t.pipe_v(cx, 15, 28, 1, ALUMINIUM)
    t.pipe_h(9, 22, 27, 1, ALUMINIUM)
    t.led(15, 18, CYAN)
    return t


@tex("block/sabatier_module_inner")
def sabatier_inner(name):
    """Сабатье: горизонтальные трубки реактора с катализатором, нагретые до оранжевого."""
    t = Tex()
    inner_box(t, 63)
    for y in (7, 13, 19, 25):
        t.pipe_h(5, 26, y, 1, ORANGE)
        t.rect(4, y - 2, 5, y + 2, STEEL[2])
        t.rect(26, y - 2, 27, y + 2, STEEL[1])
    t.glow(16, 16, 12, ORANGE[3], 0.12)
    return t


@tex("block/wrs_module_inner")
def wrs_inner(name):
    """WRS: рама фильтров, зелёная обвязка трубками, сосуд дистиллятора по центру."""
    t = Tex()
    inner_box(t, 65)
    t.pipe_v(6, 4, 27, 1, GREEN)
    t.pipe_v(25, 4, 27, 1, GREEN)
    t.pipe_h(6, 25, 26, 1, GREEN)
    t.disc(15.5, 13.5, 6.5, ALUMINIUM, base=2)
    t.ring(15.5, 13.5, 3, 4, STEEL, base=1)
    t.disc(15.5, 13.5, 2.5, GLASS, base=3)
    return t


@tex("block/cdra_module_inner")
def cdra_inner(name):
    """CDRA: торец цеолитового слоя — круглая решётка в обечайке."""
    t = Tex()
    inner_box(t, 67)
    t.disc(15.5, 15.5, 12.5, STEEL, base=2)
    t.disc(15.5, 15.5, 10.5, DARK_STEEL, base=1, lit=False)
    for y in range(6, 27, 3):
        for x in range(6, 27, 3):
            if (x + 0.5 - 15.5) ** 2 + (y + 0.5 - 15.5) ** 2 < 9.5 ** 2:
                t.put(x, y, DARK_STEEL[4])
                t.put(x + 1, y + 1, BLACK[0])
    t.disc(15.5, 15.5, 2.5, STEEL, base=3)
    return t


def controller(bg, lit, draw_screen, seed, led_pal=GREEN):
    """Общий пульт: рама своей эмали, экран по центру, ряд индикаторов снизу."""
    t = Tex()
    std_frame(t, bg, base=2, seed=seed, grain=0.15)
    t.recess(5, 5, 26, 19, BLACK, base=1)
    t.rect(6, 6, 25, 18, SCREEN[1] if lit else BLACK[1])
    draw_screen(t, lit)
    t.put(6, 6, SCREEN[3] if lit else BLACK[3])
    return t


@tex("block/eclss_controller_front", "block/eclss_controller_front_on")
def eclss_controller(name):
    """Пульт ECLSS: график парциальных давлений O₂/CO₂ на экране, три индикатора и кнопки."""
    lit = name.endswith("_on")

    def scr(t, lit):
        if not lit:
            return
        for x in range(7, 25):
            y = 11 - int(2.4 * _h(x // 3, 1, 5))
            t.put(x, y, SCREEN[4])
            t.put(x, 15 + (x % 6 == 0), AMBER[3])
        t.hline(7, 24, 17, SCREEN[2])
    t = controller(CREAM, lit, scr, 71)
    for x, pal in ((9, GREEN), (15, AMBER), (21, RED)):
        t.led(x, 23, pal, on=lit)
    for x in (7, 13, 19, 24):
        t.put(x, 27, STEEL[3]); t.put(x + 1, 27, STEEL[1])
    for x, y in ((3, 3), (27, 3), (3, 27), (27, 27)):
        t.put(x, y, STEEL[2])
    return t


@tex("block/eclss_fan")
def eclss_fan(name):
    """Лопасти вентилятора / колесо детандера — светлый алюминий, клочки 1–4 px; бесшовно."""
    t = Tex()
    t.brushed(ALUMINIUM, 2, horizontal=False, seed=73)
    for x in range(0, 32, 8):
        t.vline(x, 0, 31, ALUMINIUM[1])
    return t


@tex("block/eclss_piston")
def eclss_piston(name):
    """Шток поршня насоса — полированная сталь, шлифовка поперёк."""
    t = Tex()
    t.brushed(STEEL, 3, horizontal=True, seed=75)
    return t


# ================================================================== реактор
@tex("block/reactor_steel")
def reactor_steel(name):
    """Корпусная сталь реактора и каскада: листы на сварных швах, бесшовно по краям тайла."""
    t = Tex()
    seamless(t, STEEL, base=2, grain=0.15, seed=81)
    t.hline(0, 31, 15, STEEL[1])
    t.hline(0, 31, 16, STEEL[3])
    t.vline(0, 0, 31, STEEL[3])
    t.vline(31, 0, 31, STEEL[1])
    return t


@tex("block/reactor_dark")
def reactor_dark(name):
    """Тёмная сталь приводов и облучателя — матовая, без рисунка."""
    t = Tex()
    seamless(t, DARK_STEEL, base=2, grain=0.12, seed=83)
    return t


@tex("block/reactor_core")
def reactor_core(name):
    """Активная зона (разобранная): корпус с крышкой на шпильках по кругу."""
    t = Tex()
    std_frame(t, DARK_STEEL, base=2, seed=85)
    t.disc(15.5, 15.5, 11.5, STEEL, base=2)
    t.disc(15.5, 15.5, 8.5, DARK_STEEL, base=2)
    for i in range(8):
        a = i * math.pi / 4
        t.rivet(int(15 + 10 * math.cos(a)), int(15 + 10 * math.sin(a)), STEEL)
    t.disc(15.5, 15.5, 3, GUNMETAL, base=3)
    return t


@tex("block/reactor_core_formed")
def reactor_core_formed(name):
    """Собранная зона: урановые (U-Mo) диски-сборки в отражателе, остаточное тёплое свечение."""
    t = Tex()
    t.fill(DARK_STEEL, 1, grain=0.2, seed=87)
    for cx, cy in ((9.5, 9.5), (22.5, 9.5), (16, 16), (9.5, 22.5), (22.5, 22.5)):
        t.disc(cx, cy, 5, GUNMETAL, base=3)
        t.ring(cx, cy, 2.5, 3.5, DARK_STEEL, base=1)
        t.disc(cx, cy, 1.8, AMBER, base=1, lit=False)
        t.glow(cx, cy, 5, AMBER[3], 0.12)
    return t


@tex("block/beo_reflector", "block/beo_reflector_formed")
def beo_reflector(name):
    """Отражатель из окиси бериллия: белая керамика. Разобранный — четыре бруска, собранный — кольцевые сегменты."""
    t = Tex()
    formed = name.endswith("formed")
    t.fill(CERAMIC, 3, grain=0.15, seed=89)
    if not formed:
        for x0, y0 in ((0, 0), (16, 0), (0, 16), (16, 16)):
            t.bevel(x0, y0, x0 + 15, y0 + 15, CERAMIC, raised=True, base=3)
    else:
        # сегменты — горизонтальные пояса с вертикальными стыками вразбежку
        for y in (0, 8, 16, 24):
            t.hline(0, 31, y, CERAMIC[1])
            t.hline(0, 31, y + 1, CERAMIC[4])
            off = 8 if (y // 8) % 2 else 0
            for x in (off, off + 16):
                t.vline(x % 32, y + 1, y + 7, CERAMIC[1])
    return t


@tex("block/control_rod")
def control_rod(name):
    """Стержень B₄C: тёмная оболочка с жёлтыми поясами маркировки. Модель берёт полосу x 6–10 (px 12–19)."""
    t = Tex()
    t.fill(DARK_STEEL, 2, grain=0.15, seed=91)
    cylinder_x(t, DARK_STEEL, 16, 4)
    for y0 in (2, 12, 22):
        for y in range(y0, y0 + 3):
            for x in range(32):
                u = (x + 0.5 - 16) / 4
                k = 4 if u < -0.45 else (1 if u > 0.5 else 3)
                t.put(x, y, AMBER[k])
    # торец (uv 6–10 × 6–10) — круг поверх полосы
    return t


@tex("block/control_rod_drive_front", "block/control_rod_drive_front_on")
def rod_drive(name):
    """Привод стержня: шкала хода (вертикальная), экран, три индикатора. Нижняя полоса — решётка (её берёт мультиблок)."""
    lit = name.endswith("_on")
    t = Tex()
    std_frame(t, STEEL, base=2, seed=93, grain=0.15)
    t.recess(5, 5, 26, 19, BLACK, base=1)
    t.rect(6, 6, 25, 18, SCREEN[1] if lit else BLACK[1])
    # шкала хода стержня по центру экрана
    t.rect(14, 7, 17, 17, BLACK[0])
    if lit:
        t.rect(15, 7, 16, 12, AMBER[3])
        t.put(15, 7, AMBER[4])
        for y in range(8, 18, 2):
            t.put(12, y, SCREEN[3]); t.put(19, y, SCREEN[3])
    else:
        t.rect(15, 7, 16, 12, STEEL[1])
    for x, pal in ((10, GREEN), (15, AMBER), (20, RED)):
        t.led(x, 22, pal, on=lit)
    t.grille(4, 26, 27, 29, DARK_STEEL, pitch=2, vertical=True)
    return t


@tex("block/heat_pipe")
def heat_pipe(name):
    """Тепловая труба в сборке: модель берёт полосу x 6–10 (px 12–19) — цилиндр с поясами-хомутами."""
    t = Tex()
    t.fill(STRAW, 2, grain=0.2, seed=95)
    cylinder_x(t, STRAW, 16, 4)
    for y in (0, 15):
        for x in range(12, 20):
            u = (x + 0.5 - 16) / 4
            k = 4 if u < -0.45 else (1 if u > 0.5 else 3)
            t.put(x, y, STEEL[k]); t.put(x, y + 1, STEEL[max(0, k - 1)])
    return t


@tex("block/heat_pipe_side")
def heat_pipe_side(name):
    """Бок блока тепловых труб: четыре трубы в ряд, между ними — тень."""
    t = Tex()
    t.fill(DARK_STEEL, 1, grain=0.1, seed=97)
    for cx in (4, 12, 20, 28):
        cylinder_x(t, STRAW, cx, 3)
    for y in (0, 15):
        t.hline(0, 31, y, STEEL[3])
        t.hline(0, 31, y + 1, STEEL[1])
    return t


@tex("block/heat_pipe_end")
def heat_pipe_end(name):
    """Торец: четыре заглушённых трубы в стальной решётке."""
    t = Tex()
    std_frame(t, STEEL, base=2, seed=99)
    for cx in (9.5, 22.5):
        for cy in (9.5, 22.5):
            t.disc(cx, cy, 5, STRAW, base=2)
            t.disc(cx, cy, 2, STRAW, base=1, lit=False)
    return t


@tex("block/stirling_body")
def stirling_body(name):
    """Корпус Стирлинга в многослойной золотой изоляции: мятая фольга крупными гранями."""
    t = Tex()
    for y in range(32):
        for x in range(32):
            # грани фольги — ромбы 6×4, освещённость грани постоянная
            h = _h((x + (y // 4) * 3) // 6, y // 4, 101)
            k = 2 if h < 0.5 else (3 if h < 0.85 else 1)
            t.put(x, y, GOLD_FOIL[k])
    # складки: светлая кромка сверху каждого ряда граней, тень снизу
    for y in range(0, 32, 4):
        for x in range(32):
            if _h(x // 5, y, 102) < 0.55:
                t.put(x, y, GOLD_FOIL[4] if _h(x, y, 103) < 0.5 else GOLD_FOIL[3])
                t.put(x, y + 3, GOLD_FOIL[1])
    return t


@tex("block/radiator_panel")
def radiator_panel(name):
    """Радиатор: белое покрытие, вертикальные тепловые трубы, сваренные с ребром."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.12, seed=103)
    for cx in (4, 12, 20, 28):
        cylinder_x(t, WHITE_PAINT, cx, 2)
        t.vline(cx + 2, 0, 31, WHITE_PAINT[1])
    t.hline(0, 31, 0, WHITE_PAINT[4])
    t.hline(0, 31, 31, WHITE_PAINT[0])
    return t


# ================================================================== центрифуги
@tex("block/centrifuge_side")
def centrifuge_side(name):
    """Кожух центрифуги: тонкостенный стальной цилиндр, вертикальная шлифовка, фланцы сверху и снизу."""
    t = Tex()
    t.brushed(ALUMINIUM, 2, horizontal=False, seed=105)
    for y0 in (0, 28):
        t.panel(0, y0, 31, y0 + 3, STEEL, base=2, grain=0.05)
        for x in (3, 11, 19, 27):
            t.rivet(x, y0 + 1, STEEL)
    return t


@tex("block/centrifuge_end")
def centrifuge_end(name):
    """Торец: подшипниковый узел — кольцо фланца на болтах, вал в центре."""
    t = Tex()
    std_frame(t, STEEL, base=2, seed=107)
    t.disc(15.5, 15.5, 11, ALUMINIUM, base=2)
    t.ring(15.5, 15.5, 7, 8, STEEL, base=1)
    for i in range(8):
        a = i * math.pi / 4 + math.pi / 8
        t.rivet(int(15 + 9.5 * math.cos(a)), int(15 + 9.5 * math.sin(a)), STEEL)
    t.disc(15.5, 15.5, 4, DARK_STEEL, base=2)
    t.disc(15.5, 15.5, 1.6, STEEL, base=4)
    return t


@tex("block/centrifuge_rotor")
def centrifuge_rotor(name):
    """Ротор из углепластика: косая саржа 2×2, намоточные пояса."""
    t = Tex()
    for y in range(32):
        for x in range(32):
            k = 2 if ((x + y) // 2) % 2 == 0 else 1
            if ((x - y) // 2) % 4 == 0:
                k += 1
            t.put(x, y, CARBON[k])
    for y in (4, 16, 28):
        t.hline(0, 31, y, CARBON[4])
        t.hline(0, 31, y + 1, CARBON[0])
    return t


@tex("block/cascade_controller_front", "block/cascade_controller_front_on")
def cascade_controller(name):
    """Пульт каскада: мнемосхема ступеней — два ряда по шесть индикаторов (отбор и отвал)."""
    lit = name.endswith("_on")
    t = Tex()
    std_frame(t, STEEL, base=2, seed=109, grain=0.15)
    t.recess(4, 8, 27, 23, BLACK, base=1)
    for i in range(6):
        x = 6 + i * 4
        t.led(x, 11, CYAN, on=lit)
        t.led(x, 18, GREEN, on=lit)
        if lit and i < 5:
            t.hline(x + 2, x + 3, 12, SCREEN[3])
    if lit:
        t.glow(15.5, 15.5, 11, CYAN[3], 0.08)
    t.hline(6, 25, 26, STEEL[1])
    t.hline(6, 25, 27, STEEL[3])
    return t


# ================================================================== ВРУ (asu)
def coldbox_panels(t, seed):
    """Кожух холодного блока: синие листы на стоячем фальце, изморозь снизу."""
    t.fill(COLDBOX, 2, grain=0.2, seed=seed)
    for x in (7, 23):
        t.vline(x, 0, 31, COLDBOX[4])
        t.vline(x + 1, 0, 31, COLDBOX[1])
    t.hline(0, 31, 0, COLDBOX[3])
    t.hline(0, 31, 31, COLDBOX[0])
    # изморозь — мягкий налёт у низа листа (холодная арматура внизу), неровный край
    frost = (0xDC, 0xEA, 0xF6)
    for x in range(32):
        top = 25 + int(3 * _h(x // 3, 0, seed + 1))
        for y in range(top, 31):
            t.blend(x, y, frost, 0.18 + 0.35 * (y - top) / max(1, 31 - top))


@tex("block/asu_coldbox")
def asu_coldbox(name):
    t = Tex()
    coldbox_panels(t, 111)
    return t


@tex("block/asu_sump_front")
def asu_sump_front(name):
    """Куб колонны: смотровое окно уровня жидкого кислорода (бледно-голубой) по центру."""
    t = Tex()
    coldbox_panels(t, 113)
    t.panel(9, 6, 22, 25, STEEL, base=2)
    t.recess(11, 8, 20, 23, BLACK, base=1)
    t.rect(12, 16, 19, 22, (0x7E, 0xB4, 0xDC))
    t.hline(12, 19, 16, (0xC6, 0xE4, 0xF6))
    t.rect(12, 9, 19, 15, GLASS[1])
    t.put(12, 9, GLASS[4])
    for x, y in ((10, 7), (21, 7), (10, 24), (21, 24)):
        t.put(x, y, STEEL[4])
    return t


@tex("block/asu_sump_inner")
def asu_sump_inner(name):
    """Внутри куба: кипящий жидкий кислород в нижней трети, над ним — пар и иней по стенкам."""
    t = Tex()
    t.fill(COLDBOX, 0, grain=0.2, seed=115)
    t.ao(0, 0, 31, 31, k=0.3, width=3)
    lox = [(0x3E, 0x6E, 0x9A), (0x56, 0x8E, 0xBA), (0x74, 0xAC, 0xD6), (0x9E, 0xCC, 0xEC), (0xD4, 0xEC, 0xFA)]
    t.fill(lox, 2, 0, 20, 31, 31, grain=0.3, seed=116)
    t.hline(0, 31, 20, lox[4])
    for x, y in ((6, 24), (13, 27), (20, 23), (26, 28), (9, 29)):
        t.put(x, y, lox[4]); t.put(x + 1, y - 1, lox[3])
    for y in range(3, 19, 5):
        t.hline(3, 28, y, COLDBOX[2])
    for y in range(1, 20):
        for x in (1, 2, 29, 30):
            if _h(x, y, 117) < 0.4:
                t.put(x, y, mix(COLDBOX[4], (0xE8, 0xF2, 0xFA), 0.5))
    return t


@tex("block/asu_compressor_side")
def asu_compressor_side(name):
    """Корпус компрессора: зелёная эмаль, разъём корпуса поясом с болтами посередине, табличка."""
    t = Tex()
    t.fill(OLIVE, 2, grain=0.25, seed=119)
    t.hline(0, 31, 0, OLIVE[3]); t.hline(0, 31, 31, OLIVE[0])
    t.panel(0, 13, 31, 18, OLIVE, base=3, grain=0.05)
    for x in range(2, 31, 5):
        t.rivet(x, 15, STEEL)
    t.panel(11, 4, 20, 9, ALUMINIUM, base=2, grain=0.05)
    t.hline(13, 18, 6, ALUMINIUM[0])
    return t


@tex("block/asu_compressor_end")
def asu_compressor_end(name):
    """Торец компрессора: улитка — фланец вала с болтами на зелёном корпусе."""
    t = Tex()
    std_frame(t, OLIVE, base=2, seed=121)
    t.disc(15.5, 15.5, 11, OLIVE, base=3)
    t.ring(15.5, 15.5, 7.5, 8.5, OLIVE, base=1)
    for i in range(6):
        a = i * math.pi / 3
        t.rivet(int(15 + 9.5 * math.cos(a)), int(15 + 9.5 * math.sin(a)), STEEL)
    t.disc(15.5, 15.5, 5, STEEL, base=2)
    t.disc(15.5, 15.5, 2, DARK_STEEL, base=2)
    return t


@tex("block/asu_exchanger_side")
def asu_exchanger_side(name):
    """Пластинчато-ребристый теплообменник: паяный алюминиевый пакет, горизонтальные рёбра, коллектор."""
    t = Tex()
    for y in range(32):
        k = 3 if y % 3 == 0 else (1 if y % 3 == 2 else 2)
        t.hline(0, 31, y, ALUMINIUM[k])
    t.panel(0, 13, 31, 18, ALUMINIUM, base=2, grain=0.05)
    t.pipe_h(0, 31, 15, 1, ALUMINIUM)
    for x in (0, 31):
        t.vline(x, 0, 31, ALUMINIUM[1 if x else 4])
    return t


@tex("block/asu_exchanger_top")
def asu_exchanger_top(name):
    """Верх пакета: торцы каналов (мелкая сетка) и два коллектора-полутрубы по краям."""
    t = Tex()
    t.fill(ALUMINIUM, 2, grain=0.1, seed=123)
    for y in range(8, 24, 2):
        for x in range(2, 30, 2):
            t.put(x, y, ALUMINIUM[0])
    for y in (4, 27):
        t.pipe_h(1, 30, y, 2, ALUMINIUM)
    return t


@tex("block/asu_tray_side")
def asu_tray_side(name):
    """Обечайка колонны: изоляция в светлом кожухе, фланец тарелки поясом по центру."""
    t = Tex()
    t.brushed(ALUMINIUM, 3, horizontal=False, seed=125)
    t.panel(0, 13, 31, 18, STEEL, base=2, grain=0.05)
    for x in range(1, 31, 5):
        t.rivet(x, 15, STEEL)
    return t


@tex("block/asu_tray_end")
def asu_tray_end(name):
    """Ситчатая тарелка: перфорированный лист в кольце фланца, сливной карман сбоку."""
    t = Tex()
    t.fill(ALUMINIUM, 3, grain=0.1, seed=127)
    t.ring(15.5, 15.5, 12, 14.5, STEEL, base=2)
    for y in range(5, 28, 3):
        for x in range(5, 28, 3):
            if (x + 0.5 - 15.5) ** 2 + (y + 0.5 - 15.5) ** 2 < 10.5 ** 2 and x < 24:
                t.put(x, y, DARK_STEEL[1])
                t.put(x + 1, y, ALUMINIUM[1])
    # сливной карман — сегмент у правого края
    t.rect(24, 9, 26, 22, DARK_STEEL[1])
    t.vline(24, 9, 22, DARK_STEEL[0])
    return t


# ================================================================== дуговая печь (eaf)
@tex("block/eaf_shell")
def eaf_shell(name):
    """Кожух печи: обгоревшая сталь, листы на швах, потёки окалины."""
    t = Tex()
    t.fill(SCORCHED, 2, grain=0.35, seed=131)
    for y in (0, 16):
        t.hline(0, 31, y, SCORCHED[4 if y else 3])
        t.hline(0, 31, y + 15, SCORCHED[0])
    for x in (0, 16):
        t.vline(x, 0, 31, SCORCHED[3])
    for x in range(32):
        if _h(x, 0, 133) < 0.25:
            ln = int(3 + 6 * _h(x, 1, 133))
            for y in range(1, 1 + ln):
                t.darken(x, y, 0.2)
    return t


@tex("block/eaf_vessel")
def eaf_vessel(name):
    """Ванна печи: водоохлаждаемые панели — горизонтальные трубы, бесшовно (модель берёт полосы 3 px)."""
    t = Tex()
    for y0 in range(0, 32, 4):
        t.pipe_h(0, 31, y0 + 1, 1, DARK_STEEL)
        t.hline(0, 31, y0 + 3, BLACK[1])
    for y in range(32):
        for x in range(32):
            if _h(x // 2, y, 135) < 0.06:
                t.darken(x, y, 0.25)
    return t


@tex("block/eaf_roof")
def eaf_roof(name):
    """Свод: водоохлаждаемые трубчатые панели серого цвета с налётом, бесшовно."""
    t = Tex()
    for y0 in range(0, 32, 4):
        t.pipe_h(0, 31, y0 + 1, 1, STEEL)
        t.hline(0, 31, y0 + 3, GUNMETAL[0])
    for y in range(32):
        for x in range(32):
            h = _h(x // 3, y // 2, 137)
            if h < 0.12:
                t.blend(x, y, SCORCHED[2], 0.35)
    return t


@tex("block/eaf_transformer")
def eaf_transformer(name):
    """Печной трансформатор: тёмно-зелёный бак, вертикальные рёбра радиатора, шина вверху."""
    t = Tex()
    t.fill(FURNACE_GREEN, 1, grain=0.2, seed=139)
    for x in range(1, 32, 4):
        t.vline(x, 5, 28, FURNACE_GREEN[4])
        t.vline(x + 1, 5, 28, FURNACE_GREEN[3])
        t.vline(x + 2, 5, 28, FURNACE_GREEN[1])
    t.panel(0, 0, 31, 3, FURNACE_GREEN, base=2, grain=0.05)
    t.panel(0, 29, 31, 31, FURNACE_GREEN, base=2, grain=0.05)
    return t


@tex("block/eaf_controller_front", "block/eaf_controller_front_on")
def eaf_controller(name):
    """Пульт печи: экран с ваттметром и четырьмя фазами-индикаторами, красная полоса «дуга»."""
    lit = name.endswith("_on")
    t = Tex()
    std_frame(t, FURNACE_GREEN, base=2, seed=141, grain=0.2)
    t.recess(5, 5, 26, 17, BLACK, base=1)
    for i in range(4):
        x = 8 + i * 5
        t.led(x, 10, AMBER, on=lit)
    if lit:
        t.glow(15.5, 11, 10, AMBER[3], 0.1)
    t.recess(6, 21, 25, 25, BLACK, base=1)
    t.rect(7, 22, 24, 24, RED[3] if lit else RED[0])
    if lit:
        t.hline(7, 24, 22, RED[4])
        t.glow(15.5, 23, 9, RED[4], 0.15)
    return t


@tex("block/graphite")
def graphite(name):
    """Графитовый электрод: матово-чёрный, продольные риски механической обработки, бесшовно."""
    t = Tex()
    t.brushed(GRAPHITE, 2, horizontal=False, seed=143)
    for y in range(32):
        for x in range(32):
            if _h(x, y // 3, 144) > 0.94:
                t.put(x, y, GRAPHITE[4])
    return t


# ================================================================== DSN
@tex("block/dish_panel")
def dish_panel(name):
    """Панель отражателя: белое покрытие, перфорация по сетке, окантовка — в тайле стыки читаются как швы."""
    t = Tex()
    t.fill(WHITE_PAINT, 3, grain=0.1, seed=151)
    for y in range(3, 30, 4):
        for x in range(3, 30, 4):
            t.put(x, y, WHITE_PAINT[1])
    t.bevel(0, 0, 31, 31, WHITE_PAINT, raised=True, base=3)
    return t


@tex("block/dsn_mount")
def dsn_mount(name):
    """Опора антенны: серая конструкционная сталь, мягкое зерно (детали берут клочки 1–10 px)."""
    t = Tex()
    seamless(t, STEEL, base=2, grain=0.15, seed=153)
    for y in range(0, 32, 8):
        t.hline(0, 31, y, mix(STEEL[2], STEEL[3], 0.5))
    return t


@tex("block/dsn_cabin")
def dsn_cabin(name):
    """Аппаратная антенны: белые сэндвич-панели на горизонтальных стыках, заклёпки."""
    t = Tex()
    t.fill(WHITE_PAINT, 2, grain=0.12, seed=155)
    for y in (0, 16):
        t.hline(0, 31, y, WHITE_PAINT[4])
        t.hline(0, 31, y + 15, WHITE_PAINT[0])
        for x in (3, 15, 27):
            t.put(x, y + 2, STEEL[3]); t.put(x, y + 13, STEEL[3])
    t.vline(0, 0, 31, WHITE_PAINT[3])
    t.vline(31, 0, 31, WHITE_PAINT[1])
    return t


@tex("block/dsn_controller_front", "block/dsn_controller_front_on")
def dsn_controller(name):
    """Пульт DSN: экран с синусоидой несущей и уровнем сигнала, индикатор захвата цели."""
    lit = name.endswith("_on")
    t = Tex()
    std_frame(t, WHITE_PAINT, base=2, seed=157, grain=0.1)
    t.recess(5, 5, 26, 19, BLACK, base=1)
    t.rect(6, 6, 25, 18, SCREEN[1] if lit else BLACK[1])
    if lit:
        for x in range(7, 25):
            y = int(round(11 + 3 * math.sin((x - 7) * 0.7)))
            t.put(x, y, SCREEN[4])
            t.put(x, y + 1, SCREEN[3])
        for i, h in enumerate((1, 2, 3, 4)):
            t.vline(20 + i, 17 - h, 17, CYAN[3])
    t.put(6, 6, SCREEN[3] if lit else BLACK[3])
    t.led(15, 23, GREEN, on=lit)
    for x in (8, 22):
        t.rect(x, 23, x + 1, 24, WHITE_PAINT[1])
    return t
