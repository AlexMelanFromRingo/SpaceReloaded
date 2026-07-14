#!/usr/bin/env python3
"""Сайт SpaceReloaded для GitHub Pages: руководство (docs/index.html) и книга
рецептов (docs/recipes.html) с пояснениями к каждому предмету. Иконки и
рецепты берутся из реальных ресурсов мода. Запуск: python3 tools/gen_site.py

Оформление «пульт ЦУП»: моноширинные заголовки, приборная палитра из текстур
мода, обе темы. Текст без штампов и длинных тире."""
import base64, json, zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent / "mod/src/main/resources"
RECIPES = ROOT / "data/spacereloaded/recipe"
TEX = ROOT / "assets/spacereloaded/textures"
CLIENT_JAR = Path.home() / ".gradle/caches/fabric-loom/26.2/minecraft-client.jar"
DOCS = Path(__file__).resolve().parent.parent / "docs"

OUR_BLOCK_TEX = {
    "fuel_tank": "fuel_tank_side", "rocket_engine": "rocket_engine_bottom",
    "solar_panel": "solar_panel_top", "battery": "battery_3", "energy_cable": "energy_cable",
    "coal_generator": "coal_generator_front", "rocket_seat": "rocket_seat",
    "assembly_pylon": "assembly_pylon_formed", "electrolyzer": "electrolyzer_front",
    "moon_ice": "moon_ice", "hydrolox_engine": "hydrolox_engine_bottom", "oil_shale": "oil_shale",
    "refinery": "refinery_front", "fueling_pump": "fueling_pump", "crusher": "crusher_front",
    "electric_furnace": "electric_furnace_front", "assembly_table": "assembly_table_top",
    "docking_clamp": "docking_clamp_end", "return_capsule": "return_capsule_side",
    "landing_beacon": "landing_beacon_top", "mission_control": "mission_control_top",
    "cargo_hold": "cargo_hold", "cargo_loader": "cargo_loader_top", "vent_grate": "vent_grate",
    "orbital_cannon": "orbital_cannon_top", "telemetry_screen": "telemetry_screen_sealed",
    "launch_pad": "launch_pad_top_formed", "rtg": "rtg",
    "methalox_engine": "methalox_engine_bottom", "atmospheric_collector": "atmospheric_collector_front",
    "sabatier_reactor": "sabatier_reactor_front", "satellite": "satellite",
    "interceptor_dish": "interceptor_dish_top",
}
VANILLA_TEX = {
    "iron_ingot": "item/iron_ingot", "copper_ingot": "item/copper_ingot", "coal": "item/coal",
    "redstone": "item/redstone", "flint": "item/flint", "raw_iron": "item/raw_iron",
    "glowstone": "block/glowstone", "glowstone_dust": "item/glowstone_dust", "glass": "block/glass",
    "smooth_stone": "block/smooth_stone", "furnace": "block/furnace_front",
    "crafting_table": "block/crafting_table_front", "leather": "item/leather",
    "iron_bars": "block/iron_bars", "ender_pearl": "item/ender_pearl", "paper": "item/paper",
    "chest": "item/chest", "hopper": "item/hopper", "cooked_beef": "item/cooked_beef",
    "gold_ingot": "item/gold_ingot", "ender_eye": "item/ender_eye", "ice": "block/ice",
}
VANILLA_RU = {
    "iron_ingot": "Железный слиток", "copper_ingot": "Медный слиток", "coal": "Уголь",
    "redstone": "Редстоун", "flint": "Кремень", "raw_iron": "Необраб. железо",
    "glowstone": "Светокамень", "glowstone_dust": "Светопыль", "glass": "Стекло",
    "smooth_stone": "Гладкий камень", "furnace": "Печь", "crafting_table": "Верстак",
    "leather": "Кожа", "iron_bars": "Решётка", "ender_pearl": "Жемчуг Края", "paper": "Бумага",
    "chest": "Сундук", "hopper": "Воронка", "cooked_beef": "Жареная говядина",
    "gold_ingot": "Золотой слиток", "ender_eye": "Око Края", "ice": "Лёд",
}

DESC = {
    "assembly_table": "Верстак индустрии: собирает многокомпонентные рецепты (сплавы, блоки баз, детали ракет). Всё дерево прогрессии идёт через него.",
    "crusher": "Дробит руду в пыль с удвоением выхода. Дробить, потом плавить пыль выгоднее, чем плавить руду напрямую.",
    "electric_furnace": "Плавит пыль и шихту в слитки быстрее печи. Работает и на любых ванильных печных рецептах.",
    "coal_generator": "Первый источник энергии: жжёт уголь, пока не построены солнечные панели и РИТЭГи.",
    "iron_dust": "Промежуток дробления. В электропечи плавится в железный слиток.",
    "titanium_dust": "Пыль титановой руды. В печи плавится в титановый слиток для сплава и корпусов.",
    "tungsten_dust": "Пыль вольфрама. В печи плавится в слиток для сопел двигателей и ломов пушки.",
    "coal_dust": "Из угля. Идёт в стальную шихту и в углеволокно.",
    "steel_blend": "Шихта стали: железная пыль плюс угольная. В печи плавится в стальной слиток.",
    "iron_ingot": "База механизмов. Дробление даёт вдвое больше через пыль.",
    "steel_ingot": "Рабочий металл мода: корпуса, кабели, детали. Открывает почти всё.",
    "titanium_ingot": "Лёгкий прочный металл. Сырьё для титанового сплава.",
    "tungsten_ingot": "Тугоплавкий тяжёлый металл: сопла двигателей, боеприпас пушки, РИТЭГ.",
    "titanium_alloy_ingot": "Титан плюс сталь: лёгкий силовой материал для баков, командного модуля, капсулы.",
    "carbon_fiber": "Прессованная угольная пыль: лёгкая обшивка и композит скафандра.",
    "hull_plating": "Герметичная обшивка базы. Держит давление, если полость замкнута без щелей.",
    "hermetic_glass": "Прозрачная герметичная панель: окна баз и куполов без потери давления.",
    "atmosphere_controller": "Сердце базы: заливает воздухом замкнутый объём (26 направлений), тратит энергию. Диагональная щель считается утечкой.",
    "hermetic_hatch": "Дверь шлюза с интерлоком: два люка рядом не откроются разом. Управляется рукой или редстоуном.",
    "vent_grate": "Выглядит цельным блоком, но пропускает газ: вентшахты и фальшполы без разгерметизации.",
    "leak_scanner": "Указывает на пробоину: луч частиц к ближайшей точке утечки плюс координаты. Спасение на большой базе.",
    "telemetry_screen": "Настенная панель: лицо светится зелёным при герметичности и красным при утечке. ПКМ даёт подробный отчёт.",
    "oxygen_mask": "Дыхание в вакууме. Носится в слоте шлема, расходует баллоны.",
    "oxygen_canister": "Запас кислорода для маски. Прочность равна запасу; заряжается в электролизёре.",
    "space_suit_chestplate": "Торс скафандра. Полный сет (торс, штаны, ботинки) защищает от среды сверх дыхания.",
    "space_suit_leggings": "Штаны скафандра. Часть комплекта защиты от холода и радиации на поверхности.",
    "space_suit_boots": "Ботинки скафандра. Замыкают комплект защиты от среды.",
    "canned_ration": "Сытный паёк для дальних миссий. Всегда съедобен, после еды остаётся пустая банка.",
    "empty_can": "Тара для пайков. Возвращается после еды, выбрасывать нечего.",
    "energy_cable": "Соединяет генераторы, хранилища и станки в одну сеть.",
    "battery": "Буфер энергии на 100k. Несколько в сети выравнивают заряд между собой, без бесконечной перекачки.",
    "solar_panel": "Дневная энергия. В вакууме мощнее в 1.5 раза: нет атмосферного ослабления.",
    "rtg": "Слабый, но вечный источник для теневой стороны станций и дальних баз.",
    "gyroscope": "Гиродин: компенсирует крутящий момент, стабилизирует ракету в полёте.",
    "refinery": "Перегоняет нефтеносный сланец в керолокс. Побочно даёт серу.",
    "electrolyzer": "Разлагает лёд на гидролокс и кислород. Основа ISRU на Луне.",
    "fuel_tank": "Бак ракеты. Реально хранит топливо; из него сборка читает заправку.",
    "rocket_engine": "Керолоксовый двигатель: высокая тяга, топливо старта с Земли.",
    "hydrolox_engine": "Гидролоксовый двигатель: высокий удельный импульс, слабее тяга. Для вакуума и Луны.",
    "command_module": "Пост управления и точка сборки ракеты. Место пилота.",
    "rocket_seat": "Кресло для пассажиров сверх пилота.",
    "rocket_hull": "Лёгкая силовая обшивка ракеты.",
    "launch_pad": "Плита стартовой площадки. Прямоугольник плит задаёт площадь сборки.",
    "assembly_pylon": "Колонна у площадки: задаёт высоту ракеты. ПКМ даёт скан-отчёт, Sneak+ПКМ собирает.",
    "fueling_hose": "Переносная магистраль бак-ракета: закачка и слив топлива вручную.",
    "fueling_pump": "Заправочная колонна: сама качает топливо в припаркованную ракету в зоне действия.",
    "docking_clamp": "Плоскость разделения ступеней: расстыковка носителя и лендера, обратная стыковка захватом.",
    "heat_shield": "Абляционный экран из метеоритного железа. Метеоритное железо есть только в поясе астероидов, поэтому возвратная капсула это уже поздняя игра.",
    "return_capsule": "Титановый спасательный пост с теплозащитным экраном: держит посадку до 25 м/с.",
    "landing_beacon": "Точка прибытия полётной программы для беспилотных рейсов.",
    "mission_control": "Пульт ЦУП: телеметрия всех бортов в радиусе (статус, топливо, высота).",
    "flight_program": "Носитель маршрута: цель плюс посадочный маяк. Загружается в борт для автопилота.",
    "cargo_hold": "Грузовой отсек ракеты на 15 слотов. Хопперы и погрузчик работают с ним напрямую.",
    "cargo_loader": "Автопогрузка и разгрузка припаркованного борта из соседних контейнеров.",
    "orbital_cannon": "Орбитальное кинетическое орудие. Работает только на орбите, бьёт вольфрамовым ломом.",
    "tungsten_rod": "Боеприпас пушки. Кратер считается из кинетической энергии удара.",
    "targeting_designator": "Пульт наведения: метит цель на поверхности, привязывается к пушке, стреляет дистанционно.",
    "meteoric_iron": "Падает в кратере метеорита на Луне. Дробится в тройную порцию железной пыли.",
    "methalox_engine": "Метанокислородный двигатель: середина между керолоксом и гидролоксом. Топливо добывается на Марсе.",
    "atmospheric_collector": "Сжимает CO2 из атмосферы измерения. Быстро на Марсе, медленно на Земле, никак в вакууме.",
    "sabatier_reactor": "Реакция Сабатье: CO2 из сборщика плюс лёд дают метанокс в соседние баки. ISRU-топливо Марса.",
    "power_satellite": "Энергоспутник: разворачивается на орбите и питает наземные ректенны при чистом небе.",
    "rectenna": "Ректенна: принимает энергию с орбитальных энергоспутников. Пассивная мощность независимо от дня.",
    "satellite": "Полезная нагрузка ракеты. На орбите разворачивается в узел связи, покрытие нужно для беспилотных межпланетных рейсов.",
    "interceptor_dish": "Тарелка-перехватчик: уводит груз с открытого (незашифрованного) канала на свою площадку. Основа PvP-логистики.",
    "frequency_key": "Ключ связи: прошивается в ЦУПе, шифрует маяк своим каналом и настраивает перехватчик. Защита от угона груза.",
}

PHASE = {
    "assembly_table": 0, "crusher": 0, "electric_furnace": 0, "coal_generator": 0,
    "iron_dust": 1, "titanium_dust": 1, "tungsten_dust": 1, "coal_dust": 1, "steel_blend": 1,
    "iron_ingot": 1, "steel_ingot": 1, "titanium_ingot": 1, "tungsten_ingot": 1,
    "titanium_alloy_ingot": 1, "carbon_fiber": 1, "meteoric_iron": 1,
    "energy_cable": 2, "battery": 2, "solar_panel": 2, "rtg": 2,
    "hull_plating": 3, "hermetic_glass": 3, "atmosphere_controller": 3, "hermetic_hatch": 3,
    "vent_grate": 3, "leak_scanner": 3, "telemetry_screen": 3,
    "oxygen_mask": 3, "oxygen_canister": 3, "space_suit_chestplate": 3, "space_suit_leggings": 3,
    "space_suit_boots": 3, "canned_ration": 3, "empty_can": 3,
    "refinery": 4, "fuel_tank": 4, "fueling_hose": 4, "fueling_pump": 4,
    "rocket_engine": 5, "hydrolox_engine": 5, "command_module": 5, "rocket_seat": 5,
    "rocket_hull": 5, "gyroscope": 5, "launch_pad": 5, "assembly_pylon": 5,
    "electrolyzer": 7,
    "docking_clamp": 8, "return_capsule": 8, "landing_beacon": 8, "mission_control": 8,
    "flight_program": 8, "cargo_hold": 8, "cargo_loader": 8,
    "orbital_cannon": 9, "tungsten_rod": 9, "targeting_designator": 9,
    "methalox_engine": 10, "atmospheric_collector": 10, "sabatier_reactor": 10,
    "satellite": 11, "interceptor_dish": 11, "frequency_key": 11,
    "power_satellite": 11, "rectenna": 11,
}
PHASES = [
    ("Фаза 0. Первые станки", "Из железа и меди в верстаке собираются четыре станка. С них начинается вся промышленность."),
    ("Фаза 1. Металлы", "Руда в дробилку, пыль в электропечь. Сталь открывает энергетику, герметичность и ракеты."),
    ("Фаза 2. Энергосеть", "Кабели связывают генераторы и потребителей. Батареи буферизуют, РИТЭГ питает теневую сторону."),
    ("Фаза 3. Жизнеобеспечение", "Замкнутый объём под давлением, скафандр, приборы контроля утечек и провиант."),
    ("Фаза 4. Топливо Земли", "Перегонка сланца в керолокс, баки и заправка ракеты."),
    ("Фаза 5. Первая ракета", "Двигатели, баки, командный модуль, стартовый комплекс с площадкой и пилоном."),
    ("Фаза 6. Орбита", ""),
    ("Фаза 7. Луна и ISRU", "Электролиз льда в гидролокс и кислород: топливо для возврата добывается на месте."),
    ("Фаза 8. Логистика", "Стыковка ступеней, беспилотные рейсы, грузовые перевозки, возврат домой."),
    ("Фаза 9. Орбитальное орудие", "Вольфрамовый лом и кинетический удар по честной баллистике входа."),
    ("Фаза 10. Марс", "Красная планета: тонкая CO2-атмосфера, метанокс из сборщика и реактора Сабатье, перелёт в окне Гомана."),
    ("Фаза 11. Орбитальные сети", "Спутниковое покрытие для беспилотной логистики и защищённые каналы связи: шифруй маяки или теряй груз."),
]

lang = json.load(open(ROOT / "assets/spacereloaded/lang/ru_ru.json"))
client = zipfile.ZipFile(CLIENT_JAR)
_cache = {}

def data_uri(pb):
    raw = pb if isinstance(pb, bytes) else open(pb, "rb").read()
    return "data:image/png;base64," + base64.b64encode(raw).decode()

def icon(item_id):
    if item_id in _cache:
        return _cache[item_id]
    ns, _, name = item_id.partition(":")
    uri = None
    if ns == "spacereloaded":
        p = TEX / "item" / f"{name}.png"
        if not p.exists():
            p = TEX / "block" / f"{OUR_BLOCK_TEX.get(name, name)}.png"
        if p.exists():
            uri = data_uri(p)
    else:
        tex = VANILLA_TEX.get(name)
        if tex:
            try:
                uri = data_uri(client.read(f"assets/minecraft/textures/{tex}.png"))
            except KeyError:
                uri = None
    _cache[item_id] = uri
    return uri

def ru_name(item_id):
    ns, _, name = item_id.partition(":")
    if ns == "spacereloaded":
        return lang.get(f"item.spacereloaded.{name}") or lang.get(f"block.spacereloaded.{name}") or name
    return VANILLA_RU.get(name, name)

def item_html(item_id, count=1, big=False):
    uri = icon(item_id)
    cnt = f'<span class="cnt">{count}</span>' if count > 1 else ""
    img = f'<img src="{uri}" alt="" loading="lazy">' if uri else '<span class="noicon">?</span>'
    return f'<span class="item{" big" if big else ""}" title="{ru_name(item_id)}">{img}{cnt}</span>'

def short(item_id):
    return item_id.partition(":")[2]

recipes = {}
for f in sorted(RECIPES.glob("*.json")):
    r = json.load(open(f))
    t = r["type"]
    res = r["result"]["id"]
    rcount = r["result"].get("count", 1)
    if t in ("spacereloaded:crushing", "spacereloaded:electric_smelting"):
        io = f'{item_html(r["ingredient"])}<span class="arr">&rarr;</span>{item_html(res, rcount, True)}'
        kind = "Дробление" if t.endswith("crushing") else "Плавка"
    elif t == "spacereloaded:assembly":
        counted, order = {}, []
        for ing in r["ingredients"]:
            k = ing if isinstance(ing, str) else json.dumps(ing)
            if k not in counted:
                counted[k] = 0; order.append(k)
            counted[k] += 1
        ins = "".join(item_html(k, counted[k]) for k in order)
        io = f'<span class="ins">{ins}</span><span class="arr">&rarr;</span>{item_html(res, rcount, True)}'
        kind = "Сборочный стол"
    elif t == "minecraft:crafting_shaped":
        key = r["key"]; cells = []
        for row in r["pattern"]:
            for ch in row.ljust(3):
                if ch == " " or ch not in key:
                    cells.append('<span class="cell empty"></span>')
                else:
                    cells.append(f'<span class="cell">{item_html(key[ch])}</span>')
        io = f'<span class="grid3">{"".join(cells)}</span><span class="arr">&rarr;</span>{item_html(res, rcount, True)}'
        kind = "Верстак"
    elif t == "minecraft:crafting_shapeless":
        ins = "".join(item_html(i) for i in r["ingredients"])
        io = f'<span class="ins">{ins}</span><span class="arr">&rarr;</span>{item_html(res, rcount, True)}'
        kind = "Верстак"
    else:
        continue
    recipes[short(res)] = (kind, io)

def card(name):
    kind, io = recipes[name]
    desc = DESC.get(name, "")
    return (f'<article class="card"><div class="io">{io}</div>'
            f'<div class="cap"><span class="nm">{ru_name("spacereloaded:" + name)}</span>'
            f'<span class="kind">{kind}</span></div>'
            f'<p class="desc">{desc}</p></article>')

recipe_sections = ""
for pidx, (ptitle, psub) in enumerate(PHASES):
    names = [n for n in recipes if PHASE.get(n) == pidx]
    if not names:
        continue
    order = list(DESC.keys())
    names.sort(key=lambda n: order.index(n) if n in order else 999)
    cards = "".join(card(n) for n in names)
    recipe_sections += (f'<section class="phase"><h2>{ptitle}</h2>'
                        + (f'<p class="lead">{psub}</p>' if psub else "")
                        + f'<div class="cards">{cards}</div></section>')

CSS = """
:root{--bg:#0c1216;--panel:#141d23;--panel2:#101820;--line:#243039;--ink:#dbe3e8;--dim:#8795a0;--accent:#6fd5e8;--amber:#e0b23c;--slot:#0c1418;--slot-line:#2a3742;}
@media (prefers-color-scheme:light){:root{--bg:#e7e9ea;--panel:#f6f8f9;--panel2:#eef1f3;--line:#d0d7dc;--ink:#1a2228;--dim:#5a6670;--accent:#137a90;--slot:#e4e9ec;--slot-line:#c6ced4;}}
:root[data-theme="dark"]{--bg:#0c1216;--panel:#141d23;--panel2:#101820;--line:#243039;--ink:#dbe3e8;--dim:#8795a0;--accent:#6fd5e8;--slot:#0c1418;--slot-line:#2a3742;}
:root[data-theme="light"]{--bg:#e7e9ea;--panel:#f6f8f9;--panel2:#eef1f3;--line:#d0d7dc;--ink:#1a2228;--dim:#5a6670;--accent:#137a90;--slot:#e4e9ec;--slot-line:#c6ced4;}
*{box-sizing:border-box}
body{margin:0;background:var(--bg);color:var(--ink);font:16px/1.65 system-ui,"Segoe UI",Roboto,sans-serif;-webkit-font-smoothing:antialiased}
.mono{font-family:ui-monospace,"Cascadia Code","JetBrains Mono",Menlo,Consolas,monospace}
a{color:var(--accent);text-decoration:none}
a:hover{text-decoration:underline}
.wrap{max-width:1080px;margin:0 auto;padding:0 20px}
.top{position:sticky;top:0;z-index:9;background:var(--panel2);border-bottom:1px solid var(--line);backdrop-filter:blur(6px)}
.top .wrap{display:flex;align-items:center;gap:20px;height:52px}
.top .brand{font:700 15px/1 ui-monospace,monospace;letter-spacing:.12em;color:var(--ink)}
.top .brand b{color:var(--accent)}
.top nav{display:flex;gap:18px;margin-left:auto;font-size:14px;flex-wrap:wrap}
.top nav a{color:var(--dim);letter-spacing:.02em}
.top nav a:hover{color:var(--ink);text-decoration:none}
.hero{border:1px solid var(--line);background:linear-gradient(180deg,var(--panel),var(--panel2));margin:28px 0 8px;padding:30px 30px 26px;position:relative;overflow:hidden}
.hero::before{content:"";position:absolute;inset:0;pointer-events:none;background-image:linear-gradient(var(--line) 1px,transparent 1px),linear-gradient(90deg,var(--line) 1px,transparent 1px);background-size:26px 26px;opacity:.14}
.hero .eyebrow{font:600 12px/1 ui-monospace,monospace;letter-spacing:.28em;color:var(--accent);text-transform:uppercase;margin:0 0 12px;position:relative}
.hero h1{margin:0 0 10px;font:700 34px/1.05 ui-monospace,monospace;letter-spacing:.01em;text-wrap:balance;position:relative}
.hero p{margin:0;max-width:64ch;color:var(--ink);position:relative}
.hazard{height:6px;margin-top:20px;position:relative;background:repeating-linear-gradient(-45deg,var(--amber) 0 12px,#1a1a1c 12px 24px);opacity:.8}
.tags{display:flex;flex-wrap:wrap;gap:8px;margin:18px 0 4px;position:relative}
.tags span{font:600 11.5px/1 ui-monospace,monospace;letter-spacing:.06em;border:1px solid var(--line);color:var(--dim);padding:5px 10px;text-transform:uppercase}
h2{font:600 21px/1.2 ui-monospace,monospace;letter-spacing:.02em;margin:0 0 4px}
.phase>h2,.guide h2{border-left:4px solid var(--accent);padding-left:12px}
.lead{color:var(--dim);margin:2px 0 16px;padding-left:16px}
.guide{margin:34px 0}
.guide section{margin:26px 0;border:1px solid var(--line);background:var(--panel);padding:20px 22px}
.guide h3{margin:0 0 8px;font:600 16px/1.3 ui-monospace,monospace;letter-spacing:.03em}
.guide p{margin:8px 0}
.kbd{font:600 12.5px/1 ui-monospace,monospace;border:1px solid var(--slot-line);background:var(--slot);padding:2px 7px;color:var(--ink);white-space:nowrap}
table{width:100%;border-collapse:collapse;margin:12px 0;font-size:14.5px}
th,td{text-align:left;padding:7px 10px;border-bottom:1px solid var(--line)}
th{color:var(--dim);font:600 12px/1 ui-monospace,monospace;letter-spacing:.08em;text-transform:uppercase}
.phase{margin:34px 0}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(300px,1fr));gap:12px;margin-top:14px}
.card{border:1px solid var(--line);background:var(--panel);padding:14px 14px 12px;display:flex;flex-direction:column;gap:10px}
.io{display:flex;align-items:center;gap:10px;flex-wrap:wrap;min-height:48px}
.ins{display:flex;gap:4px;flex-wrap:wrap;max-width:190px}
.item{position:relative;width:38px;height:38px;background:var(--slot);border:1px solid var(--slot-line);display:inline-flex;align-items:center;justify-content:center;flex:none}
.item img{width:30px;height:30px;image-rendering:pixelated}
.item.big{width:46px;height:46px;border-color:var(--accent)}
.item.big img{width:36px;height:36px}
.cnt{position:absolute;right:1px;bottom:0;font:700 12px ui-monospace,monospace;color:#fff;text-shadow:1px 1px 0 #000,-1px 1px 0 #000,1px -1px 0 #000,-1px -1px 0 #000}
.arr{color:var(--dim);font-size:20px;margin-left:auto;flex:none}
.grid3{display:grid;grid-template-columns:repeat(3,34px);gap:2px}
.cell{width:34px;height:34px;background:var(--slot);border:1px solid var(--slot-line);display:flex;align-items:center;justify-content:center}
.cell .item{width:32px;height:32px;border:none;background:none}
.cell .item img{width:26px;height:26px}
.cell.empty{opacity:.3}
.cap{display:flex;align-items:baseline;gap:8px;border-top:1px solid var(--line);padding-top:9px}
.cap .nm{font-weight:600}
.cap .kind{margin-left:auto;font:600 10.5px/1 ui-monospace,monospace;letter-spacing:.06em;color:var(--dim);text-transform:uppercase;white-space:nowrap}
.desc{margin:0;color:var(--dim);font-size:13.5px;line-height:1.5}
.noicon{color:var(--dim)}
footer{border-top:1px solid var(--line);margin-top:48px;padding:24px 0 40px;color:var(--dim);font-size:13px}
footer a{color:var(--dim);text-decoration:underline}
"""

def top(active):
    def a(href, label, key):
        cur = ' style="color:var(--ink)"' if key == active else ""
        return f'<a href="{href}"{cur}>{label}</a>'
    return (f'<div class="top"><div class="wrap"><span class="brand mono">SPACE<b>RELOADED</b></span>'
            f'<nav>{a("index.html","Руководство","guide")}{a("recipes.html","Рецепты","recipes")}'
            f'<a href="https://github.com/AlexMelanFromRingo/SpaceReloaded/blob/main/docs/ADDONS.md">Аддоны</a>'
            f'<a href="https://github.com/AlexMelanFromRingo/SpaceReloaded">GitHub</a></nav></div></div>')

def page(title, active, body):
    return (f'<!doctype html><html lang="ru"><head><meta charset="utf-8">'
            f'<meta name="viewport" content="width=device-width,initial-scale=1">'
            f'<title>{title}</title><style>{CSS}</style></head><body>'
            f'{top(active)}<div class="wrap">{body}</div>'
            f'<footer><div class="wrap">SpaceReloaded для Minecraft 26.2 (Fabric). '
            f'Иконки и рецепты собраны из ресурсов мода. '
            f'<a href="https://github.com/AlexMelanFromRingo/SpaceReloaded">Исходники и сборка на GitHub</a>.'
            f'</div></footer></body></html>')

guide_body = f'''
<div class="hero">
  <p class="eyebrow">Космос на честной физике</p>
  <h1>Руководство командира</h1>
  <p>Ракета собирается из блоков, которые вы поставили сами, и летит по формуле Циолковского.
  Тяговооружённость считается по реальной массе деталей, кривая ракета заваливается на взлёте,
  а диагональная щель в обшивке базы травит воздух в вакуум. Здесь описан весь путь
  от первого слитка железа до замкнутой межпланетной логистики.</p>
  <div class="tags"><span>Minecraft 26.2</span><span>Fabric</span><span>Циолковский · TWR</span>
  <span>Герметичность 26 направлений</span><span>ISRU</span><span>Стыковка</span><span>Орбитальный удар</span><span>Jade</span><span>Топливо-жидкость</span></div>
  <div class="hazard"></div>
</div>

<div class="guide">
<section>
<h3>С чего начать</h3>
<p>В обычном верстаке собираются четыре станка: <b>сборочный стол</b> (сердце всей
промышленности), <b>дробилка</b>, <b>электропечь</b> и <b>угольный генератор</b>. Дальше почти
всё делается на сборочном столе.</p>
<p>Руду выгоднее сначала дробить, а потом плавить пыль: дробилка даёт двойной выход.
Сталь получается так: железная пыль плюс две угольные дают <b>стальную шихту</b>, шихта
в электропечи становится стальным слитком. Сталь открывает энергетику, герметичность
и ракеты. Титан и вольфрам добываются в глубинах, углепластик прессуется из угольной пыли.</p>
</section>

<section>
<h3>Энергия и кабели</h3>
<p>Все станки питаются от общей кабельной сети. Кабель соединяется с любым блоком, который
хранит или потребляет энергию: генераторы, аккумуляторы, дробилка, печь, стол, электролизёр,
перегонный куб, атмосферный контроллер, орудие, химические машины и ректенна. Достаточно
поставить кабель вплотную.</p>
<p>Кабель герметичен: его можно провести сквозь обшивку купола, воздух через ввод не уходит.
Панели и РИТЭГи выносятся наружу, а энергия заводится внутрь напрямую.</p>
<p>Несколько аккумуляторов в одной сети выравнивают заряд между собой и не гоняют энергию
по кругу. Источники: угольный генератор на старте, солнечные панели (в вакууме мощнее в
полтора раза), <b>РИТЭГ</b> для теневой стороны и бурь, позже <b>ректенна</b> от орбитальных
энергоспутников.</p>
</section>

<section>
<h3>Герметичность базы</h3>
<p>Объём проверяется заливкой в <b>26 направлениях</b>: диагональная щель в углу это утечка,
как недостроенная рамка портала. Это осознанное правило, а не баг. Кому такая строгость
мешает строить, в конфиге есть <code>sealingDiagonalLeaks: false</code>: заливка перейдёт на
классические 6 направлений, и угловой блок больше не понадобится.</p>
<p>Постройте замкнутую полость из герметичной обшивки и стекла, поставьте внутрь
<b>контроллер атмосферы</b> и подведите к нему энергию. Зона наддувается. Пробоина вызывает
декомпрессию, которая тянет к дыре.</p>
<ul>
<li><b>Сканер утечек</b>: ПКМ по контроллеру или в воздух. Покажет статус ближайшей зоны,
а при утечке нарисует луч частиц прямо к пробоине и напишет координаты.</li>
<li><b>Экран телеметрии</b>: настенная панель, зелёная при герметичности и красная при утечке.
ПКМ даёт подробный отчёт.</li>
<li><b>Вентиляционная решётка</b> выглядит цельной, но пропускает газ: шахты и фальшполы
без разгерметизации соседних отсеков.</li>
<li>На горячих и холодных телах контроллер тратит больше энергии: климат-контроль нагружен.</li>
</ul>
</section>

<section>
<h3>Шлюзы</h3>
<p>Гермолюк открывается по ПКМ через цикл выравнивания давления. Два люка рядом не откроются
одновременно: это интерлок, он и делает из тамбура настоящий шлюз. Смежные люки образуют одну
дверь (до четырёх блоков), открываются и закрываются вместе, а открытый проём становится
полностью проходимым.</p>
<p>Шлюз можно автоматизировать редстоуном: сигнал открывает дверь через тот же цикл с
интерлоком, снятие сигнала мгновенно закрывает. Открытия по нику или по приближению нет
намеренно: иначе чужой мог бы пройти следом за владельцем.</p>
</section>

<section>
<h3>Кислород и скафандр</h3>
<p>Маска в слоте шлема даёт дыхание, баллоны в инвентаре хранят кислород. Прочность баллона
и есть запас; заряжаются баллоны в слоте электролизёра. При надетой маске справа сверху
виден суммарный запас.</p>
<p>Одной маски мало. Без полного скафандра (торс, штаны, ботинки) среда на поверхности бьёт
холодом и радиацией. В открытом вакууме звук глохнет: слышно только себя через скафандр.
В творческом режиме кислород не расходуется.</p>
</section>

<section>
<h3>Топливо: три пары</h3>
<ul>
<li><b>Керолокс</b>: нефтеносный сланец в перегонный куб. Плотный и тяговитый, топливо старта
с Земли. Побочно даёт серу.</li>
<li><b>Гидролокс</b>: лёд в электролизёр. Высокий удельный импульс, слабее тяга, идеален
в вакууме. Побочно заряжает кислородный баллон.</li>
<li><b>Метанокс</b>: атмосферный сборщик берёт CO2 из воздуха, реактор Сабатье соединяет его
со льдом. Добывается прямо на Марсе.</li>
</ul>
<p>Двигатель жжёт только свой тип топлива, смешанный стек не соберётся.</p>
<p><b>Как достать топливо из машины.</b> Перегонный куб, электролизёр и реактор Сабатье не
выдают топливо предметом: они <b>перекачивают его в соседние баки</b>. Поставьте
<b>топливный бак</b> вплотную к машине, и он начнёт наполняться. Дальше два пути в ракету:</p>
<ol>
<li><b>Заправочный рукав</b>: ПКМ по баку связывает его с рукавом, затем ПКМ по ракете качает,
Sneak+ПКМ сливает обратно.</li>
<li><b>Заправочная колонна</b> у площадки: ПКМ переключает режим (заправка, слив, выкл).
Дальше она сама обслуживает припаркованный борт в зоне над собой.</li>
</ol>
<p><b>Топливо это жидкость.</b> У каждой пары есть ведро: ПКМ по баку пустым ведром наливает,
полным ведром выливает обратно. Ведро керолокса весит 24 кг, метанокса 20 кг, гидролокса 12 кг:
водород объёмный и лёгкий, поэтому его ведро несёт меньше массы. Бак виден трубам и насосам
соседних модов через Fabric Transfer API, так что топливо можно гнать и чужой логистикой.
Смешивать пары нельзя ни ведром, ни трубой.</p>
</section>

<section>
<h3>Стартовый комплекс и сборка</h3>
<p>Площадка это прямоугольник плит не меньше 3&times;3. Пилон это колонна от трёх блоков на краю
площадки; её высота ограничивает высоту ракеты. Когда площадка и пилон собраны верно,
конструкция подсвечивается жёлтой разметкой.</p>
<p>Стройте ракету над площадкой: двигатель снизу, баки, командный модуль или кресло. Все
детали должны касаться друг друга и быть связаны с командным модулем.</p>
<p><span class="kbd">ПКМ</span> по пилону даёт <b>скан-отчёт без сборки</b>: масса, тяга, TWR,
запас скорости и вердикт, хватит ли до перехода. <span class="kbd">Sneak+ПКМ</span> по пилону
поднимает блоки в единую сущность. Топливо берётся из реально залитых баков.</p>
<p>Физика честная: TWR меньше единицы не оторвёт ракету от земли, несимметричная тяга даст
крутящий момент и опрокинет её. Гиродин компенсирует момент и стабилизирует полёт.</p>
</section>

<section>
<h3>Экраны вместо чата</h3>
<p><b>Скан-отчёт</b> (ПКМ по пилону) открывается панелью: масса, тяга, тяговооружённость,
запас скорости и вердикт цветом. <b>Терминал орудия</b> (ПКМ пустой рукой по орудию) показывает
ломы, заряд, цель и перезарядку, а пуск идёт кнопкой: случайный клик по пушке больше не отправляет
лом на планету. <b>Карта полёта</b> открывается клавишей <span class="kbd">M</span> где угодно и
по <span class="kbd">Sneak+ПКМ</span> на ЦУП: схема строится из графа переходов, тела расставлены
по числу прыжков от Земли. Клик по телу выбирает его, кнопка назначает целью, если вы в кресле.</p>
<p>С установленным <b>Jade</b> наведение на блок показывает энергию, топливо в баке и заряд орудия
без единого клика.</p>
</section>

<section>
<h3>Полёт и выбор цели</h3>
<table><tr><th>Действие</th><th>Ввод</th></tr>
<tr><td>Сесть в ракету</td><td><span class="kbd">ПКМ</span> по корпусу</td></tr>
<tr><td>Тяга</td><td><span class="kbd">Прыжок</span> удерживать</td></tr>
<tr><td>Сменить цель перелёта</td><td><span class="kbd">Спринт</span> на стоянке</td></tr>
<tr><td>Карта полёта</td><td><span class="kbd">M</span></td></tr>
<tr><td>Разобрать ракету</td><td><span class="kbd">Sneak+ПКМ</span> по корпусу</td></tr>
<tr><td>Стыковка и расстыковка</td><td><span class="kbd">Sneak+ПКМ</span> по узлу</td></tr></table>
<p>Слева на экране идёт топливо, скорость, высота, тангаж, крен и цель.</p>
<p><b>Целью можно назначить любую планету, а не только соседнюю.</b> Маршрут считается сам:
если с Земли выбрать Марс, борт сначала уйдёт на орбиту, а оттуда дальше по маршруту.
На промежуточной остановке ракета паркуется, цель сохраняется, достаточно снова дать тягу.
Если пути нет, подсказка так и скажет.</p>
<p>Посадка мягче 15 м/с сохраняет аппарат; с возвратной капсулой в стеке предел 25 м/с.
Кончилось топливо: дальше только честная баллистика.</p>
</section>

<section>
<h3>Окна перелёта</h3>
<p>К Марсу нельзя лететь когда вздумается. Взаимное положение планет открывает <b>окно Гомана</b>
периодически. Выберите цель Марс: подсказка покажет, открыто окно или сколько ждать. Вне окна
ракета не уйдёт в перелёт, сколько бы топлива вы ни залили. У Луны и орбиты окон нет.</p>
</section>

<section>
<h3>Стыковка и Lander</h3>
<p>Стыковочный узел в стеке ракеты задаёт плоскость разделения. Соберите двухступенчатую
ракету: снизу лендер (двигатель, бак, кресло), затем узел, выше носитель.</p>
<p>На орбите <span class="kbd">Sneak+ПКМ</span> по узлу расстыковывает их, топливо делится по
ёмкости баков. Лендер уходит вниз, заправляется через ISRU и возвращается. Припаркуйте его
под узлом носителя (захват в радиусе трёх блоков) и повторный <span class="kbd">Sneak+ПКМ</span>
сливает структуры и суммирует топливо. Лендер без двигателя или поста управления не отделится.</p>
</section>

<section>
<h3>Беспилотные рейсы и грузы</h3>
<p><b>Полётная программа</b> хранит цель и посадочный маяк: <span class="kbd">Sneak+ПКМ</span>
в воздух циклит цель, <span class="kbd">ПКМ</span> по маяку пишет точку прибытия,
<span class="kbd">ПКМ</span> по припаркованной ракете загружает маршрут в борт.</p>
<p>Пультом наведения <span class="kbd">Sneak+ПКМ</span> по припаркованной ракете запускает
беспилотный рейс. Автопилот наберёт высоту, выполнит переход и сядет на маяк тормозным
импульсом. Экипаж на борту всегда перекрывает автопилот.</p>
<p><b>Грузовой отсек</b> в стеке ракеты держит 15 слотов и работает с хопперами со всех сторон.
<b>Погрузчик</b> у площадки грузит и разгружает припаркованный борт из соседних контейнеров.
Связка «посадочный маяк, заправочная колонна, погрузчик» и есть докинг-пэд: борт садится,
заправляется и грузится без вашего участия.</p>
</section>

<section>
<h3>Спутники, покрытие, защита каналов</h3>
<p><b>Спутник</b> это полезная нагрузка ракеты. На орбите он разворачивается в узел связи:
аппарат гибнет, покрытие измерения растёт. Разворот идёт только у беспилотного борта, экипаж
и груз не потеряются.</p>
<p>Беспилотный межпланетный рейс к Марсу требует такого покрытия на орбите вылета: наземный
ЦУП командует грузовиком через ретранслятор. Пилотируемый рейс покрытия не требует.</p>
<p>Маршрутизация грузов защищаемая. <b>Ключ связи</b> прошивается в ЦУПе и шифрует посадочный
маяк своим каналом. Доставку к защищённому маяку проходит только программа, отмеченная при
наличии этого ключа. Открытый маяк доставляет любому, но его канал может перехватить чужая
<b>тарелка-перехватчик</b> и увести груз на свою площадку. Шифруйте свои маяки.</p>
</section>

<section>
<h3>Орбитальное орудие</h3>
<p>Орудие работает только на орбите, питается от кабельной сети и бьёт вольфрамовым ломом.
Целеуказателем <span class="kbd">ПКМ</span> метит цель на поверхности, <span class="kbd">ПКМ</span>
по орудию наводит, <span class="kbd">ПКМ</span> пустой рукой стреляет.
<span class="kbd">Sneak+ПКМ</span> целеуказателем по орудию привязывает пульт: после этого цель
и выстрел управляются дистанционно из любого измерения.</p>
<p>Лом весит две тонны и входит в атмосферу на 1500 м/с: это около 2.2 ГДж, примерно
полтонны тротила. Кратер выходит под 26 блоков в поперечнике, а перезарядка занимает
десять секунд. Оружие стратегическое, не скорострельное.</p>
<p>Орудие бьёт <b>только по поверхности другого измерения</b>: стрелять по своей орбите нельзя,
станция не расстреляет сама себя. Удар идёт по честной баллистике входа, радиус кратера
считается из кинетической энергии. Обсидиан и вода держат удар, бедрок неуязвим. Внизу за
несколько секунд до прилёта слышен гром и видна огненная колонна.</p>
</section>

<section>
<h3>Опасности: метеориты, бури, тепло</h3>
<p>На безатмосферных поверхностях периодически падают <b>метеориты</b>: свист-предупреждение,
затем кратер. Защита та же, что от орбитального удара: обшивка и обсидиан держат, а слой
реголита над базой поглощает воронку. В кратере остаётся метеоритное железо, богатый источник
железной пыли. На орбитальную станцию метеориты не падают.</p>
<p>На Марсе случаются <b>пылевые бури</b>: солнечные панели теряют почти всю выработку,
абразивная пыль точит любого вне герметичной зоны. Держите РИТЭГи и стройте закрытые ангары,
иначе буря обесточит жизнеобеспечение.</p>
<p><b>Тепло</b> не бьёт по игроку напрямую, но нагружает базу: на горячем или холодном теле
контроллер атмосферы тратит больше энергии. Луна без атмосферы качается от жара к морозу.</p>
</section>

<section>
<h3>Марс, метанокс, астероиды</h3>
<p>У Марса тонкая углекислотная атмосфера и гравитация 0.38g. Дышать нельзя, но CO2 из воздуха
собирается: <b>атмосферный сборщик</b> сжимает его в баллоны (на Марсе быстро, на Земле во много
раз медленнее, в вакууме никак). <b>Реактор Сабатье</b> соединяет CO2 со льдом и льёт метанокс
в соседние баки. Топливо для возврата производится на месте.</p>
<p><b>У каждого тела своя добыча.</b> На Земле титана и вольфрама хватает ровно на первую ракету
и скафандр, дальше жилы кончаются. Основной титан лежит в лунном ильмените, вольфрам ушёл в
глубокие марсианские жилы, а пояс астероидов остался единственным источником метеоритного железа,
из которого куётся теплозащитный экран возвратной капсулы. Летать стало зачем.</p>
<p>Рельеф у каждого тела свой. Луна изрыта ударными кратерами с валами выброса, между ними
лежат пологие моря и нагорья. Марс сложен столовыми горами с обрывами, дюнами и красным
песком поверх песчаника. Пояс астероидов это глыбы в пустоте, а не сплошная плита.</p>
<p><b>Пояс астероидов</b> достижим с орбиты Земли: почти невесомость, а порода при добыче даёт
титан, вольфрам, метеоритное железо и лёд.</p>
</section>

<section>
<h3>Возврат домой</h3>
<p>Ракета не превращается в предмет в инвентаре. Домой возвращаются тремя путями: заправиться
на месте через ISRU, состыковаться с носителем на орбите или построить титановую возвратную
капсулу с теплозащитой. Межпространственные рейсы держат чанки авто-протухающими билетами:
полёт завершится даже без игроков рядом и переживёт перезапуск сервера.</p>
</section>

<section>
<h3>Если что-то не работает</h3>
<ul>
<li><b>Кабель не подключается к машине.</b> Проверьте, что блок действительно потребляет энергию,
и что кабель стоит вплотную. Все станки мода подключаемы.</li>
<li><b>Из перегонного куба не достать топливо.</b> Оно не выдаётся предметом: поставьте топливный
бак вплотную к машине, топливо перетечёт само. Из бака его можно набрать ведром.</li>
<li><b>Ракета не взлетает.</b> ПКМ по пилону и смотрите скан-отчёт: скорее всего TWR меньше единицы
(мало тяги или много массы) или в баках не то топливо, что жжёт двигатель.</li>
<li><b>Ракета опрокидывается.</b> Тяга несимметрична относительно центра масс. Двигайте двигатели
к оси или ставьте гиродин.</li>
<li><b>Орудие отказывается стрелять.</b> Оно работает только на орбите, только по поверхности
другого измерения, требует лом, энергию и выдерживает паузу между выстрелами.</li>
<li><b>Тяжело строить из-за углов.</b> Так задумано: газ уходит через диагональную щель.
Если не нравится, поставьте <code>sealingDiagonalLeaks: false</code> в конфиге.</li>
<li><b>Кабель ломает герметичность.</b> Не ломает: кабель герметичен и проходит сквозь стену.
Если зона всё же течёт, ищите пробой сканером утечек.</li>
<li><b>Перелёт не начинается.</b> Либо закрыто окно Гомана, либо у беспилотного рейса нет
спутникового покрытия, либо до цели нет маршрута.</li>
</ul>
</section>
</div>

<p style="margin:28px 0 8px"><a href="recipes.html">Открыть книгу рецептов &rarr;</a></p>
'''

recipes_body = f'''
<div class="hero">
  <p class="eyebrow">Все рецепты по фазам</p>
  <h1>Книга рецептов</h1>
  <p>Рецепты сгруппированы по этапам прогрессии, а не по станкам: сверху вниз это порядок, в котором
  их открываешь. Под каждым предметом сказано, что это и зачем нужно. Наведите курсор на иконку, чтобы
  увидеть название; число в углу это количество.</p>
  <div class="hazard"></div>
</div>
{recipe_sections}
'''

DOCS.mkdir(exist_ok=True)
(DOCS / "index.html").write_text(page("SpaceReloaded · руководство", "guide", guide_body), encoding="utf-8")
(DOCS / "recipes.html").write_text(page("SpaceReloaded · книга рецептов", "recipes", recipes_body), encoding="utf-8")
missing = [n for n in recipes if n not in DESC]
print("index.html + recipes.html written to docs/. Рецептов:", len(recipes),
      "| без описания:", missing or "нет")
