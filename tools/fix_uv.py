#!/usr/bin/env python3
"""Разовый проход (012): вписать UV всех моделей мода в 0..16 — грани элементов, выходящих за блок,
иначе берут пиксели соседних текстур атласа. Генераторы делают то же при записи (modelkit.fix_uv)."""
import glob
import json
import os

from modelkit import ASSETS, fix_uv, write

total = 0
for path in sorted(glob.glob(os.path.join(ASSETS, "models", "**", "*.json"), recursive=True)):
    with open(path, encoding="utf-8") as f:
        obj = json.load(f)
    n = fix_uv(obj)
    if n:
        write(path, obj)
        print(f"{os.path.relpath(path, ASSETS)}: {n}")
        total += n
print("исправлено граней:", total)
