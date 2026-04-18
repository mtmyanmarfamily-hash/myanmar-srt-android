#!/usr/bin/env python3
"""Generate simple launcher icons for the Android app."""
import struct, zlib, os

def make_png(size, bg=(124,92,252), fg=(255,255,255)):
    """Create a minimal PNG with gradient background and camera emoji approximation."""
    def chunk(name, data):
        c = zlib.crc32(name + data) & 0xffffffff
        return struct.pack('>I', len(data)) + name + data + struct.pack('>I', c)

    pixels = []
    for y in range(size):
        row = [0]  # filter byte
        for x in range(size):
            # Rounded rect mask
            margin = size // 8
            r = size // 4
            cx, cy = size//2, size//2

            # Background gradient
            t = (x + y) / (2 * size)
            pr = int(bg[0] + t * (232 - bg[0]))  # accent to accent2
            pg = int(bg[1] + t * (67 - bg[1]))
            pb = int(bg[2] + t * (147 - bg[2]))

            # Simple film icon in center
            icon_margin = size // 4
            in_rect = (icon_margin < x < size-icon_margin and icon_margin < y < size-icon_margin)
            is_icon = in_rect

            # Checkerboard border on icon
            if is_icon:
                edge = 2
                on_edge = (x < icon_margin+edge or x > size-icon_margin-edge or
                           y < icon_margin+edge or y > size-icon_margin-edge)
                if on_edge:
                    stripe = ((x // (size//16)) + (y // (size//16))) % 2
                    row += [255*stripe, 255*stripe, 255*stripe, 255]
                else:
                    # Center: camera icon approximation
                    mid_x, mid_y = size//2, size//2
                    dist = ((x-mid_x)**2 + (y-mid_y)**2)**0.5
                    lens_r = size//6
                    if dist < lens_r:
                        inner = dist < lens_r * 0.6
                        row += [fg[0] if inner else 200, fg[1] if inner else 200, fg[2] if inner else 200, 255]
                    else:
                        row += [pr, pg, pb, 255]
            else:
                row += [pr, pg, pb, 255]

        pixels.append(bytes(row))

    raw = b''.join(pixels)
    compressed = zlib.compress(raw, 9)

    png = b'\x89PNG\r\n\x1a\n'
    png += chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 2, 0, 0, 0))
    png += chunk(b'IDAT', compressed)
    png += chunk(b'IEND', b'')
    return png

base = os.path.dirname(os.path.abspath(__file__))
sizes = {
    'mipmap-mdpi': 48, 'mipmap-hdpi': 72,
    'mipmap-xhdpi': 96, 'mipmap-xxhdpi': 144, 'mipmap-xxxhdpi': 192
}

for folder, size in sizes.items():
    path = os.path.join(base, 'app/src/main/res', folder)
    os.makedirs(path, exist_ok=True)
    png = make_png(size)
    with open(os.path.join(path, 'ic_launcher.png'), 'wb') as f:
        f.write(png)
    with open(os.path.join(path, 'ic_launcher_round.png'), 'wb') as f:
        f.write(png)

print("Icons generated!")
