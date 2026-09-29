"""Набор для рисования текстур 32×32 SpaceReloaded (012). Координаты — пиксели 0…31, (0, 0) — левый верх.

Единый стиль — см. STYLE.md. Коротко:
- свет сверху-слева: у выпуклого — светлая верхняя и левая кромки, тёмная нижняя и правая; у утопленного — наоборот;
- материал — палитра из 5 ступеней (0 тень … 4 блик); цвет берётся только из палитр;
- зерно материала — мягкое, детерминированное (hash координат), амплитуда ≤ 1 ступени; никакого «шума в каждом пикселе»;
- детали симметричны относительно центра грани, если у предмета нет физической причины для асимметрии;
- верх и низ блока — свои текстуры (клеммы, решётки, основания), а не повтор боковой грани.
"""
import math

from PIL import Image

N = 32

# ------------------------------------------------------------------ палитры (тень → блик)
STEEL = [(0x3A, 0x40, 0x48), (0x56, 0x5D, 0x67), (0x71, 0x79, 0x84), (0x8F, 0x98, 0xA3), (0xB6, 0xBE, 0xC7)]
DARK_STEEL = [(0x1B, 0x1E, 0x23), (0x28, 0x2C, 0x33), (0x37, 0x3C, 0x44), (0x4A, 0x50, 0x59), (0x66, 0x6D, 0x77)]
GUNMETAL = [(0x24, 0x28, 0x2E), (0x33, 0x39, 0x41), (0x45, 0x4C, 0x55), (0x5B, 0x63, 0x6D), (0x7D, 0x86, 0x91)]
ALUMINIUM = [(0x7E, 0x85, 0x8D), (0x9C, 0xA3, 0xAB), (0xB9, 0xBF, 0xC6), (0xD3, 0xD8, 0xDD), (0xEE, 0xF1, 0xF4)]
WHITE_PAINT = [(0xA8, 0xAC, 0xAE), (0xC4, 0xC7, 0xC8), (0xDB, 0xDD, 0xDD), (0xEC, 0xED, 0xEC), (0xFA, 0xFA, 0xF8)]
TITANIUM = [(0x5C, 0x5E, 0x66), (0x78, 0x7A, 0x84), (0x94, 0x96, 0xA0), (0xAF, 0xB1, 0xBA), (0xD0, 0xD2, 0xDA)]
COPPER = [(0x62, 0x30, 0x1C), (0x8C, 0x47, 0x29), (0xB4, 0x63, 0x38), (0xD6, 0x88, 0x52), (0xF0, 0xB4, 0x80)]
BRASS = [(0x6A, 0x52, 0x1E), (0x94, 0x74, 0x2C), (0xB9, 0x96, 0x3E), (0xD8, 0xB8, 0x5C), (0xF2, 0xDC, 0x94)]
GOLD_FOIL = [(0x7A, 0x5A, 0x16), (0xA8, 0x80, 0x24), (0xCC, 0xA0, 0x38), (0xE6, 0xC2, 0x58), (0xFA, 0xE6, 0x9A)]
GLASS = [(0x0E, 0x14, 0x1A), (0x16, 0x20, 0x28), (0x20, 0x2E, 0x38), (0x32, 0x46, 0x54), (0x6C, 0x8E, 0x9E)]
SCREEN = [(0x06, 0x12, 0x14), (0x0A, 0x1E, 0x22), (0x10, 0x2C, 0x32), (0x2A, 0x6E, 0x72), (0x6F, 0xD5, 0xE8)]
CERAMIC = [(0x9A, 0x92, 0x86), (0xB6, 0xAE, 0xA2), (0xCC, 0xC4, 0xB8), (0xDE, 0xD8, 0xCE), (0xF0, 0xEC, 0xE4)]
REFRACTORY = [(0x5A, 0x2E, 0x22), (0x7A, 0x40, 0x2E), (0x98, 0x56, 0x3C), (0xB4, 0x70, 0x50), (0xCE, 0x92, 0x70)]
BLACK = [(0x0E, 0x0F, 0x11), (0x18, 0x1A, 0x1D), (0x24, 0x27, 0x2B), (0x32, 0x36, 0x3B), (0x48, 0x4D, 0x53)]
RUBBER = [(0x14, 0x14, 0x16), (0x1E, 0x1F, 0x22), (0x2A, 0x2B, 0x2F), (0x38, 0x3A, 0x3E), (0x4E, 0x50, 0x55)]
WOOD = [(0x4A, 0x32, 0x1C), (0x66, 0x46, 0x28), (0x84, 0x5E, 0x38), (0xA2, 0x7A, 0x4C), (0xBE, 0x98, 0x68)]
# сигнальные
GREEN = [(0x0E, 0x3A, 0x1C), (0x18, 0x5E, 0x2C), (0x2C, 0x8E, 0x44), (0x4C, 0xC0, 0x62), (0xA0, 0xF0, 0xAA)]
RED = [(0x5A, 0x12, 0x12), (0x8A, 0x1C, 0x1C), (0xB8, 0x2A, 0x26), (0xDC, 0x4A, 0x3E), (0xF4, 0x8E, 0x7E)]
AMBER = [(0x5E, 0x34, 0x06), (0x8E, 0x52, 0x0A), (0xC2, 0x76, 0x12), (0xEA, 0xA0, 0x2A), (0xFF, 0xD2, 0x7A)]
BLUE = [(0x0E, 0x24, 0x4A), (0x16, 0x38, 0x6E), (0x22, 0x52, 0x98), (0x3C, 0x78, 0xC4), (0x86, 0xB4, 0xEC)]
CYAN = [(0x0A, 0x34, 0x3C), (0x12, 0x52, 0x5C), (0x1E, 0x7A, 0x86), (0x3A, 0xAA, 0xB8), (0x9A, 0xE6, 0xEE)]
HAZARD_YELLOW = [(0x6E, 0x58, 0x08), (0x9C, 0x7E, 0x0E), (0xCC, 0xA8, 0x18), (0xEE, 0xCC, 0x30), (0xFF, 0xEC, 0x80)]
# ГОСТ 949 — маркировка баллонов: кислород голубой, азот чёрный
O2_BLUE = [(0x2A, 0x5E, 0x88), (0x3C, 0x7E, 0xAE), (0x58, 0x9E, 0xCC), (0x7E, 0xBC, 0xE2), (0xB4, 0xDC, 0xF4)]


def mix(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def shade(c, k):
    """k > 0 светлее, k < 0 темнее (доля)."""
    if k >= 0:
        return mix(c, (255, 255, 255), k)
    return mix(c, (0, 0, 0), -k)


def _h(x, y, seed):
    v = (x * 374761393 + y * 668265263 + seed * 2147483647) & 0xFFFFFFFF
    v = (v ^ (v >> 13)) * 1274126177 & 0xFFFFFFFF
    return ((v ^ (v >> 16)) & 0xFFFF) / 65535.0


class Tex:
    """Холст N×N (по умолчанию 32×32, RGBA)."""

    def __init__(self, fill=None, n=N):
        self.n = n
        self.img = Image.new("RGBA", (n, n), (0, 0, 0, 0) if fill is None else tuple(fill) + (255,))

    # --------------------------------------------------------------- базовое
    def put(self, x, y, c, a=255):
        if 0 <= x < self.n and 0 <= y < self.n:
            self.img.putpixel((int(x), int(y)), tuple(c[:3]) + ((c[3],) if len(c) == 4 else (a,)))

    def get(self, x, y):
        return self.img.getpixel((x, y))

    def rect(self, x0, y0, x1, y1, c):
        """Заливка [x0, x1] × [y0, y1] включительно."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, c)

    def hline(self, x0, x1, y, c):
        self.rect(x0, y, x1, y, c)

    def vline(self, x, y0, y1, c):
        self.rect(x, y0, x, y1, c)

    def blend(self, x, y, c, t):
        """Подмешать цвет c с долей t к существующему пикселю."""
        if 0 <= x < self.n and 0 <= y < self.n:
            r, g, b, a = self.img.getpixel((x, y))
            m = mix((r, g, b), c, t)
            self.img.putpixel((x, y), m + (a if a else 255,))

    def darken(self, x, y, k):
        if 0 <= x < self.n and 0 <= y < self.n:
            r, g, b, a = self.img.getpixel((x, y))
            self.img.putpixel((x, y), shade((r, g, b), -k) + (a,))

    def lighten(self, x, y, k):
        if 0 <= x < self.n and 0 <= y < self.n:
            r, g, b, a = self.img.getpixel((x, y))
            self.img.putpixel((x, y), shade((r, g, b), k) + (a,))

    # --------------------------------------------------------------- материал
    def fill(self, pal, base=2, x0=0, y0=0, x1=None, y1=None, grain=0.35, seed=1):
        """Заливка материалом с мягким зерном: пятна по 2×2 px, отклонение на ±1 ступень с вероятностью grain·0.5."""
        x1 = self.n - 1 if x1 is None else x1
        y1 = self.n - 1 if y1 is None else y1
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                h = _h(x // 2, y // 2, seed)
                k = base
                if h < grain * 0.25:
                    k = max(0, base - 1)
                elif h > 1 - grain * 0.25:
                    k = min(4, base + 1)
                self.put(x, y, pal[k])

    def brushed(self, pal, base=2, x0=0, y0=0, x1=None, y1=None, horizontal=True, seed=3):
        """Шлифованный металл: тонкие полосы вдоль направления шлифовки."""
        x1 = self.n - 1 if x1 is None else x1
        y1 = self.n - 1 if y1 is None else y1
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                line = y if horizontal else x
                h = _h(line, (x if horizontal else y) // 8, seed)
                c = pal[base]
                if h < 0.18:
                    c = mix(pal[base], pal[max(0, base - 1)], 0.5)
                elif h > 0.82:
                    c = mix(pal[base], pal[min(4, base + 1)], 0.5)
                self.put(x, y, c)

    # --------------------------------------------------------------- объём
    def bevel(self, x0, y0, x1, y1, pal, width=1, raised=True, base=None):
        """Фаска по периметру прямоугольника (не трогает середину): блик сверху-слева, тень снизу-справа."""
        hi, lo = (4, 1) if raised else (1, 3)
        if base is not None:
            hi, lo = (min(4, base + 2), max(0, base - 1)) if raised else (max(0, base - 1), min(4, base + 1))
        for w in range(width):
            for x in range(x0 + w, x1 - w + 1):
                self.put(x, y0 + w, pal[hi])
                self.put(x, y1 - w, pal[lo])
            for y in range(y0 + w, y1 - w + 1):
                self.put(x0 + w, y, pal[hi])
                self.put(x1 - w, y, pal[lo])
            self.put(x1 - w, y0 + w, mix(pal[hi], pal[lo], 0.5))
            self.put(x0 + w, y1 - w, mix(pal[hi], pal[lo], 0.5))

    def panel(self, x0, y0, x1, y1, pal, base=2, raised=True, grain=0.25, seed=5, width=1):
        """Панель: заливка материалом + фаска."""
        self.fill(pal, base, x0, y0, x1, y1, grain=grain, seed=seed)
        self.bevel(x0, y0, x1, y1, pal, width=width, raised=raised, base=base)

    def recess(self, x0, y0, x1, y1, pal, base=1, seed=7):
        """Утопленная ниша: тень сверху-слева внутри, блик по нижней-правой кромке."""
        self.fill(pal, base, x0, y0, x1, y1, grain=0.15, seed=seed)
        self.hline(x0, x1, y0, pal[0])
        self.vline(x0, y0, y1, pal[0])
        self.hline(x0 + 1, x1, y1, pal[min(4, base + 1)])
        self.vline(x1, y0 + 1, y1, pal[min(4, base + 1)])

    def ao(self, x0, y0, x1, y1, k=0.22, width=1):
        """Затенение внутрь от кромки прямоугольника (контакт с рамкой)."""
        for w in range(width):
            kk = k * (1 - w / max(1, width))
            for x in range(x0 + w, x1 - w + 1):
                self.darken(x, y0 + w, kk)
                self.darken(x, y1 - w, kk * 0.5)
            for y in range(y0 + w + 1, y1 - w):
                self.darken(x0 + w, y, kk)
                self.darken(x1 - w, y, kk * 0.5)

    def frame(self, pal, width=2, base=2, inner_pal=None, inner_base=None):
        """Стандартная рамка грани машины: выпуклый обод шириной width по краю блока."""
        self.bevel(0, 0, self.n - 1, self.n - 1, pal, width=1, raised=True, base=base)
        if width > 1:
            self.bevel(width - 1, width - 1, self.n - width, self.n - width, pal, width=1, raised=False, base=base)

    # --------------------------------------------------------------- детали
    def rivet(self, x, y, pal=STEEL):
        """Заклёпка 2×2: блик, полутона, тень."""
        self.put(x, y, pal[4])
        self.put(x + 1, y, pal[3])
        self.put(x, y + 1, pal[3])
        self.put(x + 1, y + 1, pal[1])

    def rivets(self, inset=3, pal=STEEL, n=None):
        """Четыре заклёпки по углам с отступом inset."""
        m = (n or self.n) - inset - 2
        for x, y in ((inset, inset), (m, inset), (inset, m), (m, m)):
            self.rivet(x, y, pal)

    def screw(self, x, y, pal=STEEL):
        """Винт 3×3 со шлицем."""
        self.rect(x, y, x + 2, y + 2, pal[3])
        self.put(x, y, pal[4])
        self.put(x + 2, y + 2, pal[1])
        self.hline(x, x + 2, y + 1, pal[1])

    def disc(self, cx, cy, r, pal, base=2, lit=True):
        """Диск со сферическим освещением (блик сверху-слева)."""
        for y in range(int(cy - r - 1), int(cy + r + 2)):
            for x in range(int(cx - r - 1), int(cx + r + 2)):
                dx, dy = x + 0.5 - cx, y + 0.5 - cy
                d = math.hypot(dx, dy)
                if d <= r:
                    k = base
                    if lit:
                        s = -(dx + dy) / (r * 1.414)   # сверху-слева положительно
                        if s > 0.45:
                            k = min(4, base + 1)
                        elif s < -0.45:
                            k = max(0, base - 1)
                    if d > r - 1:
                        k = max(0, k - 1)
                    self.put(x, y, pal[k])

    def ring(self, cx, cy, r0, r1, pal, base=2):
        for y in range(int(cy - r1 - 1), int(cy + r1 + 2)):
            for x in range(int(cx - r1 - 1), int(cx + r1 + 2)):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if r0 <= d <= r1:
                    s = -((x + 0.5 - cx) + (y + 0.5 - cy)) / (r1 * 1.414)
                    k = base + (1 if s > 0.3 else -1 if s < -0.3 else 0)
                    self.put(x, y, pal[max(0, min(4, k))])

    def grille(self, x0, y0, x1, y1, pal, pitch=3, vertical=False):
        """Вентиляционная решётка: прорези через pitch, с тенью."""
        self.recess(x0, y0, x1, y1, pal, base=1)
        if vertical:
            for x in range(x0 + 1, x1, pitch):
                self.vline(x, y0 + 1, y1 - 1, pal[0])
                self.vline(x + 1, y0 + 1, y1 - 1, pal[3])
        else:
            for y in range(y0 + 1, y1, pitch):
                self.hline(x0 + 1, x1 - 1, y, pal[0])
                self.hline(x0 + 1, x1 - 1, y + 1, pal[3])

    def screen(self, x0, y0, x1, y1, pal=SCREEN, lit=True, rows=3, seed=11):
        """Экран: тёмное стекло, строки «данных» (без букв), блик в углу."""
        self.recess(x0, y0, x1, y1, BLACK, base=1)
        self.rect(x0 + 1, y0 + 1, x1 - 1, y1 - 1, pal[1])
        if lit:
            h = (y1 - y0 - 2)
            for r in range(rows):
                yy = y0 + 2 + r * max(2, h // max(1, rows))
                if yy >= y1 - 1:
                    break
                w = int((x1 - x0 - 4) * (0.45 + 0.5 * _h(r, seed, 3)))
                self.hline(x0 + 2, x0 + 2 + w, yy, pal[3] if r else pal[4])
        self.put(x0 + 1, y0 + 1, pal[4] if lit else pal[2])

    def led(self, x, y, pal=GREEN, on=True):
        """Светодиод 2×2 с бликом."""
        c = pal[3] if on else pal[1]
        self.rect(x, y, x + 1, y + 1, c)
        self.put(x, y, pal[4] if on else pal[2])

    def hazard(self, x0, y0, x1, y1, width=3):
        """Предупредительная полоса «жёлтый-чёрный» под 45°."""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.put(x, y, HAZARD_YELLOW[3] if ((x + y) // width) % 2 == 0 else BLACK[2])

    def pipe_h(self, x0, x1, y, r, pal):
        """Горизонтальная труба радиусом r с цилиндрическим освещением."""
        for dy in range(-r, r + 1):
            t = dy / max(1, r)
            k = 3 if t < -0.4 else (1 if t > 0.5 else 2)
            if abs(dy) == r:
                k = 0 if dy > 0 else 4
            self.hline(x0, x1, y + dy, pal[k])

    def pipe_v(self, x, y0, y1, r, pal):
        for dx in range(-r, r + 1):
            t = dx / max(1, r)
            k = 3 if t < -0.4 else (1 if t > 0.5 else 2)
            if abs(dx) == r:
                k = 0 if dx > 0 else 4
            self.vline(x + dx, y0, y1, pal[k])

    def glow(self, cx, cy, r, color, strength=0.6):
        """Мягкое свечение (подмешивание цвета с убыванием от центра)."""
        for y in range(int(cy - r), int(cy + r) + 1):
            for x in range(int(cx - r), int(cx + r) + 1):
                d = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if d < r:
                    self.blend(x, y, color, strength * (1 - d / r) ** 1.5)

    # --------------------------------------------------------------- предметы
    def outline(self, color=(0x14, 0x16, 0x1A), alpha_threshold=10):
        """Тёмный контур вокруг непрозрачной формы предмета (как у ванильных иконок)."""
        src = self.img.copy()
        for y in range(self.n):
            for x in range(self.n):
                if src.getpixel((x, y))[3] > alpha_threshold:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    xx, yy = x + dx, y + dy
                    if 0 <= xx < self.n and 0 <= yy < self.n and src.getpixel((xx, yy))[3] > alpha_threshold:
                        self.img.putpixel((x, y), color + (255,))
                        break

    def blob(self, cx, cy, rx, ry, pal, base=2, seed=13, rough=0.18):
        """Кучка/комок вещества: эллипс с неровным краем, освещение сверху-слева, зернистость."""
        for y in range(int(cy - ry - 2), int(cy + ry + 3)):
            for x in range(int(cx - rx - 2), int(cx + rx + 3)):
                dx, dy = (x + 0.5 - cx) / rx, (y + 0.5 - cy) / ry
                edge = 1 + rough * (_h(int(math.atan2(dy, dx) * 6), 1, seed) - 0.5)
                d = math.hypot(dx, dy)
                if d <= edge:
                    s = -(dx + dy) / 1.414
                    k = base + (1 if s > 0.35 else -1 if s < -0.35 else 0)
                    if d > edge - 0.18:
                        k -= 1
                    if _h(x, y, seed) > 0.9:
                        k += 1
                    self.put(x, y, pal[max(0, min(4, k))])

    def ingot(self, pal, seed=17):
        """Слиток (трапеция в изометрии), как ванильные слитки, но 32 px."""
        top = [(8, 11), (24, 11), (27, 17), (5, 17)]
        for y in range(11, 23):
            for x in range(3, 29):
                if y <= 17:
                    t = (y - 11) / 6
                    l, r = 8 - 3 * t, 24 + 3 * t
                    if l <= x <= r:
                        self.put(x, y, pal[4] if y == 11 else pal[3])
                else:
                    if 5 <= x <= 27:
                        self.put(x, y, pal[2] if x < 22 else pal[1])
        self.hline(5, 27, 17, pal[4])
        self.hline(5, 27, 22, pal[0])
        for x in (9, 14, 19):
            self.put(x, 13, pal[4])
        self.outline()

    def save(self, path):
        self.img.save(path)
