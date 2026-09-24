#!/usr/bin/env python3
"""Данные и ресурсы 008 «Тяжёлая индустрия»: шаблоны мультиблоков, теги, рецепты, лут, массы,
блокстейты и модели (цельными объёмами через modelkit). Идемпотентен.
Запуск: python3 tools/gen_008.py (после gen_007.py)."""
import json
import os

import gen_006 as g
import modelkit as mk
from gen_006 import DATA, NS, assembly, chemical, sr, write

MB = os.path.join(DATA, NS, "multiblock")

# --- US1: стойка жизнеобеспечения ---------------------------------------------------------------
ECLSS_MODULES = ["ogs_module", "sabatier_module", "cdra_module", "wrs_module", "eclss_blank_panel"]
ECLSS_BLOCKS = ["eclss_controller", "eclss_rack_frame"] + ECLSS_MODULES
ANIM_PARTS = ["eclss_fan", "eclss_piston"]


def eclss_template():
    """Стойка в стене зоны: 5 × 4, ключ внизу по центру, четыре гнезда модулей (x = ±1, y = 1…2)."""
    cells = []
    for y in range(0, 4):
        for x in range(-2, 3):
            if (x, y) == (0, 0):
                continue
            socket = x in (-1, 1) and y in (1, 2)
            cells.append({"offset": [x, y, 0],
                          "block": f"#{NS}:eclss_socket" if socket else sr("eclss_rack_frame")})
    write(os.path.join(MB, "eclss_rack.json"), {"key": sr("eclss_controller"), "cells": cells})


def eclss_recipes():
    assembly("eclss_controller", ["steel_plate", "steel_plate", "circuit_board", "relay_logic", "copper_wire"],
             "eclss_controller")
    assembly("eclss_rack_frame", ["aluminium_ingot", "aluminium_ingot", "steel_plate"], "eclss_rack_frame", 4)
    # OGS: щелочной электролизёр с никелевыми электродами
    assembly("ogs_module", ["electrolysis_cell", "nickel_ingot", "nickel_ingot", "circuit_board", "copper_wire"],
             "ogs_module")
    # Сабатье: никелевый катализатор на глинозёме, подогрев пуска
    assembly("sabatier_module", ["nickel_ingot", "alumina", "alumina", "steel_plate", "copper_wire"],
             "sabatier_module")
    # CDRA: цеолитовые слои + вентилятор (мотор)
    assembly("cdra_module", ["zeolite_bed", "zeolite_bed", "motor", "steel_plate"], "cdra_module")
    # WRS: дистиллятор с компрессией пара
    assembly("wrs_module", ["copper_plate", "copper_plate", "glass_fiber", "steel_plate", "relay"], "wrs_module")
    assembly("eclss_blank_panel", ["steel_plate"], "eclss_blank_panel", 2)


def eclss_models():
    # контроллер: фасад с экраном; сформирован — экран горит
    for formed in (False, True):
        for active in (False, True):
            name = "eclss_controller" + ("_formed" if formed else "") + ("_on" if active else "")
            front = "eclss_controller_front" + ("_on" if formed and active else "")
            mk.plain(name, "minecraft:block/orientable", {"front": f"{NS}:block/{front}",
                                                         "side": f"{NS}:block/eclss_panel_side",
                                                         "top": f"{NS}:block/eclss_panel_side"})
    mk.blockstate("eclss_controller", mk.facing_variants(
        lambda formed, active: f"{NS}:block/eclss_controller" + ("_formed" if formed else "") + ("_on" if active else "")))
    mk.item("eclss_controller", f"{NS}:block/eclss_controller")
    # рама: несформированная — решётка; сформированная — стойка с направляющими
    for suffix in ("", "_formed"):
        mk.plain("eclss_rack_frame" + suffix, "minecraft:block/cube_all",
                 {"all": f"{NS}:block/eclss_rack_frame{suffix}"})
    mk.blockstate("eclss_rack_frame", {"formed=false": {"model": f"{NS}:block/eclss_rack_frame"},
                                       "formed=true": {"model": f"{NS}:block/eclss_rack_frame_formed"}})
    mk.item("eclss_rack_frame", f"{NS}:block/eclss_rack_frame")
    # модули: несформированный — фасад с маркировкой; в стойке — утопленная ниша 3 px, где идёт работа
    for m in ECLSS_MODULES:
        mk.plain(m, "minecraft:block/orientable", {"front": f"{NS}:block/{m}_front",
                                                   "side": f"{NS}:block/eclss_module_side",
                                                   "top": f"{NS}:block/eclss_module_side"})
        inner = f"{NS}:block/{m}_inner" if m != "eclss_blank_panel" else f"{NS}:block/eclss_blank_panel_front"
        els = [mk.box([0, 0, 3], [16, 16, 16], "#side", over={"north": "#inner"}),
               mk.box([0, 0, 0], [2, 16, 3], "#rim", skip=("south",)),
               mk.box([14, 0, 0], [16, 16, 3], "#rim", skip=("south",)),
               mk.box([2, 14, 0], [14, 16, 3], "#rim", skip=("south", "east", "west")),
               mk.box([2, 0, 0], [14, 2, 3], "#rim", skip=("south", "east", "west"))]
        if m == "eclss_blank_panel":
            els = [mk.box([0, 0, 0], [16, 16, 16], "#side", over={"north": "#inner"})]
        mk.model(m + "_formed", {"side": f"{NS}:block/eclss_module_side", "inner": inner,
                                 "rim": f"{NS}:block/eclss_module_rim"}, els)
        mk.blockstate(m, mk.facing_formed_variants(lambda formed, m=m: f"{NS}:block/{m}" + ("_formed" if formed else "")))
        mk.item(m, f"{NS}:block/{m}")
    # подвижные части (рисует BER контроллера в нише модуля): вентилятор CDRA, поршень насоса WRS
    fan = [mk.box([7, 7, 0.8], [9, 9, 2.6], "#hub")]
    for f, t in (([9, 7.3, 1.2], [13.5, 8.7, 2.2]), ([2.5, 7.3, 1.2], [7, 8.7, 2.2]),
                 ([7.3, 9, 1.2], [8.7, 13.5, 2.2]), ([7.3, 2.5, 1.2], [8.7, 7, 2.2])):
        fan.append(mk.box(f, t, "#blade"))
    mk.model("eclss_fan", {"hub": f"{NS}:block/eclss_piston", "blade": f"{NS}:block/eclss_fan"}, fan)
    mk.model("eclss_piston", {"rod": f"{NS}:block/eclss_piston", "head": f"{NS}:block/eclss_fan"},
             [mk.box([7.5, 8, 1.2], [8.5, 13, 2.2], "#rod"), mk.box([6, 6, 0.8], [10, 8, 2.6], "#head")])
    for p in ANIM_PARTS:
        mk.blockstate(p, {"": {"model": f"{NS}:block/{p}"}})


# --- US2: реактор деления Kilopower -------------------------------------------------------------
REACTOR_BLOCKS = ["control_rod_drive", "reactor_core", "beo_reflector", "heat_pipe", "stirling_convertor",
                  "reactor_power_cap", "radiator_panel"]
REACTOR_PARTS = ["control_rod", "stirling_piston"]
POWER_SLOTS = [[1, 2, 0], [-1, 2, 0], [0, 2, 1], [0, 2, -1]]


def reactor_template():
    """Мачта Kilopower: привод (ключ) внизу, над ним зона в кольце BeO, тепловая труба и крестовина из
    4 гнёзд Стирлингов, выше — сегменты радиатора (центральная труба + 4 ребра), 1…12 штук."""
    cells = [{"offset": [0, 1, 0], "block": sr("reactor_core")}]
    for x in (-1, 0, 1):
        for z in (-1, 0, 1):
            if (x, z) != (0, 0):
                cells.append({"offset": [x, 1, z], "block": sr("beo_reflector")})
    cells.append({"offset": [0, 2, 0], "block": sr("heat_pipe")})
    for o in POWER_SLOTS:
        cells.append({"offset": o, "block": f"#{NS}:reactor_power_slot"})
    seg = [{"offset": [0, 3, 0], "block": sr("heat_pipe")}] + [
        {"offset": [x, 3, z], "block": sr("radiator_panel")} for x, z in ((1, 0), (-1, 0), (0, 1), (0, -1))]
    write(os.path.join(MB, "fission_reactor.json"), {"key": sr("control_rod_drive"), "cells": cells,
                                                     "repeat": {"cells": seg, "step": [0, 1, 0], "min": 1, "max": 12,
                                                                "display": 3}})


def reactor_recipes():
    assembly("control_rod_drive", ["steel_plate", "steel_plate", "motor", "radhard_processor", "boron_carbide"],
             "control_rod_drive")
    # карбид бора B₄C: карботермия буры (кернит) в печи — бор из 006? нет: борная кислота из гейзерита
    assembly("reactor_core", ["steel_plate", "steel_plate", "nickel_superalloy_ingot", "nickel_superalloy_ingot"],
             "reactor_core")
    assembly("beo_reflector", ["beryllium_oxide", "beryllium_oxide", "steel_plate"], "beo_reflector")
    assembly("heat_pipe", ["nickel_superalloy_ingot", "sodium", "steel_plate"], "heat_pipe", 2)
    assembly("stirling_convertor", ["nickel_superalloy_ingot", "steel_plate", "copper_wire", "copper_wire",
                                    "minecraft:iron_ingot"], "stirling_convertor")
    assembly("reactor_power_cap", ["steel_plate"], "reactor_power_cap")
    assembly("radiator_panel", ["aluminium_ingot", "aluminium_ingot", "heat_pipe"], "radiator_panel", 2)


# --- материалы реактора и топлива (US2/US3) ---------------------------------------------------------
MATERIAL_ITEMS = ["beryl", "beryllium_hydroxide", "beryllium_oxide", "sodium", "borax", "boric_acid",
                  "boron_carbide_blend", "boron_carbide", "zircon", "zirconium", "uraninite", "yellowcake",
                  "uranium_dioxide", "uranium_tetrafluoride", "fluorine", "uranium_hexafluoride",
                  "depleted_uranium_hexafluoride"]
# урановая смолка — богатая жила (≈ 36 кг U в блоке, как Шинколобве); порция урановых предметов — 10 кг U
ORES = {"beryl_ore": ("beryl", 1, 1), "borax_ore": ("borax", 2, 4), "uraninite_ore": ("uraninite", 2, 4)}
ICE = f"#{NS}:electrolyzer_input"


def material_recipes():
    # бериллий: сульфатный процесс (берилл Be₃Al₂Si₆O₁₈ + H₂SO₄), осаждение Be(OH)₂ щёлочью; алюминий и
    # кремнезём — побочно; прокалка гидроксида до BeO (1000 °C)
    chemical("beryllium_hydroxide", "chemical_reactor", [("beryl", 1), ("sulfuric_acid", 2), ("caustic_soda", 2)],
             [("beryllium_hydroxide", 1), ("alumina", 1, 0.5), ("minecraft:sand", 1)], kwh=2.0, ticks=240)
    chemical("beryllium_oxide", "electric_furnace", [("beryllium_hydroxide", 1)], [("beryllium_oxide", 1)],
             kwh=0.8, ticks=160)
    # натрий: электролиз расплава NaCl (процесс Даунса, 600 °C), ≈ 10 кВт·ч/кг; хлор — в отвод
    chemical("sodium", "electrolyzer", [("rock_salt", 1)], [("sodium", 1)], kwh=10.0, ticks=200)
    # бор: бура (эвапорит) + H₂SO₄ → борная кислота; B₄C — карботермия 2400 °C: 2B₂O₃ + 7C → B₄C + 6CO
    chemical("boric_acid", "chemical_reactor", [("borax", 1), ("sulfuric_acid", 1)], [("boric_acid", 4)],
             kwh=0.5, ticks=160)
    assembly("boron_carbide_blend", ["boric_acid", "boric_acid", "coal_dust", "coal_dust"], "boron_carbide_blend")
    chemical("boron_carbide", "electric_furnace", [("boron_carbide_blend", 1)],
             [("boron_carbide", 1), ("carbon_monoxide", 1, 0.5)], kwh=6.0, ticks=320)
    # цирконий: тяжёлая фракция песка (циркон ZrSiO₄, ~0.3 %), процесс Кролла — хлорирование с углём и
    # восстановление (сокращённо в одном реакторе)
    chemical("zircon", "chemical_reactor", [("minecraft:sand", 16)], [("zircon", 1), ("minecraft:sand", 15)],
             kwh=0.3, ticks=200)
    chemical("zirconium", "chemical_reactor", [("zircon", 1), ("coal_dust", 1), ("hydrogen_chloride", 4)],
             [("zirconium", 1), ("minecraft:sand", 1, 0.5)], kwh=8.0, ticks=320)
    # уран: выщелачивание смолки H₂SO₄ и экстракция → жёлтый кек U₃O₈; восстановление водородом → UO₂;
    # гидрофторирование UO₂ + 4HF → UF₄; фторирование UF₄ + F₂ → UF₆. F₂ — электролиз HF в ванне KHF₂
    chemical("yellowcake", "chemical_reactor", [("uraninite", 1), ("sulfuric_acid", 2)], [("yellowcake", 1)],
             kwh=1.5, ticks=240)
    chemical("uranium_dioxide", "deposition_reactor", [("yellowcake", 1), (ICE, 1)],
             [("uranium_dioxide", 1)], kwh=2.0, ticks=240)
    chemical("uranium_tetrafluoride", "chemical_reactor", [("uranium_dioxide", 1), ("hydrofluoric_acid", 4)],
             [("uranium_tetrafluoride", 1)], kwh=1.0, ticks=200)
    chemical("fluorine", "electrolyzer", [("hydrofluoric_acid", 2)], [("fluorine", 1)], kwh=8.0, ticks=200)
    chemical("uranium_hexafluoride", "chemical_reactor", [("uranium_tetrafluoride", 1), ("fluorine", 1)],
             [("uranium_hexafluoride", 1)], kwh=0.5, ticks=160)


def material_data():
    for name, (drop, lo, hi) in ORES.items():
        functions = []
        if hi > lo:
            functions.append({"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        functions += [{"enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops", "function": "minecraft:apply_bonus"},
                      {"function": "minecraft:explosion_decay"}]
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}],
                 "name": sr(name)},
                {"type": "minecraft:item", "name": sr(drop), "functions": functions}]}]}]})
    # пегматиты (берилл — рядом со сподуменом), эвапориты (бура — неглубоко), урановая смолка (глубоко, редко)
    g.ore_feature("ore_beryl", "beryl_ore", "#minecraft:stone_ore_replaceables", 5, 2, -32, 32)
    g.ore_feature("ore_borax", "borax_ore", "#minecraft:stone_ore_replaceables", 16, 1, 30, 72)
    g.ore_feature("ore_uraninite", "uraninite_ore", "#minecraft:deepslate_ore_replaceables", 8, 2, -64, -16)
    base = os.path.join(os.path.dirname(DATA), "minecraft", "tags", "block")
    for tag, blocks in (("mineable/pickaxe", list(ORES)), ("needs_iron_tool", ["beryl_ore", "uraninite_ore"]),
                        ("needs_stone_tool", ["borax_ore"])):
        path = os.path.join(base, tag + ".json")
        with open(path, encoding="utf-8") as f:
            obj = json.load(f)
        for b in blocks:
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)


def material_models():
    for i in MATERIAL_ITEMS + ["fuel_basket"]:
        write(os.path.join(g.ASSETS, "models", "item", i + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}})
        g.item_def(i, f"{NS}:item/{i}")
    for o in ORES:
        mk.plain(o, "minecraft:block/cube_all", {"all": f"{NS}:block/{o}"})
        mk.blockstate(o, {"": {"model": f"{NS}:block/{o}"}})
        mk.item(o, f"{NS}:block/{o}")


# --- US3: каскад газовых центрифуг ------------------------------------------------------------------
CASCADE_BLOCKS = ["cascade_controller", "gas_centrifuge"]


def cascade_template():
    """Линия центрифуг за контроллером: 1…40 ступеней (обогатительная часть симметричного каскада)."""
    write(os.path.join(MB, "centrifuge_cascade.json"), {
        "key": sr("cascade_controller"),
        "repeat": {"cells": [{"offset": [0, 0, 1], "block": sr("gas_centrifuge")}], "step": [0, 0, 1], "min": 1,
                   "max": 40, "display": 8}})


def cascade_recipes():
    assembly("cascade_controller", ["steel_plate", "steel_plate", "circuit_board", "relay_logic", "copper_wire"],
             "cascade_controller")
    # ротор из углепластика (прочность на разрыв задаёт окружную скорость и коэффициент разделения)
    assembly("gas_centrifuge", ["carbon_fiber", "carbon_fiber", "motor", "steel_plate", "aluminium_ingot"],
             "gas_centrifuge")


def rotor_rings(r_out, cell=0.5):
    """Круглый ротор (объёмы по сетке, без общих плоскостей) в плоскости XZ, по высоте 2…14."""
    import math
    n = round(16 / cell)
    grid = [[math.hypot((i + 0.5) * cell - 8, (j + 0.5) * cell - 8) < r_out for i in range(n)] for j in range(n)]
    used = [[False] * n for _ in range(n)]
    out = []
    for j in range(n):
        i = 0
        while i < n:
            if not grid[j][i] or used[j][i]:
                i += 1
                continue
            i2 = i
            while i2 + 1 < n and grid[j][i2 + 1] and not used[j][i2 + 1]:
                i2 += 1
            j2 = j
            while j2 + 1 < n and all(grid[j2 + 1][k] and not used[j2 + 1][k] for k in range(i, i2 + 1)):
                j2 += 1
            for jj in range(j, j2 + 1):
                for k in range(i, i2 + 1):
                    used[jj][k] = True
            out.append((i * cell, j * cell, (i2 + 1) * cell, (j2 + 1) * cell))
            i = i2 + 1
    return out


def cascade_models():
    for formed in (False, True):
        for active in (False, True):
            name = "cascade_controller" + ("_formed" if formed else "") + ("_on" if active else "")
            front = "cascade_controller_front" + ("_on" if formed and active else "")
            mk.plain(name, "minecraft:block/orientable", {"front": f"{NS}:block/{front}", "side": f"{NS}:block/reactor_steel",
                                                         "top": f"{NS}:block/reactor_steel"})
    mk.blockstate("cascade_controller", mk.facing_variants(
        lambda formed, active: f"{NS}:block/cascade_controller" + ("_formed" if formed else "") + ("_on" if active else "")))
    mk.item("cascade_controller", f"{NS}:block/cascade_controller")
    # центрифуга: несформирована — кожух; сформирована — кожух со смотровыми щелями, ротор рисует BER
    mk.plain("gas_centrifuge", "minecraft:block/cube_column", {"end": f"{NS}:block/centrifuge_end", "side": f"{NS}:block/centrifuge_side"})
    els = [mk.box([2, 0, 2], [14, 2, 14], "#steel"), mk.box([2, 14, 2], [14, 16, 14], "#steel")]
    for x in (2, 12):
        for z in (2, 12):
            els.append(mk.box([x, 2, z], [x + 2, 14, z + 2], "#steel", skip=("up", "down")))
    mk.model("gas_centrifuge_formed", {"steel": f"{NS}:block/centrifuge_side"}, els)
    mk.blockstate("gas_centrifuge", {"formed=false": {"model": f"{NS}:block/gas_centrifuge"},
                                     "formed=true": {"model": f"{NS}:block/gas_centrifuge_formed"}})
    mk.item("gas_centrifuge", f"{NS}:block/gas_centrifuge")
    rotor = [mk.box([x1, 2, z1], [x2, 13.9, z2], "#r", skip=("up", "down")) for x1, z1, x2, z2 in rotor_rings(4.6)]
    rotor += [mk.box([x1, 14, z1], [x2, 14.5, z2], "#cap", skip=("down",)) for x1, z1, x2, z2 in rotor_rings(2.4)]
    # крышка ротора — тонкий диск встык над телом (у тела торцы не рисуются: низ закрыт плитой кожуха)
    rotor += [mk.box([x1, 13.9, z1], [x2, 14, z2], "#r", skip=("down",)) for x1, z1, x2, z2 in rotor_rings(4.6)]
    mk.model("centrifuge_rotor", {"r": f"{NS}:block/centrifuge_rotor", "cap": f"{NS}:block/reactor_steel"}, rotor)
    mk.blockstate("centrifuge_rotor", {"": {"model": f"{NS}:block/centrifuge_rotor"}})


# --- US4: криогенная воздухоразделительная колонна ---------------------------------------------------
ASU_BLOCKS = ["asu_sump", "asu_tray", "asu_heat_exchanger"]
ASU_PARTS = ["asu_expander_wheel", "asu_column_cap"]


def asu_template():
    """Испаритель (ключ) внизу, сбоку теплообменник с детандером и компрессор на валу, вверх — тарелки 3…40."""
    write(os.path.join(MB, "air_separation_column.json"), {
        "key": sr("asu_sump"),
        "cells": [{"offset": [1, 0, 0], "block": sr("asu_heat_exchanger")},
                  {"offset": [2, 0, 0], "block": sr("asu_compressor")}],
        "repeat": {"cells": [{"offset": [0, 1, 0], "block": sr("asu_tray")}], "step": [0, 1, 0], "min": 3, "max": 40,
                   "display": 11}})


def asu_recipes():
    assembly("asu_sump", ["steel_plate", "steel_plate", "copper_plate", "hermetic_glass", "relay_logic"], "asu_sump")
    assembly("asu_tray", ["aluminium_ingot", "aluminium_ingot", "copper_plate", "glass_fiber"], "asu_tray", 2)
    # пластинчато-ребристый теплообменник из паяного алюминия + турбодетандер (цикл Клода)
    assembly("asu_heat_exchanger", ["aluminium_ingot", "aluminium_ingot", "aluminium_ingot", "copper_wire",
                                    "nickel_superalloy_ingot"], "asu_heat_exchanger")
    assembly("asu_compressor", ["steel_plate", "steel_plate", "steel_shaft", "nickel_superalloy_ingot", "large_gear"],
             "asu_compressor")
    assembly("argon_canister", ["steel_plate", "steel_plate"], "argon_canister", 1)


def asu_models():
    for formed in (False, True):
        for active in (False, True):
            name = "asu_sump" + ("_formed" if formed else "") + ("_on" if active else "")
            if not formed:
                mk.plain(name, "minecraft:block/orientable", {"front": f"{NS}:block/asu_sump_front",
                                                             "side": f"{NS}:block/asu_coldbox", "top": f"{NS}:block/asu_coldbox"})
                continue
            # смотровое стекло испарителя: ниша 2 px, уровень жидкого O₂ рисует BER
            els = [mk.box([0, 0, 2], [16, 16, 16], "#box", over={"north": "#inner"}),
                   mk.box([0, 0, 0], [3, 16, 2], "#box", skip=("south",)),
                   mk.box([13, 0, 0], [16, 16, 2], "#box", skip=("south",)),
                   mk.box([3, 12, 0], [13, 16, 2], "#box", skip=("south", "east", "west")),
                   mk.box([3, 0, 0], [13, 3, 2], "#box", skip=("south", "east", "west"))]
            mk.model(name, {"box": f"{NS}:block/asu_coldbox", "inner": f"{NS}:block/asu_sump_inner"}, els)
    mk.blockstate("asu_sump", mk.facing_variants(
        lambda formed, active: f"{NS}:block/asu_sump" + ("_formed" if formed else "") + ("_on" if active and formed else "")))
    mk.item("asu_sump", f"{NS}:block/asu_sump")
    # тарелки: несформированы — кассета; сформированы — колонна в холодильном кожухе с бандажами
    mk.plain("asu_tray", "minecraft:block/cube_column", {"end": f"{NS}:block/asu_tray_end", "side": f"{NS}:block/asu_tray_side"})
    mk.model("asu_tray_formed", {"shell": f"{NS}:block/asu_coldbox", "band": f"{NS}:block/reactor_steel"},
             [mk.box([2, 0, 2], [14, 7, 14], "#shell", skip=("up", "down")),
              mk.box([1.5, 7, 1.5], [14.5, 9, 14.5], "#band", skip=()),
              mk.box([2, 9, 2], [14, 16, 14], "#shell", skip=("up", "down"))])
    mk.blockstate("asu_tray", {"formed=false": {"model": f"{NS}:block/asu_tray"}, "formed=true": {"model": f"{NS}:block/asu_tray_formed"}})
    mk.item("asu_tray", f"{NS}:block/asu_tray")
    # теплообменник: паяный алюминиевый блок, сверху корпус турбодетандера (крыльчатку рисует BER)
    mk.plain("asu_heat_exchanger", "minecraft:block/cube_column",
             {"end": f"{NS}:block/asu_exchanger_top", "side": f"{NS}:block/asu_exchanger_side"})
    mk.model("asu_heat_exchanger_formed", {"core": f"{NS}:block/asu_exchanger_side", "top": f"{NS}:block/asu_exchanger_top",
                                           "housing": f"{NS}:block/reactor_steel"},
             [mk.box([1, 0, 1], [15, 12, 15], "#core", over={"up": "#top"}),
              mk.box([4, 12, 4], [12, 13, 12], "#housing", skip=("down",))])
    mk.blockstate("asu_heat_exchanger", {"formed=false": {"model": f"{NS}:block/asu_heat_exchanger"},
                                         "formed=true": {"model": f"{NS}:block/asu_heat_exchanger_formed"}})
    mk.item("asu_heat_exchanger", f"{NS}:block/asu_heat_exchanger")
    # компрессор: корпус-улитка на валу (ось по свойству axis)
    mk.plain("asu_compressor", "minecraft:block/cube_column",
             {"end": f"{NS}:block/asu_compressor_end", "side": f"{NS}:block/asu_compressor_side"})
    mk.blockstate("asu_compressor", {"axis=y": {"model": f"{NS}:block/asu_compressor"},
                                     "axis=z": {"model": f"{NS}:block/asu_compressor", "x": 90},
                                     "axis=x": {"model": f"{NS}:block/asu_compressor", "x": 90, "y": 90}})
    mk.item("asu_compressor", f"{NS}:block/asu_compressor")
    # части: крыльчатка детандера, шапка колонны со сбросным клапаном
    wheel = [mk.box([7, 13, 7], [9, 14.5, 9], "#w")]
    for f, t in (([9, 13.2, 7.4], [11.5, 14.2, 8.6]), ([4.5, 13.2, 7.4], [7, 14.2, 8.6]),
                 ([7.4, 13.2, 9], [8.6, 14.2, 11.5]), ([7.4, 13.2, 4.5], [8.6, 14.2, 7])):
        wheel.append(mk.box(f, t, "#w"))
    mk.model("asu_expander_wheel", {"w": f"{NS}:block/eclss_fan"}, wheel)
    mk.model("asu_column_cap", {"shell": f"{NS}:block/asu_coldbox", "valve": f"{NS}:block/reactor_steel"},
             [mk.box([2, 0, 2], [14, 3, 14], "#shell", skip=("down",)),
              mk.box([7, 3, 7], [9, 7, 9], "#valve", skip=("down",)),
              mk.box([9, 5, 7.25], [12, 6.5, 8.75], "#valve")])
    for p in ASU_PARTS:
        mk.blockstate(p, {"": {"model": f"{NS}:block/{p}"}})
    write(os.path.join(g.ASSETS, "models", "item", "argon_canister.json"),
          {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/argon_canister"}})
    g.item_def("argon_canister", f"{NS}:item/argon_canister")


# --- US5: дуговая сталеплавильная печь ----------------------------------------------------------------
EAF_BLOCKS = ["eaf_controller", "eaf_shell", "eaf_roof"]
EAF_PARTS = ["eaf_vessel", "eaf_roof_part", "eaf_electrode"]
ELECTRODE_R = 7


def eaf_template():
    """Трансформатор (ключ) перед ванной 3×3×2 из огнеупорного корпуса; центр второго ряда — полость ванны;
    над центром — свод с электродами."""
    cells = []
    for y in (0, 1):
        for x in (-1, 0, 1):
            for z in (1, 2, 3):
                if y == 1 and (x, z) == (0, 2):
                    cells.append({"offset": [x, y, z], "block": "minecraft:air"})
                else:
                    cells.append({"offset": [x, y, z], "block": sr("eaf_shell")})
    cells.append({"offset": [0, 2, 2], "block": sr("eaf_roof")})
    write(os.path.join(MB, "arc_furnace.json"), {"key": sr("eaf_controller"), "cells": cells})


def eaf_recipes():
    assembly("eaf_controller", ["steel_plate", "steel_plate", "copper_plate", "copper_plate", "copper_wire",
                                "copper_wire", "relay_logic"], "eaf_controller")
    assembly("eaf_shell", ["steel_plate", "refractory_lining"], "eaf_shell", 2)
    assembly("eaf_roof", ["steel_plate", "steel_plate", "refractory_lining", "copper_plate"], "eaf_roof")
    # графитовые электроды: кокс и пек → заготовка → графитация 3000 °C (≈ 4 кВт·ч/кг)
    assembly("electrode_blank", ["coal_dust", "coal_dust", "coal_dust", "coal_dust", "coal_dust", "coal_dust"],
             "electrode_blank")
    chemical("graphite_electrode", "electric_furnace", [("electrode_blank", 1)], [("graphite_electrode", 1)],
             kwh=40.0, ticks=400)


def raster(shape, lo, hi, cell):
    """Прямоугольники (x1, z1, x2, z2) покрытия плоской фигуры shape(x, z) на сетке."""
    n = round((hi - lo) / cell)
    grid = [[shape(lo + (i + 0.5) * cell, lo + (j + 0.5) * cell) for i in range(n)] for j in range(n)]
    used = [[False] * n for _ in range(n)]
    out = []
    for j in range(n):
        i = 0
        while i < n:
            if not grid[j][i] or used[j][i]:
                i += 1
                continue
            i2 = i
            while i2 + 1 < n and grid[j][i2 + 1] and not used[j][i2 + 1]:
                i2 += 1
            j2 = j
            while j2 + 1 < n and all(grid[j2 + 1][k] and not used[j2 + 1][k] for k in range(i, i2 + 1)):
                j2 += 1
            for jj in range(j, j2 + 1):
                for k in range(i, i2 + 1):
                    used[jj][k] = True
            out.append((lo + i * cell, lo + j * cell, lo + (i2 + 1) * cell, lo + (j2 + 1) * cell))
            i = i2 + 1
    return out


def eaf_models():
    import math
    for formed in (False, True):
        for active in (False, True):
            name = "eaf_controller" + ("_formed" if formed else "") + ("_on" if active else "")
            front = "eaf_controller_front" + ("_on" if formed and active else "")
            mk.plain(name, "minecraft:block/orientable", {"front": f"{NS}:block/{front}", "side": f"{NS}:block/eaf_transformer",
                                                         "top": f"{NS}:block/eaf_transformer"})
    mk.blockstate("eaf_controller", mk.facing_variants(
        lambda formed, active: f"{NS}:block/eaf_controller" + ("_formed" if formed else "") + ("_on" if active else "")))
    mk.item("eaf_controller", f"{NS}:block/eaf_controller")
    # собранный корпус и свод — невидимы: всю печь рисует BER (она наклоняется и открывается)
    mk.model("eaf_hidden", {"particle": f"{NS}:block/eaf_shell"}, [])
    for b in ("eaf_shell", "eaf_roof"):
        mk.plain(b, "minecraft:block/cube_all", {"all": f"{NS}:block/{b}"})
        mk.blockstate(b, {"formed=false": {"model": f"{NS}:block/{b}"}, "formed=true": {"model": f"{NS}:block/eaf_hidden"}})
        mk.item(b, f"{NS}:block/{b}")
    c = 8.0
    # ванна: круглый кожух R 22 px с огнеупорной футеровкой, под — диск, носок слива спереди (−z)
    vessel = []
    for x1, z1, x2, z2 in raster(lambda x, z: math.hypot(x - c, z - c) < 22, -16, 32, 2):
        vessel.append(mk.box([x1, 0, z1], [x2, 3, z2], "#shell", skip=("up",)))
    for x1, z1, x2, z2 in raster(lambda x, z: 17 <= math.hypot(x - c, z - c) < 22, -16, 32, 2):
        vessel.append(mk.box([x1, 3, z1], [x2, 30, z2], "#shell", skip=("down",), over={"up": "#lining"}))
    for x1, z1, x2, z2 in raster(lambda x, z: math.hypot(x - c, z - c) < 17, -16, 32, 2):
        vessel.append(mk.box([x1, 3, z1], [x2, 4, z2], "#lining", skip=("down",)))  # под ванны (огнеупор)
    vessel.append(mk.box([25, 22, 5], [32, 25, 11], "#shell"))  # сливной носок (модель: +x; грани на нечётных — мимо граней кольца)
    mk.model("eaf_vessel", {"shell": f"{NS}:block/eaf_vessel", "lining": f"{NS}:block/refractory_lining"}, vessel)
    # свод: диск R 23 с тремя отверстиями под электроды (120°)
    holes = [(c + ELECTRODE_R * math.cos(a), c + ELECTRODE_R * math.sin(a)) for a in (math.pi / 2, math.pi / 2 + 2.094, math.pi / 2 + 4.189)]
    roof_shape = lambda x, z: math.hypot(x - c, z - c) < 23 and all(math.hypot(x - hx, z - hz) >= 3.2 for hx, hz in holes)
    roof = [mk.box([x1, 0, z1], [x2, 4, z2], "#roof") for x1, z1, x2, z2 in raster(roof_shape, -16, 32, 1)]
    mk.model("eaf_roof_part", {"roof": f"{NS}:block/eaf_roof"}, roof)
    # электрод: графитовый столб Ø 5 px, наверху медный держатель
    elec = [mk.box([x1, 0, z1], [x2, 28, z2], "#g", skip=("up",)) for x1, z1, x2, z2 in raster(
        lambda x, z: math.hypot(x - c, z - c) < 2.5, 4, 12, 0.5)]
    elec += [mk.box([5, 28, 5], [11, 31, 11], "#clamp")]
    mk.model("eaf_electrode", {"g": f"{NS}:block/graphite", "clamp": f"{NS}:block/copper_plate_block"}, elec)
    for part in EAF_PARTS:
        mk.blockstate(part, {"": {"model": f"{NS}:block/{part}"}})
    for i in ("electrode_blank", "graphite_electrode"):
        write(os.path.join(g.ASSETS, "models", "item", i + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}})
        g.item_def(i, f"{NS}:item/{i}")


def reactor_models():
    faces_all = mk.FACES
    # привод: несформирован — корпус с пультом; сформирован — открытый каркас (виден ход стержня)
    for formed in (False, True):
        for active in (False, True):
            name = "control_rod_drive" + ("_formed" if formed else "") + ("_on" if active else "")
            if not formed:
                mk.plain(name, "minecraft:block/orientable", {"front": f"{NS}:block/control_rod_drive_front",
                                                             "side": f"{NS}:block/reactor_steel",
                                                             "top": f"{NS}:block/reactor_steel"})
                continue
            panel = "#panel_on" if active else "#panel"
            els = [mk.box([0, 0, 0], [16, 3, 16], "#steel", over={"north": panel}),       # основание с пультом
                   mk.box([0, 13, 0], [16, 16, 16], "#steel")]                              # верхняя плита
            for x in (0, 14):
                for z in (0, 14):
                    els.append(mk.box([x, 3, z], [x + 2, 13, z + 2], "#steel", skip=("up", "down")))
            els.append(mk.box([5, 3, 5], [11, 4, 11], "#dark", skip=("down",)))            # муфта привода
            mk.model(name, {"steel": f"{NS}:block/reactor_steel", "panel": f"{NS}:block/control_rod_drive_front",
                            "panel_on": f"{NS}:block/control_rod_drive_front_on", "dark": f"{NS}:block/reactor_dark"}, els)
    mk.blockstate("control_rod_drive", mk.facing_variants(
        lambda formed, active: f"{NS}:block/control_rod_drive" + ("_formed" if formed else "") + ("_on" if active and formed else "")))
    mk.item("control_rod_drive", f"{NS}:block/control_rod_drive")
    simple = {"reactor_core": "reactor_core", "beo_reflector": "beo_reflector", "reactor_power_cap": "reactor_steel"}
    for b, tex in simple.items():
        mk.plain(b, "minecraft:block/cube_all", {"all": f"{NS}:block/{tex}"})
        mk.plain(b + "_formed", "minecraft:block/cube_all", {"all": f"{NS}:block/{tex}_formed" if b != "reactor_power_cap" else f"{NS}:block/{tex}"})
        mk.blockstate(b, {"formed=false": {"model": f"{NS}:block/{b}"}, "formed=true": {"model": f"{NS}:block/{b}_formed"}})
        mk.item(b, f"{NS}:block/{b}")
    # тепловая труба: несформирована — отрезок в кубе-кожухе; сформирована — открытая труба с фланцами
    mk.plain("heat_pipe", "minecraft:block/cube_column", {"end": f"{NS}:block/heat_pipe_end", "side": f"{NS}:block/heat_pipe_side"})
    mk.model("heat_pipe_formed", {"pipe": f"{NS}:block/heat_pipe", "flange": f"{NS}:block/reactor_steel"},
             [mk.box([6, 0, 6], [10, 16, 10], "#pipe", skip=("up", "down")),
              mk.box([5, 7, 5], [6, 9, 11], "#flange"), mk.box([10, 7, 5], [11, 9, 11], "#flange"),
              mk.box([6, 7, 5], [10, 9, 6], "#flange"), mk.box([6, 7, 10], [10, 9, 11], "#flange")])
    mk.blockstate("heat_pipe", {"formed=false": {"model": f"{NS}:block/heat_pipe"}, "formed=true": {"model": f"{NS}:block/heat_pipe_formed"}})
    mk.item("heat_pipe", f"{NS}:block/heat_pipe")
    # Стирлинг: цилиндр-вытеснитель, фланцы, ребра холодного конца
    stir = [mk.box([3, 0, 3], [13, 2, 13], "#steel"),
            mk.box([4, 2, 4], [12, 12, 12], "#body", skip=("down",)),
            mk.box([5, 12, 5], [11, 13, 11], "#steel", skip=("down",))]
    for y in (4, 7, 10):
        for f, t in (([3, y, 4], [4, y + 1, 12]), ([12, y, 4], [13, y + 1, 12]), ([4, y, 3], [12, y + 1, 4]), ([4, y, 12], [12, y + 1, 13])):
            stir.append(mk.box(f, t, "#fin"))
    mk.model("stirling_convertor", {"steel": f"{NS}:block/reactor_steel", "body": f"{NS}:block/stirling_body",
                                    "fin": f"{NS}:block/radiator_panel"}, stir)
    mk.model("stirling_convertor_formed", {"steel": f"{NS}:block/reactor_steel", "body": f"{NS}:block/stirling_body",
                                           "fin": f"{NS}:block/radiator_panel"}, stir)
    mk.blockstate("stirling_convertor", {"formed=false": {"model": f"{NS}:block/stirling_convertor"},
                                         "formed=true": {"model": f"{NS}:block/stirling_convertor_formed"}})
    mk.item("stirling_convertor", f"{NS}:block/stirling_convertor")
    # радиатор: несформирован — пакет панелей; сформирован — крестовина рёбер (видна с любой стороны)
    mk.plain("radiator_panel", "minecraft:block/cube_all", {"all": f"{NS}:block/radiator_panel"})
    mk.model("radiator_panel_formed", {"fin": f"{NS}:block/radiator_panel", "rib": f"{NS}:block/reactor_steel"},
             [mk.box([0, 0, 7.5], [16, 16, 8.5], "#fin"),
              mk.box([7.5, 0, 0], [8.5, 16, 7.5], "#fin", skip=("south",)),
              mk.box([7.5, 0, 8.5], [8.5, 16, 16], "#fin", skip=("north",))])
    mk.blockstate("radiator_panel", {"formed=false": {"model": f"{NS}:block/radiator_panel"},
                                     "formed=true": {"model": f"{NS}:block/radiator_panel_formed"}})
    mk.item("radiator_panel", f"{NS}:block/radiator_panel")
    # подвижные части: стержень B₄C с ходовым винтом, поршень Стирлинга
    mk.model("control_rod", {"rod": f"{NS}:block/control_rod", "screw": f"{NS}:block/reactor_steel"},
             [mk.box([6, 4, 6], [10, 16, 10], "#rod"), mk.box([7.25, 16, 7.25], [8.75, 28, 8.75], "#screw", skip=("down",))])
    mk.model("stirling_piston", {"p": f"{NS}:block/reactor_steel"}, [mk.box([7, 13, 7], [9, 16, 9], "#p", skip=("down",))])
    for p in REACTOR_PARTS:
        mk.blockstate(p, {"": {"model": f"{NS}:block/{p}"}})


MASSES = {
    "eclss_controller": (60.0, ["eclss_controller"]),
    "eclss_rack_frame": (25.0, ["eclss_rack_frame"]),          # стойка ISPR ~ 100 кг на 4 блока рамы
    "eclss_modules": (180.0, ["ogs_module", "sabatier_module", "cdra_module", "wrs_module"]),  # ORU класса МКС
    "eclss_blank_panel": (8.0, ["eclss_blank_panel"]),
    # Kilopower 1 кВт(э) ≈ 400 кг без радиатора: зона 28 кг ВОУ + сталь, отражатель BeO ~ 80 кг (**оценка** по KRUSTY)
    "control_rod_drive": (60.0, ["control_rod_drive"]),
    "reactor_core": (90.0, ["reactor_core"]),
    "beo_reflector": (10.0, ["beo_reflector"]),
    "heat_pipe": (4.0, ["heat_pipe"]),
    "stirling_convertor": (30.0, ["stirling_convertor"]),   # 250 Вт(э) класс ASC/Kilopower
    "reactor_power_cap": (3.0, ["reactor_power_cap"]),
    "radiator_panel": (6.0, ["radiator_panel"]),            # 1 м² алюминиевой панели с трубами, 6 кг/м²
    # по правилу 006: кусок — 1/9 блока 10 л × плотность
    "beryl": (3.0, ["beryl"]), "beryllium_oxide": (3.3, ["beryllium_oxide", "beryllium_hydroxide"]),
    "sodium": (1.08, ["sodium"]), "borate": (1.9, ["borax", "boric_acid", "boron_carbide_blend"]),
    "boron_carbide": (2.8, ["boron_carbide"]), "zircon": (5.2, ["zircon"]), "zirconium": (7.2, ["zirconium"]),
    # урановые продукты — порция 10 кг урана (учёт топлива в кг U); UF₆ — вместе с фтором (1.48 кг на кг U)
    "uranium": (10.0, ["uraninite", "yellowcake", "uranium_dioxide", "uranium_tetrafluoride", "uranium_hexafluoride",
                      "depleted_uranium_hexafluoride"]),
    "fluorine": (1.0, ["fluorine"]),
    "fuel_basket": (40.0, ["fuel_basket"]),
    "cascade_controller": (60.0, ["cascade_controller"]),
    "gas_centrifuge": (60.0, ["gas_centrifuge"]),
    "asu": (120.0, ["asu_sump", "asu_heat_exchanger", "asu_compressor"]),
    "asu_tray": (40.0, ["asu_tray"]),
    "argon_canister": (60.0, ["argon_canister"]),           # баллон 50 л + 10 кг аргона
    "eaf": (200.0, ["eaf_controller", "eaf_shell", "eaf_roof"]),
    "electrode": (40.0, ["electrode_blank", "graphite_electrode"]),   # Ø 200 мм × 0.8 м графита                 # корзина: ~30 кг сплава U-Zr + оболочка
}

BLOCKS = ECLSS_BLOCKS + REACTOR_BLOCKS + CASCADE_BLOCKS + ASU_BLOCKS + ["asu_compressor"] + EAF_BLOCKS


def data():
    eclss_template()
    reactor_template()
    cascade_template()
    asu_template()
    eaf_template()
    write(os.path.join(DATA, "tags", "item", "eaf_charge.json"), {"replace": False, "values": [
        sr("iron_dust"), sr("meteoric_iron"), "minecraft:iron_ingot", "minecraft:raw_iron", "minecraft:iron_nugget",
        "minecraft:iron_block", "minecraft:raw_iron_block"]})
    material_data()
    write(os.path.join(DATA, "tags", "block", "reactor_power_slot.json"),
          {"replace": False, "values": [sr("stirling_convertor"), sr("reactor_power_cap")]})
    write(os.path.join(DATA, "tags", "block", "eclss_socket.json"),
          {"replace": False, "values": [sr(m) for m in ECLSS_MODULES]})
    for b in BLOCKS:
        write(os.path.join(DATA, "loot_table", "blocks", b + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": sr(b)}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    base = os.path.join(os.path.dirname(DATA), "minecraft", "tags", "block")
    for path, blocks in ((os.path.join(base, "mineable", "pickaxe.json"), BLOCKS),
                         (os.path.join(DATA, "tags", "block", "airtight.json"), ECLSS_BLOCKS),
                         (os.path.join(DATA, "tags", "block", "energy_connectable.json"), ["eclss_controller"])):
        with open(path, encoding="utf-8") as f:
            obj = json.load(f)
        for b in blocks:
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)
    for name, (kg, items) in MASSES.items():
        write(os.path.join(DATA, NS, "item_mass", name + ".json"), {"items": [sr(i) for i in items], "kg": kg})


def main():
    eclss_recipes()
    reactor_recipes()
    material_recipes()
    cascade_recipes()
    asu_recipes()
    eaf_recipes()
    data()
    eclss_models()
    reactor_models()
    material_models()
    cascade_models()
    asu_models()
    eaf_models()


if __name__ == "__main__":
    main()
