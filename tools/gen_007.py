#!/usr/bin/env python3
"""Данные и ресурсы 007 «Жизнь на станции»: рецепты, лут, теги, массы, блок-стейты, модели.
Идемпотентен. Запуск: python3 tools/gen_007.py (после gen_006.py)."""
import json
import os

import gen_006 as g
from gen_006 import ASSETS, DATA, NS, assembly, chemical, sr, write

PROCESS = ["co2_scrubber", "biomass_oxidizer"]
MACHINES = ["air_separator", "airlock_pump"]
ITEMS = ["lithium_hydroxide", "lioh_cartridge", "zeolite", "zeolite_bed", "straw"]
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
    "crops_007": (0.5, ["minecraft:wheat", f"{NS}:straw"]),       # сноп/охапка 0.5 кг
    "potato": (0.2, ["minecraft:potato"]),                         # один клубень
}


def data():
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
    base = os.path.join(os.path.dirname(DATA), "minecraft", "tags", "block")
    for tag, blocks in (("mineable/pickaxe", BLOCKS),):
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


def main():
    recipes()
    data()
    assets()


if __name__ == "__main__":
    main()
