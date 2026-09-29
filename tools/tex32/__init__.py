"""Текстуры 32×32 SpaceReloaded (012). Каждый модуль группы регистрирует свои текстуры декоратором @tex;
сборка — python3 -m tex32.build (из tools/). Имя — путь в textures/ без .png: "block/battery_0", "item/steel_ingot"."""
REG = {}
GROUP = {}


def tex(*names, group=None):
    """Регистрирует функцию рисования для имён; функция получает имя и возвращает Tex или список Tex (кадры)."""
    def deco(fn):
        for n in names:
            if n in REG:
                raise ValueError(f"текстура {n} зарегистрирована дважды")
            REG[n] = fn
            GROUP[n] = group or fn.__module__.rsplit(".", 1)[-1]
        return fn
    return deco
