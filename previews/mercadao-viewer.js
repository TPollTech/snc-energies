import { createVehicleStudio, loadEmbeddedOrFetch } from './vehicle-studio.js';

const $ = (id) => document.getElementById(id);
const controlsState = { roof: true, awnings: true, npc: true, anchor: false };
let studio = null;

const ROOF_CUBES = [];
function indexRoof(model) {
  // Roof = the full 16px-thick planks layer at y = 5*16.
  for (const cube of model.cubes) {
    const [, y0] = cube.from;
    if (y0 === 5 * 16 && cube.material === 'spruce_planks') ROOF_CUBES.push(cube.name);
  }
}

function applyControls() {
  const s = controlsState;
  if (!studio) return;
  // Cubes live inside the single "mercadao" group; toggle via mesh visibility.
  // The meshes are built asynchronously: retry on the next frame until the
  // root exists so late toggles are always applied.
  const root = studio.root();
  if (!root) {
    requestAnimationFrame(applyControls);
    return;
  }
  root.traverse((object) => {
    if (!object.isMesh) return;
    const name = object.name;
    if (ROOF_CUBES.includes(name)) object.visible = s.roof;
    else if (name.startsWith('counter_glass_') || name.startsWith('counter_shelf_')) object.visible = s.awnings;
    else if (name.startsWith('npc_')) object.visible = s.npc;
    else if (name.startsWith('anchor_')) object.visible = s.anchor;
  });
  document.querySelector('.model-tag').textContent =
    `${s.roof ? 'ESTRUTURA COMPLETA' : 'INTERIOR EXPOSTO'} · ${s.npc ? 'MERCADOR NO POSTO' : 'POSTO VAZIO'}`;
}

async function initialize() {
  const [model, manifest] = await Promise.all([
    loadEmbeddedOrFetch(['mercadao-model-data'], ['../previews/mercadao-model.json']),
    loadEmbeddedOrFetch(['mercadao-materials-data'], ['../assets/mercadao-materials.json']),
  ]);
  indexRoof(model);
  const assetBase = new URL('../assets/', import.meta.url);
  studio = createVehicleStudio({
    stage: $('stage'),
    model,
    materials: manifest,
    assetBase,
    viewTarget: [88, 40, 72],
    canvasLabel: 'Modelo 3D do Mercadão do SNC. Arraste para girar e use a roda do mouse para aproximar.',
    onReset() {
      Object.assign(controlsState, { roof: true, awnings: true, npc: true, anchor: false });
      for (const name of ['roof', 'awnings', 'npc', 'anchor']) $(`toggle-${name}`).checked = controlsState[name];
      applyControls();
    },
  });
  for (const name of ['roof', 'awnings', 'npc', 'anchor']) {
    $(`toggle-${name}`).addEventListener('change', (event) => {
      controlsState[name] = event.target.checked;
      applyControls();
    });
  }
  applyControls();
  window.mercadaoPreview = { state: controlsState, model, setView: (view) => studio.setView(view) };
}

initialize().catch((error) => console.error(error));
