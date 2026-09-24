#!/usr/bin/env python3
"""Локализация 008 (en/ru/uk). Запуск: python3 tools/lang_008.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

ITEMS = {
    "beryl": ("Beryl", "Берилл", "Берил"),
    "beryllium_hydroxide": ("Beryllium Hydroxide", "Гидроксид бериллия", "Гідроксид берилію"),
    "beryllium_oxide": ("Beryllium Oxide (BeO)", "Оксид бериллия (BeO)", "Оксид берилію (BeO)"),
    "sodium": ("Sodium", "Натрий", "Натрій"),
    "borax": ("Borax", "Бура", "Бура"),
    "boric_acid": ("Boric Acid", "Борная кислота", "Борна кислота"),
    "boron_carbide_blend": ("Boron Carbide Charge", "Шихта карбида бора", "Шихта карбіду бору"),
    "boron_carbide": ("Boron Carbide (B₄C)", "Карбид бора (B₄C)", "Карбід бору (B₄C)"),
    "zircon": ("Zircon", "Циркон", "Циркон"),
    "zirconium": ("Zirconium", "Цирконий", "Цирконій"),
    "uraninite": ("Pitchblende", "Урановая смолка", "Уранова смолка"),
    "yellowcake": ("Yellowcake (U₃O₈)", "Жёлтый кек (U₃O₈)", "Жовтий кек (U₃O₈)"),
    "uranium_dioxide": ("Uranium Dioxide", "Диоксид урана", "Діоксид урану"),
    "uranium_tetrafluoride": ("Uranium Tetrafluoride (green salt)", "Тетрафторид урана (зелёная соль)",
                              "Тетрафторид урану (зелена сіль)"),
    "fluorine": ("Fluorine Cylinder", "Баллон фтора", "Балон фтору"),
    "uranium_hexafluoride": ("Uranium Hexafluoride (UF₆)", "Гексафторид урана (UF₆)", "Гексафторид урану (UF₆)"),
    "depleted_uranium_hexafluoride": ("Depleted UF₆ (tails)", "Обеднённый UF₆ (отвал)", "Збіднений UF₆ (відвал)"),
    "fuel_basket": ("Fuel Basket (U-Zr)", "Топливная корзина (U-Zr)", "Паливний кошик (U-Zr)"),
}
BLOCKS = {
    "eclss_controller": ("ECLSS Rack Controller", "Контроллер стойки жизнеобеспечения", "Контролер стійки життєзабезпечення"),
    "eclss_rack_frame": ("ECLSS Rack Frame", "Рама стойки СЖО", "Рама стійки СЖЗ"),
    "ogs_module": ("Oxygen Generation Module", "Модуль электролиза воды (OGS)", "Модуль електролізу води (OGS)"),
    "sabatier_module": ("Sabatier Module", "Модуль Сабатье", "Модуль Сабатьє"),
    "cdra_module": ("CO₂ Removal Module", "Модуль удаления CO₂ (CDRA)", "Модуль видалення CO₂ (CDRA)"),
    "wrs_module": ("Water Recovery Module", "Модуль регенерации воды (WRS)", "Модуль регенерації води (WRS)"),
    "eclss_blank_panel": ("ECLSS Blank Panel", "Глухая панель стойки", "Глуха панель стійки"),
    "control_rod_drive": ("Control Rod Drive", "Привод регулирующего стержня", "Привід регулювального стрижня"),
    "reactor_core": ("Reactor Core Vessel", "Корпус активной зоны", "Корпус активної зони"),
    "beo_reflector": ("BeO Reflector", "Отражатель BeO", "Відбивач BeO"),
    "heat_pipe": ("Sodium Heat Pipe", "Натриевая тепловая труба", "Натрієва теплова труба"),
    "stirling_convertor": ("Stirling Convertor 250 We", "Двигатель Стирлинга 250 Вт(э)", "Двигун Стірлінга 250 Вт(е)"),
    "reactor_power_cap": ("Power Slot Cap", "Заглушка гнезда", "Заглушка гнізда"),
    "radiator_panel": ("Radiator Panel", "Панель радиатора", "Панель радіатора"),
    "cascade_controller": ("Enrichment Cascade Controller", "Контроллер каскада обогащения", "Контролер каскаду збагачення"),
    "gas_centrifuge": ("Gas Centrifuge", "Газовая центрифуга", "Газова центрифуга"),
    "beryl_ore": ("Beryl Ore", "Бериллиевая руда", "Берилієва руда"),
    "borax_ore": ("Borax Deposit", "Залежь буры", "Поклад бури"),
    "uraninite_ore": ("Pitchblende Ore", "Урановая руда (смолка)", "Уранова руда (смолка)"),
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
    "screen.spacereloaded.reactor": ("Fission Reactor (Kilopower)", "Реактор деления (Kilopower)", "Реактор поділу (Kilopower)"),
    "status.spacereloaded.reactor.not_formed": ("Reactor not assembled — strike the rod drive with the engineer's hammer",
                                                "Реактор не собран — ударьте привод стержня инженерным молотом",
                                                "Реактор не зібрано — вдарте привід стрижня інженерним молотом"),
    "status.spacereloaded.reactor.melted": ("CORE MELTED — the reactor is destroyed; decay heat continues",
                                            "РАСПЛАВ ЗОНЫ — реактор разрушен, остаточное тепло продолжается",
                                            "РОЗПЛАВ ЗОНИ — реактор зруйновано, залишкове тепло триває"),
    "status.spacereloaded.reactor.damaged": ("Core damaged by overheating — output halved",
                                             "Зона повреждена перегревом — выработка вдвое ниже",
                                             "Зону пошкоджено перегрівом — вироблення вдвічі менше"),
    "status.spacereloaded.reactor.scram": ("SCRAM — rod dropped (reset when the rod is fully in)",
                                           "SCRAM — стержень сброшен (сброс, когда стержень внизу)",
                                           "SCRAM — стрижень скинуто (скидання, коли стрижень унизу)"),
    "status.spacereloaded.reactor.auto": ("Mode: regulator, setpoint %s K", "Режим: регулятор, уставка %s K",
                                          "Режим: регулятор, уставка %s K"),
    "status.spacereloaded.reactor.manual": ("Mode: manual rod", "Режим: ручной стержень", "Режим: ручний стрижень"),
    "status.spacereloaded.reactor.power": ("Power: %s kWt · %s kWe (Stirlings: %s)",
                                           "Мощность: %s кВт(т) · %s кВт(э) (Стирлингов: %s)",
                                           "Потужність: %s кВт(т) · %s кВт(е) (Стірлінгів: %s)"),
    "status.spacereloaded.reactor.temp": ("Core %s K · cold end %s K · environment %s K",
                                          "Зона %s K · холодный конец %s K · среда %s K",
                                          "Зона %s K · холодний кінець %s K · середовище %s K"),
    "status.spacereloaded.reactor.rho": ("Reactivity %s ¢ · period %s s", "Реактивность %s ¢ · период %s с",
                                         "Реактивність %s ¢ · період %s с"),
    "status.spacereloaded.reactor.rod": ("Rod withdrawn %s%% (target %s%%)", "Стержень выведен на %s%% (цель %s%%)",
                                         "Стрижень виведено на %s%% (ціль %s%%)"),
    "status.spacereloaded.reactor.no_fuel": ("No fuel — insert a basket (rod in, core cold)",
                                             "Нет топлива — вставьте корзину (стержень внизу, зона остыла)",
                                             "Немає палива — вставте кошик (стрижень унизу, зона охолола)"),
    "status.spacereloaded.reactor.fuel": ("Fuel: %s kg U-235 (%s%%) · burnup %s MWd",
                                          "Топливо: %s кг U-235 (%s %%) · выгорание %s МВт·сут",
                                          "Паливо: %s кг U-235 (%s %%) · вигоряння %s МВт·діб"),
    "status.spacereloaded.reactor.radiator": ("Radiator %s m² · decay heat %s kW", "Радиатор %s м² · остаточное тепло %s кВт",
                                              "Радіатор %s м² · залишкове тепло %s кВт"),
    "gauge.spacereloaded.rod": ("Rod", "Стержень", "Стрижень"),
    "gauge.spacereloaded.core_temp": ("Core T (melt 1420 K)", "T зоны (плавление 1420 K)", "T зони (плавлення 1420 K)"),
    "gauge.spacereloaded.electric": ("Electric output", "Выработка", "Вироблення"),
    "action.spacereloaded.reactor.auto": ("Auto", "Авто", "Авто"),
    "action.spacereloaded.reactor.reset": ("Reset", "Сброс", "Скид."),
    "action.spacereloaded.reactor.unload": ("Unload", "Выгр.", "Вивант."),
    "message.spacereloaded.reactor.meltdown": ("Reactor core meltdown!", "Расплав активной зоны реактора!",
                                               "Розплав активної зони реактора!"),
    "message.spacereloaded.reactor.basket_refused": (
        "The basket is loaded only into a cold shut-down reactor (rod fully in, core below 400 K)",
        "Корзину меняют только в заглушённом холодном реакторе (стержень внизу, зона ниже 400 K)",
        "Кошик міняють лише в заглушеному холодному реакторі (стрижень унизу, зона нижче 400 K)"),
    "message.spacereloaded.reactor.basket_in": ("Basket loaded: %s kg U-235, %s%% — excess reactivity %s $",
                                                "Корзина загружена: %s кг U-235, %s %% — запас реактивности %s $",
                                                "Кошик завантажено: %s кг U-235, %s %% — запас реактивності %s $"),
    "manual.spacereloaded.fission_reactor.title": ("Fission Reactor", "Реактор деления", "Реактор поділу"),
    "manual.spacereloaded.fission_reactor.text": (
        "A Kilopower mast: rod drive at the bottom, the core in a BeO ring, a heat pipe to four Stirling slots, "
        "radiator segments above. Pull the rod slowly or use the regulator: the reactor follows the load by "
        "itself, but a fast pull outruns the temperature feedback and melts the core.",
        "Мачта Kilopower: внизу привод стержня, над ним зона в кольце BeO, тепловая труба к четырём гнёздам "
        "Стирлингов, выше — сегменты радиатора. Выводите стержень медленно или включите регулятор: реактор сам "
        "следует за нагрузкой, но быстрый вывод обгоняет обратную связь и плавит зону.",
        "Щогла Kilopower: унизу привід стрижня, над ним зона в кільці BeO, теплова труба до чотирьох гнізд "
        "Стірлінгів, вище — сегменти радіатора. Виводьте стрижень повільно або ввімкніть регулятор: реактор сам "
        "слідує за навантаженням, але швидке виведення випереджає зворотний зв'язок і плавить зону."),
    "screen.spacereloaded.cascade": ("Centrifuge Cascade", "Каскад газовых центрифуг", "Каскад газових центрифуг"),
    "status.spacereloaded.cascade.not_formed": ("Cascade not assembled — strike the controller with the engineer's hammer",
                                                "Каскад не собран — ударьте контроллер инженерным молотом",
                                                "Каскад не зібрано — вдарте контролер інженерним молотом"),
    "status.spacereloaded.cascade.stages": ("Stages: %s · feed %s%% → product %s%% · tails %s%%",
                                            "Ступеней: %s · питание %s %% → продукт %s %% · отвал %s %%",
                                            "Ступенів: %s · живлення %s %% → продукт %s %% · відвал %s %%"),
    "status.spacereloaded.cascade.balance": ("Per kg of product: %s kg feed, %s SWU",
                                             "На кг продукта: %s кг питания, %s ЕРР",
                                             "На кг продукту: %s кг живлення, %s ОРР"),
    "status.spacereloaded.cascade.rate": ("Output: %s kg/day (%s SWU/day)", "Выход: %s кг/сут (%s ЕРР/сут)",
                                          "Вихід: %s кг/доб (%s ОРР/доб)"),
    "status.spacereloaded.cascade.stock": ("Feed %s kg U · product %s kg (%s%%) · tails %s kg",
                                           "Питание %s кг U · продукт %s кг (%s %%) · отвал %s кг",
                                           "Живлення %s кг U · продукт %s кг (%s %%) · відвал %s кг"),
    "gauge.spacereloaded.enrichment": ("Product enrichment", "Обогащение продукта", "Збагачення продукту"),
    "gauge.spacereloaded.swu": ("Next kg", "Следующий кг", "Наступний кг"),
    "action.spacereloaded.cascade.product": ("Product", "Продукт", "Продукт"),
    "action.spacereloaded.cascade.tails": ("Tails", "Отвал", "Відвал"),
    "action.spacereloaded.cascade.basket": ("Basket", "Корзина", "Кошик"),
    "message.spacereloaded.cascade.no_product": ("No enriched product yet", "Обогащённого продукта пока нет",
                                                 "Збагаченого продукту поки немає"),
    "message.spacereloaded.cascade.need_zirconium": ("U-Zr alloy needs %s zirconium", "Для сплава U-Zr нужно циркония: %s",
                                                     "Для сплаву U-Zr потрібно цирконію: %s"),
    "message.spacereloaded.cascade.basket": ("Basket made: %s kg U-235 (%s%%), excess reactivity %s $",
                                             "Корзина готова: %s кг U-235 (%s %%), запас реактивности %s $",
                                             "Кошик готовий: %s кг U-235 (%s %%), запас реактивності %s $"),
    "message.spacereloaded.cascade.basket_subcritical": (
        "Basket made: %s kg U-235 (%s%%) — below the critical mass, reactivity %s $: the reactor will not start",
        "Корзина готова: %s кг U-235 (%s %%) — меньше критической массы, реактивность %s $: реактор не запустится",
        "Кошик готовий: %s кг U-235 (%s %%) — менше критичної маси, реактивність %s $: реактор не запуститься"),
    "manual.spacereloaded.centrifuge_cascade.title": ("Centrifuge Cascade", "Каскад центрифуг", "Каскад центрифуг"),
    "manual.spacereloaded.centrifuge_cascade.text": (
        "A line of gas centrifuges behind the controller. Each stage multiplies the U-235 ratio by 1.3: 14 give "
        "~22% (LEU), 29 give ~93% (HEU). One kilogram of 93% needs 201 kg of natural uranium and 216 SWU.",
        "Линия газовых центрифуг за контроллером. Каждая ступень умножает отношение U-235 на 1.3: 14 дают "
        "~22 % (НОУ), 29 — ~93 % (ВОУ). Килограмм 93 % требует 201 кг природного урана и 216 ЕРР.",
        "Лінія газових центрифуг за контролером. Кожен ступінь множить відношення U-235 на 1.3: 14 дають "
        "~22 % (НЗУ), 29 — ~93 % (ВЗУ). Кілограм 93 % потребує 201 кг природного урану та 216 ОРР."),
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
