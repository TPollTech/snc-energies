"""Validate the actual SNC 90 harvester exports, including binary data and articulated geometry.

Run with .venv-textures/Scripts/python.exe tools/verify_harvester.py.
This checks the modelling assets only; it does not test Minecraft gameplay.
Uses the shared verification toolkit extracted from verify_tractor.py so the
harvester is held to exactly the SNC 75 structural standard.
"""
from __future__ import annotations

import base64
import hashlib
import io
import json
import uuid
from pathlib import Path

import numpy as np
from PIL import Image

from vehicle_verify_library import (articulation_findings, belongs, check, check_world_geometry_matches,
                                    glb_transforms, read_glb)

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "assets/harvester"
REPORT = ROOT / "verification/harvester-model-validation.json"
RESULT = {"status": "pending", "scope": "JSON, Blockbench and GLB modelling exports; no gameplay validation", "checks": [], "errors": [], "warnings": []}


def completed(name, **detail):
    RESULT["checks"].append({"name": name, **detail})


def spin_target(name):
    return (name.endswith("wheel") and name != "steering_wheel") or name in {"reel", "cooling_fan", "cleaning_fan", "straw_chopper"}


def articulation_checks(model):
    groups = {g["name"]: g for g in model["groups"]}
    engine_fixed = lambda c: c["name"].startswith(("engine_", "radiator_", "fan_belt_", "battery", "hydraulic_tank", "air_filter", "exhaust_", "cooling_fan_"))
    scenarios = []
    for angle in [0, 15, 30, 55]:
        scenarios.append((f"hood_open_{angle}_degrees", {"hood": [angle, 0, 0]},
                          lambda c: belongs(c, "hood", groups), engine_fixed))
    for angle in [0, 8, 16, 26]:
        scenarios.append((f"header_lift_{angle}_degrees", {"header": [angle, 0, 0]},
                          lambda c: belongs(c, "header", groups),
                          lambda c: c["name"].startswith(("main_chassis", "frame_rail", "feeder_", "stone_trap")) or c["group"] in {"front_left_wheel", "front_right_wheel"}))
    for angle in [0, 18, 38]:
        scenarios.append((f"auger_raise_{angle}_degrees", {"unloading_auger": [-angle, 0, 0]},
                          lambda c: belongs(c, "unloading_auger", groups),
                          lambda c: c["name"].startswith(("tank_left", "tank_front", "tank_rear", "cab_", "chopper_housing", "exhaust_")) or c["group"] == "rear_left_wheel"))
    for angle in [0, 70, 140]:
        # Stowing nests the spout against the outer tube on purpose; only tank walls are fixed here.
        scenarios.append((f"spout_fold_{angle}_degrees", {"spout": [angle, 0, 0]},
                          lambda c: belongs(c, "spout", groups),
                          lambda c: c["name"].startswith("tank_")))
    for angle in [0, 22.5, 45]:
        scenarios.append((f"rotor_spin_{angle}_degrees", {"rotor": [0, 0, angle]},
                          lambda c: belongs(c, "rotor", groups),
                          lambda c: c["name"].startswith(("rotor_housing", "concave_"))))
    findings = articulation_findings(model, scenarios)
    RESULT["articulation_clearance"] = findings
    impacted = [item for item in findings if item["collisions"]]
    if impacted:
        RESULT["warnings"].append({"type": "articulated_geometry_intersections", "scenarios": [item["scenario"] for item in impacted],
                                   "note": "Geometric intersections need visual review; attached or enclosed mechanical parts may intentionally overlap."})
    completed("articulated_geometry_clearance", scenarios=len(findings), pairs=sum(f["tested_pairs"] for f in findings), scenarios_with_intersections=len(impacted))


def validate():
    model = json.loads((ASSETS / "harvester-model.json").read_text(encoding="utf-8"))
    bb = json.loads((ASSETS / "snc-90-colheitadeira.bbmodel").read_text(encoding="utf-8"))
    materials = json.loads((ASSETS / "materials.json").read_text(encoding="utf-8"))
    doc, binary, accessors = read_glb(ASSETS / "snc-90-colheitadeira.glb")
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
            check(bb["textures"][face["texture"]]["name"] == original["material"] + ".png", "BB material mismatch")
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
        image = doc["images"][doc["textures"][pbr["baseColorTexture"]["index"]]["source"]]
        view = doc["bufferViews"][image["bufferView"]]
        check(binary[view["byteOffset"]:view["byteOffset"] + view["byteLength"]] == raw, "GLB bitmap differs from source")
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
                cross = np.cross(b - a, c - a)
                check(np.dot(cross, normal[triangle[0]]) > 0, "Inward or degenerate GLB face")
    completed("glb_mesh_winding_normals_uv_indices", meshes=len(doc["meshes"]))
    glb_matrices, _ = glb_transforms(doc)
    glb_names = {node["name"]: index for index, node in enumerate(doc["nodes"])}
    check(len(glb_names) == len(doc["nodes"]), "Duplicate GLB node name")
    max_delta = check_world_geometry_matches(model, doc, glb_matrices, glb_names)
    completed("world_geometry_and_rig_match_json_glb", cube_corners=len(cubes) * 8, maximum_delta_model_pixels=max_delta)
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
    expected_spin = {name for name in groups if spin_target(name)}
    check(set(animated) == expected_spin and len(animated) == len(expected_spin), "Missing/duplicated mechanism animation channel")
    completed("glb_wheel_and_mechanism_animation", channels=len(animated), targets=sorted(animated))
    articulation_checks(model)
    RESULT["files"] = {path.name: {"bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()}
                       for path in [ASSETS / "harvester-model.json", ASSETS / "snc-90-colheitadeira.bbmodel", ASSETS / "snc-90-colheitadeira.glb"]}


def main():
    try:
        validate()
    except Exception as error:
        RESULT["errors"].append(f"{type(error).__name__}: {error}")
    RESULT["status"] = "failed" if RESULT["errors"] else "passed_with_warnings" if RESULT["warnings"] else "passed"
    REPORT.parent.mkdir(parents=True, exist_ok=True)
    REPORT.write_text(json.dumps(RESULT, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    print(json.dumps({"status": RESULT["status"], "checks": len(RESULT["checks"]), "errors": RESULT["errors"], "warnings": RESULT["warnings"]}, ensure_ascii=False))
    for scenario in RESULT.get("articulation_clearance", []):
        if scenario["collisions"]:
            print(scenario["scenario"], len(scenario["collisions"]), "intersections", scenario["collisions"][:3])
    raise SystemExit(1 if RESULT["errors"] else 0)


if __name__ == "__main__":
    main()
