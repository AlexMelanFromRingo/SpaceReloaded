"""Превью группы: python3 -m tex32.preview energy [out.png] — каждая текстура ×4 с подписью и плиткой 2×2
(проверка стыков), рядом старая версия из git (HEAD) для сравнения."""
import os
import subprocess
import sys

from PIL import Image, ImageDraw

from . import GROUP, REG
from .build import ROOT, load_groups, render

SCALE = 4


def old(name):
    rel = f"mod/src/main/resources/assets/spacereloaded/textures/{name}.png"
    try:
        data = subprocess.run(["git", "show", f"main:{rel}"], capture_output=True, check=True,
                              cwd=os.path.join(os.path.dirname(__file__), "..", "..")).stdout
        from io import BytesIO
        return Image.open(BytesIO(data)).convert("RGBA")
    except Exception:
        return None


def main(argv):
    group = argv[0]
    out = argv[1] if len(argv) > 1 else f"/tmp/claude-1000/tex32_{group}.png"
    load_groups([group])
    names = sorted(n for n in REG if GROUP[n] == group)
    cell_w, cell_h = 32 * SCALE * 2 + 64 + 32 * 2 + 24, 32 * SCALE + 30
    cols = 2
    rows = (len(names) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell_w, max(1, rows) * cell_h), (34, 36, 40, 255))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(names):
        x0, y0 = (i % cols) * cell_w + 8, (i // cols) * cell_h + 4
        img = render(n)
        first = img.crop((0, 0, img.width, img.width)).resize((32, 32), Image.BOX)
        sheet.paste(first.resize((32 * SCALE, 32 * SCALE), Image.NEAREST), (x0, y0), first.resize((32 * SCALE, 32 * SCALE), Image.NEAREST))
        tile = Image.new("RGBA", (64, 64))
        for tx in (0, 32):
            for ty in (0, 32):
                tile.paste(first, (tx, ty))
        sheet.paste(tile.resize((128, 128), Image.NEAREST), (x0 + 32 * SCALE + 8, y0))
        o = old(n)
        if o is not None:
            o = o.crop((0, 0, o.width, o.width))
            sheet.paste(o.resize((64, 64), Image.NEAREST), (x0 + 32 * SCALE + 8 + 136, y0), o.resize((64, 64), Image.NEAREST))
        d.text((x0, y0 + 32 * SCALE + 2), n, fill=(220, 225, 230, 255))
    sheet.save(out)
    print(out, len(names))


if __name__ == "__main__":
    main(sys.argv[1:])
