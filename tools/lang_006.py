#!/usr/bin/env python3
"""Локализация 006 (en/ru/uk): дописывает ключи в lang-файлы, не трогая остальные.
Запуск: python3 tools/lang_006.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

# id: (en, ru, uk)
ITEMS = {
    "copper_wire": ("Copper Wire", "Медный провод", "Мідний дріт"),
    "relay": ("Relay", "Реле", "Реле"),
    "relay_logic": ("Relay Logic", "Релейная логика", "Релейна логіка"),
    "ferrite_core": ("Ferrite Core", "Ферритовый сердечник", "Феритове осердя"),
    "core_rope_memory": ("Core Rope Memory", "Прошивная память", "Прошивна пам'ять"),
    "silicon_blend": ("Silicon Charge", "Шихта кремния", "Шихта кремнію"),
    "metallurgical_silicon": ("Metallurgical Silicon", "Технический кремний", "Технічний кремній"),
    "silicon_dust": ("Silicon Dust", "Кремниевая пыль", "Кремнієвий пил"),
    "carbon_monoxide": ("Carbon Monoxide Cylinder", "Баллон угарного газа", "Балон чадного газу"),
    "brine": ("Brine", "Рассол", "Розсіл"),
    "hydrogen_chloride": ("Hydrogen Chloride Cylinder", "Баллон хлороводорода", "Балон хлороводню"),
    "caustic_soda": ("Caustic Soda", "Едкий натр", "Їдкий натр"),
    "trichlorosilane": ("Trichlorosilane Cylinder", "Баллон трихлорсилана", "Балон трихлорсилану"),
    "polysilicon": ("Polysilicon", "Поликремний", "Полікремній"),
    "silicon_boule": ("Silicon Boule", "Кремниевый слиток", "Кремнієвий зливок"),
    "multicrystalline_silicon": ("Multicrystalline Silicon Brick", "Мультикремниевый брусок",
                                 "Мультикремнієвий брусок"),
    "multicrystalline_wafer": ("Multicrystalline Wafer", "Мультикремниевая пластина", "Мультикремнієва пластина"),
    "silicon_wafer": ("Silicon Wafer", "Кремниевая пластина", "Кремнієва пластина"),
    "solar_cell": ("Multicrystalline Solar Cell", "Мультикремниевый солнечный элемент",
                   "Мультикремнієвий сонячний елемент"),
    "mono_solar_cell": ("Monocrystalline Solar Cell", "Монокристаллический солнечный элемент",
                        "Монокристалічний сонячний елемент"),
    "rock_salt": ("Rock Salt", "Каменная соль", "Кам'яна сіль"),
    "photoresist": ("Photoresist", "Фоторезист", "Фоторезист"),
    "ceramic_package": ("Ceramic Package", "Керамический корпус", "Керамічний корпус"),
    "die_logic": ("Logic Die", "Кристалл логики", "Кристал логіки"),
    "die_microprocessor": ("Microprocessor Die", "Кристалл процессора", "Кристал процесора"),
    "die_radhard": ("Rad-Hard Die", "Радстойкий кристалл", "Радстійкий кристал"),
    "logic_chip": ("Logic Chip", "Логическая микросхема", "Логічна мікросхема"),
    "microprocessor": ("Microprocessor", "Микропроцессор", "Мікропроцесор"),
    "radhard_processor": ("Rad-Hard Processor", "Радстойкий процессор", "Радстійкий процесор"),
    "hydrofluoric_acid": ("Hydrofluoric Acid", "Плавиковая кислота", "Плавикова кислота"),
    "glass_fiber": ("Glass Fiber", "Стекловолокно", "Скловолокно"),
    "epoxy_resin": ("Epoxy Resin", "Эпоксидная смола", "Епоксидна смола"),
    "fr4_laminate": ("FR-4 Prepreg", "Стеклотекстолит FR-4", "Склотекстоліт FR-4"),
    "copper_clad_laminate": ("Copper-Clad Layup", "Фольгированная заготовка", "Фольгована заготовка"),
    "incomplete_circuit_board": ("Laminated Board", "Прессованная плата", "Пресована плата"),
    "circuit_board": ("Circuit Board", "Печатная плата", "Друкована плата"),
    "flight_computer": ("Flight Computer", "Бортовой компьютер", "Бортовий комп'ютер"),
    "fluorite": ("Fluorite", "Флюорит", "Флюорит"),
    "cryolite": ("Cryolite", "Криолит", "Кріоліт"),
    "alumina": ("Alumina", "Глинозём", "Глинозем"),
    "aluminium_ingot": ("Aluminium Ingot", "Алюминиевый слиток", "Алюмінієвий зливок"),
    "aluminium_copper_ingot": ("Al-Cu 2219 Ingot", "Слиток алюмомедного сплава 2219",
                               "Зливок алюмінієво-мідного сплаву 2219"),
    "spodumene": ("Spodumene", "Сподумен", "Сподумен"),
    "beta_spodumene": ("β-Spodumene", "β-сподумен", "β-сподумен"),
    "lithium_chloride": ("Lithium Chloride", "Хлорид лития", "Хлорид літію"),
    "lithium_ingot": ("Lithium Ingot", "Литиевый слиток", "Літієвий зливок"),
    "aluminium_lithium_ingot": ("Al-Li Alloy Ingot", "Слиток алюминий-литиевого сплава",
                                "Зливок алюміній-літієвого сплаву"),
    "nickel_ingot": ("Nickel Ingot", "Никелевый слиток", "Нікелевий зливок"),
    "nickel_superalloy_ingot": ("Nickel Superalloy Ingot", "Слиток жаропрочного никелевого сплава",
                                "Зливок жароміцного нікелевого сплаву"),
    "sapphire": ("Sapphire Boule", "Сапфировая буля", "Сапфірова буля"),
    "al_cu_blend": ("Al-Cu Charge", "Шихта алюмомедного сплава", "Шихта алюмінієво-мідного сплаву"),
    "al_li_blend": ("Al-Li Charge", "Шихта алюминий-литиевого сплава", "Шихта алюміній-літієвого сплаву"),
    "superalloy_blend": ("Superalloy Charge", "Шихта жаропрочного сплава", "Шихта жароміцного сплаву"),
    "phosphate_blend": ("Phosphate Charge", "Фосфатная шихта", "Фосфатна шихта"),
    "quartz_crucible": ("Quartz Crucible", "Кварцевый тигель", "Кварцовий тигель"),
    "phosphorus": ("Phosphorus", "Фосфор", "Фосфор"),
    "sulfuric_acid": ("Sulfuric Acid", "Серная кислота", "Сірчана кислота"),
    "gypsum": ("Gypsum", "Гипс", "Гіпс"),
    "sapphire_wafer": ("Sapphire Wafer", "Сапфировая подложка", "Сапфірова підкладка"),
    "sos_wafer": ("Silicon-on-Sapphire Wafer", "Пластина «кремний на сапфире»", "Пластина «кремній на сапфірі»"),
    "photomask_logic": ("Logic Photomask", "Фотошаблон логики", "Фотошаблон логіки"),
    "photomask_microprocessor": ("Microprocessor Photomask", "Фотошаблон процессора", "Фотошаблон процесора"),
    "photomask_radhard": ("Rad-Hard Photomask", "Радстойкий фотошаблон", "Радстійкий фотошаблон"),
}
BLOCKS = {
    "chemical_reactor": ("Chemical Reactor", "Химический реактор", "Хімічний реактор"),
    "deposition_reactor": ("Siemens Deposition Reactor", "Реактор осаждения Сименса", "Реактор осадження Сіменса"),
    "crystal_puller": ("Czochralski Puller", "Установка Чохральского", "Установка Чохральського"),
    "wafer_saw": ("Precision Wafer Saw", "Прецизионная пила", "Прецизійна пилка"),
    "fan_filter_unit": ("Fan Filter Unit", "Фильтровентиляционный модуль", "Фільтровентиляційний модуль"),
    "diffusion_furnace": ("Diffusion Furnace", "Диффузионная печь", "Дифузійна піч"),
    "lithography_station": ("Lithography Station", "Станция литографии", "Станція літографії"),
    "etch_bath": ("Etch Bath", "Травильная ванна", "Травильна ванна"),
    "halite_ore": ("Halite", "Галит", "Галіт"),
    "fluorite_ore": ("Fluorite Ore", "Флюоритовая руда", "Флюоритова руда"),
    "spodumene_ore": ("Spodumene Pegmatite", "Сподуменовый пегматит", "Сподуменовий пегматит"),
    "anorthosite": ("Anorthosite", "Анортозит", "Анортозит"),
    "aluminium_fuel_tank": ("Al-Cu Fuel Tank", "Бак из сплава Al-Cu", "Бак зі сплаву Al-Cu"),
    "al_li_fuel_tank": ("Al-Li Fuel Tank", "Бак из сплава Al-Li", "Бак зі сплаву Al-Li"),
    "mono_solar_panel": ("Monocrystalline Solar Panel", "Монокристаллическая солнечная панель",
                         "Монокристалічна сонячна панель"),
}
OTHER = {
    "tooltip.spacereloaded.purity": ("Purity: %sN", "Чистота: %sN", "Чистота: %sN"),
    "tooltip.spacereloaded.wafer.blank": ("Fresh wafer · next: %s", "Чистая пластина · дальше: %s",
                                          "Чиста пластина · далі: %s"),
    "tooltip.spacereloaded.wafer": ("%s · level %s of %s · next: %s", "%s · уровень %s из %s · дальше: %s",
                                    "%s · рівень %s з %s · далі: %s"),
    "tooltip.spacereloaded.wafer.yield": ("Defects %s /cm² · expected yield %s%%",
                                          "Дефекты %s /см² · ожидаемый выход %s%%",
                                          "Дефекти %s /см² · очікуваний вихід %s%%"),
    "wafer_op.spacereloaded.oxidize": ("oxidation (diffusion furnace)", "окисление (диффузионная печь)",
                                       "окиснення (дифузійна піч)"),
    "wafer_op.spacereloaded.metallize": ("aluminium metallization (diffusion furnace)",
                                         "металлизация алюминием (диффузионная печь)",
                                         "металізація алюмінієм (дифузійна піч)"),
    "wafer_op.spacereloaded.expose": ("exposure (lithography station)", "экспонирование (станция литографии)",
                                      "експонування (станція літографії)"),
    "wafer_op.spacereloaded.etch_oxide": ("oxide etch with HF (etch bath)", "травление оксида HF (травильная ванна)",
                                          "травлення оксиду HF (травильна ванна)"),
    "wafer_op.spacereloaded.etch_metal": ("aluminium etch with caustic soda (etch bath)",
                                          "травление алюминия щёлочью (травильная ванна)",
                                          "травлення алюмінію лугом (травильна ванна)"),
    "wafer_op.spacereloaded.done": ("dicing (wafer saw)", "скрайбирование (прецизионная пила)",
                                    "скрайбування (прецизійна пилка)"),
    "tooltip.spacereloaded.guidance_tier.1": ("Guidance: open-loop pitch program (margin 5%)",
                                              "Наведение: жёсткая программа тангажа (запас 5 %)",
                                              "Наведення: жорстка програма тангажу (запас 5 %)"),
    "tooltip.spacereloaded.guidance_tier.2": ("Guidance: closed-loop, flight computer (margin 2%)",
                                              "Наведение: замкнутое, бортовой компьютер (запас 2 %)",
                                              "Наведення: замкнене, бортовий комп'ютер (запас 2 %)"),
    "tooltip.spacereloaded.reserve.diffusion_furnace": ("%s%% · evaporator aluminium: %s g",
                                                        "%s%% · алюминий испарителя: %s г",
                                                        "%s%% · алюміній випарника: %s г"),
    "tooltip.spacereloaded.reserve.lithography_station": ("%s%% · photoresist left: %s wafers",
                                                          "%s%% · фоторезиста на %s пластин",
                                                          "%s%% · фоторезисту на %s пластин"),
    "tooltip.spacereloaded.reserve.etch_bath": ("%s%% · bath charge: %s wafers", "%s%% · заправка ванны: %s пластин",
                                                "%s%% · заправка ванни: %s пластин"),
    "tooltip.spacereloaded.iso_class": ("Air cleanliness ISO %s (ISO 14644-1, particles ≥ 0.5 µm)",
                                        "Чистота воздуха ISO %s (ISO 14644-1, частицы ≥ 0,5 мкм)",
                                        "Чистота повітря ISO %s (ISO 14644-1, частинки ≥ 0,5 мкм)"),
    "message.spacereloaded.crystal_puller.report": (
        "Pull %s%% · spin variation %s%% (≤ 5%% single crystal) · dopant doses %s · melt tail %s kg · heater %s E",
        "Вытягивание %s%% · колебание оборотов %s%% (≤ 5 %% — монокристалл) · доз лигатуры %s · хвост расплава %s кг · нагреватель %s E",
        "Витягування %s%% · коливання обертів %s%% (≤ 5 %% — монокристал) · доз лігатури %s · хвіст розплаву %s кг · нагрівач %s E"),
    "message.spacereloaded.crystal_puller.status": ("Pull %s%% · dopant doses %s", "Вытягивание %s%% · доз лигатуры %s",
                                                    "Витягування %s%% · доз лігатури %s"),
    "message.spacereloaded.scanner.particles": ("Particle counter: %s /m³ (≥ 0.5 µm) — ISO class %s",
                                                "Счётчик частиц: %s /м³ (≥ 0,5 мкм) — класс ISO %s",
                                                "Лічильник частинок: %s /м³ (≥ 0,5 мкм) — клас ISO %s"),
    "message.spacereloaded.regolith_reactor.bath": ("Cryolite bath for %s kg Al · carbon anode %s kg",
                                                    "Криолитовая ванна на %s кг Al · угольный анод %s кг",
                                                    "Кріолітова ванна на %s кг Al · вугільний анод %s кг"),
    "advancements.spacereloaded.thinking_sand.title": ("Sand That Thinks", "Песок, который думает", "Пісок, що думає"),
    "advancements.spacereloaded.thinking_sand.description": (
        "Dice a finished wafer into working chips", "Разрежьте готовую пластину на годные кристаллы",
        "Розріжте готову пластину на придатні кристали"),
    "advancements.spacereloaded.iso5.title": ("ISO 5", "ISO 5", "ISO 5"),
    "advancements.spacereloaded.iso5.description": (
        "Run a fab operation in air of class ISO 5 or cleaner", "Проведите операцию фаба в воздухе класса ISO 5 или чище",
        "Проведіть операцію фабу в повітрі класу ISO 5 або чистішому"),
    "advancements.spacereloaded.nine_nines.title": ("Nine Nines", "Девять девяток", "Дев'ять дев'яток"),
    "advancements.spacereloaded.nine_nines.description": (
        "Distil trichlorosilane to 9N electronic grade", "Очистите трихлорсилан до 9N — электронного качества",
        "Очистіть трихлорсилан до 9N — електронної якості"),
    "advancements.spacereloaded.light_tank.title": ("Lighter Than Air... Almost", "Легче воздуха… почти",
                                                    "Легше за повітря… майже"),
    "advancements.spacereloaded.light_tank.description": (
        "Launch a rocket with an aluminium alloy tank", "Запустите ракету с баком из алюминиевого сплава",
        "Запустіть ракету з баком з алюмінієвого сплаву"),
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
