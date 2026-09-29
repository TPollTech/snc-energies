"""Canonical resources for the steam-to-Voltaite progression, Minecraft 26.3."""
import json
from pathlib import Path
from generate_colonial import ROOT, RES, ASSETS, DATA, write as write_resource, recipe, shapeless, element

def write(path,value):
    if path.exists() and json.loads(path.read_text(encoding='utf-8-sig'))==value:return
    write_resource(path,value)

KINDS={
 'biomass_boiler':(2,2,3,'Caldeira de Biomassa','Biomass Boiler'),
 'steam_sawmill':(3,2,2,'Serraria a Vapor','Steam Sawmill'),
 'steam_foundry':(2,2,2,'Fundição a Vapor','Steam Foundry'),
 'steam_turbine':(2,2,2,'Turbina a Vapor','Steam Turbine'),
 'laminator':(2,1,2,'Laminador','Rolling Mill'),
 'grain_dryer':(3,3,4,'Secador Industrial','Industrial Dryer'),
 'oil_extractor':(3,2,2,'Extratora de Óleo','Oil Extractor'),
 'voltaic_refinery':(3,3,3,'Refinaria Voltaica','Voltaic Refinery'),
 'mineral_synthesizer':(3,3,3,'Sintetizador Mineral','Mineral Synthesizer')}
STEEL,BRONZE,BRICK,CORE='industry_steel','industry_bronze','wood_stove_brick','industry_core'
GEOMETRY={}
for name,(w,d,h,pt,en) in KINDS.items():
    x,z,y=w*16,d*16,h*16
    boxes=[(0,0,0,x,3,z,STEEL)]
    if name=='biomass_boiler':
        boxes += [(1,3,1,x-1,13,z-1,BRICK),(3,13,3,x-3,31,z-3,BRONZE),
            (2,30,2,x-2,33,z-2,STEEL),(23,33,23,29,48,29,STEEL),
            (9,4,0,23,11,2,STEEL),(12,19,1,20,27,3,'industry_gauge')]
    elif name=='steam_sawmill':
        boxes += [(2,3,2,7,18,30,STEEL),(41,3,2,46,18,30,STEEL),(0,17,0,48,20,32,BRONZE),
            (9,20,7,39,23,13,'colonial_wood'),(22,20,15,25,31,28,STEEL),
            (1,5,20,15,15,29,BRONZE),(23,10,4,25,28,30,STEEL)]
    elif name=='steam_foundry':
        boxes += [(2,3,2,30,24,30,BRICK),(1,24,1,31,28,31,STEEL),(10,6,0,22,17,3,STEEL),
            (22,28,22,29,32,29,BRONZE),(8,28,8,24,30,24,BRONZE)]
    elif name=='steam_turbine':
        boxes += [(4,3,4,28,23,28,STEEL),(2,10,1,30,19,5,BRONZE),(10,6,0,22,24,2,STEEL),
            (12,10,0,20,20,3,'industry_gauge'),(26,7,10,32,19,22,BRONZE),
            (7,23,7,25,27,25,STEEL)]
    elif name=='laminator':
        boxes += [(2,3,2,7,25,14,STEEL),(23,3,2,28,25,14,STEEL),
            (7,9,3,23,14,13,BRONZE),(7,17,3,23,22,13,STEEL),(1,14,0,31,16,16,STEEL),
            (0,4,5,7,10,12,BRONZE),(3,25,3,27,28,13,STEEL)]
    elif name=='grain_dryer':
        boxes += [(2,3,2,7,17,46,STEEL),(41,3,2,46,17,46,STEEL),
            (2,16,2,46,56,46,'industry_panel'),(0,56,0,48,60,48,STEEL),
            (4,60,4,44,64,44,STEEL),(10,4,0,38,16,12,BRONZE),
            (15,5,0,33,15,2,'industry_gauge')]
    elif name=='oil_extractor':
        boxes += [(2,3,3,17,25,29,STEEL),(27,3,3,44,24,29,BRONZE),
            (17,9,12,30,17,20,STEEL),(4,25,5,15,31,27,BRONZE),
            (30,24,6,41,28,26,STEEL),(6,10,1,13,18,4,'industry_gauge')]
    elif name=='voltaic_refinery':
        for px in [1,x-7]:
            for pz in [1,z-7]: boxes.append((px,3,pz,px+6,y-4,pz+6,STEEL))
        boxes += [(0,y-4,0,x,y,z,STEEL),(10,8,10,x-10,y-10,z-10,CORE),
            (8,4,1,x-8,12,6,BRONZE),(15,17,0,33,29,3,'industry_gauge')]
        boxes += [(3,14,8,8,38,38,BRONZE),(40,8,9,46,36,38,BRONZE)]
    else:
        # Synthesis: open octagonal containment rings and a suspended crystal.
        # The refinery retains its existing four-column tank enclosure unchanged.
        boxes += [(5,3,5,43,6,43,STEEL),(13,6,13,35,9,35,BRONZE),
            (18,9,18,30,12,30,CORE),(22,16,22,26,18,26,CORE),
            (19,18,19,29,22,29,CORE),(16,22,16,32,29,32,CORE),
            (19,29,19,29,33,29,CORE),(22,33,22,26,36,26,CORE),
            (1,3,21,6,38,27,STEEL),(42,3,21,47,38,27,STEEL),
            (1,38,21,10,42,27,BRONZE),(38,38,21,47,42,27,BRONZE),
            (3,4,0,15,12,7,STEEL),(4,12,0,14,17,6,'industry_gauge')]
        for bottom in [12,37]:
            boxes += [(12,bottom,5,36,bottom+3,9,BRONZE),(12,bottom,39,36,bottom+3,43,BRONZE),
                (5,bottom,12,9,bottom+3,36,BRONZE),(39,bottom,12,43,bottom+3,36,BRONZE)]
            for a,c in [(8,8),(34,8),(8,34),(34,34)]:boxes.append((a,bottom,c,a+6,bottom+3,c+6,STEEL))
        boxes += [(21,42,21,27,46,27,CORE),(18,46,18,30,48,30,STEEL)]
    GEOMETRY[name]=boxes
    textures={t:f'snc_energies:block/{t}' for t in {b[6] for b in boxes}};textures['particle']=f'snc_energies:block/{STEEL}'
    variants={}
    for part in range(36):
        ox=(part%w)*16;oz=((part//w)%d)*16;oy=(part//(w*d))*16
        elements=[]
        if part<w*d*h:
            for a,b,c,aa,bb,cc,texture in boxes:
                lower=[max(a,ox),max(b,oy),max(c,oz)];upper=[min(aa,ox+16),min(bb,oy+16),min(cc,oz+16)]
                if any(lo>=hi for lo,hi in zip(lower,upper)):continue
                elements.append(element((*[v-o for v,o in zip(lower,[ox,oy,oz])],*[v-o for v,o in zip(upper,[ox,oy,oz])],texture)))
        write(ASSETS/f'models/block/{name}_{part}.json',dict(textures=textures,elements=elements))
        for facing,rotation in [('north',0),('east',90),('south',180),('west',270)]:
            variants[f'facing={facing},part={part}']=dict(model=f'snc_energies:block/{name}_{part}',y=rotation)
    write(ASSETS/f'blockstates/{name}.json',dict(variants=variants))
    scale=1/max(w,d,h)
    model=[element((a*scale+(16-x*scale)/2,b*scale,c*scale+(16-z*scale)/2,aa*scale+(16-x*scale)/2,bb*scale,cc*scale+(16-z*scale)/2,t)) for a,b,c,aa,bb,cc,t in boxes]
    write(ASSETS/f'models/item/{name}.json',dict(textures=textures,elements=model,display=dict(gui=dict(rotation=[25,225,0],translation=[0,0,0],scale=[.9,.9,.9]))))
    write(ASSETS/f'items/{name}.json',dict(model=dict(type='minecraft:model',model=f'snc_energies:item/{name}')))
    write(DATA/f'loot_table/blocks/{name}.json',dict(type='minecraft:block',pools=[dict(rolls=1,
        condition=dict(type='minecraft:match_block',blocks=f'snc_energies:{name}',state=dict(part='0')),
        entries=[dict(type='minecraft:item',name=f'snc_energies:{name}',condition=dict(type='minecraft:survives_explosion'))])]))

# Same thin, bounded geometry as cables; distinct bronze texture and steam-only network.
for suffix in ['', '_down','_up','_north','_south','_west','_east']:
    model=json.loads((ASSETS/f'models/block/energy_cable{suffix}.json').read_text(encoding='utf-8-sig'))
    model['textures']={key:f'snc_energies:block/{BRONZE}' for key in model['textures']}
    write(ASSETS/f'models/block/steam_pipe{suffix}.json',model)
state=json.loads((ASSETS/'blockstates/energy_cable.json').read_text(encoding='utf-8-sig'))
write(ASSETS/'blockstates/steam_pipe.json',json.loads(json.dumps(state).replace('energy_cable','steam_pipe')))
write(ASSETS/'items/steam_pipe.json',dict(model=dict(type='minecraft:model',model='snc_energies:item/steam_pipe')))
pipe_item=json.loads((ASSETS/'models/item/energy_cable.json').read_text(encoding='utf-8-sig'))
pipe_item['textures']={key:f'snc_energies:block/{BRONZE}' for key in pipe_item.get('textures',{})}
write(ASSETS/'models/item/steam_pipe.json',pipe_item)
write(DATA/'loot_table/blocks/steam_pipe.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='snc_energies:steam_pipe')])]))

# Item ducts share the cable silhouette; galvanized shell textures keep the amber stripe.
DUCT_TEXTURES={'particle':'snc_energies:block/item_pipe_core','core':'snc_energies:block/item_pipe_core','arm':'snc_energies:block/item_pipe_arm'}
for suffix in ['', '_down','_up','_north','_south','_west','_east']:
    model=json.loads((ASSETS/f'models/block/energy_cable{suffix}.json').read_text(encoding='utf-8-sig'))
    model['textures']={key:DUCT_TEXTURES.get(key,f'snc_energies:block/item_pipe_core') for key in model['textures']}
    write(ASSETS/f'models/block/item_pipe{suffix}.json',model)
write(ASSETS/'blockstates/item_pipe.json',json.loads(json.dumps(state).replace('energy_cable','item_pipe')))
write(ASSETS/'items/item_pipe.json',dict(model=dict(type='minecraft:model',model='snc_energies:item/item_pipe')))
pipe_item=json.loads((ASSETS/'models/item/energy_cable.json').read_text(encoding='utf-8-sig'))
pipe_item['textures']={key:DUCT_TEXTURES.get(key,f'snc_energies:block/item_pipe_core') for key in pipe_item.get('textures',{})}
write(ASSETS/'models/item/item_pipe.json',pipe_item)
write(DATA/'loot_table/blocks/item_pipe.json',dict(type='minecraft:block',pools=[dict(rolls=1,entries=[dict(type='minecraft:item',name='snc_energies:item_pipe')])]))

# Screwdriver: the side-configuration tool (flat slotted tip, orange grip).
write(ASSETS/'models/item/screwdriver.json',dict(parent='minecraft:item/generated',textures=dict(layer0='snc_energies:item/screwdriver')))
write(ASSETS/'items/screwdriver.json',dict(model=dict(type='minecraft:model',model='snc_energies:item/screwdriver')))

MATERIALS={
 'raw_tin':('Estanho Bruto','Raw Tin'),'tin_ingot':('Barra de Estanho','Tin Ingot'),
 'bronze_ingot':('Barra de Bronze','Bronze Ingot'),'steel_ingot':('Barra de Aço','Steel Ingot'),
 'steel_plate':('Chapa de Aço','Steel Plate'),'copper_wire':('Fio de Cobre','Copper Wire'),
 'steel_gear':('Engrenagem de Aço','Steel Gear'),'basic_circuit':('Circuito Eletromecânico','Electromechanical Circuit'),
 'insulated_plate':('Chapa Isolada','Insulated Plate'),'refined_voltaite':('Voltaite Refinada','Refined Voltaite'),
 'advanced_circuit':('Circuito Voltaico','Voltaic Circuit'),'mineral_matrix':('Matriz Mineral','Mineral Matrix'),
 'sawdust':('Serragem','Sawdust'),'iron_dust':('Pó de Ferro','Iron Dust'),'copper_dust':('Pó de Cobre','Copper Dust'),
 'gold_dust':('Pó de Ouro','Gold Dust'),'tin_dust':('Pó de Estanho','Tin Dust')}
for name in MATERIALS:
    write(ASSETS/f'items/{name}.json',dict(model=dict(type='minecraft:model',model=f'snc_energies:item/{name}')))
    write(ASSETS/f'models/item/{name}.json',dict(parent='minecraft:item/generated',textures=dict(layer0=f'snc_energies:item/{name}')))
for name in ['tin_ore','deepslate_tin_ore']:
    write(ASSETS/f'blockstates/{name}.json',dict(variants={'':dict(model=f'snc_energies:block/{name}')}))
    write(ASSETS/f'models/block/{name}.json',dict(parent='minecraft:block/cube_all',textures=dict(all=f'snc_energies:block/{name}')))
    write(ASSETS/f'items/{name}.json',dict(model=dict(type='minecraft:model',model=f'snc_energies:block/{name}')))
    # Copy the version-matched vanilla copper ore loot structure, retaining silk touch and fortune.
    import zipfile
    mc=Path.home()/'.gradle/caches/fabric-loom/minecraftMaven/net/minecraft/minecraft-merged-deobf/26.3/minecraft-merged-deobf-26.3.jar'
    with zipfile.ZipFile(mc) as archive: vanilla=archive.read('data/minecraft/loot_table/blocks/copper_ore.json').decode()
    vanilla=vanilla.replace('minecraft:raw_copper','snc_energies:raw_tin').replace('minecraft:copper_ore',f'snc_energies:{name}').replace('minecraft:blocks/copper_ore',f'snc_energies:blocks/{name}')
    write(DATA/f'loot_table/blocks/{name}.json',json.loads(vanilla))
feature=json.loads((DATA/'worldgen/feature/voltaite_ore.json').read_text(encoding='utf-8-sig'))
feature=json.loads(json.dumps(feature).replace('voltaite','tin'));feature['size']=7
write(DATA/'worldgen/feature/tin_ore.json',feature)
placed=json.loads((DATA/'worldgen/placed_feature/voltaite_ore.json').read_text(encoding='utf-8-sig'))
placed['feature']='snc_energies:tin_ore';placed['placement'][0]['count']=10
placed['placement'][2]['height']['min_inclusive']['absolute']=-16
placed['placement'][2]['height']['max_inclusive']['absolute']=80
write(DATA/'worldgen/placed_feature/tin_ore.json',placed)

def smelt(name,input,output):
    for mode,time in [('smelting',200),('blasting',100)]:
        write(DATA/f'recipe/{name}_{mode}.json',dict(type=f'minecraft:{mode}',category='misc',ingredient=input,result=dict(id=output),experience=.7,cookingtime=time))
smelt('tin','snc_energies:raw_tin','snc_energies:tin_ingot')
for metal in ['iron','copper','gold','tin']:
    smelt(f'{metal}_dust',f'snc_energies:{metal}_dust',('snc_energies:' if metal=='tin' else 'minecraft:')+metal+'_ingot')
S=lambda name:'snc_energies:'+name
shapeless('bronze_ingot',['minecraft:copper_ingot']*3+[S('tin_ingot')],count=4)
recipe('steam_pipe',['BBB'],dict(B=S('bronze_ingot')),count=6)
recipe('item_pipe',['PP','BB'],dict(P='minecraft:iron_ingot',B=S('bronze_ingot')),count=8)
recipe('screwdriver',['C','S','P'],dict(C=S('copper_wire'),S='minecraft:stick',P=S('steel_plate')),count=1)
recipe('biomass_boiler',['BBB','BFB','CCC'],dict(B=S('bronze_ingot'),F='minecraft:furnace',C='minecraft:bricks'))
recipe('steam_sawmill',['III','BGB','PPP'],dict(I='minecraft:iron_ingot',B=S('bronze_ingot'),G=S('grain_mill'),P='#minecraft:planks'))
recipe('steam_foundry',['BBB','IFI','CCC'],dict(B=S('bronze_ingot'),I='minecraft:iron_ingot',F='minecraft:furnace',C='minecraft:bricks'))
recipe('steam_turbine',['SBS','BIB','SBS'],dict(S=S('steel_ingot'),B=S('bronze_ingot'),I='minecraft:iron_block'))
recipe('laminator',['SSS','BRB','SSS'],dict(S=S('steel_ingot'),B=S('bronze_ingot'),R='minecraft:redstone'))
recipe('steel_gear',[' P ','PBP',' P '],dict(P=S('steel_plate'),B=S('bronze_ingot')))
recipe('basic_circuit',['WRW','PSP','WRW'],dict(W=S('copper_wire'),R='minecraft:redstone',P=S('steel_plate'),S='minecraft:quartz'))
recipe('grain_dryer',['PPP','GCG','PFP'],dict(P=S('steel_plate'),G=S('steel_gear'),C=S('basic_circuit'),F='minecraft:furnace'))
recipe('oil_extractor',['PCP','GSG','PPP'],dict(P=S('steel_plate'),G=S('steel_gear'),C=S('basic_circuit'),S=S('seed_press')))
recipe('voltaic_refinery',['PCP','PGP','PCP'],dict(P=S('insulated_plate'),C=S('basic_circuit'),G=S('steel_gear')))
recipe('advanced_circuit',['VSV','WCW','VSV'],dict(V=S('refined_voltaite'),S=S('steel_plate'),W=S('copper_wire'),C=S('basic_circuit')))
recipe('mineral_synthesizer',['PAP','VRV','PAP'],dict(P=S('insulated_plate'),A=S('advanced_circuit'),V=S('refined_voltaite'),R='minecraft:diamond'))
# Existing blocks keep their IDs and saved state; only new crafting requires progression.
recipe('coal_generator',['SRS','RFR','SRS'],dict(S=S('steel_ingot'),R='minecraft:redstone',F='minecraft:furnace'))
recipe('electric_furnace',['PPP','WFW','PPP'],dict(P=S('steel_plate'),W=S('copper_wire'),F='minecraft:furnace'))
recipe('crusher',['PPP','GFG','PPP'],dict(P=S('steel_plate'),G=S('steel_gear'),F='minecraft:iron_pickaxe'))
recipe('energy_cube',['PWP','WRW','PWP'],dict(P=S('steel_plate'),W=S('copper_wire'),R='minecraft:redstone_block'))
shapeless('sawdust_briquette',[S('sawdust')]*4,S('biomass_briquette'))

for tag in ['mineable/pickaxe','needs_stone_tool']:
    path=RES/f'data/minecraft/tags/block/{tag}.json'
    data=json.loads(path.read_text(encoding='utf-8-sig')) if path.exists() else dict(replace=False,values=[])
    names=list(KINDS)+['steam_pipe','tin_ore','deepslate_tin_ore','item_pipe']
    for name in names:
        if S(name) not in data['values']:data['values'].append(S(name))
    write(path,data)

TEXT={f'block.snc_energies.{name}':(v[3],v[4]) for name,v in KINDS.items()}
TEXT.update({f'item.snc_energies.{name}':v for name,v in MATERIALS.items()})
TEXT.update({'block.snc_energies.steam_pipe':('Tubo de Vapor','Steam Pipe'),'block.snc_energies.item_pipe':('Tubo de Itens','Item Duct'),'block.snc_energies.tin_ore':('Minério de Estanho','Tin Ore'),
 'block.snc_energies.deepslate_tin_ore':('Minério de Estanho de Ardósia','Deepslate Tin Ore')})
for code,values in enumerate([('Falta insumo','Need ingredients'),('Falta combustível','Need fuel'),('Operando','Running'),('Saída cheia','Output full'),('Falta água','Need water'),('Falta vapor','Need steam'),('Falta energia','Need power'),('Redstone inativa','Redstone inactive'),('Redstone invertida','Inverted redstone')]):
    TEXT[f'gui.snc_energies.industry_status.{code}']=values
TEXT.update({'item.snc_energies.screwdriver':('Chave de Fenda','Screwdriver'),
 'gui.snc_energies.screwdriver.tooltip':('Configura lados de máquinas industriais; agachar inverte o modo de redstone.','Configures industrial machine sides; sneak to invert the redstone mode.'),
 'gui.snc_energies.screwdriver.face':('%s: %s','%s: %s'),
 'gui.snc_energies.screwdriver.side.front':('Frente','Front'),'gui.snc_energies.screwdriver.side.back':('Traseira','Back'),
 'gui.snc_energies.screwdriver.side.left':('Esquerda','Left'),'gui.snc_energies.screwdriver.side.right':('Direita','Right'),
 'gui.snc_energies.screwdriver.mode.both':('Entrada e saída','Input and output'),'gui.snc_energies.screwdriver.mode.input':('Somente entrada','Input only'),
 'gui.snc_energies.screwdriver.mode.output':('Somente saída','Output only'),
 'gui.snc_energies.screwdriver.run_always':('Operação: sempre ativa','Operation: always on'),
 'gui.snc_energies.screwdriver.run_with':('Operação: somente com redstone','Operation: redstone required'),
 'gui.snc_energies.screwdriver.run_not':('Operação: somente sem redstone','Operation: only without redstone'),
 'gui.snc_energies.pipe_filter.title':('Filtro do Tubo de Itens','Item Duct Filter'),
 'gui.snc_energies.pipe_filter.buffer':('Célula (item em trânsito)','Cell (item in transit)'),
 'gui.snc_energies.pipe_filter.whitelist':('Lista permitida (vazio = tudo)','Whitelist (empty = everything)'),
 'gui.snc_energies.pipe_filter.hint':('O tubo só puxa ou aceita itens da lista. Para limpar, use o botão.','The duct only pulls or accepts items on the list. Use the button to clear it.'),
 'gui.snc_energies.pipe_filter.clear':('Limpar','Clear')})
TEXT['guide.snc_energies.body.0']=('Cinco tiers: Colonial → Vapor → Eletromecânico → Agroindustrial → Voltaico. Comece com lavouras, moinho e prensa; minere estanho para fazer bronze. A fundição a vapor produz aço, o laminador produz chapas e fios, o secador produz chapas isoladas e a refinaria prepara a Voltaite. As próximas páginas detalham a operação. Máquinas antigas preservam seus dados; novas receitas seguem os tiers.', 'Five tiers: Colonial → Steam → Electromechanical → Agroindustrial → Voltaic. Start with crops, a mill and a press; mine tin for bronze. The steam foundry makes steel, the rolling mill makes plates and wire, the dryer makes insulated plates and the refinery prepares Voltaite. Following pages explain operation. Existing machines retain their data; new crafting follows the tiers.')
TEXT['guide.snc_energies.industry_base']=('Controlador: base esquerda da frente. Conecte energia elétrica nele; vapor pode entrar por qualquer parte. Funis inserem por cima/lados e extraem por baixo do controlador. Cada painel identifica seus slots de insumo, reagente, produto e resíduo; produto e resíduo à direita. A receita para se faltar recurso ou houver saída cheia. Espaço reservado: %s.','Controller: front bottom left. Connect electric power there; steam may enter any part. Hoppers insert above/beside and extract below the controller. Each panel labels its ingredient, reagent, product and residue slots; outputs are on the right. Processing pauses when resources are missing or outputs are blocked. Footprint: %s.')
TEXT['guide.snc_energies.boiler']=('Insira itens nos slots marcados Combustível e Água. O recipiente vazio sai no slot Balde. Cada balde fornece 1.000 mB de água; a caldeira consome 10 mB/t para produzir 80 mB/t de vapor. Não consome combustível sem água ou com tanque cheio. Tubos de vapor têm rede separada dos cabos de energia. Bronze: 3 cobres + 1 estanho na bancada. Estanho surge em chunks novos entre Y -16 e 80.', 'Use the labeled Fuel and Water slots. The empty container exits through the Bucket slot. Each bucket adds 1,000 mB water; the boiler uses 10 mB/t to produce 80 mB/t steam. Fuel pauses without water or when full. Steam ducts are separate from power cables. Bronze: craft 3 copper + 1 tin. Tin generates in new chunks between Y -16 and 80.')
TEXT['guide.snc_energies.turbine']=('Recebe 80 mB/t de vapor e gera 80 E/t. Use a saída de energia no controlador para conectar cabos. Reservas internas param a produção quando cheias; redstone ainda não configura portas neste marco.', 'Consumes 80 mB/t steam and generates 80 E/t. Attach power cables to its controller. Full internal buffers pause generation. Redstone port configuration is not implemented in this milestone.')
TEXT['guide.snc_energies.synthesis']=('A amostra no segundo slot não é consumida. Cada lote gasta 1 matriz e 160.000 E para gerar 2 minérios brutos. A matriz é produzida com pedra e óleo na refinaria. Esta tecnologia é fictícia. Não gera carvão ou combustível, evitando um ciclo gratuito de energia.', 'The sample in the second slot is retained. Each batch consumes 1 matrix and 160,000 E to produce 2 raw ores. The refinery makes matrix from cobblestone and oil. This technology is fictional. It cannot generate coal or fuel, avoiding a free energy loop.')
TEXT['guide.snc_energies.transport']=('Tubos de itens transportam um item por vez até uma máquina ou funil vizinho à rede; armazenam nada e só movem quando o destino aceita. Funis continuam inserindo por cima/lados e retirando por baixo do controlador. Botão no painel do sintetizador alterna operação sempre ativa ou somente com redstone ativa. A chave de fenda configura cada lado das industriais (entrada, saída ou ambos) e, agachando, inverte o modo de redstone. Clique num tubo para editar a lista permitida da célula.', 'Item ducts move one item at a time towards a machine or hopper adjacent to the network; they store nothing and only move items the destination accepts. Hoppers still insert above/beside and extract below the controller. The synthesizer panel button toggles always-on or redstone-only operation. The screwdriver configures each industrial side (input, output or both) and, while sneaking, inverts the redstone mode. Click a duct to edit the cell whitelist.')
TEXT['guide.snc_energies.title.7']=('08 / Erva-mate opcional', '08 / Optional yerba mate')
TEXT['guide.snc_energies.body.7']=('Semente de erva-mate: semente de trigo + muda de carvalho. Cultive em terra arada; colha as folhas maduras. Seque as folhas na fornalha ou no secador industrial e moa no moinho. Na bancada: erva-mate moída + tigela + balde de água produzem uma infusão. O balde retorna na fabricação e a tigela retorna ao beber. Este ramo não bloqueia a progressão industrial.', 'Yerba mate seeds: wheat seeds + oak sapling. Grow on farmland and harvest mature leaves. Dry leaves in a furnace or industrial dryer, then grind them in the mill. Craft ground mate + bowl + water bucket into an infusion. Crafting returns the bucket; drinking returns the bowl. This optional branch does not gate industrial progression.')
# Crafting instructions are generated from the actual recipe, including its grid.
for name in KINDS:
    crafting=json.loads((DATA/f'recipe/{name}.json').read_text(encoding='utf-8'))
    descriptions=[]
    for index,language in enumerate(['pt_br','en_us']):
        with zipfile.ZipFile(mc) as archive:
            vanilla=json.loads(archive.read('assets/minecraft/lang/en_us.json'))
        translations=json.loads((ASSETS/f'lang/{language}.json').read_text(encoding='utf-8-sig'))
        translations.update({k:v[index] for k,v in TEXT.items()})
        def label(identifier):
            if identifier.startswith('#'):return 'Tábuas' if index==0 else 'Planks'
            key=identifier.replace(':','.')
            for prefix in ['item.','block.']:
                if prefix+key in translations:return translations[prefix+key]
                if prefix+key in vanilla:return vanilla[prefix+key]
            return identifier
        legend='; '.join(k+' = '+label(v) for k,v in crafting['key'].items())
        grid=' / '.join(row.replace(' ','·') for row in crafting['pattern'])
        descriptions.append(('Bancada (linhas de cima para baixo): ' if index==0 else 'Crafting grid (top to bottom): ')+grid+'. '+legend+'. ')
    TEXT['guide.snc_energies.craft.'+name]=tuple(descriptions)
for index,language in enumerate(['pt_br','en_us']):
    path=ASSETS/f'lang/{language}.json';data=json.loads(path.read_text(encoding='utf-8-sig'));data.update({k:v[index] for k,v in TEXT.items()});write(path,data)
write(ROOT/'previews/industry-models.json',{k:dict(dimensions=list(KINDS[k][:3]),elements=[element(b) for b in v]) for k,v in GEOMETRY.items()})
print('Industry progression generated: nine multiblocks, tin, steam ducts and tier recipes.')

import base64
template=(ROOT/'previews/fogao-a-lenha.html').read_text(encoding='utf-8')
renderer=template[template.index('function render()'):template.index('</script>')]
renderer=renderer.replace("['off','on']",'Object.keys(models)')
renderer=renderer.replace('x-=24;y-=20;z-=16','x-=model.dimensions[0]*8;y-=model.dimensions[2]*8;z-=model.dimensions[1]*8')
renderer=renderer.replace('xx*9','xx*model.scale').replace('))*9', '))*model.scale')
models={name:dict(dimensions=list(KINDS[name][:3]),scale=min(12,420/(max(KINDS[name][:3])*16)),
    elements=[element(b) for b in boxes],textures={t:f'snc_energies:block/{t}' for t in {b[6] for b in boxes}})
    for name,boxes in GEOMETRY.items()}
textures={t for boxes in GEOMETRY.values() for *_,t in boxes}
sources={f'snc_energies:block/{t}':'data:image/png;base64,'+base64.b64encode((ASSETS/f'textures/block/{t}.png').read_bytes()).decode() for t in textures}
cards=''.join(f'<article><canvas id="{name}" width="720" height="600"></canvas><h2>{v[3]}</h2><p>{v[0]} × {v[1]} × {v[2]} blocos · inventário único</p></article>' for name,v in KINDS.items())
page='''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>SNC · Cinco tiers</title><style>
body{margin:0;background:#141e1b;color:#efdec0;font:16px/1.6 system-ui}main{max-width:1180px;margin:auto;padding:35px}h1{font-size:48px;line-height:1.1}small{color:#b6bca3}.views{display:grid;grid-template-columns:1fr 1fr;gap:20px}article{background:radial-gradient(ellipse,#354337,#202c24);padding:22px;border:1px solid #4f5e48;border-radius:12px}canvas{width:100%;aspect-ratio:1.2}h2{font-size:23px;margin:0}article p{color:#bcc3af;margin:6px 0}button{background:#d3aa6b;padding:10px;border:0;border-radius:5px}a{color:#d3aa6b}.flow{padding:20px;border:1px solid #5d7356;border-radius:7px;margin:22px 0}@media(max-width:700px){.views{grid-template-columns:1fr}}</style>
<main><small>SNC ENERGIES · MODELOS REAIS DA EXPANSÃO 0.2.1</small><h1>Da oficina à indústria.</h1>
<p>Geometria dos modelos que entram no jogo, com texturas 128×128. Cada estrutura tem colisão derivada de seus próprios cubos, um inventário e um menu compartilhado.</p>
<div class="flow">Colonial → Vapor / bronze → Eletricidade / aço → Agroindústria → Voltaite / síntese mineral</div>
<label>Girar <input id="angle" type="range" min="0" max="360" value="145"></label> <button id="spin">Rotação automática</button>
<p><a href="colonial.html">Ver moinho, prensa, lavouras e materiais do Colonial</a></p><div class="views">'''+cards+'''</div>
<p>Para começar: livro + semente de trigo → Caderno da Oficina SNC. As novas receitas exigem os materiais dos tiers anteriores. As máquinas antigas preservam seus IDs e dados.</p>
<p>Integração atual Adventures: coexistência em Minecraft 26.3 e bagaço da moenda como combustível. Motorização de máquinas Adventures, UV elétrico e novas ofertas comerciais permanecem pendentes. Transporte de itens neste marco usa funis; tubos são de vapor.</p>
<footer>Iluminação aproximada na prévia. As dimensões estão na ordem largura × profundidade × altura.</footer></main><script>'''
page+='const models='+json.dumps(models)+',sources='+json.dumps(sources)+',textures={};let spinning=false;\n'+renderer+'</script></html>'
(ROOT/'previews/industria.html').write_text(page,encoding='utf-8')

# Keep native panel resources and previews synchronized after material translations.
import generate_machine_panels
