#!/usr/bin/env python3
"""
Reproducible asset preparation for XEYS VISUALS.

Inputs (download separately, not committed):
  moon/lroc_color_poles_4k.tif   NASA SVS CGI Moon Kit (LRO LROC WAC color mosaic)
  moon/ldem_16_uint.tif          NASA SVS CGI Moon Kit (LRO LOLA elevation, 16 px/deg)
  inter/package/web/*.woff2      Inter 4.1 (npm: inter-ui), SIL OFL 1.1
  lucide/package/font/lucide.ttf Lucide icons (npm: lucide-static), ISC

Usage:
  python3 tools/prepare_assets.py <download_dir> <resources_assets_dir>

Requires: Pillow, numpy, scipy, fontTools[woff] (brotli), freetype-py, uharfbuzz.
Font subsets are written to tools/fonts/ and baked into SDF atlases (see bake_sdf_fonts.py).
"""
import json
import math
import os
import sys

import numpy as np
from PIL import Image

Image.MAX_IMAGE_PIXELS = None

MOON_RADIUS_KM = 1737.4
# Relief exaggeration baked into the normal map. The shader can amplify further.
RELIEF = 2.5


def build_moon(src, out_dir):
    os.makedirs(out_dir, exist_ok=True)

    # --- Albedo -----------------------------------------------------------------
    albedo = Image.open(os.path.join(src, "moon", "lroc_color_poles_4k.tif")).convert("RGB")
    assert albedo.size == (4096, 2048), albedo.size
    albedo.save(os.path.join(out_dir, "moon_albedo.jpg"), quality=92, subsampling=2, optimize=True)

    # --- Normal map from LOLA elevation --------------------------------------
    dem = np.asarray(Image.open(os.path.join(src, "moon", "ldem_16_uint.tif")), dtype=np.float64)
    h_km = (dem - 20000.0) * 0.5 / 1000.0  # half-meters with +20000 offset -> km
    rows, cols = h_km.shape  # 2880 x 5760, row 0 = +90 lat, col 0 = -180 lon

    px_deg = cols / 360.0
    dy_km = MOON_RADIUS_KM * math.radians(1.0 / px_deg)
    lat = 90.0 - (np.arange(rows) + 0.5) / px_deg
    coslat = np.maximum(np.cos(np.radians(lat)), 0.02)[:, None]
    dx_km = dy_km * coslat

    east = np.roll(h_km, -1, axis=1)
    west = np.roll(h_km, 1, axis=1)
    north = np.vstack([h_km[:1], h_km[:-1]])
    south = np.vstack([h_km[1:], h_km[-1:]])
    dhdx = (east - west) / (2.0 * dx_km)
    dhdy = (north - south) / (2.0 * dy_km)

    nx = -dhdx * RELIEF
    ny = -dhdy * RELIEF
    nz = np.ones_like(nx)
    inv = 1.0 / np.sqrt(nx * nx + ny * ny + nz * nz)
    n = np.stack([nx * inv, ny * inv, nz * inv], axis=-1)

    rgb = np.clip((n * 0.5 + 0.5) * 255.0 + 0.5, 0, 255).astype(np.uint8)
    normal = Image.fromarray(rgb, "RGB").resize((2048, 1024), Image.LANCZOS)
    # 4:4:4 chroma: each channel is an independent vector component.
    normal.save(os.path.join(out_dir, "moon_normal.jpg"), quality=93, subsampling=0, optimize=True)
    print("moon textures ->", out_dir)


# Glyph coverage baked into the SDF atlases (see FontAtlas.java).
TEXT_RANGES = [
    (0x0020, 0x007E), (0x00A0, 0x017F), (0x0370, 0x03FF), (0x0400, 0x045F),
    (0x2010, 0x2027), (0x2030, 0x203A), (0x20AC, 0x20AC), (0x2122, 0x2122),
    (0x2190, 0x2193), (0x2212, 0x2212), (0x2022, 0x2022), (0x25B2, 0x25BC),
    (0x2713, 0x2715), (0x2605, 0x2606),
]


def subset_font(src_path, out_path, unicodes):
    from fontTools import subset
    from fontTools.ttLib import TTFont

    font = TTFont(src_path)
    options = subset.Options()
    options.layout_features = ["kern", "liga", "calt", "tnum", "case"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    options.glyph_names = False
    options.hinting = False
    options.flavor = None
    sub = subset.Subsetter(options)
    sub.populate(unicodes=unicodes)
    sub.subset(font)
    # Inputs may be WOFF2; stb_truetype needs a plain sfnt container.
    font.flavor = None
    font.save(out_path)
    print("font", os.path.basename(out_path), os.path.getsize(out_path), "bytes")


def build_fonts(src, out_dir):
    os.makedirs(out_dir, exist_ok=True)
    unicodes = []
    for a, b in TEXT_RANGES:
        unicodes.extend(range(a, b + 1))
    web = os.path.join(src, "inter", "package", "web")
    disp = os.path.join(src, "inter", "package", "display")
    for weight in ["Regular", "Medium", "SemiBold", "Bold"]:
        subset_font(os.path.join(web, f"Inter-{weight}.woff2"), os.path.join(out_dir, f"inter_{weight.lower()}.ttf"), unicodes)
    subset_font(os.path.join(disp, "InterDisplay-Black.woff2"), os.path.join(out_dir, "inter_display_black.ttf"), unicodes)

    with open(os.path.join(src, "lucide", "package", "font", "codepoints.json")) as fh:
        codepoints = json.load(fh)
    with open(os.path.join(os.path.dirname(os.path.abspath(__file__)), "icons.txt")) as fh:
        wanted = [line.strip() for line in fh if line.strip() and not line.startswith("#")]
    missing = [w for w in wanted if w not in codepoints]
    if missing:
        raise SystemExit("missing lucide icons: " + ", ".join(missing))
    subset_font(os.path.join(src, "lucide", "package", "font", "lucide.ttf"), os.path.join(out_dir, "icons.ttf"),
                sorted({codepoints[w] for w in wanted}))
    return {w: codepoints[w] for w in wanted}


def main():
    if len(sys.argv) != 3:
        print(__doc__)
        sys.exit(1)
    src, assets = sys.argv[1], sys.argv[2]
    build_moon(src, os.path.join(assets, "textures", "menu"))
    ttf_dir = os.path.join(os.path.dirname(os.path.abspath(__file__)), "fonts")
    icons = build_fonts(src, ttf_dir)
    import bake_sdf_fonts
    os.makedirs(os.path.join(assets, "fonts"), exist_ok=True)
    for name, em, ranges, kern in bake_sdf_fonts.FONTS:
        bake_sdf_fonts.bake(os.path.join(ttf_dir, name + ".ttf"), name, em, ranges, kern, os.path.join(assets, "fonts"))
    # Java constants for the icon set are generated so names never drift from codepoints.
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    gen = os.path.join(root, "src", "main", "java", "dev", "xeys", "visuals", "ui", "Icons.java")
    with open(gen, "w") as fh:
        fh.write("package dev.xeys.visuals.ui;\n\n/**\n"
                 " * Lucide icon glyphs (ISC license, https://lucide.dev) in the bundled icon font. Generated by\n"
                 " * tools/prepare_assets.py from tools/icons.txt; draw with {@code Text.icon}.\n */\n"
                 "public final class Icons {\n")
        for name, cp in icons.items():
            const = name.upper().replace("-", "_")
            fh.write(f"\tpublic static final char {const} = '\\u{cp:04x}';\n")
        fh.write("\n\tprivate Icons() {\n\t}\n}\n")
    print("icon constants ->", gen)


if __name__ == "__main__":
    main()
