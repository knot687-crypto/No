package dev.xeys.visuals.ui.core;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Spring;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** Smooth, spring-damped scrolling with an auto-hiding overlay scrollbar. */
public final class ScrollState {
	private final Spring offset = new Spring(0f, 210f, 1f);
	private final Anim barAlpha = new Anim(0f);
	private float target;
	private float viewport = 1f;
	private float content = 1f;
	private double lastActivity = -10.0;
	private boolean dragging;
	private float dragStartY;
	private float dragStartTarget;

	public void setExtent(float viewportHeight, float contentHeight) {
		this.viewport = Math.max(1f, viewportHeight);
		this.content = Math.max(1f, contentHeight);
		this.target = this.clamp(this.target);
		this.offset.target(this.target);
	}

	private float maxScroll() {
		return Math.max(0f, this.content - this.viewport);
	}

	private float clamp(float v) {
		return Math.max(0f, Math.min(v, this.maxScroll()));
	}

	public float offset() {
		return this.offset.get();
	}

	public boolean scrollable() {
		return this.content > this.viewport + 0.5f;
	}

	public void scroll(float amount) {
		if (!this.scrollable()) {
			return;
		}
		this.target = this.clamp(this.target - amount * 64f);
		this.offset.target(this.target);
		this.lastActivity = Clock.time();
	}

	public void reset() {
		this.target = 0f;
		this.offset.snap(0f);
	}

	/** Keeps a region visible (e.g. a newly focused row). */
	public void ensureVisible(float top, float bottom) {
		if (top < this.target) {
			this.target = this.clamp(top);
		} else if (bottom > this.target + this.viewport) {
			this.target = this.clamp(bottom - this.viewport);
		}
		this.offset.target(this.target);
	}

	// ------------------------------------------------------------------ scrollbar

	public boolean barContains(float x, float top, float mx, float my) {
		return this.scrollable() && mx >= x - 6f && mx <= x + 6f && my >= top && my <= top + this.viewport;
	}

	public void beginDrag(float my) {
		this.dragging = true;
		this.dragStartY = my;
		this.dragStartTarget = this.target;
	}

	public void drag(float my) {
		if (this.dragging) {
			float ratio = this.content / this.viewport;
			this.target = this.clamp(this.dragStartTarget + (my - this.dragStartY) * ratio);
			this.offset.snap(this.target);
			this.lastActivity = Clock.time();
		}
	}

	public void endDrag() {
		this.dragging = false;
	}

	/** Draws a thin bar at {@code x} along a viewport starting at {@code top}. */
	public void drawBar(GuiGraphics g, float x, float top, boolean hovered) {
		if (!this.scrollable()) {
			return;
		}
		boolean active = this.dragging || hovered || Clock.time() - this.lastActivity < 0.9;
		this.barAlpha.to(active ? 1f : 0f, active ? 0.15f : 0.45f);
		float a = this.barAlpha.get();
		if (a <= 0.01f) {
			return;
		}
		float h = Math.max(28f, this.viewport * this.viewport / this.content);
		float y = top + (this.viewport - h) * (this.offset() / Math.max(1f, this.maxScroll()));
		float w = this.dragging || hovered ? 4f : 3f;
		Draw.rect(g, x - w * 0.5f, y, w, h).pill().color(Colors.withAlpha(0xFFFFFF, 0.28f * a)).submit();
	}

	public static int fadeColor() {
		return Theme.HAIRLINE;
	}
}
