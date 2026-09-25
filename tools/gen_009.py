#!/usr/bin/env python3
"""Данные и ресурсы 009 «Навигация»: орбиты в профилях планет, сигнатуры минералов, диэлектрики,
рецепты, модели, достижения. Идемпотентен.
Запуск: python3 tools/gen_009.py (после gen_008.py)."""
import json
import os

import gen_006 as g
import modelkit as mk
from gen_006 import DATA, NS, assembly, sr, write

PLANETS = os.path.join(DATA, NS, "planets")

# --- US1: орбиты (JPL, Standish, табл. 1; Церера — JPL SBDB, L₀ пересчитана к J2000) ---------------
EARTH = {"a_au": 1.00000261, "e": 0.01671123, "i_deg": -0.00001531, "node_deg": 0.0,
         "peri_deg": 102.93768193, "mean_lon_deg": 100.46457166}
MARS = {"a_au": 1.52371034, "e": 0.09339410, "i_deg": 1.84969142, "node_deg": 49.55953891,
        "peri_deg": -23.94362959, "mean_lon_deg": -4.55343205}
CERES = {"a_au": 2.7675, "e": 0.0758, "i_deg": 10.59, "node_deg": 80.3, "peri_deg": 153.9, "mean_lon_deg": 160.7}

ORBITS = {
    # орбита Земли — парковка 200 км над Землёй; прибытие с Марса и пояса — аэрозахватом (003)
    # (park_radius, а не body_radius: радиус тела включил бы катапульту на платформе)
    "earth_orbit": {"orbit": EARTH, "mu": 3.986004418e14, "park_radius": 6571000.0, "aerocapture": True},
    "mars": {"orbit": MARS, "mu": 4.282837e13, "aerocapture": True},
    "asteroid_belt": {"orbit": CERES, "mu": 6.26e10, "aerocapture": False},
}
# окно-расписание Марса заменено небесной механикой (D91): поля уходят из профиля
DROP = {"mars": ["synodic_period_ticks", "window_width_ticks", "window_phase_ticks"],
        "earth_orbit": ["body_radius", "parking_altitude"]}


def planets():
    for name, extra in ORBITS.items():
        path = os.path.join(PLANETS, f"{name}.json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for key in DROP.get(name, []):
            data.pop(key, None)
        data.update(extra)
        write(path, data)


# --- US3: гиперспектральный спутник -----------------------------------------------------------------
# PbS — первый ИК-фотоприёмник (Kiel, 1940-е): природный галенит чувствителен на 1–3 мкм без охлаждения.
# Кремниевая линейка 007 слепа дальше 1.1 мкм, поэтому спектрометру нужен свой приёмник.
ITEMS = ["galena", "pbs_detector", "diffraction_grating", "ir_spectrometer", "mineral_map", "ground_radar", "radargram"]
ORES = {"galena_ore": ("galena", 1, 2)}
BLOCKS = ["hyperspectral_satellite"]

# Сигнатуры: только минералы с диагностическими полосами 0.4–2.5 мкм (M³, CRISM, «Клементина» UV/VIS).
# map_color_id — индекс MapColor (ванильная палитра карт).
SPECTRAL = {
    "water_ice": (["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", "minecraft:snow_block",
                   "minecraft:snow", "minecraft:powder_snow", sr("moon_ice"), sr("mars_ice")], 17, 1.5),
    "vegetation": (["minecraft:grass_block", "#minecraft:leaves", "minecraft:moss_block", "minecraft:moss_carpet"], 27, 0.7),
    "iron_oxide": (["minecraft:red_sand", "minecraft:red_sandstone", "minecraft:iron_ore", "minecraft:raw_iron_block"], 28, 0.86),
    "clay": (["minecraft:clay", "#minecraft:terracotta", "minecraft:mud"], 15, 2.2),
    "carbonate": (["minecraft:calcite", "minecraft:dripstone_block"], 14, 2.33),
    "kerogen": ([sr("oil_shale")], 26, 2.3),
    "borate": ([sr("borax_ore")], 18, 1.9),
    "ilmenite": ([sr("titanium_ore"), sr("deepslate_titanium_ore"), sr("moon_titanium_ore")], 24, 0.4),
    "plagioclase": ([sr("anorthosite")], 8, 1.25),
    # свежие мафические породы (стенки кратеров); зрелый реголит — без полос: космическое выветривание
    "pyroxene": ([sr("moon_stone"), "minecraft:basalt", "minecraft:smooth_basalt"], 21, 1.0),
}

# --- US4: диэлектрики для георадара (ε, tan δ при 500 МГц) ------------------------------------------
# Lunar Sourcebook (реголит ε ≈ 1.9^ρ, ρ 1.5 г/см³ → ≈ 3), Mars (MARSIS/SHARAD: 3–4), лёд 3.15 с tan δ 5·10⁻⁴,
# влажная почва 15–25 с tan δ 0.1 (Daniels, «Ground Penetrating Radar», 2004); металлическая руда — сильный отражатель.
DIELECTRIC = {
    "dry_regolith": ([sr("moon_regolith"), sr("sintered_regolith"), "minecraft:red_sand", "minecraft:sand",
                      "minecraft:gravel"], 3.0, 0.005),
    "basalt": ([sr("moon_stone"), sr("asteroid_stone"), "minecraft:basalt", "minecraft:smooth_basalt",
                "minecraft:red_sandstone", "minecraft:sandstone"], 6.0, 0.01),
    "anorthosite": ([sr("anorthosite")], 5.0, 0.005),
    "ice": (["minecraft:ice", "minecraft:packed_ice", "minecraft:blue_ice", sr("moon_ice"), sr("mars_ice")], 3.15, 0.0005),
    "snow": (["minecraft:snow_block", "minecraft:powder_snow"], 1.8, 0.001),
    "wet_soil": (["minecraft:dirt", "minecraft:grass_block", "minecraft:coarse_dirt", "minecraft:rooted_dirt",
                  "minecraft:mud", "minecraft:clay", "minecraft:podzol"], 15.0, 0.1),
    "granite": (["minecraft:stone", "minecraft:granite", "minecraft:diorite", "minecraft:andesite",
                 "minecraft:deepslate", "minecraft:tuff"], 6.0, 0.02),
    "ore_metal": ([sr("moon_titanium_ore"), sr("titanium_ore"), sr("deepslate_titanium_ore"), sr("mars_tungsten_ore"),
                   "minecraft:iron_ore", "minecraft:deepslate_iron_ore", sr("galena_ore")], 30.0, 0.1),
    "water": (["minecraft:water"], 80.0, 0.5),
}


def survey_recipes():
    assembly("pbs_detector", ["galena", "galena", "ceramic_package", "copper_wire"], "pbs_detector")
    # голографическая решётка: фоторезист на стекле, алюминиевое напыление
    assembly("diffraction_grating", ["hermetic_glass", "aluminium_ingot"], "diffraction_grating")
    assembly("ir_spectrometer", ["diffraction_grating", "pbs_detector", "logic_chip", "steel_plate"], "ir_spectrometer")
    assembly("hyperspectral_satellite", ["aluminium_lithium_ingot", "solar_panel", "solar_panel", "flight_computer",
                                         "telescope_mirror", "ir_spectrometer", "radhard_processor"],
             "hyperspectral_satellite")
    assembly("mineral_map", ["minecraft:map", "minecraft:paper"], "mineral_map")
    # георадар: две «бабочки» из меди, импульсный генератор и стробоскопический приёмник
    assembly("ground_radar", ["copper_plate", "copper_plate", "logic_chip", "circuit_board", "steel_plate"], "ground_radar")


MASSES = {
    "galena": (8.4, ["galena"]),                         # 1/9 блока 10 л × 7.6 г/см³
    "survey_parts": (0.3, ["pbs_detector", "diffraction_grating"]),
    "ir_spectrometer": (3.0, ["ir_spectrometer"]),
    "hyperspectral_satellite": (130.0, ["hyperspectral_satellite"]),   # класс 100 кг + спектрометр
    "ground_radar": (3.0, ["ground_radar"]),             # RIMFAX ≈ 3 кг
    "maps_009": (0.02, ["mineral_map", "radargram"]),
}


def data():
    for name, (blocks, color, band) in SPECTRAL.items():
        write(os.path.join(DATA, NS, "spectral", name + ".json"),
              {"blocks": blocks, "mineral": name, "map_color_id": color, "band_um": band})
    for name, (blocks, eps, tan) in DIELECTRIC.items():
        write(os.path.join(DATA, NS, "dielectric", name + ".json"), {"blocks": blocks, "epsilon": eps, "loss_tangent": tan})
    for name, (drop, lo, hi) in ORES.items():
        write(os.path.join(DATA, "loot_table", "blocks", name + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1.0, "entries": [{"type": "minecraft:alternatives", "children": [
                {"type": "minecraft:item", "conditions": [{"condition": "minecraft:match_tool", "predicate": {
                    "predicates": {"minecraft:enchantments": [{"enchantments": "minecraft:silk_touch", "levels": {"min": 1}}]}}}],
                 "name": sr(name)},
                {"type": "minecraft:item", "name": sr(drop), "functions": [
                    {"function": "minecraft:set_count", "count": {"type": "minecraft:uniform", "min": lo, "max": hi}},
                    {"enchantment": "minecraft:fortune", "formula": "minecraft:ore_drops", "function": "minecraft:apply_bonus"},
                    {"function": "minecraft:explosion_decay"}]}]}]}]})
    # галенит — гидротермальные жилы (часто с флюоритом)
    g.ore_feature("ore_galena", "galena_ore", "#minecraft:stone_ore_replaceables", 8, 3, -16, 64)
    for b in BLOCKS:
        write(os.path.join(DATA, "loot_table", "blocks", b + ".json"), {
            "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": sr(b)}],
                                                  "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(DATA, NS, "part_properties", "hyperspectral_satellite.json"),
          {"block": sr("hyperspectral_satellite"), "mass_kg": 130.0, "role": "hull"})
    base = os.path.join(os.path.dirname(DATA), "minecraft", "tags", "block")
    for path, blocks in ((os.path.join(DATA, "tags", "block", "rocket_parts.json"), BLOCKS),
                         (os.path.join(base, "mineable", "pickaxe.json"), BLOCKS + list(ORES)),
                         (os.path.join(base, "needs_stone_tool.json"), list(ORES))):
        with open(path, encoding="utf-8") as f:
            obj = json.load(f)
        for b in blocks:
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)
    for name, (kg, items) in MASSES.items():
        write(os.path.join(DATA, NS, "item_mass", name + ".json"), {"items": [sr(i) for i in items], "kg": kg})


def models():
    for i in ITEMS:
        write(os.path.join(g.ASSETS, "models", "item", i + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}})
        g.item_def(i, f"{NS}:item/{i}")
    for o in ORES:
        mk.plain(o, "minecraft:block/cube_all", {"all": f"{NS}:block/{o}"})
        mk.blockstate(o, {"": {"model": f"{NS}:block/{o}"}})
        mk.item(o, f"{NS}:block/{o}")
    # гиперспектральный спутник: шина, телескоп, короб спектрометра с радиатором, две панели
    mk.model("hyperspectral_satellite", {"bus": f"{NS}:block/hyperspectral_bus", "tube": f"{NS}:block/imaging_satellite_tube",
                                         "lens": f"{NS}:block/imaging_satellite_lens", "frame": f"{NS}:block/satellite",
                                         "panel": f"{NS}:block/solar_panel_top", "box": f"{NS}:block/spectrometer_box"}, [
        mk.box([5, 0, 5], [11, 8, 11], "#bus"),
        mk.box([6, 8, 6], [10, 16, 10], "#tube", over={"up": "#lens"}),
        mk.box([11, 2, 6], [14, 7, 10], "#box", skip=("west",)),
        mk.box([3, 4, 7.5], [5, 5, 8.5], "#frame", skip=("east",)),
        mk.box([0, 1, 7.25], [3, 9, 8.75], "#frame", over={"north": "#panel", "south": "#panel"}),
        mk.box([14, 4, 7.5], [16, 5, 8.5], "#frame", skip=("west",)),
    ])
    mk.blockstate("hyperspectral_satellite", {"": {"model": f"{NS}:block/hyperspectral_satellite"}})
    mk.item("hyperspectral_satellite", f"{NS}:block/hyperspectral_satellite")


ADVANCEMENTS = {  # id: (иконка, родитель, рамка)
    "celestial_mechanics": ("hyperspectral_satellite", "mars", "challenge"),
    "spectrum": ("hyperspectral_satellite", "view_from_above", "goal"),
    "below_ground": ("ground_radar", "first_track", "task"),
}


def advancements():
    for a, (icon, parent, frame) in ADVANCEMENTS.items():
        write(os.path.join(DATA, "advancement", a + ".json"), {
            "criteria": {"done": {"trigger": "minecraft:impossible"}},
            "display": {"icon": {"id": sr(icon)},
                        "title": {"translate": f"advancements.{NS}.{a}.title"},
                        "description": {"translate": f"advancements.{NS}.{a}.description"},
                        "frame": frame},
            "requirements": [["done"]], "sends_telemetry_event": False, "parent": sr(parent)})


def main():
    planets()
    advancements()
    survey_recipes()
    data()
    models()


if __name__ == "__main__":
    main()
