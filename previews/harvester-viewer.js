import { createVehicleStudio, loadEmbeddedOrFetch } from './vehicle-studio.js';

const $ = (id) => document.getElementById(id);
const controlsState = { header: true, hood: false, motion: false, lift: 0, swing: 0, spout: 0 };
let studio = null;
let wheelAngle = 0;
let rotorAngle = 0;
let fanAngle = 0;
let chopperAngle = 0;

function applyControls() {
  const s = controlsState;
  studio.setGroupVisibility('header', s.header);
  studio.angleGroup('hood', 'x', s.hood ? 55 : 0);
  studio.angleGroup('header', 'x', 26 * s.lift / 100);
  studio.angleGroup('unloading_auger', 'x', -38 * s.swing / 100);
  studio.angleGroup('spout', 'x', 150 * s.spout / 100);
  $('spout').disabled = !s.header;
  $('lift').disabled = !s.header;
  $('swing').disabled = !s.header;
  $('lift-value').textContent = `${s.lift}%`;
  $('swing-value').textContent = `${s.swing}%`;
  $('spout-value').textContent = `${s.spout}%`;
  document.querySelector('.model-tag').textContent = `${s.header ? 'PLATAFORMA ACOPLADA' : 'SEM PLATAFORMA'} · ${s.hood ? 'CAPÔ ABERTO' : 'CAPÔ FECHADO'}`;
}

function updateMechanisms() {
  for (const name of ['front_left_wheel', 'front_right_wheel']) studio.angleGroup(name, 'x', wheelAngle * (13 / 7.5));
  for (const name of ['rear_left_wheel', 'rear_right_wheel']) studio.angleGroup(name, 'x', wheelAngle);
  studio.angleGroup('reel', 'x', wheelAngle * 2.2);
  studio.angleGroup('rotor', 'z', rotorAngle);
  studio.angleGroup('cooling_fan', 'x', fanAngle);
  studio.angleGroup('cleaning_fan', 'x', fanAngle * 1.3);
  studio.angleGroup('straw_chopper', 'x', chopperAngle);
}

async function initialize() {
  const [model, manifest] = await Promise.all([
    loadEmbeddedOrFetch(['harvester-model-data'], ['../assets/harvester/harvester-model.json']),
    loadEmbeddedOrFetch(['harvester-materials-data'], ['../assets/harvester/materials.json']),
  ]);
  const assetBase = new URL('../assets/harvester/', import.meta.url);
  studio = createVehicleStudio({
    stage: $('stage'),
    model,
    materials: manifest,
    assetBase,
    canvasLabel: 'Modelo 3D da colheitadeira SNC 90. Arraste para girar e use a roda do mouse para aproximar.',
    onReset() {
      Object.assign(controlsState, { header: true, hood: false, motion: false, lift: 0, swing: 0, spout: 0 });
      for (const name of ['header', 'hood', 'motion']) $(`toggle-${name}`).checked = controlsState[name];
      for (const name of ['lift', 'swing', 'spout']) $(name).value = controlsState[name];
      wheelAngle = rotorAngle = fanAngle = chopperAngle = 0;
      updateMechanisms();
      applyControls();
    },
  });
  studio.setTick((delta) => {
    if (!controlsState.motion) return;
    wheelAngle = (wheelAngle + delta * 0.75) % (Math.PI * 200);
    rotorAngle = (rotorAngle + delta * 1.7) % (Math.PI * 200);
    fanAngle = (fanAngle + delta * 2.1) % (Math.PI * 200);
    chopperAngle = (chopperAngle + delta * 2.6) % (Math.PI * 200);
    updateMechanisms();
  });
  for (const name of ['header', 'hood', 'motion']) {
    $(`toggle-${name}`).addEventListener('change', (event) => {
      controlsState[name] = event.target.checked;
      applyControls();
    });
  }
  for (const name of ['lift', 'swing', 'spout']) {
    $(name).addEventListener('input', (event) => {
      controlsState[name] = Number(event.target.value);
      applyControls();
    });
  }
  applyControls();
  window.harvesterPreview = { state: controlsState, model, setView: (view) => studio.setView(view) };
}

initialize();
