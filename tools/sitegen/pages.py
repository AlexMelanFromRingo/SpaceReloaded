"""HTML страниц: общий каркас, руководство, книга рецептов, мультиблоки."""
import json, re
from html import escape

from . import res, icons, content as C
from .mb import Multiblock

SITE_URL = "https://alexmelanfromringo.github.io/SpaceReloaded/"
REPO_URL = "https://github.com/AlexMelanFromRingo/SpaceReloaded"
ASSET_VER = "8"  # сброс кэша CSS/JS при изменении

PROBLEMS = []  # (контекст, id, что не так) — проверка в конце генерации


# ---------- предметы ----------

def item_info(ref, ctx):
    """ref: id или #тег предмета. -> (name, Icon, alt_names)."""
    if ref.startswith("#"):
        tag = ref[1:]
        try:
            vals = res.tag_values(tag, "item")
        except res.ResourceError:
            vals = []
        first = vals[0] if vals else None
        name = C.TAG_RU.get(tag) or (f"{res.ru_name(first)} или другой из #{tag}" if first else None)
        icon = icons.get(first) if first else None
    else:
        name, icon = res.ru_name(ref), icons.get(ref)
    if not name:
        PROBLEMS.append((ctx, ref, "нет русского имени"))
    if icon is None:
        PROBLEMS.append((ctx, ref, "нет иконки"))
    return name or ref, icon


def img_tag(icon, alt, px):
    cls = "ico flat" if icon.flat else "ico"
    return (f'<img class="{cls}" src="{icon.src}" alt="{escape(alt)}" width="{px}" height="{px}" '
            f'loading="lazy" decoding="async">')


def slot(ref, ctx, count=1, big=False, extra=""):
    name, icon = item_info(ref, ctx)
    tip = f"{name} × {count}" if count > 1 else name
    inner = img_tag(icon, name, 64 if big else 48) if icon else '<span class="noicon" aria-hidden="true">?</span>'
    cnt = f'<span class="cnt" aria-hidden="true">{count}</span>' if count > 1 else ""
    return (f'<span class="slot{" big" if big else ""}{extra}" data-tip="{escape(tip)}">'
            f'{inner}{cnt}</span>')


def mini(ref, ctx):
    """Маленькая иконка станка рядом с подписью."""
    name, icon = item_info(ref, ctx)
    return img_tag(icon, "", 20).replace('class="ico', 'class="ico mini') if icon else ""


# ---------- каркас ----------

NAV = [("index.html", "Руководство", "guide"), ("recipes.html", "Рецепты", "recipes"),
       ("multiblocks.html", "Мультиблоки", "multiblocks")]

THEME_BOOT = ("<script>try{var t=localStorage.getItem('sr-theme');"
              "if(t)document.documentElement.dataset.theme=t}catch(e){}"
              "document.documentElement.classList.add('js')</script>")

SUN = ('<svg class="i-sun" viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="4.5"/>'
       '<path d="M12 1.5v3M12 19.5v3M1.5 12h3M19.5 12h3M4.6 4.6l2.1 2.1M17.3 17.3l2.1 2.1M4.6 19.4l2.1-2.1M17.3 6.7l2.1-2.1"/></svg>')
MOON = '<svg class="i-moon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.5 14.5A8.5 8.5 0 1 1 9.5 3.5a7 7 0 0 0 11 11z"/></svg>'
GH = ('<svg viewBox="0 0 16 16" aria-hidden="true"><path d="M8 0a8 8 0 0 0-2.53 15.59c.4.07.55-.17.55-.38v-1.33c-2.23.48-2.7-1.07-2.7-1.07'
      '-.36-.92-.89-1.17-.89-1.17-.73-.5.05-.49.05-.49.8.06 1.23.83 1.23.83.72 1.22 1.87.87 2.33.66.07-.52.28-.87.5-1.07'
      '-1.78-.2-3.64-.89-3.64-3.95 0-.87.31-1.59.82-2.15-.08-.2-.36-1.02.08-2.12 0 0 .67-.21 2.2.82a7.6 7.6 0 0 1 4 0'
      'c1.53-1.04 2.2-.82 2.2-.82.44 1.1.16 1.92.08 2.12.51.56.82 1.28.82 2.15 0 3.07-1.87 3.75-3.65 3.95.29.25.54.73.54'
      ' 1.48v2.2c0 .21.15.46.55.38A8 8 0 0 0 8 0z"/></svg>')


def page(fname, title, description, active, body):
    nav = "".join(
        f'<a href="{h}"{" aria-current=\"page\"" if k == active else ""}>{lbl}</a>' for h, lbl, k in NAV)
    url = SITE_URL + ("" if fname == "index.html" else fname)
    return f'''<!doctype html>
<html lang="ru">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width,initial-scale=1">
<title>{escape(title)}</title>
<meta name="description" content="{escape(description)}">
<meta name="color-scheme" content="light dark">
<meta name="theme-color" content="#0d1418" media="(prefers-color-scheme: dark)">
<meta name="theme-color" content="#eef1f3" media="(prefers-color-scheme: light)">
<link rel="canonical" href="{url}">
<meta property="og:type" content="website">
<meta property="og:site_name" content="SpaceReloaded">
<meta property="og:locale" content="ru_RU">
<meta property="og:title" content="{escape(title)}">
<meta property="og:description" content="{escape(description)}">
<meta property="og:url" content="{url}">
<meta property="og:image" content="{SITE_URL}img/favicon.png">
<meta name="twitter:card" content="summary">
<link rel="icon" type="image/png" href="img/favicon.png">
<link rel="apple-touch-icon" href="img/favicon.png">
<link rel="stylesheet" href="assets/site.css?v={ASSET_VER}">
{THEME_BOOT}
<script src="assets/site.js?v={ASSET_VER}" defer></script>
</head>
<body>
<a class="skip" href="#main">К содержимому</a>
<header class="topbar">
  <div class="wrap topbar-in">
    <a class="brand" href="index.html" aria-label="SpaceReloaded, на главную">
      <img src="img/favicon.png" alt="" width="28" height="28"><span>SPACE<b>RELOADED</b></span></a>
    <nav class="nav" aria-label="Разделы сайта">{nav}</nav>
    <a class="icon-btn gh" href="{REPO_URL}" aria-label="Исходники на GitHub">{GH}</a>
    <button class="icon-btn theme" type="button" aria-label="Переключить тему" title="Тема: светлая / тёмная">{SUN}{MOON}</button>
  </div>
</header>
<main id="main" class="wrap">
{body}
</main>
<footer class="footer">
  <div class="wrap">
    <p>SpaceReloaded для Minecraft 26.2 (Fabric). Иконки, рецепты и схемы собраны из ресурсов мода
    генератором <code>tools/gen_site.py</code>.</p>
    <p><a href="{REPO_URL}">Исходники и сборка на GitHub</a> · <a href="{REPO_URL}/blob/main/docs/ADDONS.md">Аддоны</a></p>
  </div>
</footer>
<div class="tip" role="tooltip" hidden></div>
</body>
</html>
'''


def hero(eyebrow, h1, lead, tags=()):
    t = "".join(f"<li>{escape(x)}</li>" for x in tags)
    return (f'<section class="hero"><p class="eyebrow">{eyebrow}</p><h1>{h1}</h1><p class="hero-lead">{lead}</p>'
            + (f'<ul class="tags" aria-label="Ключевые темы">{t}</ul>' if t else "")
            + '<div class="hazard" aria-hidden="true"></div></section>')


# ---------- руководство ----------

_TR = dict(zip("абвгдеёжзийклмнопрстуфхцчшщъыьэюя",
               "a b v g d e e zh z i y k l m n o p r s t u f h ts ch sh sch  y  e yu ya".split(" ")))


def slug(text):
    s = "".join(_TR.get(ch, ch) for ch in text.lower())
    s = re.sub(r"[^a-z0-9]+", "-", s).strip("-")
    return s[:40].strip("-") or "s"


def guide_page():
    toc, seen = [], set()

    def repl(m):
        title = m.group(1)
        sid = slug(title)
        while sid in seen:
            sid += "-2"
        seen.add(sid)
        toc.append((sid, title))
        return f'<section class="gsec" id="{sid}" aria-labelledby="{sid}-h">\n<h2 id="{sid}-h">{title}</h2>'

    body = re.sub(r"<section>\s*<h3>(.*?)</h3>", repl, C.GUIDE_HTML)
    toc_html = "".join(f'<li><a href="#{sid}">{t}</a></li>' for sid, t in toc)
    h = hero("Космос на честной физике", "Руководство командира",
             "Ракета собирается из блоков, которые вы поставили сами, и летит по формуле Циолковского. "
             "Тяговооружённость считается по реальной массе деталей, кривая ракета заваливается на взлёте, "
             "а диагональная щель в обшивке базы травит воздух в вакуум. Здесь описан весь путь "
             "от первого слитка железа до замкнутой межпланетной логистики.",
             ["Minecraft 26.2", "Fabric", "Циолковский · TWR", "Герметичность 26 направлений", "ISRU",
              "Стыковка", "Орбитальный удар", "Jade", "Топливо-жидкость", "Δv по Гоману", "Грузовые линии",
              "Wet workshop", "Катапульта О’Нила", "Мультиблоки", "P = τ·ω"])
    quick = ('<nav class="quick" aria-label="Другие разделы">'
             '<a class="quick-card" href="recipes.html"><b>Книга рецептов</b><span>Все рецепты по фазам прогрессии, поиск и фильтр по станку</span></a>'
             '<a class="quick-card" href="multiblocks.html"><b>Мультиблоки</b><span>Схемы по слоям, материалы и физика каждой структуры</span></a></nav>')
    return h + quick + (
        '<div class="with-toc">'
        f'<aside class="toc"><details open><summary>Содержание</summary><ol>{toc_html}</ol></details></aside>'
        f'<article class="guide prose">{body}</article></div>')


# ---------- рецепты ----------

KINDS = {
    "craft": ("Верстак", "minecraft:crafting_table"),
    "assembly": ("Сборочный стол", "spacereloaded:assembly_table"),
    "crushing": ("Дробилка", "spacereloaded:crusher"),
    "smelting": ("Электропечь", "spacereloaded:electric_furnace"),
    "pressing": ("Пресс", "spacereloaded:mechanical_press"),
    "machining": ("Токарный станок", "spacereloaded:lathe"),
    "chain": ("Цепочка операций", None),
}
TYPE_KIND = {"minecraft:crafting_shaped": "craft", "minecraft:crafting_shapeless": "craft",
             "spacereloaded:assembly": "assembly", "spacereloaded:crushing": "crushing",
             "spacereloaded:electric_smelting": "smelting", "spacereloaded:pressing": "pressing",
             "spacereloaded:machining": "machining"}


def _ing_id(x):
    if isinstance(x, str):
        return x
    if isinstance(x, dict):
        return x.get("item") or x.get("id") or ("#" + x["tag"] if "tag" in x else None)
    if isinstance(x, list) and x:
        return _ing_id(x[0])
    return None


def _short(i):
    return i.lstrip("#").partition(":")[2]


def load_recipes():
    """-> список рецептов (dict), цепочки операций собраны в один рецепт."""
    out, steps, unknown = [], {}, []
    for f in sorted((res.RES / f"data/{res.MOD}/recipe").glob("*.json")):
        r = json.loads(f.read_text())
        t = r["type"]
        kind = TYPE_KIND.get(t)
        if kind is None:
            unknown.append(f.name); continue
        res_id, rcount = r["result"]["id"], r["result"].get("count", 1)
        rec = {"file": f.stem, "kind": kind, "result": res_id, "count": rcount, "raw": r}
        if "step" in r:
            work = r["ingredient"] if r["step"] > 0 else res_id
            steps.setdefault(work, []).append(rec)
            continue
        if t == "minecraft:crafting_shaped":
            key = r["key"]
            rec["grid"] = [[_ing_id(key[ch]) if ch in key else None for ch in row.ljust(3)] for row in r["pattern"]]
            ins = [c for row in rec["grid"] for c in row if c]
        elif "ingredients" in r:
            ins = [_ing_id(i) for i in r["ingredients"]]
        else:
            ins = [_ing_id(r["ingredient"])]
        counted = {}
        for i in ins:
            counted[i] = counted.get(i, 0) + 1
        rec["inputs"] = list(counted.items())
        out.append(rec)
    for work, lst in steps.items():
        lst.sort(key=lambda x: x["raw"]["step"])
        first, last = lst[0], lst[-1]
        out.append({"file": last["file"], "kind": "chain", "result": last["result"], "count": last["count"],
                    "start": first["raw"]["ingredient"], "work": work,
                    "steps": [(s["kind"], s["raw"]) for s in lst], "inputs": [(first["raw"]["ingredient"], 1)]})
    if unknown:
        PROBLEMS.append(("рецепты", ", ".join(unknown), "неизвестный тип рецепта"))
    return out


def recipe_card(rec):
    ctx = f"рецепт {rec['file']}"
    kind_label, machine = KINDS[rec["kind"]]
    rname, _ = item_info(rec["result"], ctx)
    names = [rname, kind_label]
    result = slot(rec["result"], ctx, rec["count"], big=True)
    arrow_icon = mini(machine, ctx) if machine else ""
    arrow = f'<span class="arr" aria-hidden="true">{arrow_icon}<svg viewBox="0 0 24 12"><path d="M0 6h20M15 1l6 5-6 5"/></svg></span>'
    if rec["kind"] == "chain":
        sname, _ = item_info(rec["start"], ctx)
        wname, _ = item_info(rec["work"], ctx)
        names += [sname, wname]
        items = []
        for n, (k, raw) in enumerate(rec["steps"], 1):
            lbl, mach = KINDS[k]
            extra = []
            if raw.get("energy_j"):
                extra.append(f'{raw["energy_j"] / 1000:g} кДж')
            if raw.get("balance"):
                extra.append("балансировка")
            ex = f' <small>{", ".join(extra)}</small>' if extra else ""
            items.append(f'<li>{mini(mach, ctx)}<span>{lbl}{ex}</span></li>')
            names.append(lbl)
        io = (f'<div class="io">{slot(rec["start"], ctx)}{arrow}{slot(rec["work"], ctx)}{arrow}{result}</div>'
              f'<ol class="steps" aria-label="Операции по порядку">{"".join(items)}</ol>')
        ings = f'{escape(sname)} → {escape(wname)} → {escape(rname)}'
    else:
        if "grid" in rec:
            cells = "".join(
                (f'<span class="cell">{slot(c, ctx)}</span>' if c else '<span class="cell empty"></span>')
                for row in rec["grid"] for c in row)
            left = f'<span class="grid3" role="img" aria-label="Схема верстака">{cells}</span>'
        else:
            left = '<span class="ins">' + "".join(slot(i, ctx, n) for i, n in rec["inputs"]) + "</span>"
        io = f'<div class="io">{left}{arrow}{result}</div>'
        parts = []
        for i, n in rec["inputs"]:
            nm, _ = item_info(i, ctx)
            names.append(nm)
            parts.append(f"{n} × {escape(nm)}" if n > 1 else escape(nm))
        ings = " · ".join(parts)
    desc = C.DESC.get(_short(rec["result"]), "")
    note = C.RECIPE_NOTE.get(rec["file"], "")
    search = " ".join(names + [_short(rec["result"])]).lower()
    kind_badge = f'<span class="kind">{mini(machine, ctx) if machine else ""}{kind_label}</span>'
    cnt = f' <span class="qty">× {rec["count"]}</span>' if rec["count"] > 1 else ""
    return (f'<article class="card" data-kind="{rec["kind"]}" data-search="{escape(search)}">'
            f'<header class="card-head"><h3>{escape(rname)}{cnt}</h3>{kind_badge}</header>'
            f'{io}<p class="ings"><span class="vh">Состав: </span>{ings}</p>'
            + (f'<p class="desc">{desc}</p>' if desc else "")
            + (f'<p class="note">{note}</p>' if note else "")
            + "</article>")


def recipes_page(recipes):
    order = list(C.DESC.keys())
    by_phase = {}
    for rec in recipes:
        ph = C.RECIPE_PHASE.get(rec["file"], C.PHASE.get(_short(rec["result"]), -1))
        by_phase.setdefault(ph, []).append(rec)
    sections, toc = [], []
    phases = list(enumerate(C.PHASES))
    if -1 in by_phase:
        phases.append((-1, ("Прочее", "Рецепты, ещё не отнесённые к фазе прогрессии.")))
    for idx, (title, lead) in phases:
        lst = by_phase.get(idx)
        if not lst:
            continue
        lst.sort(key=lambda r: (order.index(_short(r["result"])) if _short(r["result"]) in order else 999, r["file"]))
        sid = f"phase-{idx}" if idx >= 0 else "phase-other"
        num, _, name = title.partition(". ")
        toc.append(f'<li><a href="#{sid}"><span>{escape(num.replace("Фаза ", ""))}</span>{escape(name or title)}</a></li>')
        sections.append(
            f'<section class="phase" id="{sid}" aria-labelledby="{sid}-h"><h2 id="{sid}-h">{escape(title)}</h2>'
            + (f'<p class="lead">{lead}</p>' if lead else "")
            + '<div class="cards">' + "".join(recipe_card(r) for r in lst) + "</div></section>")
    kinds_used = [k for k in KINDS if any(r["kind"] == k for r in recipes)]
    chips = '<button type="button" class="chip" data-kind="all" aria-pressed="true">Все</button>' + "".join(
        f'<button type="button" class="chip" data-kind="{k}" aria-pressed="false">'
        f'{mini(KINDS[k][1], "фильтр") if KINDS[k][1] else ""}{KINDS[k][0]}</button>' for k in kinds_used)
    h = hero("Все рецепты по фазам", "Книга рецептов",
             "Рецепты сгруппированы по этапам прогрессии, а не по станкам: сверху вниз это порядок, в котором "
             "их открываешь. Под каждым предметом сказано, что это и зачем нужно. Наведите курсор или нажмите на "
             "иконку, чтобы увидеть название; число в углу слота это количество.")
    tools = (f'<div class="filters" role="search"><label class="search"><span class="vh">Поиск рецепта</span>'
             f'<svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="10.5" cy="10.5" r="6.5"/><path d="M15.5 15.5l5 5"/></svg>'
             f'<input id="q" type="search" placeholder="Поиск: предмет или ингредиент" autocomplete="off"></label>'
             f'<div class="chips" role="group" aria-label="Тип станка">{chips}</div>'
             f'<p class="count" aria-live="polite"><span id="shown">{len(recipes)}</span> из {len(recipes)} рецептов</p></div>')
    return (h + f'<nav class="phase-toc" aria-label="Фазы прогрессии"><ol>{"".join(toc)}</ol></nav>' + tools
            + '<div id="recipes">' + "".join(sections) + '</div>'
            + '<p class="no-results" id="empty" hidden>Ничего не найдено. Попробуйте другое слово или сбросьте фильтр станка.</p>')


# ---------- мультиблоки ----------

def _block_label(b, ctx):
    if b == "minecraft:air":
        return res.ru_name(b)
    if b.startswith("#"):
        vals = res.tag_values(b, "block")
        return " или ".join(item_info(v, ctx)[0] for v in vals)
    return item_info(b, ctx)[0]


def mb_article(m: Multiblock):
    spec = C.MULTIBLOCKS.get(m.id, {})
    ctx = f"мультиблок {m.id}"
    title = spec.get("title") or item_info(m.key, ctx)[0]
    src, (w, h) = m.render(yrot=spec.get("view", 0))
    key_name = item_info(m.key, ctx)[0]
    sx, sy, sz = m.size
    rep = m.repeat
    rep_note = ""
    if rep:
        rep_note = (f'На схеме {rep.get("display")} повторяемых секций; допустимо от {rep["min"]} до {rep["max"]}.')
    # спецификация
    rows = []
    for b, n, rng in m.bom():
        if b == "minecraft:air":
            rows.append(f'<tr class="air"><td><span class="slot sm air-slot" aria-hidden="true"></span></td>'
                        f'<td>Воздух внутри (клетка должна быть пустой)</td><td class="num">{n}</td><td class="num">—</td></tr>')
            continue
        label = _block_label(b, ctx)
        shown = res.tag_values(b, "block")[0] if b.startswith("#") else b
        role = " (ключевой)" if b == m.key else ""
        rows.append(f'<tr><td>{slot(shown, ctx, extra=" sm")}</td><td>{escape(label)}{role}</td>'
                    f'<td class="num">{n}</td><td class="num">{f"{rng[0]}–{rng[1]}" if rng else n}</td></tr>')
    bom = ('<div class="table-wrap"><table class="bom"><caption class="vh">Материалы</caption><thead><tr><th scope="col"><span class="vh">Иконка</span></th>'
           '<th scope="col">Блок</th><th scope="col" class="num">На схеме</th><th scope="col" class="num">Диапазон</th></tr></thead>'
           f'<tbody>{"".join(rows)}</tbody></table></div>')
    # слои
    def cell_html(c, pos):
        if c is None:
            return '<span class="mcell void"></span>'
        if c.block == "minecraft:air":
            return f'<span class="mcell air" data-tip="Воздух (пусто) · {pos}" tabindex="0"><span class="vh">Воздух</span></span>'
        label = _block_label(c.block, ctx)
        tag = {"key": " · ключевой блок", "repeat": " · повторяемая секция"}.get(c.role, "")
        _, icon = item_info(c.shown, ctx)
        im = img_tag(icon, label, 40) if icon else ""
        return f'<span class="mcell {c.role}" data-tip="{escape(label + tag)} · {pos}" tabindex="0">{im}</span>'

    legend_items = ['<span><i class="lg key"></i>ключевой блок</span>']
    if m.repeat:
        legend_items.append('<span><i class="lg repeat"></i>повторяемая секция</span>')
    if any(c.block == "minecraft:air" for c in m.cells):
        legend_items.append('<span><i class="lg air"></i>воздух</span>')
    xs = range(m.min[0], m.max[0] + 1)
    if sx == 1 and sz == 1 and sy > 1:
        # столб: один вид сбоку вместо стопки слоёв по одной клетке
        by_y = {c.pos[1]: c for c in m.cells}
        grid = "".join(cell_html(by_y.get(y), f"0, {y}, 0") for y in range(m.max[1], m.min[1] - 1, -1))
        layer_ui = (f'<div class="layers"><p class="layer-cap">Вид сбоку, {sy} блоков снизу вверх</p>'
                    f'<div class="mgrid" style="--cols:1">{grid}</div>'
                    f'<p class="legend">{"".join(legend_items)}<span>низ сетки: ключевой блок</span></p></div>')
    else:
        layers = m.layers()
        zs = range(m.max[2], m.min[2] - 1, -1)  # лицевая сторона (−z) внизу
        btns, panes = [], []
        for li, (y, cells) in enumerate(layers):
            grid = "".join(cell_html(cells.get((x, z)), f"{x}, {y}, {z}") for z in zs for x in xs)
            num = li + 1
            btns.append(f'<button type="button" class="lbtn" aria-pressed="{"true" if li == 0 else "false"}" '
                        f'data-layer="{li}" aria-label="Слой {num}">{num}</button>')
            panes.append(f'<div class="layer" data-layer="{li}"{"" if li == 0 else " data-off"}>'
                         f'<p class="layer-cap">Слой {num} из {len(layers)} <small>(y = {y}, вид сверху)</small></p>'
                         f'<div class="mgrid" style="--cols:{len(xs)}">{grid}</div></div>')
        nav = ""
        if len(layers) > 1:
            nav = (f'<div class="layer-nav" role="group" aria-label="Выбор слоя, снизу вверх">'
                   f'<button type="button" class="lbtn step" data-step="-1" aria-label="Слой ниже">−</button>{"".join(btns)}'
                   f'<button type="button" class="lbtn step" data-step="1" aria-label="Слой выше">+</button></div>')
        layer_ui = (f'<div class="layers" data-layers>{nav}{"".join(panes)}'
                    f'<p class="legend">{"".join(legend_items)}<span>низ сетки: лицевая сторона ключевого блока</span></p></div>')
    form = C.MB_FORM.get(spec.get("form", ""), "")
    badges = [f"Ключ: {escape(key_name)}", f"На схеме {sx}×{sy}×{sz}"]
    if spec.get("formula"):
        badges.append(escape(spec["formula"]))
    _, kicon = item_info(m.key, ctx)
    return f'''<article class="mb" id="{m.id}" aria-labelledby="{m.id}-h">
<header class="mb-head">{slot(m.key, ctx, big=True)}<div><h2 id="{m.id}-h">{escape(title)}</h2>
<ul class="badges">{"".join(f"<li>{b}</li>" for b in badges)}</ul></div></header>
<div class="mb-top">
<figure class="mb-iso"><img src="{src}" width="{w}" height="{h}" alt="{escape(title)}: собранная структура в изометрии" loading="lazy" decoding="async">
<figcaption>Собранный вид. {escape(rep_note)}</figcaption></figure>
<div class="mb-text prose">{spec.get("physics", "")}<p class="form-note">{form}</p></div>
</div>
<div class="mb-bottom">
<section aria-label="Материалы"><h3>Материалы</h3>{bom}</section>
<section aria-label="Схема по слоям"><h3>Схема</h3>{layer_ui}</section>
</div>
</article>'''


def multiblocks_page(mbs):
    order = [k for k in C.MULTIBLOCKS if k in mbs] + [k for k in mbs if k not in C.MULTIBLOCKS]
    toc = "".join(f'<li><a href="#{k}">{escape(C.MULTIBLOCKS.get(k, {}).get("title", k))}</a></li>' for k in order)
    h = hero("Датапак-шаблоны", "Мультиблоки",
             "Каждая структура мода построена из обычных блоков по шаблону из датапака. Здесь собранный вид, "
             "список материалов, схема по слоям снизу вверх и физика, по которой структура работает. "
             "Аддоны добавляют свои шаблоны без Java, и они появятся здесь при следующей генерации.")
    return (h + f'<nav class="phase-toc" aria-label="Мультиблоки"><ol>{toc}</ol></nav>'
            + "".join(mb_article(mbs[k]) for k in order))
