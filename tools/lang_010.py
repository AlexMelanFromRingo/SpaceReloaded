#!/usr/bin/env python3
"""Локализация 010 (en/ru/uk). Запуск: python3 tools/lang_010.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

OTHER = {
    "modmenu.descriptionTranslation.spacereloaded": (
        "A space program on real physics: rockets built from blocks with honest Δv, sealed bases that breathe, "
        "orbital stations with spin gravity, interplanetary transfers by celestial mechanics, heavy industry from "
        "sand to a Kilopower reactor, and kinetic orbital strikes.",
        "Космическая программа на настоящей физике: ракеты из блоков с честной Δv, герметичные базы, которые "
        "дышат, орбитальные станции с гравитацией вращением, перелёты по небесной механике, промышленность от "
        "песка до реактора Kilopower и кинетические удары с орбиты.",
        "Космічна програма на справжній фізиці: ракети з блоків із чесною Δv, герметичні бази, що дихають, "
        "орбітальні станції з гравітацією обертанням, перельоти за небесною механікою, промисловість від піску "
        "до реактора Kilopower і кінетичні удари з орбіти."),
    "action.spacereloaded.terminal.mode": ("Auto / Hold", "Авто / Стоп", "Авто / Стоп"),
    "action.spacereloaded.mission_control.map": ("Flight map", "Карта полётов", "Карта польотів"),
    "message.spacereloaded.mission_control.entry_dv": (
        "%s %s fuel %s kg · h=%s m · Δv %s m/s · %s",
        "%s %s топливо %s кг · h=%s м · Δv %s м/с · %s",
        "%s %s паливо %s кг · h=%s м · Δv %s м/с · %s"),
    "message.spacereloaded.mission_control.satellites": (
        "Relay coverage: %s · imaging satellites %s · hyperspectral %s",
        "Покрытие ретрансляторами: %s · спутников-камер %s · гиперспектральных %s",
        "Покриття ретрансляторами: %s · супутників-камер %s · гіперспектральних %s"),
    "gauge.spacereloaded.kinetic.omega": ("Speed / flywheel limit", "Скорость / предел маховика", "Швидкість / межа маховика"),
    "gauge.spacereloaded.spin_hub.bearing": ("Bearing load", "Нагрузка подшипника", "Навантаження підшипника"),
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
