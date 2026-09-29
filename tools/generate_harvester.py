"""Author the SNC 90 combine harvester as a rigged cube model, Blockbench file and glTF 2 GLB.

Coordinates are Minecraft pixels: +Y up, -Z forward, 16 units per block.
Every pivot and cube is authored in rest/world coordinates through the shared
vehicle toolkit (tools/vehicle_library.py, extracted from the SNC 75 golden
reference). No game registries are changed: this is a reviewable modelling
asset, not a playable machine yet.

Layout (rest pose, header on ground): grain tank and closed cab over the
chassis; axial rotor and cleaning shoe in the belly; straw chopper behind the
driven axle; cutter header with reel ahead; unloading auger folded along the
left tank wall. Real asymmetries: auger, ladder and exhaust on the left/right
sides exactly where a real combine puts them.
"""
from __future__ import annotations

import json
import math
from pathlib import Path

from vehicle_library import VehicleScene, build_model, export_bbmodel, export_glb, write_json

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "assets/harvester"
OUT.mkdir(parents=True, exist_ok=True)
scene = VehicleScene("harvester")
box, centered, beam, disk, wheel = scene.box, scene.centered, scene.beam, scene.disk, scene.wheel
P = "harvester"


def group(name, origin=(0, 0, 0), parent=P, rotation=(0, 0, 0)):
    # Root default is this vehicle instead of the library's "tractor" default.
    return scene.group(name, origin, parent, rotation)



def author():
    group("harvester", parent=None)
    # ---------------------------------------------------------- chassis
    box("main_chassis", [-26, 8, -18, 26, 14, 40], "enamel_dark", P)
    for side in (-1, 1):
        centered(f"frame_rail_{side}", (side * 24.2, 10.6, 11), (1.7, 3.2, 56), "steel", P)
    centered("front_axle", (0, 8.5, -9), (30, 2.6, 2.6), "steel", P)
    centered("rear_axle", (0, 13.5, 32), (45, 4, 4.5), "steel", P)
    for side in (-1, 1):
        centered(f"final_drive_{side}", (side * 21, 13.5, 32), (6, 7.2, 7.2), "enamel_dark", P)
    wheel("rear_left_wheel", (-21, 13.5, 32), 13, 10, P, 18)
    wheel("rear_right_wheel", (21, 13.5, 32), 13, 10, P, 18)
    wheel("front_left_wheel", (-13, 8.5, -9), 7.5, 6, P, 12)
    wheel("front_right_wheel", (13, 8.5, -9), 7.5, 6, P, 12)
    for side in (-1, 1):
        for step in range(2):
            centered(f"chassis_step_{side}_{step}", (side * (25.8 + step * 1.1), 11.6 - step * 3.4, 20), (5.4, 1.1, 7), "steel", P)
            centered(f"chassis_step_tread_{side}_{step}", (side * (25.8 + step * 1.1), 12.18 - step * 3.4, 20), (5, .14, 6.6), "grille", P)
    centered("tool_box", (17.5, 15.4, -12), (7, 3.2, 8), "enamel_dark", P)
    centered("tool_box_latch", (17.5, 15.4, -8.1), (2.4, 1, .4), "amber", P)
    # Drawbar hitch for grain carts.
    beam("hitch_left", (-3.4, 11, 38), (-5.4, 10.6, 44), 1.4, "steel", P)
    beam("hitch_right", (3.4, 11, 38), (5.4, 10.6, 44), 1.4, "steel", P)
    centered("hitch_pin", (0, 10.6, 42.6), (3.4, .8, .8), "steel", P)
    # ------------------------------------------------- feeder house
    box("feeder_house", [-9.9, 10, -23.6, 9.9, 26, 1.8], "enamel_dark", P)
    for side in (-1, 1):
        centered(f"feeder_side_{side}", (side * 9.9, 18, -10.9), (.9, 16, 25.4), "enamel_orange", P)
        centered(f"feeder_window_{side}", (side * 10.4, 20, -10.9), (.16, 5.5, 7), "grille", P)
    for i in range(7):
        centered(f"feeder_slat_{i}", (0, 13.2 + i * 1.9, -21 + i * 3.1), (17.4, .8, 2.1), "steel", P)
    centered("stone_trap_door", (0, 11, -23.7), (8, 3, .5), "steel", P)
    beam("feeder_top_beam", (-9.9, 25.6, -16), (9.9, 25.6, -16), 1.3, "steel", P)
    beam("feeder_front_beam", (-9.9, 25.6, -23), (9.9, 25.6, -23), 1.3, "steel", P)
    # -------------------------------------------------- rotor threshing
    # Open housing frame keeps the rotor visible and collision-free while spinning.
    box("rotor_housing_left_plate", [-10.6, 16, 2.6, -9.8, 34, 24.6], "enamel_dark", P)
    box("rotor_housing_right_plate", [9.8, 16, 2.6, 10.6, 34, 24.6], "enamel_dark", P)
    box("rotor_housing_top", [-10.6, 31.4, 2.6, 10.6, 32.4, 24.6], "enamel_dark", P)
    box("concave_left", [-10, 18, 4, -8, 32, 22], "grille", P)
    box("concave_right", [8, 18, 4, 10, 32, 22], "grille", P)
    box("concave_bottom", [-9, 14.4, 6, 9, 16.4, 22], "grille", P)
    group("rotor", (0, 25, 13))
    centered("rotor_pipe", (0, 25, 13), (6.6, 6.6, 21), "steel", "rotor")
    for i in range(8):
        a = math.radians(i * 45)
        centered(f"rotor_bar_{i}", (math.cos(a) * 4.7, 25 + math.sin(a) * 4.7, 13), (1.7, 1.7, 19.5), "thresher", "rotor")
    for i in range(3):
        phase = math.radians(i * 120)
        end = phase + math.radians(115)
        beam(f"rotor_helix_{i}", (math.cos(phase) * 4.7, 25 + math.sin(phase) * 4.7, 3.5),
             (math.cos(end) * 4.7, 25 + math.sin(end) * 4.7, 22.5), 1.25, "thresher", "rotor")
    centered("rotor_nose_cone", (0, 25, 2.4), (6, 6, 3), "enamel_dark", "rotor")
    centered("rotor_nose_tip", (0, 25, .4), (3.8, 3.8, 2.4), "steel", "rotor")
    centered("rotor_tail_cone", (0, 25, 23.6), (5.4, 5.4, 3), "enamel_dark", "rotor")
    centered("rotor_tail_tip", (0, 25, 25.6), (3.4, 3.4, 2.2), "steel", "rotor")
    disk("beater_drum", (0, 30.5, 25.5), 5.6, 3, "steel", P, 8)
    for side in (-1, 1):
        centered(f"beater_paddle_{side}", (side * 2.2, 33.4, 25.5), (1.5, 3.4, 4.4), "thresher", P)
    # ------------------------------------------------- cleaning shoe
    box("sieve_upper", [-11, 17.4, 6.5, 11, 18.9, 21], "grille", P)
    box("sieve_lower", [-11, 12.9, 9, 11, 14.4, 23.5], "grille", P)
    box("grain_pan", [-11, 15.2, 6.5, 11, 16.6, 20.5], "steel", P, (7, 0, 0))
    group("cleaning_fan", (0, 12.4, 4.2))
    centered("cleaning_fan_hub", (0, 12.4, 4.2), (3.4, 3, 3), "enamel_dark", "cleaning_fan")
    for i in range(5):
        a = math.radians(i * 72)
        centered(f"cleaning_fan_blade_{i}", (0, 12.4 + math.sin(a) * 4.4, 4.2 + math.cos(a) * 4.4), (5.2, 1, 2.3), "steel", "cleaning_fan", (i * 72, 0, 0))
    centered("cleaning_fan_case", (0, 12.4, 2), (6.6, 10.4, 1.6), "enamel_dark", P)
    # ------------------------------------------------- straw chopper
    box("chopper_housing", [-13, 10, 25.5, 13, 18.5, 33], "enamel_dark", P)
    group("straw_chopper", (0, 13.5, 29.5))
    disk("chopper_drum", (0, 13.5, 29.5), 5.2, 23, "steel", "straw_chopper", 8)
    for end in (-11.4, 11.4):
        disk(f"chopper_disc_{end}", (end, 13.5, 29.5), 5.2, 1.4, "enamel_dark", "straw_chopper", 6)
    for i in range(8):
        knife_set = group(f"chopper_knife_set_{i}", (0, 13.5, 29.5), "straw_chopper", (i * 45, 0, 0))
        for x in (-8, 0, 8):
            centered(f"chopper_knife_{i}_{x}", (x, 18.9, 29.5), (4.6, 1.35, 2.7), "chopper", knife_set)
    for side in (-1, 1):
        centered(f"chopper_tailboard_{side}", (side * 8.5, 12.5, 34.4), (6.5, 7, .7), "enamel_dark", P, (18 * side, 0, 0))
    for side in (-1, 1):
        centered(f"tail_light_case_{side}", (side * 11.6, 19.4, 32.6), (3.6, 2.4, 1), "enamel_dark", P)
        centered(f"tail_light_{side}", (side * 11.6, 19.5, 33.18), (2.4, 1.7, .18), "taillight", P)
        centered(f"rear_reflector_{side}", (side * 6.4, 19.5, 33.18), (2.2, 1.5, .18), "amber", P)
    # ------------------------------------------------- grain tank
    box("tank_floor", [-14, 32.4, 1.6, 14, 34.2, 22.6], "steel", P)
    box("tank_front_wall", [-14, 34, 1.4, 14, 52, 2.8], "grain_tank", P)
    box("tank_rear_wall", [-14, 34, 21.6, 14, 52, 23], "grain_tank", P)
    box("tank_left_wall", [-14.4, 34, 1.4, -11.6, 52, 23], "grain_tank", P)
    box("tank_right_wall", [11.6, 34, 1.4, 14.4, 52, 23], "grain_tank", P)
    box("tank_decal_left", [-14.7, 38, 5, -14.4, 46, 17], "decal_crop", P)
    box("tank_decal_right", [14.4, 38, 5, 14.7, 46, 17], "decal_crop", P)
    box("tank_brand_left", [-14.7, 41.5, 18.5, -14.4, 46.5, 22.5], "decal_snc", P)
    for side in (-1, 1):
        beam(f"tank_top_rail_{side}", (side * 13, 52.4, 2), (side * 13, 52.4, 22.4), 1.2, "steel", P)
        centered(f"tank_rib_front_{side}", (side * 12.9, 43, 2.9), (1.1, 9.4, 1.1), "enamel_orange", P)
        centered(f"tank_rib_rear_{side}", (side * 12.9, 43, 21.5), (1.1, 9.4, 1.1), "enamel_orange", P)
    box("tank_walkway", [-11.6, 52, 7.5, 11.6, 52.8, 16.5], "grille", P)
    box("tank_hatch", [-3.4, 52.8, 9, 3.4, 53.5, 15], "enamel_dark", P)
    box("clean_grain_auger", [-7, 30.8, 6, 7, 32.6, 18], "steel", P)
    # --------------------------------------- unloading auger (left, foldable)
    group("unloading_auger", (-14, 40, 4))
    centered("auger_main_tube", (-18.6, 40, 17), (4.4, 4.4, 27), "auger", "unloading_auger")
    centered("auger_drive_house", (-15.35, 40, 5.2), (1.9, 5.6, 3.4), "enamel_dark", "unloading_auger")
    centered("auger_fold_collar", (-18.6, 40, 27.6), (5.1, 5.1, 2.4), "steel", "unloading_auger")
    beam("auger_outer_tube", (-18.6, 40, 29), (-18.6, 45.4, 32.4), 4.3, "auger", "unloading_auger")
    beam("auger_support_brace", (-14.9, 37.4, 22), (-18.6, 40, 22), 1, "steel", "unloading_auger")
    group("spout", (-18.6, 45.6, 32.8), parent="unloading_auger")
    centered("spout_downpipe", (-18.6, 40, 37.2), (3.8, 9, 3.8), "auger", "spout")
    centered("spout_flap", (-18.6, 34.8, 38.6), (3.2, 1.8, 3.2), "enamel_dark", "spout", (24, 0, 0))
    disk("spout_handwheel", (-21.6, 45.6, 32.8), 1.7, .8, "steel", "spout", 6)
    # ------------------------------------------------- engine bay + hood
    box("engine_block", [12, 15, 7, 24, 27, 27], "steel", P)
    box("valve_cover", [13, 27, 9, 23, 29.4, 25], "enamel_orange", P)
    for i in range(4):
        centered(f"valve_bolt_{i}", (18, 29.6, 11 + i * 4), (1.1, .5, 1.1), "enamel_dark", P)
    for side in (-1, 1):
        for i in range(3):
            centered(f"engine_rib_{side}_{i}", (12.4 if side < 0 else 23.6, 18 + i * 2.4, 17), (.6, .6, 9), "enamel_dark", P)
    box("radiator_core", [12.5, 16, 28.6, 23.5, 27.5, 31], "grille", P)
    box("radiator_frame", [12, 15.4, 31, 24, 28.1, 31.8], "enamel_dark", P)
    group("cooling_fan", (18, 21.6, 28))
    disk("cooling_fan_hub", (18, 21.6, 28), 1.7, 1.1, "steel", "cooling_fan", 4)
    for i in range(6):
        a = math.radians(i * 60)
        centered(f"cooling_fan_blade_{i}", (18 + math.cos(a) * 3.7, 21.6 + math.sin(a) * 3.7, 28), (1, 4.4, .6), "steel", "cooling_fan", (0, 0, i * 60))
    box("fan_belt_upper", [14.4, 24.2, 27.4, 22, 25, 28.2], "belt", P)
    box("fan_belt_lower", [14.4, 17.6, 27.4, 22, 18.4, 28.2], "belt", P)
    box("battery", [12.4, 15.6, 2.6, 17, 19.4, 6], "enamel_dark", P)
    centered("battery_cap", (14.7, 19.6, 4.3), (3.6, .5, 2.4), "amber", P)
    beam("battery_cable", (17.2, 17.5, 4.3), (19.4, 17.5, 6.4), .5, "enamel_dark", P)
    box("hydraulic_tank", [19.8, 15.4, 2.4, 25.2, 21.6, 9.4], "enamel_dark", P)
    centered("hydraulic_filler", (22.5, 22, 5.9), (1.9, 1, 1.9), "amber", P)
    centered("air_filter_body", (14.2, 27.7, 11), (4.2, 4.2, 6.4), "enamel_dark", P)
    centered("air_filter_cap", (14.2, 30, 11), (5.2, .4, 7.2), "steel", P)
    beam("air_filter_pipe", (14.2, 28.5, 11), (14.2, 29.8, 11), 1.5, "steel", P)
    # Exhaust lives behind the hood edge so the hinge opens without intersecting it.
    beam("exhaust_elbow", (18, 26.6, 27.6), (22, 29, 31.2), 1.5, "steel", P)
    centered("exhaust_muffler", (22, 32.6, 34.6), (3, 7.4, 3), "enamel_dark", P)
    centered("exhaust_stack", (22, 39.4, 34.6), (1.7, 7, 1.7), "steel", P)
    centered("exhaust_rain_flap", (22, 43.2, 34.6), (2.4, .5, 2.4), "enamel_dark", P, (0, 0, -14))
    box("engine_bay_bulkhead", [10, 15, 33.1, 26, 33.8, 33.9], "enamel_orange", P)
    group("hood", (17.5, 32, 31.4))
    box("hood_top", [12.8, 30.2, 2.6, 26, 33.8, 31.4], "enamel_orange", "hood")
    centered("hood_side_panel", (26.4, 22.6, 17.5), (1.1, 14.8, 27.8), "enamel_orange", "hood")
    box("hood_front_slope", [12.8, 26.6, 2.6, 26, 30.2, 4.4], "enamel_orange", "hood")
    for i in range(5):
        centered(f"hood_louver_{i}", (26.9, 20 + (i % 2) * 5.6, 12 + i * 4), (.18, 3.6, 1.8), "enamel_dark", "hood")
    centered("hood_badge", (26.99, 24.6, 8.6), (.18, 3.2, 5.2), "decal_snc", "hood")
    centered("hood_latch", (17.5, 29.4, 3.5), (3.4, 1.4, 1), "enamel_dark", "hood")
    # ------------------------------- closed cab over the feeder + interior
    box("cab_floor", [-12, 26, -19.4, 12, 28, -1], "enamel_dark", P)
    box("windshield", [-11, 28.4, -20, 11, 46, -18.7], "glass", P, (-11, 0, 0))
    for side in (-1, 1):
        beam(f"cab_a_pillar_{side}", (side * 10.8, 28.4, -17.2), (side * 10.8, 46.4, -18.4), 1.5, "enamel_orange", P)
        box(f"cab_side_glass_{side}", [side * 12.1 - (0 if side > 0 else .5), 30, -16.9, side * 12.1 + (.5 if side > 0 else 0), 44.4, -2.9], "glass", P)
        beam(f"cab_b_pillar_{side}", (side * 12.1, 30, -10.2), (side * 12.1, 44.4, -10.2), 1.1, "enamel_orange", P)
    box("rear_glass", [-11, 30, -0.8, 11, 44, 0.5], "glass", P)
    for side in (-1, 1):
        beam(f"cab_d_pillar_{side}", (side * 10.8, 28.4, -1.8), (side * 10.8, 46.4, -0.4), 1.5, "enamel_orange", P)
    box("cab_roof", [-12.6, 46.8, -21, 12.6, 49.4, 0.8], "enamel_orange", P)
    for side in (-1, 1):
        beam(f"roof_gutter_{side}", (side * 12.2, 46.6, -19.4), (side * 12.2, 46.6, -0.8), .9, "enamel_dark", P)
        centered(f"work_lamp_case_{side}", (side * 8.6, 46.4, -20.8), (3, 2.4, 2), "enamel_dark", P)
        centered(f"work_lamp_{side}", (side * 8.6, 46.4, -21.88), (2.4, 1.8, .2), "headlight", P)
        centered(f"head_lamp_case_{side}", (side * 9.4, 28.6, -20.6), (3.2, 2.6, 1.4), "enamel_dark", P)
        centered(f"head_lamp_{side}", (side * 9.4, 28.6, -21.42), (2.6, 2, .2), "headlight", P)
        centered(f"turn_lamp_{side}", (side * 11.7, 28.8, -20.5), (1.7, 2, .2), "amber", P)
    centered("beacon_base", (9, 49.4, -1.8), (2.8, .7, 2.8), "enamel_dark", P)
    centered("beacon_lens", (9, 50.9, -1.8), (2.3, 2.3, 2.3), "amber", P)
    box("roof_hatch", [-3.2, 49.4, -14.4, 3.2, 50.2, -8.4], "glass", P)
    for side in (-1, 1):
        beam(f"mirror_arm_{side}", (side * 10.8, 44.4, -18.8), (side * 14.4, 42.4, -19.4), .55, "steel", P)
        centered(f"mirror_case_{side}", (side * 14.6, 42.4, -19.5), (2.4, 4, .8), "enamel_dark", P)
        centered(f"mirror_face_{side}", (side * 14.6, 42.4, -20.04), (2, 3.4, .16), "rim", P)
    # Driver station: seat, tilted wheel, right console, left monitor, pedals.
    centered("seat_pedestal", (0, 29, -8), (4.4, 2.2, 4.4), "steel", P)
    for y in (30.6, 31.4):
        centered(f"seat_suspension_{y}", (0, y, -8), (5, .4, 5), "enamel_dark", P)
    centered("seat_cushion", (0, 32.6, -8.2), (9.6, 2.5, 8.6), "seat", P)
    centered("seat_back", (0, 36.6, -4.2), (9.6, 7.6, 2.1), "seat", P, (-9, 0, 0))
    centered("seat_headrest", (0, 40.8, -3.5), (6, 2.2, 1.8), "seat", P, (-9, 0, 0))
    for side in (-1, 1):
        centered(f"seat_arm_{side}", (side * 5.6, 35.4, -7.8), (1.4, 1.2, 6), "seat", P)
        beam(f"seat_arm_support_{side}", (side * 5.5, 32, -6.4), (side * 5.5, 34.8, -6.4), .7, "steel", P)
    beam("steering_column", (0, 28.4, -16), (0, 33.6, -18.6), 1.05, "steel", P)
    group("steering_wheel", (0, 34.4, -18.8), rotation=(48, 0, 0))
    for i in range(8):
        a = math.radians(i * 45)
        centered(f"steering_rim_{i}", (math.sin(a) * 3.1, 34.4 + math.cos(a) * 3.1, -18.8), (2.7, .72, .72), "seat", "steering_wheel", (0, 0, -i * 45))
    for i in range(3):
        a = math.radians(i * 120)
        beam(f"steering_spoke_{i}", (0, 34.4, -18.8), (math.sin(a) * 2.9, 34.4 + math.cos(a) * 2.9, -18.8), .45, "steel", "steering_wheel")
    centered("steering_hub", (0, 34.4, -18.8), (1.5, 1.5, 1.1), "enamel_orange", "steering_wheel")
    centered("control_console", (7.6, 29.6, -13.4), (3, 3.4, 8.4), "enamel_dark", P)
    for i in range(3):
        beam(f"console_lever_{i}", (7.6, 31.4, -15.8 + i * 2.4), (7.6, 34, -16.4 + i * 2.4), .4, "steel", P)
        centered(f"console_knob_{i}", (7.6, 34.2, -16.4 + i * 2.4), (1, 1, 1), "enamel_orange", P)
    centered("cab_monitor", (-8.8, 31.6, -15), (2.8, 3.6, .8), "gauge", P, (0, 18, 0))
    beam("throttle_lever", (-6.8, 29.4, -16.8), (-6.8, 32.4, -17.8), .4, "steel", P)
    centered("throttle_knob", (-6.8, 32.6, -17.8), (1, 1, 1), "enamel_dark", P)
    for i in range(2):
        beam(f"pedal_arm_{i}", (-2.4 + i * 4.8, 28.4, -17.4), (-2.4 + i * 4.8, 30.2, -18.4), .45, "steel", P)
        centered(f"pedal_{i}", (-2.4 + i * 4.8, 30.4, -18.7), (2.1, .55, 2.3), "rubber", P, (14, 0, 0))
    centered("radio_box", (8.6, 33.2, -18), (2.4, 1.6, 1.4), "enamel_dark", P)
    # Left entry: platform, ladder, handrails.
    box("entry_platform", [-16.2, 24.2, 11.4, -11.4, 26, 18.4], "steel", P)
    box("entry_tread", [-15.6, 26, 12, -11.8, 26.16, 17.8], "grille", P)
    for i in range(4):
        centered(f"ladder_rung_{i}", (-15.9, 21.2 - i * 4.4, 14.9), (1.6, 1, 4.6), "steel", P)
    beam("ladder_rail_front", (-15.9, 24.6, 12.4), (-15.9, 3.4, 16.4), 1, "steel", P)
    beam("ladder_rail_back", (-15.9, 24.6, 17.6), (-15.9, 3.4, 13.6), 1, "steel", P)
    beam("entry_handrail", (-16.2, 26.4, 11.8), (-16.2, 34.4, 5.4), .8, "steel", P)
    beam("entry_handrail_post", (-16.2, 26.2, 11.8), (-16.2, 34.2, 11.8), .8, "steel", P)
    # ------------------------------------------------- cutter header
    group("header", (0, 14, -16))
    box("header_frame", [-32, 4, -36, 32, 8, -23.5], "enamel_dark", "header")
    box("cutter_bar", [-32, 2.8, -36.6, 32, 4.2, -34.6], "cutter_bar", "header")
    for i in range(10):
        x = -29 + i * 6.44
        beam(f"header_guard_{i}", (x, 3.6, -35), (x, 3.1, -38.2), .9, "steel", "header")
    box("header_back_sheet", [-32, 8, -26.4, 32, 13.8, -24.8], "enamel_orange", "header")
    for side in (-1, 1):
        box(f"header_side_sheet_{side}", [side * 31.1 - (0 if side > 0 else .9), 4, -36, side * 31.1 + (.9 if side > 0 else 0), 14, -23.5], "enamel_orange", "header")
        beam(f"header_divider_{side}", (side * 31.4, 5.4, -34.6), (side * 27.6, 13.8, -29.6), 1.15, "steel", "header")
        centered(f"header_shoe_{side}", (side * 31.2, 3.4, -24), (1.4, 1.6, 5.5), "steel", "header")
    centered("header_auger_pipe", (0, 9.4, -29.4), (62, 3.2, 3.2), "auger", "header")
    for x in (-21, 0, 21):
        disk(f"header_auger_flight_{x}", (x, 9.4, -29.4), 4.6, 3, "auger", "header", 8)
    centered("header_drive_gearbox", (24.4, 9.6, -25.4), (4.4, 4.4, 3.4), "enamel_dark", "header")
    box("header_drive_belt", [22, 10.6, -26.2, 28.6, 11.4, -25.2], "belt", "header")
    group("reel", (0, 20, -29.4))
    for x in (-22.4, 0, 22.4):
        disk(f"reel_disc_{x}", (x, 20, -29.4), 8, 1.4, "steel", "reel", 8)
    for end in (-29.4, 29.4):
        disk(f"reel_end_disc_{end}", (end, 20, -29.4), 8, 1.2, "enamel_orange", "reel", 6)
    for i in range(6):
        a = math.radians(i * 60)
        y = 20 + math.cos(a) * 8.1
        z = -29.4 + math.sin(a) * 8.1
        centered(f"reel_slat_{i}", (0, y, z), (46, 1.6, 1.6), "steel", "reel")
        for k, x in enumerate((-15, 15)):
            beam(f"reel_tine_{i}_{k}", (x, y, z), (x, 20 + math.cos(a) * 10.2, -29.4 + math.sin(a) * 10.2), .55, "steel", "reel")
    for side in (-1, 1):
        beam(f"reel_arm_{side}", (side * 27.4, 9.4, -25.8), (side * 27.4, 19.2, -29.4), 1.15, "steel", "reel")
        beam(f"reel_cylinder_{side}", (side * 27.4, 12.4, -24.4), (side * 26, 11.8, -24), .8, "enamel_dark", "header")
    centered("reel_drive_motor", (-24.8, 20, -29.4), (2.6, 4.4, 4.4), "enamel_dark", "reel")


def main():
    author()
    materials = json.loads((OUT / "materials.json").read_text(encoding="utf-8"))
    for material in materials.values():
        material["file"] = str(OUT / material["file"])
    scene.validate(materials)
    spin = lambda name: (name.endswith("wheel") and name != "steering_wheel") or name in {"reel", "cooling_fan", "cleaning_fan", "straw_chopper"}
    model = build_model("SNC 90", scene,
                        locators=dict(driver_seat=[0, 33, 14.2], header_attach=[0, 14, -16], spout_tip=[-18.6, 34.4, 39],
                                      front_axle=[0, 8.5, -15], rear_axle=[0, 13.5, 32], engine_bay=[18, 21, 17],
                                      unloading_pivot=[-14, 40, 4], spout_folded=[-18.6, 34.4, 39]),
                        animation_axes=dict(wheels="X", reel="X", cooling_fan="X", cleaning_fan="X", straw_chopper="X",
                                            rotor="Z", steering_wheel="local Z", header_lift="positive X",
                                            unloading_raise="negative X", spout_fold="negative Z", hood="positive X"))
    write_json(OUT / "harvester-model.json", model)
    export_bbmodel(scene, OUT / "snc-90-colheitadeira.bbmodel", materials,
                   "SNC 90 — Combine harvester with cutter header", "snc_90_harvester")
    export_glb(scene, OUT / "snc-90-colheitadeira.glb", materials, "SNC Energies procedural harvester authoring",
               "SNC 90 (metres)", "drive_and_mechanisms_demo", spin)
    print(f"SNC 90: {len(scene.cubes)} cubes, {len(scene.groups)} articulated groups, {len(materials)} materials. Exported JSON, Blockbench and GLB.")


if __name__ == "__main__":
    main()
