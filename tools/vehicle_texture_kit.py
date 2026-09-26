"""Shared 128x128 texture-painting kit for SNC Energies vehicle assets.

Extracted verbatim from tools/generate_tractor_textures.py (SNC 75 golden
reference): seamless noise surfaces, a supersampled Painter for crisp
vector detail, and screw hardware. All vehicle texture generators use
these helpers so every vehicle keeps the same material quality.
"""
from __future__ import annotations

import math

import numpy as np
from PIL import Image, ImageDraw, ImageFont

from textures_engine import value_noise

SIZE = 128
SCALE = 4


def surface(color: str, seed: int, grain: float = 2.0, brushed: float = 0) -> Image.Image:
    """Subtle seamless color variation without artificial panel borders."""
    fine = value_noise(SIZE, 64, seed) - 0.5
    broad = value_noise(SIZE, 8, seed + 1) - 0.5
    variation = fine * grain + broad * grain * 0.6
    if brushed:
        rows = (value_noise(SIZE, 64, seed + 2)[:, 0] - 0.5)[:, None]
        variation = variation + rows * brushed
    pixels = np.clip(rgb(color)[None, None, :] + variation[:, :, None], 0, 255)
    return Image.fromarray(pixels.astype(np.uint8), "RGB")


def rgb(color: str) -> np.ndarray:
    return np.array(tuple(bytes.fromhex(color.lstrip("#"))), dtype=float)


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont | ImageFont.ImageFont:
    names = (
        ["C:/Windows/Fonts/seguisb.ttf", "C:/Windows/Fonts/arialbd.ttf", "DejaVuSans-Bold.ttf"]
        if bold else ["C:/Windows/Fonts/segoeui.ttf", "C:/Windows/Fonts/arial.ttf", "DejaVuSans.ttf"]
    )
    for name in names:
        try:
            return ImageFont.truetype(name, round(size * SCALE))
        except OSError:
            pass
    return ImageFont.load_default(size=round(size * SCALE))


class Painter:
    """Draw crisp, antialiased detail before the final 128 px downsample."""

    def __init__(self, base: Image.Image):
        self.image = base.resize((SIZE * SCALE, SIZE * SCALE), Image.Resampling.NEAREST)
        self.draw = ImageDraw.Draw(self.image)

    @staticmethod
    def box(coords):
        return tuple(round(n * SCALE) for n in coords)

    def rect(self, coords, fill, outline=None, width=1, radius=0):
        if radius:
            self.draw.rounded_rectangle(self.box(coords), radius=round(radius * SCALE),
                                        fill=fill, outline=outline, width=round(width * SCALE))
        else:
            self.draw.rectangle(self.box(coords), fill=fill, outline=outline,
                                width=round(width * SCALE))

    def line(self, coords, fill, width=1):
        self.draw.line(self.box(coords), fill=fill, width=max(1, round(width * SCALE)))

    def ellipse(self, coords, fill, outline=None, width=1):
        self.draw.ellipse(self.box(coords), fill=fill, outline=outline,
                          width=max(1, round(width * SCALE)))

    def arc(self, coords, start, end, fill, width=1):
        self.draw.arc(self.box(coords), start, end, fill=fill, width=max(1, round(width * SCALE)))

    def text(self, center, text, size, fill, bold=False):
        self.draw.text(self.box(center), text, font=font(size, bold), fill=fill, anchor="mm")

    def finish(self):
        return self.image.resize((SIZE, SIZE), Image.Resampling.LANCZOS)


def screw(p: Painter, x: float, y: float, radius: float = 2.2):
    p.ellipse((x - radius, y - radius, x + radius, y + radius), "#122026")
    p.ellipse((x - radius + 0.4, y - radius + 0.3, x + radius - 0.3, y + radius - 0.4), "#7b888a")
    p.line((x - 1, y + 0.7, x + 1, y - 0.7), "#344247", 0.65)
