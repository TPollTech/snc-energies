import { createVehicleStudio, loadEmbeddedOrFetch } from './vehicle-studio.js';

const $ = (id) => document.getElementById(id);

// Geometry facts from generate_industry.py (model pixels):
// ram box = (6,23,4)→(22,27,12); the demo presses it down to y=12.
const RAM_FROM_Y = 23;
const RAM_BOTTOM_PRESSED = 12;
// The game model's body is a solid block; the preview opens the press chamber
// (four walls) so the demo bale stays visible while the piston squeezes it.
const BALE = { from: [9, 4, 5], to: [19, 20, 11], material: 'industry_gauge' };

const controlsState = { ram: 0 };
let studio = null;

function buildModel(data) {
  // Minecraft element list → studio cubes; the ram becomes its own moving group
  // and a demo bale appears inside the chamber while pressing.
  const cubes = [];
  for (const [index, element] of data.elements.entries()) {
    const material = Object.values(element.faces)[0].texture.replace('#', '');
    const cube = { name: `cube_${index}`, from: element.from, to: element.to, material, group: 'compactor' };
    if (index === 2) {
      // Open chamber: front (low), back, left and right walls instead of the solid body.
      cubes.push(
        { name: 'wall_front', from: [2, 3, 2], to: [26, 9, 4], material, group: 'compactor' },
        { name: 'wall_back', from: [2, 3, 12], to: [26, 22, 14], material, group: 'compactor' },
        { name: 'wall_left', from: [2, 9, 4], to: [8, 22, 12], material, group: 'compactor' },
        { name: 'wall_right', from: [20, 9, 4], to: [26, 22, 12], material, group: 'compactor' },
      );
      continue;
    }
    if (element.from[1] === RAM_FROM_Y) cube.group = 'ram';
    cubes.push(cube);
  }
  cubes.push({ name: 'bale', ...BALE, group: 'bale' });
  return {
    name: 'compactor',
    units_per_block: 16,
    groups: [
      { name: 'compactor' },
      { name: 'ram', parent: 'compactor' },
      // Pivot at the bale's bottom center so the squeeze pushes downward.
      { name: 'bale', parent: 'compactor', origin: [14, 4, 8] },
    ],
    cubes,
  };
}

function applyRam() {
  if (!studio) return;
  const progress = controlsState.ram / 100;
  const root = studio.root();
  if (!root) {
    requestAnimationFrame(applyRam);
    return;
  }
  const ramGroup = root.getObjectByName('ram');
  const baleGroup = root.getObjectByName('bale');
  if (ramGroup) ramGroup.position.y = -(RAM_FROM_Y - RAM_BOTTOM_PRESSED) * progress;
  if (baleGroup) {
    baleGroup.visible = progress > 0.02;
    baleGroup.scale.set(1, Math.max(0.5, 1 - 0.5 * progress), 1);
  }
  document.querySelector('.model-tag').textContent =
    progress < 0.02 ? 'PISTÃO EM REPOUSO' : progress > 0.98 ? 'FARDO PRENSADO' : `PRENSANDO · ${Math.round(progress * 100)}%`;
}

async function initialize() {
  const [model, manifest] = await Promise.all([
    loadEmbeddedOrFetch(['compactor-model-data'], ['../previews/industry-models.json']),
    loadEmbeddedOrFetch(['compactor-materials-data'], ['../assets/industry-materials.json']),
  ]);
  // Accept either the full industry-models.json wrapper or a single model.
  const shaped = buildModel(model.compactor || model);
  const assetBase = new URL('../assets/', import.meta.url);
  studio = createVehicleStudio({
    stage: $('stage'),
    model: shaped,
    materials: manifest,
    assetBase,
    viewTarget: [16, 15, 16],
    canvasLabel: 'Modelo 3D da Compactadora. Arraste para girar e use a roda do mouse para aproximar.',
    onReset() {
      controlsState.ram = 0;
      $('ram').value = 0;
      applyRam();
    },
  });
  $('ram').addEventListener('input', (event) => {
    controlsState.ram = Number(event.target.value);
    applyRam();
  });
  applyRam();
  window.compactorPreview = { state: controlsState, model: shaped, setView: (view) => studio.setView(view) };
}

initialize().catch((error) => console.error(error));
