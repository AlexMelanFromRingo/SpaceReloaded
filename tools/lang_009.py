#!/usr/bin/env python3
"""Локализация 009 (en/ru/uk). Запуск: python3 tools/lang_009.py"""
import json
import os

LANG = os.path.join(os.path.dirname(__file__), "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "lang")

ITEMS = {
    "galena": ("Galena (PbS)", "Галенит (PbS)", "Галеніт (PbS)"),
    "pbs_detector": ("PbS Infrared Photodetector", "ИК-фотоприёмник PbS", "ІЧ-фотоприймач PbS"),
    "diffraction_grating": ("Diffraction Grating", "Дифракционная решётка", "Дифракційна ґратка"),
    "ir_spectrometer": ("Near-Infrared Spectrometer", "Спектрометр ближнего ИК", "Спектрометр ближнього ІЧ"),
    "mineral_map": ("Mineral Map (blank)", "Бланк карты минералов", "Бланк карти мінералів"),
    "mineral_map.developed": ("Mineral Map", "Карта минералов", "Карта мінералів"),
    "ground_radar": ("Ground-Penetrating Radar", "Георадар", "Георадар"),
    "radargram": ("Radargram", "Радарограмма", "Радарограма"),
    "radargram.info": ("%s traces, %s m of track, down to %s m", "%s трасс, %s м пути, до %s м в глубину",
                       "%s трас, %s м шляху, до %s м углиб"),
}
BLOCKS = {
    "galena_ore": ("Galena Ore", "Галенитовая руда", "Галенітова руда"),
    "hyperspectral_satellite": ("Hyperspectral Satellite", "Гиперспектральный спутник", "Гіперспектральний супутник"),
}
MINERALS = {
    "water_ice": ("Water ice", "Водяной лёд", "Водяний лід"),
    "vegetation": ("Vegetation (red edge)", "Растительность (красный край)", "Рослинність (червоний край)"),
    "iron_oxide": ("Iron oxides", "Оксиды железа", "Оксиди заліза"),
    "clay": ("Clay minerals", "Глинистые минералы", "Глинисті мінерали"),
    "carbonate": ("Carbonates", "Карбонаты", "Карбонати"),
    "kerogen": ("Kerogen (oil shale)", "Кероген (горючий сланец)", "Кероген (горючий сланець)"),
    "borate": ("Borates", "Бораты", "Борати"),
    "ilmenite": ("Ilmenite (TiO₂)", "Ильменит (TiO₂)", "Ільменіт (TiO₂)"),
    "plagioclase": ("Plagioclase (anorthosite)", "Плагиоклаз (анортозит)", "Плагіоклаз (анортозит)"),
    "pyroxene": ("Pyroxene (fresh basalt)", "Пироксен (свежий базальт)", "Піроксен (свіжий базальт)"),
}
ADVANCEMENTS = {
    "celestial_mechanics": (("Celestial Mechanics", "Transfer to another planet on the cheapest day of the window"),
                            ("Небесная механика", "Перелететь к другой планете в самый дешёвый день окна"),
                            ("Небесна механіка", "Перелетіти до іншої планети в найдешевший день вікна")),
    "spectrum": (("Spectrum", "Develop a mineral map with a mineral found"),
                 ("Спектр", "Проявить карту минералов с найденным минералом"),
                 ("Спектр", "Проявити карту мінералів зі знайденим мінералом")),
    "below_ground": (("Below Ground", "Print a radargram with an echo deeper than 5 m"),
                     ("Под грунтом", "Напечатать радарограмму с отражением глубже 5 м"),
                     ("Під ґрунтом", "Надрукувати радарограму з відбиттям глибше 5 м")),
}
OTHER = {
    "message.spacereloaded.radar.empty": ("The radar has recorded nothing yet — drive the rover",
                                          "Радар ещё ничего не записал — проедьте на ровере",
                                          "Радар ще нічого не записав — проїдьте на ровері"),
    "message.spacereloaded.radar.printed": ("Radargram printed: %s traces, %s m of track",
                                            "Радарограмма напечатана: %s трасс, %s м пути",
                                            "Радарограму надруковано: %s трас, %s м шляху"),
    "message.spacereloaded.spectral.none": ("No mineral with a diagnostic band covers a quarter of any pixel",
                                            "Ни один минерал с полосой не занял и четверти пикселя",
                                            "Жоден мінерал зі смугою не зайняв і чверті пікселя"),
    "message.spacereloaded.spectral.no_satellite": ("No hyperspectral satellite over this body (or a deep space link to one)",
                                                    "Над телом нет гиперспектрального спутника (и нет дальней связи с таким)",
                                                    "Над тілом немає гіперспектрального супутника (і немає далекого зв'язку з таким)"),
    "screen.spacereloaded.radargram": ("Radargram", "Радарограмма", "Радарограма"),
    "screen.spacereloaded.radargram.info": ("track %s m · ε of the top layer %s · depth scale to %s m",
                                            "путь %s м · ε верхнего слоя %s · глубина до %s м",
                                            "шлях %s м · ε верхнього шару %s · глибина до %s м"),
    "screen.spacereloaded.radargram.axis": ("track → (0.5 m per trace), depth ↓ in metres",
                                            "путь → (0.5 м на трассу), глубина ↓ в метрах",
                                            "шлях → (0.5 м на трасу), глибина ↓ у метрах"),
    "screen.spacereloaded.map.windows": ("Windows…", "Окна…", "Вікна…"),
    "screen.spacereloaded.map.route_cost": ("Route today: %s km/s", "Маршрут сегодня: %s км/с", "Маршрут сьогодні: %s км/с"),
    "screen.spacereloaded.map.stack_dv": ("Stack Δv: %s km/s", "Δv ракеты: %s км/с", "Δv ракети: %s км/с"),
    "screen.spacereloaded.map.leg_today": ("Transfer today: %s km/s", "Перелёт сегодня: %s км/с", "Переліт сьогодні: %s км/с"),
    "screen.spacereloaded.map.leg_min": ("Minimum %s km/s in %s d", "Минимум %s км/с через %s д", "Мінімум %s км/с через %s д"),
    "screen.spacereloaded.porkchop": ("Transfer windows", "Окна перелёта", "Вікна перельоту"),
    "screen.spacereloaded.porkchop.back": ("Back", "Назад", "Назад"),
    "screen.spacereloaded.porkchop.route": ("%s → %s", "%s → %s", "%s → %s"),
    "screen.spacereloaded.porkchop.summary": (
        "today %s km/s · best %s km/s in %s d, flight %s days",
        "сегодня %s км/с · лучший %s км/с через %s д, полёт %s сут",
        "сьогодні %s км/с · найкращий %s км/с через %s д, політ %s діб"),
    "screen.spacereloaded.porkchop.today": ("today", "сегодня", "сьогодні"),
    "screen.spacereloaded.porkchop.best": ("best", "лучший", "найкращий"),
    "screen.spacereloaded.porkchop.axis_date": ("departure: 0 … %s game days", "отлёт: 0 … %s игровых суток",
                                                "виліт: 0 … %s ігрових діб"),
    "screen.spacereloaded.porkchop.axis_dv": ("Δv, km/s", "Δv, км/с", "Δv, км/с"),
    "screen.spacereloaded.porkchop.axis_tof": ("flight time ↑ %s … %s days", "время полёта ↑ %s … %s сут",
                                               "час польоту ↑ %s … %s діб"),
    "message.spacereloaded.rocket.transfer_today": (
        "today %s m/s (period minimum %s m/s in %s d %s h)",
        "сегодня %s м/с (минимум периода %s м/с через %s д %s ч)",
        "сьогодні %s м/с (мінімум періоду %s м/с через %s д %s год)"),
    "message.spacereloaded.mission.wait_window": (
        "Transfer to %s costs %s m/s today, the stack can afford %s: enough in %s d %s h",
        "Перелёт к %s сегодня стоит %s м/с, ракете по силам %s: хватит через %s д %s ч",
        "Переліт до %s сьогодні коштує %s м/с, ракеті до снаги %s: вистачить через %s д %s год"),
    "message.spacereloaded.mission.no_window": (
        "Transfer to %s costs %s m/s today; the stack can afford %s — not enough on any date this period",
        "Перелёт к %s сегодня стоит %s м/с; ракете по силам %s — не хватит ни в один день периода",
        "Переліт до %s сьогодні коштує %s м/с; ракеті до снаги %s — не вистачить у жоден день періоду"),
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
        for key, names in MINERALS.items():
            data[f"mineral.spacereloaded.{key}"] = names[i]
        for key, langs in ADVANCEMENTS.items():
            data[f"advancements.spacereloaded.{key}.title"] = langs[i][0]
            data[f"advancements.spacereloaded.{key}.description"] = langs[i][1]
        with open(path, "w", encoding="utf-8") as f:
            json.dump(data, f, indent="\t", ensure_ascii=False)
            f.write("\n")


if __name__ == "__main__":
    main()
