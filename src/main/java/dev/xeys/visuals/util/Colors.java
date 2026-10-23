package dev.xeys.visuals.util;

/** ARGB color math. All colors are packed {@code 0xAARRGGBB} ints. */
public final class Colors {
	private Colors() {
	}

	public static int alpha(int c) {
		return c >>> 24;
	}

	public static int red(int c) {
		return (c >> 16) & 0xFF;
	}

	public static int green(int c) {
		return (c >> 8) & 0xFF;
	}

	public static int blue(int c) {
		return c & 0xFF;
	}

	public static int argb(int a, int r, int g, int b) {
		return (clamp255(a) << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
	}

	public static int argb(float a, float r, float g, float b) {
		return argb(Math.round(a * 255f), Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f));
	}

	private static int clamp255(int v) {
		return v < 0 ? 0 : (v > 255 ? 255 : v);
	}

	public static int withAlpha(int c, int alpha) {
		return (clamp255(alpha) << 24) | (c & 0xFFFFFF);
	}

	public static int withAlpha(int c, float alpha) {
		return withAlpha(c, Math.round(alpha * 255f));
	}

	public static int mulAlpha(int c, float factor) {
		if (factor >= 1f) {
			return c;
		}
		return withAlpha(c, Math.round(alpha(c) * Math.max(factor, 0f)));
	}

	/** Straight sRGB lerp, fine for alpha fades and close colors. */
	public static int lerp(int from, int to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		return argb(
			Math.round(alpha(from) + (alpha(to) - alpha(from)) * t),
			Math.round(red(from) + (red(to) - red(from)) * t),
			Math.round(green(from) + (green(to) - green(from)) * t),
			Math.round(blue(from) + (blue(to) - blue(from)) * t)
		);
	}

	// Render-thread scratch space so per-frame color animation allocates nothing.
	private static final float[] LAB_A = new float[3];
	private static final float[] LAB_B = new float[3];

	/** Perceptual interpolation in OKLab: no muddy midpoints between saturated hues. */
	public static int lerpOklab(int from, int to, float t) {
		if (t <= 0f) {
			return from;
		}
		if (t >= 1f) {
			return to;
		}
		float[] a = toOklab(from, LAB_A);
		float[] b = toOklab(to, LAB_B);
		float l = a[0] + (b[0] - a[0]) * t;
		float m = a[1] + (b[1] - a[1]) * t;
		float s = a[2] + (b[2] - a[2]) * t;
		int alpha = Math.round(alpha(from) + (alpha(to) - alpha(from)) * t);
		return withAlpha(fromOklab(l, m, s), alpha);
	}

	private static float toLinear(int v) {
		float c = v / 255f;
		return c <= 0.04045f ? c / 12.92f : (float) Math.pow((c + 0.055f) / 1.055f, 2.4f);
	}

	private static int toSrgb(float c) {
		c = Math.max(0f, Math.min(1f, c));
		float v = c <= 0.0031308f ? c * 12.92f : 1.055f * (float) Math.pow(c, 1f / 2.4f) - 0.055f;
		return Math.round(v * 255f);
	}

	private static float[] toOklab(int c, float[] out) {
		float r = toLinear(red(c));
		float g = toLinear(green(c));
		float b = toLinear(blue(c));
		float l = (float) Math.cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b);
		float m = (float) Math.cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b);
		float s = (float) Math.cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b);
		out[0] = 0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s;
		out[1] = 1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s;
		out[2] = 0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s;
		return out;
	}

	private static int fromOklab(float L, float A, float B) {
		float l = L + 0.3963377774f * A + 0.2158037573f * B;
		float m = L - 0.1055613458f * A - 0.0638541728f * B;
		float s = L - 0.0894841775f * A - 1.2914855480f * B;
		l = l * l * l;
		m = m * m * m;
		s = s * s * s;
		float r = 4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s;
		float g = -1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s;
		float b = -0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s;
		return argb(255, toSrgb(r), toSrgb(g), toSrgb(b));
	}

	/** Mixes toward white (amount > 0) or black (amount < 0). */
	public static int shade(int c, float amount) {
		return amount >= 0f ? lerp(c, withAlpha(0xFFFFFF, alpha(c)), amount) : lerp(c, withAlpha(0x000000, alpha(c)), -amount);
	}

	public static int fromHsv(float h, float s, float v, int alpha) {
		h = ((h % 1f) + 1f) % 1f;
		float r;
		float g;
		float b;
		int i = (int) (h * 6f);
		float f = h * 6f - i;
		float p = v * (1f - s);
		float q = v * (1f - f * s);
		float t = v * (1f - (1f - f) * s);
		switch (i % 6) {
			case 0 -> { r = v; g = t; b = p; }
			case 1 -> { r = q; g = v; b = p; }
			case 2 -> { r = p; g = v; b = t; }
			case 3 -> { r = p; g = q; b = v; }
			case 4 -> { r = t; g = p; b = v; }
			default -> { r = v; g = p; b = q; }
		}
		return argb(alpha, Math.round(r * 255f), Math.round(g * 255f), Math.round(b * 255f));
	}

	/** Returns {hue, saturation, value} in [0, 1]. */
	public static float[] toHsv(int c, float[] out) {
		float r = red(c) / 255f;
		float g = green(c) / 255f;
		float b = blue(c) / 255f;
		float max = Math.max(r, Math.max(g, b));
		float min = Math.min(r, Math.min(g, b));
		float delta = max - min;
		float h = 0f;
		if (delta > 1e-6f) {
			if (max == r) {
				h = ((g - b) / delta) % 6f;
			} else if (max == g) {
				h = (b - r) / delta + 2f;
			} else {
				h = (r - g) / delta + 4f;
			}
			h /= 6f;
			if (h < 0f) {
				h += 1f;
			}
		}
		out[0] = h;
		out[1] = max <= 1e-6f ? 0f : delta / max;
		out[2] = max;
		return out;
	}

	/** Relative luminance (WCAG) in [0, 1]. */
	public static float luminance(int c) {
		return 0.2126f * toLinear(red(c)) + 0.7152f * toLinear(green(c)) + 0.0722f * toLinear(blue(c));
	}

	public static String toHex(int c, boolean withAlpha) {
		return withAlpha ? String.format("#%08X", c) : String.format("#%06X", c & 0xFFFFFF);
	}

	/** Parses #RGB, #RRGGBB or #AARRGGBB (the leading # is optional). Returns {@code fallback} on error. */
	public static int parseHex(String text, int fallback) {
		String s = text.trim();
		if (s.startsWith("#")) {
			s = s.substring(1);
		}
		try {
			if (s.length() == 3) {
				int r = Integer.parseInt(s.substring(0, 1), 16);
				int g = Integer.parseInt(s.substring(1, 2), 16);
				int b = Integer.parseInt(s.substring(2, 3), 16);
				return argb(255, r * 17, g * 17, b * 17);
			}
			if (s.length() == 6) {
				return 0xFF000000 | Integer.parseInt(s, 16);
			}
			if (s.length() == 8) {
				return (int) Long.parseLong(s, 16);
			}
		} catch (NumberFormatException ignored) {
			// fall through
		}
		return fallback;
	}
}
