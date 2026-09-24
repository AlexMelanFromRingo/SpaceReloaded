#!/usr/bin/env python3
"""
Синтез собственных звуков SpaceReloaded (004, D40) утилитой sox → ogg vorbis.

Никакого стороннего контента: каждый звук — формула ниже (синусы, шум, огибающие),
поэтому звуки лицензируются вместе с модом (MIT) и воспроизводимы. Ресурс-пак может
заменить любой файл в assets/spacereloaded/sounds/.

Запуск: python3 tools/gen_sounds.py   (нужны sox с поддержкой ogg)
"""
import os
import subprocess

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "mod/src/main/resources/assets/spacereloaded/sounds")

RATE = ["-r", "44100", "-c", "1"]

# Имя файла → (описание, аргументы sox после "-n <out>")
SOUNDS = {
    # Заряд конденсаторов: восходящий тон с лёгкой вибрацией, 1.2 с
    "mass_driver/charge": ["synth", "1.2", "sine", "180-900", "tremolo", "18", "30",
                           "fade", "q", "0.1", "1.2", "0.15", "gain", "-6"],
    # Разряд: хлопок шума + нисходящий «вжух» 2 кГц → 80 Гц с экспоненциальным спадом
    "mass_driver/fire": ["synth", "0.9", "sine", "2000-80", "synth", "0.9", "brownnoise", "mix",
                         "fade", "p", "0", "0.9", "0.85", "overdrive", "12", "gain", "-12", "reverb", "40",
                         "norm", "-3"],
    # Возврат салазок: низкий гул мотора с тремоло, 2 с
    "mass_driver/sled": ["synth", "2.0", "sawtooth", "55", "synth", "2.0", "sine", "mix", "110",
                         "tremolo", "6", "40", "lowpass", "600", "fade", "t", "0.2", "2.0", "0.4",
                         "gain", "-10"],
    # Приём ловушкой: удар (шум) + металлический звон
    "mass_catcher/catch": ["synth", "1.4", "pinknoise", "synth", "1.4", "sine", "mix", "620",
                           "synth", "1.4", "sine", "mix", "1340", "fade", "p", "0", "1.4", "1.3",
                           "gain", "-10", "reverb", "60", "norm", "-4"],
    # Гул реактора: 60 Гц + гармоники, ровный — зацикливается раз в 3 с
    "regolith_reactor/hum": ["synth", "3.0", "sine", "60", "synth", "3.0", "sine", "mix", "120",
                             "synth", "3.0", "sine", "mix", "180", "synth", "3.0", "brownnoise", "mix",
                             "lowpass", "400", "fade", "t", "0.3", "3.0", "0.3", "gain", "-12"],
    # Выстрел орбитальной пушки: гром (коричневый шум) + металлический удар
    "orbital_cannon/fire": ["synth", "2.5", "brownnoise", "synth", "2.5", "sine", "mix", "900-40",
                            "fade", "p", "0", "2.5", "2.3", "overdrive", "18", "gain", "-12", "reverb", "80",
                            "norm", "-2"],
    # Пресс: глухой удар (коричневый шум) + металлический звон штампа
    "mechanical_press/stamp": ["synth", "0.6", "brownnoise", "synth", "0.6", "sine", "mix", "310",
                               "fade", "p", "0", "0.6", "0.55", "gain", "-10", "reverb", "30", "norm", "-4"],
    # Токарный станок: визг резца — узкополосный шум + тон 2.4 кГц с тремоло
    "lathe/cut": ["synth", "1.5", "pinknoise", "bandpass", "2400", "300", "synth", "1.5", "sine", "mix", "2400",
                  "tremolo", "11", "30", "fade", "t", "0.1", "1.5", "0.3", "gain", "-18"],
    # Мотор: гул 50 Гц (сеть) + 100 Гц магнитострикция, зацикливаемый 2 с
    "motor/hum": ["synth", "2.0", "sine", "50", "synth", "2.0", "sine", "mix", "100", "lowpass", "300",
                  "fade", "t", "0.2", "2.0", "0.2", "gain", "-14"],
}


def main():
    for name, args in SOUNDS.items():
        path = os.path.join(OUT, name + ".ogg")
        os.makedirs(os.path.dirname(path), exist_ok=True)
        subprocess.run(["sox", "-n", *RATE, path, *args], check=True)
        print("  ", os.path.relpath(path, ROOT))


if __name__ == "__main__":
    main()
