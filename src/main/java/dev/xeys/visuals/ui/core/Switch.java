package dev.xeys.visuals.ui.core;

import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;

/** The animated toggle switch. Stateless drawing: callers own the spring driving {@code t} (0 off, 1 on). */
public final class Switch {
	public static final float WIDTH = 38f;
	public static final float HEIGHT = 22f;

	private Switch() {
	}

	public static void draw(GuiGraphics g, float x, float y, float t, float hover, float alpha) {
		float w = WIDTH;
		float h = HEIGHT;
		float r = h * 0.5f;
		float on = Math.max(0f, Math.min(1f, t));
		int track = Colors.lerpOklab(Colors.lerp(0xFF1E1E24, 0xFF26262D, hover), Theme.accent(), on);
		if (on > 0.01f) {
			Draw.glow(g, x, y, w, h, r, 12f, -2f, Colors.withAlpha(Theme.accent(), 0.30f * on * alpha));
		}
		Draw.rect(g, x, y, w, h).radius(r).color(Colors.mulAlpha(track, alpha))
			.border(1f, Colors.withAlpha(0xFFFFFF, (0.10f - 0.06f * on) * alpha)).submit();
		float knobR = r - 3f;
		// Overshooting springs can push t past the ends; keep the knob inside the track.
		float travel = w - 2f * r;
		float kx = x + r + travel * t;
		kx = Math.max(x + r - 1.5f, Math.min(x + w - r + 1.5f, kx));
		float ky = y + r;
		Draw.shadow(g, kx - knobR, ky - knobR, knobR * 2f, knobR * 2f, knobR, 0f, 1f, 4f, 0f, Colors.withAlpha(0x000000, 0.45f * alpha));
		int knob = Colors.lerp(0xFFB9B9C2, 0xFFFFFFFF, Math.max(on, hover * 0.5f));
		Draw.circle(g, kx, ky, knobR, Colors.mulAlpha(knob, alpha));
	}
}
