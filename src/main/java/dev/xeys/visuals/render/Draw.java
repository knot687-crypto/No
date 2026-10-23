package dev.xeys.visuals.render;

import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTextureView;
import dev.xeys.visuals.render.blur.SceneBlur;
import dev.xeys.visuals.render.element.ShapeElement;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.render.TextureSetup;

/**
 * The XEYS 2D drawing API. Every primitive is an analytic signed distance field rendered on a single
 * quad, so edges are anti-aliased exactly at any GUI scale, rotation or fractional position.
 *
 * <p>Quick calls draw immediately. Builder calls ({@link #rect}, {@link #glass}, ...) return a pooled
 * {@link ShapeElement}: configure it, then call {@code submit()}.
 */
public final class Draw {
	private static final Pool<ShapeElement> SHAPES = Pool.create(ShapeElement::new);
	private static final float DEG = (float) (Math.PI / 180.0);

	private Draw() {
	}

	private static ShapeElement obtain(GuiGraphics g, com.mojang.blaze3d.pipeline.RenderPipeline pipeline, float x, float y, float w, float h) {
		return SHAPES.obtain().begin(g, pipeline, x, y, w, h,
			ClipStack.x0(), ClipStack.y0(), ClipStack.x1(), ClipStack.y1(), ClipStack.feather());
	}

	// ------------------------------------------------------------------ rectangles

	public static ShapeElement rect(GuiGraphics g, float x, float y, float w, float h) {
		return obtain(g, XeysPipelines.RECT, x, y, w, h);
	}

	public static void rect(GuiGraphics g, float x, float y, float w, float h, float radius, int color) {
		if (Colors.alpha(color) == 0) {
			return;
		}
		rect(g, x, y, w, h).radius(radius).color(color).submit();
	}

	public static void outline(GuiGraphics g, float x, float y, float w, float h, float radius, float width, int color) {
		rect(g, x, y, w, h).radius(radius).color(0).border(width, color).submit();
	}

	/** Soft drop shadow (CSS-like): offset, blur radius, spread. */
	public static void shadow(GuiGraphics g, float x, float y, float w, float h, float radius, float offsetX, float offsetY,
							  float blur, float spread, int color) {
		if (Colors.alpha(color) == 0) {
			return;
		}
		rect(g, x + offsetX, y + offsetY, w, h).radius(radius).color(color).soft(blur).spread(spread).submit();
	}

	/** Additive light bloom around a box; looks right on dark backgrounds. */
	public static void glow(GuiGraphics g, float x, float y, float w, float h, float radius, float blur, float spread, int color) {
		if (Colors.alpha(color) == 0) {
			return;
		}
		rect(g, x, y, w, h).radius(radius).color(color).soft(blur).spread(spread).additive(0.65f).submit();
	}

	// ------------------------------------------------------------------ glass

	/**
	 * Frosted glass panel: the blurred scene behind it tinted by {@code tint} (alpha = tint strength).
	 * Falls back to a translucent fill if blur is disabled or unavailable.
	 */
	public static ShapeElement glass(GuiGraphics g, float x, float y, float w, float h, int tint) {
		TextureSetup blur = SceneBlur.acquire();
		if (blur == null) {
			int fallback = Colors.withAlpha(tint, Math.min(255, Colors.alpha(tint) + 40));
			return rect(g, x, y, w, h).color(fallback);
		}
		return obtain(g, XeysPipelines.GLASS, x, y, w, h).color(tint).texture(blur);
	}

	// ------------------------------------------------------------------ circles & arcs

	public static void circle(GuiGraphics g, float cx, float cy, float radius, int color) {
		rect(g, cx - radius, cy - radius, radius * 2f, radius * 2f).radius(radius).color(color).submit();
	}

	public static ShapeElement circleShape(GuiGraphics g, float cx, float cy, float radius) {
		return rect(g, cx - radius, cy - radius, radius * 2f, radius * 2f).radius(radius);
	}

	/**
	 * Ring segment with round caps. Angles in degrees, clockwise from 12 o'clock. A sweep of 360 draws a
	 * full ring. Call {@code gradientTo} for an angular gradient.
	 */
	public static ShapeElement arc(GuiGraphics g, float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg) {
		float ext = radius + thickness * 0.5f;
		float sweep = Math.min(Math.abs(sweepDeg), 360f) * DEG;
		float start = sweepDeg < 0f ? (startDeg + sweepDeg) * DEG : startDeg * DEG;
		return obtain(g, XeysPipelines.ARC, cx - ext, cy - ext, ext * 2f, ext * 2f)
			.params(radius, thickness, start, sweep >= 6.2831f ? 6.2832f : sweep);
	}

	public static void ring(GuiGraphics g, float cx, float cy, float radius, float thickness, int color) {
		arc(g, cx, cy, radius, thickness, 0f, 360f).color(color).submit();
	}

	/** Arc with a gradient from {@code from} to {@code to} along its length. */
	public static void arcGradient(GuiGraphics g, float cx, float cy, float radius, float thickness, float startDeg, float sweepDeg,
								   int from, int to) {
		arc(g, cx, cy, radius, thickness, startDeg, sweepDeg).color(from).secondary(to).paramsB(1f, 0f, 0f, 0f).submit();
	}

	// ------------------------------------------------------------------ lines

	public static ShapeElement line(GuiGraphics g, float x0, float y0, float x1, float y1, float thickness) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float len = (float) Math.sqrt(dx * dx + dy * dy);
		float cos = len > 1e-5f ? dx / len : 1f;
		float sin = len > 1e-5f ? dy / len : 0f;
		float cx = (x0 + x1) * 0.5f;
		float cy = (y0 + y1) * 0.5f;
		float half = thickness * 0.5f;
		return obtain(g, XeysPipelines.LINE, cx - len * 0.5f - half, cy - half, len + thickness, thickness)
			.params(len * 0.5f, half, 1f, 0f)
			.rotation(cos, sin);
	}

	public static void line(GuiGraphics g, float x0, float y0, float x1, float y1, float thickness, int color) {
		line(g, x0, y0, x1, y1, thickness).color(color).submit();
	}

	// ------------------------------------------------------------------ images

	public static ShapeElement image(GuiGraphics g, GpuTextureView texture, GpuSampler sampler, float x, float y, float w, float h,
									 float u0, float v0, float u1, float v1) {
		return obtain(g, XeysPipelines.IMAGE, x, y, w, h)
			.texture(TextureSetup.singleTexture(texture, sampler))
			.uv(u0, v0, u1, v1);
	}

	// ------------------------------------------------------------------ clipping

	public static void pushClip(GuiGraphics g, float x0, float y0, float x1, float y1) {
		ClipStack.push(g, x0, y0, x1, y1, 1f);
	}

	/** Clip with a soft edge, in framebuffer pixels. */
	public static void pushClip(GuiGraphics g, float x0, float y0, float x1, float y1, float featherPx) {
		ClipStack.push(g, x0, y0, x1, y1, featherPx);
	}

	public static void popClip() {
		ClipStack.pop();
	}
}
