"""Generate SNC 90 harvester materials at exactly 128 x 128.

Standalone modelling/preview assets (not the game registry). Shares the
SNC 75 texture kit: seamless noise surfaces, supersampled Painter and
screw hardware. Harvester-specific painters: rotary thresher drum,
cutter bar, auger, chopper, belt drive, Hydro4 hydrostatic decal, tinted
cab glass and a pale straw/ear corn crop decal.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from vehicle_texture_kit import Painter, screw, surface

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "assets" / "harvester"
TEXTURE_DIR = ASSET_DIR / "textures"

MATERIALS = {
    "enamel_orange": {"color": "#e57525", "roughness": 0.4, "metalness": 0.25},
    "enamel_dark": {"color": "#2c3438", "roughness": 0.48, "metalness": 0.35},
    "rubber": {"color": "#25292a", "roughness": 0.95, "metalness": 0.0},
    "tread": {"color": "#303435", "roughness": 0.98, "metalness": 0.0},
    "steel": {"color": "#75848a", "roughness": 0.42, "metalness": 0.82},
    "rim": {"color": "#c3c5b8", "roughness": 0.4, "metalness": 0.65},
    "grain_tank": {"color": "#d9d6bd", "roughness": 0.58, "metalness": 0.05},
    "grille": {"color": "#273338", "roughness": 0.62, "metalness": 0.72},
    "thresher": {"color": "#8a969c", "roughness": 0.38, "metalness": 0.85},
    "auger": {"color": "#8a969c", "roughness": 0.42, "metalness": 0.8},
    "chopper": {"color": "#6d7a81", "roughness": 0.44, "metalness": 0.78},
    "belt": {"color": "#25292a", "roughness": 0.95, "metalness": 0.0},
    "cutter_bar": {"color": "#aab3b8", "roughness": 0.3, "metalness": 0.9},
    "glass": {"color": "#8fb6c4", "roughness": 0.14, "metalness": 0.1},
    "headlight": {"color": "#ffe5ad", "roughness": 0.23, "metalness": 0.2},
    "taillight": {"color": "#c43725", "roughness": 0.28, "metalness": 0.05},
    "amber": {"color": "#f6a82d", "roughness": 0.26, "metalness": 0.05},
    "decal_snc": {"color": "#e57525", "roughness": 0.5, "metalness": 0.12},
    "decal_crop": {"color": "#e0c46d", "roughness": 0.55, "metalness": 0.05},
    "gauge": {"color": "#1d292e", "roughness": 0.3, "metalness": 0.1},
    "seat": {"color": "#252e30", "roughness": 0.9, "metalness": 0.0},
}


def paint_thresher():
    """Rotary thresher drum: radial cage bars over a steel shell."""
    p = Painter(surface(MATERIALS["thresher"]["color"], 171, grain=3.4, brushed=3))
    p.ellipse((14, 14, 113, 113), "#5f6d73", "#39464b", 2.4)
    p.ellipse((22, 22, 105, 105), "#75828a")
    for angle in range(0, 360, 30):
        radians = math.radians(angle)
        cx, cy = 63.5, 63.5
        x0, y0 = cx + math.cos(radians) * 12, cy + math.sin(radians) * 12
        x1, y1 = cx + math.cos(radians) * 45, cy + math.sin(radians) * 45
        p.line((x0, y0, x1, y1), "#4c5a61", 3.4)
        p.line((x0 + 1.4, y0, x1 + 1.4, y1), "#aeb9be", 0.8)
    p.ellipse((52, 52, 75, 75), "#39464b", "#22303a", 1.4)
    p.ellipse((56, 56, 71, 71), "#57646b")
    for x, y in ((16, 16), (111, 16), (16, 111), (111, 111)):
        screw(p, x, y, 1.8)
    return p.finish()


def paint_cutter():
    """Cutter bar: alternating knife sections with ledger plates."""
    p = Painter(surface(MATERIALS["cutter_bar"]["color"], 173, grain=2.2, brushed=4))
    for x in range(6, 122, 16):
        p.line((x, 20, x + 9, 107), "#77848b", 5.2)
        p.line((x + 3, 22, x + 11, 104), "#d7dee0", 1.6)
        p.rect((x - 1, 16, x + 12, 22), "#3c4a50")
        p.rect((x - 1, 105, x + 12, 111), "#3c4a50")
    p.rect((2, 8, 125, 13), "#242e33")
    p.rect((2, 114, 125, 119), "#242e33")
    for y in (5, 122):
        for x in (10, 42, 74, 106):
            screw(p, x, y, 1.5)
    return p.finish()


def paint_auger():
    """Unloading auger: helical flighting wrapping a pipe."""
    p = Painter(surface(MATERIALS["thresher"]["color"], 177, grain=3, brushed=2))
    for y in range(-10, 140, 18):
        p.line((0, y + 14, 127, y), "#5b686f", 6)
        p.line((0, y + 10.5, 127, y - 3.5), "#b9c2c7", 1.8)
        p.line((0, y + 17, 127, y + 3), "#2c383e", 1.4)
    p.rect((0, 58, 127, 70), "#49565d")
    p.line((0, 59, 127, 59), "#8b989e", 1.2)
    p.line((0, 68, 127, 68), "#1f2a30", 1.4)
    return p.finish()


def paint_chopper():
    """Straw chopper: staggered free-swinging knife edges."""
    p = Painter(surface("#6d7a81", 179, grain=3, brushed=2.4))
    for i, x in enumerate(range(8, 121, 15)):
        y0 = 26 if i % 2 == 0 else 40
        y1 = y0 + 44
        p.line((x, y0, x + 7, y1), "#39464c", 5)
        p.line((x + 3, y0 + 2, x + 9, y1), "#c9d1d5", 1.8)
        p.ellipse((x + 1, y0 - 8, x + 11, y0 + 2), "#2a343a", "#141d22", 1)
        p.line((x + 6, y0 - 6, x + 6, y0), "#7f8c92", 1)
    return p.finish()


def paint_belt():
    """V-belt drive: rubber band with visible pulley contact sheen."""
    p = Painter(surface(MATERIALS["rubber"]["color"], 181, grain=4))
    for y in (30, 38, 90, 98):
        p.line((0, y, 127, y), "#1c2021", 1.6)
    for y in (34, 94):
        p.line((0, y, 127, y), "#3a4143", 0.9)
    for x in range(8, 128, 21):
        p.line((x, 16, x, 30), "#171b1c", 2.2)
        p.line((x, 98, x, 112), "#171b1c", 2.2)
    return p.finish()


def paint_glass():
    """Tinted cab glass with a light sky reflection sweep."""
    p = Painter(surface(MATERIALS["glass"]["color"], 183, grain=1.6))
    p.rect((2, 2, 125, 125), "#7fb0c0", "#5d8fa1", 3, radius=9)
    p.line((14, 112, 78, 16), "#c9e4ec", 5)
    p.line((24, 114, 88, 18), "#e9f5f8", 2.2)
    p.line((96, 110, 112, 30), "#a8ccd8", 3)
    for x in (10, 117):
        for y in (10, 117):
            screw(p, x, y, 1.4)
    return p.finish()


def paint_lens(name: str, seed: int):
    colors = {
        "headlight": ("#c8b37d", "#f9dc9c", "#fff5d5", "#a79265"),
        "taillight": ("#7b211c", "#c73522", "#ed6e3a", "#781e1b"),
        "amber": ("#a15a17", "#e99422", "#ffce62", "#8b5017"),
    }
    border, base, highlight, groove = colors[name]
    p = Painter(surface(base, seed, grain=4))
    p.rect((1, 1, 126, 126), base, border, 4, radius=13)
    for x in range(12, 121, 9):
        p.line((x, 10, x, 117), highlight, 1.4)
        p.line((x + 2.2, 10, x + 2.2, 117), groove, 0.5)
    for y in range(18, 118, 15):
        p.line((10, y, 117, y), border, 0.5)
    p.line((16, 8, 111, 8), highlight, 1.8)
    p.line((12, 12, 12, 105), highlight, 1.1)
    return p.finish()


def paint_decal():
    """Hydro4 brand plate: model stripe with hydrostatic callout."""
    p = Painter(surface("#e57525", 191, grain=2.8))
    p.rect((3, 14, 124, 113), "#1d2a2f", "#ef9b50", 1.5, radius=7)
    p.line((11, 23, 116, 23), "#5f6663", 0.65)
    p.text((64, 44), "SNC", 30, "#f5eee0", True)
    p.rect((16, 63, 112, 65), "#e57525")
    p.text((64, 84), "90", 30, "#f1a052", True)
    p.rect((30, 100, 98, 109), "#0f1a1f", "#4c5a5b", 0.6, radius=2)
    p.text((64, 104.5), "HYDRO4", 6, "#b9c9c0", True)
    for x in (10, 117):
        for y in (21, 106):
            screw(p, x, y, 1.6)
    return p.finish()


def paint_crop_decal():
    """Pale straw and ear corn artwork for the tank body."""
    p = Painter(surface("#e0c46d", 193, grain=3.2))
    p.rect((4, 16, 123, 111), "#caa94e", "#8a6d2a", 1.6, radius=6)
    for x in (16, 46, 76, 106):
        p.line((x, 30, x, 96), "#a8843a", 2.6)
        p.ellipse((x - 4, 24, x + 4, 38), "#d9b95e", "#8a6d2a", 1)
        for dy in range(0, 10, 3):
            p.line((x - 3, 27 + dy, x + 3, 27 + dy), "#8a6d2a", 0.7)
        p.line((x - 4, 40, x + 4, 40), "#8a6d2a", 1)
        p.line((x - 4, 52, x + 4, 52), "#8a6d2a", 1)
    for x in (10, 118):
        for y in (22, 104):
            screw(p, x, y, 1.6)
    return p.finish()


def paint_gauge():
    p = Painter(surface("#253237", 197, grain=2.0))
    p.rect((2, 2, 125, 125), "#28383e", "#6a7c80", 1, radius=8)
    for x in (9, 118):
        for y in (9, 118):
            screw(p, x, y, 2)
    p.ellipse((17, 12, 111, 106), "#09161c", "#8e9d9d", 2)
    p.ellipse((22, 17, 106, 101), "#122329", "#30464b", 1)
    p.arc((28, 23, 100, 95), 138, 310, "#a9c9bd", 2)
    p.arc((28, 23, 100, 95), 311, 344, "#df6337", 2.3)
    for index in range(25):
        degrees = 136 + index * 8.7
        outer = radial(64, 59, 34, degrees)
        inner = radial(64, 59, 28 if index % 4 == 0 else 31, degrees)
        p.line((*inner, *outer), "#dee8d7", 1 if index % 4 == 0 else 0.55)
    for index, value in enumerate(("0", "1", "2", "3", "4", "5", "6")):
        p.text(radial(64, 59, 23, 136 + index * 34.8), value, 5.5, "#dae5d4")
    p.text((64, 50), "RPM", 6, "#92acaa", True)
    p.text((64, 75), "x100", 5, "#92acaa")
    p.line((*radial(64, 59, 8, 66), *radial(64, 59, 28, 245)), "#e77a3b", 1.6)
    p.ellipse((60.7, 55.7, 67.3, 62.3), "#a5b2ad", "#273e43", 0.5)
    p.rect((45, 83, 83, 91), "#08181b", "#526a64", 0.6, radius=1)
    p.text((64, 87), "0090.0", 5.5, "#b4c9a6")
    for x, fill, label in ((37, "#c07733", "OIL"), (64, "#7baa72", "PWR"), (91, "#517682", "TEMP")):
        p.ellipse((x - 3.1, 106, x + 3.1, 112.2), fill, "#111f24", 0.8)
        p.text((x, 118), label, 4.3, "#aebeb8")
    return p.finish()


def radial(cx, cy, radius, degrees):
    radians = math.radians(degrees)
    return cx + math.cos(radians) * radius, cy + math.sin(radians) * radius


def paint_rubber(tread: bool = False):
    p = Painter(surface(MATERIALS["tread" if tread else "rubber"]["color"], 199, grain=4))
    if tread:
        for y in range(-32, 161, 32):
            p.line((0, y + 26, 61, y + 3, 64, y + 3, 127, y + 26), "#24292a", 4)
            p.line((0, y + 22, 61, y - 1, 64, y - 1, 127, y + 22), "#393e3e", 1.6)
    else:
        for y in (27, 30, 96, 99):
            p.line((0, y, 127, y), "#2a2f30", 0.65)
    return p.finish()


def paint_grille():
    p = Painter(surface("#29353a", 201, grain=3, brushed=1.5))
    p.rect((6, 6, 121, 121), "#101c21", "#637177", 1.1, radius=3)
    p.rect((10, 10, 117, 117), "#152329", radius=2)
    for x in range(15, 117, 8):
        p.line((x, 11, x, 116), "#26383f", 1.4)
    for y in range(14, 116, 8):
        p.rect((11, y, 116, y + 3.8), "#39484e")
        p.line((12, y, 115, y), "#647177", 0.8)
        p.line((12, y + 3.8, 115, y + 3.8), "#0c171c", 1)
    for x in (4.1, 123):
        for y in (5, 63.5, 122):
            screw(p, x, y, 1.5)
    return p.finish()


def paint_seat():
    p = Painter(surface({"color": "#252e30"}["color"], 203, grain=5))
    for x in (18, 109):
        p.line((x, 0, x, 127), "#1d2528", 1.7)
        for y in range(2, 128, 6):
            p.line((x + 2, y, x + 2, y + 2.3), "#5a6260", 0.65)
    for y in (36, 61, 86):
        p.line((23, y, 104, y), "#1c2527", 1.3)
        p.line((25, y + 1.3, 102, y + 1.3), "#343d3f", 0.7)
    return p.finish()


def main():
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    images = {
        "enamel_orange": surface(MATERIALS["enamel_orange"]["color"], 141, grain=3.2),
        "enamel_dark": surface(MATERIALS["enamel_dark"]["color"], 149, grain=2.8),
        "rubber": paint_rubber(),
        "tread": paint_rubber(True),
        "steel": surface(MATERIALS["steel"]["color"], 151, grain=3.2, brushed=4),
        "rim": surface(MATERIALS["rim"]["color"], 157, grain=2.6, brushed=1),
        "grain_tank": surface(MATERIALS["grain_tank"]["color"], 163, grain=3.3),
        "grille": paint_grille(),
        "thresher": paint_thresher(),
        "cutter_bar": paint_cutter(),
        "auger": paint_auger(),
        "chopper": paint_chopper(),
        "belt": paint_belt(),
        "glass": paint_glass(),
        "headlight": paint_lens("headlight", 211),
        "taillight": paint_lens("taillight", 223),
        "amber": paint_lens("amber", 227),
        "decal_snc": paint_decal(),
        "decal_crop": paint_crop_decal(),
        "gauge": paint_gauge(),
        "seat": paint_seat(),
    }
    manifest = {}
    for name, image in images.items():
        assert image.size == (128, 128), f"Invalid texture dimensions: {name} {image.size}"
        image.save(TEXTURE_DIR / f"{name}.png", optimize=True)
        manifest[name] = {"file": f"textures/{name}.png", **MATERIALS[name]}
    (ASSET_DIR / "materials.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(images)} harvester material textures, all 128x128, in {TEXTURE_DIR}")


if __name__ == "__main__":
    main()
