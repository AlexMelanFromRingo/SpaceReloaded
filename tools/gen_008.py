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


MASSES = {
    "eclss_controller": (60.0, ["eclss_controller"]),
    "eclss_rack_frame": (25.0, ["eclss_rack_frame"]),          # стойка ISPR ~ 100 кг на 4 блока рамы
    "eclss_modules": (180.0, ["ogs_module", "sabatier_module", "cdra_module", "wrs_module"]),  # ORU класса МКС
    "eclss_blank_panel": (8.0, ["eclss_blank_panel"]),
}

BLOCKS = ECLSS_BLOCKS


def data():
    eclss_template()
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
    data()
    eclss_models()


if __name__ == "__main__":
    main()
