// SpaceReloaded — просмотр 3D-моделей предметов и мультиблоков (клик по слоту или схеме с data-model).
// Модели выгружает tools/sitegen/models3d.py: элементы блочной модели Minecraft (from/to, поворот,
// грани с UV) или плоская текстура предмета, которую просмотрщик выдавливает по пикселям, как игра.
// three.js грузится лениво при первом открытии.

const FACES = {
  up:    [[0, 1, 0], [1, 1, 0], [1, 1, 1], [0, 1, 1]],
  down:  [[0, 0, 1], [1, 0, 1], [1, 0, 0], [0, 0, 0]],
  north: [[1, 1, 0], [0, 1, 0], [0, 0, 0], [1, 0, 0]],
  south: [[0, 1, 1], [1, 1, 1], [1, 0, 1], [0, 0, 1]],
  west:  [[0, 1, 0], [0, 1, 1], [0, 0, 1], [0, 0, 0]],
  east:  [[1, 1, 1], [1, 1, 0], [1, 0, 0], [1, 0, 1]],
};
const VIEW = [-1, 0.85, -1]; // как у иконок сайта: сверху, с северо-запада

let lib = null;
async function three() {
  if (!lib) {
    const [T, C] = await Promise.all([import('three'), import('three/addons/controls/OrbitControls.js')]);
    lib = { T, OrbitControls: C.OrbitControls };
  }
  return lib;
}

// освещение граней как в игре: верх 1.0, низ 0.5, север/юг 0.8, запад/восток 0.6
function shade(n) {
  return n[0] * n[0] * 0.6 + n[1] * n[1] * (n[1] > 0 ? 1 : 0.5) + n[2] * n[2] * 0.8;
}

function rotate(p, rot) {
  if (!rot) return p;
  const a = rot.angle * Math.PI / 180, c = Math.cos(a), s = Math.sin(a), o = rot.origin;
  let x = p[0] - o[0], y = p[1] - o[1], z = p[2] - o[2];
  if (rot.rescale) {
    const k = 1 / Math.cos(a);
    if (rot.axis !== 'x') x *= k;
    if (rot.axis !== 'y') y *= k;
    if (rot.axis !== 'z') z *= k;
  }
  if (rot.axis === 'x') [y, z] = [y * c - z * s, y * s + z * c];
  else if (rot.axis === 'y') [x, z] = [x * c + z * s, -x * s + z * c];
  else [x, y] = [x * c - y * s, x * s + y * c];
  return [x + o[0], y + o[1], z + o[2]];
}

function loadTexture(T, url) {
  return new Promise((ok, fail) => new T.TextureLoader().load(url, (tex) => {
    tex.magFilter = T.NearestFilter;
    tex.minFilter = T.NearestFilter;
    tex.generateMipmaps = false;
    tex.colorSpace = T.SRGBColorSpace;
    ok(tex);
  }, undefined, fail));
}

function material(T, tex) {
  return new T.MeshBasicMaterial({ map: tex, vertexColors: true, alphaTest: 0.1, side: T.DoubleSide });
}

function geometry(T, quads) {
  const pos = [], uv = [], col = [], idx = [];
  for (const q of quads) {
    const base = pos.length / 3;
    for (let i = 0; i < 4; i++) {
      pos.push(...q.p[i]);
      uv.push(...q.uv[i]);
      col.push(q.shade, q.shade, q.shade);
    }
    idx.push(base, base + 1, base + 2, base, base + 2, base + 3);
  }
  const g = new T.BufferGeometry();
  g.setAttribute('position', new T.Float32BufferAttribute(pos, 3));
  g.setAttribute('uv', new T.Float32BufferAttribute(uv, 2));
  g.setAttribute('color', new T.Float32BufferAttribute(col, 3));
  g.setIndex(idx);
  return g;
}

// блочная модель: грани элементов, сгруппированные по текстуре
async function buildBlock(T, data) {
  const textures = await Promise.all(data.textures.map((u) => loadTexture(T, u)));
  const groups = data.textures.map(() => []);
  for (const el of data.elements) {
    const f = el.from, t = el.to;
    for (const [name, face] of Object.entries(el.faces)) {
      const corners = FACES[name].map((c) => rotate([
        c[0] ? t[0] : f[0], c[1] ? t[1] : f[1], c[2] ? t[2] : f[2]], el.rot).map((v) => v / 16));
      const [u1, v1, u2, v2] = face.uv;
      const uvc = [[u1, v1], [u2, v1], [u2, v2], [u1, v2]];
      const shift = ((face.r || 0) / 90) % 4;
      const uv = [0, 1, 2, 3].map((i) => { const c = uvc[(i + shift) % 4]; return [c[0] / 16, 1 - c[1] / 16]; });
      const a = corners[0], b = corners[1], d = corners[3];
      const e1 = [b[0] - a[0], b[1] - a[1], b[2] - a[2]], e2 = [d[0] - a[0], d[1] - a[1], d[2] - a[2]];
      const n = [e2[1] * e1[2] - e2[2] * e1[1], e2[2] * e1[0] - e2[0] * e1[2], e2[0] * e1[1] - e2[1] * e1[0]];
      const ln = Math.hypot(...n) || 1;
      groups[face.t].push({ p: corners, uv, shade: el.noshade ? 1 : shade(n.map((v) => v / ln)) });
    }
  }
  const root = new T.Group();
  groups.forEach((quads, i) => { if (quads.length) root.add(new T.Mesh(geometry(T, quads), material(T, textures[i]))); });
  return root;
}

// плоский предмет: лицевая и тыльная грани + рёбра по контуру непрозрачных пикселей (толщина 1/16)
async function buildFlat(T, url) {
  const tex = await loadTexture(T, url);
  const img = tex.image, w = img.width, h = img.height;
  const cv = document.createElement('canvas');
  cv.width = w; cv.height = h;
  const cx = cv.getContext('2d');
  cx.drawImage(img, 0, 0);
  const px = cx.getImageData(0, 0, w, h).data;
  const solid = (x, y) => x >= 0 && y >= 0 && x < w && y < h && px[(y * w + x) * 4 + 3] > 25;
  const d = 0.5 / 16, quads = [];
  const P = (x, y, z) => [x / w, 1 - y / h, z];
  quads.push({ p: [P(0, 0, d), P(w, 0, d), P(w, h, d), P(0, h, d)], uv: [[0, 1], [1, 1], [1, 0], [0, 0]], shade: 1 });
  quads.push({ p: [P(w, 0, -d), P(0, 0, -d), P(0, h, -d), P(w, h, -d)], uv: [[1, 1], [0, 1], [0, 0], [1, 0]], shade: 0.8 });
  for (let y = 0; y < h; y++) {
    for (let x = 0; x < w; x++) {
      if (!solid(x, y)) continue;
      const u = (x + 0.5) / w, v = 1 - (y + 0.5) / h, uv = [[u, v], [u, v], [u, v], [u, v]];
      if (!solid(x, y - 1)) quads.push({ p: [P(x, y, d), P(x + 1, y, d), P(x + 1, y, -d), P(x, y, -d)], uv, shade: 1 });
      if (!solid(x, y + 1)) quads.push({ p: [P(x, y + 1, -d), P(x + 1, y + 1, -d), P(x + 1, y + 1, d), P(x, y + 1, d)], uv, shade: 0.5 });
      if (!solid(x - 1, y)) quads.push({ p: [P(x, y, -d), P(x, y + 1, -d), P(x, y + 1, d), P(x, y, d)], uv, shade: 0.6 });
      if (!solid(x + 1, y)) quads.push({ p: [P(x + 1, y, d), P(x + 1, y + 1, d), P(x + 1, y + 1, -d), P(x + 1, y, -d)], uv, shade: 0.6 });
    }
  }
  const root = new T.Group();
  root.add(new T.Mesh(geometry(T, quads), material(T, tex)));
  return root;
}

// мультиблок: сцена из блочных моделей — позиция клетки и поворот варианта блокстейта (сначала X, потом Y,
// как в игре); одинаковые модели строятся один раз и клонируются (геометрия и материалы общие)
async function buildScene(T, data) {
  const cache = new Map();
  const root = new T.Group();
  for (const part of data.parts) {
    if (part.m && !cache.has(part.m)) cache.set(part.m, fetch(part.m).then((r) => r.json()).then((d) => buildBlock(T, d)));
  }
  for (const part of data.parts) {
    if (part.mesh) {
      // готовая сетка (оболочка тарелки, растяжки): вершины уже в координатах сцены
      const tex = await loadTexture(T, part.mesh.t);
      const quads = part.mesh.quads.map((q) => ({ p: q.p, uv: q.uv.map(([u, v]) => [u, 1 - v]), shade: q.s }));
      root.add(new T.Mesh(geometry(T, quads), material(T, tex)));
      continue;
    }
    const inner = (await cache.get(part.m)).clone();
    inner.position.set(-0.5, -0.5, -0.5);
    const g = new T.Group();
    g.add(inner);
    if (part.m3) {
      // произвольный поворот (плитки тарелки по нормали параболоида), строки матрицы 3×3
      const r = part.m3;
      g.quaternion.setFromRotationMatrix(new T.Matrix4().set(r[0], r[1], r[2], 0, r[3], r[4], r[5], 0, r[6], r[7], r[8], 0, 0, 0, 0, 1));
    } else {
      g.rotation.set(-(part.x || 0) * Math.PI / 180, -(part.y || 0) * Math.PI / 180, 0, 'YXZ');
    }
    g.position.set(part.p[0] + 0.5, part.p[1] + 0.5, part.p[2] + 0.5);
    root.add(g);
  }
  return root;
}

let ui = null;
function dialog() {
  if (ui) return ui;
  const dlg = document.createElement('dialog');
  dlg.className = 'v3d';
  dlg.innerHTML = '<div class="v3d-head"><b></b><button type="button" class="v3d-close" aria-label="Закрыть">×</button></div>'
    + '<div class="v3d-stage" aria-hidden="true"></div>'
    + '<p class="v3d-hint">Тяните — вращать · колесо или щипок — приблизить · двойной клик — вид по умолчанию</p>';
  document.body.appendChild(dlg);
  dlg.querySelector('.v3d-close').addEventListener('click', () => dlg.close());
  dlg.addEventListener('click', (e) => { if (e.target === dlg) dlg.close(); });
  ui = { dlg, title: dlg.querySelector('b'), stage: dlg.querySelector('.v3d-stage'), hint: dlg.querySelector('.v3d-hint') };
  return ui;
}

let session = null;
async function open(slot) {
  const { dlg, title, stage, hint } = dialog();
  title.textContent = slot.getAttribute('data-name') || '';
  dlg.classList.toggle('wide', slot.tagName === 'FIGURE');   // мультиблок — большое окно
  stage.classList.add('loading');
  if (!dlg.open) dlg.showModal();
  let T, OrbitControls, data;
  try {
    ({ T, OrbitControls } = await three());
    data = await (await fetch(slot.getAttribute('data-model'))).json();
  } catch (err) {
    stage.classList.remove('loading');
    stage.textContent = 'Не удалось загрузить модель: ' + err;
    return;
  }
  close();
  hint.textContent = 'Тяните — вращать · колесо или щипок — приблизить'
    + (data.parts ? ' · правая кнопка или два пальца — сдвинуть' : '') + ' · двойной клик — вид по умолчанию';
  const model = data.parts ? await buildScene(T, data) : data.flat ? await buildFlat(T, data.flat) : await buildBlock(T, data);
  const box = new T.Box3().setFromObject(model);
  const center = box.getCenter(new T.Vector3());
  model.position.sub(center);
  const radius = box.getSize(new T.Vector3()).length() / 2 || 0.5;

  const renderer = new T.WebGLRenderer({ antialias: true, alpha: true });
  renderer.setPixelRatio(Math.min(2, window.devicePixelRatio || 1));
  renderer.outputColorSpace = T.SRGBColorSpace;
  stage.textContent = '';
  stage.classList.remove('loading');
  stage.appendChild(renderer.domElement);
  const scene = new T.Scene();
  scene.add(model);
  const camera = new T.PerspectiveCamera(32, 1, radius / 50, radius * 50);
  const dist = radius / Math.sin(camera.fov * Math.PI / 360) * 1.08;
  const home = new T.Vector3(...VIEW).normalize().multiplyScalar(dist);
  camera.position.copy(home);
  const controls = new OrbitControls(camera, renderer.domElement);
  controls.enableDamping = true;
  controls.enablePan = !!data.parts;   // по большой структуре удобно ходить (правая кнопка / два пальца)
  controls.minDistance = dist * (data.parts ? 0.08 : 0.35);   // мультиблок — подойти к отдельному блоку
  controls.maxDistance = dist * 3;
  controls.autoRotate = !matchMedia('(prefers-reduced-motion: reduce)').matches;
  controls.autoRotateSpeed = 1.6;
  controls.addEventListener('start', () => { controls.autoRotate = false; });
  renderer.domElement.addEventListener('dblclick', () => { camera.position.copy(home); controls.target.set(0, 0, 0); });

  const resize = () => {
    const w = stage.clientWidth, h = stage.clientHeight;
    renderer.setSize(w, h, false);
    camera.aspect = w / h;
    camera.updateProjectionMatrix();
  };
  const ro = new ResizeObserver(resize);
  ro.observe(stage);
  resize();
  let frame = 0;
  const loop = () => { frame = requestAnimationFrame(loop); controls.update(); renderer.render(scene, camera); };
  loop();
  session = { renderer, controls, ro, scene, stop: () => cancelAnimationFrame(frame) };
}

function close() {
  if (!session) return;
  session.stop();
  session.ro.disconnect();
  session.controls.dispose();
  const seen = new Set();   // клоны делят геометрию и материалы — освобождаем по разу
  session.scene.traverse((o) => {
    if (o.geometry && !seen.has(o.geometry)) { seen.add(o.geometry); o.geometry.dispose(); }
    if (o.material && !seen.has(o.material)) {
      seen.add(o.material);
      if (o.material.map) o.material.map.dispose();
      o.material.dispose();
    }
  });
  session.renderer.dispose();
  session.renderer.domElement.remove();
  session = null;
}

document.addEventListener('click', (e) => {
  const slot = e.target.closest && e.target.closest('[data-model]');
  if (slot) { e.preventDefault(); open(slot); }
});
document.addEventListener('keydown', (e) => {
  if ((e.key === 'Enter' || e.key === ' ') && e.target.matches && e.target.matches('[data-model]')) {
    e.preventDefault();
    open(e.target);
  }
});
document.addEventListener('close', (e) => { if (e.target.classList && e.target.classList.contains('v3d')) close(); }, true);
