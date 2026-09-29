"""Generate 128x128 item icons for the SNC 75-P planter and SNC 90 harvester.

Reuses the shared canvas helpers from tools/generate_textures.py (imported as
a library, mirroring generate_planter_textures.py's reuse of the golden
tractor generator). The icons show the implements' side silhouettes in the
industrial SNC palette.
"""
from __future__ import annotations

import importlib.util
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("gt", ROOT / "tools/generate_textures.py")
gt = importlib.util.module_from_spec(spec)
spec.loader.exec_module(gt)


def tex_planter_item():
    """SNC 75-P side view: three canvas tanks on a dark toolbar, transport wheel."""
    cv = gt.Canvas(seed=890)
    cv.brushed(gt.STEEL_LIGHT, seed=891)
    cv.contact_shadow(4, 0.5)
    # Transport wheel on the right.
    gt.wheel(cv, 94, 88, 24)
    # Hitch arm reaching to the left.
    gt.line(cv, 6, 96, 44, 78, gt.STEEL_DARK, 9)
    gt.line(cv, 6, 96, 44, 78, (110, 118, 128), 3)
    # Toolbar across the middle.
    cv.rect(20, 70, 108, 82, gt.ORANGE)
    cv.rect(20, 70, 108, 74, (255, 176, 74))
    # Three seed tanks with lids.
    for i, x in enumerate((24, 56, 88)):
        cv.rect(x, 30, x + 26, 70, (222, 219, 194))
        cv.rect(x, 30, x + 26, 34, (208, 204, 176))
        cv.rect(x - 1, 26, x + 27, 31, gt.ORANGE)
        cv.rect(x + 4, 40, x + 22, 62, (200, 195, 164))
        gt.line(cv, x + 4, 44 + i * 3, x + 22, 44 + i * 3, (172, 166, 132), 2)
    # Row shanks dropping from the toolbar.
    for x in (34, 66, 98):
        cv.rect(x, 82, x + 5, 100, gt.STEEL_DARK)
        cv.rect(x - 4, 98, x + 9, 104, (70, 76, 84))
    save = gt.save
    save(cv, "planter", "item")


def tex_harvester_item():
    """SNC 90 side view: header left, glass cab, grain tank, big rear wheel."""
    cv = gt.Canvas(seed=900)
    cv.brushed(gt.STEEL_LIGHT, seed=901)
    cv.contact_shadow(4, 0.6)
    # Rear drive wheel and small front wheel.
    gt.wheel(cv, 92, 88, 28)
    gt.wheel(cv, 26, 98, 15)
    # Chassis belly.
    cv.rect(14, 66, 110, 82, (44, 40, 36))
    cv.rect(14, 66, 110, 70, (58, 52, 46))
    # Grain tank (rear top) with orange lid.
    cv.rect(62, 28, 112, 66, gt.ORANGE)
    cv.rect(62, 28, 112, 33, (255, 176, 74))
    cv.rect(68, 38, 106, 60, (222, 118, 18))
    # Glass cab over the front of the tank.
    cv.rect(38, 30, 62, 66, (40, 46, 52))
    cv.rect(41, 34, 59, 60, (150, 190, 200))
    cv.rect(41, 34, 59, 38, (190, 220, 226))
    # Header with reel points at the left.
    cv.rect(2, 70, 40, 84, gt.STEEL_DARK)
    cv.rect(2, 82, 44, 88, (120, 128, 138))
    for i in range(4):
        x = 6 + i * 10
        cv.rect(x, 64, x + 4, 72, gt.ORANGE)
        cv.rect(x - 2, 60, x + 6, 65, (168, 178, 188))
    # Unloading auger tube across the tank top.
    gt.line(cv, 50, 24, 118, 14, gt.STEEL_DARK, 7)
    gt.line(cv, 50, 24, 118, 14, (150, 158, 168), 3)
    cv.rect(112, 8, 120, 22, gt.STEEL_DARK)
    save = gt.save
    save(cv, "harvester", "item")


def main():
    gt.written.clear()
    tex_planter_item()
    tex_harvester_item()
    print("Generated item icons:", [(f, n) for f, n in gt.written])


if __name__ == "__main__":
    main()
