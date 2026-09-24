#!/usr/bin/env python3
"""Цвета блоков мода на ванильных картах и орбитальных снимках: «вид сверху».

Для каждого блока берутся грани «up» его модели (взвешенно по площади), средний цвет непрозрачных
пикселей текстуры сравнивается с палитрой MapColor (разобранной из байткода клиента) — ближайший
цвет по евклидову расстоянию в RGB. Прозрачный сверху блок (стекло) остаётся NONE, как ванильное
стекло. Пишет mod/src/main/java/.../registry/ModMapColors.java. Запуск: python3 tools/gen_map_colors.py
"""
import re
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from sitegen import res  # noqa: E402

JAVAP = Path.home() / ".sdkman/candidates/java/25.0.3-tem/bin/javap"
COMMON_JAR = res.LOOM / "26.2/minecraft-common.jar"
OUT = res.REPO / "mod/src/main/java/org/alex_melan/spacereloaded/registry/ModMapColors.java"


def palette():
    """[(поле, id, rgb)] из статического инициализатора MapColor."""
    with tempfile.TemporaryDirectory() as tmp:
        with zipfile.ZipFile(COMMON_JAR) as z:
            z.extract("net/minecraft/world/level/material/MapColor.class", tmp)
        out = subprocess.run([str(JAVAP), "-c", "-p", "-cp", tmp, "net.minecraft.world.level.material.MapColor"],
                             capture_output=True, text=True, check=True).stdout
    body = out[out.index("static {};"):]
    colors, nums = [], []
    for line in body.splitlines():
        m = re.search(r"(?:bipush|sipush|iconst_|ldc)\s*(\S+)?.*?(?://\s*int\s+(-?\d+))?$", line)
        if re.search(r"\biconst_(\d)", line):
            nums.append(int(re.search(r"\biconst_(\d)", line).group(1)))
        elif re.search(r"\b(bipush|sipush)\s+(-?\d+)", line):
            nums.append(int(re.search(r"\b(bipush|sipush)\s+(-?\d+)", line).group(2)))
        elif "// int" in line:
            nums.append(int(line.split("// int")[1]))
        f = re.search(r"putstatic .*// Field (\w+):Lnet/minecraft/world/level/material/MapColor;", line)
        if f:
            if len(nums) >= 2 and f.group(1) != "NONE":
                colors.append((f.group(1), nums[-2], nums[-1]))
            nums = []
    return [(name, i, ((c >> 16) & 255, (c >> 8) & 255, c & 255)) for name, i, c in colors]


def top_color(model_id):
    """Вид сверху: для каждой клетки 16×16 — самая высокая грань «up» над ней, пиксель её текстуры по UV."""
    m = res.resolve_model(model_id)
    faces = []
    for e in m["elements"] or []:
        face = e.get("faces", {}).get("up")
        if face is None or e.get("rotation") and e["rotation"].get("axis") != "y":
            continue
        tex = res.texture_ref(m["textures"], face.get("texture"))
        im = res.load_texture(tex) if tex else None
        if im is not None:
            (x1, _, z1), (x2, y2, z2) = e["from"], e["to"]
            uv = face.get("uv", [x1, z1, x2, z2])
            faces.append((y2, min(x1, x2), max(x1, x2), min(z1, z2), max(z1, z2), uv, im))
    if not faces:
        return None
    samples, seen = [], 0
    for cx in range(16):
        for cz in range(16):
            x, z = cx + 0.5, cz + 0.5
            over = [f for f in faces if f[1] <= x <= f[2] and f[3] <= z <= f[4]]
            if not over:
                continue
            seen += 1
            y2, x1, x2, z1, z2, uv, im = max(over, key=lambda f: f[0])
            w, h = im.size
            u = uv[0] + (x - x1) / max(1e-6, x2 - x1) * (uv[2] - uv[0])
            v = uv[1] + (z - z1) / max(1e-6, z2 - z1) * (uv[3] - uv[1])
            px = im.getpixel((min(w - 1, max(0, int(u / 16 * w))), min(h - 1, max(0, int(v / 16 * h)))))
            if px[3] > 127:
                samples.append(px)
    if not samples or len(samples) < 0.5 * max(seen, 1) or seen < 32:
        return None  # сверху прозрачен или почти пуст (стекло, провод) — на карте не виден
    n = len(samples)
    return (sum(p[0] for p in samples) / n, sum(p[1] for p in samples) / n, sum(p[2] for p in samples) / n)


def block_model(block):
    bs = res.read_json(res.MOD, f"blockstates/{block}.json")
    if bs is None:
        return None
    if "variants" in bs:
        v = next(iter(bs["variants"].values()))
        return (v[0] if isinstance(v, list) else v)["model"]
    for part in bs.get("multipart", []):
        a = part["apply"]
        return (a[0] if isinstance(a, list) else a)["model"]
    return None


def main():
    pal = palette()
    java = (res.REPO / "mod/src/main/java/org/alex_melan/spacereloaded/registry/ModBlocks.java").read_text(encoding="utf-8")
    blocks = re.findall(r'(?<!NoItem)\bregister\("([a-z0-9_]+)"', java)
    rows = []
    for b in sorted(set(blocks)):
        model = block_model(b)
        rgb = top_color(model) if model else None
        if rgb is None:
            continue
        name, _, _ = min(pal, key=lambda p: sum((a - c) ** 2 for a, c in zip(rgb, p[2])))
        rows.append((b, name))
    lines = "\n".join(f'        Map.entry("{b}", MapColor.{n}),' for b, n in rows).rstrip(",")
    OUT.write_text(f'''package org.alex_melan.spacereloaded.registry;

import net.minecraft.world.level.material.MapColor;

import java.util.Map;

/**
 * Цвет блоков мода на картах и орбитальных снимках — «вид сверху»: ближайший цвет палитры к среднему
 * цвету верхних граней модели. Сгенерировано tools/gen_map_colors.py, руками не править.
 */
public final class ModMapColors {{

    private static final Map<String, MapColor> COLORS = Map.ofEntries(
{lines});

    private ModMapColors() {{
    }}

    /** Цвет блока на карте; NONE — прозрачен сверху (стекло) или невидимая модель. */
    public static MapColor of(String block) {{
        return COLORS.getOrDefault(block, MapColor.NONE);
    }}
}}
''', encoding="utf-8")
    print(f"   {len(rows)} блоков с цветом карты, палитра {len(pal)} цветов")
    for b, n in rows:
        if b in ("hull_plating", "solar_panel", "launch_pad", "hermetic_glass", "moon_regolith", "gas_tank",
                 "hydroponic_tray", "imaging_satellite"):
            print(f"     {b}: {n}")


if __name__ == "__main__":
    main()
