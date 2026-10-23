package dev.xeys.visuals.ui.core;

import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Resolution-independent "design space" for XEYS screens: 1080 design units tall at 1080p, scaling with
 * the window instead of vanilla's integer GUI scale. Because every primitive is a distance field, the UI
 * stays pixel-crisp at any fractional factor, and looks identical at every GUI scale setting.
 */
public final class UiScale {
	private static float userScale = 1f;

	private UiScale() {
	}

	public static void setUserScale(float scale) {
		userScale = Math.max(0.5f, Math.min(scale, 2f));
	}

	public static float userScale() {
		return userScale;
	}

	private static Window window() {
		return Minecraft.getInstance().getWindow();
	}

	/** Framebuffer pixels per design unit. Never below 0.62 so small windows stay legible. */
	public static float factor() {
		float base = window().getHeight() / 1080f;
		return Math.max(0.62f, Math.min(base, 4f)) * userScale;
	}

	/** GUI units per design unit (vanilla GUI scale already applied by the projection). */
	public static float guiPerDesign() {
		return factor() / window().getGuiScale();
	}

	public static float width() {
		return window().getWidth() / factor();
	}

	public static float height() {
		return window().getHeight() / factor();
	}

	/** Converts a GUI coordinate (as vanilla passes mouse positions) to design units. */
	public static float toDesign(double gui) {
		return (float) (gui / guiPerDesign());
	}

	public static void push(GuiGraphics g) {
		float s = guiPerDesign();
		g.pose().pushMatrix();
		g.pose().scale(s, s);
	}

	public static void pop(GuiGraphics g) {
		g.pose().popMatrix();
	}
}
