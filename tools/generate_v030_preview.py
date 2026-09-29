"""Build the v0.3.0 feature preview from the real game assets.

Run from the project root: .venv-textures/Scripts/python.exe tools/generate_v030_preview.py
Outputs previews/v030.html: a self-contained page (data URIs only) showing
the item pipe line (core + arm cells exactly as modeled in the JAR) between
the native electric furnace and crusher, plus the real mineral synthesizer
GUI panel with the redstone toggle button (panel rects come from the shared
machine_panels.json the game screens also consume).
"""
from __future__ import annotations

import base64
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / "src/main/resources/assets/snc_energies"
PANELS = json.loads((RES / "machine_panels.json").read_text(encoding="utf-8"))


def data_uri(path: Path) -> str:
    return "data:image/png;base64," + base64.b64encode(path.read_bytes()).decode("ascii")


def texture_uri(reference: str) -> str:
    relative = reference.split(":", 1)[1].replace("block/", "textures/block/") + ".png"
    return data_uri(RES / relative)


def load_model(name: str) -> dict:
    return json.loads((RES / "models/block" / f"{name}.json").read_text(encoding="utf-8"))


def cube_elements(model: dict) -> list:
    """Cube-parent models (block/cube) carry no elements: rebuild one cube from their own face textures."""
    textures = model.get("textures", {})

    def key_for(face):
        value = textures.get(face) or textures.get("particle") or next(iter(textures.values()))
        return value.split("/")[-1]

    return [{"from": [0, 0, 0], "to": [16, 16, 16],
             "faces": {face: {"texture": "#" + key_for(face)}
                       for face in ("north", "south", "east", "west", "up", "down")}}]


def main():
    sources = {}
    models = {}
    for cell in ("item_pipe", "item_pipe_north", "item_pipe_south", "item_pipe_east", "item_pipe_west", "item_pipe_up", "item_pipe_down"):
        model = load_model(cell)
        models[cell] = {"elements": model["elements"]}
        for key, reference in model.get("textures", {}).items():
            uri = texture_uri(reference)
            sources[key] = uri
            sources[reference.split("/")[-1]] = uri
    for machine in ("electric_furnace_on", "crusher_on"):
        model = load_model(machine)
        models[machine] = {"elements": cube_elements(model)}
        for key, reference in model.get("textures", {}).items():
            uri = texture_uri(reference)
            sources[key] = uri
            sources[reference.split("/")[-1]] = uri
    lang = json.loads((RES / "lang/pt_br.json").read_text(encoding="utf-8-sig"))
    panel = PANELS["mineral_synthesizer"]

    page = """<!doctype html><html lang="pt-BR"><meta charset="utf-8"><title>SNC · Prévia 0.3.0</title><script type="importmap">{"imports":{"three":"./vendor/three.module.js","three-controls":"./vendor/OrbitControls.js"}}</script><style>
body{margin:0;background:#141e1b;color:#efdec0;font:16px/1.6 system-ui}main{max-width:1180px;margin:auto;padding:35px}h1{font-size:48px;line-height:1.1}small{color:#b6bca3}
.wrap{display:grid;grid-template-columns:minmax(0,1fr) 420px;gap:22px;align-items:start}
.scene{background:radial-gradient(ellipse,#354337,#202c24);padding:14px;border:1px solid #4f5e48;border-radius:12px}
canvas{width:100%;border-radius:8px;display:block;touch-action:none}
.panelcard{background:#19282d;border:1px solid #40535a;border-radius:12px;padding:18px}
.panelcard canvas{image-rendering:pixelated;width:100%;max-width:512px}
button{background:#d3aa6b;padding:10px 14px;border:0;border-radius:5px;cursor:pointer;font:inherit}
h2{font-size:23px;margin:0 0 8px}p{color:#bcc3af;margin:6px 0}.flow{padding:16px 20px;border:1px solid #5d7356;border-radius:7px;margin:20px 0}
ul{color:#bcc3af;line-height:1.8}.tag{display:inline-block;background:#22373b;border:1px solid #40535a;border-radius:6px;padding:2px 10px;margin:2px;font-size:13px;color:#8bd3cc}
.note{font-size:13px;color:#8fa39a}
@media(max-width:1000px){.wrap{grid-template-columns:1fr}}
</style>
<main><small>SNC ENERGIES · MODELOS REAIS DA VERSÃO 0.3.0</small><h1>Itens viajando pela indústria.</h1>
<p>O Tubo de Itens usa a mesma silhueta dos cabos: casca galvanizada 128×128 com faixa âmbar. Cada célula tem buffer de 1 item e aceita exatamente os braços de conexão renderizados aqui, direto dos modelos do JAR.</p>
<div class="flow">Fornalha Elétrica → tubo vazio suga <b>por baixo</b> a 1 item/passo → 3 células → Britador aceita insumo por qualquer lado</div>
<div class="wrap">
<section class="scene"><canvas id="scene" width="720" height="560"></canvas>
<p class="note">Arraste para orbitar · scroll para zoom · botão direito move. Modelos: item_pipe, item_pipe_{north,east,up}, electric_furnace_on, crusher_on (sem escala/alterações).</p></section>
<section class="panelcard"><h2>__PANEL_TITLE__</h2>
<canvas id="panel" width="512" height="476"></canvas>
<p style="display:flex;gap:10px;align-items:center"><button id="rs">Alternar redstone</button><span id="rsstate" class="tag">Operando</span></p>
<p class="note">Painel real do Sintetizador Mineral (machine_panels.json compartilhado com o jogo). O botão alterna o data slot 11; no jogo o servidor confirma via clickMenuButton e o status 7 mostra “Redstone inativa”.</p>
<p style="margin-top:8px"><label>Enchimento ilustrativo <input id="level" type="range" min="0" max="100" value="64"></label></p>
</section></div>
<h2>O que entrou na 0.3.0</h2><ul>
<li>Tubo de Itens: roteamento vizinhos + BFS ≤512 células, 1 item/passo, commit só quando o destino aceita; item estaciona na célula quando o destino enche (conservação exata).</li>
<li>Sucção só de saídas de máquinas (fornalha, britador, controladores por baixo); funis e tubos nunca são drenados.</li>
<li>Máquinas nativas WorldlyContainer: insumo/product por qualquer lado; gerador aceita combustível e não solta nada.</li>
<li>Válvula de redstone nas 11 industriais com modo persistido; lote em curso termina, nada novo começa sem sinal.</li></ul>
<p class="note">Prévia de modelagem — comportamento validado por 469/473 verificações de servidor (logs em verification/).</p></main>
<script type="module">
import * as THREE from 'three';
import { OrbitControls } from 'three-controls';
const models=__MODELS__;
const sources=__SOURCES__;
const panel=__PANEL__;
const names=__NAMES__;
const loader=new THREE.TextureLoader();
const tex=(uri)=>{const t=loader.load(uri);t.magFilter=THREE.NearestFilter;t.minFilter=THREE.NearestFilter;t.colorSpace=THREE.SRGBColorSpace;return t;};
const scene=new THREE.Scene();scene.background=new THREE.Color('#202c24');
const camera=new THREE.PerspectiveCamera(40,720/560,.1,200);camera.position.set(-4.5,3.6,7);
const renderer=new THREE.WebGLRenderer({canvas:document.getElementById('scene'),antialias:true});
renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.setPixelRatio(Math.min(devicePixelRatio,2));
scene.add(new THREE.HemisphereLight(0xfff6e8,0x3b4a40,2.4));
const sun=new THREE.DirectionalLight(0xffeccc,2.2);sun.position.set(-6,10,4);scene.add(sun);
const ground=new THREE.Mesh(new THREE.PlaneGeometry(60,60),new THREE.MeshStandardMaterial({color:'#26332c',roughness:1}));
ground.rotation.x=-Math.PI/2;ground.position.y=-1.01;scene.add(ground);
const grid=new THREE.GridHelper(48,48,0x39503f,0x2c3d33);grid.position.y=-1;scene.add(grid);
const cache={};
const matFor=(key)=>cache[key]??=(()=>{const m=new THREE.MeshStandardMaterial({map:tex(sources[key]),roughness:.85});return m;})();
function addModel(name,x,y,z,rotY=0){
  for(const e of models[name].elements){
    const from=e.from,to=e.to;
    const size=[to[0]-from[0],to[1]-from[1],to[2]-from[2]];
    const center=[(from[0]+to[0])/2,(from[1]+to[1])/2,(from[2]+to[2])/2];
    const mats=[];
    for(const face of ['east','west','up','down','south','north']){
      const ref=e.faces[face]?.texture;
      mats.push(ref?matFor(ref.replace(/^#/,'')):new THREE.MeshStandardMaterial({color:'#667'}));
    }
    const box=new THREE.Mesh(new THREE.BoxGeometry(size[0]/16,size[1]/16,size[2]/16),mats);
    box.position.set(x+center[0]/16,y+center[1]/16,z+center[2]/16);
    box.rotation.y=rotY*Math.PI/180;
    scene.add(box);
  }
}
addModel('electric_furnace_on',0,0,0,0);
addModel('item_pipe_west',1,0,0);addModel('item_pipe_east',1,0,0);
addModel('item_pipe_west',2,0,0);addModel('item_pipe_east',2,0,0);
addModel('item_pipe_west',3,0,0);addModel('item_pipe_east',3,0,0);
addModel('crusher_on',4,0,0,180);
const orbit=new OrbitControls(camera,renderer.domElement);
orbit.target.set(2,.2,0);orbit.enableDamping=true;orbit.dampingFactor=.08;orbit.minDistance=3;orbit.maxDistance=22;
renderer.setSize(720,560,false);
renderer.setAnimationLoop(()=>{orbit.update();renderer.render(scene,camera);});
// ---- real synthesizer panel
const p=document.getElementById('panel').getContext('2d');p.imageSmoothingEnabled=false;
let rsOn=true,f=.64;
function text(s,x,y,color='#d8e4d9'){p.fillStyle=color;p.font='9px monospace';p.fillText(s,x,y+8);}
function draw(){
  p.setTransform(2,0,0,2,0,0);
  for(const [x,y,w,h,color] of panel.rects){p.fillStyle=color;p.fillRect(x,y,w,h);}
  for(const b of panel.bars){p.fillStyle=b.color;if(b.vertical)p.fillRect(b.x,b.y+b.h*(1-f),b.w,b.h*f);else p.fillRect(b.x,b.y,b.w*f,b.h);}
  text(names['block.snc_energies.mineral_synthesizer'],13,11);text(panel.subtitle[0],13,23,panel.accent);
  for(const s of panel.slots)text(names['gui.snc_energies.panel.'+s.role],s.x-2,s.y-12,panel.accent);
  text(rsOn?'Operando':'Redstone inativa',13,132,rsOn?panel.accent:'#e59a4c');
  text('Inventário',48,145);
  const [x,y,w,h]=panel.button;p.fillStyle='#172422';p.fillRect(x,y,w,h);p.fillStyle='#5d6949';p.fillRect(x+1,y+1,w-2,h-2);
  text(names['gui.snc_energies.panel.redstone'],x+4,y+4);
  p.strokeStyle=rsOn?'#5d6949':'#e59a4c';p.lineWidth=1;p.strokeRect(x-1.5,y-1.5,w+3,h+3);
}
draw();
document.getElementById('rs').addEventListener('click',()=>{rsOn=!rsOn;document.getElementById('rsstate').textContent=rsOn?'Operando':'Redstone inativa';draw();});
document.getElementById('level').addEventListener('input',e=>{f=e.target.value/100;draw();});
</script></html>"""

    page = (page
            .replace("__MODELS__", json.dumps(models, separators=(",", ":")))
            .replace("__SOURCES__", json.dumps(sources, separators=(",", ":")))
            .replace("__PANEL__", json.dumps(panel, separators=(",", ":")))
            .replace("__NAMES__", json.dumps(lang, ensure_ascii=False, separators=(",", ":")))
            .replace("__PANEL_TITLE__", lang["block.snc_energies.mineral_synthesizer"]))
    out = ROOT / "previews/v030.html"
    out.write_text(page, encoding="utf-8", newline="\n")
    print(f"Created {out} ({out.stat().st_size / 1024:.0f} KiB, {len(sources)} embedded textures)")


if __name__ == "__main__":
    main()
