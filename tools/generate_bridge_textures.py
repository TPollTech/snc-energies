"""128x128 textures for the optional Adventures bridges: beverage motor and electric UV lamp."""
import struct, zlib, math
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'src/main/resources/assets/snc_energies/textures/block'


def png(name, pixels):
    h = len(pixels); w = len(pixels[0])
    raw = b''
    for row in pixels:
        raw += b'\x00'
        for argb in row:
            raw += bytes(((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, (argb >> 24) & 255))
    def chunk(tag, data):
        c = struct.pack('>I', len(data)) + tag + data
        return c + struct.pack('>I', zlib.crc32(tag + data) & 0xFFFFFFFF)
    body = b'\x89PNG\r\n\x1a\n'
    body += chunk(b'IHDR', struct.pack('>IIBBBBB', w, h, 8, 6, 0, 0, 0))
    body += chunk(b'IDAT', zlib.compress(raw, 9))
    body += chunk(b'IEND', b'')
    (OUT / f'{name}.png').write_bytes(body)


def canvas(base, noise=8, seed=13):
    r, g, b = base
    import random
    rng = random.Random(seed)
    px = [[0] * 128 for _ in range(128)]
    for y in range(128):
        for x in range(128):
            d = rng.randint(-noise, noise)
            px[y][x] = (min(255, max(0, r + d)) << 16) | (min(255, max(0, g + d)) << 8) | min(255, max(0, b + d)) | 0xFF000000
    return px


def rect(px, x0, y0, x1, y1, color, outline=None):
    r, g, b = color
    argb = (r << 16) | (g << 8) | b | 0xFF000000
    for y in range(y0, y1):
        for x in range(x0, x1):
            if 0 <= x < 128 and 0 <= y < 128: px[y][x] = argb
    if outline:
        r, g, b = outline
        argb = (r << 16) | (g << 8) | b | 0xFF000000
        for x in range(x0, x1):
            if 0 <= x < 128:
                if y0 >= 0 and y0 < 128: px[y0][x] = argb
                if y1 - 1 >= 0 and y1 - 1 < 128: px[y1 - 1][x] = argb
        for y in range(y0, y1):
            if 0 <= y < 128:
                if x0 >= 0 and x0 < 128: px[y][x0] = argb
                if x1 - 1 >= 0 and x1 - 1 < 128: px[y][x1 - 1] = argb


# Machine side/top/bottom already exist for the other machines; the motor shares
# machine_side / machine_top / machine_bottom and adds only its front face.

# ---- beverage motor front: docking socket with intake arrow and gauge ----
px = canvas((58, 66, 72), 9, seed=21)
rect(px, 14, 22, 114, 106, (34, 44, 50), (140, 152, 158))       # socket housing
rect(px, 30, 38, 98, 90, (24, 32, 38), (110, 122, 126))          # opening
rect(px, 40, 48, 88, 80, (201, 138, 46), (240, 200, 120))        # amber dock mouth
for y in range(52, 78, 6): rect(px, 46, y, 82, y + 3, (240, 210, 140))  # feed fins
rect(px, 56, 26, 72, 34, (216, 230, 208), (90, 100, 94))          # status lamp
rect(px, 18, 6, 110, 16, (44, 54, 60), (140, 152, 158))           # top plaque
for x in range(22, 106, 12): rect(px, x, 9, x + 6, 13, (170, 182, 188))
png('beverage_motor_front', px)

# ---- electric UV lamp: glass cylinder, lit and unlit ----
def uv(lit):
    px = canvas((168, 190, 200), 6, seed=31)
    rect(px, 24, 8, 104, 120, (150, 172, 186), (110, 128, 140))   # glass body
    rect(px, 32, 16, 96, 112, (222, 236, 244) if lit else (188, 208, 220))
    # inner tube
    rect(px, 48, 12, 80, 116, (168, 245, 214) if lit else (150, 176, 184), (120, 150, 150))
    for y in range(20, 110, 12):
        rect(px, 52, y, 76, y + 5, (236, 255, 244) if lit else (160, 186, 192))
    if lit:
        for y in range(16, 112, 7):                               # halo bands
            rect(px, 36, y, 92, y + 2, (196, 250, 226))
    rect(px, 40, 0, 88, 12, (130, 138, 144), (90, 96, 102))       # metal cap
    rect(px, 40, 116, 88, 128, (130, 138, 144), (90, 96, 102))    # base
    for x in range(46, 84, 12): rect(px, x, 119, x + 7, 125, (96, 104, 110))
    return px
png('electric_uv_lamp', uv(True))
png('electric_uv_lamp_off', uv(False))

print('Adventures bridge textures generated (beverage_motor_front, electric_uv_lamp[off]).')
