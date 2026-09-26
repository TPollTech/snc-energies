"""Shared procedural vehicle authoring toolkit for SNC Energies.

Golden reference: the SNC 75 tractor (AGENTS.md rules 6 and 7). Every
vehicle generator authors rigs and exports through this library so all
vehicles keep the same conventions: Minecraft pixels (+Y up, -Z forward,
16 units per block), Blockbench ZYX Euler rotations, absolute rest-pose
origins, and canonical JSON + Blockbench + glTF 2 GLB exports.

The API is extracted from tools/generate_tractor.py: the refactored
tractor generator regenerates its assets byte-identically through this
library, which pins the expected behaviour for every future vehicle.
"""
from __future__ import annotations

import base64
import json
import math
import struct
import uuid
from pathlib import Path


class VehicleScene:
    """Ordered authoring buffer of articulated groups and primitive cubes.

    Call order is preserved all the way into the exports (groups become
    GLB nodes in authoring order; cubes keep authoring order inside each
    group), so generators must build from back to front, bottom to top.
    """

    def __init__(self, vehicle_slug: str):
        self.slug = vehicle_slug
        self.groups: list[dict] = []
        self.cubes: list[dict] = []

    # ------------------------------------------------------------ authoring
    def group(self, name, origin=(0, 0, 0), parent="tractor", rotation=(0, 0, 0)):
        self.groups.append(dict(name=name, origin=list(origin), parent=parent, rotation=list(rotation)))
        return name

    def box(self, name, bounds, material="enamel_dark", parent="tractor", rotation=(0, 0, 0), pivot=None):
        a, b = bounds[:3], bounds[3:]
        assert all(b[i] > a[i] for i in range(3)), name
        self.cubes.append(dict(name=name, group=parent, **{"from": list(a)}, to=list(b), material=material,
                               origin=list(pivot or [(a[i] + b[i]) / 2 for i in range(3)]), rotation=list(rotation)))

    def centered(self, name, center, size, material="enamel_dark", parent="tractor", rotation=(0, 0, 0), pivot=None):
        self.box(name, [center[i] - size[i] / 2 for i in range(3)] + [center[i] + size[i] / 2 for i in range(3)],
                 material, parent, rotation, pivot)

    def beam(self, name, start, end, thickness=.8, material="steel", parent="tractor"):
        # A box aligned with local Y, rotated in Blockbench's ZYX order.
        dx, dy, dz = [end[i] - start[i] for i in range(3)]
        length = math.sqrt(dx * dx + dy * dy + dz * dz)
        rx = math.degrees(math.asin(dz / length))
        rz = math.degrees(math.atan2(-dx, dy))
        self.centered(name, [(start[i] + end[i]) / 2 for i in range(3)], [thickness, length, thickness], material, parent, (rx, 0, rz))

    def disk(self, name, center, radius, width, material, parent, bands=10):
        # Stepped circular cross-section keeps the native Minecraft cuboid aesthetic.
        x, y, z = center
        for i in range(bands):
            y0 = -radius + i * radius * 2 / bands
            y1 = -radius + (i + 1) * radius * 2 / bands
            half = math.sqrt(max(0, radius * radius - ((y0 + y1) / 2) ** 2))
            self.box(f"{name}_{i:02}", [x - width / 2, y + y0, z - half, x + width / 2, y + y1, z + half], material, parent)

    def wheel(self, name, center, radius, width, parent="tractor", tread_count=16, full=True):
        """SNC 75 quality floor: voxel tire + sidewalls + rims + hubs + bolts + tread."""
        self.group(name, center, parent)
        x, y, z = center
        self.disk(name + "_tire", center, radius, width, "rubber", name, 10 if full else 6)
        for side in [-1, 1]:
            self.disk(name + f"_sidewall_{side}", (x + side * (width / 2 + .13), y, z), radius * .86, .4, "rubber", name, 8)
            self.disk(name + f"_rim_{side}", (x + side * (width / 2 + .38), y, z), radius * .50, .48, "rim", name, 6)
            self.disk(name + f"_hub_{side}", (x + side * (width / 2 + .70), y, z), radius * .22, .55, "enamel_dark", name, 4)
            if full:
                for bolt in range(6):
                    angle = bolt * math.tau / 6
                    self.centered(name + f"_bolt_{side}_{bolt}", (x + side * (width / 2 + 1), y + math.cos(angle) * radius * .32, z + math.sin(angle) * radius * .32), (.25, .6, .6), "steel", name)
        for i in range(tread_count):
            angle = i * 360 / tread_count
            if full:
                tread_group = self.group(name + f"_tread_pair_{i}", center, name, (angle, 0, 0))
                for side in [-1, 1]:
                    self.centered(name + f"_chevron_{i}_{side}", (x + side * width * .23, y + radius + .3, z),
                                  (width * .60, .95, 1.35), "tread", tread_group, (0, side * 26, 0))
            else:
                self.centered(name + f"_tread_{i}", (x, y + radius + .15, z), (width + .15, .55, 1), "tread", name, (angle, 0, 0), center)

    # ------------------------------------------------------------ checks
    def validate(self, materials):
        assert len(set(c["name"] for c in self.cubes)) == len(self.cubes)
        assert len(set(g["name"] for g in self.groups)) == len(self.groups)
        assert all(c["material"] in materials for c in self.cubes)


def stable_id(slug, name):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, "snc-energies/" + slug + "/" + name))


def write_json(path, data):
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


def export_bbmodel(scene: VehicleScene, path: Path, materials, name, model_identifier):
    textures = []
    for i, (key, mat) in enumerate(materials.items()):
        textures.append(dict(name=key + ".png", id=str(i), uuid=stable_id(scene.slug, "texture/" + key),
                             width=128, height=128, uv_width=128, uv_height=128,
                             mode="bitmap", internal=True, visible=True, saved=False,
                             source="data:image/png;base64," + base64.b64encode(Path(mat["file"]).read_bytes()).decode()))
    indices = {key: i for i, key in enumerate(materials)}
    elements = []
    for c in scene.cubes:
        elements.append(dict(name=c["name"], uuid=stable_id(scene.slug, c["name"]), type="cube",
                             **{"from": c["from"]}, to=c["to"], origin=c["origin"], rotation=c["rotation"],
                             box_uv=False, autouv=0, rescale=False, locked=False, visibility=True,
                             faces={face: dict(uv=[0, 0, 128, 128], texture=indices[c["material"]])
                                    for face in ["north", "south", "east", "west", "up", "down"]}))

    def outline(g):
        return dict(name=g["name"], uuid=stable_id(scene.slug, "group/" + g["name"]), origin=g["origin"], rotation=g["rotation"],
                    export=True, isOpen=False, visibility=True,
                    children=[stable_id(scene.slug, c["name"]) for c in scene.cubes if c["group"] == g["name"]]
                             + [outline(child) for child in scene.groups if child["parent"] == g["name"]])

    write_json(path, dict(meta=dict(format_version="4.10", model_format="free", box_uv=False),
               name=name, model_identifier=model_identifier,
               resolution=dict(width=128, height=128), elements=elements,
               outliner=[outline(g) for g in scene.groups if g["parent"] is None], textures=textures))


def quaternion(degrees):
    x, y, z = [math.radians(d) / 2 for d in degrees]
    a, b, c = math.cos(x), math.cos(y), math.cos(z)
    d, e, f = math.sin(x), math.sin(y), math.sin(z)
    return [d * b * c - a * e * f, a * e * c + d * b * f, a * b * f - d * e * c, a * b * c + d * e * f]


def export_glb(scene: VehicleScene, path: Path, materials, generator, root_name, animation_name="wheel_rotation_demo", spin_rule=None):
    """glTF 2.0 binary export with rest pose, pivots and a wheel-spin demo rig.

    spin_rule(group_name) selects which articulated groups receive the
    rotation demo; the SNC 75 default is every *wheel group except the
    steering wheel.
    """
    if spin_rule is None:
        spin_rule = lambda name: name.endswith("wheel") and name != "steering_wheel"
    data = bytearray()
    doc = dict(asset=dict(version="2.0", generator=generator),
               scene=0, scenes=[dict(nodes=[0])], nodes=[dict(name=root_name, scale=[1 / 16] * 3, children=[])],
               meshes=[], materials=[], textures=[], images=[], samplers=[dict(magFilter=9728, minFilter=9728, wrapS=10497, wrapT=10497)],
               bufferViews=[], accessors=[])

    def view(raw, target=None):
        while len(data) % 4:
            data.append(0)
        offset = len(data)
        data.extend(raw)
        item = dict(buffer=0, byteOffset=offset, byteLength=len(raw))
        if target:
            item["target"] = target
        doc["bufferViews"].append(item)
        return len(doc["bufferViews"]) - 1

    def accessor(values, kind, components=5126, target=None, bounds=False):
        count = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4}[kind]
        raw = struct.pack("<" + ("f" if components == 5126 else "H") * len(values), *values)
        item = dict(bufferView=view(raw, target), componentType=components, count=len(values) // count, type=kind)
        if bounds:
            item["min"] = [min(values[i::count]) for i in range(count)]
            item["max"] = [max(values[i::count]) for i in range(count)]
        doc["accessors"].append(item)
        return len(doc["accessors"]) - 1

    # Counterclockwise face winding, one UV square per face; identical to preview.
    pos = []
    normal = []
    uv = []
    indices = []
    faces = [([(1, -1, -1), (1, 1, -1), (1, 1, 1), (1, -1, 1)], (1, 0, 0)),
             ([(-1, -1, 1), (-1, 1, 1), (-1, 1, -1), (-1, -1, -1)], (-1, 0, 0)),
             ([(-1, 1, -1), (-1, 1, 1), (1, 1, 1), (1, 1, -1)], (0, 1, 0)),
             ([(-1, -1, 1), (-1, -1, -1), (1, -1, -1), (1, -1, 1)], (0, -1, 0)),
             ([(1, -1, 1), (1, 1, 1), (-1, 1, 1), (-1, -1, 1)], (0, 0, 1)),
             ([(-1, -1, -1), (-1, 1, -1), (1, 1, -1), (1, -1, -1)], (0, 0, -1))]
    for corners, n in faces:
        base = len(pos) // 3
        for p in corners:
            pos.extend(v * .5 for v in p)
            normal.extend(n)
        uv.extend([0, 1, 0, 0, 1, 0, 1, 1])
        indices.extend([base, base + 1, base + 2, base, base + 2, base + 3])
    attrs = dict(POSITION=accessor(pos, "VEC3", target=34962, bounds=True), NORMAL=accessor(normal, "VEC3", target=34962), TEXCOORD_0=accessor(uv, "VEC2", target=34962))
    idx = accessor(indices, "SCALAR", 5123, 34963)
    mat_indices = {}
    for i, (name, mat) in enumerate(materials.items()):
        doc["images"].append(dict(bufferView=view(Path(mat["file"]).read_bytes()), mimeType="image/png", name=name))
        doc["textures"].append(dict(sampler=0, source=i))
        doc["materials"].append(dict(name=name, pbrMetallicRoughness=dict(baseColorTexture=dict(index=i), metallicFactor=mat["metalness"], roughnessFactor=mat["roughness"])))
        doc["meshes"].append(dict(name=name + "_cube", primitives=[dict(attributes=attrs, indices=idx, material=i)]))
        mat_indices[name] = i
    group_indices = {}
    origins = {g["name"]: g["origin"] for g in scene.groups}
    for g in scene.groups:
        parent = origins[g["parent"]] if g["parent"] else [0, 0, 0]
        node = dict(name=g["name"], translation=[g["origin"][i] - parent[i] for i in range(3)], rotation=quaternion(g["rotation"]), children=[])
        group_indices[g["name"]] = len(doc["nodes"])
        doc["nodes"].append(node)
    for g in scene.groups:
        pi = group_indices[g["parent"]] if g["parent"] else 0
        doc["nodes"][pi]["children"].append(group_indices[g["name"]])
    for c in scene.cubes:
        parent_idx = group_indices[c["group"]]
        origin = origins[c["group"]]
        if any(c["rotation"]):
            pivot = dict(name=c["name"] + "_pivot", translation=[c["origin"][i] - origin[i] for i in range(3)], rotation=quaternion(c["rotation"]), children=[])
            doc["nodes"][parent_idx]["children"].append(len(doc["nodes"]))
            parent_idx = len(doc["nodes"])
            doc["nodes"].append(pivot)
            origin = c["origin"]
        node = dict(name=c["name"], mesh=mat_indices[c["material"]], translation=[(c["from"][i] + c["to"][i]) / 2 - origin[i] for i in range(3)], scale=[c["to"][i] - c["from"][i] for i in range(3)])
        doc["nodes"][parent_idx]["children"].append(len(doc["nodes"]))
        doc["nodes"].append(node)
    time_idx = accessor([0, .5, 1, 1.5, 2], "SCALAR", bounds=True)
    animation = dict(name=animation_name, samplers=[], channels=[])
    for g in scene.groups:
        if spin_rule(g["name"]):
            rotations = [v for angle in [0, 90, 180, 270, 360] for v in quaternion((angle, 0, 0))]
            samp = len(animation["samplers"])
            animation["samplers"].append(dict(input=time_idx, output=accessor(rotations, "VEC4"), interpolation="LINEAR"))
            animation["channels"].append(dict(sampler=samp, target=dict(node=group_indices[g["name"]], path="rotation")))
    if animation["channels"]:
        doc["animations"] = [animation]
    while len(data) % 4:
        data.append(0)
    doc["buffers"] = [dict(byteLength=len(data))]
    js = json.dumps(doc, separators=(",", ":")).encode()
    while len(js) % 4:
        js += b" "
    glb = struct.pack("<III", 0x46546C67, 2, 12 + 8 + len(js) + 8 + len(data)) + struct.pack("<II", len(js), 0x4E4F534A) + js + struct.pack("<II", len(data), 0x004E4942) + data
    path.write_bytes(glb)


def build_model(name, scene: VehicleScene, locators=None, animation_axes=None, status="modelling_preview", units_per_block=16, forward_axis="-Z"):
    return dict(name=name, units_per_block=units_per_block, status=status, forward_axis=forward_axis,
                groups=scene.groups, cubes=scene.cubes,
                locators=locators or {}, animation_axes=animation_axes or {})
