package dev.xeys.visuals.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.joml.Matrix3x2f;

/**
 * Soft clip regions evaluated per pixel in the XEYS shaders. Unlike vanilla scissoring (whole GUI pixels,
 * hard edges) these are exact framebuffer rectangles with an optional feather, so scrolling lists can fade
 * out at their edges and nothing snaps at fractional UI scales.
 */
public final class ClipStack {
	private static final int MAX = 32;
	private static final float NONE = 1.0e7f;
	private static final float[] STACK = new float[MAX * 5];
	private static int depth;

	private ClipStack() {
	}

	static {
		reset();
	}

	public static void reset() {
		depth = 0;
		STACK[0] = -NONE;
		STACK[1] = -NONE;
		STACK[2] = NONE;
		STACK[3] = NONE;
		STACK[4] = 1f;
	}

	/** Pushes a clip rectangle given in the current local (pose) coordinates. */
	public static void push(GuiGraphics g, float x0, float y0, float x1, float y1, float featherPx) {
		if (depth + 1 >= MAX) {
			throw new IllegalStateException("Clip stack overflow");
		}
		Matrix3x2f m = g.pose();
		float scale = Minecraft.getInstance().getWindow().getGuiScale();
		float ax = (m.m00 * x0 + m.m10 * y0 + m.m20) * scale;
		float ay = (m.m01 * x0 + m.m11 * y0 + m.m21) * scale;
		float bx = (m.m00 * x1 + m.m10 * y1 + m.m20) * scale;
		float by = (m.m01 * x1 + m.m11 * y1 + m.m21) * scale;
		int base = depth * 5;
		int next = base + 5;
		STACK[next] = Math.max(Math.min(ax, bx), STACK[base]);
		STACK[next + 1] = Math.max(Math.min(ay, by), STACK[base + 1]);
		STACK[next + 2] = Math.min(Math.max(ax, bx), STACK[base + 2]);
		STACK[next + 3] = Math.min(Math.max(ay, by), STACK[base + 3]);
		STACK[next + 4] = Math.max(featherPx, STACK[base + 4]);
		depth++;
	}

	public static void pop() {
		if (depth == 0) {
			throw new IllegalStateException("Clip stack underflow");
		}
		depth--;
	}

	public static boolean active() {
		return depth > 0;
	}

	public static float x0() {
		return STACK[depth * 5];
	}

	public static float y0() {
		return STACK[depth * 5 + 1];
	}

	public static float x1() {
		return STACK[depth * 5 + 2];
	}

	public static float y1() {
		return STACK[depth * 5 + 3];
	}

	public static float feather() {
		return STACK[depth * 5 + 4];
	}

	/** Whether a framebuffer-space box is entirely clipped away. */
	static boolean rejects(float minX, float minY, float maxX, float maxY) {
		int b = depth * 5;
		return maxX <= STACK[b] || maxY <= STACK[b + 1] || minX >= STACK[b + 2] || minY >= STACK[b + 3];
	}
}
