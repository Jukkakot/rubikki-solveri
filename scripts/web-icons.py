"""Draw the browser version's icons from the launcher icon (one-off, kept for redo).

Usage: python scripts/web-icons.py

Reads the stickers of app/src/main/res/drawable/ic_launcher_foreground.xml (a 108-unit adaptive-icon
canvas) and draws them on the launcher background #1E2A3A into web/src/wasmJsMain/resources/icons/:
icon-192.png and icon-512.png (the cube a little larger, rounded corners) and icon-maskable-512.png
(full-bleed background, the cube inside the 80 % safe zone, as on Android). Needs Pillow.
"""
import re
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main/res/drawable/ic_launcher_foreground.xml"
OUT = ROOT / "web/src/wasmJsMain/resources/icons"
BACKGROUND = (0x1E, 0x2A, 0x3A, 255)


def stickers():
    """(argb colour, x, y, w, h) per sticker in the 108-unit canvas."""
    xml = SOURCE.read_text(encoding="utf-8")
    for color, x, y, w, h in re.findall(r'fillColor="#([0-9A-Fa-f]{8})"\s+android:pathData="M(\d+),(\d+)h(\d+)v(\d+)', xml):
        a, r, g, b = (int(color[i:i + 2], 16) for i in (0, 2, 4, 6))
        yield (r, g, b, a), int(x), int(y), int(w), int(h)


def draw(size: int, maskable: bool) -> Image.Image:
    # The stickers span 33..75 of 108; a plain icon zooms in so the cube fills more of it.
    scale = size / 108 if maskable else size / 72
    offset = 0 if maskable else -18 * scale
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    radius = 0 if maskable else size // 5
    d.rounded_rectangle([0, 0, size - 1, size - 1], radius=radius, fill=BACKGROUND)
    for color, x, y, w, h in stickers():
        box = [offset + x * scale, offset + y * scale, offset + (x + w) * scale, offset + (y + h) * scale]
        d.rounded_rectangle(box, radius=max(1, int(2 * scale)), fill=color)
    return img


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    draw(192, False).save(OUT / "icon-192.png")
    draw(512, False).save(OUT / "icon-512.png")
    draw(512, True).save(OUT / "icon-maskable-512.png")
    print("icons written to", OUT.relative_to(ROOT))


if __name__ == "__main__":
    main()
