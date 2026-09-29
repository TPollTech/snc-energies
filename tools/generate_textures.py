"""High-quality procedural textures for SNC Energies (AGENTS.md 128x128 rule).

Same file set, palette and GUI slot coordinates as the old System.Drawing
scripts, but drawn with layered value noise, anisotropic brushed metal,
bevel lighting, rivets and soft emissive glows instead of flat rectangles.

Run:  ./.venv-textures/Scripts/python tools/generate_textures.py
"""
from __future__ import annotations

import math
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from textures_engine import Canvas, fbm, rgb, value_noise, value_noise_aniso  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'snc_energies')
TX = {name: os.path.join(ASSETS, 'textures', name) for name in ('block', 'item', 'gui')}

# ------------------------------------------------------------------ palette
STEEL = (58, 62, 70)
STEEL_LIGHT = (82, 88, 98)
STEEL_HI = (118, 124, 134)
STEEL_DARK = (40, 44, 50)
FRAME = (24, 26, 30)
ORANGE = (255, 140, 26)
ORANGE_HOT = (255, 208, 92)
EMBER = (214, 72, 18)
COPPER = (198, 110, 58)
COPPER_LIGHT = (240, 172, 104)
CYAN = (64, 220, 255)
CYAN_GLOW = (168, 246, 255)
CYAN_CELL = (16, 56, 76)
YELLOW = (255, 214, 64)
YELLOW_CORE = (255, 248, 196)
VIO_CORE = (186, 86, 255)
VIO_MID = (140, 54, 214)
VIO_DARK = (84, 26, 138)
VIO_GLOW = (236, 190, 255)
STONE_BASE = (126, 126, 126)
DEEP_BASE = (78, 80, 88)

written = []


def save(cv: Canvas, name: str, folder: str):
    path = os.path.join(TX[folder], name + '.png')
    cv.save(path)
    written.append((folder, name))


# ------------------------------------------------------- shared machine faces
def machine_side_base(seed: int) -> Canvas:
    cv = Canvas(seed=seed)
    cv.brushed(STEEL, seed=seed + 1)
    cv.aniso_roughness(7.0, seed=seed + 2)
    cv.grain(5.0, seed=seed + 3)
    cv.vignette(0.10)
    cv.frame(5, FRAME)
    return cv


def machine_side(seed: int = 11) -> Canvas:
    cv = machine_side_base(seed)
    cv.bevel(inset=5, depth=8, top=14.0, bottom=-10.0, left=12.0, right=-12.0)
    # recessed service strip along the lower third
    cv.rect(12, 62, 116, 65, (20, 22, 26))
    cv.rect(12, 65, 116, 67, (72, 78, 88))
    for x in (16, 112):
        cv.rivet(x, 16, 6, seed=seed)
        cv.rivet(x, 112, 6, seed=seed + 40)
    return cv


def machine_top(seed: int = 21) -> Canvas:
    cv = machine_side_base(seed)
    cv.bevel(inset=5, depth=8, top=12.0, bottom=-12.0, left=12.0, right=-12.0)
    # vent shaft with lit slats
    cv.rect(24, 24, 104, 104, (18, 20, 24))
    for i, y in enumerate(range(28, 98, 9)):
        cv.rect(28, y, 100, y + 4, (56 + (i % 2) * 6, 62 + (i % 2) * 6, 72 + (i % 2) * 6))
        cv.rect(28, y + 4, 100, y + 5, (22, 24, 28))
    for x, y in ((12, 12), (116, 12), (12, 116), (116, 116)):
        cv.rivet(x, y, 5, seed=seed)
    return cv


def machine_bottom(seed: int = 31) -> Canvas:
    cv = machine_side_base(seed)
    cv.bevel(inset=5, depth=8, top=8.0, bottom=-14.0, left=10.0, right=-10.0)
    cv.rect(44, 44, 84, 84, (32, 35, 41))
    n = fbm(40, seed + 9, base_cells=5, octaves=4)
    patch = cv.a[44:84, 44:84, :3]
    cv.a[44:84, 44:84, :3] = np.clip(patch * (0.8 + 0.4 * n[..., None]), 0, 255)
    for x in (16, 112):
        cv.rivet(x, 16, 6, seed=seed)
        cv.rivet(x, 112, 6, seed=seed + 40)
    return cv


def hazard_band(cv: Canvas, y0: int = 8, height: int = 26, seed: int = 2):
    """Diagonal hazard stripes with grime, spanning the full width."""
    s = cv.size
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    diag = ((xx + (s - yy)) / 20.0).astype(int) % 2 == 0
    band = (yy >= y0) & (yy < y0 + height)
    yellow = np.array([224, 178, 40], np.float32)
    dark = np.array([30, 30, 34], np.float32)
    col = np.where(diag[..., None], yellow, dark)
    grime = fbm(s, seed, base_cells=8, octaves=4)[..., None]
    col = np.clip(col * (0.82 + 0.36 * grime), 0, 255)
    alpha = band * 255
    layer = np.zeros((s, s, 4), np.float32)
    layer[..., :3] = col
    layer[..., 3] = alpha
    cv.blit(layer)
    cv.rect(0, y0, s, y0 + 2, FRAME)
    cv.rect(0, y0 + height - 2, s, y0 + height, FRAME)


def vent_grille(cv: Canvas, x0: int, y0: int, x1: int, y1: int, seed: int = 4):
    cv.rect(x0, y0, x1, y1, (20, 22, 26))
    w = x1 - x0
    for y in range(y0 + 4, y1 - 3, 8):
        tone = 58 + ((y // 8) % 2) * 8 + int(fbm(w, seed + y, base_cells=6, octaves=3)[3, 3] * 18)
        cv.rect(x0 + 4, y, x1 - 4, y + 3, (tone, tone + 5, tone + 14))
        cv.rect(x0 + 4, y + 3, x1 - 4, y + 4, (12, 13, 16))


def window_glass(cv: Canvas, x0, y0, x1, y1, seed: int = 9):
    """Dark glass with faint reflections."""
    w, h = x1 - x0, y1 - y0
    glass = np.zeros((h, w, 4), np.float32)
    base = np.zeros((h, w, 3), np.float32)
    base[..., :] = np.array([24, 26, 32], np.float32)
    streaks = value_noise_aniso(128, max(3, w // 24), 3, seed)[:h, :w]
    base += (streaks[..., None] * np.array([26, 30, 40], np.float32))
    glass[..., :3] = np.clip(base, 0, 255)
    glass[..., 3] = 255
    cv.blit(glass, at=(x0, y0))
    # frame
    for t in range(3):
        shade = (90 - t * 22, 96 - t * 22, 106 - t * 22)
        cv.rect(x0 - 3 + t, y0 - 3 + t, x1 + 3 - t, y0 - 2 + t, shade)
        cv.rect(x0 - 3 + t, y1 + 2 - t, x1 + 3 - t, y1 + 3 - t, shade)
        cv.rect(x0 - 3 + t, y0 - 3 + t, x0 - 2 + t, y1 + 3 - t, shade)
        cv.rect(x1 + 2 - t, y0 - 3 + t, x1 + 3 - t, y1 + 3 - t, shade)


def lightning(cv: Canvas, x: int, y: int, h: int, color=YELLOW, core=YELLOW_CORE):
    w = int(h * 0.62)
    pts = [
        (x + int(w * 0.55), y),
        (x + int(w * 0.10), y + int(h * 0.48)),
        (x + int(w * 0.42), y + int(h * 0.48)),
        (x + int(w * 0.22), y + h),
        (x + int(w * 0.90), y + int(h * 0.42)),
        (x + int(w * 0.55), y + int(h * 0.42)),
    ]
    s = cv.size
    mask = _fill_polygon(np.zeros((s, s), bool), pts)
    col = np.zeros((s, s, 4), np.float32)
    col[..., 0], col[..., 1], col[..., 2] = color
    col[..., 3] = mask.astype(np.float32) * 255
    cv.blit(col)
    # bright core stripe
    cx = x + int(w * 0.5)
    core_pts = [(cx - 2, y + int(h * 0.12)), (cx + 2, y + int(h * 0.40)),
                (cx - 1, y + int(h * 0.75)), (cx + 2, y + int(h * 0.40)), (cx - 2, y + int(h * 0.40))]
    cmask = _fill_polygon(np.zeros((s, s), bool), core_pts)
    colc = np.zeros((s, s, 4), np.float32)
    colc[..., 0], colc[..., 1], colc[..., 2] = core
    colc[..., 3] = cmask * 255
    cv.blit(colc)


def _fill_polygon(mask: np.ndarray, pts) -> np.ndarray:
    """Scanline polygon fill into a boolean mask."""
    h, w = mask.shape
    ys = [p[1] for p in pts]
    xs = [p[0] for p in pts]
    min_y, max_y = max(0, int(min(ys))), min(h - 1, int(max(ys)) + 1)
    n = len(pts)
    for y in range(min_y, max_y + 1):
        nodes = []
        j = n - 1
        for i in range(n):
            yi, xi = pts[i][1], pts[i][0]
            yj, xj = pts[j][1], pts[j][0]
            if (yi <= y < yj) or (yj <= y < yi):
                nodes.append(xi + (y - yi) / (yj - yi) * (xj - xi))
            j = i
        nodes.sort()
        for k in range(0, len(nodes) - 1, 2):
            x0 = max(0, int(round(nodes[k])))
            x1 = min(w - 1, int(round(nodes[k + 1])))
            mask[y, x0:x1 + 1] = True
    return mask


def gauge(cv: Canvas, cx: int, cy: int, radius: int, needle_deg: float = -35.0):
    s = cv.size
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    # housing
    cv.glow(cx, cy, radius * 1.5, (90, 96, 104), peak=0.22)
    rim = np.clip(1.0 - np.abs(d - radius * 0.92) / 2.0, 0, 1)
    face = (d <= radius * 0.9)
    col = np.zeros((s, s, 4), np.float32)
    face_col = np.array([200, 198, 176], np.float32)
    dirt = fbm(s, 77, base_cells=6, octaves=4)
    face_col = np.clip(face_col * (0.85 + 0.3 * dirt[..., None]), 0, 255)
    for c in range(3):
        col[..., c] = np.where(face, face_col[..., c], col[..., c])
    col[..., 3] = face * 255
    cv.blit(col)
    # rim shading
    rim_dark = np.clip(1.0 - np.abs(d - radius * 0.88) / (radius * 0.12), 0, 1)
    col = np.zeros((s, s, 4), np.float32)
    col[..., 0] = col[..., 1] = col[..., 2] = 40
    col[..., 3] = rim_dark * 230
    cv.blit(col)
    # ticks
    for i in range(10):
        ang = math.radians(i * 28 + 140)
        x0 = cx + 29 * math.cos(ang); y0 = cy + 29 * math.sin(ang)
        x1 = cx + 37 * math.cos(ang); y1 = cy + 37 * math.sin(ang)
        line(cv, x0, y0, x1, y1, (67, 74, 64), 3)
    # needle with shadow
    ang = math.radians(needle_deg)
    line(cv, cx + 2, cy + 2, cx + 20 * math.cos(ang), cy + 20 * math.sin(ang), (30, 28, 24), 4, alpha=0.5)
    line(cv, cx, cy, cx + 19 * math.cos(ang), cy + 19 * math.sin(ang), (166, 77, 49), 4)
    cv.rivet(cx, cy, 5, seed=13)


def line(cv: Canvas, x0, y0, x1, y1, color, width: int = 2, alpha: float = 1.0):
    steps = int(max(abs(x1 - x0), abs(y1 - y0))) + 1
    for i in range(steps + 1):
        t = i / max(1, steps)
        px, py = x0 + (x1 - x0) * t, y0 + (y1 - y0) * t
        half = max(0.5, width / 2)
        cv.glow(px, py, half * 1.4, color, peak=alpha * 0.9, falloff=1.2)


def bolts4(cv: Canvas, xs=(16, 112), ys=(16, 112), r=6, seed=5):
    for x in xs:
        for y in ys:
            cv.rivet(x, y, r, seed=seed)


# ------------------------------------------------------------- machine faces
def tex_machine_side():
    save(machine_side(), 'machine_side', 'block')


def tex_machine_top():
    save(machine_top(), 'machine_top', 'block')


def tex_machine_bottom():
    save(machine_bottom(), 'machine_bottom', 'block')


def tex_generator():
    off = machine_side(41)
    vent_grille(off, 20, 20, 108, 72, seed=44)
    off.glow(64, 96, 26, EMBER, peak=0.55)
    off.glow(64, 96, 14, ORANGE, peak=0.5)
    off.rect(44, 86, 84, 112, (26, 18, 12))
    off.glow(64, 99, 12, ORANGE_HOT, peak=0.4)
    bolts4(off)
    save(off, 'coal_generator_front_off', 'block')

    on = machine_side(41)
    vent_grille(on, 20, 20, 108, 72, seed=44)
    lightning(on, 40, 24, 46)
    on.glow(64, 96, 30, EMBER, peak=0.75)
    on.glow(64, 96, 18, ORANGE, peak=0.7)
    on.rect(44, 86, 84, 112, (34, 22, 12))
    on.glow(64, 99, 16, ORANGE_HOT, peak=0.55)
    bolts4(on)
    save(on, 'coal_generator_front_on', 'block')

    side = machine_side(41)
    lightning(side, 42, 26, 66)
    side.glow(58, 60, 20, (180, 150, 40), peak=0.25)
    save(side, 'coal_generator_side', 'block')


def tex_furnace():
    side = machine_side(51)
    # copper power port
    side.rect(96, 48, 116, 80, (26, 28, 32))
    side.rect(100, 54, 112, 74, COPPER)
    side.rect(100, 54, 112, 58, COPPER_LIGHT)
    side.rect(100, 74, 112, 78, (120, 62, 30))
    save(side, 'electric_furnace_side', 'block')

    for lit, name in ((False, 'electric_furnace_front_off'), (True, 'electric_furnace_front_on')):
        cv = machine_side(51)
        window_glass(cv, 30, 28, 98, 78, seed=59)
        if lit:
            cv.glow(64, 58, 24, EMBER, peak=0.85)
            cv.glow(64, 62, 16, ORANGE, peak=0.8)
            cv.glow(64, 66, 9, ORANGE_HOT, peak=0.9)
            sparks = fbm(128, 61, base_cells=16, octaves=3)
            sp = np.clip((sparks - 0.72) * 6, 0, 1)
            layer = np.zeros((128, 128, 4), np.float32)
            layer[..., 0], layer[..., 1], layer[..., 2] = ORANGE_HOT
            layer[..., 3] = sp * 200
            cv.blit(layer)
        else:
            for i in range(5):
                cv.rect(38, 36 + i * 8, 90, 37 + i * 8, (70, 76, 86))
        cv.rect(46, 88, 82, 112, (24, 26, 30))
        cv.rect(50, 92, 78, 108, (40, 44, 52))
        cv.bevel(inset=8, depth=4, top=6.0, bottom=-6.0, left=6.0, right=-6.0)
        bolts4(cv)
        save(cv, name, 'block')


def tex_crusher():
    side = Canvas(seed=71)
    side.brushed(STEEL, seed=72)
    side.aniso_roughness(7, seed=73)
    side.frame(5, FRAME)
    hazard_band(side, 8, 26, seed=74)
    bolts4(side, ys=(44, 108))
    save(side, 'crusher_side', 'block')

    for lit, name in ((False, 'crusher_front_off'), (True, 'crusher_front_on')):
        cv = machine_side(71)
        cv.rect(24, 20, 104, 80, (20, 22, 26))
        depth_grad = np.linspace(0.55, 1.0, 60, dtype=np.float32)
        chamber = cv.a[20:80, 24:104, :3]
        cv.a[20:80, 24:104, :3] = np.clip(chamber * depth_grad[:, None, None], 0, 255)
        for i in range(5):
            xx = 26 + i * 16
            top_tri = [(xx, 24), (xx + 14, 24), (xx + 7, 52)]
            bot_tri = [(xx, 78), (xx + 14, 78), (xx + 7, 50)]
            col = STEEL_HI if not lit else (255, 170, 70)
            for tri in (top_tri, bot_tri):
                mask = _fill_polygon(np.zeros((128, 128), bool), tri)
                layer = np.zeros((128, 128, 4), np.float32)
                layer[..., 0], layer[..., 1], layer[..., 2] = col
                layer[..., 3] = mask * 255
                cv.blit(layer)
        if lit:
            cv.glow(64, 52, 26, (200, 110, 30), peak=0.7)
            rubble = fbm(128, 79, base_cells=20, octaves=3)
            sp = np.clip((rubble - 0.62) * 5, 0, 1) * 0.5
            layer = np.zeros((128, 128, 4), np.float32)
            layer[..., 0], layer[..., 1], layer[..., 2] = 120, 110, 100
            layer[..., 3] = sp * 255
            cv.blit(layer)
        cv.rect(46, 92, 82, 114, (24, 26, 30))
        cv.rect(50, 96, 78, 110, (40, 44, 52))
        bolts4(cv, xs=(12, 116), ys=(12, 116))
        save(cv, name, 'block')

    top = machine_top(81)
    top.rect(34, 34, 94, 94, (18, 20, 24))
    top.rect(40, 40, 88, 88, (30, 32, 38))
    top.rect(34, 34, 94, 38, (14, 15, 18))
    save(top, 'crusher_top', 'block')


def tex_energy_cube():
    cv = Canvas(seed=91)
    cv.brushed(STEEL, seed=92)
    cv.aniso_roughness(6, seed=93)
    cv.frame(5, FRAME)
    cv.bevel(inset=5, depth=8, top=12, bottom=-10, left=12, right=-12)
    cv.rect(18, 18, 110, 110, (26, 30, 36))
    cells = ((24, 24), (68, 24), (24, 68), (68, 68))
    for ci, (cx, cy) in enumerate(cells):
        cv.rect(cx, cy, cx + 36, cy + 36, CYAN_CELL)
        cv.glow(cx + 18, cy + 18, 15, CYAN, peak=0.75)
        cv.rect(cx + 4, cy + 4, cx + 32, cy + 32, (10, 30, 40))
        cv.glow(cx + 18, cy + 18, 10, CYAN, peak=0.85)
        # hot core with iridescent rim
        core = cv.a[cy + 12:cy + 24, cx + 12:cx + 24, :3]
        cv.a[cy + 12:cy + 24, cx + 12:cx + 24, :3] = np.clip(core * 0.2 + np.array((220, 250, 255), np.float32) * 0.8, 0, 255)
        # cell frame
        for t, sh in ((0, (140, 210, 230)), (1, (30, 60, 80))):
            cv.rect(cx + t, cy + t, cx + 36 - t, cy + t + 1, sh)
            cv.rect(cx + t, cy + 35 - t, cx + 36 - t, cy + 36 - t, sh)
            cv.rect(cx + t, cy + t, cx + t + 1, cy + 36 - t, sh)
            cv.rect(cx + 35 - t, cy + t, cx + 36 - t, cy + 36 - t, sh)
    bolts4(cv, xs=(10, 118), ys=(10, 118), r=4)
    save(cv, 'energy_cube', 'block')

    top = Canvas(seed=95)
    top.brushed(STEEL, seed=96)
    top.frame(5, FRAME)
    top.bevel(inset=5, depth=8, top=12, bottom=-12, left=12, right=-12)
    top.rect(30, 30, 98, 98, (26, 30, 36))
    top.glow(64, 64, 26, CYAN, peak=0.8)
    top.rect(52, 52, 76, 76, (220, 250, 255))
    bolts4(top, xs=(10, 118), ys=(10, 118), r=4)
    save(top, 'energy_cube_top', 'block')


def tex_cables():
    core = Canvas(seed=101)
    core.fill((36, 39, 45))
    n = fbm(128, 102, base_cells=10, octaves=4)
    core.a[..., :3] = np.clip(core.a[..., :3] * (0.9 + 0.25 * n[..., None]), 0, 255)
    core.glow(64, 64, 34, COPPER, peak=0.6)
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    d = np.sqrt((xx - 64) ** 2 + (yy - 64) ** 2)
    core_a = np.clip(1 - np.abs(d - 26) / 10, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = COPPER
    layer[..., 3] = core_a * 255
    core.blit(layer)
    inner = np.clip(1 - np.abs(d - 14) / 8, 0, 1)
    layer2 = np.zeros((128, 128, 4), np.float32)
    layer2[..., 0], layer2[..., 1], layer2[..., 2] = COPPER_LIGHT
    layer2[..., 3] = inner * 255
    core.blit(layer2)
    save(core, 'cable_core', 'block')

    arm = Canvas(seed=105)
    arm.fill((36, 39, 45))
    na = fbm(128, 106, base_cells=10, octaves=4)
    arm.a[..., :3] = np.clip(arm.a[..., :3] * (0.9 + 0.25 * na[..., None]), 0, 255)
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    d = np.sqrt((xx - 64) ** 2 + (yy - 64) ** 2)
    ring = np.clip(1 - np.abs(d - 32) / 8, 0, 1)
    ring2 = np.clip(1 - np.abs(d - 20) / 7, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = COPPER
    layer[..., 3] = ring * 255
    arm.blit(layer)
    layer2 = np.zeros((128, 128, 4), np.float32)
    layer2[..., 0], layer2[..., 1], layer2[..., 2] = COPPER_LIGHT
    layer2[..., 3] = ring2 * 255
    arm.blit(layer2)
    save(arm, 'cable_arm', 'block')

    # Item ducts: same silhouette, galvanized panel shell with an amber viewing stripe.
    GALV = (150, 158, 162)
    GALV_LIGHT = (196, 203, 206)
    AMBER = (214, 146, 50)
    duct = Canvas(seed=109)
    duct.fill((44, 48, 54))
    nd = fbm(128, 110, base_cells=10, octaves=4)
    duct.a[..., :3] = np.clip(duct.a[..., :3] * (0.9 + 0.25 * nd[..., None]), 0, 255)
    duct.aniso_roughness(8, seed=111)
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    d = np.sqrt((xx - 64) ** 2 + (yy - 64) ** 2)
    shell = np.clip(1 - np.abs(d - 26) / 10, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = GALV
    layer[..., 3] = shell * 255
    duct.blit(layer)
    stripe = np.clip(1 - np.abs(d - 26) / 4, 0, 1) * (np.abs(yy - 64) < 34)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = AMBER
    layer[..., 3] = stripe * 210
    duct.blit(layer)
    highlight = np.clip(1 - np.abs(d - 14) / 8, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = GALV_LIGHT
    layer[..., 3] = highlight * 255
    duct.blit(layer)
    save(duct, 'item_pipe_core', 'block')

    duct_arm = Canvas(seed=113)
    duct_arm.fill((44, 48, 54))
    duct_arm.a[..., :3] = np.clip(duct_arm.a[..., :3] * (0.9 + 0.25 * nd[..., None]), 0, 255)
    ring = np.clip(1 - np.abs(d - 32) / 8, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = GALV
    layer[..., 3] = ring * 255
    duct_arm.blit(layer)
    ring2 = np.clip(1 - np.abs(d - 20) / 7, 0, 1) * (np.abs(yy - 64) >= 34)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = AMBER
    layer[..., 3] = ring2 * 210
    duct_arm.blit(layer)
    inner = np.clip(1 - np.abs(d - 14) / 8, 0, 1)
    layer = np.zeros((128, 128, 4), np.float32)
    layer[..., 0], layer[..., 1], layer[..., 2] = GALV_LIGHT
    layer[..., 3] = inner * 255
    duct_arm.blit(layer)
    save(duct_arm, 'item_pipe_arm', 'block')


# --------------------------------------------------------------------- ores
def ore(base, name, seed=120, cluster_color=(VIO_DARK, VIO_MID, VIO_CORE, VIO_GLOW)):
    cv = Canvas(seed=seed)
    cv.fill(base)
    # rocky mottle: large stains + fine grain + cracks
    stains = fbm(128, seed + 1, base_cells=5, octaves=5)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.78 + 0.44 * stains[..., None]), 0, 255)
    cv.grain(11, cells=110, seed=seed + 2)
    h = fbm(128, seed + 3, base_cells=8, octaves=4)
    cv.shade(h, strength=2.4)
    # cracks
    for c in range(3):
        x = 10 + (seed * 7 + c * 41) % 100
        y = 8 + (seed * 13 + c * 29) % 100
        for _ in range(26):
            x += (seed + c * 9 + _ * 7) % 5 - 2
            y += 1 + (seed + c * 3 + _ * 5) % 3 - 1
            x = max(2, min(125, x)); y = max(2, min(125, y))
            cv.glow(x, y, 1.6, (max(0, base[0] - 55), max(0, base[1] - 55), max(0, base[2] - 55)), peak=0.8, falloff=1.0)
    # crystal clusters
    clusters = ((30, 34), (84, 28), (56, 72), (100, 88), (24, 96), (70, 110))
    dark, mid, core, glowc = cluster_color
    for cx, cy in clusters:
        cv.glow(cx + 8, cy + 8, 13, dark, peak=0.8)
        for spk in range(4):
            sx = cx + (seed * 3 + spk * 11) % 12
            sy = cy + (seed * 5 + spk * 7) % 12
            sh = 8 + (seed * 7 + spk * 13) % 10
            tri = [(sx + 3, sy), (sx + 7, sy + sh), (sx + 3, sy + int(sh * 0.7)), (sx, sy + sh)]
            mask = _fill_polygon(np.zeros((128, 128), bool), tri)
            layer = np.zeros((128, 128, 4), np.float32)
            layer[..., 0], layer[..., 1], layer[..., 2] = mid
            layer[..., 3] = mask * 255
            cv.blit(layer)
            core_tri = [(sx + 3, sy + 1), (sx + 5, sy + sh), (sx + 3, sy + int(sh * 0.6))]
            cmask = _fill_polygon(np.zeros((128, 128), bool), core_tri)
            layer2 = np.zeros((128, 128, 4), np.float32)
            layer2[..., 0], layer2[..., 1], layer2[..., 2] = core
            layer2[..., 3] = cmask * 255
            cv.blit(layer2)
            cv.glow(sx + 3, sy + 1, 2.4, glowc, peak=0.9)
    save(cv, name, 'block')


def tex_ores():
    ore(STONE_BASE, 'voltaite_ore', seed=131)
    ore(DEEP_BASE, 'deepslate_voltaite_ore', seed=137)
    ore((124, 128, 124), 'tin_ore', seed=143, cluster_color=((59, 76, 77), (168, 195, 188), (225, 232, 215), (255, 255, 240)))
    ore((65, 75, 79), 'deepslate_tin_ore', seed=149, cluster_color=((40, 54, 55), (150, 178, 170), (214, 224, 206), (255, 255, 244)))


# -------------------------------------------------------------------- items
def ingot(name, main, light, dark, seed=150):
    cv = Canvas(seed=seed, transparent=True)
    poly = [(8, 40), (56, 16), (120, 40), (120, 62), (72, 90), (8, 62)]
    mask = _fill_polygon(np.zeros((128, 128), bool), poly)
    top = _fill_polygon(np.zeros((128, 128), bool), [(8, 40), (56, 16), (120, 40), (72, 64)])
    yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
    noise = fbm(128, seed + 1, base_cells=12, octaves=4)
    layer = np.zeros((128, 128, 4), np.float32)
    base_col = np.array(dark, np.float32) * (0.85 + 0.3 * noise[..., None])
    layer[..., :3] = np.clip(base_col, 0, 255)
    layer[..., 3] = mask * 255
    cv.blit(layer)
    layer2 = np.zeros((128, 128, 4), np.float32)
    layer2[..., :3] = np.clip(np.array(main, np.float32) * (0.88 + 0.24 * noise[..., None]), 0, 255)
    layer2[..., 3] = top * 255
    cv.blit(layer2)
    # specular streak on the top facet
    streak = _fill_polygon(np.zeros((128, 128), bool), [(20, 40), (56, 22), (92, 40), (56, 56)])
    layer3 = np.zeros((128, 128, 4), np.float32)
    layer3[..., :3] = light
    layer3[..., 3] = streak * 190
    cv.blit(layer3)
    # stamped mark
    cv.glow(64, 46, 6, light, peak=0.35)
    save(cv, name, 'item')


def plate(name, main, light, seed=160, decor=None):
    cv = Canvas(seed=seed, transparent=True)
    cv.rect(20, 24, 108, 104, tuple(max(0, c - 40) for c in main))
    body = np.zeros((80, 88, 4), np.float32)
    noise = fbm(128, seed + 1, base_cells=6, octaves=4)[:80, :88]
    body[..., :3] = np.clip(np.array(main, np.float32) * (0.82 + 0.36 * noise[..., None]), 0, 255)
    body[..., 3] = 255
    sub = Canvas(); sub.a = body
    _blit_region(cv, sub, 20, 24)
    # bevel edges
    cv.rect(20, 20, 108, 24, light)
    cv.rect(20, 100, 108, 104, tuple(max(0, c - 60) for c in main))
    cv.rect(20, 20, 24, 104, tuple(min(255, c + 20) for c in main))
    cv.rect(104, 20, 108, 104, tuple(max(0, c - 70) for c in main))
    # corner bolts
    for bx, by in ((28, 32), (100, 32), (28, 96), (100, 96)):
        cv.rivet(bx, by, 4, seed=seed)
    if decor:
        decor(cv)
    save(cv, name, 'item')


def _blit_region(cv: Canvas, sub: Canvas, x0: int, y0: int):
    h, w = sub.a.shape[:2]
    cv.a[y0:y0 + h, x0:x0 + w] = sub.a


def circuit(name, board, seed=170):
    cv = Canvas(seed=seed, transparent=True)
    cv.rect(20, 20, 108, 108, (32, 42, 40))
    body = np.zeros((88, 88, 4), np.float32)
    noise = fbm(88, seed + 1, base_cells=8, octaves=3)
    body[..., :3] = np.clip(np.array(board, np.float32) * (0.85 + 0.3 * noise[..., None]), 0, 255)
    body[..., 3] = 255
    sub = Canvas(); sub.a = body
    _blit_region(cv, sub, 20, 20)
    # gold traces
    for y in (36, 58, 81):
        line(cv, 26, y, 99, y, (223, 198, 120), 4)
    # chip
    cv.rect(46, 43, 80, 79, (45, 53, 53))
    chip_noise = fbm(64, seed + 2, base_cells=6, octaves=3)[:32, :26]
    region = cv.a[46:78, 50:76, :3]
    cv.a[46:78, 50:76, :3] = np.clip(region * (0.8 + 0.4 * chip_noise[..., None]), 0, 255)
    for y in range(46, 76, 7):
        line(cv, 42, y, 82, y, (177, 185, 175), 2, alpha=0.8)
    cv.rect(52, 48, 74, 74, (68, 68, 75))
    cv.glow(63, 61, 7, (255, 230, 150), peak=0.35)
    save(cv, name, 'item')


def gear(name, seed=180):
    cv = Canvas(seed=seed, transparent=True)
    s = 128
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    cx = cy = 64.0
    d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2)
    ang = np.arctan2(yy - cy, xx - cx)
    # teeth: 8 square teeth on a ring
    tooth = (np.abs(((ang + math.pi) / (math.pi / 4)) % 1 - 0.5) < 0.18) & (d > 38) & (d < 50)
    ring = (d >= 26) & (d <= 40)
    metal = np.clip(np.array((158, 175, 176), np.float32) * (0.8 + 0.35 * fbm(s, seed + 1, base_cells=10, octaves=3)[..., None]), 0, 255)
    # shading: bright top-left
    shade = np.clip(0.75 + 0.5 * (-(xx - cx) / 64 * 0.55 + -(yy - cy) / 64 * 0.83), 0.55, 1.3)
    layer = np.zeros((s, s, 4), np.float32)
    body = (ring | tooth)
    for c in range(3):
        layer[..., c] = np.where(body, np.clip(metal[..., c] * shade, 0, 255), 0)
    layer[..., 3] = body * 255
    cv.blit(layer)
    # hub
    hub = d < 26
    layer2 = np.zeros((s, s, 4), np.float32)
    for c, v in enumerate((79, 96, 98)):
        layer2[..., c] = np.where(hub, v * shade, 0)
    layer2[..., 3] = hub * 255
    cv.blit(layer2)
    # center highlight + hole
    hl = d < 13
    layer3 = np.zeros((s, s, 4), np.float32)
    for c, v in enumerate((212, 220, 212)):
        layer3[..., c] = np.where(hl, v * shade, 0)
    layer3[..., 3] = hl * 255
    cv.blit(layer3)
    hole = d < 7
    layer4 = np.zeros((s, s, 4), np.float32)
    layer4[..., :3] = 39
    layer4[..., 3] = hole * 255
    cv.blit(layer4)
    save(cv, name, 'item')


def wire(name, seed=190):
    cv = Canvas(seed=seed, transparent=True)
    for x in (26, 38, 50, 62, 74):
        yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
        d = np.sqrt((xx - (x + 14)) ** 2 + (yy - 61) ** 2)
        coil = (np.abs(((yy / 9.0) % 1) - 0.5) < 0.3) & (d < 34)
        shade = np.clip(0.7 + 0.55 * (-(xx - x - 14) / 34 * 0.4 - (yy - 61) / 34 * 0.9), 0.5, 1.35)
        layer = np.zeros((128, 128, 4), np.float32)
        for c, v in enumerate((224, 160, 99)):
            layer[..., c] = np.where(coil, np.clip(v * shade, 0, 255), 0)
        layer[..., 3] = coil * 255
        cv.blit(layer)
        dark = (np.abs(((yy / 9.0) % 1) - 0.5) >= 0.42) & (d < 30)
        layer2 = np.zeros((128, 128, 4), np.float32)
        layer2[..., :3] = (36, 51, 51)
        layer2[..., 3] = dark * 255
        cv.blit(layer2)
        side = np.clip(1 - np.abs(d - 34) / 3, 0, 1)
        layer3 = np.zeros((128, 128, 4), np.float32)
        layer3[..., :3] = (118, 62, 44)
        layer3[..., 3] = side * 255
        cv.blit(layer3)
    save(cv, name, 'item')


def dust_pile(name, color_main, color_dark, color_light, seed=200):
    cv = Canvas(seed=seed, transparent=True)
    s = 128
    yy, xx = np.mgrid[0:s, 0:s].astype(np.float32)
    # mound silhouette
    edge = np.abs(xx - 64) / 46.0
    mound_h = 34 * (1 - np.clip(edge, 0, 1) ** 0.8)
    inside = (100 - yy) <= mound_h
    n = fbm(s, seed + 1, base_cells=14, octaves=4)
    shade = np.clip(0.7 + 0.5 * n + 0.3 * (yy / s), 0.5, 1.3)
    layer = np.zeros((s, s, 4), np.float32)
    base = np.array(color_main, np.float32)
    for c in range(3):
        layer[..., c] = np.where(inside, np.clip(base[c] * shade, 0, 255), 0)
    layer[..., 3] = inside * 255
    cv.blit(layer)
    # light grains on top-left, dark in crevices
    spark = np.clip((fbm(s, seed + 2, base_cells=22, octaves=3) - 0.66) * 5, 0, 1)
    layer2 = np.zeros((s, s, 4), np.float32)
    layer2[..., :3] = color_light
    layer2[..., 3] = spark * inside * 200
    cv.blit(layer2)
    darkn = np.clip((fbm(s, seed + 3, base_cells=18, octaves=3) - 0.6) * 5, 0, 1)
    layer3 = np.zeros((s, s, 4), np.float32)
    layer3[..., :3] = color_dark
    layer3[..., 3] = darkn * inside * 160
    cv.blit(layer3)
    save(cv, name, 'item')


def crystal_item(name, seed=210):
    """Raw voltaite chunk: faceted crystal cluster."""
    cv = Canvas(seed=seed, transparent=True)
    s = 128
    cv.glow(64, 68, 40, VIO_DARK, peak=0.75)
    pts = []
    for a in (0, 55, 120, 200, 260, 310):
        rr = 34 + (seed * 3 + a) % 17 - 8
        pts.append((64 + rr * math.cos(a * math.pi / 180), 66 + rr * math.sin(a * math.pi / 180) * 0.85))
    mask = _fill_polygon(np.zeros((s, s), bool), pts)
    noise = fbm(s, seed + 1, base_cells=10, octaves=4)
    layer = np.zeros((s, s, 4), np.float32)
    for c, v in enumerate(VIO_MID):
        layer[..., c] = np.clip(v * (0.75 + 0.5 * noise), 0, 255)
    layer[..., 3] = mask * 255
    cv.blit(layer)
    inner = _fill_polygon(np.zeros((s, s), bool), [(48, 56), (72, 48), (82, 66), (62, 84), (48, 84)])
    layer2 = np.zeros((s, s, 4), np.float32)
    for c, v in enumerate(VIO_CORE):
        layer2[..., c] = np.clip(v * (0.85 + 0.3 * noise), 0, 255)
    layer2[..., 3] = inner * 255
    cv.blit(layer2)
    cv.glow(62, 62, 12, VIO_GLOW, peak=0.8)
    save(cv, name, 'item')


def refined_bar(name, main, streak, seed=220):
    cv = Canvas(seed=seed, transparent=True)
    cv.rect(22, 25, 106, 105, (44, 36, 50))
    body = np.zeros((80, 84, 4), np.float32)
    noise = fbm(128, seed + 1, base_cells=7, octaves=4)[:80, :84]
    body[..., :3] = np.clip(np.array(main, np.float32) * (0.8 + 0.4 * noise[..., None]), 0, 255)
    body[..., 3] = 255
    sub = Canvas(); sub.a = body
    _blit_region(cv, sub, 22, 20)
    cv.rect(22, 20, 106, 25, tuple(min(255, c + 40) for c in main))
    for x in (37, 56, 75):
        line(cv, x, 31, x + 10, 83, streak, 4, alpha=0.85)
    save(cv, name, 'item')


def matrix_item(seed=230):
    cv = Canvas(seed=seed, transparent=True)
    cv.rect(22, 25, 106, 105, (52, 52, 44))
    body = np.zeros((80, 84, 4), np.float32)
    noise = fbm(128, seed + 1, base_cells=7, octaves=4)[:80, :84]
    body[..., :3] = np.clip(np.array((137, 136, 116), np.float32) * (0.8 + 0.4 * noise[..., None]), 0, 255)
    body[..., 3] = 255
    sub = Canvas(); sub.a = body
    _blit_region(cv, sub, 22, 20)
    cv.rect(22, 20, 106, 25, (176, 175, 150))
    for x in (37, 56, 75):
        line(cv, x, 31, x + 10, 83, (212, 195, 224), 4, alpha=0.8)
    # grid hint
    for gx in range(30, 100, 16):
        line(cv, gx, 26, gx, 98, (90, 90, 76), 1, alpha=0.6)
    save(cv, 'mineral_matrix', 'item')


def tex_industry_materials():
    ingot('tin_ingot', (184, 205, 196), (235, 244, 240), (90, 110, 104), seed=150)
    ingot('bronze_ingot', (196, 138, 71), (240, 196, 138), (110, 66, 30), seed=152)
    ingot('steel_ingot', (158, 171, 176), (222, 230, 234), (74, 84, 90), seed=154)
    ingot('voltaite_ingot', (140, 54, 214), (236, 190, 255), (60, 18, 100), seed=156)

    def insulated_decor(cv):
        cv.rect(35, 34, 93, 88, (104, 82, 50))
        cv.rect(42, 40, 86, 82, (161, 132, 78))
        n = fbm(128, 163, base_cells=6, octaves=3)[:40, :44]
        region = cv.a[42:82, 42:86, :3]
        cv.a[42:82, 42:86, :3] = np.clip(region * (0.85 + 0.3 * n[..., None]), 0, 255)

    plate('steel_plate', (158, 171, 176), (210, 220, 226), seed=160)
    # insulated plate gets a copper frame + insulator center
    plate('insulated_plate', (150, 148, 128), (206, 204, 180), seed=161, decor=insulated_decor)

    circuit('basic_circuit', (66, 119, 97), seed=170)
    circuit('advanced_circuit', (99, 69, 134), seed=171)

    gear('steel_gear', seed=180)
    wire('copper_wire', seed=190)

    refined_bar('refined_voltaite', (152, 98, 196), (212, 195, 224), seed=220)
    matrix_item(seed=230)

    dust_pile('sawdust', (176, 146, 91), (120, 96, 56), (222, 198, 148), seed=201)
    dust_pile('iron_dust', (150, 150, 155), (95, 95, 100), (220, 220, 226), seed=202)
    dust_pile('copper_dust', (198, 118, 66), (130, 70, 36), (238, 176, 130), seed=203)
    dust_pile('gold_dust', (223, 195, 93), (150, 124, 44), (250, 234, 160), seed=204)
    dust_pile('tin_dust', (184, 205, 196), (110, 130, 124), (235, 246, 240), seed=205)

    crystal_item('raw_voltaite', seed=210)

    # raw tin chunk: grey metallic nuggets
    cv = Canvas(seed=213, transparent=True)
    rng_seed = 213
    for i in range(13):
        x = 20 + (rng_seed * 7 + i * 37) % 70
        y = 23 + (rng_seed * 11 + i * 53) % 68
        w, h = 14, 12
        yy, xx = np.mgrid[0:128, 0:128].astype(np.float32)
        d = np.sqrt((xx - (x + w / 2)) ** 2 + (yy - (y + h / 2)) ** 2) / (w * 0.6)
        nugget = d < 1
        shade = np.clip(0.75 + 0.45 * (-(xx - x - w / 2) / w * 0.5 - (yy - y - h / 2) / h * 0.86), 0.55, 1.3)
        layer = np.zeros((128, 128, 4), np.float32)
        for c, v in enumerate((168, 176, 172)):
            layer[..., c] = np.where(nugget, np.clip(v * shade, 0, 255), 0)
        layer[..., 3] = nugget * 255
        cv.blit(layer)
        rim = np.clip(1 - d, 0, 1) * (d > 0.6)
        layer2 = np.zeros((128, 128, 4), np.float32)
        layer2[..., :3] = (94, 100, 98)
        layer2[..., 3] = rim * nugget * 255
        cv.blit(layer2)
    save(cv, 'raw_tin', 'item')


def tex_voltaite_items():
    dust_pile('voltaite_dust', (140, 54, 214), (70, 20, 110), (236, 190, 255), seed=206)
    ingot('voltaite_ingot', (140, 54, 214), (236, 190, 255), (60, 18, 100), seed=156)


# ----------------------------------------------------------- colonial blocks
def tex_colonial_blocks():
    # ---- colonial_wood: aged planks with grain, knots and nail heads
    cv = Canvas(seed=301)
    cv.fill((120, 80, 49))
    grain = value_noise_aniso(128, 3, 30, 302)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.82 + 0.36 * grain[..., None]), 0, 255)
    streaks = fbm(128, 303, base_cells=4, octaves=5, aniso=6.0)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.88 + 0.24 * streaks[..., None]), 0, 255)
    for y in (0, 31, 63, 95):
        cv.rect(0, y, 128, y + 2, (59, 43, 34))
        cv.rect(0, y + 2, 128, y + 3, (146, 100, 64))
    # knots
    for kx, ky in ((44, 48), (88, 78)):
        cv.glow(kx, ky, 12, (89, 59, 41), peak=0.8)
        cv.glow(kx, ky, 7, (73, 47, 32), peak=0.9)
    cv.grain(7, seed=304)
    for nx in (7, 119):
        for ny in (9, 40, 73, 104):
            cv.rivet(nx, ny, 3, seed=305)
    save(cv, 'colonial_wood', 'block')

    # ---- colonial_iron: hammered dark iron
    cv = Canvas(seed=311)
    cv.fill((81, 88, 90))
    hammer = fbm(128, 312, base_cells=12, octaves=4)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.8 + 0.4 * hammer[..., None]), 0, 255)
    cv.aniso_roughness(6, seed=313)
    cv.shade(hammer, strength=1.6)
    cv.grain(6, seed=314)
    for nx in (12, 116):
        for ny in (12, 116):
            cv.rivet(nx, ny, 5, seed=315)
    cv.rect(0, 0, 128, 2, (172, 177, 171))
    save(cv, 'colonial_iron', 'block')

    # ---- colonial_stone: rough granite blocks
    cv = Canvas(seed=321)
    cv.fill((137, 136, 124))
    stains = fbm(128, 322, base_cells=6, octaves=5)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.78 + 0.44 * stains[..., None]), 0, 255)
    cv.grain(12, seed=323)
    h = fbm(128, 324, base_cells=9, octaves=4)
    cv.shade(h, strength=2.0)
    for i, y in enumerate((20, 45, 70, 95, 120)):
        cv.rect(0, y, 128, y + 2, (93, 98, 92))
        cv.rect(0, y - 2, 128, y, (156, 156, 142))
    save(cv, 'colonial_stone', 'block')


# --------------------------------------------------------------- crop stages
def tex_crops():
    for crop in ('rice', 'soy', 'mate'):
        for age in range(8):
            cv = Canvas(seed=400 + age * 3 + {'rice': 0, 'soy': 1, 'mate': 2}[crop], transparent=True)
            top = 110 - age * 12
            for x in (22, 45, 68, 94):
                # stalk with slight lean and two-tone edge
                line(cv, x + 1, 127, x + 5, top, (60, 87, 39), 5)
                line(cv, x + 3, 127, x + 6, top, (121, 150, 66), 2)
                y = 113
                while y > top + 8:
                    line(cv, x, y, x - 13, y - 15, (89, 126, 54), 5)
                    line(cv, x + 3, y - 8, x + 15, y - 20, (145, 173, 75), 4)
                    if crop in ('soy', 'mate'):
                        cv.glow(x - 9, y - 16, 7, (108, 151, 61), peak=0.85)
                        cv.glow(x - 10, y - 17, 4, (130, 172, 80), peak=0.7)
                    y -= 19
                if age >= 5:
                    if crop == 'rice':
                        line(cv, x + 4, top + 13, x + 15, top + 5, (187, 166, 91), 3)
                        for off in (0, 6, 12):
                            cv.glow(x + off + 3, top + 10 + off / 2, 4, (219, 199, 121), peak=0.95)
                            cv.glow(x + off + 3, top + 9 + off / 2, 2, (244, 230, 170), peak=0.8)
                    elif crop == 'soy':
                        for off in (5, 18, 31):
                            col = (199, 172, 91) if age == 7 else (145, 168, 72)
                            cv.glow(x + 7, top + off + 7, 5, col, peak=0.95)
            save(cv, f'{crop}_stage{age}', 'block')


# ------------------------------------------------------- colonial/other items
def tex_colonial_items():
    # mate_infusion: solid cuia (gourd) with yerba top and metal bombilla.
    # Silhouette-first: every body pixel carries full alpha so the item reads
    # in the inventory (the old glow-only art was 95% transparent).
    cv = Canvas(seed=501, transparent=True)
    GOURD=(148,100,59); GOURD_LO=(110,70,38); GOURD_HI=(189,140,84)
    LEATHER=(56,44,29); YERBA=(100,125,56); YERBA_HI=(140,165,90)
    METAL=(214,221,210); METAL_LO=(150,158,156)
    cv.contact_shadow(6, 0.45)
    # Body: stacked discs bulging at the belly, narrowing at foot and mouth.
    rows=[
        (38,92,40),(34,88,44),(30,84,48),(28,80,50),(26,76,52),(24,70,54),
        (22,60,56),(22,52,56),(24,46,54),(28,42,50),(34,40,44)]
    # rows are bottom-up: (y0,y1,halfwidth) with y measured from mouth top.
    # Draw foot, belly and shoulder as filled ellipses for a round cuia.
    cv.glow(64,86,30,LEATHER,peak=1.0,falloff=0.45)
    cv.glow(64,76,34,GOURD_LO,peak=1.0,falloff=0.5)
    cv.glow(64,66,36,GOURD,peak=1.0,falloff=0.45)
    cv.glow(64,54,34,GOURD_HI,peak=1.0,falloff=0.6)
    # Mouth ring + yerba mound poking above the rim.
    cv.glow(64,40,22,(90,60,34),peak=1.0,falloff=0.5)
    cv.glow(64,36,16,YERBA,peak=1.0,falloff=0.55)
    cv.glow(58,33,7,YERBA_HI,peak=1.0,falloff=0.7)
    # Metal rim band.
    cv.glow(64,41,24,METAL_LO,peak=0.9,falloff=0.12)
    # Bombilla: bright metal straw leaning right, dark edge for contrast.
    line(cv,72,68,90,16,METAL_LO,8)
    line(cv,72,66,90,14,METAL,6)
    line(cv,86,22,94,12,METAL,4)
    # Leather strap around the belly.
    line(cv,30,78,98,78,LEATHER,6)
    line(cv,30,74,98,74,GOURD_LO,2)
    save(cv, 'mate_infusion', 'item')

    # mate_leaf / dried_mate / ground_mate / mate_seeds: three leaves
    for name, base_col, vein in (
            ('mate_leaf', (118, 166, 76), (193, 199, 134)),
            ('dried_mate', (161, 162, 90), (206, 208, 150)),
            ('ground_mate', (146, 145, 82), (190, 190, 130)),
            ('mate_seeds', (150, 148, 84), (190, 190, 130))):
        cv = Canvas(seed=510, transparent=True)
        for x in (24, 48, 71):
            cv.glow(x + 14, 64, 15, (51, 76, 43), peak=0.9, falloff=1.3)
            cv.glow(x + 14, 60, 13, base_col, peak=0.95, falloff=1.4)
            line(cv, x + 14, 87, x + 14, 35, vein, 2)
        if name == 'ground_mate':
            cv.rect(20, 74, 108, 104, (142, 116, 74))
            n = fbm(128, 511, base_cells=12, octaves=3)[:30, :88]
            region = cv.a[74:104, 20:108, :3]
            cv.a[74:104, 20:108, :3] = np.clip(region * (0.8 + 0.4 * n[..., None]), 0, 255)
            cv.rect(22, 79, 106, 83, (190, 163, 120))
        if name == 'mate_seeds':
            for x in (37, 61, 87):
                cv.glow(x + 9, 86, 9, (210, 181, 130), peak=0.95, falloff=1.4)
        save(cv, name, 'item')

    # field_guide: leather book with leaf emblem
    cv = Canvas(seed=520, transparent=True)
    cv.rect(20, 12, 108, 117, (33, 27, 19))
    cv.rect(23, 14, 105, 113, (149, 101, 52))
    n = fbm(128, 521, base_cells=8, octaves=4)[:99, :82]
    region = cv.a[14:113, 23:105, :3]
    cv.a[14:113, 23:105, :3] = np.clip(region * (0.85 + 0.3 * n[..., None]), 0, 255)
    cv.rect(33, 17, 100, 109, (234, 217, 172))
    cv.rect(26, 14, 39, 113, (186, 136, 73))
    cv.rect(48, 38, 90, 84, (103, 117, 74))
    line(cv, 69, 72, 69, 48, (226, 203, 126), 5)
    line(cv, 69, 57, 59, 50, (226, 203, 126), 4)
    line(cv, 69, 65, 79, 57, (226, 203, 126), 4)
    for y in (25, 49, 73, 97):
        cv.rect(20, y, 39, y + 4, (73, 55, 35))
    save(cv, 'field_guide', 'item')

    # biomass_briquette: compressed block
    cv = Canvas(seed=530, transparent=True)
    cv.rect(19, 32, 107, 94, (42, 38, 30))
    cv.rect(23, 29, 103, 88, (112, 92, 61))
    n = fbm(128, 531, base_cells=10, octaves=4)[:59, :80]
    region = cv.a[29:88, 23:103, :3]
    cv.a[29:88, 23:103, :3] = np.clip(region * (0.8 + 0.4 * n[..., None]), 0, 255)
    cv.rect(23, 29, 103, 39, (146, 120, 73))
    for y in (48, 62, 76):
        line(cv, 28, y, 98, y - 3, (178, 154, 102), 3, alpha=0.8)
    cv.rect(35, 27, 43, 92, (65, 68, 54))
    cv.rect(82, 27, 90, 92, (65, 68, 54))
    save(cv, 'biomass_briquette', 'item')

    # vegetable_oil: golden bottle
    cv = Canvas(seed=540, transparent=True)
    cv.glow(64, 66, 34, (112, 82, 34), peak=0.85, falloff=1.4)
    cv.glow(64, 62, 30, (199, 150, 45), peak=0.95, falloff=1.6)
    cv.glow(62, 62, 20, (239, 211, 98), peak=0.9, falloff=1.8)
    cv.glow(50, 54, 9, (255, 240, 161), peak=0.9, falloff=1.8)
    save(cv, 'vegetable_oil', 'item')

    # flour / soy_meal: paper sack with powder top
    for name, powd in (('flour', (241, 232, 203)), ('soy_meal', (202, 179, 115))):
        cv = Canvas(seed=550, transparent=True)
        cv.rect(24, 37, 104, 106, (126, 101, 65))
        cv.rect(29, 42, 99, 100, (187, 165, 117))
        n = fbm(128, 551, base_cells=6, octaves=3)[:58, :70]
        region = cv.a[42:100, 29:99, :3]
        cv.a[42:100, 29:99, :3] = np.clip(region * (0.85 + 0.3 * n[..., None]), 0, 255)
        cv.glow(64, 46, 26, powd, peak=0.95, falloff=1.7)
        cv.rect(42, 70, 86, 90, (122, 140, 88))
        line(cv, 29, 104, 98, 104, (95, 81, 52), 3)
        save(cv, name, 'item')

    # grain piles: rice (polished), paddy, soybean, soy_seeds, rice_husk, rice_seeds
    specs = (
        ('rice', (238, 230, 202), (176, 168, 140), (255, 252, 235)),
        ('rice_paddy', (203, 178, 120), (140, 118, 70), (240, 224, 170)),
        ('soybean', (214, 189, 107), (150, 128, 62), (244, 228, 160)),
        ('soy_seeds', (180, 173, 98), (124, 118, 60), (228, 222, 150)),
        ('rice_husk', (181, 139, 69), (124, 92, 44), (232, 196, 130)),
        ('rice_seeds', (212, 188, 112), (146, 124, 68), (244, 228, 170)),
    )
    for name, main, dark, light in specs:
        cv = Canvas(seed=560, transparent=True)
        for i in range(13):
            x = 20 + (561 * 7 + i * 37) % 70
            y = 23 + (561 * 11 + i * 53) % 68
            w = 18 if name.startswith('soy') else 9
            h = 17 if name.startswith('soy') else 22
            cv.glow(x + w / 2, y + h / 2, max(w, h) * 0.62, dark, peak=0.9, falloff=1.6)
            cv.glow(x + w / 2, y + h / 2, max(w, h) * 0.52, main, peak=0.95, falloff=1.8)
            cv.glow(x + w * 0.3, y + h * 0.35, 2.2, light, peak=0.8, falloff=1.6)
        if name.endswith('seeds'):
            line(cv, 66, 106, 67, 73, (70, 92, 44), 5)
            line(cv, 67, 92, 84, 76, (146, 174, 81), 5)
        save(cv, name, 'item')


# ------------------------------------------------------------------ industry
def tex_industry_blocks():
    specs = {
        'industry_steel': ((57, 71, 78), 'steel'),
        'industry_bronze': ((156, 104, 62), 'bronze'),
        'industry_panel': ((176, 174, 142), 'panel'),
        'industry_core': ((84, 45, 119), 'core'),
        'industry_gauge': ((57, 71, 78), 'gauge'),
    }
    for name, (base, kind) in specs.items():
        cv = Canvas(seed=600)
        cv.fill(base)
        # brushed horizontal micro-streaks
        cv.aniso_roughness(8, seed=601)
        n = fbm(128, 602, base_cells=5, octaves=4)
        cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.85 + 0.3 * n[..., None]), 0, 255)
        cv.grain(6, seed=603)
        cv.rect(0, 0, 128, 5, (36, 45, 49))
        cv.rect(0, 123, 128, 128, (36, 45, 49))
        cv.bevel(inset=5, depth=6, top=10, bottom=-8, left=8, right=-8)
        for x in (12, 116):
            for y in (13, 115):
                cv.rivet(x, y, 5, seed=604)
        if kind == 'panel':
            for y in (24, 42, 60, 78, 96):
                cv.rect(16, y, 112, y + 7, (79, 91, 80))
                cv.rect(16, y + 7, 112, y + 8, (215, 211, 179))
                tone = 0.9 + 0.2 * fbm(96, 605 + y, base_cells=8, octaves=3)[4, 4]
                region = cv.a[y:y + 7, 16:112, :3]
                cv.a[y:y + 7, 16:112, :3] = np.clip(region * tone, 0, 255)
        elif kind == 'core':
            for x in (27, 58, 89):
                cv.rect(x, 16, x + 12, 112, (180, 108, 234))
                cv.glow(x + 6, 64, 9, (228, 175, 255), peak=0.5)
                cv.rect(x + 4, 16, x + 8, 112, (228, 175, 255))
        elif kind == 'gauge':
            gauge(cv, 64, 64, 46, needle_deg=-35.0)
        save(cv, name, 'block')


def tex_wood_stove():
    for kind, seed in (('brick', 701), ('iron', 702), ('cooktop', 703),
                       ('door_off', 704), ('door_on', 705), ('oven', 706), ('wood', 707)):
        cv = Canvas(seed=seed)
        if kind == 'brick':
            cv.fill((177, 157, 131))
            cv.grain(9, seed=708)
            for row in range(8):
                for col in range(-1, 5):
                    x = col * 32 + (row % 2) * 16
                    y = row * 16
                    v = (seed * 13 + row * 31 + col * 17) % 25 - 12
                    cv.rect(x + 1, y + 1, x + 31, y + 15, (151 + v, 72 + v // 2, 44 + v // 2))
                    cv.rect(x + 2, y + 2, x + 30, y + 3, (193 + v, 107 + v // 2, 66 + v // 2))
                    cv.rect(x + 2, y + 13, x + 31, y + 15, (108 + v, 49 + v // 2, 31 + v // 2))
            n = fbm(128, 709, base_cells=14, octaves=3)
            cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.92 + 0.16 * n[..., None]), 0, 255)
        elif kind == 'wood':
            cv.fill((91, 52, 25))
            for x in range(0, 128, 4):
                tone = 70 + (x * 7 + 13) % 75
                line(cv, x, 0, x + 5, 128, (tone, 66, 30), 2, alpha=0.7)
            cv.aniso_roughness(6, seed=710)
            cv.grain(5, seed=711)
        else:
            # dark steel base with vertical brush
            cv.fill((36, 38, 40))
            for y in range(128):
                v = 30 + (y * 11 + 3) % 13
                cv.rect(0, y, 128, y + 1, (v, v + 2, v + 4))
            cv.aniso_roughness(5, seed=712)
            hammer = fbm(128, 714, base_cells=9, octaves=5)
            cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.60 + 0.80 * hammer[..., None]), 0, 255)
            cv.shade(hammer, strength=3.0)
            cv.grain(6, seed=715)
            if kind == 'cooktop':
                for xy in ((35, 35), (91, 35), (35, 92)):
                    for radius in (25, 20, 14, 7):
                        ring(cv, xy[0], xy[1], radius, (10, 12, 13), 3)
                        arc(cv, xy[0], xy[1], radius - 1, (92, 95, 94), 1, 190, 120)
                    line(cv, xy[0] - 4, xy[1], xy[0] + 4, xy[1], (9, 10, 11), 2)
                cv.rect(0, 0, 128, 2, (95, 97, 94))
            elif kind.startswith('door'):
                cv.rect(8, 8, 120, 120, (10, 11, 12))
                if kind == 'door_on':
                    for y in range(15, 113):
                        t = (y - 15) / 98.0
                        cv.rect(13, y, 115, y + 1, (240, int(55 + 130 * t), 14))
                    for x in (23, 49, 78, 98):
                        pts = [(x, 105), (x - 12, 73), (x + 4, 30), (x + 14, 106)]
                        mask = _fill_polygon(np.zeros((128, 128), bool), pts)
                        layer = np.zeros((128, 128, 4), np.float32)
                        layer[..., :3] = (255, 204, 69)
                        layer[..., 3] = mask * 255
                        cv.blit(layer)
                for x in (24, 48, 72, 96):
                    cv.rect(x, 8, x + 5, 120, (43, 44, 43))
                for y in (9, 111):
                    cv.rect(7, y, 121, y + 8, (77, 75, 67))
            elif kind == 'oven':
                cv.rect(12, 12, 116, 116, (18, 20, 22))
                cv.rect(18, 18, 110, 110, (47, 50, 52))
                n = fbm(92, 713, base_cells=8, octaves=3)
                region = cv.a[18:110, 18:110, :3]
                cv.a[18:110, 18:110, :3] = np.clip(region * (0.85 + 0.3 * n[..., None]), 0, 255)
                ring(cv, 64, 42, 17, (124, 122, 111), 3)
                line(cv, 64, 42, 73, 33, (232, 165, 76), 2)
                cv.rect(32, 80, 96, 88, (113, 103, 87))
        save(cv, f'wood_stove_{kind}', 'block')


def ring(cv: Canvas, cx, cy, radius, color, width):
    steps = max(24, int(2 * math.pi * radius))
    for i in range(steps):
        a = 2 * math.pi * i / steps
        x = cx + radius * math.cos(a)
        y = cy + radius * math.sin(a)
        cv.glow(x, y, width * 0.9, color, peak=0.9, falloff=1.2)


def arc(cv: Canvas, cx, cy, radius, color, width, start_deg, sweep_deg):
    steps = max(10, int(sweep_deg * radius / 20))
    for i in range(steps + 1):
        a = math.radians(start_deg + sweep_deg * i / steps)
        x = cx + radius * math.cos(a)
        y = cy + radius * math.sin(a)
        cv.glow(x, y, width * 1.2, color, peak=0.85, falloff=1.4)


# ----------------------------------------------------------------------- GUI
def gui_slot(cv: Canvas, x: int, y: int):
    cv.rect(x - 1, y - 1, x + 17, y + 17, (26, 28, 33))
    cv.rect(x, y, x + 16, y + 16, (18, 20, 24))
    cv.rect(x, y, x + 16, y + 1, (12, 13, 16))
    cv.rect(x, y, x + 1, y + 16, (12, 13, 16))
    cv.rect(x, y + 15, x + 16, y + 16, (70, 76, 86))
    cv.rect(x + 15, y, x + 16, y + 16, (70, 76, 86))


def gui_energy_bar(cv: Canvas, x: int, y: int):
    cv.rect(x - 1, y - 1, x + 15, y + 65, (26, 28, 33))
    cv.rect(x, y, x + 14, y + 64, (14, 16, 19))
    cv.rect(x, y, x + 14, y + 2, (10, 11, 13))
    cv.rect(x, y + 62, x + 14, y + 64, (10, 11, 13))


def gui_panel(cv: Canvas, w=176, h=166):
    cv.rect(0, 0, w, h, (42, 44, 50))
    n = fbm(176, 801, base_cells=4, octaves=3)
    cv.a[..., :3] = np.clip(cv.a[..., :3] * (0.94 + 0.12 * n[:h, :w][..., None]), 0, 255)
    cv.rect(0, 0, w, 4, (22, 24, 28))
    cv.rect(0, h - 4, w, h, (22, 24, 28))
    cv.rect(0, 0, 4, h, (22, 24, 28))
    cv.rect(w - 4, 0, w, h, (22, 24, 28))
    cv.rect(4, 4, w - 4, 6, (96, 102, 112))
    cv.rect(8, 10, w - 8, 32, (30, 32, 37))


def gui_player_inv(cv: Canvas):
    for r in range(3):
        for c in range(9):
            gui_slot(cv, 8 + c * 18, 84 + r * 18)
    for c in range(9):
        gui_slot(cv, 8 + c * 18, 142)


def wheel(cv, cx: int, cy: int, radius: int):
    """Voxel-free wheel icon: rubber disc, steel hub, tread notches."""
    import numpy as np
    yy, xx = np.mgrid[0:cv.h, 0:cv.w].astype(np.float32)
    d2 = (xx - cx) ** 2 + (yy - cy) ** 2
    layer = np.zeros((cv.h, cv.w, 4), np.float32)
    rubber = d2 <= radius * radius
    inner = d2 <= (radius - 6) ** 2 if radius > 10 else d2 <= (radius - 4) ** 2
    layer[rubber, :3] = (20, 22, 26)
    layer[inner, :3] = (34, 37, 43)
    alpha = np.zeros((cv.h, cv.w), np.float32)
    alpha[rubber] = 255
    layer[..., 3] = alpha
    cv.blit(layer)
    for i in range(10):
        import math
        a = i * math.pi / 5
        x = cx + math.cos(a) * (radius - 2)
        y = cy + math.sin(a) * (radius - 2)
        cv.rect(int(x) - 2, int(y) - 2, int(x) + 2, int(y) + 2, (48, 52, 58))
    cv.rivet(cx, cy, max(6, radius // 3), seed=cx)


def tex_tractor_item():
    """SNC 75 side-view icon: orange hood, dark chassis, big rear wheel, exhaust."""
    cv = Canvas(seed=880)
    cv.brushed(STEEL_LIGHT, seed=881)
    cv.aniso_roughness(6, seed=882)
    # Ground shadow.
    cv.contact_shadow(4, 0.6)
    # Rear wheel (big, right side of the icon): dark disc + rivet rim + hub.
    wheel(cv, 88, 86, 30)
    # Front wheel (small, left).
    wheel(cv, 30, 98, 16)
    # Chassis.
    cv.rect(16, 62, 104, 80, (34, 30, 26))
    cv.rect(16, 62, 104, 66, (52, 46, 40))
    # Engine hood (SNC enamel orange).
    cv.rect(14, 40, 62, 64, ORANGE)
    cv.rect(14, 40, 62, 45, (255, 176, 74))
    cv.rect(18, 48, 34, 56, (222, 118, 18))
    # Grille lines on the hood front.
    for i in range(3):
        cv.rect(18 + i * 5, 52, 21 + i * 5, 62, (150, 78, 12))
    # Exhaust pipe with smoke tip.
    cv.rect(52, 24, 58, 42, STEEL_DARK)
    cv.rect(53, 22, 57, 26, (120, 126, 136))
    # Cab posts and canopy hint.
    cv.rect(62, 34, 68, 62, STEEL_DARK)
    cv.rect(92, 34, 98, 62, STEEL_DARK)
    cv.rect(58, 26, 102, 36, (40, 44, 50))
    # Seat.
    cv.rect(72, 46, 88, 60, (60, 44, 34))
    # Rear fender over wheel.
    cv.rect(64, 56, 112, 64, ORANGE)
    save(cv, 'tractor', 'item')


def tex_screwdriver():
    """Side-configuration tool: steel shaft, slotted tip, orange insulated grip."""
    cv = Canvas(seed=860)
    cv.brushed(STEEL_LIGHT, seed=861)
    cv.aniso_roughness(6, seed=862)
    # Diagonal shaft from the handle (top right) to the slotted tip (bottom left).
    line(cv, 38, 90, 84, 44, STEEL_DARK, 13)
    line(cv, 38, 90, 84, 44, STEEL_HI, 8)
    line(cv, 41, 87, 87, 41, (150, 158, 168), 3)
    # Slotted flat tip.
    cv.rect(24, 98, 46, 104, STEEL_DARK)
    cv.rect(26, 99, 44, 103, (168, 176, 186))
    cv.rect(30, 100, 40, 102, (14, 16, 19))
    # Insulated grip with moulded ribs.
    for i in range(9):
        t = i / 8
        x = 76 + int(t * 30)
        y = 46 - int(t * 30)
        cv.rect(x - 7, y - 7, x + 9, y + 9, ORANGE if i % 2 == 0 else (208, 112, 20))
    cv.rect(96, 30, 118, 52, (30, 26, 22))
    line(cv, 96, 30, 118, 52, ORANGE_HOT, 2)
    cv.rect(100, 34, 114, 48, (16, 18, 22))
    for i in range(4):
        line(cv, 103 + i * 3, 36, 103 + i * 3, 46, (222, 132, 30), 2)
    cv.contact_shadow(4, 0.5)
    save(cv, 'screwdriver', 'item')


def tex_guis():
    os.makedirs(TX['gui'], exist_ok=True)
    g1 = Canvas(176, 166, seed=810)
    gui_panel(g1)
    gui_slot(g1, 80, 33)
    gui_energy_bar(g1, 150, 10)
    gui_player_inv(g1)
    save(g1, 'gui_coal_generator', 'gui')

    for name in ('gui_electric_furnace', 'gui_crusher'):
        cv = Canvas(176, 166, seed=811)
        gui_panel(cv)
        gui_slot(cv, 56, 26)
        gui_slot(cv, 116, 26)
        gui_energy_bar(cv, 8, 10)
        gui_player_inv(cv)
        save(cv, name, 'gui')


def tex_icon():
    cv = Canvas(seed=900)
    cv.brushed(STEEL, seed=901)
    cv.aniso_roughness(7, seed=902)
    cv.frame(5, FRAME)
    cv.vignette(0.2)
    cv.glow(64, 64, 34, (200, 130, 20), peak=0.5)
    lightning(cv, 34, 26, 76)
    save(cv, 'icon_128', 'block')


# ---------------------------------------------------------------------- main
def main():
    for d in TX.values():
        os.makedirs(d, exist_ok=True)

    # blocks — machines
    tex_machine_side()
    tex_machine_top()
    tex_machine_bottom()
    tex_generator()
    tex_furnace()
    tex_crusher()
    tex_energy_cube()
    tex_cables()
    # blocks — ores
    tex_ores()
    # blocks — colonial materials + crops
    tex_colonial_blocks()
    tex_crops()
    # blocks — industrial materials + stove
    tex_industry_blocks()
    tex_wood_stove()
    # items
    tex_screwdriver()
    tex_tractor_item()
    tex_industry_materials()
    tex_voltaite_items()
    tex_colonial_items()
    # gui + icon
    tex_guis()
    tex_icon()

    # icon lives at assets root, not textures/: move it there
    src = os.path.join(TX['block'], 'icon_128.png')
    dst = os.path.join(ASSETS, 'icon_128.png')
    os.replace(src, dst)
    written.remove(('block', 'icon_128'))

    print(f'{len(written)} textures written.')


if __name__ == '__main__':
    main()
