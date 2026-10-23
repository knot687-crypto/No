package dev.xeys.visuals.render.text;

import dev.xeys.visuals.render.ClipStack;
import dev.xeys.visuals.render.Pool;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;

/**
 * SDF text drawing. Sizes are in local GUI units (the font's em size). Text is positioned either by its
 * baseline, its line box top, or optically centered on its cap height, which is what makes labels sit
 * perfectly in the middle of buttons.
 */
public final class Text {
	private static final Pool<TextElement> POOL = Pool.create(TextElement::new);
	/** Interned single-character strings for icon glyphs (Private Use Area). */
	private static final String[] ICON_STRINGS = new String[0x1900];

	public enum Align {
		LEFT,
		CENTER,
		RIGHT
	}

	private Text() {
	}

	// ------------------------------------------------------------------ convenience

	/** Draws with {@code y} at the top of the line box. Returns the advance width. */
	public static float draw(GuiGraphics g, XeysFont font, CharSequence text, float x, float y, float size, int color) {
		return drawBaseline(g, font, text, x, y + font.ascent(size), size, color, 0f, 0f);
	}

	/** Draws vertically centered on {@code centerY} using the cap height (optical centering). */
	public static float drawMiddle(GuiGraphics g, XeysFont font, CharSequence text, float x, float centerY, float size, int color, Align align) {
		return drawMiddle(g, font, text, x, centerY, size, color, align, 0f);
	}

	public static float drawMiddle(GuiGraphics g, XeysFont font, CharSequence text, float x, float centerY, float size, int color, Align align,
								   float tracking) {
		float w = font.width(text, size, tracking);
		float left = switch (align) {
			case LEFT -> x;
			case CENTER -> x - w * 0.5f;
			case RIGHT -> x - w;
		};
		drawBaseline(g, font, text, left, centerY + font.capHeight(size) * 0.5f, size, color, tracking, 0f);
		return w;
	}

	/** Icon glyph centered on a point. Icons are square, so the em box is used for centering. */
	public static void icon(GuiGraphics g, char icon, float cx, float cy, float size, int color) {
		XeysFont font = Fonts.ICONS;
		float w = font.width(icon, size);
		// Lucide glyphs occupy the em square from the baseline up (y-down: baseline - size .. baseline).
		submit(g, font, iconString(icon), 0, 1, cx - w * 0.5f, cy + size * 0.5f, size, color, 0f, 0f, 0f, 0f, 0);
	}

	private static String iconString(char icon) {
		int index = icon - 0xE000;
		if (index < 0 || index >= ICON_STRINGS.length) {
			return String.valueOf(icon);
		}
		String s = ICON_STRINGS[index];
		if (s == null) {
			s = String.valueOf(icon);
			ICON_STRINGS[index] = s;
		}
		return s;
	}

	public static float drawBaseline(GuiGraphics g, XeysFont font, CharSequence text, float x, float baseline, float size, int color,
									 float tracking, float weight) {
		if (text.isEmpty() || Colors.alpha(color) == 0) {
			return 0f;
		}
		submit(g, font, text, 0, text.length(), x, baseline, size, color, tracking, weight, 0f, 0f, 0);
		return font.width(text, size, tracking);
	}

	/** Soft shadow under text. Draw before the text itself. */
	public static void shadow(GuiGraphics g, XeysFont font, CharSequence text, float x, float baseline, float size, int color,
							  float offsetX, float offsetY, float blur, float tracking) {
		if (text.isEmpty() || Colors.alpha(color) == 0) {
			return;
		}
		submit(g, font, text, 0, text.length(), x + offsetX, baseline + offsetY, size, color, tracking, 0f, 0f, Math.max(blur, 0.5f), 0);
	}

	/** Text with an outline ring (e.g. nametags, crisp HUD labels on bright scenes). */
	public static void outlined(GuiGraphics g, XeysFont font, CharSequence text, float x, float baseline, float size, int color,
								float outlinePx, int outlineColor, float tracking) {
		if (text.isEmpty()) {
			return;
		}
		submit(g, font, text, 0, text.length(), x, baseline, size, color, tracking, 0f, outlinePx, 0f, outlineColor);
	}

	/** Draws {@code text} truncated with an ellipsis to fit {@code maxWidth}. */
	public static void drawTruncated(GuiGraphics g, XeysFont font, String text, float x, float centerY, float size, int color, float maxWidth) {
		if (font.width(text, size) <= maxWidth) {
			drawMiddle(g, font, text, x, centerY, size, color, Align.LEFT);
			return;
		}
		float ell = font.width("…", size);
		int fit = font.fit(text, size, Math.max(0f, maxWidth - ell));
		String cut = text.substring(0, fit).stripTrailing() + "…";
		drawMiddle(g, font, cut, x, centerY, size, color, Align.LEFT);
	}

	// ------------------------------------------------------------------ core

	static void submit(GuiGraphics g, XeysFont font, CharSequence text, int start, int end, float x, float baseline, float size, int color,
					   float tracking, float weight, float outline, float softness, int secondary) {
		FontAtlas atlas = font.atlas();
		if (atlas == null || font.view() == null || size <= 0f) {
			return;
		}
		TextElement e = POOL.obtain();
		e.pose.set(g.pose());
		e.atlas = atlas;
		e.textureSetup = font.textureSetup();
		e.setText(text, start, end);
		e.x = x;
		e.baseline = baseline;
		e.size = size;
		e.tracking = tracking;
		e.color = color;
		e.weight = weight;
		e.outline = outline;
		e.softness = softness;
		e.c0 = Colors.red(secondary) / 255f;
		e.c1 = Colors.green(secondary) / 255f;
		e.c2 = Colors.blue(secondary) / 255f;
		e.c3 = Colors.alpha(secondary) / 255f;
		e.clipX0 = ClipStack.x0();
		e.clipY0 = ClipStack.y0();
		e.clipX1 = ClipStack.x1();
		e.clipY1 = ClipStack.y1();
		e.feather = ClipStack.feather();
		e.scissor = g.scissorStack.peek();

		float width = font.width(text, start, end, size, tracking);
		float pad = 2f + outline + softness * 2f;
		float lx0 = x - pad;
		float lx1 = x + width + pad;
		float ly0 = baseline - font.ascent(size) - pad;
		float ly1 = baseline - font.descent(size) + pad;
		var m = e.pose;
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			float lx = (i == 0 || i == 3) ? lx0 : lx1;
			float ly = i < 2 ? ly0 : ly1;
			float tx = m.m00 * lx + m.m10 * ly + m.m20;
			float ty = m.m01 * lx + m.m11 * ly + m.m21;
			minX = Math.min(minX, tx);
			minY = Math.min(minY, ty);
			maxX = Math.max(maxX, tx);
			maxY = Math.max(maxY, ty);
		}
		float scale = Minecraft.getInstance().getWindow().getGuiScale();
		if (maxX * scale <= e.clipX0 || maxY * scale <= e.clipY0 || minX * scale >= e.clipX1 || minY * scale >= e.clipY1) {
			return;
		}
		int bx0 = (int) Math.floor(Math.max(minX, e.clipX0 / scale));
		int by0 = (int) Math.floor(Math.max(minY, e.clipY0 / scale));
		int bx1 = (int) Math.ceil(Math.min(maxX, e.clipX1 / scale));
		int by1 = (int) Math.ceil(Math.min(maxY, e.clipY1 / scale));
		if (bx1 <= bx0 || by1 <= by0) {
			return;
		}
		ScreenRectangle rect = new ScreenRectangle(bx0, by0, bx1 - bx0, by1 - by0);
		e.bounds = e.scissor != null ? e.scissor.intersection(rect) : rect;
		if (e.bounds != null) {
			g.guiRenderState.submitGuiElement(e);
		}
	}
}
