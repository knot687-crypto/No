package dev.xeys.visuals.ui.clickgui;

import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.ui.core.Switch;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import java.util.function.Consumer;
import net.minecraft.client.gui.GuiGraphics;

/** A module tile: click the switch to toggle, anywhere else to open its settings. */
final class ModuleCard extends Widget {
	static final float HEIGHT = 74f;
	final Module module;
	private final Spring on;
	private final Spring enabledGlow;
	private final Consumer<Module> openSettings;
	boolean showCategory;
	float appear = 1f;
	private float switchX;
	private float switchY;

	ModuleCard(Module module, Consumer<Module> openSettings) {
		this.module = module;
		this.openSettings = openSettings;
		this.on = new Spring(module.isEnabled() ? 1f : 0f, 380f, 0.78f);
		this.enabledGlow = new Spring(module.isEnabled() ? 1f : 0f, 200f, 1f);
	}

	@Override
	protected void draw(GuiGraphics g, float mx, float my) {
		float a = this.appear;
		if (a <= 0.001f) {
			return;
		}
		boolean enabled = this.module.isEnabled();
		this.on.target(enabled ? 1f : 0f);
		this.enabledGlow.target(enabled ? 1f : 0f);
		float hv = this.hover.get();
		float pr = this.press.get();
		float en = this.enabledGlow.get();
		float lift = -1.5f * hv + (1f - a) * 10f;
		float s = 1f - 0.012f * pr;

		g.pose().pushMatrix();
		g.pose().translate(this.x + this.w * 0.5f, this.y + this.h * 0.5f + lift);
		g.pose().scale(s, s);
		float x = -this.w * 0.5f;
		float y = -this.h * 0.5f;
		float r = 14f;

		if (en > 0.01f) {
			Draw.glow(g, x, y, this.w, this.h, r, 22f, -6f, Colors.withAlpha(Theme.accent(), 0.10f * en * a));
		}
		int base = Colors.lerp(0xC2121215, 0xD919191E, hv);
		int tintL = Colors.lerpOklab(base, Colors.withAlpha(Colors.lerpOklab(0xFF121215, Theme.accent(), 0.13f), Colors.alpha(base)), en);
		int border = Colors.lerpOklab(Colors.lerp(Theme.HAIRLINE, 0x24FFFFFF, hv), Colors.withAlpha(Theme.accent(), 0.38f + 0.12f * hv), en);
		Draw.rect(g, x, y, this.w, this.h).radius(r).gradientH(Colors.mulAlpha(tintL, a), Colors.mulAlpha(base, a))
			.border(1f, Colors.mulAlpha(border, a)).borderFade(0.55f).submit();

		// Icon tile.
		float ts = 40f;
		float tx = x + 16f;
		float ty = -ts * 0.5f;
		int tile = Colors.lerpOklab(0xFF1D1D23, Colors.lerpOklab(0xFF16161B, Theme.accent(), 0.22f), en);
		Draw.rect(g, tx, ty, ts, ts).radius(11f).color(Colors.mulAlpha(tile, a))
			.border(1f, Colors.mulAlpha(Colors.lerpOklab(0x14FFFFFF, Colors.withAlpha(Theme.accent(), 0.4f), en), a)).submit();
		Text.icon(g, this.module.icon(), tx + ts * 0.5f, 0f, 18f, Colors.mulAlpha(Colors.lerpOklab(Theme.TEXT_SECONDARY, Theme.accent(), en), a));

		float textX = tx + ts + 14f;
		float textW = this.w - (textX - x) - Switch.WIDTH - 30f;
		Text.drawTruncated(g, Fonts.SEMIBOLD, this.module.name(), textX, -9f, 13.5f, Colors.mulAlpha(Theme.TEXT, a), textW);
		String sub = this.showCategory ? this.module.category().title() + "  ·  " + this.module.description() : this.module.description();
		Text.drawTruncated(g, Fonts.REGULAR, sub, textX, 10f, 11f, Colors.mulAlpha(Theme.TEXT_MUTED, a), textW);

		this.switchX = this.x + this.w - 16f - Switch.WIDTH;
		this.switchY = this.y + (this.h - Switch.HEIGHT) * 0.5f;
		if (this.module.isAlwaysOn()) {
			Text.icon(g, Icons.CHEVRON_RIGHT, x + this.w - 16f - 10f, 0f, 15f, Colors.mulAlpha(Colors.lerp(Theme.TEXT_FAINT, Theme.TEXT_SECONDARY, hv), a));
		} else {
			boolean overSwitch = this.overSwitch(mx, my);
			Switch.draw(g, x + this.w - 16f - Switch.WIDTH, -Switch.HEIGHT * 0.5f, this.on.get(), overSwitch ? 1f : hv * 0.4f, a);
		}
		g.pose().popMatrix();
	}

	private boolean overSwitch(float mx, float my) {
		return mx >= this.switchX - 8f && mx <= this.switchX + Switch.WIDTH + 8f && my >= this.switchY - 8f && my <= this.switchY + Switch.HEIGHT + 8f;
	}

	@Override
	protected void onClick(int button) {
		// Handled in mouseReleased so we know where the release happened.
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		if (!this.pressed) {
			return false;
		}
		this.pressed = false;
		if (!this.contains(mx, my)) {
			return true;
		}
		if (button == 1 || this.module.isAlwaysOn() || !this.overSwitch(mx, my)) {
			UiSounds.click();
			this.openSettings.accept(this.module);
		} else {
			this.module.toggle();
			UiSounds.toggle(this.module.isEnabled());
		}
		return true;
	}
}
