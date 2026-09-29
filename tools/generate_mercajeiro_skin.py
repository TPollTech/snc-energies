"""Draws the Mercajeiro shopkeeper skin: 64x64 player-style layout with a
mustachioed face, straw hat band, apron over the shirt. Only the head and
body/front areas carry custom art; the rest is flat colors."""
import os
import random
import struct
import zlib

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "snc_energies",
                   "textures", "entity", "mercajeiro.png")

W = H = 64
SKIN = 0xFF8D5542      # warm skin tone
HAIR = 0xFF3A2A1C      # dark hair
MUSTACHE = 0xFF2A1E12
SHIRT = 0xFFC8B98A     # straw-colored market shirt
VEST = 0xFF5A4632      # brown vest
APRON = 0xFF8A4A3A     # apron red-brown
PANTS = 0xFF4A3E5A     # dark trousers
SHOES = 0xFF2A241E
HAT = 0xFFD8C070       # straw hat
EYE = 0xFF1A1410
MOUTH = 0xFF6A3A2A


def png(path, pixels):
    h = len(pixels)
    w = len(pixels[0])
    raw = b""
    for row in pixels:
        raw += b"\x00"
        for argb in row:
            raw += bytes(((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255))

    def chunk(tag, data):
        c = struct.pack(">I", len(data)) + tag + data
        return c + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)
    body = b"\x89PNG\r\n\x1a\n"
    body += chunk(b"IHDR", struct.pack(">IIBBBBB", w, h, 8, 6, 0, 0, 0))
    body += chunk(b"IDAT", zlib.compress(raw, 9))
    body += chunk(b"IEND", b"")
    with open(path, "wb") as f:
        f.write(body)


def fill(px, x0, y0, x1, y1, color):
    for y in range(max(0, y0), min(H, y1)):
        for x in range(max(0, x0), min(W, x1)):
            px[y][x] = color


def main():
    px = [[0] * W for _ in range(H)]

    # ---- Head: base/front at (8,8)..(16,16) region (8x8 face at 9,9..15,15)
    fill(px, 8, 8, 24, 16, SKIN)          # head base + face box
    fill(px, 9, 12, 11, 14, EYE)          # eyes (2 px each)
    fill(px, 13, 12, 15, 14, EYE)
    fill(px, 11, 14, 13, 15, MUSTACHE)    # big mustache over the mouth
    fill(px, 12, 15, 12, 16, MOUTH)
    fill(px, 8, 8, 24, 9, HAIR)           # hairline top
    # Hat: straw band above the hairline (drawn in hat layer area 40,8..56,16)
    fill(px, 40, 8, 56, 16, HAT)
    fill(px, 40, 8, 56, 10, 0x00000000)   # hat top layer transparent in base
    fill(px, 40, 10, 56, 16, HAT)
    fill(px, 40, 15, 56, 16, 0xFFB89A50)  # hat brim shading

    # ---- Body front at (20,20)..(28,32)
    fill(px, 20, 20, 28, 32, SHIRT)
    fill(px, 20, 20, 22, 32, VEST)        # vest sides
    fill(px, 26, 20, 28, 32, VEST)
    fill(px, 22, 24, 26, 32, APRON)       # apron over the shirt
    fill(px, 22, 23, 26, 24, 0xFFD8C070)  # apron strap
    # ---- Arms, legs, shoes (flat colors)
    fill(px, 40, 20, 48, 32, SHIRT)       # right arm
    fill(px, 44, 26, 48, 32, SKIN)        # hand
    fill(px, 32, 48, 40, 56, PANTS)       # right leg
    fill(px, 36, 52, 40, 56, SHOES)
    fill(px, 0, 48, 16, 56, PANTS)        # left leg (base layer)
    fill(px, 4, 52, 8, 56, SHOES)
    fill(px, 0, 20, 16, 32, SHIRT)        # left arm base

    # ---- Overlay/second layers stay transparent (hat drawn on base for simplicity)
    png(OUT, px)
    print("wrote", os.path.relpath(OUT, ROOT))


if __name__ == "__main__":
    main()
