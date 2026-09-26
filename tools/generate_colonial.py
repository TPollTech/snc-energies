"""Canonical native models, recipes and translations for the Colonial workshop."""
import json
import base64
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources'
ASSETS = RES / 'assets/snc_energies'
DATA = RES / 'data/snc_energies'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def recipe(name, pattern, keys, result=None, count=1):
    write(DATA / f'recipe/{name}.json', dict(type='minecraft:crafting_shaped', category='misc',
          pattern=pattern, key=keys, result=dict(id=result or f'snc_energies:{name}', count=count)))


def shapeless(name, ingredients, result=None, count=1):
    write(DATA / f'recipe/{name}.json', dict(type='minecraft:crafting_shapeless', category='misc',
          ingredients=ingredients, result=dict(id=result or f'snc_energies:{name}', count=count)))


# All coordinates are in model pixels, north-facing. Four bounded cell models.
WOOD, IRON, STONE = 'colonial_wood', 'colonial_iron', 'colonial_stone'
COMMON = [(0, 0, 1, 32, 3, 15, WOOD), (2, 3, 2, 5, 22, 5, WOOD),
          (27, 3, 2, 30, 22, 5, WOOD), (2, 3, 11, 5, 22, 14, WOOD),
          (27, 3, 11, 30, 22, 14, WOOD), (1, 18, 1, 31, 21, 15, WOOD)]
GEOMETRY = {
    'grain_mill': COMMON + [
        (5, 7, 3, 27, 12, 13, STONE), (5, 13, 3, 27, 18, 13, STONE),
        (14, 6, 6, 18, 24, 10, IRON), (9, 21, 3, 11, 30, 13, WOOD),
        (21, 21, 3, 23, 30, 13, WOOD), (11, 21, 3, 21, 30, 5, WOOD),
        (11, 21, 11, 21, 30, 13, WOOD), (9, 29, 3, 23, 30, 5, IRON),
        (9, 29, 11, 23, 30, 13, IRON), (9, 29, 5, 11, 30, 11, IRON), (21, 29, 5, 23, 30, 11, IRON),
        (6, 7, 0, 13, 9, 4, IRON), (0, 10, 6, 5, 12, 8, IRON),
        (0, 11, 6, 2, 17, 8, IRON), (0, 16, 7, 2, 18, 13, WOOD)],
    'seed_press': COMMON + [
        (4, 26, 2, 28, 30, 14, WOOD), (14, 11, 5, 18, 28, 9, IRON),
        (6, 10, 3, 26, 13, 13, IRON), (7, 5, 4, 25, 7, 13, IRON),
        (6, 7, 3, 9, 11, 14, WOOD), (23, 7, 3, 26, 11, 14, WOOD),
        (9, 7, 12, 23, 11, 14, WOOD), (5, 23, 6, 27, 25, 8, WOOD),
        (10, 3, 1, 22, 5, 5, IRON)]
}


def element(coords):
    x, y, z, xx, yy, zz, texture = coords
    return dict(**{'from': [x, y, z], 'to': [xx, yy, zz]},
                faces={face: dict(texture='#' + texture, uv=[0, 0, 16, 16])
                       for face in ['north', 'south', 'east', 'west', 'up', 'down']})


for name, boxes in GEOMETRY.items():
    textures = {t: f'snc_energies:block/{t}' for t in [WOOD, IRON, STONE]}
    textures['particle'] = textures[WOOD]
    variants = {}
    for part in range(4):
        ox, oy = part % 2 * 16, part // 2 * 16
        elements = []
        for x, y, z, xx, yy, zz, texture in boxes:
            a, b, c, d = max(x, ox), max(y, oy), min(xx, ox + 16), min(yy, oy + 16)
            if a >= c or b >= d:
                continue
            elements.append(element((a - ox, b - oy, z, c - ox, d - oy, zz, texture)))
        write(ASSETS / f'models/block/{name}_{part}.json', dict(textures=textures, elements=elements))
        for facing, rotation in [('north', 0), ('east', 90), ('south', 180), ('west', 270)]:
            variants[f'facing={facing},part={part}'] = dict(model=f'snc_energies:block/{name}_{part}', y=rotation)
    write(ASSETS / f'blockstates/{name}.json', dict(variants=variants))
    # Whole assembly scaled to one inventory model, without exceeding native bounds.
    item_boxes = [(x/2, y/2, z/2+4, xx/2, yy/2, zz/2+4, tex) for x,y,z,xx,yy,zz,tex in boxes]
    write(ASSETS / f'models/item/{name}.json', dict(textures=textures, elements=[element(b) for b in item_boxes],
        display=dict(gui=dict(rotation=[25, 225, 0], translation=[0, 0, 0], scale=[.8,.8,.8]),
                     ground=dict(rotation=[0,0,0], translation=[0,3,0], scale=[.35,.35,.35]),
                     fixed=dict(rotation=[0,180,0], translation=[0,0,0], scale=[.65,.65,.65]))))
    write(ASSETS / f'items/{name}.json', dict(model=dict(type='minecraft:model', model=f'snc_energies:item/{name}')))
    write(DATA / f'loot_table/blocks/{name}.json', dict(type='minecraft:block', pools=[dict(rolls=1,
        entries=[dict(type='minecraft:item', name=f'snc_energies:{name}', condition=dict(type='minecraft:survives_explosion'))],
        condition=dict(type='minecraft:match_block', blocks=f'snc_energies:{name}', state=dict(part='0')))]))

ITEMS = {
 'mate_seeds': ('Sementes de Erva-mate', 'Yerba Mate Seeds'), 'mate_leaf': ('Folhas de Erva-mate', 'Yerba Mate Leaves'),
 'dried_mate': ('Erva-mate Seca', 'Dried Yerba Mate'), 'ground_mate': ('Erva-mate Moída', 'Ground Yerba Mate'),
 'mate_infusion': ('Infusão de Erva-mate', 'Yerba Mate Infusion'),
 'rice_seeds': ('Sementes de Arroz', 'Rice Seeds'), 'soy_seeds': ('Sementes de Soja', 'Soybean Seeds'),
 'rice_paddy': ('Arroz em Casca', 'Paddy Rice'), 'rice': ('Arroz Beneficiado', 'Milled Rice'),
 'soybean': ('Soja', 'Soybeans'), 'flour': ('Farinha', 'Flour'), 'rice_husk': ('Casca de Arroz', 'Rice Husk'),
 'vegetable_oil': ('Porção de Óleo Vegetal', 'Vegetable Oil Portion'), 'soy_meal': ('Farelo de Soja', 'Soybean Meal'),
 'biomass_briquette': ('Briquete de Biomassa', 'Biomass Briquette'), 'field_guide': ('Caderno da Oficina SNC', 'SNC Workshop Field Guide')
}
for name in ITEMS:
    write(ASSETS / f'items/{name}.json', dict(model=dict(type='minecraft:model', model=f'snc_energies:item/{name}')))
    write(ASSETS / f'models/item/{name}.json', dict(parent='minecraft:item/generated', textures=dict(layer0=f'snc_energies:item/{name}')))

for crop, seed, product in [('rice','rice_seeds','rice_paddy'), ('soy','soy_seeds','soybean'), ('mate','mate_seeds','mate_leaf')]:
    variants = {}
    for age in range(8):
        variants[f'age={age}'] = dict(model=f'snc_energies:block/{crop}_stage{age}')
        write(ASSETS / f'models/block/{crop}_stage{age}.json', dict(parent='minecraft:block/cross', textures=dict(cross=f'snc_energies:block/{crop}_stage{age}')))
    write(ASSETS / f'blockstates/{crop}_crop.json', dict(variants=variants))
    mature = dict(type='minecraft:match_block', blocks=f'snc_energies:{crop}_crop', state=dict(age='7'))
    write(DATA / f'loot_table/blocks/{crop}_crop.json', dict(type='minecraft:block', pools=[
        dict(rolls=1, entries=[dict(type='minecraft:item', name=f'snc_energies:{seed}')]),
        dict(rolls=1, condition=mature, entries=[dict(type='minecraft:item', name=f'snc_energies:{seed}')]),
        dict(rolls=1, condition=mature, entries=[dict(type='minecraft:item', name=f'snc_energies:{product}',
             modifier=dict(type='minecraft:set_count', count=2))])]))

recipe('grain_mill', ['PIP','SSS','P P'], dict(P='#minecraft:planks', I='minecraft:iron_ingot', S='minecraft:smooth_stone'))
recipe('seed_press', ['PIP','PIP','SCS'], dict(P='#minecraft:planks', I='minecraft:iron_ingot', S='minecraft:stone', C='minecraft:copper_ingot'))
shapeless('rice_seeds', ['minecraft:wheat_seeds','minecraft:wheat'])
shapeless('soy_seeds', ['minecraft:wheat_seeds','minecraft:beetroot_seeds'])
shapeless('mate_seeds', ['minecraft:wheat_seeds','minecraft:oak_sapling'])
shapeless('mate_infusion', ['snc_energies:ground_mate','minecraft:bowl','minecraft:water_bucket'])
write(DATA / 'recipe/dried_mate.json',dict(type='minecraft:smelting',category='food',ingredient='snc_energies:mate_leaf',
      result=dict(id='snc_energies:dried_mate'),experience=.1,cookingtime=200))
shapeless('field_guide', ['minecraft:book','minecraft:wheat_seeds'])
shapeless('flour_bread', ['snc_energies:flour'] * 3, 'minecraft:bread')
shapeless('rice_bread', ['snc_energies:rice'] * 3, 'minecraft:bread')
shapeless('soy_fertilizer', ['snc_energies:soy_meal'] * 3, 'minecraft:bone_meal')

for tag in ['mineable/axe', 'mineable/pickaxe']:
    path = RES / f'data/minecraft/tags/block/{tag}.json'
    data = json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else dict(replace=False, values=[])
    for name in GEOMETRY:
        if f'snc_energies:{name}' not in data['values']: data['values'].append(f'snc_energies:{name}')
    write(path, data)
write(RES / 'data/minecraft/tags/block/crops.json', dict(replace=False, values=['snc_energies:rice_crop','snc_energies:soy_crop','snc_energies:mate_crop']))

TEXT = {
 'block.snc_energies.grain_mill': ('Moinho Colonial', 'Colonial Grain Mill'),
 'block.snc_energies.seed_press': ('Prensa Colonial', 'Colonial Seed Press'),
 'block.snc_energies.rice_crop': ('Arroz', 'Rice Crop'), 'block.snc_energies.soy_crop': ('Soja', 'Soybean Crop'),
 'block.snc_energies.mate_crop': ('Erva-mate', 'Yerba Mate'),
 'gui.snc_energies.crank': ('Manivela', 'Crank'),
 'gui.snc_energies.search': ('Buscar no caderno', 'Search the guide'),
 'gui.snc_energies.search_hint': ('Digite para buscar páginas…', 'Type to search pages...'),
 'gui.snc_energies.no_results': ('Nada encontrado. Tente outra palavra.', 'No matches. Try another word.'),
 'gui.snc_energies.matches': ('Página %s de %s', 'Page %s of %s'),
 'gui.snc_energies.index': ('Índice', 'Index'),
 'gui.snc_energies.workshop_status.0': ('Falta insumo', 'Need input'),
 'gui.snc_energies.workshop_status.1': ('Gire a manivela', 'Crank required'),
 'gui.snc_energies.workshop_status.2': ('Trabalhando', 'Working'),
 'gui.snc_energies.workshop_status.3': ('Saída cheia', 'Output blocked'),
 'guide.snc_energies.title.0': ('01 / A oficina colonial', '01 / The colonial workshop'),
 'guide.snc_energies.body.0': ('Este é o primeiro tier jogável da expansão. Fabrique moinho, prensa e fogão. Plante arroz e soja; processe os grãos e use os resíduos como combustível. Os tiers Vapor, Eletromecânico, Agroindustrial e Voltaico ainda estão em desenvolvimento. As máquinas elétricas antigas continuam disponíveis.', 'This is the first playable expansion tier. Craft a mill, press and stove. Grow rice and soybeans, process grain and burn the residues. Steam, Electromechanical, Agroindustrial and Voltaic tiers are still in development. Existing electric machines remain available.'),
 'guide.snc_energies.title.1': ('02 / Sementes e montagem', '02 / Seeds and assembly'),
 'guide.snc_energies.body.1': ('Semente de arroz: semente de trigo + trigo. Semente de soja: semente de trigo + semente de beterraba. Plante em terra arada, com água próxima e luz. Farinha de osso acelera o crescimento. Plantas maduras rendem 2 produtos e 2 sementes. Reserve espaço de 2×1 no chão e 2 blocos de altura para moinho/prensa; a estrutura cresce à sua direita ao olhar para a frente dela. Quebrar qualquer parte desmonta tudo e devolve o inventário.', 'Rice seed: wheat seeds + wheat. Soybean seed: wheat seeds + beetroot seeds. Plant on farmland near water and light. Bonemeal speeds growth. Mature crops yield 2 products and 2 seeds. Mills and presses require a 2×1 footprint and 2 blocks of height. Breaking any part dismantles the structure and returns the inventory.'),
 'guide.snc_energies.title.2': ('03 / Moinho — receitas', '03 / Mill recipes'),
 'guide.snc_energies.body.2': ('Entrada à esquerda; produto e resíduo à direita. Gire a manivela para acumular trabalho (até 4 cargas). Nenhum trabalho é gasto se faltar ingrediente ou houver saída bloqueada. Três farinhas ou três arrozes beneficiados fazem um pão na bancada.', 'Input on the left; product and residue on the right. Crank to store work (up to 4 charges). No work is spent while input is missing or output is blocked. Craft three flour or three milled rice into bread.'),
 'guide.snc_energies.title.3': ('04 / Prensa — receitas', '04 / Press recipes'),
 'guide.snc_energies.body.3': ('A prensa não substitui a prensa de uvas do Adventures. Processa soja e compacta cascas. Três farelos fazem uma farinha de osso. Óleo é uma porção de ingrediente, sem garrafa retornável. Mantenha as duas saídas livres.', 'This press does not replace the Adventures grape press. It processes soybeans and compacts husks. Three soybean meal craft into bonemeal. Oil is an ingredient portion with no returnable bottle. Keep both output slots available.'),
 'guide.snc_energies.title.4': ('05 / Resíduos e energia', '05 / Residues and power'),
 'guide.snc_energies.body.4': ('No fogão: uma casca de arroz gera 8.000 E; uma porção de óleo gera 32.000 E; um briquete gera 40.000 E. Compactar quatro cascas melhora o rendimento. O fogão conserva seu combustível enquanto cheio. Sua estrutura ocupa 3×2 no chão, com chaminé até 3 blocos de altura.', 'In the stove: rice husk produces 8,000 E; vegetable oil 32,000 E; biomass briquette 40,000 E. Compacting four husks improves yield. A full stove pauses fuel consumption. Its footprint is 3×2 with a chimney reaching 3 blocks high.'),
 'guide.snc_energies.title.5': ('06 / SNC Adventures', '06 / SNC Adventures'),
 'guide.snc_energies.body.5': ('Com Adventures instalado, o bagaço de cana da moenda também alimenta o fogão: 8.000 E por unidade. Bebidas, dinheiro, receitas e lâmpadas UV mantêm suas regras originais. Motorização das máquinas Adventures, tubos e UV elétrico ainda não foram implementados. Nenhum dos mods depende do outro para iniciar.', 'With Adventures installed, bagasse from its mill fuels the stove: 8,000 E per item. Drinks, money, recipes and UV lamps keep their original rules. Adventures machine motors, pipes and powered UV are not implemented yet. Both mods can start independently.'),
 'guide.snc_energies.title.6': ('07 / Operação e transporte', '07 / Operation and transport'),
 'guide.snc_energies.body.6': ('Funis inserem ingredientes por cima ou pelos lados do controlador (base esquerda, olhando a frente); por baixo retiram produto e resíduo. As outras partes abrem o mesmo menu, mas não possuem inventários separados. A manivela é manual neste tier. Progresso e trabalho são salvos; a máquina para em chunks descarregados. O caderno é feito com livro + semente de trigo.', 'Hoppers insert ingredients above or beside the controller (bottom left from the front); below they extract products and residues. Other parts share the menu but have no separate inventory. Cranking is manual in this tier. Progress and work are saved; unloaded chunks pause processing. Craft this guide with a book and wheat seeds.')
}
TEXT.update({f'item.snc_energies:{k}'.replace(':', '.'): v for k, v in ITEMS.items()})
for index, language in enumerate(['pt_br', 'en_us']):
    path = ASSETS / f'lang/{language}.json'
    data = json.loads(path.read_text(encoding='utf-8-sig'))
    data.update({key: pair[index] for key, pair in TEXT.items()})
    write(path, data)

# Preview consumes the same source geometry, eliminating an independent model copy.
write(ROOT / 'previews/colonial-models.json', {name: [element(b) for b in boxes] for name, boxes in GEOMETRY.items()})
print('Colonial models, recipes, crop loot and translations generated.')

# Reuse the native-model preview renderer, sourcing the actual production geometry/textures.
template = (ROOT / 'previews/fogao-a-lenha.html').read_text(encoding='utf-8')
renderer = template[template.index('function render()'):template.index('</script>')]
renderer = renderer.replace('x-=24;y-=20;z-=16', 'x-=16;y-=15;z-=8').replace('xx*9', 'xx*12').replace('))*9', '))*12')
models = {key: dict(elements=[element(b) for b in GEOMETRY[name]],
                    textures={t: f'snc_energies:block/{t}' for t in [WOOD,IRON,STONE]})
          for key, name in [('off','grain_mill'),('on','seed_press')]}
sources = {f'snc_energies:block/{t}': 'data:image/png;base64,' + base64.b64encode(
            (ASSETS / f'textures/block/{t}.png').read_bytes()).decode() for t in [WOOD,IRON,STONE]}
gallery = ''.join(f'<figure><img width="80" height="80" src="data:image/png;base64,{base64.b64encode((ASSETS / ("textures/item/"+name+".png")).read_bytes()).decode()}"><figcaption>{labels[0]}</figcaption></figure>' for name,labels in ITEMS.items())
html = '''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>SNC · Oficina Colonial</title>
<style>body{margin:0;background:#1c231c;color:#eee1c2;font:16px/1.6 system-ui}main{max-width:1160px;margin:auto;padding:35px}h1{font-size:46px;line-height:1.1}small{color:#b9c29f}.views{display:grid;grid-template-columns:1fr 1fr;gap:22px}article{background:#2a3328;border:1px solid #4b5941;border-radius:12px;padding:22px}canvas{width:100%;aspect-ratio:1.2}.gallery{display:flex;flex-wrap:wrap;gap:12px}figure{margin:0;width:130px;padding:10px;background:#303b2c;text-align:center;font-size:12px}img{image-rendering:pixelated}button{padding:10px;background:#c9a36a;border:0;border-radius:6px}footer{color:#b9c29f}code{color:#dfbc77}@media(max-width:650px){.views{grid-template-columns:1fr}}</style>
<main><small>SNC ENERGIES 0.2.0 · PRIMEIRO TIER</small><h1>Oficina Colonial</h1><p>Modelos reais do moinho e da prensa: madeira, pedra e ferro, com quatro células e um único inventário.<br>2 blocos de largura × 1 de profundidade × 2 de altura reservada.</p>
<label>Girar <input id="angle" type="range" min="0" max="360" value="145"></label> <button id="spin">Rotação automática</button>
<div class="views"><article><canvas id="off" width="720" height="600"></canvas><h2>Moinho Colonial</h2><p>Arroz → grão + casca. Trigo → farinha.<br>2 tábuas nas pontas de cima + ferro no centro; 3 pedras lisas no meio; 2 tábuas nas pontas de baixo.</p></article><article><canvas id="on" width="720" height="600"></canvas><h2>Prensa Colonial</h2><p>Soja → óleo + farelo. Cascas → briquete.<br>2 linhas de tábua / ferro / tábua; base de pedra / cobre / pedra.</p></article></div>
<h2>Lavouras e aproveitamento</h2><div class="gallery">''' + gallery + '''</div>
<h2>Como começar</h2><p>Fabrique o caderno com <b>livro + semente de trigo</b>. Ele reúne as receitas e instruções.<br>Arroz: semente de trigo + trigo. Soja: semente de trigo + semente de beterraba.<br>Abra a máquina, coloque o ingrediente e use o botão <b>Manivela</b>. Produtos e resíduos têm saídas separadas.</p>
<p>Funis funcionam no controlador, na base esquerda olhando a frente: entrada por cima/lados, retirada por baixo. O fogão aceita casca, óleo, briquete e bagaço do Adventures.</p>
<footer>Prévia dos recursos reais, iluminação aproximada. Texturas 128×128. Oficina Colonial dos cinco tiers. <a href="industria.html">Ver máquinas industriais</a>.</footer></main><script>'''
html += 'const models=' + json.dumps(models) + ',sources=' + json.dumps(sources) + ',textures={};let spinning=false;\n' + renderer + '</script></html>'
(ROOT / 'previews/colonial.html').write_text(html, encoding='utf-8')
