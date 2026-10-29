package dev.xeys.visuals.cosmetic;

import dev.xeys.visuals.Xeys;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * Procedural cape textures at four times vanilla's 64x32 layout. The design fills the cape's outer and
 * inner faces and the elytra region (vanilla draws elytra from the cape texture), so wings match too.
 * Animated designs are redrawn a few times a second into the same texture.
 */
public final class CapeTextures {
	public enum Design {
		OBSIDIAN(false),
		GRADIENT(false),
		GALAXY(true),
		AURORA(true),
		CARBON(false),
		SUNSET(false);

		final boolean animated;

		Design(boolean animated) {
			this.animated = animated;
		}

		public boolean animated() {
			return this.animated;
		}
	}

	/** Pixels per vanilla cape-texture pixel. */
	private static final int S = 4;
	private static final int W = 64 * S;
	private static final int H = 32 * S;
	public static final Identifier TEXTURE = Xeys.id("textures/cape/xeys.png");
	public static final ClientAsset.Texture ASSET = new ClientAsset.ResourceTexture(Xeys.id("cape/xeys"), TEXTURE);

	private static @Nullable DynamicTexture texture;

	private CapeTextures() {
	}

	/** Draws {@code design} with {@code accent} at time {@code t} (seconds) and uploads it. Render thread. */
	public static void draw(Design design, int accent, float t) {
		DynamicTexture tex = texture;
		if (tex == null || tex.getPixels() == null) {
			tex = new DynamicTexture(() -> "XEYS cape", W, H, true);
			texture = tex;
			Minecraft.getInstance().getTextureManager().register(TEXTURE, tex);
		}
		var img = tex.getPixels();
		// Outer face (seen from behind) and inner face of the cape.
		face(img, 1, 1, 10, 16, design, accent, t, 1f);
		face(img, 12, 1, 10, 16, design, accent, t, 0.7f);
		// Cape edges: a thin dark trim.
		fill(img, 0, 1, 1, 16, 0xFF0A0A0C);
		fill(img, 11, 1, 1, 16, 0xFF0A0A0C);
		fill(img, 1, 0, 20, 1, 0xFF0A0A0C);
		// The elytra region, drawn as one tall face so the wings carry the design.
		face(img, 22, 0, 24, 22, design, accent, t, 0.9f);
		tex.upload();
	}

	/** Frees the texture; vanilla capes show again. */
	public static void release() {
		if (texture != null) {
			Minecraft.getInstance().getTextureManager().release(TEXTURE);
			texture = null;
		}
	}

	private static void fill(com.mojang.blaze3d.platform.NativeImage img, int x, int y, int w, int h, int argb) {
		img.fillRect(x * S, y * S, w * S, h * S, argb);
	}

	private static void face(com.mojang.blaze3d.platform.NativeImage img, int fx, int fy, int fw, int fh, Design d, int accent, float t,
							 float shade) {
		int x0 = fx * S;
		int y0 = fy * S;
		int w = fw * S;
		int h = fh * S;
		for (int y = 0; y < h; y++) {
			for (int x = 0; x < w; x++) {
				float u = (x + 0.5f) / w;
				float v = (y + 0.5f) / h;
				int c = shade(color(d, u, v, x0 + x, y0 + y, accent, t), shade);
				img.setPixel(x0 + x, y0 + y, c);
			}
		}
	}

	private static int color(Design d, float u, float v, int px, int py, int accent, float t) {
		float ar = ((accent >> 16) & 0xFF) / 255f;
		float ag = ((accent >> 8) & 0xFF) / 255f;
		float ab = (accent & 0xFF) / 255f;
		return switch (d) {
			case OBSIDIAN -> {
				// Near-black glass with a fine accent border and a crossed monogram.
				float base = 0.04f + 0.035f * v;
				float r = base;
				float g = base;
				float b = base * 1.15f;
				float edge = Math.min(Math.min(u, 1f - u), Math.min(v, 1f - v));
				float border = edge < 0.035f ? 1f : (float) Math.exp(-(edge - 0.035f) * 90f) * 0.25f;
				float cx = (u - 0.5f) * 1.6f;
				float cy = (v - 0.36f) * 1.0f;
				float d1 = Math.abs(cx - cy);
				float d2 = Math.abs(cx + cy);
				float inside = Math.max(Math.abs(cx), Math.abs(cy)) < 0.22f ? 1f : 0f;
				float line = inside * Math.max(lineGlow(d1), lineGlow(d2));
				float a = Math.min(1f, border + line);
				yield rgb(r + ar * a, g + ag * a, b + ab * a);
			}
			case GRADIENT -> {
				float k = (1f - v) * (1f - v);
				float n = (hash(px, py) - 0.5f) * 0.03f;
				yield rgb(0.03f + ar * k * 0.9f + n, 0.03f + ag * k * 0.9f + n, 0.04f + ab * k * 0.9f + n);
			}
			case GALAXY -> {
				float neb = fbm(u * 3f + 1.3f, v * 4f + t * 0.04f);
				float r = 0.02f + 0.35f * neb * neb * 0.6f + ar * 0.12f * neb;
				float g = 0.01f + 0.08f * neb * neb;
				float b = 0.05f + 0.45f * neb * neb + ab * 0.1f * neb;
				float star = hash(px / 2, py / 2);
				if (star > 0.985f) {
					float tw = 0.6f + 0.4f * (float) Math.sin(t * 3.0 + star * 400.0);
					float s = (star - 0.985f) / 0.015f * tw;
					r += s;
					g += s;
					b += s;
				}
				yield rgb(r, g, b);
			}
			case AURORA -> {
				float base = 0.03f;
				float wave = (float) Math.sin(u * 7.0 + t * 0.9 + Math.sin(v * 5.0 + t * 0.5) * 1.4);
				float band = (float) Math.exp(-Math.pow((v - 0.45f - wave * 0.12f) / 0.16f, 2.0));
				float rays = 0.6f + 0.4f * fbm(u * 18f, t * 0.3f);
				float glow = band * rays;
				float top = Math.max(0f, 0.6f - v);
				yield rgb(base + glow * 0.15f + top * 0.25f * ar, base + glow * 0.95f, base + glow * 0.55f + top * 0.4f * ab);
			}
			case CARBON -> {
				// A 2x2 twill weave with a soft sheen, and an accent stripe down the middle.
				int cell = 6;
				int cxI = Math.floorDiv(px, cell);
				int cyI = Math.floorDiv(py, cell);
				boolean warp = ((cxI + cyI) & 1) == 0;
				float fx = (px % cell + 0.5f) / cell;
				float fy = (py % cell + 0.5f) / cell;
				float sheen = warp ? 0.5f + 0.5f * (float) Math.sin(fx * Math.PI) : 0.5f + 0.5f * (float) Math.sin(fy * Math.PI);
				float base = 0.05f + 0.07f * sheen;
				float stripe = Math.abs(u - 0.5f) < 0.06f ? 1f : (float) Math.exp(-(Math.abs(u - 0.5f) - 0.06f) * 60f) * 0.3f;
				yield rgb(base + ar * stripe * 0.9f, base + ag * stripe * 0.9f, base * 1.1f + ab * stripe * 0.9f);
			}
			case SUNSET -> {
				float k = v;
				float r = lerp(1.0f, 0.42f, k);
				float g = lerp(0.62f, 0.12f, k);
				float b = lerp(0.32f, 0.5f, k);
				// A retro sun cut by horizontal bars.
				float dx = (u - 0.5f) * 1.4f;
				float dy = v - 0.42f;
				boolean inSun = dx * dx + dy * dy < 0.07f && (v < 0.42f || ((int) (v * 40f) & 1) == 0);
				if (inSun) {
					r = 1f;
					g = lerp(0.9f, 0.45f, (v - 0.16f) / 0.5f);
					b = 0.35f;
				}
				yield rgb(r * 0.9f, g * 0.9f, b * 0.9f);
			}
		};
	}

	private static float lineGlow(float d) {
		return d < 0.012f ? 1f : (float) Math.exp(-(d - 0.012f) * 60f) * 0.6f;
	}

	private static int shade(int argb, float k) {
		int r = Math.round(((argb >> 16) & 0xFF) * k);
		int g = Math.round(((argb >> 8) & 0xFF) * k);
		int b = Math.round((argb & 0xFF) * k);
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}

	private static int rgb(float r, float g, float b) {
		int ri = Math.round(Math.max(0f, Math.min(1f, r)) * 255f);
		int gi = Math.round(Math.max(0f, Math.min(1f, g)) * 255f);
		int bi = Math.round(Math.max(0f, Math.min(1f, b)) * 255f);
		return 0xFF000000 | (ri << 16) | (gi << 8) | bi;
	}

	private static float lerp(float a, float b, float t) {
		float c = Math.max(0f, Math.min(1f, t));
		return a + (b - a) * c;
	}

	private static float hash(int x, int y) {
		int h = x * 374761393 + y * 668265263;
		h = (h ^ (h >>> 13)) * 1274126177;
		return ((h ^ (h >>> 16)) & 0xFFFFFF) / (float) 0x1000000;
	}

	private static float noise(float x, float y) {
		int xi = (int) Math.floor(x);
		int yi = (int) Math.floor(y);
		float fx = x - xi;
		float fy = y - yi;
		fx = fx * fx * (3f - 2f * fx);
		fy = fy * fy * (3f - 2f * fy);
		float a = hash(xi, yi);
		float b = hash(xi + 1, yi);
		float c = hash(xi, yi + 1);
		float d = hash(xi + 1, yi + 1);
		return a + (b - a) * fx + (c - a) * fy + (a - b - c + d) * fx * fy;
	}

	private static float fbm(float x, float y) {
		float sum = 0f;
		float amp = 0.5f;
		for (int i = 0; i < 4; i++) {
			sum += noise(x, y) * amp;
			x *= 2.03f;
			y *= 2.03f;
			amp *= 0.5f;
		}
		return sum;
	}
}
