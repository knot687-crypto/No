package dev.xeys.visuals.ui.menu;

import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Round glass icon button with a glass tooltip that eases in below it. */
final class IconButton extends Widget {
	static final float SIZE = 40f;

	private final char icon;
	private final String tooltip;
	private final Runnable action;
	float appear = 1f;

	IconButton(char icon, String tooltip, Runnable action) {
		this.icon = icon;
		this.tooltip = tooltip;
		this.action = action;
	}

	@Override
	public boolean focusable() {
		return true;
	}

	@Override
	protected void draw(GuiGraphics g, float mx, float my) {
		float hv = this.hover.get();
		float pr = this.press.get();
		float a = this.appear;
		if (a <= 0.001f) {
			return;
		}
		float cx = this.x + this.w * 0.5f;
		float cy = this.y + this.h * 0.5f;
		float s = 1f + 0.05f * hv - 0.07f * pr;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(s, s);
		float r = this.w * 0.5f;
		if (hv > 0.01f) {
			Draw.glow(g, -r, -r, r * 2f, r * 2f, r, 20f, -2f, Colors.withAlpha(Theme.accent(), 0.28f * hv * a));
		}
		Draw.glass(g, -r, -r, r * 2f, r * 2f, Colors.lerp(0xA6080809, 0x8C121216, hv)).radius(r)
			.border(1f, Colors.lerpOklab(Theme.GLASS_BORDER, Colors.withAlpha(Theme.accent(), 0.65f), hv)).borderFade(0.4f)
			.alpha(a).submit();
		if (this.focus.get() > 0.01f) {
			Draw.circleShape(g, 0f, 0f, r + 4f).color(0).border(1.5f, Colors.withAlpha(Theme.accent(), 0.85f * this.focus.get() * a)).submit();
		}
		Text.icon(g, this.icon, 0f, 0f, 17f, Colors.mulAlpha(Colors.lerpOklab(Theme.TEXT_SECONDARY, Theme.TEXT, hv), a));
		g.pose().popMatrix();

		float tip = Math.max(hv, this.focus.get()) * a;
		if (tip > 0.01f) {
			float tw = Fonts.MEDIUM.width(this.tooltip, 12f) + 20f;
			float th = 28f;
			float tx = Math.min(cx - tw * 0.5f, dev.xeys.visuals.ui.core.UiScale.width() - tw - 12f);
			float ty = this.y + this.h + 8f + (1f - tip) * -4f;
			Draw.shadow(g, tx, ty, tw, th, 9f, 0f, 6f, 18f, 0f, Colors.withAlpha(0x000000, 0.45f * tip));
			Draw.glass(g, tx, ty, tw, th, 0xD00A0A0C).radius(9f).border(1f, Theme.HAIRLINE_STRONG).alpha(tip).submit();
			Text.drawMiddle(g, Fonts.MEDIUM, this.tooltip, tx + tw * 0.5f, ty + th * 0.5f, 12f, Colors.mulAlpha(Theme.TEXT, tip), Text.Align.CENTER);
		}
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
