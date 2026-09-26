"""Validate actual tractor exports, including binary data and articulated geometry.

Run with .venv-textures/Scripts/python.exe tools/verify_tractor.py.
This checks the modelling assets only; it does not test Minecraft gameplay.
"""
from __future__ import annotations

import base64
import hashlib
import io
import itertools
import json
import math
import struct
import uuid
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets/tractor"
REPORT = ROOT / "verification/tractor-model-validation.json"
RESULT = {"status": "pending", "scope": "JSON, Blockbench and GLB modelling exports; no gameplay validation", "checks": [], "errors": [], "warnings": []}


def check(condition, message):
    if not condition:
        raise AssertionError(message)


def completed(name, **detail):
    RESULT["checks"].append({"name": name, **detail})


def translation(values):
    matrix = np.eye(4)
    matrix[:3, 3] = values
    return matrix


def scale(values):
    return np.diag([*values, 1.0])


def rotation(degrees):
    """Independent matrix construction for Blockbench's ZYX Euler convention."""
    x, y, z = np.radians(degrees)
    cx, cy, cz = np.cos([x, y, z])
    sx, sy, sz = np.sin([x, y, z])
    rx = np.array([[1, 0, 0, 0], [0, cx, -sx, 0], [0, sx, cx, 0], [0, 0, 0, 1.]])
    ry = np.array([[cy, 0, sy, 0], [0, 1, 0, 0], [-sy, 0, cy, 0], [0, 0, 0, 1.]])
    rz = np.array([[cz, -sz, 0, 0], [sz, cz, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1.]])
    return rz @ ry @ rx


def quaternion_matrix(values):
    x, y, z, w = values
    check(abs(sum(v * v for v in values) - 1) < 1e-6, "Non-unit GLB quaternion")
    matrix = np.eye(4)
    matrix[:3, :3] = [[1-2*y*y-2*z*z, 2*x*y-2*z*w, 2*x*z+2*y*w],
                      [2*x*y+2*z*w, 1-2*x*x-2*z*z, 2*y*z-2*x*w],
                      [2*x*z-2*y*w, 2*y*z+2*x*w, 1-2*x*x-2*y*y]]
    return matrix


def read_glb(path):
    blob = path.read_bytes()
    magic, version, length = struct.unpack_from("<III", blob)
    check(magic == 0x46546C67 and version == 2 and length == len(blob), "Invalid GLB header/length")
    chunks = []
    cursor = 12
    while cursor < len(blob):
        size, kind = struct.unpack_from("<II", blob, cursor)
        check(size % 4 == 0 and cursor + 8 + size <= len(blob), "Invalid GLB chunk alignment/length")
        chunks.append((kind, blob[cursor+8:cursor+8+size]))
        cursor += 8 + size
    check(cursor == len(blob), "Trailing GLB bytes")
    check([x[0] for x in chunks] == [0x4E4F534A, 0x004E4942], "Expected JSON then BIN chunks")
    doc = json.loads(chunks[0][1])
    binary = chunks[1][1]
    check(doc["asset"]["version"] == "2.0", "GLB asset version")
    check(len(doc["buffers"]) == 1 and doc["buffers"][0]["byteLength"] == len(binary), "GLB buffer length")
    for view in doc["bufferViews"]:
        offset, count = view.get("byteOffset", 0), view["byteLength"]
        check(view["buffer"] == 0 and offset >= 0 and count > 0 and offset + count <= len(binary), "Buffer view exceeds binary")
        check(offset % 4 == 0, "Unaligned buffer view")
    accessors = []
    components = {5120: "b", 5121: "B", 5122: "h", 5123: "H", 5125: "I", 5126: "f"}
    widths = {"SCALAR": 1, "VEC2": 2, "VEC3": 3, "VEC4": 4, "MAT4": 16}
    for accessor in doc["accessors"]:
        check("sparse" not in accessor, "Unexpected sparse accessor")
        view = doc["bufferViews"][accessor["bufferView"]]
        component = components[accessor["componentType"]]
        component_size = struct.calcsize("<" + component)
        width = widths[accessor["type"]]
        item_size = component_size * width
        stride = view.get("byteStride", item_size)
        offset = accessor.get("byteOffset", 0)
        check(offset % component_size == 0 and stride >= item_size, "Accessor alignment/stride")
        check(offset + (accessor["count"]-1)*stride + item_size <= view["byteLength"], "Accessor exceeds view")
        rows = [struct.unpack_from("<" + component*width, binary, view.get("byteOffset", 0)+offset+i*stride)
                for i in range(accessor["count"])]
        values = np.array(rows)
        check(np.isfinite(values).all(), "Non-finite accessor data")
        if "min" in accessor:
            check(np.allclose(values.min(axis=0), accessor["min"], atol=1e-6), "Accessor min mismatch")
        if "max" in accessor:
            check(np.allclose(values.max(axis=0), accessor["max"], atol=1e-6), "Accessor max mismatch")
        accessors.append(values)
    completed("glb_container_and_binary_accessors", byte_length=len(blob), buffer_views=len(doc["bufferViews"]), accessors=len(accessors))
    return doc, binary, accessors


def authored_transforms(model, overrides=None):
    group_map = {g["name"]: g for g in model["groups"]}
    matrices = {}
    pending = set()
    overrides = overrides or {}

    def group_matrix(name):
        if name in matrices:
            return matrices[name]
        check(name not in pending, "Cyclic authored hierarchy")
        pending.add(name)
        g = group_map[name]
        parent = g["parent"]
        parent_origin = group_map[parent]["origin"] if parent else [0, 0, 0]
        local = translation(np.array(g["origin"])-parent_origin) @ rotation(overrides.get(name, g["rotation"]))
        matrices[name] = group_matrix(parent) @ local if parent else local
        pending.remove(name)
        return matrices[name]

    for name in group_map:
        group_matrix(name)
    cube_matrices = {}
    for cube in model["cubes"]:
        center = (np.array(cube["from"])+cube["to"])/2
        size = np.array(cube["to"])-cube["from"]
        cube_matrices[cube["name"]] = (matrices[cube["group"]]
            @ translation(np.array(cube["origin"])-group_map[cube["group"]]["origin"])
            @ rotation(cube["rotation"])
            @ translation(center-cube["origin"]) @ scale(size))
    return matrices, cube_matrices


def glb_transforms(doc):
    matrices, parents = {}, {}

    def visit(index, parent_matrix):
        check(index not in matrices, "GLB graph cycle or multiple parents")
        node = doc["nodes"][index]
        local = (translation(node.get("translation", [0, 0, 0]))
                 @ quaternion_matrix(node.get("rotation", [0, 0, 0, 1]))
                 @ scale(node.get("scale", [1, 1, 1])))
        matrices[index] = parent_matrix @ local
        for child in node.get("children", []):
            check(0 <= child < len(doc["nodes"]), "Invalid child node")
            parents[child] = index
            visit(child, matrices[index])

    for root in doc["scenes"][doc["scene"]]["nodes"]:
        visit(root, np.eye(4))
    check(len(matrices) == len(doc["nodes"]), "Unreachable GLB node")
    return matrices, parents


def obb(matrix):
    axes = matrix[:3, :3].copy()
    extents = np.linalg.norm(axes, axis=0)/2
    axes /= extents * 2
    return matrix[:3, 3], axes, extents


def overlap(a, b, tolerance=.025):
    """Separating-axis test. Return penetration depth; touching faces are ignored."""
    ca, aa, ea = a
    cb, ab, eb = b
    delta = cb-ca
    candidates = [aa[:, i] for i in range(3)] + [ab[:, i] for i in range(3)]
    candidates += [np.cross(aa[:, i], ab[:, j]) for i in range(3) for j in range(3)]
    depth = float("inf")
    for axis in candidates:
        length = np.linalg.norm(axis)
        if length < 1e-8:
            continue
        axis = axis/length
        distance = abs(np.dot(delta, axis))
        limit = np.dot(ea, np.abs(aa.T @ axis)) + np.dot(eb, np.abs(ab.T @ axis))
        penetration = limit-distance
        if penetration <= tolerance:
            return 0.0
        depth = min(depth, penetration)
    return depth


def articulation_checks(model):
    cubes = {c["name"]: c for c in model["cubes"]}
    groups = {g["name"]: g for g in model["groups"]}

    def belongs(cube, ancestor):
        name = cube["group"]
        while name:
            if name == ancestor:
                return True
            name = groups[name]["parent"]
        return False

    scenarios = []
    for angle in [0, 14, 28, -28]:
        fixed = [c for c in cubes.values() if c["name"] in {"main_chassis", "engine_sump", "bumper", "engine_block"}]
        moving = [c for c in cubes.values() if belongs(c, "front_left_steering") or belongs(c, "front_right_steering")]
        scenarios.append((f"front_steering_{angle}_degrees", {"front_left_steering": [0, angle, 0], "front_right_steering": [0, angle, 0]}, moving, fixed))
    for angle in [0, 15, 30, 55]:
        moving = [c for c in cubes.values() if belongs(c, "hood")]
        fixed = [c for c in cubes.values() if c["name"].startswith(("engine_", "cylinder_head_", "exhaust_", "air_filter", "dashboard_"))]
        scenarios.append((f"hood_open_{angle}_degrees", {"hood": [angle, 0, 0]}, moving, fixed))
    for angle in [0, -12, -23]:
        moving = [c for c in cubes.values() if belongs(c, "planter")]
        fixed = [c for c in cubes.values() if c["name"].startswith(("fender_", "rear_light", "turn_light", "rollbar_", "seat_", "operator_floor"))
                 or belongs(c, "rear_left_wheel") or belongs(c, "rear_right_wheel")]
        scenarios.append((f"planter_lift_{-angle}_degrees", {"planter": [angle, 0, 0]}, moving, fixed))
    findings = []
    for name, overrides, moving, fixed in scenarios:
        _, matrices = authored_transforms(model, overrides)
        collisions = []
        boxes = {key: obb(matrix) for key, matrix in matrices.items()}
        for a in moving:
            for b in fixed:
                depth = overlap(boxes[a["name"]], boxes[b["name"]])
                if depth:
                    collisions.append({"moving": a["name"], "fixed": b["name"], "penetration_model_pixels": round(depth, 4)})
        findings.append({"scenario": name, "tested_pairs": len(moving)*len(fixed), "collisions": collisions})
    RESULT["articulation_clearance"] = findings
    impacted = [item for item in findings if item["collisions"]]
    if impacted:
        RESULT["warnings"].append({"type": "articulated_geometry_intersections", "scenarios": [item["scenario"] for item in impacted],
                                   "note": "Geometric intersections need visual review; attached or enclosed mechanical parts may intentionally overlap."})
    completed("articulated_geometry_clearance", scenarios=len(findings), pairs=sum(f["tested_pairs"] for f in findings), scenarios_with_intersections=len(impacted))


def validate():
    model = json.loads((ASSETS / "tractor-model.json").read_text(encoding="utf-8"))
    bb = json.loads((ASSETS / "snc-75-trator.bbmodel").read_text(encoding="utf-8"))
    materials = json.loads((ASSETS / "materials.json").read_text(encoding="utf-8"))
    doc, binary, accessors = read_glb(ASSETS / "snc-75-trator.glb")
    groups = {g["name"]: g for g in model["groups"]}
    cubes = {c["name"]: c for c in model["cubes"]}
    check(len(groups) == len(model["groups"]) and len(cubes) == len(model["cubes"]), "Duplicate authored name")
    check(all(g["parent"] is None or g["parent"] in groups for g in groups.values()), "Missing group parent")
    for cube in cubes.values():
        check(cube["group"] in groups and cube["material"] in materials, "Missing cube group/material")
        check(np.all(np.array(cube["to"]) > cube["from"]), "Nonpositive cube dimension")
    check(bb["meta"] == {"format_version": "4.10", "model_format": "free", "box_uv": False}, "Unexpected BB format")
    check(bb["resolution"] == {"width": 128, "height": 128}, "Unexpected BB resolution")
    bb_cubes = {c["uuid"]: c for c in bb["elements"]}
    check(len(bb_cubes) == len(cubes), "Blockbench cube count/UUID mismatch")
    bb_groups, membership, all_ids = {}, {}, set()

    def scan_outline(items, parent=None):
        for item in items:
            if isinstance(item, str):
                check(item in bb_cubes and item not in membership, "Unknown/duplicated outline cube")
                membership[item] = parent
            else:
                check(item["name"] not in bb_groups, "Duplicate BB group name")
                bb_groups[item["name"]] = dict(item, parent=parent)
                check(item["uuid"] not in all_ids, "Duplicate BB UUID")
                uuid.UUID(item["uuid"])
                all_ids.add(item["uuid"])
                scan_outline(item["children"], item["name"])
    scan_outline(bb["outliner"])
    check(set(bb_groups) == set(groups) and set(membership) == set(bb_cubes), "Incomplete BB outliner")
    for name, group in groups.items():
        for field in ["origin", "rotation", "parent"]:
            check(bb_groups[name][field] == group[field], f"BB group {name} {field} mismatch")
    for cube in bb_cubes.values():
        uuid.UUID(cube["uuid"])
        check(cube["uuid"] not in all_ids, "Duplicate element/group UUID")
        all_ids.add(cube["uuid"])
        original = cubes[cube["name"]]
        check(cube["type"] == "cube" and not cube["box_uv"], "Unexpected BB cube format")
        check(membership[cube["uuid"]] == original["group"], "BB parent mismatch")
        for field in ["from", "to", "origin", "rotation"]:
            check(cube[field] == original[field], f"BB cube {cube['name']} {field} mismatch")
        check(set(cube["faces"]) == {"north", "south", "east", "west", "up", "down"}, "Missing BB cube face")
        for face in cube["faces"].values():
            check(face["uv"] == [0, 0, 128, 128], "Unexpected BB face UV")
            check(isinstance(face["texture"], int) and 0 <= face["texture"] < len(bb["textures"]), "Invalid BB texture index")
            check(bb["textures"][face["texture"]]["name"] == original["material"]+".png", "BB material mismatch")
    completed("blockbench_hierarchy_pivots_material_assignments", cubes=len(cubes), groups=len(groups), unique_uuids=len(all_ids))
    check(len(materials) == len(bb["textures"]) == len(doc["materials"]), "Material count mismatch")
    for index, (name, material) in enumerate(materials.items()):
        raw = (ASSETS / material["file"]).read_bytes()
        texture = bb["textures"][index]
        check(base64.b64decode(texture["source"].split(",", 1)[1]) == raw, "BB bitmap differs from source")
        check(texture["internal"] and texture["uv_width"] == texture["uv_height"] == 128, "Invalid BB bitmap settings")
        with Image.open(io.BytesIO(raw)) as image:
            check(image.size == (128, 128), "Texture is not 128 by 128")
            image.verify()
        glb_material = doc["materials"][index]
        check(glb_material["name"] == name, "GLB material ordering")
        pbr = glb_material["pbrMetallicRoughness"]
        check(pbr["metallicFactor"] == material["metalness"] and pbr["roughnessFactor"] == material["roughness"], "GLB PBR mismatch")
        texture_index = pbr["baseColorTexture"]["index"]
        image = doc["images"][doc["textures"][texture_index]["source"]]
        view = doc["bufferViews"][image["bufferView"]]
        check(binary[view["byteOffset"]:view["byteOffset"]+view["byteLength"]] == raw, "GLB bitmap differs from source")
    completed("textures_embedded_identically", textures=len(materials), size="128x128")
    for mesh in doc["meshes"]:
        for primitive in mesh["primitives"]:
            pos = accessors[primitive["attributes"]["POSITION"]]
            normal = accessors[primitive["attributes"]["NORMAL"]]
            uv = accessors[primitive["attributes"]["TEXCOORD_0"]]
            idx = accessors[primitive["indices"]].flatten().astype(int)
            check(pos.shape == normal.shape == (24, 3) and uv.shape == (24, 2), "Invalid cube vertex attributes")
            check(idx.size == 36 and idx.min() >= 0 and idx.max() < len(pos), "Invalid triangle indices")
            check(np.all((uv >= 0) & (uv <= 1)), "UV outside full texture square")
            check(np.allclose(np.linalg.norm(normal, axis=1), 1), "Non-unit normals")
            for triangle in idx.reshape(-1, 3):
                a, b, c = pos[triangle]
                cross = np.cross(b-a, c-a)
                check(np.dot(cross, normal[triangle[0]]) > 0, "Inward or degenerate GLB face")
    completed("glb_mesh_winding_normals_uv_indices", meshes=len(doc["meshes"]))
    authored_groups, authored_cubes = authored_transforms(model)
    glb_matrices, glb_parents = glb_transforms(doc)
    glb_names = {node["name"]: index for index, node in enumerate(doc["nodes"])}
    check(len(glb_names) == len(doc["nodes"]), "Duplicate GLB node name")
    corners = np.array([[*coords, 1] for coords in itertools.product([-.5, .5], repeat=3)]).T
    max_delta = 0.0
    for name, cube in cubes.items():
        index = glb_names[name]
        node = doc["nodes"][index]
        expected = authored_cubes[name] @ corners
        actual = glb_matrices[index] @ corners
        actual[:3] *= model["units_per_block"]
        delta = float(np.max(np.abs(expected-actual)))
        max_delta = max(max_delta, delta)
        check(delta < 1e-6, f"GLB world geometry differs for {name}: {delta}")
        primitive = doc["meshes"][node["mesh"]]["primitives"][0]
        check(doc["materials"][primitive["material"]]["name"] == cube["material"], "GLB cube material mismatch")
    for name, expected in authored_groups.items():
        actual = glb_matrices[glb_names[name]].copy()
        actual[:3] *= model["units_per_block"]
        check(np.allclose(expected, actual, atol=1e-7), f"GLB group pivot/rotation mismatch: {name}")
    completed("world_geometry_and_rig_match_json_bbmodel_glb", cube_corners=len(cubes)*8, maximum_delta_model_pixels=max_delta)
    animated = []
    for animation in doc.get("animations", []):
        for channel in animation["channels"]:
            sampler = animation["samplers"][channel["sampler"]]
            times = accessors[sampler["input"]].flatten()
            outputs = accessors[sampler["output"]]
            check(np.all(np.diff(times) > 0) and len(times) == len(outputs), "Invalid animation keyframe timing")
            check(channel["target"]["path"] == "rotation" and outputs.shape[1] == 4, "Invalid rotation animation")
            check(np.allclose(np.linalg.norm(outputs, axis=1), 1, atol=1e-6), "Animation quaternion not normalized")
            animated.append(doc["nodes"][channel["target"]["node"]]["name"])
    expected_wheels = {name for name in groups if name.endswith("wheel") and name != "steering_wheel"}
    check(set(animated) == expected_wheels and len(animated) == len(expected_wheels), "Missing/duplicated wheel animation channel")
    completed("glb_wheel_animation", channels=len(animated), targets=animated)
    articulation_checks(model)
    RESULT["files"] = {path.name: {"bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
                       for path in [ASSETS/"tractor-model.json", ASSETS/"snc-75-trator.bbmodel", ASSETS/"snc-75-trator.glb"]}


def main():
    try:
        validate()
    except Exception as error:
        RESULT["errors"].append(f"{type(error).__name__}: {error}")
    RESULT["status"] = "failed" if RESULT["errors"] else "passed_with_warnings" if RESULT["warnings"] else "passed"
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(RESULT, indent=2, ensure_ascii=False)+"\n", encoding="utf-8")
    print(json.dumps({"status": RESULT["status"], "checks": len(RESULT["checks"]), "errors": RESULT["errors"], "warnings": RESULT["warnings"]}, ensure_ascii=False))
    for scenario in RESULT.get("articulation_clearance", []):
        if scenario["collisions"]:
            print(scenario["scenario"], len(scenario["collisions"]), "intersections", scenario["collisions"][:3])
    raise SystemExit(1 if RESULT["errors"] else 0)


if __name__ == "__main__":
    main()
