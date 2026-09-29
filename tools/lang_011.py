#!/usr/bin/env python3
"""Локализация 011 (en/ru/uk). Запуск: python3 tools/lang_011.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

OTHER = {
    "message.spacereloaded.motor.voltage": (
        "Armature voltage %s%% · no-load %s rpm · peak power %s kW",
        "Напряжение якоря %s %% · холостой ход %s об/мин · пиковая мощность %s кВт",
        "Напруга якоря %s %% · холостий хід %s об/хв · пікова потужність %s кВт"),
    "action.spacereloaded.motor.slower": ("Voltage −10%", "Напряжение −10 %", "Напруга −10 %"),
    "action.spacereloaded.motor.faster": ("Voltage +10%", "Напряжение +10 %", "Напруга +10 %"),
}


def main():
    for i, code in enumerate(("en_us", "ru_ru", "uk_ua")):
        path = os.path.join(LANG, code + ".json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for key, names in OTHER.items():
            data[key] = names[i]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent="\t", ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    main()
