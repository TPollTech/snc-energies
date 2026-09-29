"""Shared vehicle verification toolkit for SNC Energies.

Extracted from tools/verify_tractor.py (SNC 75 golden reference): real GLB
binary validation, mathematical JSON<->GLB rig/geometry comparison and
separating-axis articulation clearance tests. Every vehicle verifier uses
these helpers so all vehicles are held to the same structural standard.
"""
from __future__ import annotations

import itertools
import json
import struct
from pathlib import Path

import numpy as np


def check(condition, message):
    if not condition:
        raise AssertionError(message)


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
    matrix[:3, :3] = [[1 - 2 * y * y - 2 * z * z, 2 * x * y - 2 * z * w, 2 * x * z + 2 * y * w],
                      [2 * x * y + 2 * z * w, 1 - 2 * x * x - 2 * z * z, 2 * y * z - 2 * x * w],
                      [2 * x * z - 2 * y * w, 2 * y * z + 2 * x * w, 1 - 2 * x * x - 2 * y * y]]
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
        chunks.append((kind, blob[cursor + 8:cursor + 8 + size]))
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
        check(offset + (accessor["count"] - 1) * stride + item_size <= view["byteLength"], "Accessor exceeds view")
        rows = [struct.unpack_from("<" + component * width, binary, view.get("byteOffset", 0) + offset + i * stride)
                for i in range(accessor["count"])]
        values = np.array(rows)
        check(np.isfinite(values).all(), "Non-finite accessor data")
        if "min" in accessor:
            check(np.allclose(values.min(axis=0), accessor["min"], atol=1e-6), "Accessor min mismatch")
        if "max" in accessor:
            check(np.allclose(values.max(axis=0), accessor["max"], atol=1e-6), "Accessor max mismatch")
        accessors.append(values)
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
        local = translation(np.array(g["origin"]) - parent_origin) @ rotation(overrides.get(name, g["rotation"]))
        matrices[name] = group_matrix(parent) @ local if parent else local
        pending.remove(name)
        return matrices[name]

    for name in group_map:
        group_matrix(name)
    cube_matrices = {}
    for cube in model["cubes"]:
        center = (np.array(cube["from"]) + cube["to"]) / 2
        size = np.array(cube["to"]) - cube["from"]
        cube_matrices[cube["name"]] = (matrices[cube["group"]]
            @ translation(np.array(cube["origin"]) - group_map[cube["group"]]["origin"])
            @ rotation(cube["rotation"])
            @ translation(center - cube["origin"]) @ scale(size))
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
    extents = np.linalg.norm(axes, axis=0) / 2
    axes /= extents * 2
    return matrix[:3, 3], axes, extents


def overlap(a, b, tolerance=.025):
    """Separating-axis test. Return penetration depth; touching faces are ignored."""
    ca, aa, ea = a
    cb, ab, eb = b
    delta = cb - ca
    candidates = [aa[:, i] for i in range(3)] + [ab[:, i] for i in range(3)]
    candidates += [np.cross(aa[:, i], ab[:, j]) for i in range(3) for j in range(3)]
    depth = float("inf")
    for axis in candidates:
        length = np.linalg.norm(axis)
        if length < 1e-8:
            continue
        axis = axis / length
        distance = abs(np.dot(delta, axis))
        limit = np.dot(ea, np.abs(aa.T @ axis)) + np.dot(eb, np.abs(ab.T @ axis))
        penetration = limit - distance
        if penetration <= tolerance:
            return 0.0
        depth = min(depth, penetration)
    return depth


def belongs(cube, ancestor, groups):
    name = cube["group"]
    while name:
        if name == ancestor:
            return True
        name = groups[name]["parent"]
    return False


def articulation_findings(model, scenarios):
    """Run separating-axis clearance checks for (name, overrides, moving, fixed) specs.

    moving/fixed are predicates over cube dicts; overrides map group names to
    alternate ZYX rotations simulating the articulation.
    """
    groups = {g["name"]: g for g in model["groups"]}
    cubes = {c["name"]: c for c in model["cubes"]}
    findings = []
    for name, overrides, moving, fixed in scenarios:
        _, matrices = authored_transforms(model, overrides)
        boxes = {key: obb(matrix) for key, matrix in matrices.items()}
        moving_cubes = [c for c in cubes.values() if moving(c)]
        fixed_cubes = [c for c in cubes.values() if fixed(c)]
        collisions = []
        for a in moving_cubes:
            for b in fixed_cubes:
                depth = overlap(boxes[a["name"]], boxes[b["name"]])
                if depth:
                    collisions.append({"moving": a["name"], "fixed": b["name"], "penetration_model_pixels": round(depth, 4)})
        findings.append({"scenario": name, "tested_pairs": len(moving_cubes) * len(fixed_cubes), "collisions": collisions})
    return findings


def corners():
    return np.array([[*coords, 1] for coords in itertools.product([-.5, .5], repeat=3)]).T


def check_world_geometry_matches(model, doc, glb_matrices, glb_names):
    """Compare authored cube corners against GLB world geometry (scaled 1/16)."""
    authored_groups, authored_cubes = authored_transforms(model)
    units = model["units_per_block"]
    max_delta = 0.0
    for name, cube in {c["name"]: c for c in model["cubes"]}.items():
        index = glb_names[name]
        node = doc["nodes"][index]
        expected = authored_cubes[name] @ corners()
        actual = glb_matrices[index] @ corners()
        actual[:3] *= units
        delta = float(np.max(np.abs(expected - actual)))
        max_delta = max(max_delta, delta)
        check(delta < 1e-6, f"GLB world geometry differs for {name}: {delta}")
        check(doc["materials"][doc["meshes"][node["mesh"]]["primitives"][0]["material"]]["name"] == cube["material"], "GLB cube material mismatch")
    for name, expected in authored_groups.items():
        actual = glb_matrices[glb_names[name]].copy()
        actual[:3] *= units
        check(np.allclose(expected, actual, atol=1e-7), f"GLB group pivot/rotation mismatch: {name}")
    return max_delta
