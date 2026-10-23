#!/usr/bin/env python3
"""
Offline SDF font atlas baker for XEYS VISUALS.

Bakes signed-distance-field glyph atlases from TrueType fonts, so the game never runs font rasterisation
or distance-field code at runtime. Output matches dev.xeys.visuals.render.text.FontAtlas#read
(zlib-wrapped, big-endian DataOutputStream layout).

Method: each glyph is rasterised by FreeType at SUPERSAMPLE x the target em size, an exact Euclidean
distance transform (scipy) is computed on the high-resolution coverage mask, then box-averaged down to
the target resolution and quantised (128 = outline, 16 levels per atlas pixel). Kerning is measured with
HarfBuzz (GPOS + kern) for every pair within each script group.

Usage: python3 tools/bake_sdf_fonts.py <ttf_dir> <out_dir>
Requires: numpy, scipy, freetype-py, uharfbuzz
"""
import os
import struct
import sys
import zlib

import freetype
import numpy as np
import uharfbuzz as hb
from scipy import ndimage

CACHE_VERSION = 3
ON_EDGE = 128
PADDING = 8
DIST_SCALE = ON_EDGE / PADDING  # 16 levels per atlas pixel
MIP_LEVELS = 3
SUPERSAMPLE = 8
GUTTER = 2

TEXT_RANGES = [
    (0x0020, 0x007E), (0x00A0, 0x017F), (0x0370, 0x03FF), (0x0400, 0x045F),
    (0x2010, 0x2027), (0x2030, 0x203A), (0x20AC, 0x20AC), (0x2122, 0x2122),
    (0x2190, 0x2193), (0x2212, 0x2212), (0x25B2, 0x25BC), (0x2713, 0x2715), (0x2605, 0x2606),
]
ICON_RANGES = [(0xE000, 0xF8FF)]

FONTS = [
    # name, em px, ranges, kerning
    ("inter_regular", 48, TEXT_RANGES, True),
    ("inter_medium", 48, TEXT_RANGES, True),
    ("inter_semibold", 48, TEXT_RANGES, True),
    ("inter_bold", 48, TEXT_RANGES, True),
    ("inter_display_black", 64, TEXT_RANGES, True),
    ("icons", 48, ICON_RANGES, False),
]


def codepoints(face, ranges):
    out = []
    for a, b in ranges:
        for cp in range(a, b + 1):
            if face.get_char_index(cp) != 0:
                out.append(cp)
    return out


def glyph_sdf(face, cp, em_px):
    """Returns (advance_em, xoff_px, yoff_px, sdf uint8 array or None) at target resolution."""
    face.set_pixel_sizes(0, em_px * SUPERSAMPLE)
    face.load_char(cp, freetype.FT_LOAD_DEFAULT | freetype.FT_LOAD_NO_HINTING | freetype.FT_LOAD_RENDER)
    slot = face.glyph
    advance_em = slot.linearHoriAdvance / 65536.0 / (em_px * SUPERSAMPLE)
    bm = slot.bitmap
    if bm.width == 0 or bm.rows == 0:
        return advance_em, 0, 0, None

    cov = np.array(bm.buffer, dtype=np.uint8).reshape(bm.rows, bm.pitch)[:, :bm.width]
    pad_hi = PADDING * SUPERSAMPLE
    # Align the padded high-res box to whole target pixels.
    left_hi = slot.bitmap_left - pad_hi
    top_hi = slot.bitmap_top + pad_hi
    shift_x = left_hi % SUPERSAMPLE
    shift_y = (-top_hi) % SUPERSAMPLE
    w_hi = cov.shape[1] + 2 * pad_hi + shift_x
    h_hi = cov.shape[0] + 2 * pad_hi + shift_y
    w_hi += (-w_hi) % SUPERSAMPLE
    h_hi += (-h_hi) % SUPERSAMPLE
    canvas = np.zeros((h_hi, w_hi), dtype=np.uint8)
    canvas[pad_hi + shift_y:pad_hi + shift_y + cov.shape[0], pad_hi + shift_x:pad_hi + shift_x + cov.shape[1]] = cov

    inside = canvas >= 128
    if not inside.any():
        return advance_em, 0, 0, None
    d_out = ndimage.distance_transform_edt(~inside)
    d_in = ndimage.distance_transform_edt(inside)
    signed = np.where(inside, d_in - 0.5, -(d_out - 0.5))  # high-res pixels, + inside

    h_lo = h_hi // SUPERSAMPLE
    w_lo = w_hi // SUPERSAMPLE
    lo = signed.reshape(h_lo, SUPERSAMPLE, w_lo, SUPERSAMPLE).mean(axis=(1, 3)) / SUPERSAMPLE
    q = np.clip(np.round(ON_EDGE + lo * DIST_SCALE), 0, 255).astype(np.uint8)

    xoff = (left_hi - shift_x) // SUPERSAMPLE
    yoff = -((top_hi + shift_y) // SUPERSAMPLE)  # y-down offset of the bitmap top from the baseline
    return advance_em, xoff, yoff, q


def kerning_pairs(ttf_path, cps, em_units_scale):
    blob = hb.Blob.from_file_path(ttf_path)
    face = hb.Face(blob)
    font = hb.Font(face)
    upem = face.upem
    font.scale = (upem, upem)
    nominal = {}
    for cp in cps:
        buf = hb.Buffer()
        buf.add_codepoints([cp])
        buf.guess_segment_properties()
        hb.shape(font, buf, {"kern": True, "liga": False, "calt": False})
        nominal[cp] = buf.glyph_positions[0].x_advance if buf.glyph_positions else 0

    def group(cp):
        if 0x0370 <= cp <= 0x03FF:
            return "greek"
        if 0x0400 <= cp <= 0x045F:
            return "cyrillic"
        return "latin"

    pairs = {}
    common = [c for c in cps if c < 0x80 and not chr(c).isalpha()]
    groups = {}
    for cp in cps:
        groups.setdefault(group(cp), []).append(cp)
    for name, members in groups.items():
        members = sorted(set(members + common))
        for a in members:
            for b in members:
                buf = hb.Buffer()
                buf.add_codepoints([a, b])
                buf.guess_segment_properties()
                hb.shape(font, buf, {"kern": True, "liga": False, "calt": False})
                if len(buf.glyph_positions) != 2:
                    continue
                k = buf.glyph_positions[0].x_advance - nominal[a]
                if k != 0:
                    pairs[(a, b)] = k / upem
    return pairs


def pack(glyphs):
    """Shelf-packs glyph bitmaps; returns (width, height, placements)."""
    items = sorted((g for g in glyphs if g["sdf"] is not None), key=lambda g: -g["sdf"].shape[0])
    area = sum((g["sdf"].shape[1] + GUTTER) * (g["sdf"].shape[0] + GUTTER) for g in items)
    width = 1024 if area < 1024 * 1024 * 0.7 else 2048
    x = y = GUTTER
    shelf = 0
    for g in items:
        h, w = g["sdf"].shape
        if x + w + GUTTER > width:
            x = GUTTER
            y += shelf + GUTTER
            shelf = 0
        g["ax"], g["ay"] = x, y
        x += w + GUTTER
        shelf = max(shelf, h)
    needed = y + shelf + GUTTER
    height = 64
    while height < needed:
        height *= 2
    return width, height


def mip_chain(level0):
    levels = [level0]
    for _ in range(1, MIP_LEVELS):
        src = levels[-1].astype(np.uint16)
        h, w = src.shape
        src = src[:h - h % 2, :w - w % 2]
        down = (src[0::2, 0::2] + src[1::2, 0::2] + src[0::2, 1::2] + src[1::2, 1::2] + 2) // 4
        levels.append(down.astype(np.uint8))
    return levels


def bake(ttf_path, name, em_px, ranges, with_kerning, out_dir):
    face = freetype.Face(ttf_path)
    cps = codepoints(face, ranges)
    glyphs = []
    for cp in cps:
        adv, xoff, yoff, sdf = glyph_sdf(face, cp, em_px)
        glyphs.append({"cp": cp, "adv": adv, "xoff": xoff, "yoff": yoff, "sdf": sdf, "ax": 0, "ay": 0})
    width, height = pack(glyphs)
    atlas = np.zeros((height, width), dtype=np.uint8)
    for g in glyphs:
        if g["sdf"] is not None:
            h, w = g["sdf"].shape
            atlas[g["ay"]:g["ay"] + h, g["ax"]:g["ax"] + w] = g["sdf"]

    # Font-wide metrics in em.
    face.set_char_size(face.units_per_EM * 64)
    upem = float(face.units_per_EM)
    asc = face.ascender / upem
    desc = face.descender / upem
    gap = max(face.height - (face.ascender - face.descender), 0) / upem

    from fontTools.ttLib import TTFont
    os2 = TTFont(ttf_path)["OS/2"]
    cap = (getattr(os2, "sCapHeight", 0) or 0.7 * face.ascender) / upem
    xh = (getattr(os2, "sxHeight", 0) or 0.75 * cap * upem) / upem
    kern = kerning_pairs(ttf_path, cps, upem) if with_kerning else {}

    out = bytearray()
    out += struct.pack(">i", CACHE_VERSION)
    out += struct.pack(">ffffff", float(em_px), asc, desc, gap, cap, xh)
    out += struct.pack(">ii", width, height)
    out += struct.pack(">i", len(glyphs))
    for g in glyphs:
        if g["sdf"] is not None:
            h, w = g["sdf"].shape
        else:
            h = w = 0
        out += struct.pack(">ifffffiiii", g["cp"], g["adv"], g["xoff"] / em_px, g["yoff"] / em_px, w / em_px, h / em_px,
                           g["ax"], g["ay"], w, h)
    out += struct.pack(">i", len(kern))
    for (a, b), k in sorted(kern.items()):
        out += struct.pack(">qf", (a << 32) | b, k)
    levels = mip_chain(atlas)
    out += struct.pack(">i", len(levels))
    for lv in levels:
        data = lv.tobytes()
        out += struct.pack(">i", len(data))
        out += data
    path = os.path.join(out_dir, name + ".sdf")
    with open(path, "wb") as fh:
        fh.write(zlib.compress(bytes(out), 9))
    print(f"{name}: {len(glyphs)} glyphs, {len(kern)} kern pairs, atlas {width}x{height}, {os.path.getsize(path)} bytes")


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    ttf_dir, out_dir = sys.argv[1], sys.argv[2]
    os.makedirs(out_dir, exist_ok=True)
    for name, em, ranges, kern in FONTS:
        bake(os.path.join(ttf_dir, name + ".ttf"), name, em, ranges, kern, out_dir)


if __name__ == "__main__":
    main()
