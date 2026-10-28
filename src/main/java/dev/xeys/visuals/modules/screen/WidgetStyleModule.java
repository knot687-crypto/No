package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.mixin.AbstractWidgetAccessor;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.render.text.XeysFont;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import java.util.WeakHashMap;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import org.jspecify.annotations.Nullable;

/**
 * Restyles vanilla's widgets everywhere (buttons, option toggles, sliders, checkboxes and text fields)
 * as rounded translucent controls with an accent hover, while every screen keeps its own layout and
 * behavior. Labels move to the client font.
 */
public final class WidgetStyleModule extends Module {
	private static WidgetStyleModule instance;
	private static final WeakHashMap<Object, float[]> HOVER = new WeakHashMap<>();

	private final NumberSetting radius = this.add(new NumberSetting("radius", "Corner Radius", 6f, 0f, 10f, 0.5f).unit("px").decimals(1)
		.group("Widgets"));
	private final NumberSetting fill = this.add(new NumberSetting("fill", "Fill", 9f, 0f, 30f, 1f).unit("%")
		.describe("How bright buttons are at rest").group("Widgets"));
	private final BoolSetting modernLabels = this.add(new BoolSetting("modern_labels", "Modern Labels", true)
		.describe("Draw button text in the client font").group("Widgets"));
	private final BoolSetting shadows = this.add(new BoolSetting("shadows", "Shadows", true).group("Widgets"));

	/** The button whose background was restyled last, so its label (if it has one) follows suit. */
	private static @Nullable AbstractButton labelFor;
	private static @Nullable GuiGraphics labelGraphics;

	public WidgetStyleModule() {
		super("widget_style", "Widget Style", Category.SCREEN, Icons.TOGGLE_RIGHT, "Modern buttons, sliders and fields in every menu");
		instance = this;
		this.enabledByDefault();
	}

	private static boolean on() {
		return instance != null && instance.isEnabled();
	}

	private static float hover(Object widget, boolean target) {
		float[] h = HOVER.computeIfAbsent(widget, k -> new float[] {target ? 1f : 0f, Clock.frame()});
		if (h[1] != Clock.frame()) {
			h[1] = Clock.frame();
			h[0] += ((target ? 1f : 0f) - h[0]) * Clock.smoothing(16f);
		}
		return h[0];
	}

	private static float alpha(AbstractWidget w) {
		return ((AbstractWidgetAccessor) w).xeys$alpha();
	}

	private float radiusFor(float h) {
		return Math.min(this.radius.value(), h * 0.4f);
	}

	private void box(GuiGraphics g, float x, float y, float w, float h, boolean active, float hv, boolean focused, float a) {
		float r = this.radiusFor(h);
		if (this.shadows.on() && active) {
			Draw.shadow(g, x, y, w, h, r, 0f, 1f, 4f, 0f, Colors.withAlpha(0x000000, 0.28f * a));
		}
		float base = this.fill.value() / 100f;
		int fillColor = active ? Colors.withAlpha(0xFFFFFF, (base + 0.07f * hv) * a) : Colors.withAlpha(0xFFFFFF, base * 0.45f * a);
		int border = active ? Colors.lerpOklab(Colors.withAlpha(0xFFFFFF, 0.13f), Theme.accent(0.75f), hv) : Colors.withAlpha(0xFFFFFF, 0.06f);
		Draw.rect(g, x, y, w, h).radius(r).color(fillColor).border(1f, Colors.mulAlpha(border, a)).borderFade(0.45f).submit();
		if (focused && active && hv < 0.5f) {
			Draw.outline(g, x - 1.5f, y - 1.5f, w + 3f, h + 3f, r + 1.5f, 1f, Colors.mulAlpha(Theme.accent(0.85f), a));
		}
		if (hv > 0.01f && active) {
			Draw.glow(g, x, y, w, h, r, 6f, -1f, Colors.withAlpha(Theme.accent(), 0.12f * hv * a));
		}
	}

	private void label(GuiGraphics g, String text, float cx, float cy, float maxW, boolean active, float a) {
		XeysFont font = Fonts.MEDIUM;
		float size = 9f;
		float tw = font.width(text, size);
		if (tw > maxW) {
			size = Math.max(7.5f, size * maxW / tw);
			tw = font.width(text, size);
		}
		int color = Colors.mulAlpha(active ? Theme.TEXT : Theme.TEXT_FAINT, a);
		if (tw > maxW) {
			Text.drawTruncated(g, font, text, cx - maxW * 0.5f, cy, size, color, maxW);
		} else {
			Text.drawMiddle(g, font, text, cx, cy, size, color, Text.Align.CENTER);
		}
	}

	// ------------------------------------------------------------------ hooks (called from mixins)

	public static boolean drawButton(GuiGraphics g, AbstractButton b, boolean highlighted) {
		if (!on()) {
			return false;
		}
		float a = alpha(b);
		float hv = hover(b, highlighted && b.active);
		instance.box(g, b.getX(), b.getY(), b.getWidth(), b.getHeight(), b.active, hv, b.isFocused(), a);
		labelFor = instance.modernLabels.on() ? b : null;
		labelGraphics = g;
		return true;
	}

	public static boolean drawButtonLabel(AbstractButton b) {
		if (b != labelFor || labelGraphics == null) {
			return false;
		}
		labelFor = null;
		instance.label(labelGraphics, b.getMessage().getString(), b.getX() + b.getWidth() * 0.5f, b.getY() + b.getHeight() * 0.5f,
			b.getWidth() - 8f, b.active, alpha(b));
		return true;
	}

	public static boolean drawSlider(GuiGraphics g, AbstractSliderButton s, float value, boolean dragging) {
		if (!on()) {
			return false;
		}
		float a = alpha(s);
		float x = s.getX();
		float y = s.getY();
		float w = s.getWidth();
		float h = s.getHeight();
		float hv = hover(s, (s.isHoveredOrFocused() || dragging) && s.active);
		instance.box(g, x, y, w, h, s.active, hv, s.isFocused(), a);
		float r = instance.radiusFor(h);
		float hx = x + 4f + value * (w - 8f);
		if (hx - x > 1f) {
			Draw.rect(g, x, y, hx - x, h).radii(r, Math.min(r, (hx - x) * 0.5f), Math.min(r, (hx - x) * 0.5f), r)
				.color(Theme.accent((0.16f + 0.1f * hv) * a)).submit();
		}
		// A slim notch rather than a solid knob, so it never hides the label's letters.
		float hh = h - 8f;
		Draw.rect(g, hx - 1.25f, y + 4f, 2.5f, hh).pill()
			.color(Colors.mulAlpha(Colors.lerpOklab(Colors.withAlpha(0xFFFFFF, 0.55f), Theme.accent(0.95f), hv), a)).submit();
		if (instance.modernLabels.on()) {
			String text = s.getMessage().getString();
			float size = Math.min(9f, 9f * (w - 10f) / Math.max(1f, Fonts.MEDIUM.width(text, 9f)));
			float tw = Fonts.MEDIUM.width(text, size);
			Text.shadow(g, Fonts.MEDIUM, text, x + (w - tw) * 0.5f, y + h * 0.5f + Fonts.MEDIUM.capHeight(size) * 0.5f, size,
				Colors.withAlpha(0x000000, 0.75f * a), 0f, 0f, 2.2f, 0f);
			instance.label(g, text, x + w * 0.5f, y + h * 0.5f, w - 10f, s.active, a);
		} else {
			g.drawCenteredString(net.minecraft.client.Minecraft.getInstance().font, s.getMessage(), (int) (x + w / 2), (int) (y + (h - 8) / 2),
				Colors.mulAlpha(0xFFFFFFFF, a));
		}
		return true;
	}

	public static boolean drawCheckbox(GuiGraphics g, Checkbox c, int x, int y, int size, boolean selected) {
		if (!on()) {
			return false;
		}
		float a = alpha(c);
		float hv = hover(c, c.isHoveredOrFocused() && c.active);
		float r = Math.min(4f, size * 0.25f);
		if (selected) {
			Draw.rect(g, x, y, size, size).radius(r).color(Colors.mulAlpha(Theme.accent(), a)).submit();
			Text.icon(g, Icons.CHECK, x + size * 0.5f, y + size * 0.5f, size * 0.7f, Colors.mulAlpha(Theme.onAccent(), a));
		} else {
			Draw.rect(g, x, y, size, size).radius(r).color(Colors.withAlpha(0xFFFFFF, (0.06f + 0.06f * hv) * a))
				.border(1f, Colors.mulAlpha(Colors.lerpOklab(Colors.withAlpha(0xFFFFFF, 0.32f), Theme.accent(0.85f), hv), a)).submit();
		}
		if (c.isFocused() && hv < 0.5f) {
			Draw.outline(g, x - 1.5f, y - 1.5f, size + 3f, size + 3f, r + 1.5f, 1f, Colors.mulAlpha(Theme.accent(0.85f), a));
		}
		return true;
	}

	public static boolean drawField(GuiGraphics g, EditBox e, int x, int y, int w, int h) {
		if (!on()) {
			return false;
		}
		float hv = hover(e, e.isFocused());
		float r = instance.radiusFor(h);
		int border = e.isActive() ? Colors.lerpOklab(Colors.withAlpha(0xFFFFFF, 0.16f), Theme.accent(0.9f), hv) : Colors.withAlpha(0xFFFFFF, 0.06f);
		Draw.rect(g, x, y, w, h).radius(r).color(Colors.withAlpha(0x000000, e.isActive() ? 0.4f : 0.22f)).border(1f, border).submit();
		if (hv > 0.01f) {
			Draw.glow(g, x, y, w, h, r, 6f, -1f, Colors.withAlpha(Theme.accent(), 0.14f * hv));
		}
		return true;
	}
}
