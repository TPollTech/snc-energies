# Builds a self-contained HTML gallery comparing old (backup) vs new textures.
import base64
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'src', 'main', 'resources', 'assets', 'snc_energies')
OLD = os.path.join(ROOT, 'tools', 'backup-textures-v1')

SECTIONS = [
    ('Blocos · Máquinas', 'block', ['machine_side', 'machine_top', 'machine_bottom',
        'coal_generator_front_off', 'coal_generator_front_on', 'coal_generator_side',
        'electric_furnace_side', 'electric_furnace_front_off', 'electric_furnace_front_on',
        'crusher_side', 'crusher_front_off', 'crusher_front_on', 'crusher_top',
        'energy_cube', 'energy_cube_top', 'cable_core', 'cable_arm']),
    ('Blocos · Minérios', 'block', ['voltaite_ore', 'deepslate_voltaite_ore', 'tin_ore', 'deepslate_tin_ore']),
    ('Blocos · Colonial e fogão', 'block', ['colonial_wood', 'colonial_iron', 'colonial_stone',
        'wood_stove_brick', 'wood_stove_iron', 'wood_stove_cooktop', 'wood_stove_door_off',
        'wood_stove_door_on', 'wood_stove_oven', 'wood_stove_wood']),
    ('Blocos · Indústria', 'block', ['industry_steel', 'industry_bronze', 'industry_panel',
        'industry_core', 'industry_gauge']),
    ('Colheitas (amostra)', 'block', ['rice_stage0', 'rice_stage4', 'rice_stage7',
        'soy_stage2', 'soy_stage7', 'mate_stage7']),
    ('Itens · Metais e circuitos', 'item', ['tin_ingot', 'bronze_ingot', 'steel_ingot',
        'steel_plate', 'insulated_plate', 'steel_gear', 'copper_wire',
        'basic_circuit', 'advanced_circuit', 'refined_voltaite', 'mineral_matrix']),
    ('Itens · Poeiras e brutos', 'item', ['raw_voltaite', 'voltaite_dust', 'voltaite_ingot',
        'raw_tin', 'sawdust', 'iron_dust', 'copper_dust', 'gold_dust', 'tin_dust']),
    ('Itens · Colonial', 'item', ['mate_seeds', 'mate_leaf', 'dried_mate', 'ground_mate',
        'mate_infusion', 'rice', 'rice_paddy', 'soybean', 'flour', 'rice_husk',
        'vegetable_oil', 'biomass_briquette', 'field_guide', 'rice_seeds', 'soy_seeds']),
    ('GUIs', 'gui', ['gui_coal_generator', 'gui_electric_furnace', 'gui_crusher']),
]


def b64(path):
    with open(path, 'rb') as fh:
        return base64.b64encode(fh.read()).decode()


def card(folder, name, big=False):
    new_path = os.path.join(ASSETS, 'textures', folder, name + '.png')
    old_path = os.path.join(OLD, folder, name + '.png')
    cls = 'big' if big else ''
    old_html = ''
    if os.path.exists(old_path):
        old_html = f"<figure class='{cls}'><img src='data:image/png;base64,{b64(old_path)}'><figcaption>antes</figcaption></figure>"
    return (f"<div class='pair'><div class='pairhead'>{name}</div>"
            f"{old_html}"
            f"<figure class='{cls}'><img src='data:image/png;base64,{b64(new_path)}'><figcaption>agora</figcaption></figure></div>")


html = ["<!DOCTYPE html><html lang='pt-br'><head><meta charset='utf-8'><title>SNC Energies — Texturas antes/depois</title><style>",
        "body{background:#171a1f;color:#e8e4da;font-family:'Segoe UI',sans-serif;margin:24px}",
        "h1{color:#40dcff}h2{color:#ffca5c;margin-top:36px;border-bottom:1px solid #33363f;padding-bottom:4px}",
        ".grid{display:flex;flex-wrap:wrap;gap:16px}",
        ".pair{background:#22252c;border:1px solid #33363f;border-radius:10px;padding:10px}",
        ".pairhead{font-family:Consolas,monospace;font-size:12px;color:#a0a4ac;margin-bottom:6px;max-width:280px}",
        ".grid figure{margin:0;text-align:center}",
        ".grid img{width:128px;image-rendering:auto;background:#3a3d47;border-radius:4px;display:block}",
        ".grid img.big{width:280px}",
        ".grid figcaption{font-size:11px;color:#8a8f98;margin-top:4px}",
        ".legend{color:#b6bca3;font-size:14px}",
        "</style></head><body>",
        "<h1>SNC Energies — Regeneração de texturas</h1>",
        "<p class='legend'>Cada par mostra <b>antes</b> (esquerda, versão chapada) e <b>agora</b> (direita, ruído fractal + bevel + materiais). 128×128; GUIs 176×166.</p>"]
for title, folder, names in SECTIONS:
    big = folder == 'gui'
    html.append(f"<h2>{title}</h2><div class='grid'>")
    for name in names:
        html.append(card(folder, name, big))
    html.append('</div>')
with open(os.path.join(ROOT, 'previews', 'texturas-v2.html'), 'w', encoding='utf-8') as fh:
    fh.write(''.join(html))
print('Gallery: previews/texturas-v2.html')
