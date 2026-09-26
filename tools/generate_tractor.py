"""Author the SNC 75 tractor as a rigged cube model, Blockbench file and glTF 2 GLB.

Coordinates are Minecraft pixels: +Y up, -Z forward, 16 units per block.
Every pivot and cube is authored in rest/world coordinates. No game registries
are changed: this is a reviewable modelling asset, not a playable vehicle yet.

Primitives, rig conventions and the JSON/Blockbench/GLB exporters live in
tools/vehicle_library.py — the shared vehicle toolkit extracted verbatim from
this golden-reference generator (AGENTS.md rules 6 and 7).
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from vehicle_library import VehicleScene, build_model, export_bbmodel, export_glb, write_json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets/tractor"
OUT.mkdir(parents=True, exist_ok=True)
scene = VehicleScene("tractor")
group = scene.group
box = scene.box
centered = scene.centered
beam = scene.beam
disk = scene.disk
wheel = scene.wheel


def author():
    group("tractor", parent=None)
    # Chassis, transmission, sump and axles.
    box("main_chassis",[-7,10,-29,7,14,24])
    for side in [-1,1]:
        centered(f"frame_rail_{side}",(side*6.8,10.7,-4),(1.5,2.8,52),"steel")
    box("engine_sump",[-5,7.8,-25,5,12,-9],"enamel_dark")
    box("transmission",[-5,10,0,5,18,20],"steel")
    centered("rear_axle",(0,13,13.5),(31,3.8,4),"steel")
    centered("front_axle",(0,9,-23),(26,2.5,2.5),"steel")
    centered("rear_differential",(0,12.6,13.5),(8,7,8),"enamel_dark")
    for side,label in [(-1,"left"),(1,"right")]:
        wheel(f"rear_{label}_wheel",(side*15,13,13.5),11.5,8,tread_count=18)
        group(f"front_{label}_steering",(side*12,8.8,-23))
        wheel(f"front_{label}_wheel",(side*12,8.8,-23),7.5,5.5,f"front_{label}_steering",14)
        beam(f"steering_rod_{label}",(side*6,9.8,-21),(side*11,9.8,-21),.65)
    # Separate engine geometry stays visible beneath an opening hood.
    box("engine_block",[-5.6,14,-25,5.6,22,-9],"steel")
    for i in range(4):
        centered(f"cylinder_head_{i}",(0,22,-22+i*3.4),(8.2,1.8,2.7),"enamel_dark")
    for side in [-1,1]:
        for i in range(4):
            centered(f"engine_rib_{side}_{i}",(side*5.9,16+i*1.4,-17),(.7,.55,12),"enamel_dark")
    beam("exhaust_manifold",(-6,19,-22),(-6,19,-10),1.3,"steel")
    box("battery",[3.9,15,-6.6,7,19,-2.5],"enamel_dark")
    centered("battery_cap",(5.5,19.1,-4.5),(3.4,.4,4.5),"amber")
    group("hood",(0,25.5,-5.5))
    box("hood_top",[-7.8,24.3,-29,7.8,27.2,-5.5],"enamel_orange","hood")
    for side in [-1,1]:
        centered(f"hood_side_{side}",(side*7.3,20,-17.1),(1.25,8.7,23.4),"enamel_orange","hood")
        centered(f"hood_stripe_{side}",(side*8,19.1,-17),( .16,1.7,21.5),"enamel_dark","hood")
        centered(f"side_brand_{side}",(side*8.1,22.1,-12.5),(.18,2.8,7.4),"decal_snc","hood")
        for i in range(5):
            centered(f"hood_vent_{side}_{i}",(side*8,22.3,-25+i*1.6),(.18,3,.48),"enamel_dark","hood")
    box("nose_surround",[-7.4,15.8,-30,7.4,24.5,-28.7],"enamel_orange","hood")
    box("front_grille",[-5.8,16.4,-30.18,5.8,23.7,-30],"grille","hood")
    box("nose_badge",[-1.4,22,-30.4,1.4,24,-30.2],"decal_snc","hood")
    for side in [-1,1]:
        centered(f"headlamp_case_{side}",(side*5.6,25,-29.8),(3.4,2.8,1.6),"enamel_dark","hood")
        centered(f"headlamp_lens_{side}",(side*5.6,25,-30.65),(2.8,2.2,.2),"headlight","hood")
    centered("bumper",(0,11.7,-32),(20,3.7,3),"enamel_dark")
    for i in range(5):
        centered(f"front_counterweight_{i}",(-4.2+i*2.1,10,-34),(1.8,5,3),"steel")
    # Exhaust outside the bonnet so the hinge can open without intersecting it.
    beam("exhaust_elbow",(-6,19,-13),(-9,21,-13),1.5,"steel")
    centered("exhaust_muffler",(-9,28,-13),(2.4,11,2.4),"enamel_dark")
    centered("exhaust_stack",(-9,37,-13),(1.3,9,1.3),"steel")
    centered("exhaust_rain_flap",(-9,41.7,-13),(2,.4,2),"enamel_dark",rotation=(0,0,-12))
    centered("air_filter",(8.8,26,-7),(2.7,7,2.7),"enamel_dark")
    centered("air_filter_cap",(8.8,29.7,-7),(3.5,.8,3.5),"steel")
    # Driver platform, seat, instruments and controls.
    box("operator_floor",[-10,14,-1.5,10,16,23],"enamel_dark")
    box("floor_tread",[-7.5,16,-.5,7.5,16.25,17.5],"grille")
    centered("seat_pedestal",(0,19,13),(4,6,4),"steel")
    for y in [18,19,20]:
        centered(f"seat_suspension_{y}",(0,y,13),(4.7,.45,4.7),"enamel_dark")
    centered("seat_cushion",(0,23,12.8),(9.5,2.6,9),"seat")
    centered("seat_back",(0,27.1,17),(9.6,7.4,2.1),"seat",rotation=(-9,0,0))
    for side in [-1,1]:
        centered(f"seat_arm_{side}",(side*5.5,26,13.3),(1.4,1.2,6),"seat")
        beam(f"arm_support_{side}",(side*5.4,23,15),(side*5.4,25.8,15),.7,"steel")
        for step in range(2):
            centered(f"step_{side}_{step}",(side*(12.3+step*1.3),11-step*3.4,4),(5,1.1,6),"steel")
            centered(f"step_tread_{side}_{step}",(side*(12.3+step*1.3),11.58-step*3.4,4),(4.6,.12,5.6),"grille")
        beam(f"step_support_{side}",(side*10,15,6),(side*14,6.7,6),.8)
    centered("dashboard_housing",(0,24,-2.3),(11,5.8,4.3),"enamel_dark",rotation=(-15,0,0))
    centered("dashboard_instruments",(0,25.2,.0),(8.7,3.6,.2),"gauge",rotation=(-15,0,0))
    beam("steering_column",(0,19,1),(0,28,4),1.05,"steel")
    group("steering_wheel",(0,29,4),rotation=(60,0,0))
    for i in range(8):
        a=i*math.tau/8
        centered(f"steering_rim_{i}",(math.sin(a)*3.1,29+math.cos(a)*3.1,4),(2.7,.72,.72),"seat","steering_wheel",(0,0,-i*45))
    for i in range(3):
        a=i*math.tau/3
        beam(f"steering_spoke_{i}",(0,29,4),(math.sin(a)*2.9,29+math.cos(a)*2.9,4),.45,"steel","steering_wheel")
    centered("steering_center",(0,29,4),(1.5,1.5,1.1),"enamel_orange","steering_wheel")
    for i in range(2):
        beam(f"pedal_arm_{i}",(-3+i*6,16,2),(-3+i*6,18.2,1),.45)
        centered(f"pedal_{i}",(-3+i*6,18.4,.8),(2.1,.55,2.4),"rubber",rotation=(15,0,0))
    beam("gear_lever",(5.7,16,8),(5.7,23,7),.48,"steel")
    centered("gear_knob",(5.7,23.5,7),(1.5,1.5,1.5),"seat")
    centered("hydraulic_console",(-6.7,20,11),(2.2,4,6),"enamel_dark")
    for i in range(2):
        beam(f"hydraulic_lever_{i}",(-6.7,22,9.7+i*2),(-6.7,25,9+i*2),.35)
        centered(f"hydraulic_knob_{i}",(-6.7,25,9+i*2),(.9,.9,.9),"enamel_orange")
    # Mudguards, safety structure and detachable sun canopy.
    for side in [-1,1]:
        centered(f"fender_top_{side}",(side*14.9,26.6,13.2),(10.8,1.5,23.8),"enamel_orange")
        centered(f"fender_inner_{side}",(side*9.6,22,13.2),(1.1,8,24),"enamel_orange")
        centered(f"fender_lip_{side}",(side*20.2,25.7,13.2),(.65,2.4,23.5),"enamel_orange")
        centered(f"rear_light_case_{side}",(side*15.5,26,25.3),(4,2.5,1.1),"enamel_dark")
        centered(f"rear_light_{side}",(side*15.5,26.1,25.94),(2.2,1.7,.2),"taillight")
        centered(f"turn_light_{side}",(side*18,26.1,25.94),(1.5,1.7,.2),"amber")
        beam(f"rollbar_post_{side}",(side*8.5,16,19.2),(side*8.5,44.5,19.2),1.9,"enamel_dark")
        beam(f"entry_handle_{side}",(side*8.5,28,18.3),(side*8.5,34,18.3),.7,"steel")
        beam(f"mirror_arm_{side}",(side*8.5,38,18.8),(side*13,37,11),.65,"steel")
        centered(f"mirror_case_{side}",(side*13,37,10.8),(2.6,4,.8),"enamel_dark")
        centered(f"mirror_face_{side}",(side*13,37,11.28),(2.1,3.4,.16),"rim")
    centered("rollbar_crossbeam",(0,43.8,19.2),(19,2,2),"enamel_dark")
    group("canopy",(0,44,10))
    box("canopy_shell",[-13,45.3,-4,13,47.2,25],"enamel_orange","canopy")
    box("canopy_top",[-11.7,47.2,-2.8,11.7,47.8,23.8],"enamel_orange","canopy")
    box("canopy_underside",[-12,44.8,-3,12,45.3,24],"enamel_dark","canopy")
    for side in [-1,1]:
        centered(f"roof_worklamp_case_{side}",(side*9,44.8,-3),(3,2.5,2),"enamel_dark","canopy")
        centered(f"roof_worklamp_{side}",(side*9,44.8,-4.06),(2.5,1.8,.18),"headlight","canopy")
    centered("beacon_base",(-10,48.2,20),(2.9,.7,2.9),"enamel_dark","canopy")
    centered("beacon_lens",(-10,49.7,20),(2.4,2.3,2.4),"amber","canopy")
    # Hitch remains on the tractor when the planter is detached.
    group("hitch",(0,12,24))
    for side in [-1,1]:
        beam(f"lower_hitch_{side}",(side*5.3,10,20),(side*7.2,9,28),1.3,"steel","hitch")
        beam(f"lift_rod_{side}",(side*5.3,17.5,21),(side*6.7,10,26),.75,"steel","hitch")
    centered("pto_guard",(0,11.5,25),(3.4,3.4,4),"enamel_orange","hitch")
    centered("hitch_pin",(0,11,27.3),(3.5,.7,.7),"steel","hitch")
    # A three-row planter with individual tanks, metering housings and openers.
    group("planter",(0,12,25))
    for side in [-1,1]:
        beam(f"planter_drawbar_{side}",(side*6.5,11,27),(side*11,13,36),1.5,"steel","planter")
        beam(f"planter_top_link_{side}",(0,18,25),(side*8,14,36),1.1,"enamel_dark","planter")
    centered("planter_toolbar",(0,13,36),(49,3.5,3.5),"enamel_orange","planter")
    centered("planter_rear_rail",(0,14.5,46),(48,1.7,1.7),"steel","planter")
    for row,x in enumerate([-16,0,16]):
        prefix=f"row_{row+1}"
        box(prefix+"_hopper",[x-5.5,17,36.8,x+5.5,25,45.2],"seed_tank","planter")
        centered(prefix+"_hopper_lid",(x,25.5,41),(12,1.1,9.8),"enamel_orange","planter")
        centered(prefix+"_lid_handle",(x,26.5,41),(3,.9,1.3),"enamel_dark","planter")
        centered(prefix+"_meter",(x,15.6,41),(5,3.2,5),"enamel_dark","planter")
        centered(prefix+"_seed_tube",(x,10.5,43),(1.4,8.5,1.4),"rubber","planter",(12,0,0))
        beam(prefix+"_arm",(x,13,36),(x,6,45),1.5,"steel","planter")
        beam(prefix+"_press_arm",(x,9,43),(x,4.5,53),1,"enamel_dark","planter")
        for side in [-1,1]:
            disk(prefix+f"_opener_{side}",(x+side*1.8,4.5,45),3.7,.65,"steel","planter",8)
        wheel(prefix+"_press_wheel",(x,4.4,53),3.4,2.3,"planter",10,False)
        centered(prefix+"_rear_reflector",(x,16.4,46.95),(3,1.6,.2),"taillight","planter")
    for side,label in [(-1,"left"),(1,"right")]:
        beam(f"planter_wheel_arm_{label}",(side*23,13,36),(side*25,6,42),1.4,"steel","planter")
        wheel(f"planter_{label}_wheel",(side*25,6,42),5,3.8,"planter",12,False)
        centered(f"planter_endcap_{label}",(side*24,14,36),(1.2,5,5),"enamel_dark","planter")
    centered("planter_brand",(0,14.5,47.05),(8,2.4,.22),"decal_snc","planter")
    centered("rear_safety_plate",(0,20,47.3),(4.5,3.2,.45),"amber","planter")


def main():
    author()
    materials = json.loads((OUT / "materials.json").read_text(encoding="utf-8"))
    for material in materials.values():
        material["file"] = str(OUT / material["file"])
    scene.validate(materials)
    model = build_model("SNC 75", scene,
                        locators=dict(driver_seat=[0, 24.3, 12.8], rear_hitch=[0, 12, 25], front_axle=[0, 8.8, -23],
                                      rear_axle=[0, 13, 13.5], seed_rows=[[-16, 0, 45], [0, 0, 45], [16, 0, 45]]),
                        animation_axes=dict(wheels="X", front_steering="Y", steering_wheel="local Z", hood="X",
                                            planter_lift="negative X"))
    write_json(OUT / "tractor-model.json", model)
    export_bbmodel(scene, OUT / "snc-75-trator.bbmodel", materials,
                   "SNC 75 — Tractor and three-row planter", "snc_75_tractor")
    export_glb(scene, OUT / "snc-75-trator.glb", materials, "SNC Energies procedural tractor authoring", "SNC 75 (metres)")
    print(f"SNC 75: {len(scene.cubes)} cubes, {len(scene.groups)} articulated groups, {len(materials)} materials. Exported JSON, Blockbench and GLB.")


if __name__ == "__main__":
    main()
