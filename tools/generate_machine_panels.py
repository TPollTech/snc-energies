"""One source for native vector panels, slot layouts and an exact browser preview."""
import json, math
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/snc_energies'
profiles={}
LABELS={
 'input':('Insumo','Input'),'product':('Produto','Product'),'residue':('Resíduo','Residue'),
 'fuel':('Combustível','Fuel'),'water':('Água','Water'),'bucket':('Balde','Bucket'),
 'carbon':('Carbono','Carbon'),'grain':('Grãos','Grain'),'oil':('Óleo','Oil'),
 'matrix':('Matriz','Matrix'),'sample':('Amostra','Sample'),'ore':('Minério','Ore'),
 'reagent':('Reagente','Reagent'),'energy':('Energia','Energy'),'steam':('Vapor','Steam'),
 'work':('Trabalho','Work'),'progress':('Processo','Process'),'burn':('Queima','Burn'),
 'ready':('Aguardando insumo','Waiting for input'),'running':('Operando','Running'),
 'stored':('Reserva da rede','Grid reserve'),'reserve':('Reserva','Reserve'),
 'press':('Acionar prensa','Press lever'),'crank':('Girar manivela','Turn crank'),
 'redstone':('Alternar redstone','Toggle redstone'),
 'sample_hint':('Amostra preservada ao produzir','Sample retained during production'),
 'intake':('Recepção','Intake'),'row':('Fileira','Row'),'out':('Saída','Output')}
def rect(x,y,w,h,c): p['rects'].append([int(x),int(y),int(w),int(h),c])
def line(x,y,xx,yy,c,width=1):
    steps=max(1,int(max(abs(xx-x),abs(yy-y))))
    for i in range(steps+1):rect(x+(xx-x)*i/steps,y+(yy-y)*i/steps,width,width,c)
def ring(x,y,r,c,width=2):
    for yy in range(-r,r+1):
        outer=int(math.sqrt(max(0,r*r-yy*yy)))
        inner=int(math.sqrt(max(0,(r-width)**2-yy*yy))) if abs(yy)<r-width else 0
        rect(x-outer,y+yy,outer-inner+1,1,c);rect(x+inner,y+yy,outer-inner+1,1,c)
def panel(x,y,w,h,c):
    rect(x,y,w,h,'#10171b');rect(x+2,y+2,w-4,h-4,c)
    line(x+3,y+3,x+w-4,y+3,p['accent'])
def pipe(x,y,xx,yy):line(x,y,xx,yy,'#0b1114',5);line(x+1,y+1,xx+1,yy+1,p['accent'],2)
def slot(i,x,y,role):p['slots'].append(dict(index=i,x=x,y=y,role=role))
def bar(source,x,y,w,h,c=None,vertical=False):
    p['bars'].append(dict(source=source,x=x,y=y,w=w,h=h,color=c or p['accent'],vertical=vertical))
def begin(name,subtitle,theme,accent,material):
    global p
    p=dict(id=name,subtitle=subtitle,accent=accent,rects=[],slots=[],bars=[],button=None)
    profiles[name]=p
    rect(0,0,256,238,'#101619');rect(2,2,252,234,theme);rect(7,7,242,27,'#131c21')
    rect(7,33,242,2,accent);rect(7,39,242,90,'#1a252b')
    if material=='timber':
        rect(7,39,242,90,'#493329')
        for y in range(42,127,9):line(9,y,246,y,'#5b4434')
    elif material=='ceramic':
        rect(7,39,242,90,'#363e3b')
        for y in range(40,128,12):
            line(9,y,246,y,'#4c5650')
            for x in range(10+(y%24),249,28):line(x,y,x,y+10,'#4c5650')
    elif material=='lab':
        for x in range(14,249,12):line(x,41,x,127,'#233a45')
        for y in range(44,128,12):line(9,y,246,y,'#233a45')
    else:
        for y in range(43,128,4):line(9,y,246,y,'#202c32')
    rect(7,130,242,13,'#10191d');rect(41,153,174,81,'#10181c')
    for x in [3,250]:
        for y in [4,145,231]:rect(x,y,3,3,'#97a49c')

begin('wood_stove',('Fogo de chão · geração a lenha','Wood fire · thermal generation'),'#574239','#eda56b','ceramic')
panel(25,58,82,62,'#73412f');rect(22,52,88,8,'#202b2c')
for x in [39,64,88]:ring(x,48,8,'#bdc0a3')
rect(81,40,14,14,'#192429');slot(0,50,86,'fuel');bar('burn',34,78,5,35,'#f48940',True)
pipe(108,94,153,94);panel(155,54,76,61,'#253e39');bar('energy',169,77,49,17,'#8fcbad')

begin('coal_generator',('Grupo gerador · combustão','Generator set · combustion'),'#343f42','#e9b74c','steel')
panel(23,65,46,46,'#4b4333');slot(0,38,82,'fuel');bar('burn',26,69,37,4,'#ef9349')
pipe(70,88,91,88);panel(88,59,80,54,'#35494c')
for x in range(96,164,8):rect(x,65,3,42,'#111d23')
ring(125,87,18,'#7f9392');pipe(168,86,205,86);bar('energy',211,53,13,66,'#e9b74c',True)

begin('electric_furnace',('Câmara térmica · fundição elétrica','Thermal chamber · electric smelting'),'#4e3f37','#f5aa72','ceramic')
slot(0,34,79,'input');slot(1,207,79,'product');panel(86,46,89,75,'#716155')
for x in [94,159]:
    for y in range(55,113,8):rect(x,y,8,4,'#f29362')
panel(108,55,45,52,'#252827');bar('progress',117,66,27,32,'#ffa15e',True)
pipe(54,88,83,88);pipe(176,88,205,88);bar('energy',23,116,49,4,'#9dd9e9')

begin('crusher',('Mandíbulas · redução de minério','Jaw assembly · ore reduction'),'#41463e','#dfb84f','steel')
slot(0,34,53,'ore');slot(1,207,100,'product');pipe(54,62,99,62);pipe(167,107,204,107)
for i in range(5):
    rect(98+i*3,53+i*12,15,8,'#c1c8b5');rect(155-i*3,53+i*12,15,8,'#7e8e80')
bar('progress',120,60,21,49,'#dfb84f',True);bar('energy',23,112,51,5,'#d7c67e')
for x in range(86,177,12):line(x,43,x+6,49,'#dfb84f',3)

begin('grain_mill',('Mós de pedra · trabalho manual','Stone burrs · manual work'),'#5d4532','#dac18c','timber')
slot(0,28,63,'grain');slot(1,206,58,'product');slot(2,206,99,'residue')
ring(124,78,29,'#b5b4a0',5);ring(124,78,19,'#797e72',4);ring(124,78,6,'#dbc593',3)
for a in range(0,360,45):line(124,78,124+24*math.cos(math.radians(a)),78+24*math.sin(math.radians(a)),'#6f796c')
pipe(47,72,91,72);pipe(156,70,202,70);bar('progress',105,96,37,3);bar('work',22,113,45,5,'#adc17c')
p['button']=[82,110,85,17]

begin('seed_press',('Fuso e pistão · óleo e briquetes','Screw press · oil and briquettes'),'#4c4531','#b3c681','timber')
panel(74,48,80,58,'#2b3024');rect(80,48,6,57,'#ac956c');rect(143,48,6,57,'#ac956c')
for y in range(54,82,4):rect(109,y,13,2,'#c9c39a')
rect(91,84,47,7,'#879672');bar('progress',90,95,48,4);slot(0,107,54,'input')
slot(1,180,86,'oil');slot(2,215,86,'residue');pipe(151,92,176,92)
bar('work',181,117,47,4,'#b3c681');p['button']=[24,109,98,18]

begin('compactor',('Pistão e prensagem · fardos em lote','Piston press · batch baling'),'#3f463c','#dfb84f','steel')
slot(0,34,79,'input');slot(4,207,79,'product')
pipe(54,88,83,88);pipe(176,88,205,88)
panel(88,48,85,70,'#4b5546');rect(96,56,69,26,'#202c32')
for y in [60,68,76]:rect(100,y,61,3,'#dfb84f')
rect(112,86,37,18,'#111d23');ring(130,95,7,'#dfb84f',3)
bar('progress',49,86,165,4)
bar('energy',190,46,41,5,'#aad2e5')

begin('silo',('Graneleira · 16 fileiras de 16.384','Bulk bin · 16 rows of 16,384'),'#4a4231','#c9b06a','timber')
panel(84,40,96,88,'#63512f')
for x in range(88,178,12):line(x,44,x,124,'#574429')
for column in range(4):
    for row in range(4):slot(1+column*4+row,90+column*20,54+row*19,'row')
slot(0,34,60,'intake');slot(17,222,60,'out')
pipe(54,88,83,88);pipe(176,88,205,88)
bar('work',30,112,45,5,'#c9b06a')

begin('biomass_boiler',('Água + biomassa · circuito de vapor','Water + biomass · steam circuit'),'#514437','#e5ad6c','steel')
slot(2,29,101,'fuel');slot(3,29,55,'water');slot(6,210,101,'bucket')
panel(90,47,67,77,'#755b41');ring(123,60,10,'#e5ad6c');rect(100,111,47,8,'#32291f')
pipe(50,63,87,63);pipe(50,109,87,109);pipe(158,62,186,62)
bar('water',101,76,13,30,'#74b5df',True);bar('steam',178,54,13,57,'#e5d2a1',True);bar('burn',125,83,21,23,'#ed9354',True)

begin('steam_sawmill',('Bancada de corte · serragem recuperada','Saw table · recovered sawdust'),'#3a4948','#c2ba8d','steel')
slot(0,24,76,'input');slot(4,210,58,'product');slot(5,210,103,'residue')
panel(57,83,140,13,'#625b43');ring(127,76,25,'#b8c8c2',4)
for a in range(0,360,30):
    rad=math.radians(a);line(127+24*math.cos(rad),76+24*math.sin(rad),127+29*math.cos(rad+.08),76+29*math.sin(rad+.08),'#b8c8c2',2)
bar('progress',70,89,113,3);bar('steam',57,115,78,5,'#e2cda2')

begin('steam_foundry',('Cadinho · ferro e carbono','Crucible · iron and carbon'),'#59413d','#f3a379','ceramic')
slot(0,29,55,'input');slot(1,29,104,'carbon');slot(4,210,104,'product')
for row in range(44):rect(88+row//4,55+row,74-row//2,1,'#bf7150')
rect(83,50,84,7,'#d9b18b');bar('progress',106,65,37,23,'#ffd69c',True)
pipe(169,76,212,76);line(220,76,220,95,'#d99159',3);bar('steam',182,46,49,5,'#e8d1aa')

begin('steam_turbine',('Rotor · conversão vapor / energia','Rotor · steam to power'),'#304b50','#8bd3cc','steel')
ring(106,82,36,'#779f9f',4);ring(106,82,12,'#d6e6c6',5)
for a in range(0,360,60):
    rad=math.radians(a);line(106+12*math.cos(rad),82+12*math.sin(rad),106+30*math.cos(rad+.4),82+30*math.sin(rad+.4),'#8bd3cc',5)
pipe(37,80,68,80);pipe(145,82,169,82);bar('steam',22,52,12,64,'#e5d5a9',True)
panel(173,53,64,63,'#203638');bar('energy',184,74,42,21,'#8bd3cc')

begin('laminator',('Rolos paralelos · chapas e fios','Parallel rolls · plates and wire'),'#354953','#aad2e5','steel')
slot(0,24,82,'input');slot(4,217,82,'product');panel(76,47,103,75,'#4b5d66')
for y in [67,101]:
    rect(86,y-10,81,19,'#9aaeb7');ring(94,y,10,'#dfded0',3);ring(159,y,10,'#5d727b',3)
bar('progress',49,86,165,4,'#e6c684');bar('energy',190,46,41,5,'#aad2e5')

begin('grain_dryer',('Coluna de secagem · processamento em lote','Drying column · batch processing'),'#4b5140','#cbd393','steel')
slot(0,26,55,'grain');slot(1,26,104,'reagent');slot(4,214,55,'product');slot(5,214,104,'residue')
panel(91,43,75,82,'#6b7551')
for y in [57,78,99]:
    rect(102,y,51,9,'#222f26')
    for x in range(105,151,8):line(x,y+1,x+4,y+7,'#cbd393')
bar('progress',175,55,7,61,'#ddbe79',True);bar('energy',52,79,28,4,'#a5c7b7')

begin('oil_extractor',('Separação · óleo e farelo','Separation · oil and meal'),'#42533f','#d4cc78','steel')
slot(0,24,55,'input');slot(4,179,89,'oil');slot(5,218,89,'residue');pipe(45,63,77,63)
ring(112,79,31,'#8ca282',5);ring(112,79,20,'#d4cc78',3);ring(112,79,8,'#dfe2b1',3)
pipe(146,80,184,80);pipe(184,80,220,80);bar('progress',152,55,7,54,'#e4c773',True);bar('energy',22,116,49,4,'#a4cfa5')

begin('voltaic_refinery',('Colunas de reação · refinamento','Reaction columns · refining'),'#4d395b','#c2a4e4','lab')
slot(0,22,55,'ore');slot(1,22,104,'oil');slot(4,218,84,'product')
for x,h in [(82,62),(134,43)]:
    panel(x,115-h,30,h,'#675576');ring(x+14,115-h,14,'#b8a5c7',2)
pipe(43,64,79,64);pipe(43,112,79,112);pipe(112,99,132,99);pipe(165,96,215,96)
bar('progress',91,68,11,39,'#caabea',True);bar('energy',184,48,47,4,'#b6cfee')

begin('mineral_synthesizer',('Matriz + amostra · cristalização','Matrix + sample · crystallization'),'#254754','#78e5de','lab')
slot(0,22,90,'matrix');slot(1,118,48,'sample');slot(4,218,90,'ore')
for offset in [0,8]:
    points=[(127,66+offset),(158-offset,89),(127,112-offset),(96+offset,89),(127,66+offset)]
    for a,b in zip(points,points[1:]):line(*a,*b,'#78e5de',2)
pipe(43,97,89,97);pipe(164,97,215,97);bar('progress',120,80,14,22,'#d7faf1',True);bar('energy',83,120,90,3,'#78e5de')
p['button']=[22,110,98,18]

begin('energy_cube',('Banco de células · armazenamento','Cell bank · storage'),'#284653','#86d2eb','lab')
for row in range(2):
    for col in range(4):panel(42+col*44,49+row*36,36,27,'#29444d')
for row in range(2):
    for col in range(4):bar('energy',47+44*col,55+36*row,26,15,'#75cbdc')

for p in profiles.values():
    for s in p['slots']:
        x,y=s['x'],s['y'];rect(x-2,y-2,20,20,'#0b1418');rect(x-1,y-1,18,18,p['accent']);rect(x,y,16,16,'#283337')
    for row in range(4):
        for col in range(9):
            x=48+18*col;y=156+18*row if row<3 else 214
            rect(x-1,y-1,18,18,'#536368');rect(x,y,16,16,'#202b30')
    for b in p['bars']:rect(b['x']-1,b['y']-1,b['w']+2,b['h']+2,'#0a1418')
    if p['button']:
        x,y,w,h=p['button'];rect(x,y,w,h,'#172422');rect(x+1,y+1,w-2,h-2,'#5d6949')

target=ASSETS/'machine_panels.json';target.write_text(json.dumps(profiles,ensure_ascii=False,separators=(',',':')),encoding='utf-8')
for index,lang in enumerate(['pt_br','en_us']):
    path=ASSETS/f'lang/{lang}.json';data=json.loads(path.read_text(encoding='utf-8-sig'))
    data.update({'gui.snc_energies.panel.'+key:values[index] for key,values in LABELS.items()})
    data.update({'gui.snc_energies.panel.subtitle.'+name:p['subtitle'][index] for name,p in profiles.items()})
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
names=json.loads((ASSETS/'lang/pt_br.json').read_text(encoding='utf-8'))
page='''<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>SNC · Identidade das máquinas</title><style>
body{margin:0;background:#101a20;color:#e4eee8;font:16px system-ui}main{max-width:1220px;margin:auto;padding:32px}h1{font-size:44px;max-width:850px;line-height:1.1}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:18px}article{background:#19282d;border:1px solid #40535a;border-radius:12px;padding:14px}h2{font-size:18px;margin:4px 0 12px}canvas{width:100%;image-rendering:pixelated}a{color:#80ddd5}p{color:#b9cdc8;line-height:1.6}.note{padding:16px;background:#22373b;margin:24px 0}input{width:240px} @media(max-width:900px){.grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:580px){.grid{grid-template-columns:1fr}}</style>
<main><small>SNC ENERGIES · REDESENHO DAS INTERFACES</small><h1>Cada processo tem seu próprio painel.</h1><p>Madeira e pedra na oficina, instrumentos nas máquinas a vapor, mecanismos na indústria e câmaras de reação no tier voltaico. Mesmos desenhos e posições usados no jogo; a fonte do navegador é aproximada.</p><div class="note">Prévia de preenchimento <input id="level" type="range" min="0" max="100" value="64"> <span id="value">64%</span> · Valores ilustrativos. No jogo, os indicadores mostram dados reais do servidor.</div><p><a href="industria.html">Ver modelos 3D — sintetizador remodelado; refinaria preservada</a></p><div class="grid" id="grid"></div></main><script>'''
page+='const panels='+json.dumps(profiles,ensure_ascii=False)+',names='+json.dumps(names,ensure_ascii=False)+';'
page+='''function text(c,s,x,y,color='#d8e4d9'){c.fillStyle=color;c.font='9px monospace';c.fillText(s,x,y+8)}
function draw(){let f=Number(document.querySelector('#level').value)/100;document.querySelector('#value').textContent=Math.round(f*100)+'%';for(let [id,p] of Object.entries(panels)){let c=document.querySelector('#'+id).getContext('2d');c.setTransform(2,0,0,2,0,0);for(let [x,y,w,h,color] of p.rects){c.fillStyle=color;c.fillRect(x,y,w,h)}for(let b of p.bars){c.fillStyle=b.color;if(b.vertical)c.fillRect(b.x,b.y+b.h*(1-f),b.w,b.h*f);else c.fillRect(b.x,b.y,b.w*f,b.h)}text(c,names['block.snc_energies.'+id],13,11);text(c,p.subtitle[0],13,23,p.accent);for(let s of p.slots)text(c,names['gui.snc_energies.panel.'+s.role],s.x-2,s.y-12,p.accent);text(c,'Operando',13,132,p.accent);text(c,'Inventário',48,145);if(p.button){let [x,y,w,h]=p.button;text(c,names['gui.snc_energies.panel.'+(id==='seed_press'?'press':id==='mineral_synthesizer'?'redstone':'crank')],x+4,y+4)}}}
for(let [id,p] of Object.entries(panels)){let a=document.createElement('article');a.innerHTML='<h2>'+names['block.snc_energies.'+id]+'</h2><canvas id="'+id+'" width="512" height="476"></canvas>';document.querySelector('#grid').append(a)}document.querySelector('#level').addEventListener('input',draw);draw();</script></html>'''
(ROOT/'previews/paineis.html').write_text(page,encoding='utf-8')
print(f'Generated {len(profiles)} distinct machine panels and shared slot layouts.')
