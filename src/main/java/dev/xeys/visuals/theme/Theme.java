package dev.xeys.visuals.theme;

import dev.xeys.visuals.util.Colors;

/**
 * The XEYS design tokens. Everything visual reads from here so the whole client stays consistent and a
 * single accent change restyles every screen.
 *
 * <p>Palette: near-black surfaces (#050505 to #18181B), translucent black glass, white hairlines at low
 * alpha, and one user-chosen accent from which hover/pressed/soft/glow variants are derived.
 */
public final class Theme {
	// ------------------------------------------------------------------ surfaces
	public static final int BG_DEEP = 0xFF050505;
	public static final int BG = 0xFF0A0A0B;
	public static final int SURFACE = 0xFF111113;
	public static final int SURFACE_RAISED = 0xFF16161A;
	public static final int SURFACE_HOVER = 0xFF1C1C21;
	public static final int SURFACE_ACTIVE = 0xFF222228;

	// ------------------------------------------------------------------ glass
	/** Tint over the blurred backdrop (alpha = how much of the scene is hidden). */
	public static final int GLASS_TINT = 0xB8070708;
	public static final int GLASS_TINT_LIGHT = 0x8C0A0A0C;
	public static final int GLASS_TINT_HEAVY = 0xD9060607;
	/** 1px highlight border (fades toward the bottom). */
	public static final int GLASS_BORDER = 0x26FFFFFF;
	public static final int HAIRLINE = 0x14FFFFFF;
	public static final int HAIRLINE_STRONG = 0x24FFFFFF;
	public static final int INNER_GLOW = 0x0DFFFFFF;

	// ------------------------------------------------------------------ text
	public static final int TEXT = 0xFFF4F4F6;
	public static final int TEXT_SECONDARY = 0xB3F4F4F6;
	public static final int TEXT_MUTED = 0x80F4F4F6;
	public static final int TEXT_FAINT = 0x52F4F4F6;
	public static final int TEXT_DISABLED = 0x3DF4F4F6;

	// ------------------------------------------------------------------ status
	public static final int SUCCESS = 0xFF34D399;
	public static final int WARNING = 0xFFFBBF24;
	public static final int DANGER = 0xFFF87171;
	public static final int INFO = 0xFF60A5FA;

	// ------------------------------------------------------------------ shadow
	public static final int SHADOW = 0x8C000000;
	public static final int SHADOW_SOFT = 0x59000000;

	// ------------------------------------------------------------------ geometry (8px grid)
	public static final float GRID = 8f;
	public static final float S0_5 = 4f;
	public static final float S1 = 8f;
	public static final float S1_5 = 12f;
	public static final float S2 = 16f;
	public static final float S3 = 24f;
	public static final float S4 = 32f;
	public static final float S5 = 40f;
	public static final float S6 = 48f;

	public static final float R_XS = 4f;
	public static final float R_SM = 6f;
	public static final float R_MD = 10f;
	public static final float R_LG = 14f;
	public static final float R_XL = 20f;

	// ------------------------------------------------------------------ type scale (design px)
	public static final float T_MICRO = 9f;
	public static final float T_CAPTION = 10.5f;
	public static final float T_SMALL = 11.5f;
	public static final float T_BODY = 13f;
	public static final float T_LABEL = 14f;
	public static final float T_TITLE = 16f;
	public static final float T_HEADING = 20f;
	public static final float T_DISPLAY = 28f;

	// ------------------------------------------------------------------ motion
	public static final float DUR_FAST = 0.12f;
	public static final float DUR_BASE = 0.2f;
	public static final float DUR_SLOW = 0.35f;
	public static final float DUR_PAGE = 0.45f;

	// ------------------------------------------------------------------ accent
	public static final int DEFAULT_ACCENT = 0xFF8AB4FF;

	public static final int[] ACCENT_PRESETS = {
		0xFF8AB4FF, // Moonlight
		0xFFA78BFA, // Violet
		0xFFF472B6, // Rose
		0xFFFB7185, // Coral
		0xFFFBBF24, // Amber
		0xFF34D399, // Emerald
		0xFF67E8F9, // Ice
		0xFFE5E7EB, // Platinum
	};

	public static final String[] ACCENT_PRESET_NAMES = {
		"Moonlight", "Violet", "Rose", "Coral", "Amber", "Emerald", "Ice", "Platinum"
	};

	private static int accent = DEFAULT_ACCENT;
	private static int accentHover;
	private static int accentPressed;
	private static int accentSoft;
	private static int accentGlow;
	private static int accentText;
	private static int onAccent;

	static {
		setAccent(DEFAULT_ACCENT);
	}

	private Theme() {
	}

	public static void setAccent(int argb) {
		accent = argb | 0xFF000000;
		accentHover = Colors.shade(accent, 0.12f);
		accentPressed = Colors.shade(accent, -0.18f);
		accentSoft = Colors.withAlpha(accent, 0.16f);
		accentGlow = Colors.withAlpha(accent, 0.38f);
		// Accent used as text on dark surfaces: lifted slightly for legibility.
		accentText = Colors.luminance(accent) < 0.25f ? Colors.shade(accent, 0.3f) : accent;
		onAccent = Colors.luminance(accent) > 0.45f ? 0xFF0A0A0B : 0xFFFFFFFF;
	}

	public static int accent() {
		return accent;
	}

	public static int accentHover() {
		return accentHover;
	}

	public static int accentPressed() {
		return accentPressed;
	}

	public static int accentSoft() {
		return accentSoft;
	}

	public static int accentGlow() {
		return accentGlow;
	}

	public static int accentText() {
		return accentText;
	}

	/** Readable foreground on top of a solid accent fill. */
	public static int onAccent() {
		return onAccent;
	}

	public static int accent(float alpha) {
		return Colors.withAlpha(accent, alpha);
	}
}
