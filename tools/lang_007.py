#!/usr/bin/env python3
"""Локализация 007 (en/ru/uk). Запуск: python3 tools/lang_007.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

ITEMS = {
    "lithium_hydroxide": ("Lithium Hydroxide", "Гидроксид лития", "Гідроксид літію"),
    "lioh_cartridge": ("LiOH Cartridge", "Картридж LiOH", "Картридж LiOH"),
    "zeolite": ("Zeolite 5A", "Цеолит 5A", "Цеоліт 5A"),
    "zeolite_bed": ("Zeolite Bed", "Цеолитовый слой", "Цеолітовий шар"),
    "straw": ("Straw", "Солома", "Солома"),
    "rover_chassis": ("Rover Chassis", "Шасси ровера", "Шасі ровера"),
    "rover_wheel": ("Rover Hub-Motor Wheel", "Мотор-колесо ровера", "Мотор-колесо ровера"),
    "nife_battery": ("Ni–Fe Battery 8.7 kWh", "Ni–Fe батарея 8,7 кВт·ч", "Ni–Fe батарея 8,7 кВт·год"),
    "telescope_mirror": ("Cassegrain Mirror Ø10 cm", "Зеркало Кассегрена Ø 10 см", "Дзеркало Кассегрена Ø 10 см"),
    "image_sensor": ("Pushbroom CCD Line (10⁴ px)", "ПЗС-линейка 10⁴ пикселей", "ПЗЗ-лінійка 10⁴ пікселів"),
    "orbital_image": ("Orbital Image (pending)", "Орбитальный снимок (ожидание)", "Орбітальний знімок (очікування)"),
}
BLOCKS = {
    "imaging_satellite": ("Imaging Satellite", "Спутник-камера", "Супутник-камера"),
    "gas_tank": ("Gas Tank", "Газовый баллон", "Газовий балон"),
    "air_separator": ("Air Separation Unit", "Воздухоразделительная установка", "Повітророзділювальна установка"),
    "airlock_pump": ("Airlock Pump", "Насос шлюза", "Насос шлюзу"),
    "co2_scrubber": ("CO₂ Scrubber", "Поглотитель CO₂", "Поглинач CO₂"),
    "hydroponic_tray": ("Hydroponic Tray", "Гидропонный лоток", "Гідропонний лоток"),
    "grow_lamp": ("Grow Lamp", "Фитолампа", "Фітолампа"),
    "biomass_oxidizer": ("Biomass Oxidizer", "Окислитель биомассы", "Окиснювач біомаси"),
    "spin_hub": ("Spin Hub", "Ступица кольца", "Маточина кільця"),
    "rim_thruster": ("Rim Thruster", "Двигатель обода", "Двигун обода"),
    "rover_charger": ("Rover Charger", "Зарядная станция ровера", "Зарядна станція ровера"),
    "despin_motor": ("Counter-Rotation Motor", "Мотор противовращения", "Мотор протиобертання"),
}
OTHER = {
    "gas.spacereloaded.none": ("empty", "пусто", "порожньо"),
    "gas.spacereloaded.oxygen": ("oxygen", "кислород", "кисень"),
    "gas.spacereloaded.nitrogen": ("nitrogen", "азот", "азот"),
    "message.spacereloaded.gas_tank": ("Tank: %s, %s of %s kg", "Баллон: %s, %s из %s кг", "Балон: %s, %s з %s кг"),
    "message.spacereloaded.life.gas": ("Air: %s kPa · O₂ %s kPa · CO₂ %s kPa · %s kg of gas",
                                       "Воздух: %s кПа · O₂ %s кПа · CO₂ %s кПа · газа %s кг",
                                       "Повітря: %s кПа · O₂ %s кПа · CO₂ %s кПа · газу %s кг"),
    "message.spacereloaded.life.store": ("Tanks at the controller: O₂ %s kg · N₂ %s kg",
                                         "Баллоны у контроллера: O₂ %s кг · N₂ %s кг",
                                         "Балони біля контролера: O₂ %s кг · N₂ %s кг"),
    "message.spacereloaded.life.forecast": ("Forecast: CO₂ limit in %s h · hypoxia in %s h (game hours)",
                                            "Прогноз: предел CO₂ через %s ч · гипоксия через %s ч (игровые часы)",
                                            "Прогноз: межа CO₂ через %s год · гіпоксія через %s год (ігрові години)"),
    "message.spacereloaded.airlock_suit": ("Airlock: someone inside has no full suit and mask — depressurisation blocked",
                                           "Шлюз: внутри кто-то без полного скафандра и маски — сброс давления запрещён",
                                           "Шлюз: усередині хтось без повного скафандра й маски — скидання тиску заборонено"),
    "message.spacereloaded.airlock_pump": ("Airlock pump-down: %s s, residual vented %s kg",
                                           "Откачка тамбура: %s с, стравится остаток %s кг",
                                           "Відкачування тамбура: %s с, стравиться залишок %s кг"),
    "death.attack.spacereloaded.asphyxia": ("%1$s passed out in bad air", "%1$s потерял(а) сознание в отравленном воздухе",
                                            "%1$s знепритомнів(ла) у отруєному повітрі"),
    "death.attack.spacereloaded.asphyxia.player": ("%1$s passed out in bad air fighting %2$s",
                                                   "%1$s потерял сознание в отравленном воздухе, сражаясь с %2$s",
                                                   "%1$s знепритомнів у отруєному повітрі, б'ючись із %2$s"),
    "message.spacereloaded.tray": ("Tray: %s · growth rate %s%% · ready %s%% · fertiliser %s m²·days · water %s kg",
                                   "Лоток: %s · темп роста %s%% · готовность %s%% · удобрения %s м²·сут · вода %s кг",
                                   "Лоток: %s · темп росту %s%% · готовність %s%% · добрива %s м²·діб · вода %s кг"),
    "message.spacereloaded.spin_hub.attached": ("Hub: the assembly touches the world — nothing to spin (isolate it, leave only the axle)",
                                                "Ступица: сборка касается мира — вращать нечего (изолируйте её, оставьте только ось)",
                                                "Маточина: збірка торкається світу — обертати нічого (ізолюйте її, лишіть тільки вісь)"),
    "message.spacereloaded.spin_hub.report": ("Ring: %s rpm · mass %s kg · I %s kg·m² · L %s N·m·s · bearing load %s of %s N",
                                              "Кольцо: %s об/мин · масса %s кг · I %s кг·м² · L %s Н·м·с · нагрузка подшипника %s из %s Н",
                                              "Кільце: %s об/хв · маса %s кг · I %s кг·м² · L %s Н·м·с · навантаження підшипника %s з %s Н"),
    "message.spacereloaded.despin.spin": ("Counter-rotation motor: spin up", "Мотор противовращения: раскрутка", "Мотор протиобертання: розкручування"),
    "message.spacereloaded.despin.brake": ("Counter-rotation motor: brake", "Мотор противовращения: торможение", "Мотор протиобертання: гальмування"),
    "message.spacereloaded.rover.report": ("Rover: wheels %s/4 · battery %s%% · mass %s kg · %s km/h · odometer %s km",
                                           "Ровер: колёс %s/4 · батарея %s%% · масса %s кг · %s км/ч · пробег %s км",
                                           "Ровер: коліс %s/4 · батарея %s%% · маса %s кг · %s км/год · пробіг %s км"),
    "advancements.spacereloaded.green_air.title": ("Green Air", "Зелёный воздух", "Зелене повітря"),
    "advancements.spacereloaded.green_air.description": (
        "Let the greenhouse make more oxygen than the crew breathes", "Пусть оранжерея даёт больше кислорода, чем дышит экипаж",
        "Нехай оранжерея дає більше кисню, ніж дихає екіпаж"),
    "advancements.spacereloaded.coriolis.title": ("Coriolis", "Кориолис", "Коріоліс"),
    "advancements.spacereloaded.coriolis.description": (
        "Stand on the floor of a spinning ring at 0.3 g or more", "Встаньте на пол вращающегося кольца при 0,3 g и больше",
        "Станьте на підлогу кільця, що обертається, при 0,3 g і більше"),
    "advancements.spacereloaded.first_track.title": ("First Track", "Первая колея", "Перша колія"),
    "advancements.spacereloaded.first_track.description": (
        "Drive a rover a kilometre on another world", "Проедьте на ровере километр по другому миру",
        "Проїдьте ровером кілометр іншим світом"),
    "advancements.spacereloaded.view_from_above.title": ("View from Above", "Взгляд сверху", "Погляд згори"),
    "advancements.spacereloaded.view_from_above.description": (
        "Receive an orbital image from your own satellite", "Получите снимок с орбиты от своего спутника",
        "Отримайте знімок з орбіти від свого супутника"),
    "message.spacereloaded.imaging.no_satellite": (
        "Mission control: no imaging satellite over this body — launch one (to Earth orbit, or unmanned to the Moon or Mars)",
        "ЦУП: над этим телом нет спутника-камеры — запустите его (на орбиту Земли или беспилотно к Луне или Марсу)",
        "ЦУП: над цим тілом немає супутника-камери — запустіть його (на орбіту Землі або безпілотно до Місяця чи Марса)"),
    "message.spacereloaded.imaging.no_orbit": ("Mission control: this body has no parking orbit for a camera",
                                               "ЦУП: у этого тела нет парковочной орбиты для камеры",
                                               "ЦУП: у цього тіла немає паркувальної орбіти для камери"),
    "message.spacereloaded.imaging.diffraction": (
        "Diffraction limit: the 10 cm telescope resolves %s m here — the finest map scale is %s",
        "Дифракционный предел: телескоп 10 см здесь различает %s м — самый мелкий масштаб карты %s",
        "Дифракційна межа: телескоп 10 см тут розрізняє %s м — найдрібніший масштаб карти %s"),
    "message.spacereloaded.imaging.scale": (
        "Scale %s: %s m per pixel (optics resolve %s m) · swath %s km · wait up to %s min",
        "Масштаб %s: %s м на пиксель (оптика различает %s м) · полоса %s км · ожидание до %s мин",
        "Масштаб %s: %s м на піксель (оптика розрізняє %s м) · смуга %s км · очікування до %s хв"),
    "message.spacereloaded.imaging.ordered": (
        "Image ordered: scale %s (%s m/px), swath %s km, %s satellite(s) — ready in %s min",
        "Снимок заказан: масштаб %s (%s м/пикс), полоса %s км, спутников %s — готов через %s мин",
        "Знімок замовлено: масштаб %s (%s м/пікс), смуга %s км, супутників %s — готовий за %s хв"),
    "message.spacereloaded.imaging.waiting": ("The satellite has not passed over yet — %s min left",
                                              "Спутник ещё не пролетал над точкой — осталось %s мин",
                                              "Супутник ще не пролітав над точкою — лишилося %s хв"),
    "message.spacereloaded.imaging.developed": ("Orbital image received: scale %s, %s m per pixel",
                                                "Снимок с орбиты получен: масштаб %s, %s м на пиксель",
                                                "Знімок з орбіти отримано: масштаб %s, %s м на піксель"),
    "message.spacereloaded.imaging.blank": ("Blank image — order one at mission control with an empty map",
                                            "Пустой снимок — закажите его в ЦУПе пустой картой",
                                            "Порожній знімок — замовте його в ЦУПі порожньою картою"),
    "entity.spacereloaded.rover": ("Rover", "Ровер", "Ровер"),
    "message.spacereloaded.tray.empty": ("empty", "пусто", "порожньо"),
    "tooltip.spacereloaded.reserve.co2_scrubber": ("%s%% · cartridge left: %s g CO₂", "%s%% · картриджа хватит на %s г CO₂",
                                                   "%s%% · картриджа вистачить на %s г CO₂"),
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
