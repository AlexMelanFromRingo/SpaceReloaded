#!/usr/bin/env python3
"""Локализация 008 (en/ru/uk). Запуск: python3 tools/lang_008.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

ITEMS = {}
BLOCKS = {
    "eclss_controller": ("ECLSS Rack Controller", "Контроллер стойки жизнеобеспечения", "Контролер стійки життєзабезпечення"),
    "eclss_rack_frame": ("ECLSS Rack Frame", "Рама стойки СЖО", "Рама стійки СЖЗ"),
    "ogs_module": ("Oxygen Generation Module", "Модуль электролиза воды (OGS)", "Модуль електролізу води (OGS)"),
    "sabatier_module": ("Sabatier Module", "Модуль Сабатье", "Модуль Сабатьє"),
    "cdra_module": ("CO₂ Removal Module", "Модуль удаления CO₂ (CDRA)", "Модуль видалення CO₂ (CDRA)"),
    "wrs_module": ("Water Recovery Module", "Модуль регенерации воды (WRS)", "Модуль регенерації води (WRS)"),
    "eclss_blank_panel": ("ECLSS Blank Panel", "Глухая панель стойки", "Глуха панель стійки"),
}
OTHER = {
    "screen.spacereloaded.eclss": ("Life Support Rack", "Стойка жизнеобеспечения", "Стійка життєзабезпечення"),
    "status.spacereloaded.eclss.not_formed": ("Rack not assembled — strike the controller with the engineer's hammer",
                                              "Стойка не собрана — ударьте контроллер инженерным молотом",
                                              "Стійку не зібрано — вдарте контролер інженерним молотом"),
    "status.spacereloaded.eclss.no_zone": ("No sealed zone next to the controller", "У контроллера нет герметичной зоны",
                                           "Біля контролера немає герметичної зони"),
    "status.spacereloaded.eclss.modules": ("Modules: %s", "Модули: %s", "Модулі: %s"),
    "status.spacereloaded.eclss.crew": ("Crew in zone: %s · pO₂ %s kPa · pCO₂ %s kPa",
                                        "Экипаж в зоне: %s · pO₂ %s кПа · pCO₂ %s кПа",
                                        "Екіпаж у зоні: %s · pO₂ %s кПа · pCO₂ %s кПа"),
    "status.spacereloaded.eclss.o2": ("O₂ produced: %s kg/day (water %s kg/day)",
                                      "O₂ выработка: %s кг/сут (вода %s кг/сут)",
                                      "O₂ вироблення: %s кг/доб (вода %s кг/доб)"),
    "status.spacereloaded.eclss.co2": ("CO₂ removed: %s kg/day, reduced by Sabatier %s%%",
                                       "CO₂ удалено: %s кг/сут, восстановлено Сабатье %s%%",
                                       "CO₂ видалено: %s кг/доб, відновлено Сабатьє %s%%"),
    "status.spacereloaded.eclss.water": ("Water: %s kg · recovered %s kg/day · makeup %s kg/day",
                                         "Вода: %s кг · возврат %s кг/сут · подпитка %s кг/сут",
                                         "Вода: %s кг · повернення %s кг/доб · підживлення %s кг/доб"),
    "status.spacereloaded.eclss.ch4_fuel": ("CH₄: %s kg/day → methalox into the tank (O₂ from the cylinder)",
                                            "CH₄: %s кг/сут → метанокс в бак (O₂ из баллона)",
                                            "CH₄: %s кг/доб → метанокс у бак (O₂ з балона)"),
    "status.spacereloaded.eclss.ch4_vent": ("CH₄: %s kg/day vented (no fuel tank + O₂ cylinder next to the controller)",
                                            "CH₄: %s кг/сут в сброс (рядом нет топливного бака и баллона O₂)",
                                            "CH₄: %s кг/доб у скид (поруч немає паливного бака й балона O₂)"),
    "status.spacereloaded.eclss.power": ("Power: %s kW", "Мощность: %s кВт", "Потужність: %s кВт"),
    "status.spacereloaded.eclss.no_water": ("No water — load ice or a water bucket", "Нет воды — заложите лёд или ведро воды",
                                            "Немає води — закладіть лід або відро води"),
    "status.spacereloaded.eclss.no_power": ("Not enough power", "Не хватает энергии", "Бракує енергії"),
    "gauge.spacereloaded.water": ("Water", "Вода", "Вода"),
    "gauge.spacereloaded.po2": ("pO₂", "pO₂", "pO₂"),
    "gauge.spacereloaded.pco2": ("pCO₂ (limit 0.53)", "pCO₂ (предел 0.53)", "pCO₂ (межа 0.53)"),
    "module.spacereloaded.ogs": ("electrolysis", "электролиз", "електроліз"),
    "module.spacereloaded.sabatier": ("Sabatier", "Сабатье", "Сабатьє"),
    "module.spacereloaded.cdra": ("CO₂ removal", "удаление CO₂", "видалення CO₂"),
    "module.spacereloaded.wrs": ("water recovery", "регенерация воды", "регенерація води"),
    "module.spacereloaded.none": ("none", "нет", "немає"),
    "message.spacereloaded.eclss.water": ("Rack water: +%s kg (%s kg)", "Вода стойки: +%s кг (%s кг)",
                                          "Вода стійки: +%s кг (%s кг)"),
    "manual.spacereloaded.eclss_rack.title": ("Life Support Rack", "Стойка жизнеобеспечения", "Стійка життєзабезпечення"),
    "manual.spacereloaded.eclss_rack.text": (
        "A 5×4 rack in the wall of a sealed zone with four module sockets. Electrolysis turns water into O₂; "
        "Sabatier turns exhaled CO₂ and the electrolysis H₂ into water and methane — hydrogen covers 57% of CO₂, "
        "so half the oxygen comes back, as on the ISS. Water recovery returns metabolic water.",
        "Стойка 5×4 в стене герметичной зоны с четырьмя гнёздами модулей. Электролиз делает O₂ из воды; Сабатье "
        "превращает выдохнутый CO₂ и водород электролиза в воду и метан — водорода хватает на 57 % CO₂, "
        "поэтому возвращается половина кислорода, как на МКС. Регенерация возвращает метаболическую воду.",
        "Стійка 5×4 у стіні герметичної зони з чотирма гніздами модулів. Електроліз робить O₂ з води; Сабатьє "
        "перетворює видихнутий CO₂ і водень електролізу на воду й метан — водню вистачає на 57 % CO₂, "
        "тож повертається половина кисню, як на МКС. Регенерація повертає метаболічну воду."),
}


def main():
    for i, code in enumerate(("en_us", "ru_ru", "uk_ua")):
        path = os.path.join(LANG, code + ".json")
        with open(path, encoding="utf-8") as f:
            data = json.load(f)
        for key, names in ITEMS.items():
            data[f"item.spacereloaded.{key}"] = names[i]
        for key, names in BLOCKS.items():
            data[f"block.spacereloaded.{key}"] = names[i]
        for key, names in OTHER.items():
            data[key] = names[i]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent="\t", ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    main()
