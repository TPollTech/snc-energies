#!/usr/bin/env python3
"""Generates every Mercadão asset: 16x16 textures (shelf counters, spawn egg),
block models, blockstate variants, item models (26.3 items/ folder), the
machine panel, recipe, loot table and pt_br/en_us lang entries."""
import json
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, "src", "main", "resources", "assets", "snc_energies")
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "snc_energies")


def write(path, content):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    mode = "wb" if isinstance(content, bytes) else "w"
    with open(path, mode, **({} if isinstance(content, bytes) else {"encoding": "utf-8"})) as f:
        f.write(content)
    print("wrote", os.path.relpath(path, ROOT))


def png(path, pixels):
    """pixels: list of rows of ARGB ints (0 = transparent)."""
    h = len(pixels)
    w = len(pixels[0])
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for argb in row:
            raw += bytes(((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255))
    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    body = b"\x89PNG\r\n\x1a\n"
    body += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    body += chunk(b"IDAT", zlib.compress(raw, 9))
    body += chunk(b"IEND", b"")
    write(path, body)


def rect(x0, y0, x1, y1, color):
    return (x0, y0, x1, y1, color)


def draw(pixels, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(len(pixels), y1)):
        for x in range(max(0, x0), min(16, x1)):
            pixels[y][x] = color


SHELVES = {
    "mercadao_shelf_bebidas": {"wood": "#8a5a2e", "wood_dark": "#6e421f", "top": "#b07c42", "cloth": "#2e6e4e", "accent": "#e8c86a"},
    "mercadao_shelf_sementes": {"wood": "#8a5a2e", "wood_dark": "#6e421f", "top": "#b07c42", "cloth": "#7a5a2e", "accent": "#d8e86a"},
    "mercadao_shelf_frios": {"wood": "#8a5a2e", "wood_dark": "#6e421f", "top": "#b07c42", "cloth": "#c05a3a", "accent": "#f2f2e0"},
    "mercadao_shelf_balcao_forte": {"wood": "#4a3826", "wood_dark": "#332616", "top": "#6a5238", "cloth": "#5a2a3a", "accent": "#c8a24a"},
}


def shelf_top_texture(name, c):
    """Counter top plank: light wood with seams and grain dashes."""
    px = [[0] * 16 for _ in range(16)]
    top = int(c["top"][1:], 16) | 0xFF000000
    dark = int(c["wood_dark"][1:], 16) | 0xFF000000
    draw(px, 0, 0, 16, 16, top)
    for x in (4, 8, 12):
        draw(px, x, 0, x + 1, 16, dark)          # plank seams
    draw(px, 1, 5, 6, 6, dark)                    # grain dashes
    draw(px, 9, 10, 15, 11, dark)
    png(os.path.join(ASSETS, "textures", "block", f"{name}_top.png"), px)


def shelf_frame_texture(name, c):
    """Dark wood frame carrying the shelf's accent stripe at the top edge."""
    px = [[0] * 16 for _ in range(16)]
    dark = int(c["wood_dark"][1:], 16) | 0xFF000000
    accent = int(c["accent"][1:], 16) | 0xFF000000
    draw(px, 0, 0, 16, 16, dark)
    draw(px, 0, 0, 16, 2, accent)
    png(os.path.join(ASSETS, "textures", "block", f"{name}_frame.png"), px)


def shelf_glass_texture(name, c):
    """Vanilla-style shop glass: pale frame, two highlight streaks, clear center."""
    px = [[0] * 16 for _ in range(16)]
    edge = 0xFFC8E8EC
    hi = 0xFFE8F6F8
    draw(px, 0, 0, 16, 1, edge)
    draw(px, 0, 15, 16, 16, edge)
    draw(px, 0, 0, 1, 16, edge)
    draw(px, 15, 0, 16, 16, edge)
    for i in range(16):                            # diagonal streaks
        if 3 <= i <= 6:
            px[i][i + 2 if i + 2 < 15 else 14] = hi
            px[i + 1][i] = hi
        if 9 <= i <= 11:
            px[i][i + 3 if i + 3 < 15 else 14] = hi
    png(os.path.join(ASSETS, "textures", "block", f"{name}_glass.png"), px)


def shelf_texture(name, c):
    px = [[0] * 16 for _ in range(16)]
    draw(px, 0, 0, 16, 15, int(c["wood"][1:], 16) | 0xFF000000)       # counter body
    draw(px, 0, 0, 16, 1, int(c["top"][1:], 16) | 0xFF000000)         # top plank
    draw(px, 0, 14, 16, 15, int(c["wood_dark"][1:], 16) | 0xFF000000) # base shadow
    for x in (3, 7, 11):                                              # plank seams
        draw(px, x, 1, x + 1, 14, int(c["wood_dark"][1:], 16) | 0xFF000000)
    draw(px, 0, 2, 16, 5, int(c["cloth"][1:], 16) | 0xFF000000)       # awning stripe
    draw(px, 0, 4, 16, 5, int(c["accent"][1:], 16) | 0xFF000000)      # accent stripe
    draw(px, 1, 6, 2, 14, int(c["wood_dark"][1:], 16) | 0xFF000000)   # corner posts
    draw(px, 14, 6, 15, 14, int(c["wood_dark"][1:], 16) | 0xFF000000)
    png(os.path.join(ASSETS, "textures", "block", f"{name}.png"), px)


SIGN_FONT = {
    # 3x5 pixel glyphs, row-major bits (MSB = left pixel).
    "M": [0b101, 0b111, 0b101, 0b101, 0b101],
    "E": [0b111, 0b100, 0b110, 0b100, 0b111],
    "R": [0b110, 0b101, 0b110, 0b101, 0b101],
    "C": [0b011, 0b100, 0b100, 0b100, 0b011],
    "A": [0b010, 0b101, 0b111, 0b101, 0b101],
    "D": [0b110, 0b101, 0b101, 0b101, 0b110],
    "O": [0b010, 0b101, 0b101, 0b101, 0b010],
    "~": [0b000, 0b101, 0b010, 0b000, 0b000],
}


def sign_textures():
    """The painted Mercadão sign: three 16x16 boards that read as one band.
    The word M E R C A D Ã O is drawn from the 3x5 SIGN_FONT, upscaled 2x,
    split left/center/right. Board background pale straw with a dark wood
    frame and grain; letters near-black like hand paint. Each part samples
    its slice of the shared band, so the frames join seamlessly."""
    BOARD = 0xFFF2DC9A
    BOARD_SHADE = 0xFFE8C86A
    FRAME = 0xFF6E421F
    INK = 0xFF2A1A0C
    word = "MERCADÃO"
    scale = 2
    gh = 6  # rows: 1 tilde row + 5 glyph rows
    # Word grid at 2x: 8 letters * 3 columns * 2 = 48 px, exactly the band
    # width — one continuous painted band, no per-board seams.
    grid = [[0] * (len(word) * 3 * scale) for _ in range(gh * scale)]
    for index, ch in enumerate(word):
        for r, bits in enumerate(SIGN_FONT["A" if ch == "Ã" else ch]):
            for col in range(3):
                if bits & (0b100 >> col):
                    for dy in range(scale):
                        for dx in range(scale):
                            grid[(r + 1) * scale + dy][index * 3 * scale + col * scale + dx] = 1
    # Tilde over the A (index 6 in MERCADÃO): dedicated two-row glyph on
    # the top grid rows, directly above the A's peak.
    tilde_rows = (0b101, 0b010)
    for r, bits in enumerate(tilde_rows):
        for col in range(3):
            if bits & (0b100 >> col):
                for dy in range(scale):
                    for dx in range(scale):
                        grid[r * scale + dy][6 * 3 * scale + col * scale + dx] = 1

    # Full band: 48x16 px (3 boards side by side, seamless).
    full = [[0] * 48 for _ in range(16)]
    for y in range(16):
        for x in range(48):
            full[y][x] = BOARD
    # Frame: top and bottom only (the sides run into the wall columns).
    for x in range(48):
        for y in (0, 1, 14, 15):
            full[y][x] = FRAME
    # Wood grain.
    rng = random.Random(31)
    for _ in range(40):
        y = rng.randint(2, 13)
        x = rng.randint(0, 47)
        if full[y][x] == BOARD:
            full[y][x] = BOARD_SHADE
    # Stamp the word (12 rows incl. tilde) between the frames.
    for gy, row in enumerate(grid):
        for gx, on in enumerate(row):
            if on:
                full[2 + gy][gx] = INK
    return {"left": [row[0:16] for row in full],
            "center": [row[16:32] for row in full],
            "right": [row[32:48] for row in full]}


def egg_texture():
    """Vanilla-style spawn egg: proper egg silhouette (narrow top, wide base),
    shaded with the merchant's vest palette and straw spots. Vanilla eggs are
    pre-colored PNGs — no tint layers — so the shape must carry the art."""
    import math
    rng = random.Random(90)
    BODY = 0xFF6E421F       # counter wood / vest brown
    BODY_HI = 0xFF8A5A2E
    BODY_LO = 0xFF4A2E14
    SPOT = 0xFFF2DC9A       # straw spots (straw hat)
    OUTLINE = 0xFF2A1A0C
    px = [[0] * 16 for _ in range(16)]
    cx = 7.5
    # Egg outline: half-width per row (y 0..15), narrow top, wide base.
    halfwidth = [1.1, 2.0, 2.6, 3.1, 3.5, 3.9, 4.2, 4.4, 4.6, 4.7, 4.7, 4.6, 4.4, 4.0, 3.4, 2.4]
    for y in range(16):
        hw = halfwidth[y]
        for x in range(16):
            d = abs(x - cx)
            if d <= hw:
                px[y][x] = BODY
    # Highlight upper-left, shade lower-right for the vanilla rounded look.
    for y in range(16):
        hw = halfwidth[y]
        for x in range(16):
            if px[y][x] != BODY:
                continue
            d = x - cx
            if d < -hw * 0.25 and y < 9:
                px[y][x] = BODY_HI
            if d > hw * 0.35 and y > 7:
                px[y][x] = BODY_LO
    # Outline: darkest at the silhouette edge.
    for y in range(16):
        hw = halfwidth[y]
        for x in range(16):
            d = abs(x - cx)
            if px[y][x] != 0 and (d > hw - 1.0 or y == 0):
                px[y][x] = OUTLINE
    # Straw freckles inside the body only.
    for _ in range(12):
        y = rng.randint(2, 13)
        hw = max(1, int(halfwidth[y]) - 2)
        x = int(cx) + rng.randint(-hw, hw)
        if px[y][x] not in (0, OUTLINE):
            px[y][x] = SPOT
            if x + 1 < 16 and px[y][x + 1] not in (0, OUTLINE):
                px[y][x + 1] = SPOT
    png(os.path.join(ASSETS, "textures", "item", "mercajeiro_spawn_egg.png"), px)


def block_model(name):
    return {
        "parent": "block/block",
        "textures": {
            "top": f"snc_energies:block/{name}_top" if False else f"snc_energies:block/{name}",
            "side": f"snc_energies:block/{name}",
            "bottom": f"snc_energies:block/{name}",
            "particle": f"snc_energies:block/{name}",
        },
    }


def main():
    # Textures
    for name, colors in SHELVES.items():
        shelf_texture(name, colors)
        shelf_top_texture(name, colors)
        shelf_frame_texture(name, colors)
        shelf_glass_texture(name, colors)
    egg_texture()
    sign_parts = sign_textures()
    for part, pixels in sign_parts.items():
        png(os.path.join(ASSETS, "textures", "block", f"mercadao_sign_{part}.png"), pixels)

    # Sign blockstate: 3 parts x 4 facings. The band texture runs left to
    # right when read from the street, so west/east rotate the model and the
    # east side mirrors its slice (the word stays in order on the facade).
    sign_variants = {}
    for part in ("left", "center", "right"):
        for facing, y in [("north", 0), ("south", 180), ("east", 270), ("west", 90)]:
            sign_variants[f"part={part},facing={facing}"] = {
                "model": f"snc_energies:block/mercadao_sign_{part}",
                "y": y,
                "uvlock": False,
            }
    write(os.path.join(ASSETS, "blockstates", "mercadao_sign.json"),
          json.dumps({"variants": sign_variants}, indent=1))

    # Sign models: thin board on the wall face, each sampling its slice of
    # the band texture. Model north = the reader side; the blockstate
    # rotates it to the sign's facing (structure builds them facing south).
    for part, uside, vside in (("left", 0, 0), ("center", 16, 0), ("right", 32, 0)):
        model = {
            "parent": "block/block",
            "textures": {
                "sign": f"snc_energies:block/mercadao_sign_{part}",
                "planks": "minecraft:block/spruce_planks",
                "particle": "minecraft:block/spruce_planks",
            },
            "elements": [
                {"from": [0, 10, 0], "to": [16, 16, 2], "faces": {
                    "north": {"texture": "#sign", "uv": [uside, vside, uside + 16, vside + 16]},
                    "south": {"texture": "#planks"},
                    "east": {"texture": "#planks"},
                    "west": {"texture": "#planks"},
                    "up": {"texture": "#planks"},
                    "down": {"texture": "#planks"}}},
                # Wall plug behind the board: fills the cell so no z-fight
                # with the building wall behind it.
                {"from": [0, 0, 2], "to": [16, 16, 16], "faces": {
                    "north": {"texture": "#planks"}, "south": {"texture": "#planks"},
                    "east": {"texture": "#planks"}, "west": {"texture": "#planks"},
                    "up": {"texture": "#planks"}, "down": {"texture": "#planks"}}},
            ],
        }
        write(os.path.join(ASSETS, "models", "block", f"mercadao_sign_{part}.json"),
              json.dumps(model, indent=1))

    # Block models: glass display counter (base, top, posts, back panel,
    # inner shelf, front glass). Model north = display side; the blockstate
    # rotates it to the shelf's facing. The renderer floats the goods on the
    # inner shelf, behind the glass. Cutout so the glass center is see-through.
    for name, colors in SHELVES.items():
        model = {
            "parent": "block/block",
            "render_type": "minecraft:cutout",
            "textures": {
                "side": f"snc_energies:block/{name}",
                "top": f"snc_energies:block/{name}_top",
                "frame": f"snc_energies:block/{name}_frame",
                "glass": f"snc_energies:block/{name}_glass",
                "particle": f"snc_energies:block/{name}",
            },
            "elements": [
                # Base: solid front skirt, back skirt and side cheeks.
                {"from": [0, 0, 0], "to": [16, 7, 3], "faces": {
                    "north": {"texture": "#side"}, "south": {"texture": "#side"},
                    "east": {"texture": "#side"}, "west": {"texture": "#side"},
                    "up": {"texture": "#frame"}, "down": {"texture": "#side"}}},
                {"from": [0, 0, 13], "to": [16, 7, 13 + 3], "faces": {
                    "south": {"texture": "#side"},
                    "east": {"texture": "#side"}, "west": {"texture": "#side"},
                    "down": {"texture": "#side"}}},
                {"from": [0, 0, 3], "to": [3, 7, 13], "faces": {
                    "east": {"texture": "#side"}, "up": {"texture": "#frame"}}},
                {"from": [13, 0, 3], "to": [16, 7, 13], "faces": {
                    "west": {"texture": "#side"}, "up": {"texture": "#frame"}}},
                # Back panel rising from the back skirt to the top.
                {"from": [0, 7, 13], "to": [16, 15, 16], "faces": {
                    "south": {"texture": "#side"}, "east": {"texture": "#frame"},
                    "west": {"texture": "#frame"}}},
                # Counter top slab (15/16 keeps the old click plane).
                {"from": [0, 15, 0], "to": [16, 16, 16], "faces": {
                    "north": {"texture": "#frame"}, "south": {"texture": "#frame"},
                    "east": {"texture": "#frame"}, "west": {"texture": "#frame"},
                    "up": {"texture": "#top"}, "down": {"texture": "#side"}}},
                # Front glass: one thin cutout pane just outside the posts.
                {"from": [0, 7, 0.5], "to": [16, 15, 1.5], "faces": {
                    "north": {"texture": "#glass"}, "south": {"texture": "#glass"},
                    "up": {"texture": "#frame"}, "down": {"texture": "#frame"}}},
                # Corner posts beside the glass.
                {"from": [0, 7, 2], "to": [2, 15, 4], "faces": {
                    "north": {"texture": "#frame"}, "south": {"texture": "#frame"},
                    "east": {"texture": "#frame"}, "west": {"texture": "#frame"},
                    "up": {"texture": "#frame"}}},
                {"from": [14, 7, 2], "to": [16, 15, 4], "faces": {
                    "north": {"texture": "#frame"}, "south": {"texture": "#frame"},
                    "east": {"texture": "#frame"}, "west": {"texture": "#frame"},
                    "up": {"texture": "#frame"}}},
                # Inner shelf the goods stand on (renderer row height 10/16).
                {"from": [0, 9.5, 3], "to": [16, 10.5, 12], "faces": {
                    "north": {"texture": "#frame"}, "south": {"texture": "#frame"},
                    "up": {"texture": "#top"}, "down": {"texture": "#side"},
                    "east": {"texture": "#frame"}, "west": {"texture": "#frame"}}},
            ],
        }
        write(os.path.join(ASSETS, "models", "block", f"{name}.json"),
              json.dumps(model, indent=1))

    # Blockstate: 4 shelf variants x 4 facings.
    variants = {}
    for shelf_key, variant in [("bebidas", "bebidas"), ("sementes", "sementes"),
                               ("frios", "frios"), ("balcao_forte", "balcao_forte")]:
        name = f"mercadao_shelf_{shelf_key}"
        for facing, y in [("north", 0), ("south", 180), ("west", 270), ("east", 90)]:
            variants[f"shelf={variant},facing={facing}"] = {
                "model": f"snc_energies:block/{name}", "y": y}
    write(os.path.join(ASSETS, "blockstates", "mercadao_shelf.json"),
          json.dumps({"variants": variants}, indent=1))

    # Item model (26.3): block item via items/ definition.
    write(os.path.join(ASSETS, "items", "mercadao_shelf.json"),
          json.dumps({"model": {"type": "minecraft:model",
                                "model": "snc_energies:block/mercadao_shelf_bebidas"}}, indent=1))
    write(os.path.join(ASSETS, "items", "mercajeiro_spawn_egg.json"),
          json.dumps({"model": {"type": "minecraft:model",
                                "model": "snc_energies:item/mercajeiro_spawn_egg"}}, indent=1))
    write(os.path.join(ASSETS, "models", "item", "mercajeiro_spawn_egg.json"),
          json.dumps({"parent": "minecraft:item/template_spawn_egg",
                      "textures": {"layer0": "snc_energies:item/mercajeiro_spawn_egg"}}, indent=1))

    # Recipe: wooden commerce stall (planks + barrel + emerald counter).
    recipe = {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "pattern": ["PPP", "PEP", "BPB"],
        "key": {
            "P": "minecraft:spruce_planks",
            "E": "minecraft:emerald",
            "B": "minecraft:barrel",
        },
        "result": {"id": "snc_energies:mercadao_shelf", "count": 4},
    }
    write(os.path.join(DATA, "recipe", "mercadao_shelf.json"), json.dumps(recipe, indent=1))

    # Loot table.
    write(os.path.join(DATA, "loot_table", "blocks", "mercadao_shelf.json"),
          json.dumps({
              "type": "minecraft:block",
              "pools": [{"rolls": 1, "entries": [
                  {"type": "minecraft:item", "name": "snc_energies:mercadao_shelf"}],
                  "conditions": [{"condition": "minecraft:survives_explosion"}]}],
          }, indent=1))

    # Machine panel for the screen.
    panel = {
        "id": "mercadao",
        "subtitle": ["Mercadão do SNC · balcão de vendas",
                     "SNC street market · purchase counter"],
        "accent": "#e8c86a",
        "rects": [
            [0, 0, 256, 238, "#101619"],
            [2, 2, 252, 234, "#6e421f"],
            [7, 7, 242, 27, "#131c21"],
            [7, 33, 242, 2, "#e8c86a"],
            [7, 39, 242, 94, "#1a252b"],
            [7, 137, 242, 2, "#e8c86a"],
            [7, 143, 242, 90, "#182028"],
        ],
        "slots": [],
        "bars": [],
        "button": None,
    }
    panels_path = os.path.join(ASSETS, "machine_panels.json")
    with open(panels_path, encoding="utf-8") as f:
        panels = json.load(f)
    panels["mercadao"] = panel
    write(panels_path, json.dumps(panels, indent=1, ensure_ascii=False))
    # Grain cart panel (SNC 90-C): keep regeneration idempotent with the game JSON.
    import copy
    grain_cart_panel = json.loads(json.dumps({'id': 'grain_cart', 'subtitle': ['Carreta graneleira · carga do tanque', 'Grain cart · tank cargo'], 'accent': '#b0873f', 'rects': [[0, 0, 256, 238, '#101619'], [2, 2, 252, 234, '#5a4630'], [7, 7, 242, 27, '#131c21'], [7, 33, 242, 2, '#e8973f'], [7, 39, 242, 60, '#1a252b'], [62, 43, 116, 56, '#232e35'], [66, 45, 112, 52, '#181f24'], [13, 100, 231, 2, '#e8973f'], [7, 107, 242, 21, '#131c21'], [13, 111, 108, 13, '#2c3a42'], [135, 111, 108, 13, '#2c3a42']], 'slots': [{'index': 0, 'x': 70, 'y': 47, 'role': 'c1'}, {'index': 1, 'x': 92, 'y': 47, 'role': 'c2'}, {'index': 2, 'x': 114, 'y': 47, 'role': 'c3'}, {'index': 3, 'x': 136, 'y': 47, 'role': 'c4'}, {'index': 4, 'x': 158, 'y': 47, 'role': 'c5'}, {'index': 5, 'x': 70, 'y': 69, 'role': 'c6'}, {'index': 6, 'x': 92, 'y': 69, 'role': 'c7'}, {'index': 7, 'x': 114, 'y': 69, 'role': 'c8'}, {'index': 8, 'x': 136, 'y': 69, 'role': 'c9'}, {'index': 9, 'x': 158, 'y': 69, 'role': 'c10'}, {'index': 10, 'x': 70, 'y': 91, 'role': 'c11'}, {'index': 11, 'x': 92, 'y': 91, 'role': 'c12'}, {'index': 12, 'x': 114, 'y': 91, 'role': 'c13'}, {'index': 13, 'x': 136, 'y': 91, 'role': 'c14'}, {'index': 14, 'x': 158, 'y': 91, 'role': 'c15'}], 'bars': [], 'button': [13, 134, 110, 16]}, ensure_ascii=False))
    panels["grain_cart"] = grain_cart_panel
    write(panels_path, json.dumps(panels, indent=1, ensure_ascii=False))

    print("done")


if __name__ == "__main__":
    main()
