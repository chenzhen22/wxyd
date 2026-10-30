#!/usr/bin/env python3
"""Generate wxyd app launcher icons (adaptive + legacy) as PNGs.

Design: rounded-square tile with indigo→violet gradient; centered abstract
"water drop + signal wave" mark (泽 = marsh/water grace; 小泽助手 = assistant):
a large white water-drop with three concentric arc waves radiating from its
top-right, suggesting a smart assistant listening/responding.

Outputs:
  mipmap-*/ic_launcher.png            legacy square (rounded corners)
  mipmap-*/ic_launcher_round.png      legacy round
  mipmap-*/ic_launcher_background.png adaptive background (gradient)
  mipmap-*/ic_launcher_foreground.png adaptive foreground (mark on transparent)
"""
from PIL import Image, ImageDraw
import math
import os

OUT = "app/src/main/res"
SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

GRAD_TOP = (63, 81, 255)
GRAD_BOTTOM = (149, 84, 248)


def gradient(size):
    img = Image.new("RGBA", (size, size))
    px = img.load()
    for y in range(size):
        t = y / (size - 1)
        c = tuple(int(a + (b - a) * t) for a, b in zip(GRAD_TOP, GRAD_BOTTOM)) + (255,)
        for x in range(size):
            px[x, y] = c
    return img


def tile(size, radius_ratio=0.0):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    if radius_ratio <= 0:
        img.alpha_composite(gradient(size))
        return img
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        [0, 0, size - 1, size - 1], radius=int(size * radius_ratio), fill=255)
    img.paste(gradient(size), (0, 0), mask)
    return img


def water_drop_mask(s, cx, cy, rx, ry, tip_dy):
    """Classic drop: circle body + two tangent curves up to a tip. Returns L mask."""
    mask = Image.new("L", (s, s), 0)
    d = ImageDraw.Draw(mask)
    d.ellipse([cx - rx, cy - ry, cx + rx, cy + ry], fill=255)
    # tip above the circle: tangent lines from tip point to circle
    tip = (cx, cy - ry - tip_dy)
    # tangent length & angle
    L = math.hypot(rx, tip_dy)
    base_ang = math.atan2(tip_dy, rx)  # angle of tangent at tip relative to x-axis
    # tangent points on circle
    for sign in (-1, 1):
        ang = math.atan2(-tip_dy, sign * rx) + sign * math.asin(rx / L) * 0  # placeholder
    # simpler robust approach: sample angles, find tangent via perpendicularity
    pts_left, pts_right = None, None
    best = {}
    for i in range(3600):
        a = i / 3600 * 2 * math.pi
        px = cx + rx * math.cos(a)
        py = cy + ry * math.sin(a)
        vx, vy = tip[0] - px, tip[1] - py
        # radius direction
        nx, ny = -ry * math.cos(a), rx * math.sin(a)
        dot = vx * nx + vy * ny
        side = 0 if dot > 0 else 1
        # distance from tip to tangent line through p with normal (nx,ny)
        dist = abs((tip[0] - px) * nx + (tip[1] - py) * ny) / math.hypot(nx, ny)
        key = ("L",) if px < cx else ("R",)
        if key not in best or dist < best[key][0]:
            best[key] = (dist, (px, py))
    d.polygon([tip, best["L"][1], best["R"][1]], fill=255)
    return mask


def draw_mark(s):
    """Water drop (white) + three signal arcs radiating from top-right of drop."""
    img = Image.new("RGBA", (s, s), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    white = (255, 255, 255, 255)

    # drop geometry: body circle center-left, tip up-left-ish tilted
    cx, cy = 0.44 * s, 0.60 * s
    r = 0.17 * s
    tip = (cx + 0.02 * s, cy - 0.40 * s)  # tip nearly straight up
    mask = Image.new("L", (s, s), 0)
    dm = ImageDraw.Draw(mask)
    dm.ellipse([cx - r, cy - r, cx + r, cy + r], fill=255)
    # tangent triangle from tip
    best = {}
    for i in range(3600):
        a = i / 3600 * 2 * math.pi
        px, py = cx + r * math.cos(a), cy + r * math.sin(a)
        nx, ny = -math.sin(a), math.cos(a)
        dist = abs((tip[0] - px) * nx + (tip[1] - py) * ny)
        key = "L" if px < cx + r * 0.2 else "R"
        if key not in best or dist < best[key][0]:
            best[key] = (dist, (px, py))
    dm.polygon([tip, best["L"][1], best["R"][1]], fill=255)
    solid = Image.new("RGBA", (s, s), white)
    img.paste(solid, (0, 0), mask)

    # small inner highlight circle (cut-out, shows gradient through) — subtle
    cut = Image.new("L", (s, s), 0)
    ImageDraw.Draw(cut).ellipse(
        [cx - r * 0.45, cy - r * 0.55, cx - r * 0.05, cy - r * 0.15], fill=110)
    alpha = img.split()[3]
    from PIL import ImageChops
    img.putalpha(ImageChops.subtract(alpha, cut))

    # three arcs radiating from drop tip area (signal waves), centered near tip
    wx, wy = tip[0], tip[1] + 0.02 * s
    for i, (radius, width, alpha_v) in enumerate([
        (0.155 * s, 0.028 * s, 255),
        (0.255 * s, 0.030 * s, 200),
        (0.355 * s, 0.032 * s, 140),
    ]):
        # arc spanning from -80° to -10° (up-right quadrant)
        d.arc(
            [wx - radius, wy - radius, wx + radius, wy + radius],
            start=-80, end=-8, fill=(255, 255, 255, alpha_v), width=int(width))
    return img


def compose_legacy(size, round_icon=False):
    img = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    img.alpha_composite(tile(size, 0 if round_icon else 0.18))
    img.alpha_composite(draw_mark(size))
    return img


def main():
    for dpi, size in SIZES.items():
        compose_legacy(size).save(f"{OUT}/{dpi}/ic_launcher.png")
        compose_legacy(size, round_icon=True).save(f"{OUT}/{dpi}/ic_launcher_round.png")
        print(f"{dpi}: {size}px")

    # adaptive: background gradient full-bleed; mark inside safe zone (~66/108)
    for dpi, size in SIZES.items():
        d = size * 2
        tile(d).save(f"{OUT}/{dpi}/ic_launcher_background.png")
        fg_d = d * 2
        fg = Image.new("RGBA", (fg_d, fg_d), (0, 0, 0, 0))
        mark = draw_mark(int(fg_d * 0.72))
        off = (fg_d - mark.size[0]) // 2
        fg.alpha_composite(mark, (off, off))
        fg = fg.resize((d, d), Image.LANCZOS)
        fg.save(f"{OUT}/{dpi}/ic_launcher_foreground.png")
    print("adaptive layers done")


if __name__ == "__main__":
    main()
