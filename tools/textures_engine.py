"""High-quality procedural texture engine for SNC Energies (128x128 rule).

Replaces the flat System.Drawing rectangles with layered value noise,
anisotropic brushed metal, bevel lighting, contact shadows and soft
emissive glows. Everything is deterministic: a fixed integer seed feeds
an LCG so every run produces byte-identical PNGs.

All canvases are float RGBA arrays (0..255). Painters return canvases so
they can be layered; blit() composites with alpha.
"""
from __future__ import annotations

import numpy as np
from PIL import Image

SIZE = 128


# ----------------------------------------------------------------- RNG / noise
class Rng:
    """Small deterministic LCG so output never depends on numpy version."""

    def __init__(self, seed: int):
        self.state = int(seed) & 0x7FFFFFFF
        if self.state == 0:
            self.state = 0x2545F491

    def next(self) -> int:
        self.state = (self.state * 48271) % 0x7FFFFFFF
        return self.state

    def f(self) -> float:
        return self.next() / 0x7FFFFFFF

    def range(self, a: float, b: float) -> float:
        return a + (b - a) * self.f()

    def int(self, a: int, b: int) -> int:
        return a + int(self.f() * (b - a + 1)) % (b - a + 1)


def _lcg(state: int):
    while True:
        state = (state * 48271) % 0x7FFFFFFF
        yield state


def _hash01(ix: int, iy: int, seed: int) -> float:
    """Deterministic lattice hash -> 0..1, independent of numpy version."""
    h = (ix * 374761393 + iy * 668265263 + seed * 2246822519) & 0xFFFFFFFF
    h = ((h ^ (h >> 13)) * 1274126177) & 0xFFFFFFFF
    return ((h ^ (h >> 16)) & 0xFFFFFF) / 0x1000000


def value_noise(size: int, cells: int, seed: int, wrap: bool = True) -> np.ndarray:
    """Seamless (periodic) value noise in 0..1."""
    step = size / cells
    lattice = np.empty((cells + 1, cells + 1), dtype=np.float32)
    for cy in range(cells + 1):
        for cx in range(cells + 1):
            wx = cx % cells if wrap else cx
            wy = cy % cells if wrap else cy
            lattice[cy, cx] = _hash01(wx, wy, seed)
    ys = np.arange(size, dtype=np.float32) / step
    xs = np.arange(size, dtype=np.float32) / step
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    v00 = lattice[np.ix_(y0, x0)]
    v01 = lattice[np.ix_(y0, x0 + 1)]
    v10 = lattice[np.ix_(y0 + 1, x0)]
    v11 = lattice[np.ix_(y0 + 1, x0 + 1)]
    top = v00 + (v01 - v00) * fx[None, :]
    bottom = v10 + (v11 - v10) * fx[None, :]
    return top + (bottom - top) * fy[:, None]


def fbm(size: int, seed: int, base_cells: int = 4, octaves: int = 5,
        persistence: float = 0.55, aniso: float = 1.0) -> np.ndarray:
    """Fractal brownian motion. aniso>1 stretches features along Y."""
    total = np.zeros((size, size), dtype=np.float32)
    amplitude = 1.0
    max_amp = 0.0
    cells = base_cells
    for octave in range(octaves):
        oc = cells
        oy = max(2, int(round(cells / max(aniso, 0.01))))
        noise = value_noise(size, max(2, oc), seed + octave * 101)
        if aniso != 1.0:
            noise = value_noise_aniso(size, max(2, oc), oy, seed + octave * 101)
        total += noise * amplitude
        max_amp += amplitude
        amplitude *= persistence
        cells *= 2
    return total / max_amp


def value_noise_aniso(size: int, cells_x: int, cells_y: int, seed: int) -> np.ndarray:
    step_x = size / cells_x
    step_y = size / cells_y
    lattice = np.empty((cells_y + 1, cells_x + 1), dtype=np.float32)
    for cy in range(cells_y + 1):
        for cx in range(cells_x + 1):
            lattice[cy, cx] = _hash01(cx % cells_x, cy % cells_y, seed)
    ys = np.arange(size, dtype=np.float32) / step_y
    xs = np.arange(size, dtype=np.float32) / step_x
    y0 = np.floor(ys).astype(int)
    x0 = np.floor(xs).astype(int)
    fy = ys - y0
    fx = xs - x0
    fy = fy * fy * (3 - 2 * fy)
    fx = fx * fx * (3 - 2 * fx)
    v00 = lattice[np.ix_(y0, x0)]
    v01 = lattice[np.ix_(y0, x0 + 1)]
    v10 = lattice[np.ix_(y0 + 1, x0)]
    v11 = lattice[np.ix_(y0 + 1, x0 + 1)]
    top = v00 + (v01 - v00) * fx[None, :]
    bottom = v10 + (v11 - v10) * fx[None, :]
    return top + (bottom - top) * fy[:, None]


def normal_from_height(height: np.ndarray, strength: float = 2.2):
    """Screen-space normals from a height field; returns (nx, ny) in -1..1."""
    dx = np.zeros_like(height)
    dy = np.zeros_like(height)
    dx[:, 1:-1] = height[:, 2:] - height[:, :-2]
    dx[:, 0] = dx[:, 1]
    dx[:, -1] = dx[:, -2]
    dy[1:-1, :] = height[2:, :] - height[:-2, :]
    dy[0, :] = dy[1, :]
    dy[-1, :] = dy[-2, :]
    return -dx * strength, -dy * strength


# -------------------------------------------------------------------- canvas
def rgb(hexstr: str) -> tuple:
    hexstr = hexstr.lstrip('#')
    return int(hexstr[0:2], 16), int(hexstr[2:4], 16), int(hexstr[4:6], 16)


class Canvas:
    def __init__(self, width: int = SIZE, height: int | None = None, seed: int = 1,
                 transparent: bool = False):
        if height is None:
            height = width
        self.w = width
        self.h = height
        self.size = width  # compatibility for square canvases
        self.seed = seed
        self.rng = Rng(seed)
        self.a = np.zeros((height, width, 4), dtype=np.float32)
        self.a[..., 3] = 0 if transparent else 255

    # ---- primitives
    def fill(self, color):
        r, g, b = rgb(color) if isinstance(color, str) else color
        self.a[..., 0], self.a[..., 1], self.a[..., 2] = r, g, b
        self.a[..., 3] = 255

    def rect(self, x0, y0, x1, y1, color):
        r, g, b = rgb(color) if isinstance(color, str) else color
        xi, yi, xj, yj = int(x0), int(y0), int(np.ceil(x1)), int(np.ceil(y1))
        region = (slice(max(0, yi), min(self.h, yj)), slice(max(0, xi), min(self.w, xj)))
        self.a[region[0], region[1], 0] = r
        self.a[region[0], region[1], 1] = g
        self.a[region[0], region[1], 2] = b
        self.a[region[0], region[1], 3] = 255

    def blit(self, overlay: np.ndarray, alpha: np.ndarray | float = 1.0,
             at: tuple = (0, 0)):
        """Composite an RGBA or RGB float layer, optionally at a sub-region."""
        if overlay.ndim == 2:
            overlay = np.dstack([overlay] * 3 + [np.full_like(overlay, 255)])
        if overlay.shape[2] == 3:
            overlay = np.dstack([overlay, np.full(overlay.shape[:2], 255, np.float32)])
        oy, ox = at
        oh, ow = overlay.shape[:2]
        ys, xs = slice(oy, min(self.h, oy + oh)), slice(ox, min(self.w, ox + ow))
        overlay = overlay[:ys.stop - ys.start, :xs.stop - xs.start]
        if np.isscalar(alpha):
            alpha = np.full(overlay.shape[:2], alpha, np.float32)
        else:
            alpha = alpha[:ys.stop - ys.start, :xs.stop - xs.start]
        src_a = overlay[..., 3:4] / 255.0 * alpha[..., None]
        dst_a = self.a[ys, xs, 3:4] / 255.0
        out_a = src_a + dst_a * (1 - src_a)
        safe = np.maximum(out_a, 1e-6)
        rgb_out = (overlay[..., :3] * src_a + self.a[ys, xs, :3] * dst_a * (1 - src_a)) / safe
        self.a[ys, xs, :3] = rgb_out
        self.a[ys, xs, 3:4] = out_a * 255

    # ---- grain / noise
    def grain(self, amount: float = 7.0, cells: int = 96, seed: int | None = None, wrap: bool = True):
        n = value_noise(self.size, cells, (seed if seed is not None else self.seed + 17), wrap=wrap)
        shift = (n - 0.5) * 2 * amount
        self.a[..., :3] = np.clip(self.a[..., :3] + shift[..., None], 0, 255)

    def stains(self, amount: float = 12.0, cells: int = 5, seed: int | None = None):
        n = fbm(self.size, (seed if seed is not None else self.seed + 31), base_cells=cells, octaves=4)
        shift = (n - 0.5) * 2 * amount
        self.a[..., :3] = np.clip(self.a[..., :3] + shift[..., None], 0, 255)

    # ---- lighting
    def shade(self, height: np.ndarray, strength: float = 2.0, ambient: float = 1.0):
        nx, ny = normal_from_height(height, strength)
        # light from the top-left; k tunes how much normals move brightness
        light = np.clip(1.0 + (nx * 0.45 + ny * 0.6) * 0.10, 0.62, 1.45)
        self.a[..., :3] = np.clip(self.a[..., :3] * (light * ambient)[..., None], 0, 255)

    def bevel(self, inset: int = 0, depth: int = 6, top: float = 26.0, bottom: float = -20.0,
              left: float = 18.0, right: float = -18.0):
        """Inner bevel lighting inside [inset, size-inset)."""
        s = self.h
        h = np.zeros((self.h, self.w), dtype=np.float32)
        d = depth
        ramp = np.linspace(1.0, 0.0, d, dtype=np.float32)
        for i in range(d):
            v = float(ramp[i])
            y0, y1 = inset + i, self.h - inset - 1 - i
            x0, x1 = inset + i, self.w - inset - 1 - i
            h[y0, x0:x1 + 1] += v * 1.0   # top edge raised
            h[y1, x0:x1 + 1] -= v * 1.0   # bottom edge lowered
            h[y0:y1 + 1, x0] += v * 0.8
            h[y0:y1 + 1, x1] -= v * 0.8
        # raised edges catch light from top-left; sunken ones fall into shade
        light = np.clip(1.0 + h * ((top - bottom) / 200.0), 0.6, 1.5)
        self.a[..., :3] = np.clip(self.a[..., :3] * light[..., None], 0, 255)

    def edge_line(self, thickness: int = 2, color=(14, 15, 18), alpha: float = 1.0):
        t = thickness
        self.a[:t, :, :3] = color
        self.a[-t:, :, :3] = color
        self.a[:, :t, :3] = color
        self.a[:, -t:, :3] = color
        self.a[:t, :, 3] = np.maximum(self.a[:t, :, 3], alpha * 255)
        self.a[-t:, :, 3] = np.maximum(self.a[-t:, :, 3], alpha * 255)
        self.a[:, :t, 3] = np.maximum(self.a[:, :t, 3], alpha * 255)
        self.a[:, -t:, 3] = np.maximum(self.a[:, -t:, 3], alpha * 255)

    def frame(self, width: int = 5, color=(24, 26, 30), bevel: bool = True):
        s = self.h
        w = self.w
        c = np.array(color, np.float32)
        self.a[:width, :, :3] = c
        self.a[s - width:, :, :3] = c
        self.a[:, :width, :3] = c
        self.a[:, w - width:, :3] = c
        self.a[:width, :, 3] = 255
        self.a[s - width:, :, 3] = 255
        self.a[:, :width, 3] = 255
        self.a[:, w - width:, 3] = 255
        if bevel:
            # bright top-left inner edge, dark bottom-right inner edge
            hi = np.clip(c + 52, 0, 255)
            lo = np.clip(c - 26, 0, 255)
            self.a[width, width:w - width, :3] = hi
            self.a[s - width - 1, width:w - width, :3] = lo
            self.a[width:s - width, width, :3] = hi * 0.9
            self.a[width:s - width, w - width - 1, :3] = lo

    # ---- features
    def rivet(self, cx: float, cy: float, radius: float, seed: int = 5):
        yy, xx = np.mgrid[0:self.h, 0:self.w].astype(np.float32)
        d2 = (xx - cx) ** 2 + (yy - cy) ** 2
        inside = d2 <= radius * radius
        ring = inside & (d2 > (radius - 1.6) ** 2)
        dome = np.clip(1.0 - d2 / (radius * radius * 0.85), 0, 1)
        dark = np.array([38, 41, 47], np.float32)
        mid = np.array([105, 111, 121], np.float32)
        hi = np.array([168, 175, 186], np.float32)
        lx, ly = cx - radius * 0.35, cy - radius * 0.35
        ldir = np.stack([xx - lx, yy - ly], -1)
        ldir /= (np.linalg.norm(ldir, axis=-1, keepdims=True) + 1e-6)
        shade = np.clip(0.55 + 0.6 * (-ldir[..., 0] * 0.5 - ldir[..., 1] * 0.86), 0.35, 1.35)
        col = dark + (mid - dark) * dome[..., None]
        col = col + (hi - mid) * (dome ** 3)[..., None]
        col *= shade[..., None]
        alpha = np.zeros((self.h, self.w), np.float32)
        alpha[inside] = 255
        layer = np.zeros((self.h, self.w, 4), np.float32)
        layer[..., :3] = np.clip(col, 0, 255)
        layer[..., 3] = alpha
        # dark seat ring
        layer[ring, :3] *= 0.55
        self.blit(layer)

    def brushed(self, base, streak_light: int = 14, streak_dark: int = 16, density: float = 0.5,
                seed: int = 3, vertical: bool = False):
        r, g, b = rgb(base) if isinstance(base, str) else base
        self.fill((r, g, b))
        s = self.size
        rng = Rng(seed)
        strokes = int(s * 2.6 * density)
        for _ in range(strokes):
            y = rng.range(0, s)
            length = rng.range(s * 0.15, s * 0.9)
            x = rng.range(-length * 0.2, s * 0.2)
            lum = rng.f()
            if lum < 0.45:
                delta = rng.range(4, streak_light)
            else:
                delta = -rng.range(4, streak_dark)
            alpha = rng.range(26, 96)
            yy0 = int(max(0, y))
            yy1 = int(min(s, y + 1))
            xx0 = int(max(0, x))
            xx1 = int(min(s, x + length))
            if vertical:
                yy0, yy1 = int(max(0, x)), int(min(s, x + length))
                xx0, xx1 = int(max(0, y)), int(min(s, y + 1))
            if yy1 <= yy0 or xx1 <= xx0:
                continue
            col = np.clip(np.array([r + delta, g + delta, b + delta], np.float32), 0, 255)
            self.a[yy0:yy1, xx0:xx1, :3] = self.a[yy0:yy1, xx0:xx1, :3] * (1 - alpha / 255) + col * (alpha / 255)
            self.a[yy0:yy1, xx0:xx1, 3] = 255

    def aniso_roughness(self, amount: float = 9.0, seed: int = 7, vertical: bool = False):
        """Fine directional micro-roughness over existing pixels."""
        n = fbm(self.size, seed, base_cells=6, octaves=4, aniso=8.0 if not vertical else 0.14)
        shift = (n - 0.5) * 2 * amount
        self.a[..., :3] = np.clip(self.a[..., :3] + shift[..., None], 0, 255)

    def vignette(self, strength: float = 0.16, inset: int = 0):
        s = self.size
        yy, xx = np.mgrid[0:self.h, 0:self.w].astype(np.float32)
        cx = (self.w - 1) / 2
        cy = (self.h - 1) / 2
        r = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / (s * 0.71)
        dark = 1.0 - strength * np.clip(r - 0.35, 0, 1) ** 1.6
        self.a[..., :3] = np.clip(self.a[..., :3] * dark[..., None], 0, 255)

    def glow(self, cx: float, cy: float, radius: float, color, peak: float = 0.85, falloff: float = 2.0):
        r, g, b = rgb(color) if isinstance(color, str) else color
        yy, xx = np.mgrid[0:self.h, 0:self.w].astype(np.float32)
        d = np.sqrt((xx - cx) ** 2 + (yy - cy) ** 2) / radius
        alpha = np.clip(1 - d, 0, 1) ** falloff * peak
        layer = np.zeros((self.h, self.w, 4), np.float32)
        layer[..., 0], layer[..., 1], layer[..., 2] = r, g, b
        layer[..., 3] = alpha * 255
        self.blit(layer)

    def contact_shadow(self, inset: int = 6, strength: float = 0.4, spread: int = 10):
        s = self.h
        inner = np.zeros((self.h, self.w), bool)
        inner[inset:s - inset, inset:self.w - inset] = True
        mask = inner.astype(np.float32)
        # blur by repeated box blur
        for _ in range(3):
            pad = np.pad(mask, 1)
            mask = (pad[:-2, :-2] + pad[:-2, 1:-1] + pad[:-2, 2:] +
                    pad[1:-1, :-2] + pad[1:-1, 1:-1] + pad[1:-1, 2:] +
                    pad[2:, :-2] + pad[2:, 1:-1] + pad[2:, 2:]) / 9.0
        edge = mask
        self.a[..., :3] = np.clip(self.a[..., :3] * (1 - strength * (1 - edge))[..., None], 0, 255)

    # ---- output
    def save(self, path):
        img = Image.fromarray(np.clip(self.a, 0, 255).astype(np.uint8), 'RGBA')
        img.save(path, 'PNG')
