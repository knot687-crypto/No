package dev.xeys.visuals.hud;

import dev.xeys.visuals.modules.client.HudStyleModule;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Drawing helpers that apply the global HUD style. Every HUD module draws through these. */
public final class HudStyle {
	private static HudStyleModule style;

	private HudStyle() {
	}

	public static void bind(HudStyleModule module) {
		style = module;
	}

	public static HudStyleModule config() {
		return style;
	}

	public static float pad() {
		return style.padding.value();
	}

	public static float radius() {
		return style.radius.value();
	}

	public static float textSize() {
		return style.textSize.value();
	}

	public static int text() {
		return style.text.color();
	}

	public static int textSecondary() {
		return Colors.mulAlpha(style.text.color(), 0.62f);
	}

	public static int accent() {
		return style.accent.color();
	}

	public static XeysFont font() {
		return switch (style.weight.get()) {
			case REGULAR -> Fonts.REGULAR;
			case MEDIUM -> Fonts.MEDIUM;
			case SEMIBOLD -> Fonts.SEMIBOLD;
			case BOLD -> Fonts.BOLD;
		};
	}

	public static XeysFont fontStrong() {
		return switch (style.weight.get()) {
			case REGULAR, MEDIUM -> Fonts.SEMIBOLD;
			case SEMIBOLD, BOLD -> Fonts.BOLD;
		};
	}

	/** The element background in the configured style. */
	public static void panel(GuiGraphics g, float x, float y, float w, float h) {
		panel(g, x, y, w, h, style.radius.value());
	}

	public static void panel(GuiGraphics g, float x, float y, float w, float h, float radius) {
		float opacity = style.opacity.value() / 100f;
		boolean border = style.border.on();
		switch (style.background.get()) {
			case GLASS -> {
				if (style.shadow.on()) {
					Draw.shadow(g, x, y, w, h, radius, 0f, 1.5f, 6f, 0f, Colors.withAlpha(0x000000, 0.35f * opacity));
				}
				int tint = Colors.withAlpha(0x060607, 0.25f + 0.65f * opacity);
				var e = Draw.glass(g, x, y, w, h, tint).radius(radius);
				if (border) {
					e.border(0.75f, Theme.GLASS_BORDER).borderFade(0.25f);
				}
				e.submit();
			}
			case SOLID -> {
				if (style.shadow.on()) {
					Draw.shadow(g, x, y, w, h, radius, 0f, 1.5f, 6f, 0f, Colors.withAlpha(0x000000, 0.4f * opacity));
				}
				var e = Draw.rect(g, x, y, w, h).radius(radius).color(Colors.withAlpha(Theme.SURFACE, opacity));
				if (border) {
					e.border(0.75f, Theme.HAIRLINE_STRONG).borderFade(0.4f);
				}
				e.submit();
			}
			case OUTLINE -> Draw.outline(g, x, y, w, h, radius, 1f, Colors.withAlpha(style.accent.color(), 0.4f + 0.6f * opacity));
			case NONE -> {
			}
		}
	}

	/** Text vertically centered on {@code centerY}. Returns the advance width. */
	public static float label(GuiGraphics g, XeysFont font, CharSequence text, float x, float centerY, float size, int color, Text.Align align) {
		if (style.textShadow.on() || style.background.get() == HudStyleModule.Background.NONE) {
			float w = font.width(text, size);
			float left = align == Text.Align.LEFT ? x : (align == Text.Align.CENTER ? x - w * 0.5f : x - w);
			Text.shadow(g, font, text, left, centerY + font.capHeight(size) * 0.5f, size, Colors.withAlpha(0x000000, 0.55f * Colors.alpha(color) / 255f),
				0f, 0.6f, 1.6f, 0f);
		}
		return Text.drawMiddle(g, font, text, x, centerY, size, color, align);
	}

	public static float label(GuiGraphics g, CharSequence text, float x, float centerY, int color) {
		return label(g, font(), text, x, centerY, textSize(), color, Text.Align.LEFT);
	}

	public static float width(CharSequence text) {
		return font().width(text, textSize());
	}
}
