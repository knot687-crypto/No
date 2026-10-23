package dev.xeys.visuals.ui.core;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.anim.Spring;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Base for XEYS widgets. Geometry is in design units. Hover and press are springs, so every state change
 * eases in and out and an interrupted hover never snaps.
 */
public abstract class Widget {
	public float x;
	public float y;
	public float w;
	public float h;
	public boolean visible = true;
	public boolean enabled = true;
	protected boolean hovered;
	protected boolean pressed;
	protected boolean focused;
	protected final Spring hover = new Spring(0f, 300f, 1f);
	protected final Spring press = new Spring(0f, 700f, 0.9f);
	protected final Spring focus = new Spring(0f, 300f, 1f);

	public Widget bounds(float x, float y, float w, float h) {
		this.x = x;
		this.y = y;
		this.w = w;
		this.h = h;
		return this;
	}

	public boolean contains(float px, float py) {
		return px >= this.x && py >= this.y && px < this.x + this.w && py < this.y + this.h;
	}

	public final void render(GuiGraphics g, float mx, float my) {
		if (!this.visible) {
			return;
		}
		this.hovered = this.enabled && this.contains(mx, my);
		this.hover.target(this.hovered ? 1f : 0f);
		this.press.target(this.pressed && this.hovered ? 1f : 0f);
		this.focus.target(this.focused ? 1f : 0f);
		if (this.hovered && this.clickable()) {
			g.requestCursor(CursorTypes.POINTING_HAND);
		}
		this.draw(g, mx, my);
	}

	protected abstract void draw(GuiGraphics g, float mx, float my);

	protected boolean clickable() {
		return true;
	}

	public boolean mouseClicked(float mx, float my, int button) {
		if (this.visible && this.enabled && this.contains(mx, my)) {
			this.pressed = true;
			return true;
		}
		return false;
	}

	public boolean mouseReleased(float mx, float my, int button) {
		if (this.pressed) {
			this.pressed = false;
			if (this.contains(mx, my) && this.enabled) {
				this.onClick(button);
			}
			return true;
		}
		return false;
	}

	public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		return false;
	}

	public boolean mouseScrolled(float mx, float my, float amount) {
		return false;
	}

	public boolean keyPressed(int key, int scancode, int modifiers) {
		return false;
	}

	public boolean charTyped(int codepoint, int modifiers) {
		return false;
	}

	protected void onClick(int button) {
	}

	public boolean focusable() {
		return false;
	}

	public void setFocused(boolean focused) {
		this.focused = focused;
	}

	public boolean isFocused() {
		return this.focused;
	}

	public boolean isHovered() {
		return this.hovered;
	}
}
