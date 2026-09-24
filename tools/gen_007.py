#!/usr/bin/env python3
"""Данные и ресурсы 007 «Жизнь на станции»: рецепты, лут, теги, массы, блок-стейты, модели.
Идемпотентен. Запуск: python3 tools/gen_007.py (после gen_006.py)."""
import json
import os

import gen_006 as g
from gen_006 import ASSETS, DATA, NS, assembly, chemical, sr, write

PROCESS = ["co2_scrubber", "biomass_oxidizer"]
MACHINES = ["air_separator", "airlock_pump", "rover_charger"]
ITEMS = ["lithium_hydroxide", "lioh_cartridge", "zeolite", "zeolite_bed", "straw", "rover_chassis", "rover_wheel",
         "nife_battery", "telescope_mirror", "image_sensor", "orbital_image"]
BLOCKS = PROCESS + MACHINES + ["gas_tank", "hydroponic_tray", "grow_lamp", "spin_hub", "rim_thruster", "despin_motor"]

# Культуры NASA BVAD (табл. 4-89…4-91), fresh_factor — сырая масса урожая на сухую
CROPS = {
    "wheat": {"seed": "minecraft:wheat_seeds", "harvest": "minecraft:wheat", "byproduct": f"{NS}:straw",
              "dli": 115, "cycle_days": 80, "edible_g_m2_day": 20.0, "harvest_index": 0.40,
              "o2_g_m2_day": 56.0, "co2_g_m2_day": 77.0, "water_kg_m2_day": 11.8, "fresh_factor": 1.14},
    "potato": {"seed": "minecraft:potato", "harvest": "minecraft:potato", "byproduct": f"{NS}:straw",
               "dli": 28, "cycle_days": 132, "edible_g_m2_day": 21.1, "harvest_index": 0.70,
               "o2_g_m2_day": 32.2, "co2_g_m2_day": 45.2, "water_kg_m2_day": 4.0, "fresh_factor": 5.0},
}


def recipes():
    assembly("gas_tank", ["steel_plate"] * 4 + ["carbon_fiber", "carbon_fiber"], "gas_tank")
    assembly("air_separator", ["steel_plate", "steel_plate", "copper_plate", "copper_plate", "steel_shaft",
                               "relay_logic", "glass_fiber"], "air_separator")
    assembly("airlock_pump", ["steel_plate", "steel_plate", "steel_shaft", "copper_wire", "copper_wire", "relay"],
             "airlock_pump")
    assembly("co2_scrubber", ["steel_plate", "steel_plate", "copper_wire", "glass_fiber", "glass_fiber", "relay"],
             "co2_scrubber")
    # каустификация: LiCl + NaOH → LiOH + NaCl
    chemical("lithium_hydroxide", "chemical_reactor", [("lithium_chloride", 1), ("caustic_soda", 1)],
             [("lithium_hydroxide", 1), ("rock_salt", 1)], energy=150, ticks=160)
    assembly("lioh_cartridge", ["lithium_hydroxide", "lithium_hydroxide", "steel_plate", "glass_fiber"],
             "lioh_cartridge")
    # цеолит 5A: гидротермальный синтез алюмосиликата натрия (100 °C)
    chemical("zeolite", "chemical_reactor", [("alumina", 1), ("minecraft:sand", 1), ("caustic_soda", 1)],
             [("zeolite", 4)], energy=300, ticks=240)
    assembly("zeolite_bed", ["zeolite"] * 4 + ["steel_plate", "copper_wire"], "zeolite_bed")
    assembly("hydroponic_tray", ["steel_plate", "steel_plate", "glass_fiber", "minecraft:bucket"], "hydroponic_tray", 2)
    assembly("grow_lamp", ["minecraft:glowstone_dust", "minecraft:glowstone_dust", "copper_wire", "hermetic_glass",
                           "steel_plate"], "grow_lamp")
    assembly("spin_hub", ["steel_plate", "steel_plate", "steel_shaft", "steel_shaft", "large_gear", "steel_ingot"],
             "spin_hub")
    assembly("rim_thruster", ["tungsten_ingot", "steel_plate", "copper_wire", "relay"], "rim_thruster")
    assembly("despin_motor", ["motor", "gearbox", "steel_plate", "steel_plate", "relay_logic"], "despin_motor")
    assembly("rover_chassis", ["aluminium_copper_ingot"] * 4 + ["relay_logic", "steel_plate", "rocket_seat",
                                                                   "rocket_seat"], "rover_chassis")
    assembly("rover_wheel", ["aluminium_copper_ingot", "copper_wire", "copper_wire", "steel_plate", "minecraft:iron_ingot"],
             "rover_wheel")
    assembly("nife_battery", ["nickel_ingot", "nickel_ingot", "minecraft:iron_ingot", "minecraft:iron_ingot",
                              "caustic_soda", "steel_plate"], "nife_battery")
    assembly("rover_charger", ["steel_plate", "steel_plate", "copper_wire", "copper_wire", "relay_logic"], "rover_charger")
    # US5: Кассегрен Ø 10 см — стекло с алюминиевым напылением; линейка ПЗС 10⁴ пикселей на пластине
    assembly("telescope_mirror", ["hermetic_glass", "hermetic_glass", "aluminium_ingot"], "telescope_mirror")
    assembly("image_sensor", ["silicon_wafer", "ceramic_package", "logic_chip", "copper_wire"], "image_sensor")
    assembly("imaging_satellite", ["aluminium_lithium_ingot", "solar_panel", "solar_panel", "flight_computer",
                                   "telescope_mirror", "image_sensor", "radhard_processor"], "imaging_satellite")
    assembly("biomass_oxidizer", ["steel_plate", "steel_plate", "refractory_lining", "copper_wire", "relay"],
             "biomass_oxidizer")


MASSES = {
    "gas_tank": (120.0, ["gas_tank"]),                 # композитный баллон 1 м³ на 20 МПа, пустой
    "lithium_hydroxide": (1.62, ["lithium_hydroxide"]),  # 1/9 блока 10 л × 1.46
    "lioh_cartridge": (1.2, ["lioh_cartridge"]),
    "zeolite": (2.2, ["zeolite"]),
    "zeolite_bed": (12.0, ["zeolite_bed"]),
    "life_machines": (150.0, ["air_separator", "airlock_pump", "co2_scrubber", "biomass_oxidizer"]),
    "greenhouse": (40.0, ["hydroponic_tray"]),
    "grow_lamp": (6.0, ["grow_lamp"]),
    "spin_hub": (400.0, ["spin_hub"]),        # стальная ступица с опорно-поворотным подшипником
    "rim_thruster": (15.0, ["rim_thruster"]),
    "despin_motor": (250.0, ["despin_motor"]),
    # LRV: 210 кг пустой (с Ag–Zn 60 кг); шасси с сиденьями и электроникой ~102 кг, мотор-колесо 12 кг
    "rover_chassis": (102.0, ["rover_chassis"]),
    "rover_wheel": (12.0, ["rover_wheel"]),
    "nife_battery": (350.0, ["nife_battery"]),           # 8.7 кВт·ч при 25 Вт·ч/кг
    "rover_charger": (60.0, ["rover_charger"]),
    # US5: малый спутник ДЗЗ класса 100 кг (оптика 10 см, как у Dove/SkySat-мини)
    "imaging_satellite": (110.0, ["imaging_satellite"]),
    "telescope_optics": (1.5, ["telescope_mirror", "image_sensor"]),
    "crops_007": (0.5, ["minecraft:wheat", f"{NS}:straw"]),       # сноп/охапка 0.5 кг
    "potato": (0.2, ["minecraft:potato"]),                         # один клубень
}


SOILS = {
    # Lunar Sourcebook, табл. 9.14; Carrier 2006
    "moon": {"dimension": f"{NS}:moon", "n": 1.0, "kc": 0.14, "kphi": 0.82, "c": 0.017, "phi_deg": 35, "k_cm": 1.78,
             "surface": f"{NS}:moon_regolith"},
    # Марс: рыхлый наносный песок (оценка по данным MER/Pathfinder)
    "mars": {"dimension": f"{NS}:mars", "n": 1.0, "kc": 0.068, "kphi": 0.82, "c": 0.02, "phi_deg": 30, "k_cm": 1.8,
             "surface": "minecraft:red_sand"},
    # Земля: сухой песок (Wong), переведено в Н и см
    "earth": {"dimension": "minecraft:overworld", "n": 1.1, "kc": 0.0625, "kphi": 0.964, "c": 0.104, "phi_deg": 28,
              "k_cm": 2.5},
}


def rover_models():
    faces = ("up", "down", "north", "south", "east", "west")
    def box(f, t, tex):
        return {"from": f, "to": t, "faces": {x: {"texture": tex} for x in faces}}
    body = {"parent": "minecraft:block/block", "textures": {"particle": f"{NS}:block/rover_body", "frame": f"{NS}:block/rover_body",
                                                             "seat": f"{NS}:block/rover_seat", "panel": f"{NS}:block/rover_panel"},
            "elements": [box([-8, 5, -16], [24, 8, 32], "#frame"),
                         box([-3, 8, 0], [6, 11, 9], "#seat"), box([-3, 11, 0], [6, 20, 2], "#seat"),
                         box([10, 8, 0], [19, 11, 9], "#seat"), box([10, 11, 0], [19, 20, 2], "#seat"),
                         box([2, 8, 22], [14, 18, 26], "#panel"),
                         box([20, 8, -12], [21, 30, -11], "#frame"), box([15, 30, -16], [26, 31, -7], "#panel")]}
    g.model("rover_body_model", body)
    wheel = {"parent": "minecraft:block/block", "textures": {"particle": f"{NS}:block/rover_wheel", "tire": f"{NS}:block/rover_wheel",
                                                              "hub": f"{NS}:block/rover_wheel_hub"},
             "elements": [{"from": [6, 1.5, 1.5], "to": [10, 14.5, 14.5], "faces": {
                 "east": {"texture": "#hub"}, "west": {"texture": "#hub"}, "north": {"texture": "#tire"},
                 "south": {"texture": "#tire"}, "up": {"texture": "#tire"}, "down": {"texture": "#tire"}}},
                          {"from": [6.1, 1.5, 1.5], "to": [9.9, 14.5, 14.5], "rotation": {"origin": [8, 8, 8], "axis": "x", "angle": 45},
                           "faces": {"north": {"texture": "#tire"}, "south": {"texture": "#tire"}, "up": {"texture": "#tire"},
                                     "down": {"texture": "#tire"}}}]}
    g.model("rover_wheel_model", wheel)
    g.model("rover_battery_model", {"parent": "minecraft:block/block", "textures": {"particle": f"{NS}:block/rover_battery",
                                                                                     "b": f"{NS}:block/rover_battery"},
                                    "elements": [box([0, 8, -14], [16, 14, -3], "#b")]})
    for b in ("rover_body_model", "rover_wheel_model", "rover_battery_model"):
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {"": {"model": f"{NS}:block/{b}"}}})


def data():
    for name, obj in SOILS.items():
        write(os.path.join(DATA, NS, "soils", name + ".json"), obj)
    for crop, obj in CROPS.items():
        write(os.path.join(DATA, NS, "crops", crop + ".json"), obj)
    write(os.path.join(DATA, "tags", "item", "biomass.json"),
          {"replace": False, "values": [f"{NS}:straw", "minecraft:short_grass", "minecraft:kelp", "minecraft:hay_block"]})
    for b in BLOCKS:
        write(os.path.join(DATA, "loot_table", "blocks", b + ".json"), {
            "type": "minecraft:block",
            "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": sr(b), "functions": [
                {"function": "minecraft:copy_components", "source": "block_entity",
                 "include": [f"{NS}:gas_kind", f"{NS}:gas_kg"]}]}],
                       "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    write(os.path.join(DATA, "loot_table", "blocks", "imaging_satellite.json"), {
        "type": "minecraft:block", "pools": [{"rolls": 1, "entries": [{"type": "minecraft:item", "name": sr("imaging_satellite")}],
                                              "conditions": [{"condition": "minecraft:survives_explosion"}]}]})
    # спутник-камера — деталь ракеты (полезная нагрузка), масса как у малого ДЗЗ-аппарата
    write(os.path.join(DATA, NS, "part_properties", "imaging_satellite.json"),
          {"block": sr("imaging_satellite"), "mass_kg": 110.0, "role": "hull"})
    path = os.path.join(DATA, "tags", "block", "rocket_parts.json")
    with open(path, encoding="utf-8") as f:
        obj = json.load(f)
    if sr("imaging_satellite") not in obj["values"]:
        obj["values"].append(sr("imaging_satellite"))
    write(path, obj)
    base = os.path.join(os.path.dirname(DATA), "minecraft", "tags", "block")
    for tag, blocks in (("mineable/pickaxe", BLOCKS + ["imaging_satellite"]),):
        path = os.path.join(base, tag + ".json")
        with open(path, encoding="utf-8") as f:
            obj = json.load(f)
        for b in blocks:
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)
    for tag in ("airtight", "energy_connectable"):
        path = os.path.join(DATA, "tags", "block", tag + ".json")
        with open(path, encoding="utf-8") as f:
            obj = json.load(f)
        for b in BLOCKS:
            if tag == "energy_connectable" and b == "gas_tank":
                continue
            if sr(b) not in obj["values"]:
                obj["values"].append(sr(b))
        write(path, obj)
    # рыхлый грунт под колесом ровера: механика Беккера по грунту тела; остальное — твёрдая поверхность
    write(os.path.join(DATA, "tags", "block", "loose_soil.json"), {"replace": False, "values": [
        "#minecraft:sand", "minecraft:gravel", "minecraft:dirt", "minecraft:coarse_dirt", "minecraft:grass_block",
        "minecraft:podzol", "minecraft:mud", "minecraft:snow_block", sr("moon_regolith")]})
    for name, (kg, items) in MASSES.items():
        write(os.path.join(DATA, NS, "item_mass", name + ".json"), {"items": [sr(i) for i in items], "kg": kg})


def assets():
    for i in ITEMS:
        write(os.path.join(ASSETS, "models", "item", i + ".json"),
              {"parent": "minecraft:item/generated", "textures": {"layer0": f"{NS}:item/{i}"}})
        g.item_def(i, f"{NS}:item/{i}")
    rot = {"north": 0, "east": 90, "south": 180, "west": 270}
    for b in PROCESS:
        for lit in (False, True):
            suffix = "_on" if lit else ""
            g.model(b + suffix, {"parent": "minecraft:block/orientable", "textures": {
                "top": f"{NS}:block/{b}_top", "front": f"{NS}:block/{b}_front{suffix}", "side": f"{NS}:block/{b}_side"}})
        variants = {}
        for facing, y in rot.items():
            for lit in ("false", "true"):
                v = {"model": f"{NS}:block/{b}{'_on' if lit == 'true' else ''}"}
                if y:
                    v["y"] = y
                variants[f"facing={facing},lit={lit}"] = v
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": variants})
        g.item_def(b, f"{NS}:block/{b}")
    tray_models()
    rover_models()
    imaging_model()
    for b in ("spin_hub", "despin_motor"):
        g.model(b, {"parent": "minecraft:block/cube_column",
                    "textures": {"end": f"{NS}:block/{b}_end", "side": f"{NS}:block/{b}_side"}})
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {
            "axis=x": {"model": f"{NS}:block/{b}", "x": 90, "y": 90}, "axis=z": {"model": f"{NS}:block/{b}", "x": 90}}})
        g.item_def(b, f"{NS}:block/{b}")
    g.model("rim_thruster", {"parent": "minecraft:block/orientable_vertical",
                             "textures": {"front": f"{NS}:block/rim_thruster_front", "side": f"{NS}:block/rim_thruster_side"}})
    rot = {"north": (0, 0), "east": (0, 90), "south": (0, 180), "west": (0, 270), "up": (270, 0), "down": (90, 0)}
    # orientable_vertical смотрит лицом вверх: поворот к нужной грани
    rv = {"up": (0, 0), "down": (180, 0), "north": (90, 0), "south": (90, 180), "east": (90, 90), "west": (90, 270)}
    variants = {}
    for facing, (x, y) in rv.items():
        v = {"model": f"{NS}:block/rim_thruster"}
        if x:
            v["x"] = x
        if y:
            v["y"] = y
        variants[f"facing={facing}"] = v
    write(os.path.join(ASSETS, "blockstates", "rim_thruster.json"), {"variants": variants})
    g.item_def("rim_thruster", f"{NS}:block/rim_thruster")
    lamp = {"parent": "minecraft:block/block", "textures": {"particle": f"{NS}:block/grow_lamp", "panel": f"{NS}:block/grow_lamp"},
            "elements": [{"from": [1, 12, 1], "to": [15, 16, 15], "faces": {f: {"texture": "#panel"} for f in
                                                                          ("up", "down", "north", "south", "east", "west")}}]}
    g.model("grow_lamp", lamp)
    lamp_on = json.loads(json.dumps(lamp).replace(f"{NS}:block/grow_lamp\"", f"{NS}:block/grow_lamp_on\""))
    g.model("grow_lamp_on", lamp_on)
    write(os.path.join(ASSETS, "blockstates", "grow_lamp.json"), {"variants": {
        "lit=false": {"model": f"{NS}:block/grow_lamp"}, "lit=true": {"model": f"{NS}:block/grow_lamp_on"}}})
    g.item_def("grow_lamp", f"{NS}:block/grow_lamp")
    for b in MACHINES:
        g.model(b, {"parent": "minecraft:block/cube_column",
                    "textures": {"end": f"{NS}:block/{b}_top", "side": f"{NS}:block/{b}_side"}})
        write(os.path.join(ASSETS, "blockstates", b + ".json"), {"variants": {"": {"model": f"{NS}:block/{b}"}}})
        g.item_def(b, f"{NS}:block/{b}")
    variants = {}
    for gas in ("none", "oxygen", "nitrogen"):
        for fill in range(5):
            name = f"gas_tank_{gas}_{fill}"
            g.model(name, {"parent": "minecraft:block/cube_column", "textures": {
                "side": f"{NS}:block/gas_tank_{gas}_{fill}", "end": f"{NS}:block/gas_tank_top"}})
            variants[f"fill={fill},gas={gas}"] = {"model": f"{NS}:block/{name}"}
    write(os.path.join(ASSETS, "blockstates", "gas_tank.json"), {"variants": variants})
    g.item_def("gas_tank", f"{NS}:block/gas_tank_none_0")


def imaging_model():
    """Спутник-камера: шина в золотистой ЭВТИ, бленда телескопа сверху, два крыла солнечных батарей.
    Все элементы — отдельные объёмы без общих плоскостей (без z-fighting)."""
    faces = ("up", "down", "north", "south", "east", "west")
    def box(f, t, tex, **over):
        return {"from": f, "to": t, "faces": {x: {"texture": over.get(x, tex)} for x in faces}}
    g.model("imaging_satellite", {"parent": "minecraft:block/block", "textures": {
        "particle": f"{NS}:block/imaging_satellite", "bus": f"{NS}:block/imaging_satellite",
        "tube": f"{NS}:block/imaging_satellite_tube", "lens": f"{NS}:block/imaging_satellite_lens",
        "panel": f"{NS}:block/solar_panel_top", "frame": f"{NS}:block/satellite"},
        "elements": [box([5, 0, 5], [11, 9, 11], "#bus"),
                     box([6, 9, 6], [10, 16, 10], "#tube", up="#lens"),
                     box([3, 5, 7.5], [5, 6, 8.5], "#frame"), box([11, 5, 7.5], [13, 6, 8.5], "#frame"),
                     box([0, 1.5, 7.25], [3, 9.5, 8.75], "#frame", north="#panel", south="#panel"),
                     box([13, 1.5, 7.25], [16, 9.5, 8.75], "#frame", north="#panel", south="#panel")]})
    write(os.path.join(ASSETS, "blockstates", "imaging_satellite.json"),
          {"variants": {"": {"model": f"{NS}:block/imaging_satellite"}}})
    g.item_def("imaging_satellite", f"{NS}:block/imaging_satellite")


def tray_models():
    faces = ("up", "down", "north", "south", "east", "west")
    tray = [{"from": [0, 0, 0], "to": [16, 8, 16], "faces": {f: {"texture": "#tray" if f != "up" else "#top"} for f in faces}}]
    stages = {"wheat": ["wheat_stage1", "wheat_stage3", "wheat_stage5", "wheat_stage7"],
              "potato": ["potatoes_stage0", "potatoes_stage1", "potatoes_stage2", "potatoes_stage3"],
              "other": ["wheat_stage1", "wheat_stage3", "wheat_stage5", "wheat_stage7"]}
    variants = {}
    base = {"parent": "minecraft:block/block", "textures": {"particle": f"{NS}:block/hydroponic_tray",
                                                            "tray": f"{NS}:block/hydroponic_tray",
                                                            "top": f"{NS}:block/hydroponic_tray_top"},
            "elements": tray}
    g.model("hydroponic_tray", base)
    for stage in range(4):
        variants[f"crop=none,stage={stage}"] = {"model": f"{NS}:block/hydroponic_tray"}
    for crop, tex in stages.items():
        for stage in range(4):
            name = f"hydroponic_tray_{crop}_{stage}"
            elements = list(tray)
            for x in (4, 12):
                elements.append({"from": [x, 8, 0], "to": [x, 16, 16], "shade": False,
                                 "faces": {"east": {"texture": "#crop"}, "west": {"texture": "#crop"}}})
            for z in (4, 12):
                elements.append({"from": [0, 8, z], "to": [16, 16, z], "shade": False,
                                 "faces": {"north": {"texture": "#crop"}, "south": {"texture": "#crop"}}})
            g.model(name, {"parent": "minecraft:block/block", "ambientocclusion": False,
                           "textures": dict(base["textures"], crop=f"minecraft:block/{tex[stage]}"), "elements": elements})
            variants[f"crop={crop},stage={stage}"] = {"model": f"{NS}:block/{name}"}
    write(os.path.join(ASSETS, "blockstates", "hydroponic_tray.json"), {"variants": variants})
    g.item_def("hydroponic_tray", f"{NS}:block/hydroponic_tray")


ADVANCEMENTS = {  # id: (иконка, родитель, рамка)
    "green_air": ("hydroponic_tray", "sealed", "goal"),
    "coriolis": ("spin_hub", "orbit", "challenge"),
    "first_track": ("rover_wheel", "moon", "task"),
    "view_from_above": ("imaging_satellite", "satellite", "goal"),
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
    advancements()
    recipes()
    data()
    assets()


if __name__ == "__main__":
    main()
