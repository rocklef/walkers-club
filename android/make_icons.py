"""Draws the Walkers Club launcher icon (lime footprints on cobalt) at every Android density."""
import os
from PIL import Image, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
SIZES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
COBALT, LIME, INK = (28, 63, 216), (217, 238, 87), (14, 26, 19)

def draw(px):
    S = 1024
    im = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    d.rounded_rectangle([0, 0, S - 1, S - 1], radius=230, fill=COBALT)
    d.ellipse([132, 132, 892, 892], fill=LIME)
    # two footprints, tilted slightly by offsetting
    def foot(cx, cy):
        d.ellipse([cx - 62, cy - 108, cx + 62, cy + 108], fill=INK)
        for dx, dy, r in ((-52, -150, 22), (-12, -168, 22), (30, -160, 20)):
            d.ellipse([cx + dx - r, cy + dy - r, cx + dx + r, cy + dy + r], fill=INK)
    foot(420, 590)
    foot(610, 470)
    return im.resize((px, px), Image.LANCZOS)

for name, px in SIZES.items():
    out = os.path.join(HERE, "res", f"mipmap-{name}")
    os.makedirs(out, exist_ok=True)
    draw(px).save(os.path.join(out, "ic_launcher.png"))
print("icons ok")
