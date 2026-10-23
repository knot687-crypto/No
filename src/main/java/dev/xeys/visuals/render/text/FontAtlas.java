package dev.xeys.visuals.render.text;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2FloatOpenHashMap;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

/**
 * Signed-distance-field glyph atlas, baked offline from a TrueType font (tools/bake_sdf_fonts.py).
 *
 * <p>All metrics are stored in em units (1.0 = font size) so text can be drawn at any size from the same
 * atlas. Distances are encoded with {@link #ON_EDGE} at the outline and {@link #DIST_SCALE} levels per
 * atlas pixel, matching {@code sdf_text.fsh}.
 */
public final class FontAtlas {
	public static final int ON_EDGE = 128;
	public static final int PADDING = 8;
	public static final float DIST_SCALE = (float) ON_EDGE / PADDING;
	private static final int CACHE_VERSION = 3;
	public static final int MIP_LEVELS = 3;

	public final String name;
	public final float emPx;
	public final float ascent;
	public final float descent;
	public final float lineGap;
	public final float capHeight;
	public final float xHeight;
	public final int width;
	public final int height;
	/** Mip chain (level 0 first), single channel. */
	public final byte[][] pixels;
	final Int2ObjectOpenHashMap<Glyph> glyphs;
	final Glyph[] ascii = new Glyph[128];
	private final Long2FloatOpenHashMap kerning;

	private FontAtlas(String name, float emPx, float ascent, float descent, float lineGap, float capHeight, float xHeight,
					  int width, int height, byte[][] pixels, Int2ObjectOpenHashMap<Glyph> glyphs, Long2FloatOpenHashMap kerning) {
		this.name = name;
		this.emPx = emPx;
		this.ascent = ascent;
		this.descent = descent;
		this.lineGap = lineGap;
		this.capHeight = capHeight;
		this.xHeight = xHeight;
		this.width = width;
		this.height = height;
		this.pixels = pixels;
		this.glyphs = glyphs;
		this.kerning = kerning;
		for (int cp = 0; cp < 128; cp++) {
			this.ascii[cp] = glyphs.get(cp);
		}
	}

	public Glyph glyph(int codepoint) {
		return codepoint < 128 && codepoint >= 0 ? this.ascii[codepoint] : this.glyphs.get(codepoint);
	}

	/** Kerning adjustment in em between two codepoints (0 when none). */
	public float kern(int left, int right) {
		return this.kerning.get(key(left, right));
	}

	private static long key(int left, int right) {
		return ((long) left << 32) | (right & 0xFFFFFFFFL);
	}

	/** Baked glyph. Offsets and sizes in em, atlas rectangle in texels. */
	public record Glyph(int codepoint, float advance, float xOff, float yOff, float w, float h,
						int atlasX, int atlasY, int atlasW, int atlasH) {
		public boolean visible() {
			return this.atlasW > 0 && this.atlasH > 0;
		}
	}

	// ------------------------------------------------------------------ loading

	/**
	 * Reads an atlas baked offline by {@code tools/bake_sdf_fonts.py}. No font rasterisation or distance
	 * field generation ever runs in game.
	 */
	public static FontAtlas load(String name, byte[] data) throws IOException {
		return read(name, data);
	}

	// ------------------------------------------------------------------ binary format

	private static FontAtlas read(String name, byte[] data) throws IOException {
		try (DataInputStream in = new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(data)))) {
			if (in.readInt() != CACHE_VERSION) {
				throw new IOException("cache version mismatch");
			}
			float emPx = in.readFloat();
			float ascent = in.readFloat();
			float descent = in.readFloat();
			float lineGap = in.readFloat();
			float capHeight = in.readFloat();
			float xHeight = in.readFloat();
			int width = in.readInt();
			int height = in.readInt();
			int count = in.readInt();
			Int2ObjectOpenHashMap<Glyph> glyphs = new Int2ObjectOpenHashMap<>(count);
			for (int i = 0; i < count; i++) {
				Glyph g = new Glyph(in.readInt(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat(),
					in.readInt(), in.readInt(), in.readInt(), in.readInt());
				glyphs.put(g.codepoint, g);
			}
			int kernCount = in.readInt();
			Long2FloatOpenHashMap kerning = new Long2FloatOpenHashMap(kernCount);
			for (int i = 0; i < kernCount; i++) {
				kerning.put(in.readLong(), in.readFloat());
			}
			int levels = in.readInt();
			byte[][] pixels = new byte[levels][];
			for (int i = 0; i < levels; i++) {
				pixels[i] = new byte[in.readInt()];
				in.readFully(pixels[i]);
			}
			return new FontAtlas(name, emPx, ascent, descent, lineGap, capHeight, xHeight, width, height, pixels, glyphs, kerning);
		}
	}
}
