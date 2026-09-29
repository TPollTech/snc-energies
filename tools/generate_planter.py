"""Author the SNC 75-P detachable three-row planter as its own golden asset.

The planter becomes a vehicle of its own: the SNC 75 golden tractor keeps its
approved geometry untouched (AGENTS.md rules 6 and 7), so this standalone
implement re-authors the same three-row planter through the shared vehicle
toolkit, adds its own transport frame with the lower-link hitch that couples
to the tractor's rear three-point hitch, plus ID tags, PTO shaft and safety
chain. It carries every functional part: three seed tanks, metering housings,
double-disc openers, gauge/press wheels and side transport wheels.

Coordinates are Minecraft pixels: +Y up, -Z forward, 16 units per block.
All authoring goes through tools/vehicle_library.py (SNC 75 conventions:
Blockbench ZYX Euler rotations, absolute rest-pose origins, canonical JSON +
Blockbench + glTF 2 GLB exports).
"""
from __future__ import annotations

import json
from pathlib import Path

from vehicle_library import VehicleScene, build_model, export_bbmodel, export_glb, write_json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets/planter"
OUT.mkdir(parents=True, exist_ok=True)
scene = VehicleScene("planter")
group = scene.group
box = scene.box
centered = scene.centered
beam = scene.beam
disk = scene.disk
wheel = scene.wheel
P = "planter"


def _group(name, origin=(0, 0, 0), parent=P, rotation=(0, 0, 0)):
    # Root default is this vehicle instead of the library's "tractor" default.
    return scene.group(name, origin, parent, rotation)


group = _group


def author():
    # The lift pivot sits at the lower-link hitch, so the implement rotates
    # about the coupling point exactly like the golden tractor's planter group.
    group("planter", (0, 12, 27), parent=None)
    # Own transport frame: the frame box, wheels and hitches stay on the
    # implement when it is detached from the tractor.
    box("planter_frame", [-24, 10, 27, 24, 14, 38], "enamel_dark", P)
    for side in (-1, 1):
        beam(f"planter_drawbar_{side}", (side * 6.5, 11, 27), (side * 11, 13, 36), 1.5, "steel", P)
        beam(f"planter_top_link_{side}", (0, 18, 25), (side * 8, 14, 36), 1.1, "enamel_dark", P)
        centered(f"frame_rail_{side}", (side * 22.5, 11.6, 32.5), (1.7, 2.8, 11), "steel", P)
    # Lower-link clevis pairs and safety chain for the tractor's hitch arms.
    for side in (-1, 1):
        beam(f"hitch_clevis_{side}", (side * 5.8, 10.2, 26.4), (side * 5.8, 11.6, 27.6), 2.1, "steel", P)
        centered(f"hitch_bushing_{side}", (side * 5.8, 10.9, 27.2), (2.6, 2.6, .9), "enamel_dark", P)
    beam("safety_chain_left", (-4.6, 11.6, 27.2), (-1.4, 12.4, 26.6), .45, "steel", P)
    beam("safety_chain_right", (4.6, 11.6, 27.2), (1.4, 12.4, 26.6), .45, "steel", P)
    centered("chain_master_link", (0, 12.6, 26.4), (1.2, 1.2, .7), "steel", P)
    # PTO shaft guard running from the clevis toward the central meter drive.
    group("pto_shaft", (0, 12.6, 24.2))
    centered("pto_guard", (0, 12.6, 24.2), (3, 3, 4.4), "enamel_orange", "pto_shaft")
    centered("pto_guard_end", (0, 12.6, 21.6), (2.6, 2.6, 1), "steel", "pto_shaft")
    centered("pto_yoke", (0, 12.6, 26.9), (2.4, 2.4, 1.2), "steel", "pto_shaft")
    # Toolbar and rear rail carry the three row units.
    centered("planter_toolbar", (0, 13, 36), (49, 3.5, 3.5), "enamel_orange", P)
    centered("planter_rear_rail", (0, 14.5, 46), (48, 1.7, 1.7), "steel", P)
    for row, x in enumerate([-16, 0, 16]):
        prefix = f"row_{row + 1}"
        box(prefix + "_hopper", [x - 5.5, 17, 36.8, x + 5.5, 25, 45.2], "seed_tank", P)
        centered(prefix + "_hopper_lid", (x, 25.5, 41), (12, 1.1, 9.8), "enamel_orange", P)
        centered(prefix + "_lid_handle", (x, 26.5, 41), (3, .9, 1.3), "enamel_dark", P)
        centered(prefix + "_meter", (x, 15.6, 41), (5, 3.2, 5), "enamel_dark", P)
        centered(prefix + "_seed_tube", (x, 10.5, 43), (1.4, 8.5, 1.4), "rubber", P, (12, 0, 0))
        beam(prefix + "_arm", (x, 13, 36), (x, 6, 45), 1.5, "steel", P)
        beam(prefix + "_press_arm", (x, 9, 43), (x, 4.5, 53), 1, "enamel_dark", P)
        for side in (-1, 1):
            disk(prefix + f"_opener_{side}", (x + side * 1.8, 4.5, 45), 3.7, .65, "steel", P, 8)
        wheel(prefix + "_press_wheel", (x, 4.4, 53), 3.4, 2.3, P, 10, False)
        centered(prefix + "_rear_reflector", (x, 16.4, 46.95), (3, 1.6, .2), "taillight", P)
        # Per-row fill plug and seed level window on the tank wall.
        centered(prefix + "_fill_plug", (x, 23.2, 46), (2.2, 2.2, .5), "enamel_dark", P)
        centered(prefix + "_level_window", (x + 5.66, 21, 41), (.18, 2.6, 4), "steel", P)
    for side, label in [(-1, "left"), (1, "right")]:
        beam(f"planter_wheel_arm_{label}", (side * 23, 13, 36), (side * 25, 6, 42), 1.4, "steel", P)
        wheel(f"planter_{label}_wheel", (side * 25, 6, 42), 5, 3.8, P, 12, False)
        centered(f"planter_endcap_{label}", (side * 24, 14, 36), (1.2, 5, 5), "enamel_dark", P)
    # Branding, safety plate and rolling ID tag (standalone implement).
    centered("planter_brand", (0, 14.5, 47.05), (8, 2.4, .22), "decal_snc", P)
    centered("rear_safety_plate", (0, 20, 47.3), (4.5, 3.2, .45), "amber", P)
    centered("id_tag_plate", (-20.5, 12.4, 45.9), (3.6, 2.4, .18), "steel", P)
    centered("id_tag_code", (-20.5, 12.4, 46.02), (2.6, 1.4, .06), "decal_snc", P)
    centered("jack_stand", (12, 8.5, 31), (2.4, 9, 2.4), "steel", P)
    centered("jack_foot", (12, 3.8, 31), (4, 1.2, 4), "enamel_dark", P)


def main():
    author()
    materials = json.loads((OUT / "materials.json").read_text(encoding="utf-8"))
    for material in materials.values():
        material["file"] = str(OUT / material["file"])
    scene.validate(materials)
    cubes = len(scene.cubes)
    groups = len(scene.groups)
    model = build_model(
        "SNC 75-P", scene,
        locators=dict(hitch_center=[0, 11.5, 27], seed_rows=[[-16, 0, 45], [0, 0, 45], [16, 0, 45]],
                      pto_port=[0, 12.6, 26.9]),
        animation_axes=dict(wheels="X", planter_lift="negative X"))
    model["cube_count"] = cubes
    model["group_count"] = groups
    write_json(OUT / "planter-model.json", model)
    export_bbmodel(scene, OUT / "snc-75-p-plantadeira.bbmodel", materials,
                   "SNC 75-P — Plantadeira de três linhas destacável", "snc_75_p_planter")
    export_glb(scene, OUT / "snc-75-p-plantadeira.glb", materials,
               "snc_energies planter generator", "SNC 75-P planter",
               spin_rule=lambda name: (name.endswith("wheel") and name != "steering_wheel") or name == "pto_shaft")
    print(f"SNC 75-P: {cubes} cubes, {groups} articulated groups, "
          f"{len(materials)} materials. Exported JSON, Blockbench and GLB.")


if __name__ == "__main__":
    main()
