package dev.xeys.visuals.ui.clickgui.control;

import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.TextField;
import dev.xeys.visuals.ui.core.UiSounds;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

/** HSV color picker with alpha, hex entry, dynamic modes and presets. */
public final class ColorPickerPopover extends Popover {
	/** What the picker edits: a ColorSetting or one stop of a gradient. */
	public interface Target {
		int base();

		void set(int argb);

		boolean alpha();

		default ColorSetting.@Nullable Mode mode() {
			return null;
		}

		default void mode(ColorSetting.Mode mode) {
		}

		default float speed() {
			return 1f;
		}

		default void speed(float speed) {
		}
	}

	private static final float PAD = 14f;
	private static final float SV_H = 150f;
	private static final float STRIP = 12f;
	private static final ColorSetting.Mode[] MODES = ColorSetting.Mode.values();
	private static final String[] MODE_LABELS = {"Static", "Accent", "Rainbow", "Pulse"};

	private final Target target;
	private final float[] hsv = new float[3];
	private float alpha;
	private final TextField hex = new TextField().textSize(12f).maxLength(9);
	private int drag;
	private float svX;
	private float svY;
	private float svW;
	private float hueY;
	private float alphaY;
	private float modesY;
	private float speedY;
	private float presetsY;

	public ColorPickerPopover(Target target) {
		this.target = target;
		int c = target.base();
		Colors.toHsv(c, this.hsv);
		this.alpha = Colors.alpha(c) / 255f;
		this.hex.filter(ch -> Character.digit(ch, 16) >= 0 || ch == '#').onSubmit(this::applyHex).onChange(s -> {
			if (s.replace("#", "").length() == 6 || s.replace("#", "").length() == 8) {
				this.applyHex();
			}
		});
		this.hex.setValue(Colors.toHex(c, target.alpha()));
		this.w = 268f;
		float h = PAD + SV_H + 12f + STRIP + (target.alpha() ? 12f + STRIP : 0f) + 14f + 32f;
		if (target.mode() != null) {
			h += 12f + 28f + 30f;
		}
		h += 12f + 22f + PAD;
		this.h = h;
	}

	private void applyHex() {
		int fallback = this.current();
		int parsed = Colors.parseHex(this.hex.value(), fallback);
		if (parsed != fallback) {
			Colors.toHsv(parsed, this.hsv);
			String raw = this.hex.value().replace("#", "");
			this.alpha = raw.length() == 8 ? Colors.alpha(parsed) / 255f : this.alpha;
			this.push(false);
		}
	}

	private int current() {
		return Colors.fromHsv(this.hsv[0], this.hsv[1], this.hsv[2], this.target.alpha() ? Math.round(this.alpha * 255f) : 255);
	}

	private void push(boolean updateHex) {
		int c = this.current();
		this.target.set(c);
		if (this.target.mode() == ColorSetting.Mode.ACCENT) {
			this.target.mode(ColorSetting.Mode.STATIC);
		}
		if (updateHex && !this.hex.isFocused()) {
			this.hex.setValue(Colors.toHex(c, this.target.alpha()));
		}
	}

	@Override
	protected void drawContent(GuiGraphics g, float mx, float my, float a) {
		this.handleDrag(mx, my);
		float x = this.x + PAD;
		float y = this.y + PAD;
		float w = this.w - PAD * 2f;
		this.svX = x;
		this.svY = y;
		this.svW = w;

		int hue = Colors.fromHsv(this.hsv[0], 1f, 1f, 255);
		Draw.rect(g, x, y, w, SV_H).radius(10f).gradientH(dim(0xFFFFFFFF, a), dim(hue, a)).submit();
		Draw.rect(g, x, y, w, SV_H).radius(10f).gradientV(0x00000000, dim(0xFF000000, a)).border(1f, dim(0x14FFFFFF, a)).submit();
		float kx = x + this.hsv[1] * w;
		float ky = y + (1f - this.hsv[2]) * SV_H;
		Draw.shadow(g, kx - 8f, ky - 8f, 16f, 16f, 8f, 0f, 1f, 5f, 0f, dim(0x80000000, a));
		Draw.circleShape(g, kx, ky, 7.5f).color(dim(this.current() | 0xFF000000, a)).border(2.5f, dim(0xFFFFFFFF, a)).submit();
		y += SV_H + 12f;

		// Hue strip: six segments through the color wheel.
		this.hueY = y;
		int[] stops = {0xFFFF0000, 0xFFFFFF00, 0xFF00FF00, 0xFF00FFFF, 0xFF0000FF, 0xFFFF00FF, 0xFFFF0000};
		float seg = w / 6f;
		for (int i = 0; i < 6; i++) {
			var e = Draw.rect(g, x + seg * i - (i > 0 ? 0.5f : 0f), y, seg + (i > 0 && i < 5 ? 1f : 0.5f), STRIP).gradientH(dim(stops[i], a), dim(stops[i + 1], a));
			if (i == 0) {
				e.radii(STRIP * 0.5f, 0f, 0f, STRIP * 0.5f);
			} else if (i == 5) {
				e.radii(0f, STRIP * 0.5f, STRIP * 0.5f, 0f);
			}
			e.submit();
		}
		this.knob(g, x + this.hsv[0] * w, y + STRIP * 0.5f, hue, a);
		y += STRIP;

		if (this.target.alpha()) {
			y += 12f;
			this.alphaY = y;
			this.checker(g, x, y, w, STRIP, a);
			int opaque = this.current() | 0xFF000000;
			Draw.rect(g, x, y, w, STRIP).pill().gradientH(dim(opaque & 0x00FFFFFF, a), dim(opaque, a)).border(1f, dim(0x14FFFFFF, a)).submit();
			this.knob(g, x + this.alpha * w, y + STRIP * 0.5f, this.current(), a);
			y += STRIP;
		}

		// Hex + preview.
		y += 14f;
		float sw = 32f;
		this.checker(g, x, y, sw, 32f, a);
		Draw.rect(g, x, y, sw, 32f).radius(8f).color(dim(this.target.mode() == null ? this.current() : this.preview(), a)).border(1f, dim(0x1AFFFFFF, a)).submit();
		this.hex.bounds(x + sw + 10f, y, w - sw - 10f, 32f);
		this.hex.render(g, mx, my);
		y += 32f;

		if (this.target.mode() != null) {
			y += 12f;
			this.modesY = y;
			float cw = w / MODES.length;
			Draw.rect(g, x, y, w, 28f).radius(8f).color(dim(0xFF141418, a)).border(1f, dim(0x12FFFFFF, a)).submit();
			int sel = this.target.mode().ordinal();
			Draw.rect(g, x + cw * sel + 2f, y + 2f, cw - 4f, 24f).radius(6f).gradientV(dim(0xFF2A2A31, a), dim(0xFF222228, a)).border(1f, dim(0x1AFFFFFF, a)).submit();
			for (int i = 0; i < MODES.length; i++) {
				boolean hov = mx >= x + cw * i && mx < x + cw * (i + 1) && my >= y && my < y + 28f;
				int c = i == sel ? Theme.TEXT : (hov ? Theme.TEXT_SECONDARY : Theme.TEXT_MUTED);
				Text.drawMiddle(g, Fonts.MEDIUM, MODE_LABELS[i], x + cw * (i + 0.5f), y + 14f, 11f, dim(c, a), Text.Align.CENTER);
			}
			y += 28f;
			// Speed slider for animated modes.
			this.speedY = y + 15f;
			boolean animated = this.target.mode() == ColorSetting.Mode.RAINBOW || this.target.mode() == ColorSetting.Mode.PULSE;
			float sa = animated ? a : a * 0.35f;
			Text.drawMiddle(g, Fonts.MEDIUM, "Speed", x, this.speedY, 11f, dim(Theme.TEXT_MUTED, sa), Text.Align.LEFT);
			float tx = x + 48f;
			float tw = w - 48f;
			float f = (this.target.speed() - 0.05f) / (5f - 0.05f);
			Draw.rect(g, tx, this.speedY - 2f, tw, 4f).pill().color(dim(0xFF1C1C21, sa)).submit();
			Draw.rect(g, tx, this.speedY - 2f, Math.max(4f, tw * f), 4f).pill().color(dim(Theme.accent(), sa)).submit();
			Draw.circle(g, tx + tw * f, this.speedY, 5.5f, dim(0xFFFFFFFF, sa));
			y += 30f;
		}

		// Presets: the accent palette.
		y += 12f;
		this.presetsY = y;
		int n = Theme.ACCENT_PRESETS.length;
		float gap = (w - n * 22f) / (n - 1);
		for (int i = 0; i < n; i++) {
			float cx = x + 11f + i * (22f + gap);
			boolean hov = Math.abs(mx - cx) < 11f && Math.abs(my - (y + 11f)) < 11f;
			Draw.circleShape(g, cx, y + 11f, hov ? 10.5f : 9.5f).color(dim(Theme.ACCENT_PRESETS[i], a))
				.border(hov ? 1.5f : 1f, dim(hov ? 0xCCFFFFFF : 0x26FFFFFF, a)).submit();
		}
	}

	private int preview() {
		ColorSetting.Mode m = this.target.mode();
		if (m == ColorSetting.Mode.ACCENT) {
			return Colors.withAlpha(Theme.accent(), Colors.alpha(this.target.base()));
		}
		return this.current();
	}

	private void knob(GuiGraphics g, float x, float y, int fill, float a) {
		Draw.shadow(g, x - 8f, y - 8f, 16f, 16f, 8f, 0f, 1f, 5f, 0f, dim(0x80000000, a));
		Draw.circleShape(g, x, y, 8f).color(dim(fill | 0xFF000000, a)).border(2.5f, dim(0xFFFFFFFF, a)).submit();
	}

	private void checker(GuiGraphics g, float x, float y, float w, float h, float a) {
		float cell = 4f;
		Draw.rect(g, x, y, w, h).radius(Math.min(h, 16f) * 0.5f).color(dim(0xFF2A2A2E, a)).submit();
		Draw.pushClip(g, x, y, x + w, y + h);
		for (float cy = 0; cy < h; cy += cell) {
			for (float cx = ((int) (cy / cell) % 2) * cell; cx < w; cx += cell * 2f) {
				Draw.rect(g, x + cx, y + cy, cell, cell).color(dim(0xFF3C3C42, a)).submit();
			}
		}
		Draw.popClip();
	}

	private void handleDrag(float mx, float my) {
		switch (this.drag) {
			case 1 -> {
				this.hsv[1] = clamp01((mx - this.svX) / this.svW);
				this.hsv[2] = 1f - clamp01((my - this.svY) / SV_H);
				this.push(true);
			}
			case 2 -> {
				this.hsv[0] = clamp01((mx - this.svX) / this.svW) * 0.9999f;
				this.push(true);
			}
			case 3 -> {
				this.alpha = clamp01((mx - this.svX) / this.svW);
				this.push(true);
			}
			case 4 -> this.target.speed(0.05f + clamp01((mx - this.svX - 48f) / (this.svW - 48f)) * (5f - 0.05f));
			default -> {
			}
		}
	}

	private static float clamp01(float v) {
		return Math.max(0f, Math.min(1f, v));
	}

	@Override
	public boolean mouseClicked(float mx, float my, int button) {
		if (!this.contains(mx, my)) {
			return false;
		}
		if (this.hex.mouseClicked(mx, my, button)) {
			this.hex.setFocused(true);
			return true;
		}
		this.hex.setFocused(false);
		boolean inX = mx >= this.svX - 6f && mx <= this.svX + this.svW + 6f;
		if (inX && my >= this.svY && my <= this.svY + SV_H) {
			this.drag = 1;
		} else if (inX && Math.abs(my - (this.hueY + STRIP * 0.5f)) <= 10f) {
			this.drag = 2;
		} else if (this.target.alpha() && inX && Math.abs(my - (this.alphaY + STRIP * 0.5f)) <= 10f) {
			this.drag = 3;
		} else if (this.target.mode() != null && my >= this.modesY && my < this.modesY + 28f) {
			int i = (int) ((mx - this.svX) / (this.svW / MODES.length));
			if (i >= 0 && i < MODES.length) {
				this.target.mode(MODES[i]);
				UiSounds.soft();
			}
		} else if (this.target.mode() != null && Math.abs(my - this.speedY) <= 10f && mx >= this.svX + 40f) {
			this.drag = 4;
		} else if (my >= this.presetsY && my <= this.presetsY + 22f) {
			int n = Theme.ACCENT_PRESETS.length;
			float gap = (this.svW - n * 22f) / (n - 1);
			for (int i = 0; i < n; i++) {
				float cx = this.svX + 11f + i * (22f + gap);
				if (Math.abs(mx - cx) <= 11f) {
					Colors.toHsv(Theme.ACCENT_PRESETS[i], this.hsv);
					this.push(true);
					UiSounds.soft();
				}
			}
		}
		this.handleDrag(mx, my);
		return true;
	}

	@Override
	public boolean mouseReleased(float mx, float my, int button) {
		this.drag = 0;
		return this.hex.mouseReleased(mx, my, button);
	}

	@Override
	public boolean mouseDragged(float mx, float my, int button, float dx, float dy) {
		return this.drag != 0 || this.hex.mouseDragged(mx, my, button, dx, dy);
	}

	@Override
	public boolean keyPressed(int key, int scancode, int modifiers) {
		return this.hex.keyPressed(key, scancode, modifiers);
	}

	@Override
	public boolean charTyped(int codepoint, int modifiers) {
		return this.hex.charTyped(codepoint, modifiers);
	}
}
