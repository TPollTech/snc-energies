import { createVehicleStudio, loadEmbeddedOrFetch } from './vehicle-studio.js';

const $ = (id) => document.getElementById(id);
const controlsState = { motion: false, lift: 0 };
let studio = null;
let wheelAngle = 0;
let ptoAngle = 0;

function applyControls() {
  const s = controlsState;
  // Same convention as the golden rig: negative X on the lift pivot raises
  // the toolbar (the working end) off the ground. angleGroup takes radians.
  studio.angleGroup('planter', 'x', (-23 * s.lift / 100) * Math.PI / 180);
  $('lift').disabled = s.motion;
  $('lift-value').textContent = `${s.lift}%`;
  document.querySelector('.model-tag').textContent =
    `${s.lift > 50 ? 'LEVANTADA · TRANSPORTE' : 'ABAIXADA · EM LINHA'} · ${s.motion ? 'PTO ATIVO' : 'PTO PARADO'}`;
}

function updateMechanisms() {
  for (const name of ['planter_left_wheel', 'planter_right_wheel']) studio.angleGroup(name, 'x', wheelAngle * (13 / 5));
  for (const name of ['row_1_press_wheel', 'row_2_press_wheel', 'row_3_press_wheel']) studio.angleGroup(name, 'x', wheelAngle * (13 / 3.4));
  studio.angleGroup('pto_shaft', 'x', ptoAngle);
}

async function initialize() {
  const [model, manifest] = await Promise.all([
    loadEmbeddedOrFetch(['planter-model-data'], ['../assets/planter/planter-model.json']),
    loadEmbeddedOrFetch(['planter-materials-data'], ['../assets/planter/materials.json']),
  ]);
  const assetBase = new URL('../assets/planter/', import.meta.url);
  studio = createVehicleStudio({
    stage: $('stage'),
    model,
    materials: manifest,
    assetBase,
    canvasLabel: 'Modelo 3D da plantadeira SNC 75-P. Arraste para girar e use a roda do mouse para aproximar.',
    onReset() {
      Object.assign(controlsState, { motion: false, lift: 0 });
      $('toggle-motion').checked = false;
      $('lift').value = 0;
      wheelAngle = ptoAngle = 0;
      updateMechanisms();
      applyControls();
    },
  });
  studio.setTick((delta) => {
    if (!controlsState.motion) return;
    wheelAngle = (wheelAngle + delta * 0.75) % (Math.PI * 200);
    ptoAngle = (ptoAngle + delta * 3.4) % (Math.PI * 200);
    updateMechanisms();
  });
  $('toggle-motion').addEventListener('change', (event) => {
    controlsState.motion = event.target.checked;
    applyControls();
  });
  $('lift').addEventListener('input', (event) => {
    controlsState.lift = Number(event.target.value);
    applyControls();
  });
  applyControls();
  window.planterPreview = { state: controlsState, model, setView: (view) => studio.setView(view) };
}

initialize();
