#!/usr/bin/env python3
"""Generates the Mercadão structure NBT (gzip NBT, DataVersion 5023 = MC 26.3)
and the worldgen JSONs, mirroring the Adventures mercado_gago layout.

The NBT writer is declarative: build a tree of Python values, one recursive
serializer emits every tag exactly once (tag byte + name + payload), so there
is no chance of duplicated tag bytes.
"""
import gzip
import json
import os
import struct

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA = os.path.join(ROOT, "src", "main", "resources", "data", "snc_energies")

W, H, D = 11, 6, 9  # x, y, z


# ------------------------- declarative NBT -------------------------
# Node shapes:
#   ("byte", name, int) ("int", name, int) ("string", name, str)
#   ("byte_array", name, bytes) ("list", name, elem_tag_id, payloads:list[bytes])
#   ("compound", name, [nodes])   payload_bytes(node) -> bytes without tag+name

def _str(s):
    b = s.encode("utf-8")
    return struct.pack(">H", len(b)) + b


def payload_bytes(node):
    kind, name = node[0], node[1]
    if kind == "byte":
        return bytes([node[2]])
    if kind == "int":
        return struct.pack(">i", node[2])
    if kind == "string":
        return _str(node[2])
    if kind == "byte_array":
        return struct.pack(">i", len(node[2])) + bytes(node[2])
    if kind == "list":
        _, _, elem, children = node
        # List elements carry only payloads (no tag byte, no name).
        payloads = [payload_bytes(child) for child in children]
        return bytes([elem]) + struct.pack(">i", len(payloads)) + b"".join(payloads)
    if kind == "compound":
        return b"".join(named_bytes(child) for child in node[2]) + b"\x00"
    raise ValueError(kind)


def named_bytes(node):
    kind, name = node[0], node[1]
    tag_id = {"byte": 1, "int": 3, "string": 8, "byte_array": 7,
              "list": 9, "compound": 10}[kind]
    return bytes([tag_id]) + _str(name) + payload_bytes(node)


def to_nbt(root_node):
    # Root: one compound with an empty name; payload_bytes already ends it.
    return bytes([10]) + _str("") + payload_bytes(root_node)


# ------------------------- structure content -------------------------

def build_nbt():
    palette = []
    cache = {}

    def block_state(name, props=None):
        key = (name, tuple(sorted((props or {}).items())))
        if key not in cache:
            fields = [("string", "Name", name)]
            if props:
                fields.append(("compound", "Properties",
                               [("string", k, v) for k, v in sorted(props.items())]))
            cache[key] = len(palette)
            palette.append(("compound", "", fields))
        return cache[key]

    AIR = block_state("minecraft:air")
    PLANKS = block_state("minecraft:spruce_planks")
    LOG = block_state("minecraft:spruce_log")
    STRIPPED = block_state("minecraft:stripped_spruce_wood")
    PANE = block_state("minecraft:glass_pane")
    BARREL = block_state("minecraft:barrel")
    SACK = block_state("minecraft:white_wool")
    TILE_A = block_state("minecraft:stone_bricks")
    TILE_B = block_state("minecraft:polished_andesite")
    LANTERN = block_state("minecraft:lantern", {"hanging": "false"})
    ANCHOR = block_state("snc_energies:mercadao_anchor")

    def sign(part):
        return block_state("snc_energies:mercadao_sign",
                           {"facing": "south", "part": part})

    def shelf(shelf_id):
        return block_state("snc_energies:mercadao_shelf",
                           {"shelf": shelf_id, "facing": "south"})

    blocks = bytearray(W * H * D)

    def setb(x, y, z, b):
        blocks[(y * D + z) * W + x] = b

    # Floor (checker tile) and roof.
    for x in range(W):
        for z in range(D):
            setb(x, 0, z, TILE_A if (x + z) % 2 == 0 else TILE_B)
            setb(x, H - 1, z, PLANKS)
    # Back and side walls: spruce planks with log columns every third line.
    for y in range(1, H - 1):
        for x in range(W):
            if x % 3 == 0:
                setb(x, y, 0, LOG)
            else:
                setb(x, y, 0, PLANKS)
        for z in range(1, D):
            for x in (0, W - 1):
                if z % 3 == 0:
                    setb(x, y, z, LOG)
                else:
                    setb(x, y, z, PLANKS)
    # Side shop windows: glass panes flanked by the log columns.
    for z in (4, 5):
        for x in (0, W - 1):
            setb(x, 2, z, PANE)
            setb(x, 3, z, PANE)
    # Front wall (z = D-1, south): a centered 2x2 doorway (two blocks wide,
    # two tall: a player walks through without jumping) plus two display
    # windows, so the hall reads as a real market from the street. The band
    # above the door carries the painted Mercadão sign.
    front = D - 1
    for y in range(1, H - 1):
        for x in range(W):
            if 5 <= x <= 6 and y <= 2:
                continue  # doorway
            if x % 3 == 0:
                setb(x, y, front, LOG)
            elif (1 <= x <= 2 or 7 <= x <= 8) and y in (2, 3):
                setb(x, y, front, PANE)  # shop windows between the log columns
            else:
                setb(x, y, front, PLANKS)
    setb(4, 3, front, sign("left"))
    setb(5, 3, front, sign("center"))
    setb(6, 3, front, sign("right"))
    # Interior scenery, away from the counters and the attendant's strip:
    # crates (one pair carries the standing lanterns), flour sacks.
    for pos in ((1, 1, 1), (9, 1, 1), (1, 1, 2), (9, 1, 2), (1, 1, 7), (9, 1, 7)):
        setb(*pos, BARREL)
    setb(1, 2, 1, LANTERN)
    setb(9, 2, 1, LANTERN)
    for pos in ((2, 1, 1), (3, 1, 1), (7, 1, 1), (8, 1, 1), (2, 1, 6), (8, 1, 6), (4, 1, 1), (6, 1, 1)):
        setb(*pos, SACK)

    # Counter row facing the open front (south); attendant stands behind it.
    counter_z = 3
    for i, shelf_id in enumerate(("bebidas", "sementes", "frios", "balcao_forte")):
        x = 2 + i * 2
        setb(x, 1, counter_z, shelf(shelf_id))
    # The invisible market anchor behind the counter, centered: the BE below
    # spawns the Mercajeiro at (5,1,2), facing the aisle (south).
    setb(5, 2, 2, ANCHOR)
    # Lanterns resting on the counter ends (supported by the shelves).
    setb(2, 2, counter_z, LANTERN)
    setb(8, 2, counter_z, LANTERN)

    # Glass connectivity: vanilla panes decide their connections from neighbor
    # updates, and structure blocks do not run those updates during placement.
    # Every pane gets its north/south/east/west neighbors written explicitly so
    # no window generates with floating or miswired glass.
    PANE_PROPS = ("north", "south", "east", "west")

    def pal_name(idx):
        for f in palette[idx][2]:
            if f[1] == "Name":
                return f[2]
        return None

    def cell_name(x, y, z):
        if not (0 <= x < W and 0 <= y < H and 0 <= z < D):
            return "minecraft:air"
        return pal_name(blocks[(y * D + z) * W + x])

    pane_positions = []
    for y in range(H):
        for z in range(D):
            for x in range(W):
                if cell_name(x, y, z) == "minecraft:glass_pane":
                    pane_positions.append((x, y, z))
    for x, y, z in pane_positions:
        connections = {}
        for prop, (dx, dz) in zip(PANE_PROPS, ((0, -1), (0, 1), (1, 0), (-1, 0))):
            nname = cell_name(x + dx, y, z + dz)
            connections[prop] = "true" if nname in (
                "minecraft:glass_pane", "minecraft:spruce_log", "minecraft:spruce_planks") else "false"
        cache_key = ("pane-wired", tuple(sorted(connections.items())))
        if cache_key not in cache:
            cache[cache_key] = len(palette)
            palette.append(("compound", "", [
                ("string", "Name", "minecraft:glass_pane"),
                ("compound", "Properties", [("string", k, v) for k, v in sorted(connections.items())]),
            ]))
        blocks[(y * D + z) * W + x] = cache[cache_key]

    root = ("compound", "", [
        ("int", "DataVersion", 5023),
        ("list", "size", 3, [("int", "", v) for v in (W, H, D)]),
        ("list", "palette", 10, palette),
        ("byte_array", "blocks", bytes(blocks)),
        ("list", "entities", 10, []),
    ])
    return gzip.compress(to_nbt(root))


WORLDGEN = {
    # Data-driven worldgen JSONs live under worldgen/ in the datapack namespace
    # (structure NBT templates are the exception: data/<ns>/structure/). The
    # first release shipped the JSONs without the worldgen/ prefix and the
    # registries silently skipped them — /locate never found the Mercadão.
    "worldgen/structure/mercadao.json": {
        "type": "minecraft:jigsaw",
        "biomes": "#snc_energies:has_structure/mercadao",
        "spawn_overrides": {},
        "step": "surface_structures",
        "terrain_adaptation": "beard_box",
        "start_pool": "snc_energies:mercadao/start",
        "size": 1,
        "start_height": {"absolute": 0},
        "project_start_to_heightmap": "WORLD_SURFACE_WG",
        "max_distance_from_center": 80,
        "use_expansion_hack": True,
    },
    "worldgen/template_pool/mercadao/start.json": {
        "fallback": "minecraft:empty",
        "elements": [{
            "weight": 1,
            "element": {
                "projection": "rigid",
                "element_type": "minecraft:legacy_single_pool_element",
                "location": "snc_energies:mercadao",
                "processors": {"processors": []},
            },
        }],
    },
    "worldgen/structure_set/mercadao.json": {
        "structures": [{"structure": "snc_energies:mercadao", "weight": 1}],
        "placement": {
            "type": "minecraft:random_spread",
            "spacing": 24,
            "separation": 12,
            "salt": 557700679,
        },
    },
    "tags/worldgen/biome/has_structure/mercadao.json": {
        "replace": False,
        "values": [
            {"id": "minecraft:plains", "required": False},
            {"id": "minecraft:sunflower_plains", "required": False},
            {"id": "minecraft:forest", "required": False},
            {"id": "minecraft:birch_forest", "required": False},
            {"id": "minecraft:old_growth_birch_forest", "required": False},
            {"id": "minecraft:flower_forest", "required": False},
            {"id": "minecraft:meadow", "required": False},
            {"id": "minecraft:savanna", "required": False},
            {"id": "minecraft:savanna_plateau", "required": False},
            {"id": "minecraft:windswept_savanna", "required": False},
            {"id": "minecraft:beach", "required": False},
            # Taiga family.
            {"id": "minecraft:taiga", "required": False},
            {"id": "minecraft:snowy_taiga", "required": False},
            {"id": "minecraft:old_growth_pine_taiga", "required": False},
            {"id": "minecraft:old_growth_spruce_taiga", "required": False},
            # Dark forest and swamp family.
            {"id": "minecraft:dark_forest", "required": False},
            {"id": "minecraft:swamp", "required": False},
            {"id": "minecraft:mangrove_swamp", "required": False},
        ],
    },
}


def main():
    path = os.path.join(DATA, "structure", "mercadao.nbt")
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as f:
        f.write(build_nbt())
    print("wrote", os.path.relpath(path, ROOT))
    for rel, payload in WORLDGEN.items():
        target = os.path.join(DATA, rel)
        os.makedirs(os.path.dirname(target), exist_ok=True)
        with open(target, "w", encoding="utf-8", newline="\n") as f:
            json.dump(payload, f, indent=1)
        print("wrote", os.path.relpath(target, ROOT))


if __name__ == "__main__":
    main()
