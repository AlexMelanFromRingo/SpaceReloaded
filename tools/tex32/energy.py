"""Энергетика: батарея (эталон стиля 012), конденсатор, кабели, генераторы, панели, РИТЭГ, ректенна."""
from . import tex
from .kit import (ALUMINIUM, AMBER, BLACK, BLUE, COPPER, CYAN, DARK_STEEL, GLASS, GOLD_FOIL, GREEN, GUNMETAL, RED,
                  RUBBER, STEEL, WHITE_PAINT, Tex, mix, shade)


def battery_frame(t):
    """Корпус шкафа батареи: стальная рама 2 px, углы на заклёпках."""
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.3, seed=21)
    t.bevel(2, 2, 29, 29, STEEL, raised=False, base=2)
    t.rivets(inset=3, pal=STEEL)


@tex("block/battery_0", "block/battery_1", "block/battery_2", "block/battery_3", "block/battery_4")
def battery_side(name):
    """Бок батареи: утопленная тёмная панель, по центру окно шкалы из 4 секций (заряд снизу вверх),
    по бокам — вентиляционные щели; всё симметрично."""
    level = int(name[-1])
    t = Tex()
    battery_frame(t)
    t.recess(6, 5, 25, 26, DARK_STEEL, base=1)
    t.ao(7, 6, 24, 25, k=0.25)
    # окно шкалы
    t.recess(12, 7, 19, 24, BLACK, base=1)
    for k in range(4):
        y1 = 23 - k * 4
        y0 = y1 - 2
        lit = k < level
        pal = GREEN if lit else GLASS
        t.rect(13, y0, 18, y1, pal[3] if lit else pal[2])
        t.hline(13, 18, y0, pal[4] if lit else pal[3])
        t.put(18, y1, pal[2] if lit else pal[1])
    if level > 0:
        t.glow(15.5, 23 - (level - 1) * 4, 5, GREEN[4], 0.18)
    # вентиляция по бокам окна
    for y in range(8, 24, 3):
        for x0 in (8, 21):
            t.hline(x0, x0 + 2, y, DARK_STEEL[0])
            t.hline(x0, x0 + 2, y + 1, DARK_STEEL[3])
    return t


@tex("block/battery_top")
def battery_top(name):
    """Верх: клеммы «+» (красная) и «−» (чёрная) на медных площадках, между ними — решётка газоотвода."""
    t = Tex()
    battery_frame(t)
    t.grille(12, 11, 19, 20, DARK_STEEL, pitch=3)
    for cx, cap in ((7, RED), (24, BLACK)):
        t.panel(cx - 4, 10, cx + 3, 21, COPPER, base=2)
        t.disc(cx, 15.5, 3.2, cap, base=2)
    # знаки полярности (штрихи 1 px)
    t.hline(5, 9, 5, RED[3])
    t.vline(7, 3, 7, RED[3])
    t.hline(22, 26, 5, STEEL[4])
    return t


@tex("block/battery_bottom")
def battery_bottom(name):
    """Дно: гладкая плита на четырёх опорах."""
    t = Tex()
    t.panel(0, 0, 31, 31, DARK_STEEL, base=2, grain=0.08, seed=23)
    for x, y in ((3, 3), (24, 3), (3, 24), (24, 24)):
        t.panel(x, y, x + 4, y + 4, STEEL, base=1)
    return t


# ================================================================== клеммы NiFe-батареи ровера
@tex("block/battery_terminal_plus", "block/battery_terminal_minus")
def battery_terminal(name):
    """Колпачок клеммы: крошечные кубики 2×2 берут разные участки — ровный пластик с мягким зерном и
    светлой верхней кромкой, без мелких деталей."""
    pal = RED if name.endswith("plus") else BLACK
    t = Tex()
    t.fill(pal, 2, grain=0.3, seed=31)
    t.bevel(0, 0, 31, 31, pal, raised=True, base=2)
    return t


# ================================================================== конденсатор (суперконденсаторы)
CAP_CELLS = ((3, 10), (12, 19), (21, 28))   # симметрично относительно центра грани
CAP_WRAP = [(0x12, 0x2A, 0x5A), (0x1A, 0x3E, 0x86), (0x26, 0x56, 0xB0), (0x3C, 0x74, 0xD2), (0x7C, 0xAA, 0xEC)]
CAP_CHARGE = [(0x1C, 0x5E, 0x8E), (0x2C, 0x86, 0xBC), (0x48, 0xAE, 0xE0), (0x78, 0xCE, 0xF4), (0xC0, 0xEC, 0xFF)]


def cylinder_v(t, x0, x1, y0, y1, pal):
    """Вертикальный цилиндр шириной x1−x0+1: блик слева от оси, тень справа."""
    w = x1 - x0
    for x in range(x0, x1 + 1):
        u = (x - x0) / max(1, w)          # 0 слева … 1 справа
        k = 4 if 0.15 <= u < 0.35 else 3 if u < 0.5 else 2 if u < 0.8 else 1
        if x in (x0, x1):
            k = 1 if x == x0 else 0
        t.vline(x, y0, y1, pal[k])


@tex("block/capacitor_side", "block/capacitor_side_0", "block/capacitor_side_1", "block/capacitor_side_2",
     "block/capacitor_side_3", "block/capacitor_side_4")
def capacitor_side(name):
    """Бок батареи конденсаторов: белый корпус, три цилиндра в синей термоусадке; заряженная часть
    каждого — светло-голубая, растёт снизу (уровень 0…4; без суффикса — полный)."""
    level = 4 if name.endswith("side") else int(name[-1])
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.12, seed=33)
    t.recess(2, 2, 29, 29, WHITE_PAINT, base=1)
    y0, y1 = 7, 27
    fill_top = y1 + 1 - round((y1 - y0 + 1) * level / 4)
    for x0, x1 in CAP_CELLS:
        cylinder_v(t, x0, x1, y0, y1, CAP_WRAP)
        if level:
            cylinder_v(t, x0, x1, fill_top, y1, CAP_CHARGE)
            if level < 4:
                t.hline(x0 + 1, x1 - 1, fill_top, CAP_CHARGE[4])
        # верхний торец с медным выводом и закатка
        cylinder_v(t, x0, x1, 4, 6, ALUMINIUM)
        t.hline(x0, x1, 6, ALUMINIUM[0])
        t.rect(x0 + 3, 3, x1 - 3, 4, COPPER[3])
        t.put(x0 + 3, 3, COPPER[4])
        # нижняя закатка
        t.hline(x0, x1, y1 + 1, CAP_WRAP[0])
    if level:
        for x0, x1 in CAP_CELLS:
            t.glow((x0 + x1 + 1) / 2, y1 - 1, 5, CAP_CHARGE[4], 0.12)
    return t


@tex("block/capacitor_top")
def capacitor_top(name):
    """Верх и дно: 3×3 торца ячеек с крестовым клапаном, выводы соединены медными шинами."""
    t = Tex()
    t.panel(0, 0, 31, 31, WHITE_PAINT, base=2, grain=0.12, seed=35)
    t.recess(2, 2, 29, 29, WHITE_PAINT, base=1)
    centers = (6.5, 15.5, 24.5)
    # шины по рядам (под торцами — рисуем раньше)
    for cy in centers:
        t.rect(4, int(cy) - 1, 27, int(cy) + 1, COPPER[2])
        t.hline(4, 27, int(cy) - 1, COPPER[3])
        t.hline(4, 27, int(cy) + 1, COPPER[1])
    for cy in centers:
        for cx in centers:
            t.disc(cx, cy, 3.6, ALUMINIUM, base=2)
            t.put(int(cx), int(cy), ALUMINIUM[0])
            t.put(int(cx) - 1, int(cy), ALUMINIUM[1])
            t.put(int(cx), int(cy) - 1, ALUMINIUM[1])
    return t


# ================================================================== кабель
CABLE_LO, CABLE_HI = 10, 21   # полоса 6/16 блока — сечение кабеля (ядро 5…11)


def cable(name, pal_stripe, lit):
    """Развёртка кабеля: крест из полос (рукава берут верхнюю/левую половины креста), ядро — муфта.
    Оболочка — резина с цилиндрическим светом поперёк полосы, по оси — цветная жила-маркер."""
    t = Tex()
    t.fill(BLACK, 1, grain=0.2, seed=41)      # вне креста не видно, но пусть будет ровно
    def band(horizontal):
        for i in range(CABLE_LO, CABLE_HI + 1):
            u = (i - CABLE_LO) / (CABLE_HI - CABLE_LO)
            k = 4 if u < 0.12 else 3 if u < 0.4 else 2 if u < 0.75 else 1
            if i == CABLE_HI:
                k = 0
            if horizontal:
                t.hline(0, 31, i, RUBBER[k])
            else:
                t.vline(i, 0, 31, RUBBER[k])
        # кольцевые рубчики оболочки
        for j in range(1, 32, 4):
            for i in range(CABLE_LO + 1, CABLE_HI):
                if horizontal:
                    t.darken(j, i, 0.18)
                else:
                    t.darken(i, j, 0.18)
        # жила-маркер по оси
        for i in (15, 16):
            for j in range(32):
                c = pal_stripe[3] if i == 15 else pal_stripe[2]
                t.put(j, i, c) if horizontal else t.put(i, j, c)
    band(True)
    band(False)
    # муфта ядра
    t.panel(CABLE_LO, CABLE_LO, CABLE_HI, CABLE_HI, DARK_STEEL, base=2, grain=0.1, seed=43)
    t.bevel(CABLE_LO + 2, CABLE_LO + 2, CABLE_HI - 2, CABLE_HI - 2, DARK_STEEL, raised=False, base=2)
    t.disc(15.5, 15.5, 2.6, pal_stripe, base=3 if lit else 2)
    for x, y in ((CABLE_LO + 1, CABLE_LO + 1), (CABLE_HI - 2, CABLE_LO + 1), (CABLE_LO + 1, CABLE_HI - 2),
                 (CABLE_HI - 2, CABLE_HI - 2)):
        t.rivet(x, y, STEEL)
    if lit:
        for x in range(32):
            for y in (14, 17):
                if not (CABLE_LO <= x <= CABLE_HI):
                    t.blend(x, y, pal_stripe[4], 0.35)
                    t.blend(y, x, pal_stripe[4], 0.35)
        t.glow(15.5, 15.5, 5, pal_stripe[4], 0.35)
    return t


@tex("block/energy_cable")
def energy_cable(name):
    return cable(name, COPPER, False)


@tex("block/energy_cable_on")
def energy_cable_on(name):
    return cable(name, CYAN, True)


# ================================================================== корпус машин и угольный генератор
def machine_casing(t, seed=51):
    """Общий корпус машин: стальная панель с рамкой, утопленное поле и четыре винта."""
    t.panel(0, 0, 31, 31, STEEL, base=2, grain=0.25, seed=seed)
    t.bevel(2, 2, 29, 29, STEEL, raised=False, base=2)
    for x, y in ((3, 3), (26, 3), (3, 26), (26, 26)):
        t.screw(x, y, STEEL)


@tex("block/machine_side")
def machine_side(name):
    """Бок/верх машины: корпус, по центру съёмная крышка на четырёх винтах с двумя щелями."""
    t = Tex()
    machine_casing(t)
    t.panel(8, 8, 23, 23, STEEL, base=2, grain=0.2, seed=53)
    t.ao(7, 7, 24, 24, k=0.18)
    for x, y in ((9, 9), (21, 9), (9, 21), (21, 21)):
        t.rivet(x, y, STEEL)
    for y in (14, 17):
        t.hline(11, 20, y, STEEL[0])
        t.hline(11, 20, y + 1, STEEL[3])
    return t


@tex("block/coal_generator_front", "block/coal_generator_front_on")
def coal_generator_front(name):
    """Фронт угольного генератора: топка с чугунной решёткой, колосник и зольник снизу, лампа над топкой."""
    on = name.endswith("_on")
    t = Tex()
    machine_casing(t, seed=55)
    # топка
    t.recess(7, 9, 24, 25, DARK_STEEL, base=1)
    inner = (8, 10, 23, 24)
    if on:
        for y in range(inner[1], inner[3] + 1):
            u = (y - inner[1]) / (inner[3] - inner[1])
            c = mix(AMBER[1], AMBER[3], u)
            t.hline(inner[0], inner[2], y, c)
        # угли на колоснике
        for x in range(inner[0], inner[2] + 1):
            h = (x * 7) % 5
            for y in range(20 - (h > 2), 25):
                t.put(x, y, AMBER[4] if (x + y) % 3 == 0 else RED[3])
        t.glow(15.5, 20, 11, AMBER[4], 0.3)
    else:
        t.rect(*inner, BLACK[1])
        for x in range(inner[0], inner[2] + 1):
            for y in range(21, 25):
                t.put(x, y, BLACK[3] if (x * 3 + y) % 4 == 0 else BLACK[2])
    # решётка дверцы: вертикальные прутья
    for x in range(9, 24, 3):
        t.vline(x, 10, 24, DARK_STEEL[3])
        t.vline(x + 1, 10, 24, DARK_STEEL[1])
    t.hline(8, 23, 16, DARK_STEEL[3])
    t.hline(8, 23, 17, DARK_STEEL[1])
    # петли дверцы — симметрично по сторонам
    for x in (5, 25):
        t.rect(x, 12, x + 1, 13, STEEL[1])
        t.rect(x, 21, x + 1, 22, STEEL[1])
    # лампа работы
    t.recess(14, 4, 17, 7, BLACK, base=1)
    t.led(15, 5, AMBER if on else AMBER, on=on)
    return t


@tex("block/copper_plate_block")
def copper_plate_block(name):
    """Медная плита (зажим электрода печи): шлифованная медь с фаской и швом по середине."""
    t = Tex()
    t.fill(COPPER, 2, grain=0.18, seed=57)
    t.bevel(0, 0, 31, 31, COPPER, raised=True, base=2)
    t.hline(1, 30, 15, COPPER[1])
    t.hline(1, 30, 16, COPPER[3])
    return t


@tex("block/creative_power")
def creative_power(name):
    """Творческий источник: корпус, светящийся янтарный ромб-кристалл с лучами."""
    t = Tex()
    machine_casing(t, seed=59)
    t.recess(5, 5, 26, 26, BLACK, base=1)
    t.glow(15.5, 15.5, 12, AMBER[3], 0.45)
    for y in range(6, 26):
        for x in range(6, 26):
            d = abs(x + 0.5 - 16) + abs(y + 0.5 - 16)
            if d <= 9:
                s = (x + 0.5 - 16) + (y + 0.5 - 16)
                k = 4 if d < 3 else 3 if s < -2 else 2 if s < 3 else 1
                t.put(x, y, AMBER[k])
            elif d <= 10:
                t.put(x, y, AMBER[0])
    # грани кристалла
    for i in range(-8, 9):
        t.blend(15 + (i > 0), 15 + i, AMBER[4], 0.35)
        t.blend(15 + i, 15 + (i > 0), AMBER[4], 0.25)
    return t


# ================================================================== солнечные панели
MONO = [(0x06, 0x08, 0x10), (0x0C, 0x10, 0x1E), (0x14, 0x1A, 0x2E), (0x22, 0x2C, 0x48), (0x4A, 0x5C, 0x88)]
POLY = [(0x10, 0x22, 0x52), (0x18, 0x32, 0x72), (0x22, 0x46, 0x94), (0x34, 0x60, 0xB4), (0x6A, 0x92, 0xD8)]
BACKSHEET = WHITE_PAINT


@tex("block/solar_panel_top")
def solar_panel_top(name):
    """Поликристаллический модуль: алюминиевая рамка, 3×3 элемента 8×8 на белой подложке, «иней»
    кристаллитов, два шинных проводника и тонкие токосъёмные полоски в каждом элементе."""
    t = Tex()
    t.rect(0, 0, 31, 31, BACKSHEET[2])
    t.bevel(0, 0, 31, 31, ALUMINIUM, raised=True, base=2)
    cells = (2, 12, 22)
    for i, cx in enumerate(cells):
        for j, cy in enumerate(cells):
            seed = 61 + i * 3 + j
            for y in range(cy, cy + 8):
                for x in range(cx, cx + 8):
                    # кристаллиты — пятна 3×2 разного оттенка
                    from .kit import _h
                    h = _h(x // 3 + (y // 2) % 2, y // 2, seed)
                    k = 1 if h < 0.3 else 3 if h > 0.78 else 2
                    t.put(x, y, POLY[k])
            for y in range(cy + 1, cy + 8, 2):
                for x in range(cx, cx + 8):
                    t.blend(x, y, POLY[4], 0.18)
            for bx in (cx + 2, cx + 5):
                t.vline(bx, cy, cy + 7, ALUMINIUM[1])
            t.put(cx, cy, POLY[4])
    return t


@tex("block/mono_solar_panel_top")
def mono_solar_panel_top(name):
    """Монокристаллическая панель: 2×2 псевдоквадратные пластины (срезанные углы открывают белую
    подложку), три серебряные шины и частые токосъёмные полоски; стык блоков — 2 px подложки."""
    t = Tex()
    t.rect(0, 0, 31, 31, BACKSHEET[3])
    for ox in (0, 16):
        for oy in (0, 16):
            x0, y0, x1, y1 = ox + 1, oy + 1, ox + 14, oy + 14
            for y in range(y0, y1 + 1):
                for x in range(x0, x1 + 1):
                    cx, cy = min(x - x0, x1 - x), min(y - y0, y1 - y)
                    if cx + cy < 2:
                        continue            # срезанный угол
                    k = 2
                    if y - y0 < 3 and x - x0 < 6:
                        k = 3                # отсвет неба на стекле
                    t.put(x, y, MONO[k])
            for y in range(y0 + 1, y1, 2):
                t.hline(x0 + 1, x1 - 1, y, MONO[1])
            for bx in (x0 + 3, x0 + 6, x0 + 10):
                t.vline(bx, y0, y1, ALUMINIUM[2])
                t.put(bx, y0, ALUMINIUM[4])
    return t


@tex("block/solar_panel_side")
def solar_panel_side(name):
    """Изнанка и торцы панели, стойка: анодированный алюминий, продольная шлифовка, профиль рамки
    и поперечные рёбра жёсткости; средняя полоса (стойка) — трубчатый профиль."""
    t = Tex()
    t.brushed(ALUMINIUM, 1, horizontal=False, seed=71)
    t.bevel(0, 0, 31, 31, ALUMINIUM, raised=True, base=1)
    for y in (5, 26):
        t.hline(1, 30, y, ALUMINIUM[3])
        t.hline(1, 30, y + 1, ALUMINIUM[0])
    # профиль стойки по центру
    for x in range(13, 19):
        u = (x - 13) / 5
        k = 3 if u < 0.3 else 2 if u < 0.7 else 0
        t.vline(x, 1, 30, ALUMINIUM[k])
    t.vline(12, 1, 30, ALUMINIUM[0])
    return t


@tex("block/solar_panel_bottom")
def solar_panel_bottom(name):
    """Опорная плита стойки: сталь на четырёх анкерах; по центру — фланец крепления стойки."""
    t = Tex()
    t.panel(0, 0, 31, 31, GUNMETAL, base=2, grain=0.2, seed=73)
    t.bevel(4, 4, 27, 27, GUNMETAL, raised=True, base=2)
    for x, y in ((6, 6), (24, 6), (6, 24), (24, 24)):
        t.rivet(x, y, STEEL)
        t.ao(x - 1, y - 1, x + 2, y + 2, k=0.2)
    t.ring(15.5, 15.5, 4, 6.5, STEEL, base=2)
    t.disc(15.5, 15.5, 4, GUNMETAL, base=1)
    return t


# ================================================================== РИТЭГ
@tex("block/rtg")
def rtg(name):
    """Корпус РИТЭГа (видна центральная полоса 16 px): тёмный корпус с фланцами сверху и снизу,
    по центру — окно-крышка источника с тёплым свечением и знаком радиации."""
    t = Tex()
    t.fill(GUNMETAL, 1, grain=0.2, seed=81)
    for x in range(8, 24):
        u = (x - 8) / 15
        k = 3 if u < 0.25 else 2 if u < 0.65 else 1
        t.vline(x, 0, 31, GUNMETAL[k])
    t.vline(8, 0, 31, GUNMETAL[4])
    t.vline(23, 0, 31, GUNMETAL[0])
    for y0 in (0, 28):
        t.panel(8, y0, 23, y0 + 3, STEEL, base=2, grain=0.1, seed=83)
        for x in (10, 15, 20):
            t.rivet(x, y0 + 1, STEEL)
    # крышка источника
    t.recess(10, 10, 21, 21, BLACK, base=1)
    t.glow(15.5, 15.5, 7, AMBER[3], 0.55)
    t.disc(15.5, 15.5, 4.5, AMBER, base=3)
    t.ring(15.5, 15.5, 4.5, 5.6, STEEL, base=2)
    for x, y in ((11, 11), (19, 11), (11, 19), (19, 19)):
        t.put(x, y, STEEL[4])
        t.put(x + 1, y + 1, STEEL[0])
    return t


@tex("block/rtg_fin")
def rtg_fin(name):
    """Ребро-радиатор: чёрная эмаль высокой излучательной способности, продольные каналы, светлая кромка."""
    t = Tex()
    t.brushed(BLACK, 3, horizontal=False, seed=85)
    for x in range(1, 32, 4):
        t.vline(x, 0, 31, BLACK[1])
        t.vline(x + 1, 0, 31, BLACK[4])
    t.bevel(0, 0, 31, 31, BLACK, raised=True, base=2)
    for y in (3, 28):
        t.hline(1, 30, y, GUNMETAL[3])
    return t


# ================================================================== ректенна и спутник энергии
@tex("block/rectenna_top")
def rectenna_top(name):
    """Поле ректенны: экран-сетка отражателя на тёмном основании, диполи с диодами по сетке 4 px;
    по центру — приёмно-выпрямительный узел."""
    t = Tex()
    t.fill(DARK_STEEL, 1, grain=0.2, seed=91)
    for i in range(0, 32, 2):
        for j in range(32):
            if (j + i) % 4 == 0:
                t.put(j, i, DARK_STEEL[2])
    for gy in range(2, 32, 8):
        for gx in range(2, 32, 8):
            # диполь: два медных плеча и диод между ними
            t.hline(gx, gx + 1, gy + 1, COPPER[3])
            t.hline(gx + 4, gx + 5, gy + 1, COPPER[3])
            t.hline(gx, gx + 5, gy + 2, COPPER[1])
            t.rect(gx + 2, gy, gx + 3, gy + 2, BLACK[1])
            t.put(gx + 2, gy, BLACK[4])
    t.bevel(0, 0, 31, 31, DARK_STEEL, raised=True, base=1)
    t.panel(12, 12, 19, 19, STEEL, base=2)
    t.led(15, 15, GREEN)
    return t


@tex("block/rectenna_side")
def rectenna_side(name):
    """Бок и дно основания ректенны (видна нижняя полоса 4 px): бетонно-стальная плита с кантом."""
    t = Tex()
    t.panel(0, 0, 31, 31, DARK_STEEL, base=2, grain=0.2, seed=93)
    t.hline(0, 31, 27, STEEL[3])
    t.rect(0, 28, 31, 30, STEEL[1])
    t.hline(0, 31, 31, STEEL[0])
    for x in range(3, 32, 8):
        t.rivet(x, 28, STEEL)
    return t


MLI = [(0x6A, 0x4A, 0x10), (0x9A, 0x70, 0x1C), (0xC4, 0x96, 0x30), (0xE2, 0xBE, 0x56), (0xFA, 0xE6, 0x9A)]


@tex("block/power_satellite")
def power_satellite(name):
    """Корпус спутника энергии (грани куба — центр 16×16): золотая экранно-вакуумная изоляция со
    складками, по центру — белый радиатор; снизу — тёмное кольцо адаптера."""
    from .kit import _h
    t = Tex()
    for y in range(32):
        for x in range(32):
            # складки плёнки — диагональные полосы переменной ширины
            h = _h((x + y) // 4, (x - y) // 9, 95)
            k = 1 if h < 0.18 else 3 if h > 0.8 else 2
            t.put(x, y, MLI[k])
    for y in range(32):
        for x in range(32):
            if _h((x + y) // 4, (x - y) // 9, 95) > 0.8 and (x + y) % 4 == 0:
                t.put(x, y, MLI[4])
    t.bevel(8, 8, 23, 23, GOLD_FOIL, raised=True, base=2)
    t.panel(11, 11, 20, 20, WHITE_PAINT, base=2, grain=0.1, seed=97)
    for y in range(13, 20, 2):
        t.hline(12, 19, y, WHITE_PAINT[0])
    # адаптер (видны строки 24…29 по центру)
    t.panel(9, 24, 22, 29, DARK_STEEL, base=2, grain=0.1, seed=99)
    for x in (11, 15, 19):
        t.rivet(x, 26, STEEL)
    return t
