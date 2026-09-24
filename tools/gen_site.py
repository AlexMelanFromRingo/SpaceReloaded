#!/usr/bin/env python3
"""Сайт SpaceReloaded для GitHub Pages: руководство (docs/index.html), книга рецептов
(docs/recipes.html) и мультиблоки (docs/multiblocks.html).

Иконки, рецепты и схемы берутся из реальных ресурсов мода и клиентского jar Minecraft
(модели резолвятся обобщённо, блоки рендерятся изометрически). Русские имена ванили — из
ru_ru.json хранилища ассетов Loom. Стили и скрипт: docs/assets/site.css, docs/assets/site.js.

Запуск: python3 tools/gen_site.py  (ненулевой код выхода, если у предмета нет иконки или имени)."""
import shutil, sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

from sitegen import res, icons, mb, pages, content as C  # noqa: E402


def main():
    recipes = pages.load_recipes()
    multiblocks = mb.load_all()
    html = {
        "index.html": pages.page("index.html", "SpaceReloaded · руководство",
                                 "Руководство по моду SpaceReloaded для Minecraft 26.2: ракеты по Циолковскому, "
                                 "герметичные базы, ISRU, межпланетная логистика, лунная индустрия и инженерия.",
                                 "guide", pages.guide_page()),
        "recipes.html": pages.page("recipes.html", "SpaceReloaded · книга рецептов",
                                   f"Все {len(recipes)} рецептов SpaceReloaded по фазам прогрессии: "
                                   "поиск, фильтр по станку, пояснение к каждому предмету.",
                                   "recipes", pages.recipes_page(recipes)),
        "multiblocks.html": pages.page("multiblocks.html", "SpaceReloaded · мультиблоки",
                                       "Схемы мультиблоков SpaceReloaded по слоям: электролизный стек, "
                                       "ректификационная колонна, реголитовый реактор, электромагнитная катапульта.",
                                       "multiblocks", pages.multiblocks_page(multiblocks)),
    }
    stale = icons.finalize()

    if pages.PROBLEMS:
        print("ОШИБКА: у предметов нет иконки или человеческого имени:", file=sys.stderr)
        for ctx, rid, what in sorted(set(pages.PROBLEMS)):
            print(f"  {ctx}: {rid} — {what}", file=sys.stderr)
        sys.exit(1)

    res.DOCS.mkdir(exist_ok=True)
    (res.DOCS / "img").mkdir(exist_ok=True)
    shutil.copyfile(res.RES / f"assets/{res.MOD}/icon.png", res.DOCS / "img/favicon.png")
    for name, text in html.items():
        (res.DOCS / name).write_text(text, encoding="utf-8")

    results = {pages._short(r["result"]) for r in recipes}
    missing = sorted(n for n in results if n not in C.DESC)
    no_phase = sorted(pages._short(r["result"]) for r in recipes
                      if r["file"] not in C.RECIPE_PHASE and pages._short(r["result"]) not in C.PHASE)
    size = sum(p.stat().st_size for p in res.DOCS.rglob("*") if p.is_file())
    print(f"{', '.join(html)} written to docs/. Рецептов: {len(recipes)}"
          f" (файлов {sum(len(r.get('steps', [])) or 1 for r in recipes)})"
          f" | мультиблоков: {len(multiblocks)} | иконок: {len(icons._written)} (удалено устаревших: {stale})"
          f" | docs/: {size / 1e6:.2f} МБ")
    print("без описания:", missing or "нет", "| без фазы:", no_phase or "нет")


if __name__ == "__main__":
    main()
