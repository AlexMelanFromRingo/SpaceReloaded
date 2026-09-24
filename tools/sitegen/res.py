"""Доступ к ресурсам: ресурсы мода, клиентский jar Minecraft, русские имена.

Всё резолвится обобщённо: items/<id>.json -> модель -> цепочка parent ->
текстуры/элементы. Ручных таблиц «id -> текстура» нет."""
import io, json, zipfile
from pathlib import Path

from PIL import Image

REPO = Path(__file__).resolve().parents[2]
RES = REPO / "mod/src/main/resources"
MOD = "spacereloaded"
DOCS = REPO / "docs"
LOOM = Path.home() / ".gradle/caches/fabric-loom"
CLIENT_JAR = LOOM / "26.2/minecraft-client.jar"
ASSETS = LOOM / "assets"


class ResourceError(Exception):
    pass


_client = zipfile.ZipFile(CLIENT_JAR)


def _split(rid, default_ns="minecraft"):
    ns, sep, path = rid.partition(":")
    return (ns, path) if sep else (default_ns, rid)


def read_bytes(ns, path):
    """assets/<ns>/<path> из ресурсов мода или клиентского jar."""
    if ns == MOD:
        p = RES / "assets" / ns / path
        return p.read_bytes() if p.exists() else None
    try:
        return _client.read(f"assets/{ns}/{path}")
    except KeyError:
        return None


def read_json(ns, path):
    b = read_bytes(ns, path)
    return json.loads(b) if b is not None else None


# ---------- имена ----------

def _vanilla_ru():
    """ru_ru.json ванили лежит не в jar, а в хранилище ассетов (по хэшу из индекса)."""
    for idx in sorted((ASSETS / "indexes").glob("*.json"), reverse=True):
        obj = json.loads(idx.read_text()).get("objects", {}).get("minecraft/lang/ru_ru.json")
        if obj:
            h = obj["hash"]
            p = ASSETS / "objects" / h[:2] / h
            if p.exists():
                return json.loads(p.read_text(encoding="utf-8"))
    raise ResourceError("minecraft/lang/ru_ru.json не найден в " + str(ASSETS))


LANG_MOD = json.loads((RES / f"assets/{MOD}/lang/ru_ru.json").read_text(encoding="utf-8"))
LANG_MC = _vanilla_ru()
EXTRA_NAMES = {"minecraft:air": "Воздух (пусто)"}


def ru_name(item_id):
    """Русское имя предмета/блока или None."""
    if item_id in EXTRA_NAMES:
        return EXTRA_NAMES[item_id]
    ns, name = _split(item_id)
    lang = LANG_MOD if ns == MOD else LANG_MC
    return lang.get(f"item.{ns}.{name}") or lang.get(f"block.{ns}.{name}")


# ---------- модели ----------

def item_model_id(item_id):
    """items/<id>.json -> id модели (первая "model"-строка; для special берётся base)."""
    ns, name = _split(item_id)
    d = read_json(ns, f"items/{name}.json")
    if d is None:
        return None

    def find(node):
        if isinstance(node, dict):
            t = node.get("type", "")
            if t.endswith("special") and isinstance(node.get("base"), str):
                return node["base"]
            if isinstance(node.get("model"), str):
                return node["model"]
            for key in ("model", "fallback", "on_false", "on_true", "cases", "entries", "models"):
                if key in node:
                    r = find(node[key])
                    if r:
                        return r
        elif isinstance(node, list):
            for x in node:
                r = find(x)
                if r:
                    return r
        return None

    return find(d.get("model"))


def is_special(item_id):
    ns, name = _split(item_id)
    d = read_json(ns, f"items/{name}.json")
    return d is not None and '"minecraft:special"' in json.dumps(d)


def resolve_model(model_id):
    """Сливает цепочку parent: textures (дочерние перекрывают), elements (первые встреченные).
    Возвращает dict(textures, elements, generated)."""
    textures, elements, generated, chain = {}, None, False, []
    mid = model_id
    while mid:
        if mid.startswith("builtin/") or mid.endswith(":builtin/generated"):
            generated = generated or mid.endswith("generated")
            break
        ns, path = _split(mid)
        d = read_json(ns, f"models/{path}.json")
        if d is None:
            raise ResourceError(f"модель {mid} не найдена")
        chain.append(mid)
        for k, v in d.get("textures", {}).items():
            textures.setdefault(k, v)
        if elements is None and "elements" in d:
            elements = d["elements"]
        mid = d.get("parent")
    return {"textures": textures, "elements": elements, "generated": generated, "chain": chain}


def texture_ref(textures, ref, depth=0):
    """#var -> id текстуры."""
    if isinstance(ref, dict):
        ref = ref.get("sprite")
    if ref is None or depth > 10:
        return None
    if ref.startswith("#"):
        return texture_ref(textures, textures.get(ref[1:]), depth + 1)
    return ref


def load_texture(tex_id):
    """id текстуры -> RGBA Image (первый кадр анимации)."""
    ns, path = _split(tex_id)
    b = read_bytes(ns, f"textures/{path}.png")
    if b is None:
        return None
    im = Image.open(io.BytesIO(b)).convert("RGBA")
    w, h = im.size
    if h > w and h % w == 0:
        im = im.crop((0, 0, w, w))
    return im


# ---------- датапак ----------

def tag_values(tag, kind="block"):
    """#ns:tag -> список id (рекурсивно). kind: block | item."""
    ns, name = _split(tag.lstrip("#"))
    rel = f"data/{ns}/tags/{kind}/{name}.json"
    p = RES / rel
    if p.exists():
        d = json.loads(p.read_text())
    else:
        try:
            d = json.loads(_client.read(rel))
        except KeyError:
            raise ResourceError(f"тег {tag} ({kind}) не найден")
    out = []
    for v in d["values"]:
        vid = v if isinstance(v, str) else v["id"]
        out.extend(tag_values(vid, kind) if vid.startswith("#") else [vid])
    return out
