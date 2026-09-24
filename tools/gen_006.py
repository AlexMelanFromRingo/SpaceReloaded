#!/usr/bin/env python3
"""Данные и ресурсы 006 «Материалы и электроника»: рецепты, лут, генерация мира, теги, массы,
блок-стейты, модели и определения предметов. Идемпотентен: перезаписывает только свои файлы и
правит перечисленные рецепты 002–005 (перевод на электронику). Запуск: python3 tools/gen_006.py

Энергия процессных рецептов — реальные кВт·ч на килограмм продукта, масса продукта — по таблице
масс (кусок = 1/9 блока 10 л), масштаб A = 31 E на кВт·ч: честны отношения цен процессов
(Сименс ~6× карботермии, Чохральский ещё дороже), абсолют — калибровка 007.
"""
import json
import os

ROOT = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources")
DATA = os.path.join(ROOT, "data", "spacereloaded")
ASSETS = os.path.join(ROOT, "assets", "spacereloaded")
NS = "spacereloaded"
A = 31  # E на кВт·ч


def sr(name):
    return name if ":" in name or name.startswith("#") else f"{NS}:{name}"


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent="\t", ensure_ascii=False)
        f.write("\n")


def recipe(name, obj):
    write(os.path.join(DATA, "recipe", name + ".json"), obj)


# ---------------------------------------------------------------------------
# Рецепты
# ---------------------------------------------------------------------------

def result(item, count=1, components=None):
    r = {"id": sr(item)}
    if count != 1:
        r["count"] = count
    if components:
        r["components"] = components
    return r


def assembly(name, ingredients, item, count=1, components=None):
    assert len(ingredients) <= 9, name
    recipe("assembly_" + name, {"type": f"{NS}:assembly", "ingredients": [sr(i) for i in ingredients],
                                "result": result(item, count, components)})


def smelting(name, ingredient, item, count=1):
    recipe("electric_smelting_" + name, {"type": f"{NS}:electric_smelting", "ingredient": sr(ingredient),
                                         "result": result(item, count)})


def machining(kind, name, ingredient, step, item, count=1, energy_j=None, components=None, balance=False):
    obj = {"type": f"{NS}:{kind}", "ingredient": sr(ingredient), "step": step,
           "result": result(item, count, components)}
    if energy_j:
        obj["energy_j"] = energy_j
    if balance:
        obj["balance"] = True
    recipe(f"{kind}_{name}", obj)


def chemical(name, machine, inputs, outputs, kwh=None, energy=None, ticks=200, purity=None, keep_purity=False,
             oxygen=0):
    """inputs: [(item, count)], outputs: [(item, count, yield)]."""
    obj = {"type": f"{NS}:chemical", "machine": machine,
           "inputs": [{"item": sr(i), "count": c} if c != 1 else {"item": sr(i)} for i, c in inputs],
           "outputs": []}
    for out in outputs:
        item, count, y = (out + (1.0,))[:3] if len(out) == 2 else out
        o = {"id": sr(item)}
        if count != 1:
            o["count"] = count
        if y != 1.0:
            o["yield"] = y
        obj["outputs"].append(o)
    obj["energy"] = int(round(kwh * A)) if kwh is not None else energy
    obj["ticks"] = ticks
    if purity is not None:
        obj["purity"] = purity
    if keep_purity:
        obj["keep_purity"] = True
    if oxygen:
        obj["oxygen"] = oxygen
    recipe(f"{machine}_{name}", obj)


SI = 2.59       # кг кремния в куске (1/9 блока 10 л × 2.33)
ICE = f"#{NS}:electrolyzer_input"


def recipes():
    # --- нулевой тир: как у Apollo ---
    machining("pressing", "copper_wire", "copper_plate", 0, "copper_wire", 8)
    assembly("relay", ["copper_wire", "copper_wire", "minecraft:iron_ingot", "minecraft:redstone"], "relay", 2)
    assembly("relay_logic", ["relay", "relay", "relay", "relay", "copper_wire", "steel_plate"], "relay_logic")
    smelting("ferrite_core", "iron_dust", "ferrite_core", 16)
    assembly("core_rope_memory", ["ferrite_core"] * 8 + ["copper_wire"], "core_rope_memory")

    # --- кремний ---
    assembly("silicon_blend", ["minecraft:sand", "coal_dust", "coal_dust"], "silicon_blend")
    assembly("silicon_blend_quartz", ["minecraft:quartz"] * 4 + ["coal_dust", "coal_dust"], "silicon_blend",
             components={f"{NS}:purity": 2.5})
    chemical("carbothermy", "electric_furnace", [("silicon_blend", 1)],
             [("metallurgical_silicon", 1), ("carbon_monoxide", 2)], kwh=12 * SI, ticks=240, keep_purity=True)
    smelting("silicon_dust", "silicon_dust", "metallurgical_silicon")
    chemical("directional_solidification", "electric_furnace", [("metallurgical_silicon", 1)],
             [("multicrystalline_silicon", 1)], kwh=8 * SI, ticks=400, purity=5.0)
    chemical("brine", "chemical_reactor", [("rock_salt", 1), (ICE, 1)], [("brine", 2)], energy=50, ticks=60)
    chemical("chlor_alkali", "electrolyzer", [("brine", 1)], [("hydrogen_chloride", 1), ("caustic_soda", 1)],
             energy=400, ticks=100)
    chemical("trichlorosilane", "sabatier_reactor", [("metallurgical_silicon", 1), ("hydrogen_chloride", 3)],
             [("trichlorosilane", 1)], energy=300, ticks=160, keep_purity=True)
    chemical("siemens", "deposition_reactor", [("trichlorosilane", 1), (ICE, 1)],
             [("polysilicon", 1), ("hydrogen_chloride", 3, 0.95)], kwh=70 * SI, ticks=600, keep_purity=True)
    assembly("phosphate_blend", ["minecraft:bone_meal"] * 3 + ["minecraft:sand", "coal_dust"], "phosphate_blend")
    chemical("wohler", "electric_furnace", [("phosphate_blend", 1)], [("phosphorus", 1), ("slag", 1)],
             kwh=14 * 2.0, ticks=300)
    smelting("quartz_crucible", "minecraft:quartz_block", "quartz_crucible")

    # --- кислоты, фтор ---
    chemical("sulfuric_acid", "chemical_reactor", [("minecraft:sulfur", 1), (ICE, 1)], [("sulfuric_acid", 2)],
             energy=100, ticks=160)
    chemical("hydrofluoric_acid", "chemical_reactor", [("fluorite", 1), ("sulfuric_acid", 1)],
             [("hydrofluoric_acid", 2), ("gypsum", 1)], energy=300, ticks=160)
    chemical("photoresist", "chemical_reactor", [("coal_dust", 1), ("sulfuric_acid", 1)], [("photoresist", 4)],
             energy=150, ticks=160)
    chemical("epoxy_resin", "chemical_reactor", [("coal_dust", 1), ("hydrogen_chloride", 1), ("caustic_soda", 1)],
             [("epoxy_resin", 2)], energy=200, ticks=200)

    # --- фаб, корпуса, чипы ---
    smelting("ceramic_package", "minecraft:clay_ball", "ceramic_package", 2)
    assembly("photomask_logic", ["hermetic_glass", "iron_dust", "relay_logic"], "photomask_logic")
    assembly("photomask_microprocessor", ["hermetic_glass", "iron_dust", "logic_chip", "logic_chip"],
             "photomask_microprocessor")
    assembly("photomask_radhard", ["hermetic_glass", "iron_dust", "microprocessor"], "photomask_radhard")
    for die, chip in (("die_logic", "logic_chip"), ("die_microprocessor", "microprocessor"),
                      ("die_radhard", "radhard_processor")):
        assembly(chip, [die] * 4 + ["ceramic_package"] * 4 + ["minecraft:gold_nugget"], chip, 4)
    smelting("sapphire", "alumina", "sapphire")
    chemical("sos_epitaxy", "deposition_reactor", [("trichlorosilane", 1), (ICE, 1), ("sapphire_wafer", 16)],
             [("sos_wafer", 16), ("hydrogen_chloride", 3, 0.95)], energy=2000, ticks=400, keep_purity=True)

    # --- платы и компьютер ---
    smelting("glass_fiber", "minecraft:glass", "glass_fiber", 4)
    assembly("fr4_laminate", ["glass_fiber"] * 4 + ["epoxy_resin"], "fr4_laminate")
    assembly("copper_clad_laminate", ["fr4_laminate", "copper_plate"], "copper_clad_laminate")
    machining("pressing", "circuit_board_1", "copper_clad_laminate", 0, "incomplete_circuit_board")
    machining("machining", "circuit_board_2", "incomplete_circuit_board", 1, "circuit_board", energy_j=40000)
    assembly("flight_computer", ["circuit_board", "microprocessor", "logic_chip", "logic_chip", "logic_chip",
                                 "logic_chip", "core_rope_memory", "copper_wire", "steel_plate"], "flight_computer")
    recipe("flight_program_guidance", {
        "type": "minecraft:crafting_transmute", "category": "misc",
        "input": sr("flight_program"), "material": sr("flight_computer"),
        "result": result("flight_program", components={f"{NS}:guidance_tier": 2})})

    # --- алюминий, литий, никель, сплавы ---
    chemical("alumina", "chemical_reactor", [("minecraft:clay_ball", 4), ("hydrogen_chloride", 2)],
             [("alumina", 1), ("minecraft:sand", 1), ("hydrogen_chloride", 2, 0.9)], energy=600, ticks=200)
    chemical("cryolite", "chemical_reactor", [("hydrofluoric_acid", 3), ("alumina", 1), ("caustic_soda", 3)],
             [("cryolite", 2)], energy=200, ticks=160)
    # 2 куска глинозёма (8.8 кг) несут 4.65 кг Al = 1.55 слитка по 3.0 кг и 4.1 кг O₂
    chemical("alumina_electrolysis", "regolith_reactor", [("alumina", 2)], [("aluminium_ingot", 2, 0.775)],
             kwh=20 * 4.65, ticks=300, oxygen=4.1)
    # анортозит CaAl₂Si₂O₈, блок 27.3 кг: Al 5.3 кг, Si 5.5 кг, O₂ ~11 кг, CaO — шлак
    chemical("anorthosite", "regolith_reactor", [("anorthosite", 1)],
             [("aluminium_ingot", 2, 0.88), ("metallurgical_silicon", 2), ("slag", 1)],
             kwh=20 * 5.3 + 13 * 5.5, ticks=400, oxygen=11.0)
    smelting("beta_spodumene", "spodumene", "beta_spodumene")
    chemical("lithium_chloride", "chemical_reactor", [("beta_spodumene", 1), ("hydrogen_chloride", 2)],
             [("lithium_chloride", 1), ("minecraft:sand", 1)], energy=300, ticks=200)
    # 3 куска LiCl (6.9 кг) → 1.13 кг Li = 1.9 слитка по 0.59 кг; Cl₂ — в скруббер
    chemical("lithium_electrolysis", "regolith_reactor", [("lithium_chloride", 3)], [("lithium_ingot", 2, 0.96)],
             kwh=35 * 1.13, ticks=300)
    # Монд: камасит 5–7 % Ni, тэнит до 35 % — в среднем ~8 %; CO — переносчик, возвращается
    chemical("mond", "sabatier_reactor", [("meteoric_iron", 1), ("carbon_monoxide", 1)],
             [("iron_dust", 1, 0.92), ("nickel_ingot", 1, 0.08), ("carbon_monoxide", 1, 0.95)],
             energy=200, ticks=200)
    # 2219: 6 Al (18 кг) + самородок меди (1.1 кг) → Cu 5.8 %
    assembly("al_cu_blend", ["aluminium_ingot"] * 6 + ["minecraft:copper_nugget"], "al_cu_blend")
    smelting("aluminium_copper_ingot", "al_cu_blend", "aluminium_copper_ingot", 6)
    # Al-Li: 7 Al + Cu + Li → Cu 4.9 %, Li 2.6 % (семейство Weldalite); угар и дросс ~7 %
    assembly("al_li_blend", ["aluminium_ingot"] * 7 + ["minecraft:copper_nugget", "lithium_ingot"], "al_li_blend")
    smelting("aluminium_lithium_ingot", "al_li_blend", "aluminium_lithium_ingot", 7)
    # γ′-сплав Ni₃(Al,Ti): 7 Ni + Al + Ti → Al 3.9 %, Ti 6.5 % (класс IN-100 без Cr/Co)
    assembly("superalloy_blend", ["nickel_ingot"] * 7 + ["aluminium_ingot", "titanium_ingot"], "superalloy_blend")
    smelting("nickel_superalloy_ingot", "superalloy_blend", "nickel_superalloy_ingot", 8)
    machining("machining", "turbopump_superalloy_1", "nickel_superalloy_ingot", 0, "incomplete_turbopump",
              energy_j=240000, components={f"{NS}:turbine_superalloy": 1})

    # --- блоки 006 ---
    assembly("chemical_reactor", ["steel_plate", "steel_plate", "hermetic_glass", "copper_wire", "relay_logic",
                                  "minecraft:cauldron"], "chemical_reactor")
    assembly("deposition_reactor", ["minecraft:quartz_block", "steel_plate", "steel_plate", "copper_plate",
                                    "copper_plate", "relay_logic", "hermetic_glass"], "deposition_reactor")
    assembly("crystal_puller", ["steel_plate", "steel_plate", "steel_shaft", "minecraft:quartz_block",
                                "copper_wire", "copper_wire", "refractory_lining", "relay_logic"], "crystal_puller")
    assembly("wafer_saw", ["steel_plate", "steel_plate", "steel_shaft", "minecraft:diamond", "relay_logic"],
             "wafer_saw")
    assembly("fan_filter_unit", ["steel_plate", "steel_plate", "glass_fiber", "glass_fiber", "glass_fiber",
                                 "glass_fiber", "copper_wire", "copper_wire", "minecraft:iron_ingot"],
             "fan_filter_unit")
    assembly("diffusion_furnace", ["minecraft:quartz_block", "refractory_lining", "refractory_lining",
                                   "copper_wire", "copper_wire", "steel_plate", "relay_logic"], "diffusion_furnace")
    assembly("lithography_station", ["steel_plate", "steel_plate", "hermetic_glass", "minecraft:glowstone_dust",
                                     "relay_logic", "relay_logic", "minecraft:iron_ingot"], "lithography_station")
    assembly("etch_bath", ["steel_plate", "steel_plate", "epoxy_resin", "epoxy_resin", "relay_logic"], "etch_bath")
    assembly("mono_solar_panel", ["mono_solar_cell"] * 4 + ["hermetic_glass", "steel_ingot", "copper_wire",
                                                            "copper_wire"], "mono_solar_panel")
    assembly("aluminium_fuel_tank", ["aluminium_copper_ingot", "aluminium_copper_ingot", "steel_ingot",
                                     "carbon_fiber"], "aluminium_fuel_tank")
    assembly("al_li_fuel_tank", ["aluminium_lithium_ingot", "aluminium_lithium_ingot", "steel_ingot",
                                 "carbon_fiber"], "al_li_fuel_tank")


def convert(name, replace, add=()):
    """Перевод рецепта сборки 002–005 на электронику: замены по одному вхождению."""
    path = os.path.join(DATA, "recipe", name + ".json")
    with open(path, encoding="utf-8") as f:
        obj = json.load(f)
    ing = obj["ingredients"]
    for old, new in replace:
        if sr(new) in ing and sr(old) not in ing:
            continue  # уже переведён
        ing[ing.index(sr(old))] = sr(new)
    for extra in add:
        if ing.count(sr(extra)) < add.count(extra):
            ing.append(sr(extra))
    assert len(ing) <= 9, name
    write(path, obj)


def conversions():
    path = os.path.join(DATA, "recipe", "assembly_flight_program.json")
    write(path, {"type": f"{NS}:assembly",
                 "ingredients": [sr("core_rope_memory"), "minecraft:paper", sr("steel_ingot")],
                 "result": result("flight_program")})
    convert("assembly_command_module", [], add=("relay_logic",))
    for n in ("assembly_leak_scanner", "assembly_telemetry_screen", "assembly_landing_beacon"):
        convert(n, [("minecraft:redstone", "relay_logic")])
    convert("assembly_frequency_key", [("minecraft:redstone", "logic_chip")])
    convert("assembly_targeting_designator", [("minecraft:redstone", "logic_chip"), ("minecraft:redstone", "logic_chip")])
    convert("assembly_interceptor_dish", [("minecraft:redstone", "microprocessor")])
    convert("assembly_cargo_terminal", [("minecraft:copper_ingot", "flight_computer")])
    convert("assembly_mission_control", [("minecraft:copper_ingot", "flight_computer"),
                                         ("minecraft:redstone", "circuit_board")])
    convert("assembly_satellite", [("minecraft:redstone", "flight_computer")])
    convert("assembly_power_satellite", [("solar_panel", "mono_solar_panel"), ("solar_panel", "mono_solar_panel"),
                                         ("solar_panel", "mono_solar_panel")], add=("radhard_processor",))


# ---------------------------------------------------------------------------
# Лут, мир, теги, массы
# ---------------------------------------------------------------------------

BLOCKS = ["chemical_reactor", "deposition_reactor", "crystal_puller", "wafer_saw", "fan_filter_unit",
          "diffusion_furnace", "lithography_station", "etch_bath", "mono_solar_panel", "aluminium_fuel_tank",
          "al_li_fuel_tank", "anorthosite"]
ORES = {"halite_ore": ("rock_salt", 2, 4), "fluorite_ore": ("fluorite", 1, 2), "spodumene_ore": ("spodumene", 1, 1)}


def loot():
    for b in BLOCKS:
        write(os.path.join(DATA, "loot_table", "blocks", b + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": sr(b)}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    for ore, (drop, lo, hi) in ORES.items():
        functions = []
        if hi > lo:
            functions.append({"function": "minecraft:set_count",
                              "count": {"type": "minecraft:uniform", "min": lo, "max": hi}})
        functions += [{"enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops",
                       "function": "minecraft:apply_bonus"}, {"function": "minecraft:explosion_decay"}]
        write(os.path.join(DATA, "loot_table", "blocks", ore + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [
                        {"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}], "name": sr(ore)},
                {"type": "minecraft:item", "functions": functions, "name": sr(drop)}]}]}],
            "random_sequence": f"{NS}:blocks/{ore}"})


def ore_feature(name, block, target, size, count, lo, hi, air=0.0):
    tgt = ({"predicate_type": "minecraft:tag_match", "tag": target[1:]} if target.startswith("#")
           else {"predicate_type": "minecraft:block_match", "block": target})
    write(os.path.join(DATA, "worldgen", "configured_feature", name + ".json"), {
        "type": "minecraft:ore", "config": {"discard_chance_on_air_exposure": air, "size": size,
                                            "targets": [{"state": {"Name": sr(block)}, "target": tgt}]}})
    write(os.path.join(DATA, "worldgen", "placed_feature", name + ".json"), {
        "feature": sr(name), "placement": [
            {"type": "minecraft:count", "count": count}, {"type": "minecraft:in_square"},
            {"type": "minecraft:height_range", "height": {"type": "minecraft:uniform",
                                                          "max_inclusive": {"absolute": hi},
                                                          "min_inclusive": {"absolute": lo}}},
            {"type": "minecraft:biome"}]})


def worldgen():
    # эвапориты — неглубокие пласты, большие тела; флюорит — гидротермальные жилы; сподумен — пегматиты
    ore_feature("ore_halite", "halite_ore", "#minecraft:stone_ore_replaceables", 24, 2, 20, 64)
    ore_feature("ore_fluorite", "fluorite_ore", "#minecraft:stone_ore_replaceables", 8, 3, -16, 48)
    ore_feature("ore_spodumene", "spodumene_ore", "#minecraft:stone_ore_replaceables", 6, 2, -32, 32)
    # анортозит нагорий Луны: массивы выше равнин морей
    ore_feature("moon_anorthosite", "anorthosite", f"{NS}:moon_stone", 48, 6, 55, 120)
    path = os.path.join(DATA, "worldgen", "biome", "moon_plains.json")
    with open(path, encoding="utf-8") as f:
        biome = json.load(f)
    for step in biome["features"]:
        if isinstance(step, list) and sr("moon_titanium") in step and sr("moon_anorthosite") not in step:
            step.append(sr("moon_anorthosite"))
    write(path, biome)


def tags():
    base = os.path.join(ROOT, "data", "minecraft", "tags", "block")
    for tag, blocks in (("mineable/pickaxe", BLOCKS + list(ORES)), ("needs_iron_tool", ["fluorite_ore", "spodumene_ore"]),
                        ("needs_stone_tool", ["halite_ore", "anorthosite"])):
        path = os.path.join(base, tag + ".json")
        obj = {"replace": False, "values": []}
        if os.path.exists(path):
            with open(path, encoding="utf-8") as f:
                obj = json.load(f)
        for b in blocks:
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)


# Масса предмета (кг): кусок/слиток = 1/9 блока 10 л × ρ; изделия — по реальным размерам
MASSES = {
    "silicon_006": (2.59, ["metallurgical_silicon", "polysilicon", "silicon_dust", "multicrystalline_silicon",
                           "silicon_blend"]),
    "silicon_boule": (0.69, ["silicon_boule"]),                      # Ø50 × 150 мм × 2.33 г/см³
    "wafers": (0.00126, ["silicon_wafer", "multicrystalline_wafer", "sos_wafer", "solar_cell", "mono_solar_cell"]),
    "sapphire_wafer": (0.00215, ["sapphire_wafer"]),
    "sapphire": (4.42, ["sapphire"]),
    "alumina": (4.39, ["alumina"]),
    "aluminium": (3.0, ["aluminium_ingot"]),
    "aluminium_copper": (3.16, ["aluminium_copper_ingot"]),
    "aluminium_lithium": (3.01, ["aluminium_lithium_ingot"]),
    "lithium": (0.59, ["lithium_ingot"]),
    "nickel": (9.89, ["nickel_ingot"]),
    "superalloy": (9.44, ["nickel_superalloy_ingot"]),
    "al_cu_blend": (19.1, ["al_cu_blend"]),
    "al_li_blend": (22.7, ["al_li_blend"]),
    "superalloy_blend": (77.2, ["superalloy_blend"]),
    "minerals_006": (3.5, ["fluorite", "spodumene", "cryolite"]),
    "beta_spodumene": (2.67, ["beta_spodumene"]),
    "salts": (2.4, ["rock_salt", "gypsum", "lithium_chloride"]),
    "phosphorus": (2.02, ["phosphorus", "phosphate_blend"]),
    "reagents": (1.2, ["brine", "hydrogen_chloride", "caustic_soda", "sulfuric_acid", "hydrofluoric_acid",
                       "trichlorosilane", "carbon_monoxide", "photoresist", "epoxy_resin"]),
    "crucible": (0.5, ["quartz_crucible"]),
    "electronics": (0.05, ["die_logic", "die_microprocessor", "die_radhard", "logic_chip", "microprocessor",
                           "radhard_processor", "ceramic_package", "relay", "ferrite_core", "photomask_logic",
                           "photomask_microprocessor", "photomask_radhard"]),
    "wiring": (1.24, ["copper_wire"]),                               # слиток меди 9.96 кг → 8 проводов
    "boards": (0.3, ["glass_fiber", "fr4_laminate", "copper_clad_laminate", "incomplete_circuit_board",
                     "circuit_board", "relay_logic", "core_rope_memory"]),
    "flight_computer": (8.0, ["flight_computer"]),
    "anorthosite": (27.3, ["anorthosite"]),
}


def masses():
    for name, (kg, items) in MASSES.items():
        write(os.path.join(DATA, NS, "item_mass", name + ".json"), {"items": [sr(i) for i in items], "kg": kg})


# ---------------------------------------------------------------------------
# Ресурсы: блок-стейты, модели, предметы
# ---------------------------------------------------------------------------

ITEMS = ["copper_wire", "relay", "relay_logic", "ferrite_core", "core_rope_memory", "silicon_blend",
         "metallurgical_silicon", "silicon_dust", "carbon_monoxide", "brine", "hydrogen_chloride", "caustic_soda",
         "trichlorosilane", "polysilicon", "silicon_boule", "multicrystalline_silicon", "multicrystalline_wafer",
         "silicon_wafer", "solar_cell", "mono_solar_cell", "rock_salt", "photoresist", "ceramic_package",
         "die_logic", "die_microprocessor", "die_radhard", "logic_chip", "microprocessor", "radhard_processor",
         "hydrofluoric_acid", "glass_fiber", "epoxy_resin", "fr4_laminate", "copper_clad_laminate",
         "incomplete_circuit_board", "circuit_board", "flight_computer", "fluorite", "cryolite", "alumina",
         "aluminium_ingot", "aluminium_copper_ingot", "spodumene", "lithium_chloride", "lithium_ingot",
         "aluminium_lithium_ingot", "nickel_ingot", "nickel_superalloy_ingot", "sapphire", "quartz_crucible",
         "phosphorus", "sulfuric_acid", "gypsum", "beta_spodumene", "sapphire_wafer", "sos_wafer",
         "photomask_logic", "photomask_microprocessor", "photomask_radhard", "al_cu_blend", "al_li_blend",
         "superalloy_blend", "phosphate_blend"]
PROCESS = ["chemical_reactor", "deposition_reactor", "diffusion_furnace", "lithography_station", "etch_bath"]
SIMPLE = ["halite_ore", "fluorite_ore", "spodumene_ore", "anorthosite", "fan_filter_unit"]


def model(name, obj):
    write(os.path.join(ASSETS, "models", "block", name + ".json"), obj)


def item_def(name, model_ref):
    write(os.path.join(ASSETS, "items", name + ".json"),
          {"model": {"type": "minecraft:model", "model": model_ref}})


def assets():
    for i in ITEMS:
        write(os.path.join(ASSETS, "models", "item", i + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}})
        item_def(i, f"{NS}:item/{i}")
    for b in SIMPLE:
        model(b, {"parent": "minecraft:block/cube_all", "textures": {"all": f"{NS}:block/{b}"}})
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {"": {"model": f"{NS}:block/{b}"}}})
        item_def(b, f"{NS}:block/{b}")
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for b in PROCESS:
        for lit in (False, True):
            suffix = "_on" if lit else ""
            model(b + suffix, {"parent": "minecraft:block/orientable", "textures": {
                "top": f"{NS}:block/{b}_top", "front": f"{NS}:block/{b}_front{suffix}",
                "side": f"{NS}:block/{b}_side"}})
        variants = {}
        for facing, y in rot.items():
            for lit in ("false", "true"):
                v = {"model": f"{NS}:block/{b}{'_on' if lit == 'true' else ''}"}
                if y:
                    v["y"] = y
                variants[f"facing={facing},lit={lit}"] = v
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": variants})
        item_def(b, f"{NS}:block/{b}")
    # кинетические машины 006 — столб по оси вала, как токарный
    for b in ("crystal_puller", "wafer_saw"):
        model(b, {"parent": "minecraft:block/cube_column",
                  "textures": {"end": f"{NS}:block/{b}_end", "side": f"{NS}:block/{b}_side"}})
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {
            "axis=x": {"model": f"{NS}:block/{b}", "x": 90, "y": 90}, "axis=y": {"model": f"{NS}:block/{b}"},
            "axis=z": {"model": f"{NS}:block/{b}", "x": 90}}})
        item_def(b, f"{NS}:block/{b}")
    # баки из алюминиевых сплавов — как титановый, со своим смотровым стеклом
    for b in ("aluminium_fuel_tank", "al_li_fuel_tank"):
        variants = {}
        for lvl in range(5):
            model(f"{b}_{lvl}", {"parent": "minecraft:block/cube_column", "textures": {
                "side": f"{NS}:block/{b}_side_{lvl}", "end": f"{NS}:block/{b}_top"}})
            variants[f"level={lvl}"] = {"model": f"{NS}:block/{b}_{lvl}"}
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": variants})
        item_def(b, f"{NS}:block/{b}_0")
    # монокристаллическая панель: та же рама и трекер, другие элементы
    with open(os.path.join(ASSETS, "models", "block", "solar_panel.json"), encoding="utf-8") as f:
        panel = json.load(f)
    text = json.dumps(panel).replace(f"{NS}:block/solar_panel_top", f"{NS}:block/mono_solar_panel_top")
    model("mono_solar_panel", json.loads(text))
    write(os.path.join(ASSETS, "blockstates", "mono_solar_panel.json"),
          {"variants": {"": {"model": f"{NS}:block/mono_solar_panel"}}})
    item_def("mono_solar_panel", f"{NS}:block/mono_solar_panel")
    with open(os.path.join(ASSETS, "models", "block", "rotor_solar_array.json"), encoding="utf-8") as f:
        array = json.load(f)
    text = json.dumps(array).replace(f"{NS}:block/solar_panel_top", f"{NS}:block/mono_solar_panel_top")
    model("rotor_mono_solar_array", json.loads(text))
    write(os.path.join(ASSETS, "blockstates", "rotor_mono_solar_array.json"),
          {"variants": {"": {"model": f"{NS}:block/rotor_mono_solar_array"}}})


def main():
    recipes()
    conversions()
    loot()
    worldgen()
    tags()
    masses()
    assets()


if __name__ == "__main__":
    main()
