import * as THREE from 'three';
import { OrbitControls } from 'three-controls';

// Shared vehicle studio (SNC 75 golden pattern, AGENTS.md rule 7).
// Parameterized extraction of previews/tractor-viewer.js: same renderer
// settings, studio lighting, ground/grid, orbit damping, automatic
// bounding-box framing, view tweens and metrics for every vehicle page.

const deg = THREE.MathUtils.degToRad;
const reducedMotion = typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const DEFAULT_VIEWS = {
  perspective: new THREE.Vector3(-85, 56, -104),
  front: new THREE.Vector3(0, 31, -140),
  side: new THREE.Vector3(-145, 29, 0),
  rear: new THREE.Vector3(0, 41, 145),
};

export async function loadEmbeddedOrFetch(ids, urls) {
  for (let i = 0; i < ids.length; i++) {
    const embedded = document.getElementById(ids[i]);
    if (embedded) return JSON.parse(embedded.textContent);
    if (!urls[i]) continue;
    const response = await fetch(new URL(urls[i], document.baseURI));
    if (!response.ok) throw new Error(`Não foi possível carregar ${urls[i]} (${response.status}).`);
    return response.json();
  }
  throw new Error('Modelo ou materiais indisponíveis nesta página.');
}

export function createVehicleStudio(config) {
  const stage = config.stage;
  const $ = (id) => document.getElementById(id);
  const workspace = config.workspace || document.querySelector('.workspace');
  const viewDirections = {};
  for (const [name, direction] of Object.entries({ ...DEFAULT_VIEWS, ...(config.views || {}) })) {
    viewDirections[name] = new THREE.Vector3(...direction);
  }
  const viewTarget = new THREE.Vector3(...(config.viewTarget || [0, 18, 6]));
  const groups = {};
  const materials = {};
  let renderer, scene, camera, orbit, root, modelBounds, cameraTween;
  let tick = null;
  let toastTimeout;

  function announce(message) {
    const toast = $('toast');
    if (!toast) return;
    toast.textContent = message;
    toast.classList.add('visible');
    window.clearTimeout(toastTimeout);
    toastTimeout = window.setTimeout(() => toast.classList.remove('visible'), 2000);
  }

  function setRestRotation(object, values = [0, 0, 0]) {
    object.rotation.order = 'ZYX';
    object.rotation.set(...values.map(deg));
    object.userData.restRotation = object.rotation.clone();
  }

  function createScene() {
    scene = new THREE.Scene();
    scene.background = new THREE.Color('#eeece5');
    scene.fog = new THREE.Fog('#eeece5', 240, 600);
    camera = new THREE.PerspectiveCamera(36, 1, 0.1, 1200);
    camera.position.set(-115, 77, -130);
    renderer = new THREE.WebGLRenderer({ antialias: true, alpha: false });
    renderer.setPixelRatio(Math.min(window.devicePixelRatio || 1, 2));
    renderer.shadowMap.enabled = true;
    renderer.shadowMap.type = THREE.PCFSoftShadowMap;
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.15;
    renderer.domElement.setAttribute('aria-label', config.canvasLabel || 'Modelo 3D. Arraste para girar e use a roda do mouse para aproximar.');
    renderer.domElement.setAttribute('role', 'img');
    renderer.domElement.setAttribute('tabindex', '0');
    stage.appendChild(renderer.domElement);

    orbit = new OrbitControls(camera, renderer.domElement);
    orbit.target.copy(viewTarget);
    orbit.enableDamping = true;
    orbit.dampingFactor = 0.075;
    orbit.minDistance = 40;
    orbit.maxDistance = 380;
    orbit.maxPolarAngle = Math.PI / 2 - 0.015;
    orbit.minPolarAngle = 0.12;
    orbit.enablePan = true;
    orbit.screenSpacePanning = true;
    orbit.addEventListener('start', () => {
      cameraTween = null;
      document.querySelectorAll('[data-view]').forEach((button) => button.setAttribute('aria-pressed', 'false'));
    });

    scene.add(new THREE.HemisphereLight(0xfffaf0, 0x9aa590, 2.7));
    const key = new THREE.DirectionalLight(0xfff2d7, 3.1);
    key.position.set(-70, 140, -85);
    key.castShadow = true;
    key.shadow.mapSize.set(2048, 2048);
    Object.assign(key.shadow.camera, { left: -95, right: 95, top: 95, bottom: -95, near: 1, far: 320 });
    key.shadow.bias = -0.00025;
    key.shadow.normalBias = 0.09;
    key.target.position.set(0, 12, 10);
    scene.add(key, key.target);
    const fill = new THREE.DirectionalLight(0xe7f0ff, 1.3);
    fill.position.set(85, 65, 40);
    scene.add(fill);

    const ground = new THREE.Mesh(new THREE.PlaneGeometry(1400, 1400), new THREE.MeshStandardMaterial({ color: '#e7e8de', roughness: 1 }));
    ground.rotation.x = -Math.PI / 2;
    ground.position.y = -0.22;
    ground.receiveShadow = true;
    scene.add(ground);
    const grid = new THREE.GridHelper(448, 28, 0xb5bea6, 0xc3cab6);
    grid.position.y = -0.19;
    grid.material.transparent = true;
    grid.material.opacity = 0.25;
    grid.material.depthWrite = false;
    scene.add(grid);

    new ResizeObserver(resize).observe(stage);
    resize();
  }

  function resize() {
    if (!renderer || !camera) return;
    const width = stage.clientWidth;
    const height = stage.clientHeight;
    if (!width || !height) return;
    camera.aspect = width / height;
    camera.updateProjectionMatrix();
    renderer.setSize(width, height, false);
    const activeView = document.querySelector('[data-view][aria-pressed=true]');
    if (modelBounds && activeView && !cameraTween) setView(activeView.dataset.view, false);
  }

  async function createMaterials(manifest) {
    const assetBase = config.assetBase;
    const materialEntries = Object.entries(manifest.materials || manifest).filter(([, value]) => value && typeof value === 'object' && ('file' in value || 'color' in value));
    const textureLoader = new THREE.TextureLoader();
    const unavailableTextures = [];
    await Promise.all(materialEntries.map(async ([name, definition]) => {
      let texture = null;
      if (definition.file) {
        try {
          texture = await textureLoader.loadAsync(new URL(definition.file, assetBase).href);
          texture.colorSpace = THREE.SRGBColorSpace;
          texture.magFilter = THREE.NearestFilter;
          texture.minFilter = THREE.NearestMipmapNearestFilter;
          texture.wrapS = THREE.RepeatWrapping;
          texture.wrapT = THREE.RepeatWrapping;
          texture.anisotropy = Math.min(4, renderer.capabilities.getMaxAnisotropy());
        } catch (error) {
          unavailableTextures.push(name);
          console.warn(`Textura indisponível: ${name}`, error);
        }
      }
      materials[name] = new THREE.MeshStandardMaterial({
        name,
        color: texture ? 0xffffff : (definition.color ?? '#92968b'),
        map: texture,
        roughness: definition.roughness ?? 0.8,
        metalness: definition.metalness ?? 0.05,
      });
    }));
    const swatchContainer = $('materials');
    if (swatchContainer) {
      for (const [name, definition] of materialEntries) {
        const swatch = document.createElement('span');
        swatch.className = 'swatch';
        swatch.title = definition.label || name.replaceAll('_', ' ');
        swatch.setAttribute('aria-label', swatch.title);
        swatch.style.backgroundColor = definition.color || '#92968b';
        if (definition.file) swatch.style.backgroundImage = `url("${new URL(definition.file, assetBase).href}")`;
        swatchContainer.appendChild(swatch);
      }
      const label = document.createElement('span');
      label.className = 'materials-count';
      label.textContent = `${materialEntries.length} materiais`;
      swatchContainer.appendChild(label);
    }
    return unavailableTextures;
  }

  function createModel(data) {
    root = new THREE.Group();
    root.name = data.name || 'vehicle';
    scene.add(root);
    const definitions = new Map(data.groups.map((group) => [group.name, group]));
    for (const group of data.groups) {
      const object = new THREE.Group();
      object.name = group.name;
      object.userData.origin = new THREE.Vector3(...(group.origin || [0, 0, 0]));
      setRestRotation(object, group.rotation);
      groups[group.name] = object;
    }
    for (const group of data.groups) {
      const object = groups[group.name];
      const parent = group.parent ? groups[group.parent] : root;
      if (!parent) throw new Error(`Grupo pai ausente: ${group.parent}`);
      object.position.copy(object.userData.origin);
      if (group.parent) object.position.sub(groups[group.parent].userData.origin);
      parent.add(object);
    }
    const fallbackMaterial = new THREE.MeshStandardMaterial({ color: '#d56d2c', roughness: 0.8 });
    for (const cube of data.cubes) {
      const low = new THREE.Vector3(...cube.from);
      const high = new THREE.Vector3(...cube.to);
      const center = low.clone().add(high).multiplyScalar(0.5);
      const size = high.clone().sub(low);
      const box = new THREE.BoxGeometry(Math.abs(size.x), Math.abs(size.y), Math.abs(size.z));
      const mesh = new THREE.Mesh(box, materials[cube.material] || fallbackMaterial);
      mesh.name = cube.name || cube.material;
      mesh.castShadow = true;
      mesh.receiveShadow = true;
      mesh.userData.cube = cube;
      const group = groups[cube.group] || root;
      const groupOrigin = definitions.has(cube.group) ? groups[cube.group].userData.origin : new THREE.Vector3();
      const rotation = cube.rotation || [0, 0, 0];
      if (rotation.some((value) => value !== 0)) {
        const pivot = new THREE.Group();
        pivot.name = `${mesh.name}_pivot`;
        pivot.rotation.order = 'ZYX';
        const origin = cube.origin ? new THREE.Vector3(...cube.origin) : center;
        pivot.position.copy(origin).sub(groupOrigin);
        pivot.rotation.set(...rotation.map(deg));
        mesh.position.copy(center).sub(origin);
        pivot.add(mesh);
        group.add(pivot);
      } else {
        mesh.position.copy(center).sub(groupOrigin);
        group.add(mesh);
      }
    }
    root.updateMatrixWorld(true);
    modelBounds = new THREE.Box3().setFromObject(root);
    const dimensions = modelBounds.getSize(new THREE.Vector3()).divideScalar(data.units_per_block || 16);
    const cubeCount = $('cube-count');
    const dimensionsLabel = $('dimensions');
    if (cubeCount) cubeCount.textContent = data.cubes.length.toLocaleString('pt-BR');
    if (dimensionsLabel) dimensionsLabel.textContent = [dimensions.x, dimensions.y, dimensions.z].map((value) => value.toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })).join(' × ');
    const center = modelBounds.getCenter(new THREE.Vector3());
    viewTarget.set(center.x, center.y * 0.76, center.z);
    orbit.target.copy(viewTarget);
    setView(config.initialView || 'perspective', false);
  }

  function angleGroup(name, axis, offset) {
    const group = groups[name];
    if (group) group.rotation[axis] = group.userData.restRotation[axis] + offset;
  }

  function setGroupVisibility(name, visible) {
    if (groups[name]) groups[name].visible = visible;
  }

  function setView(name, animate = !reducedMotion) {
    const direction = (viewDirections[name] || viewDirections.perspective).clone().normalize();
    // Project all bounding-box corners to the selected view so wide and portrait windows both fit.
    const forward = direction;
    const right = new THREE.Vector3().crossVectors(new THREE.Vector3(0, 1, 0), forward).normalize();
    const up = new THREE.Vector3().crossVectors(forward, right).normalize();
    const verticalTangent = Math.tan(deg(camera.fov) / 2);
    const horizontalTangent = verticalTangent * camera.aspect;
    let requiredDistance = 100;
    if (modelBounds) {
      for (const x of [modelBounds.min.x, modelBounds.max.x]) {
        for (const y of [modelBounds.min.y, modelBounds.max.y]) {
          for (const z of [modelBounds.min.z, modelBounds.max.z]) {
            const corner = new THREE.Vector3(x, y, z).sub(viewTarget);
            const depth = corner.dot(forward);
            requiredDistance = Math.max(requiredDistance, Math.abs(corner.dot(right)) / horizontalTangent + depth, Math.abs(corner.dot(up)) / verticalTangent + depth);
          }
        }
      }
    } else requiredDistance = 200;
    const destination = viewTarget.clone().addScaledVector(forward, requiredDistance * 1.3);
    if (animate) {
      cameraTween = { start: performance.now(), from: camera.position.clone(), targetFrom: orbit.target.clone(), to: destination, duration: 520 };
    } else {
      cameraTween = null;
      camera.position.copy(destination);
      orbit.target.copy(viewTarget);
      orbit.update();
    }
    document.querySelectorAll('[data-view]').forEach((button) => button.setAttribute('aria-pressed', String(button.dataset.view === name)));
  }

  function bindViewButtons() {
    document.querySelectorAll('[data-view]').forEach((button) => button.addEventListener('click', () => setView(button.dataset.view)));
    const reset = $('reset');
    if (reset) reset.addEventListener('click', () => {
      if (config.onReset) config.onReset();
      setView(config.initialView || 'perspective');
      announce('Modelo e controles restaurados');
    });
    const fullscreen = $('fullscreen');
    if (fullscreen) fullscreen.addEventListener('click', async () => {
      try {
        if (document.fullscreenElement) await document.exitFullscreen();
        else if (workspace.requestFullscreen) await workspace.requestFullscreen();
        else announce('Tela cheia indisponível neste navegador');
      } catch { announce('Tela cheia indisponível neste navegador'); }
    });
    document.addEventListener('fullscreenchange', () => {
      resize();
      const activeView = document.querySelector('[data-view][aria-pressed=true]');
      if (activeView) requestAnimationFrame(() => setView(activeView.dataset.view, false));
    });
  }

  function animate(time) {
    const delta = Math.min((time - (animate.last || time)) / 1000, 0.05);
    animate.last = time;
    if (!document.hidden) {
      if (tick) tick(delta);
      if (cameraTween) {
        const progress = Math.min((time - cameraTween.start) / cameraTween.duration, 1);
        const eased = 1 - Math.pow(1 - progress, 3);
        camera.position.lerpVectors(cameraTween.from, cameraTween.to, eased);
        orbit.target.lerpVectors(cameraTween.targetFrom, viewTarget, eased);
        if (progress === 1) cameraTween = null;
      }
      orbit.update();
      renderer.render(scene, camera);
    }
  }

  async function initialize() {
    try {
      createScene();
      const unavailableTextures = await createMaterials(config.materials);
      createModel(config.model);
      bindViewButtons();
      if (config.onReady) config.onReady(api);
      $('loading').classList.add('hidden');
      const status = $('render-status');
      if (status) status.textContent = unavailableTextures.length ? 'Texturas incompletas' : 'Modelo 3D / local';
      renderer.setAnimationLoop(animate);
      if (unavailableTextures.length) announce(`${unavailableTextures.length} textura(s) não carregada(s)`);
    } catch (error) {
      console.error(error);
      const loading = $('loading');
      if (loading) {
        loading.classList.add('error');
        const title = $('loading-title');
        const text = $('loading-text');
        if (title) title.textContent = 'A prévia não pôde ser carregada';
        if (text) text.textContent = `${error.message} Abra esta página pelo servidor local do projeto.`;
      }
      const status = $('render-status');
      if (status) status.textContent = 'Modelo indisponível';
      throw error;
    }
  }

  const api = {
    scene: () => scene, renderer: () => renderer, camera: () => camera, orbit: () => orbit,
    groups, materials, bounds: () => modelBounds, root: () => root,
    viewTarget, announce, angleGroup, setGroupVisibility, setView, resize,
    setTick: (fn) => { tick = fn; },
  };
  initialize();
  return api;
}
