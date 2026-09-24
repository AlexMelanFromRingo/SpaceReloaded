"""Иконки предметов и блоков: плоские (item/generated) и изометрические (блочные модели).

Файлы кладутся в docs/img/icons/<ns>__<id>.png; устаревшие удаляются в finalize()."""
from . import res, render

ICON_DIR = res.DOCS / "img/icons"
ICON_PX = 96          # итоговый размер изометрической иконки
SS = 3                # суперсэмплинг рендера
_cache, _written = {}, set()


class Icon:
    def __init__(self, src, flat):
        self.src, self.flat = src, flat


def _fname(item_id):
    ns, _, name = item_id.partition(":")
    return f"{ns}__{name}.png"


def model_for(item_id):
    """Разрешённая модель предмета (для special-сундука — синтетическая)."""
    if item_id == "minecraft:chest" or (res.is_special(item_id) and item_id.endswith("chest")):
        return render.CHEST_MODEL
    mid = res.item_model_id(item_id)
    if mid is None:
        # предмет без items/*.json: пробуем модель блока/предмета напрямую
        ns, _, name = item_id.partition(":")
        for cand in (f"{ns}:item/{name}", f"{ns}:block/{name}"):
            try:
                return res.resolve_model(cand)
            except res.ResourceError:
                pass
        return None
    return res.resolve_model(mid)


def get(item_id):
    """Icon или None, если иконку построить не удалось."""
    if item_id in _cache:
        return _cache[item_id]
    icon = None
    if item_id != "minecraft:air":
        model = model_for(item_id)
        img, flat = None, False
        if model is not None:
            if model.get("elements"):
                faces = render.model_faces(model)
                S = ICON_PX * SS
                img, _ = render.draw_faces(faces, S)
                if img is not None:
                    img = render.fit_square(img, ICON_PX, ref=S)
            elif "layer0" in model["textures"]:
                img, flat = render.flat_layers(model), True
        if img is not None and img.getchannel("A").getbbox():
            ICON_DIR.mkdir(parents=True, exist_ok=True)
            fn = _fname(item_id)
            img.save(ICON_DIR / fn, optimize=True)
            _written.add(fn)
            icon = Icon(f"img/icons/{fn}", flat)
    _cache[item_id] = icon
    return icon


def finalize():
    """Удаляет иконки, не использованные в этом прогоне."""
    removed = 0
    if ICON_DIR.exists():
        for p in ICON_DIR.glob("*.png"):
            if p.name not in _written:
                p.unlink(); removed += 1
    return removed
