package dev.xeys.visuals.render;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.state.GuiRenderState;

/**
 * Full-screen color fade drawn above everything (including other screens), used for transitions:
 * menu exit into a vanilla screen, and the fade into the world after loading.
 */
public final class ScreenFade {
	private static final Anim ALPHA = new Anim(0f);
	private static int color = 0xFF000000;

	private ScreenFade() {
	}

	/** Starts fully covered by {@code argb} and fades out. */
	public static void fadeFrom(int argb, float seconds, Easing easing) {
		color = argb;
		ALPHA.restart(1f, 0f, seconds, easing);
	}

	public static void fadeFromBlack(float seconds) {
		fadeFrom(0xFF020203, seconds, Easing.OUT_CUBIC);
	}

	/** Holds the screen covered (used between a fade-out and the next screen appearing). */
	public static void cover(int argb) {
		color = argb;
		ALPHA.snap(1f);
	}

	public static boolean active() {
		return ALPHA.get() > 0.001f;
	}

	static void render(GuiRenderState state) {
		float a = ALPHA.get();
		if (a <= 0.001f) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		state.nextStratum();
		GuiGraphics g = new GuiGraphics(mc, state, 0, 0);
		Draw.rect(g, -2f, -2f, g.guiWidth() + 4f, g.guiHeight() + 4f).color(Colors.mulAlpha(color, a)).submit();
	}
}
