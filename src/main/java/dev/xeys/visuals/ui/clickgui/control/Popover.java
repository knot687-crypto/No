package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.Widget;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Floating glass panel (color picker, dropdown, profiles) that eases in near its anchor. */
public abstract class Popover extends Widget {
	protected final Anim open = new Anim(0f);
	private boolean closing;

	protected Popover() {
		this.open.to(1f, 0.22f, Easing.OUT_CUBIC);
	}

	/** Places the popover below (or above, if it would overflow) an anchor rectangle, inside the screen. */
	public void place(float ax, float ay, float aw, float ah, float screenW, float screenH) {
		float px = Math.min(Math.max(12f, ax + aw - this.w), screenW - this.w - 12f);
		float py = ay + ah + 8f;
		if (py + this.h > screenH - 12f) {
			py = Math.max(12f, ay - this.h - 8f);
		}
		this.x = px;
		this.y = py;
	}

	public void close() {
		if (!this.closing) {
			this.closing = true;
			this.open.to(0f, 0.14f, Easing.OUT_CUBIC);
		}
	}

	public boolean closed() {
		return this.closing && this.open.get() <= 0.001f;
	}

	public boolean closing() {
		return this.closing;
	}

	@Override
	protected boolean clickable() {
		return false;
	}

	@Override
	protected final void draw(GuiGraphics g, float mx, float my) {
		float a = this.open.get();
		if (a <= 0.001f) {
			return;
		}
		float s = 0.96f + 0.04f * a;
		g.pose().pushMatrix();
		g.pose().translate(this.x + this.w * 0.5f, this.y + this.h * 0.5f + (1f - a) * -6f);
		g.pose().scale(s, s);
		g.pose().translate(-(this.x + this.w * 0.5f), -(this.y + this.h * 0.5f));
		Draw.shadow(g, this.x, this.y, this.w, this.h, 14f, 0f, 14f, 36f, -4f, Colors.withAlpha(0x000000, 0.6f * a));
		Draw.rect(g, this.x, this.y, this.w, this.h).radius(14f).color(Colors.withAlpha(0xFF0E0E11, 0.985f * a))
			.border(1f, Colors.withAlpha(0xFFFFFF, 0.13f * a)).borderFade(0.35f).submit();
		this.drawContent(g, mx, my, a);
		g.pose().popMatrix();
	}

	protected abstract void drawContent(GuiGraphics g, float mx, float my, float alpha);

	protected static int dim(int color, float alpha) {
		return Colors.mulAlpha(color, alpha);
	}

	protected static int text(float alpha) {
		return Colors.mulAlpha(Theme.TEXT, alpha);
	}
}
