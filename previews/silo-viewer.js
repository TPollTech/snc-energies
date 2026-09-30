import { createVehicleStudio, loadEmbeddedOrFetch } from './vehicle-studio.js';

const $ = (id) => document.getElementById(id);

// The silo model (pixels): 2x2 blocks = 32 px footprint, 48 px tall.
// Panels (the amber "walls") sit in three bands: y 2..14, 16..28, 30..42.
// The grain fill materializes one amber/grain slab per band, from bottom up.
const BANDS = [
  { y0: 3, y1: 14 },
  { y0: 17, y1: 28 },
  { y0: 31, y1: 42 },
];

const controlsState = { grain: 0 };
let studio = null;

function buildModel(data) {
  const cubes = data.elements.map((element, index) => ({
    name: `cube_${index}`,
    from: element.from,
    to: element.to,
    material: Object.values(element.faces)[0].texture.replace('#', ''),
    group: 'silo',
  }));
  return {
    name: 'silo',
    units_per_block: 16,
    groups: [{ name: 'silo' }],
    cubes,
  };
}

// Grain fill: 2×2 inset slabs, one per band, revealed as the level rises.
const GRAIN = { from: [2, 3, 2], to: [30, 14, 30], material: 'industry_panel' };

function applyGrain() {
  if (!studio) return;
  const fraction = controlsState.grain / 100;
  const root = studio.root();
  if (!root) {
    requestAnimationFrame(applyGrain);
    return;
  }
  const total = BANDS.length;
  root.traverse((object) => {
    if (!object.isMesh || !object.name.startsWith('grain_')) return;
    const index = Number(object.name.split('_')[1]);
    const fill = Math.min(1, Math.max(0, fraction * total - index));
    object.visible = fill > 0.01;
    object.scale.y = Math.max(0.02, fill);
  });
  const level = Math.min(100, Math.round(fraction * 100));
  document.querySelector('.model-tag').textContent =
    level === 0 ? 'SILO VAZIO' : level >= 100 ? 'SILO CHEIO · 16 FILEIRAS' : `ENCHENDO · ${level}%`;
}

async function initialize() {
  const [model, manifest] = await Promise.all([
    loadEmbeddedOrFetch(['silo-model-data'], ['../previews/industry-models.json']),
    loadEmbeddedOrFetch(['silo-materials-data'], ['../assets/industry-materials.json']),
  ]);
  // Accept either the full industry-models.json wrapper or a single model.
  const shaped = buildModel(model.silo || model);
  // One grain slab per band, inset 2px so the panel ring stays visible.
  BANDS.forEach((band, index) => {
    shaped.cubes.push({
      name: `grain_${index}`,
      from: [2, band.y0, 2],
      to: [30, band.y1, 30],
      material: 'industry_gauge',
      group: 'silo',
    });
  });
  const assetBase = new URL('../assets/', import.meta.url);
  studio = createVehicleStudio({
    stage: $('stage'),
    model: shaped,
    materials: manifest,
    assetBase,
    viewTarget: [16, 22, 16],
    canvasLabel: 'Modelo 3D do Silo. Arraste para girar e use a roda do mouse para aproximar.',
    onReset() {
      controlsState.grain = 0;
      $('grain').value = 0;
      applyGrain();
    },
  });
  $('grain').addEventListener('input', (event) => {
    controlsState.grain = Number(event.target.value);
    applyGrain();
  });
  applyGrain();
  window.siloPreview = { state: controlsState, model: shaped, setView: (view) => studio.setView(view) };
}

initialize().catch((error) => console.error(error));
