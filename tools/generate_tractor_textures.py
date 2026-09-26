"""Generate the SNC 75 tractor authoring materials at exactly 128 x 128.

These assets belong to the standalone model/preview, not the game registry.
Surface colors are baked into each PNG. Use white tint with a texture map;
the manifest color is a useful untextured fallback and palette swatch.

The noise surface, supersampled Painter and screw hardware live in
tools/vehicle_texture_kit.py — the shared vehicle texture kit extracted
verbatim from this golden-reference generator (AGENTS.md rules 6 and 7).
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from vehicle_texture_kit import Painter, screw, surface

ROOT = Path(__file__).resolve().parents[1]
ASSET_DIR = ROOT / "assets" / "tractor"
TEXTURE_DIR = ASSET_DIR / "textures"

MATERIALS = {
    "enamel_orange": {"color": "#e57525", "roughness": 0.4, "metalness": 0.25},
    "enamel_dark": {"color": "#303a3e", "roughness": 0.48, "metalness": 0.35},
    "rubber": {"color": "#25292a", "roughness": 0.95, "metalness": 0.0},
    "tread": {"color": "#303435", "roughness": 0.98, "metalness": 0.0},
    "steel": {"color": "#75848a", "roughness": 0.42, "metalness": 0.82},
    "rim": {"color": "#c3c5b8", "roughness": 0.4, "metalness": 0.65},
    "seat": {"color": "#252e30", "roughness": 0.9, "metalness": 0.0},
    "grille": {"color": "#273338", "roughness": 0.62, "metalness": 0.72},
    "headlight": {"color": "#ffe5ad", "roughness": 0.23, "metalness": 0.2},
    "taillight": {"color": "#c43725", "roughness": 0.28, "metalness": 0.05},
    "amber": {"color": "#f6a82d", "roughness": 0.26, "metalness": 0.05},
    "seed_tank": {"color": "#dedbc2", "roughness": 0.58, "metalness": 0.05},
    "decal_snc": {"color": "#e57525", "roughness": 0.5, "metalness": 0.12},
    "gauge": {"color": "#1d292e", "roughness": 0.3, "metalness": 0.1},
}


def paint_rubber(tread: bool = False):
    p = Painter(surface(MATERIALS["tread" if tread else "rubber"]["color"], 132, grain=4))
    if tread:
        for y in range(-32, 161, 32):
            p.line((0, y + 26, 61, y + 3, 64, y + 3, 127, y + 26), "#24292a", 4)
            p.line((0, y + 22, 61, y - 1, 64, y - 1, 127, y + 22), "#393e3e", 1.6)
    else:
        for y in (27, 30, 96, 99):
            p.line((0, y, 127, y), "#2a2f30", 0.65)
    return p.finish()


def paint_seat():
    p = Painter(surface(MATERIALS["seat"]["color"], 136, grain=5))
    for x in (18, 109):
        p.line((x, 0, x, 127), "#1d2528", 1.7)
        for y in range(2, 128, 6):
            p.line((x + 2, y, x + 2, y + 2.3), "#5a6260", 0.65)
    for y in (36, 61, 86):
        p.line((23, y, 104, y), "#1c2527", 1.3)
        p.line((25, y + 1.3, 102, y + 1.3), "#343d3f", 0.7)
    return p.finish()


def paint_grille():
    p = Painter(surface("#29353a", 138, grain=3, brushed=1.5))
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
    p = Painter(surface("#e57525", 155, grain=2.8))
    p.rect((3, 14, 124, 113), "#1d2a2f", "#ef9b50", 1.5, radius=7)
    p.line((11, 23, 116, 23), "#5f6663", 0.65)
    p.text((64, 45), "SNC", 32, "#f5eee0", True)
    p.rect((16, 66, 112, 68), "#e57525")
    p.text((64, 89), "75", 32, "#f1a052", True)
    for x in (10, 117):
        for y in (21, 106):
            screw(p, x, y, 1.6)
    return p.finish()


def radial(cx, cy, radius, degrees):
    radians = math.radians(degrees)
    return cx + math.cos(radians) * radius, cy + math.sin(radians) * radius


def paint_gauge():
    p = Painter(surface("#253237", 160, grain=2.0))
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
    for index, value in enumerate(("0", "5", "10", "15", "20", "25", "30")):
        p.text(radial(64, 59, 23, 136 + index * 34.8), value, 5.5, "#dae5d4")
    p.text((64, 50), "RPM", 6, "#92acaa", True)
    p.text((64, 75), "x100", 5, "#92acaa")
    p.line((*radial(64, 59, 8, 39), *radial(64, 59, 28, 219)), "#e77a3b", 1.6)
    p.ellipse((60.7, 55.7, 67.3, 62.3), "#a5b2ad", "#273e43", 0.5)
    p.rect((45, 83, 83, 91), "#08181b", "#526a64", 0.6, radius=1)
    p.text((64, 87), "0075.0", 5.5, "#b4c9a6")
    for x, fill, label in ((37, "#c07733", "OIL"), (64, "#7baa72", "PWR"), (91, "#517682", "TEMP")):
        p.ellipse((x - 3.1, 106, x + 3.1, 112.2), fill, "#111f24", 0.8)
        p.text((x, 118), label, 4.3, "#aebeb8")
    return p.finish()


def main():
    TEXTURE_DIR.mkdir(parents=True, exist_ok=True)
    images = {
        "enamel_orange": surface(MATERIALS["enamel_orange"]["color"], 101, grain=3.2),
        "enamel_dark": surface(MATERIALS["enamel_dark"]["color"], 105, grain=2.8),
        "rubber": paint_rubber(),
        "tread": paint_rubber(True),
        "steel": surface(MATERIALS["steel"]["color"], 119, grain=3.2, brushed=4),
        "rim": surface(MATERIALS["rim"]["color"], 121, grain=2.6, brushed=1),
        "seat": paint_seat(),
        "grille": paint_grille(),
        "headlight": paint_lens("headlight", 144),
        "taillight": paint_lens("taillight", 146),
        "amber": paint_lens("amber", 147),
        "seed_tank": surface(MATERIALS["seed_tank"]["color"], 150, grain=3.3),
        "decal_snc": paint_decal(),
        "gauge": paint_gauge(),
    }
    manifest = {}
    for name, image in images.items():
        assert image.size == (128, 128), f"Invalid texture dimensions: {name} {image.size}"
        image.save(TEXTURE_DIR / f"{name}.png", optimize=True)
        manifest[name] = {"file": f"textures/{name}.png", **MATERIALS[name]}
    (ASSET_DIR / "materials.json").write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    print(f"Generated {len(images)} tractor material textures, all 128x128, in {TEXTURE_DIR}")


if __name__ == "__main__":
    main()
