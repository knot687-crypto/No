package dev.xeys.visuals.ui.menu;

import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Glass dock button: icon + label, hover lift and accent glow, spring press, keyboard focus ring. */
final class MenuButton extends Widget {
	static final float HEIGHT = 50f;
	private static final float ICON = 17f;
	private static final float LABEL = 14.5f;
	private static final float PAD = 22f;
	private static final float GAP = 11f;

	private final char icon;
	private final String label;
	private final boolean primary;
	private final Runnable action;
	/** Entrance progress (0 hidden .. 1 shown), driven by the screen for staggering. */
	float appear = 1f;

	MenuButton(char icon, String label, boolean primary, Runnable action) {
		this.icon = icon;
		this.label = label;
		this.primary = primary;
		this.action = action;
	}

	float preferredWidth() {
		return PAD * 2f + ICON + GAP + Fonts.SEMIBOLD.width(this.label, LABEL);
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	protected void draw(GuiGraphics g, float mx, float my) {
		float hv = this.hover.get();
		float pr = this.press.get();
		float fc = this.focus.get();
		float a = this.appear;
		if (a <= 0.001f) {
			return;
		}
		float lift = -2.5f * hv + (1f - a) * 18f;
		float scale = 1f + 0.018f * hv - 0.035f * pr;
		float cx = this.x + this.w * 0.5f;
		float cy = this.y + this.h * 0.5f + lift;

		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(scale, scale);
		float x0 = -this.w * 0.5f;
		float y0 = -this.h * 0.5f;
		float r = 15f;

		int accent = Theme.accent();
		// Depth: soft shadow that deepens on hover, plus an additive accent bloom.
		Draw.shadow(g, x0, y0, this.w, this.h, r, 0f, 10f + 6f * hv, 28f + 10f * hv, -4f, Colors.withAlpha(0x000000, (0.42f + 0.18f * hv) * a));
		float glow = (this.primary ? 0.45f : 0f) + hv * 0.75f;
		if (glow > 0.01f) {
			Draw.glow(g, x0, y0, this.w, this.h, r, 26f, -2f, Colors.withAlpha(accent, 0.30f * glow * a));
		}

		int tint = this.primary
			? Colors.lerp(Colors.withAlpha(Colors.lerpOklab(0xFF0B0B10, accent, 0.10f), 0.70f), Colors.withAlpha(Colors.lerpOklab(0xFF0B0B10, accent, 0.18f), 0.62f), hv)
			: Colors.lerp(0xB2080809, 0x9E101014, hv);
		int border = Colors.lerpOklab(this.primary ? Colors.withAlpha(accent, 0.42f) : Theme.GLASS_BORDER, Colors.withAlpha(accent, 0.70f), hv);
		Draw.glass(g, x0, y0, this.w, this.h, tint).radius(r).border(1f, border).borderFade(0.4f).innerGlow(14f, 0.35f + 0.25f * hv).alpha(a).submit();

		if (fc > 0.01f) {
			Draw.rect(g, x0 - 4f, y0 - 4f, this.w + 8f, this.h + 8f).radius(r + 4f).color(0).border(1.5f, Colors.withAlpha(accent, 0.85f * fc * a)).submit();
		}

		float contentW = ICON + GAP + Fonts.SEMIBOLD.width(this.label, LABEL);
		float left = -contentW * 0.5f;
		int iconColor = Colors.lerpOklab(this.primary ? accent : Theme.TEXT_SECONDARY, accent, hv);
		Text.icon(g, this.icon, left + ICON * 0.5f, 0f, ICON, Colors.mulAlpha(iconColor, a));
		int labelColor = this.enabled ? Theme.TEXT : Theme.TEXT_DISABLED;
		Text.drawMiddle(g, Fonts.SEMIBOLD, this.label, left + ICON + GAP, 0f, LABEL, Colors.mulAlpha(labelColor, a), Text.Align.LEFT);
		g.pose().popMatrix();
	}

	@Override
	protected void onClick(int button) {
		if (button == 0) {
			UiSounds.click();
			this.action.run();
		}
	}

	void activate() {
		UiSounds.click();
		this.action.run();
	}
}
