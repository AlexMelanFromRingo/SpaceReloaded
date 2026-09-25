"""3D-модели предметов мода для просмотрщика сайта (docs/assets/viewer3d.js).

Для каждого предмета SpaceReloaded выгружается разрешённая модель: элементы (from/to/поворот), грани
с UV (по умолчанию — как у FaceBakery), поворотом текстуры и индексом текстуры; текстуры кладутся
первым кадром анимации в docs/models/tex/. Плоские предметы (item/generated) выгружаются одной
склеенной текстурой — просмотрщик выдавливает её по пикселям, как игра.

Файлы: docs/models/<ns>__<id>.json; устаревшие удаляются в finalize()."""
import json

from . import icons, render, res

MODEL_DIR = res.DOCS / "models"
TEX_DIR = MODEL_DIR / "tex"
_cache, _written, _tex_written = {}, set(), set()


def _tex_file(tex_id):
    ns, _, path = tex_id.partition(":")
    if not path:
        ns, path = "minecraft", tex_id
    fn = f"{ns}__{path.replace('/', '__')}.png"
    if fn not in _tex_written:
        im = res.load_texture(tex_id if ":" in tex_id else f"minecraft:{tex_id}")
        if im is None:
            return None
        TEX_DIR.mkdir(parents=True, exist_ok=True)
        im.save(TEX_DIR / fn, optimize=True)
        _tex_written.add(fn)
    return f"models/tex/{fn}"


def export(item_id):
    """Путь к JSON модели для просмотрщика или None (предмет не мода или модель не построить)."""
    if item_id in _cache:
        return _cache[item_id]
    out = None
    if item_id.startswith(res.MOD + ":"):
        try:
            model = icons.model_for(item_id)
        except res.ResourceError:
            model = None
        data = _elements(model) if model is not None and model.get("elements") else None
        if data is None and model is not None and "layer0" in model["textures"]:
            img = render.flat_layers(model)
            if img is not None:
                TEX_DIR.mkdir(parents=True, exist_ok=True)
                fn = f"item__{item_id.replace(':', '__')}.png"
                img.save(TEX_DIR / fn, optimize=True)
                _tex_written.add(fn)
                data = {"flat": f"models/tex/{fn}"}
        if data is not None:
            MODEL_DIR.mkdir(parents=True, exist_ok=True)
            fn = f"{item_id.replace(':', '__')}.json"
            (MODEL_DIR / fn).write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
            _written.add(fn)
            out = f"models/{fn}"
    _cache[item_id] = out
    return out


def texture_path(tex_id):
    """Путь текстуры для просмотрщика (кладёт файл в docs/models/tex)."""
    return _tex_file(tex_id)


def export_model(model):
    """Разрешённая блочная модель (вариант блокстейта мультиблока) → путь к JSON; одинаковые — один файл."""
    import hashlib
    key = hashlib.sha1(json.dumps(model, sort_keys=True).encode()).hexdigest()[:16]
    fn = f"blk__{key}.json"
    if fn not in _written:
        data = _elements(model) if model and model.get("elements") else None
        if data is None:
            return None
        MODEL_DIR.mkdir(parents=True, exist_ok=True)
        (MODEL_DIR / fn).write_text(json.dumps(data, separators=(",", ":")), encoding="utf-8")
        _written.add(fn)
    return f"models/{fn}"


def export_scene(name, parts):
    """Сцена мультиблока: [{m: модель, p: [x, y, z], x: поворот X, y: поворот Y}] → путь к JSON."""
    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    fn = f"mb__{name}.json"
    (MODEL_DIR / fn).write_text(json.dumps({"parts": parts}, separators=(",", ":")), encoding="utf-8")
    _written.add(fn)
    return f"models/{fn}"


def _elements(model):
    textures, index = [], {}
    elements = []
    for el in model["elements"]:
        faces = {}
        for name, fd in el.get("faces", {}).items():
            tex_id = res.texture_ref(model["textures"], fd.get("texture"))
            path = _tex_file(tex_id) if tex_id else None
            if path is None:
                continue
            if path not in index:
                index[path] = len(textures)
                textures.append(path)
            face = {"uv": fd.get("uv") or render.default_uv(name, el["from"], el["to"]), "t": index[path]}
            if fd.get("rotation"):
                face["r"] = fd["rotation"]
            faces[name] = face
        if not faces:
            continue
        e = {"from": el["from"], "to": el["to"], "faces": faces}
        if el.get("rotation") and el["rotation"].get("angle"):
            r = el["rotation"]
            e["rot"] = {"axis": r.get("axis", "y"), "angle": r["angle"], "origin": r.get("origin", [8, 8, 8]),
                        "rescale": bool(r.get("rescale"))}
        if el.get("shade") is False:
            e["noshade"] = True
        elements.append(e)
    return {"textures": textures, "elements": elements} if elements else None


def finalize():
    removed = 0
    for directory, keep, pattern in ((MODEL_DIR, _written, "*.json"), (TEX_DIR, _tex_written, "*.png")):
        if directory.exists():
            for p in directory.glob(pattern):
                if p.name not in keep:
                    p.unlink()
                    removed += 1
    return removed
