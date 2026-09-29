"""Сборка текстур 32×32 в ресурсы мода и отчёт покрытия.

python3 -m tex32.build            — все группы
python3 -m tex32.build energy     — только группа (модуль tex32/energy.py)
python3 -m tex32.build --check    — только отчёт: какие текстуры мода ещё не перерисованы
"""
import importlib
import os
import pkgutil
import sys

from PIL import Image

from . import GROUP, REG
from .kit import Tex

ROOT = os.path.join(os.path.dirname(__file__), "..", "..", "mod", "src", "main", "resources", "assets",
                    "spacereloaded", "textures")


def load_groups(only=None):
    pkg = os.path.dirname(__file__)
    for m in pkgutil.iter_modules([pkg]):
        if m.name in ("kit", "build", "preview") or m.name.startswith("_"):
            continue
        if only and m.name not in only:
            continue
        importlib.import_module(f"tex32.{m.name}")


def existing():
    out = []
    for sub in ("block", "item"):
        d = os.path.join(ROOT, sub)
        for f in sorted(os.listdir(d)):
            if f.endswith(".png"):
                out.append(f"{sub}/{f[:-4]}")
    return out


def render(name):
    r = REG[name](name)
    frames = r if isinstance(r, list) else [r]
    imgs = [f.img if isinstance(f, Tex) else f for f in frames]
    # 64×64 — только текущие жидкости: игра растягивает кадр flow на половину грани, как у ванильной воды
    size = 64 if name.endswith("_flow") else 32
    for im in imgs:
        if im.size != (size, size):
            raise ValueError(f"{name}: кадр {im.size}, нужен {size}×{size}")
    if len(imgs) == 1:
        return imgs[0]
    strip = Image.new("RGBA", (size, size * len(imgs)))
    for i, im in enumerate(imgs):
        strip.paste(im, (0, size * i))
    return strip


def main(argv):
    check = "--check" in argv
    only = [a for a in argv if not a.startswith("--")]
    load_groups(only or None)
    have = set(existing())
    unknown = sorted(n for n in REG if n not in have)
    if unknown:
        print("новые текстуры (их ещё нет в моде — проверьте, что модели на них ссылаются):", ", ".join(unknown))
    if not check:
        for name in sorted(REG):
            if only and GROUP[name] not in only:
                continue
            img = render(name)
            img.save(os.path.join(ROOT, name + ".png"))
        print(f"записано: {sum(1 for n in REG if not only or GROUP[n] in only)}")
    if check or not only:
        load_groups()
        missing = sorted(have - set(REG))
        print(f"покрыто {len(have) - len(missing)} из {len(have)}; не перерисовано: {len(missing)}")
        for m in missing:
            print("  ", m)


if __name__ == "__main__":
    main(sys.argv[1:])
