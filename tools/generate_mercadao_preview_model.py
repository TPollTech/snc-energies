"""Generate the Mercadão 3D preview model (previews/mercadao-model.json).

Replays the exact block layout of tools/generate_mercadao_structure.py into a
vehicle-library style cuboid model (16 px = 1 block) so the preview shows the
building as it generates in the world: closed spruce hall with a centered
doorway, glass shop windows, four glass display counters with their goods on
the inner shelf, the attendant's post, lanterns and the anchor cell.
Standalone preview asset — nothing here is registered.
"""
from __future__ import annotations

import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "previews/mercadao-model.json"

W, H, D = 11, 6, 9  # x, y, z — same dims as the structure

# Material palette (128x128 swatches drawn below in write_materials()).
MAT = {
    "floor": "floor_stone",
    "floor_accent": "floor_accent",
    "planks": "spruce_planks",
    "log": "spruce_log",
    "pane": "glass_pane",
    "lantern": "lantern",
    "counter": "counter_wood",
    "counter_top": "counter_top",
    "counter_frame": "counter_frame",
    "glass": "counter_glass",
    "anchor": "anchor_ghost",
    "npc": "npc_mercador",
    "sign_board": "sign_board",
    "sack": "sack",
    "barrel": "barrel",
    # Goods colors follow each shelf line's cloth color.
    "goods_bebidas": "goods_bebidas",
    "goods_sementes": "goods_sementes",
    "goods_frios": "goods_frios",
    "goods_balcao_forte": "goods_balcao_forte",
}

GOODS_KEY = {"bebidas": "goods_bebidas", "sementes": "goods_sementes",
             "frios": "goods_frios", "balcao_forte": "goods_balcao_forte"}


def build_blocks():
    """Same placement rules as generate_mercadao_structure.build_nbt()."""
    grid = {}
    AIR = ("air", None)

    def setb(x, y, z, kind, shelf=None):
        grid[(x, y, z)] = (kind, shelf)

    for x in range(W):
        for z in range(D):
            setb(x, 0, z, "floor" if (x + z) % 2 == 0 else "floor_accent")
            setb(x, H - 1, z, "planks")
    # Back and side walls: planks with log columns every third line.
    for y in range(1, H - 1):
        for x in range(W):
            setb(x, y, 0, "log" if x % 3 == 0 else "planks")
        for z in range(1, D):
            for x in (0, W - 1):
                setb(x, y, z, "log" if z % 3 == 0 else "planks")
    # Side shop windows.
    for z in (4, 5):
        for x in (0, W - 1):
            setb(x, 2, z, "pane")
            setb(x, 3, z, "pane")
    # Front wall (z = D-1, south): centered 2x2 doorway plus display windows;
    # the band above the door carries the painted Mercadão sign.
    front = D - 1
    for y in range(1, H - 1):
        for x in range(W):
            if 5 <= x <= 6 and y <= 2:
                continue  # doorway
            if x % 3 == 0:
                setb(x, y, front, "log")
            elif (1 <= x <= 2 or 7 <= x <= 8) and y in (2, 3):
                setb(x, y, front, "pane")
            else:
                setb(x, y, front, "planks")
    for part, sx in (("left", 4), ("center", 5), ("right", 6)):
        setb(sx, 3, front, "sign", part)
    # Interior scenery: crates (one pair carries the standing lanterns), sacks.
    for pos in ((1, 1, 1), (9, 1, 1), (1, 1, 2), (9, 1, 2), (1, 1, 7), (9, 1, 7)):
        setb(*pos, "barrel")
    setb(1, 2, 1, "lantern")
    setb(9, 2, 1, "lantern")
    for pos in ((2, 1, 1), (3, 1, 1), (7, 1, 1), (8, 1, 1), (2, 1, 6), (8, 1, 6), (4, 1, 1), (6, 1, 1)):
        setb(*pos, "sack")
    counter_z = 3
    for i, shelf_id in enumerate(("bebidas", "sementes", "frios", "balcao_forte")):
        x = 2 + i * 2
        setb(x, 1, counter_z, "counter", shelf_id)
    setb(5, 2, 2, "anchor")
    setb(5, 1, 2, "npc")  # the Mercador at his post
    setb(2, 2, counter_z, "lantern")
    setb(8, 2, counter_z, "lantern")
    return grid


def build_model():
    groups = []
    cubes = []
    grid = build_blocks()

    def group(name, origin, parent=None):
        groups.append(dict(name=name, origin=list(origin), parent=parent, rotation=[0, 0, 0]))

    group("mercadao", [0, 0, 0])

    def cube(name, x0, y0, z0, x1, y1, z1, material):
        cubes.append(dict(name=name, group="mercadao",
                          **{"from": [x0, y0, z0], "to": [x1, y1, z1]},
                          material=material,
                          origin=[(x0 + x1) / 2, (y0 + y1) / 2, (z0 + z1) / 2],
                          rotation=[0, 0, 0]))

    for (x, y, z), (kind, shelf) in sorted(grid.items()):
        if kind == "air":
            continue
        if kind == "counter":
            bx, bz = x * 16, z * 16
            # Base: front skirt, back skirt, side cheeks (hollow middle).
            cube(f"counter_base_{x}", bx, y * 16, bz, (x + 1) * 16, y * 16 + 7, bz + 3, MAT["counter"])
            cube(f"counter_back_{x}", bx, y * 16, bz + 13, (x + 1) * 16, y * 16 + 15, bz + 16, MAT["counter"])
            cube(f"counter_cheek_l_{x}", bx, y * 16, bz + 3, bx + 3, y * 16 + 7, bz + 13, MAT["counter"])
            cube(f"counter_cheek_r_{x}", bx + 13, y * 16, bz + 3, (x + 1) * 16, y * 16 + 7, bz + 13, MAT["counter"])
            # Counter top slab.
            cube(f"counter_top_{x}", bx, y * 16 + 15, bz, (x + 1) * 16, y * 16 + 16, bz + 16, MAT["counter_top"])
            # Front glass + corner posts + inner shelf.
            cube(f"counter_glass_{x}", bx, y * 16 + 7, bz + 0.5, (x + 1) * 16, y * 16 + 15, bz + 1.5, MAT["glass"])
            cube(f"counter_post_l_{x}", bx, y * 16 + 7, bz + 2, bx + 2, y * 16 + 15, bz + 4, MAT["counter_frame"])
            cube(f"counter_post_r_{x}", bx + 14, y * 16 + 7, bz + 2, (x + 1) * 16, y * 16 + 15, bz + 4, MAT["counter_frame"])
            cube(f"counter_shelf_{x}", bx, y * 16 + 9.5, bz + 3, (x + 1) * 16, y * 16 + 10.5, bz + 12, MAT["counter_frame"])
            # The goods: three mini products on the inner shelf, behind the glass.
            gk = MAT[GOODS_KEY[shelf]]
            for g in range(3):
                gx0 = bx + 1.5 + g * 4.5
                cube(f"counter_shelf_{x}_good{g}", gx0, y * 16 + 10.5, bz + 3.5,
                     gx0 + 3, y * 16 + 14, bz + 11.5, gk)
        elif kind == "pane":
            cube(f"pane_{x}_{y}_{z}", x * 16 + 7, y * 16, z * 16, x * 16 + 9, y * 16 + 16, (z + 1) * 16, MAT["pane"])
        elif kind == "npc":
            # Simple attendant figure: legs, body with apron, head with hat.
            cx = x * 16 + 8
            cube(f"npc_legs", cx - 3, y * 16, z * 16 + 4, cx + 3, y * 16 + 10, z * 16 + 12, MAT["planks"])
            cube(f"npc_body", cx - 4, y * 16 + 10, z * 16 + 4, cx + 4, y * 16 + 22, z * 16 + 12, MAT["counter_top"])
            cube(f"npc_apron", cx - 3, y * 16 + 12, z * 16 + 3, cx + 3, y * 16 + 20, z * 16 + 5, MAT["counter"])
            cube(f"npc_head", cx - 4, y * 16 + 22, z * 16 + 4, cx + 4, y * 16 + 30, z * 16 + 12, MAT["floor_accent"])
            cube(f"npc_hat", cx - 5, y * 16 + 30, z * 16 + 3, cx + 5, y * 16 + 32, z * 16 + 13, MAT["counter"])
            cube(f"npc_hat_top", cx - 3, y * 16 + 32, z * 16 + 5, cx + 3, y * 16 + 34, z * 16 + 11, MAT["counter"])
        elif kind == "sign":
            # Thin painted board on the wall face (game block model shape).
            cube(f"sign_{shelf}_{x}", x * 16, y * 16 + 10, z * 16, (x + 1) * 16, y * 16 + 16, z * 16 + 2, MAT["sign_board"])
        elif kind == "barrel":
            cube(f"barrel_{x}_{y}_{z}", x * 16 + 1, y * 16, z * 16 + 1, (x + 1) * 16 - 1, y * 16 + 14, (z + 1) * 16 - 1, MAT["barrel"])
        elif kind == "sack":
            cube(f"sack_{x}_{y}_{z}", x * 16 + 2, y * 16, z * 16 + 2, (x + 1) * 16 - 2, y * 16 + 11, (z + 1) * 16 - 2, MAT["sack"])
        elif kind == "anchor":
            cube(f"anchor_{x}_{y}_{z}", x * 16 + 6, y * 16 + 6, z * 16 + 6, x * 16 + 10, y * 16 + 10, z * 16 + 10, MAT["anchor"])
        else:
            material = MAT[kind]
            cube(f"{kind}_{x}_{y}_{z}", x * 16, y * 16, z * 16, (x + 1) * 16, (y + 1) * 16, (z + 1) * 16, material)

    return dict(
        name="Mercadão do SNC",
        units_per_block=16,
        status="modelling_preview",
        forward_axis="-Z",
        groups=groups,
        cubes=cubes,
        locators={"counter_row": [5, 16, 3 * 16], "npc_post": [5 * 16, 16, 2 * 16]},
        animation_axes={},
        cube_count=len(cubes),
        group_count=len(groups),
    )


PALETTE = {
    "floor_stone": ("#8f8b83", "piso de pedra"),
    "floor_accent": ("#7d7a72", "piso (junta)"),
    "spruce_planks": ("#6e4a2a", "parede/telhado pinheiro"),
    "spruce_log": ("#4a3018", "pilares"),
    "glass_pane": ("#bcd4dc", "vidro (janelas)"),
    "lantern": ("#e8c86a", "lanterna"),
    "counter_wood": ("#8a5a2e", "balcão"),
    "counter_top": ("#b07c42", "tampo"),
    "counter_frame": ("#6e421f", "moldura/prateleira"),
    "counter_glass": ("#d8eef2", "vidro da vitrine"),
    "sign_board": ("#f2dc9a", "placa pintada do letreiro"),
    "sack": ("#e8e2d0", "sacaria"),
    "anchor_ghost": ("#9adfff", "âncora (invisível no jogo)"),
    "npc_mercador": ("#d8c070", "chapéu"),
    "goods_bebidas": ("#2e6e4e", "produtos · bebidas"),
    "goods_sementes": ("#7a5a2e", "produtos · sementes"),
    "goods_frios": ("#c05a3a", "produtos · frios"),
    "goods_balcao_forte": ("#5a2a3a", "produtos · balcão forte"),
}


def write_materials(target_dir: Path):
    textures = target_dir / "mercadao"
    textures.mkdir(parents=True, exist_ok=True)
    manifest = {}
    import struct, zlib, math

    def png(path, pixels):
        h = len(pixels); w = len(pixels[0])
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
        path.write_bytes(body)

    def hexrgb(c):
        c = c.lstrip("#")
        return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))

    rng_seed = 77
    for key, (color, _label) in PALETTE.items():
        r, g, b = hexrgb(color)
        rng = __import__("random").Random(rng_seed)
        rng_seed += 1
        px = [[0] * 128 for _ in range(128)]
        for y in range(128):
            for x in range(128):
                d = rng.randint(-10, 10)
                px[y][x] = (min(255, max(0, r + d)) << 16) | (min(255, max(0, g + d)) << 8) | min(255, max(0, b + d)) | 0xFF000000
        # Panel lines for the big surfaces.
        for y in (0, 42, 84, 127):
            for x in range(128):
                px[y][x] = 0xFF000000 | (r // 2 << 16) | (g // 2 << 8) | b // 2
        png(textures / f"{key}.png", px)
        manifest[key] = {"file": f"mercadao/{key}.png", "color": color,
                         "roughness": 0.7, "metalness": 0.05}
    (target_dir / "mercadao-materials.json").write_text(
        json.dumps(manifest, indent=2) + "\n", encoding="utf-8")
    return manifest


def main():
    model = build_model()
    OUT.write_text(json.dumps(model, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    write_materials(ROOT / "assets")
    print(f"Mercadão preview: {len(model['cubes'])} cubes, "
          f"{len(model['groups'])} groups -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    main()
