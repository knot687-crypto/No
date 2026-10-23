package dev.xeys.visuals.render.element;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.mixin.BufferBuilderAccessor;
import dev.xeys.visuals.render.XeysVertexFormats;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * One SDF shape rendered as a single quad. Instances are pooled per frame (see {@code Draw}); configure
 * with the fluent setters, then {@link #submit()}. Everything the fragment shader needs is written as
 * vertex attributes, so thousands of shapes share one draw call per pipeline.
 */
public final class ShapeElement implements GuiElementRenderState {
	private final Matrix3x2f pose = new Matrix3x2f();
	private @Nullable GuiGraphics graphics;
	private RenderPipeline pipeline;
	private TextureSetup textureSetup = TextureSetup.noTexture();
	private @Nullable ScreenRectangle scissor;
	private @Nullable ScreenRectangle bounds;

	// Shape box in local space.
	private float x;
	private float y;
	private float w;
	private float h;
	private float margin;

	private int cTL;
	private int cTR;
	private int cBR;
	private int cBL;

	private final float[] a = new float[4];
	private final float[] b = new float[4];
	private final float[] c = new float[4];
	private final float[] d = new float[4];
	private float spread;

	private float clipX0;
	private float clipY0;
	private float clipX1;
	private float clipY1;
	private float feather;

	// Lines are drawn in a rotated local frame.
	private boolean rotated;
	private float rotCos = 1f;
	private float rotSin;

	/** Resets the element for a new shape. Called by {@code Draw}. */
	public ShapeElement begin(GuiGraphics g, RenderPipeline pipeline, float x, float y, float w, float h,
							  float clipX0, float clipY0, float clipX1, float clipY1, float feather) {
		this.graphics = g;
		this.pipeline = pipeline;
		this.textureSetup = TextureSetup.noTexture();
		this.pose.set(g.pose());
		this.scissor = g.scissorStack.peek();
		this.bounds = null;
		this.x = x;
		this.y = y;
		this.w = Math.max(w, 0f);
		this.h = Math.max(h, 0f);
		this.margin = 1f;
		this.cTL = this.cTR = this.cBR = this.cBL = 0xFFFFFFFF;
		this.a[0] = this.w * 0.5f;
		this.a[1] = this.h * 0.5f;
		this.a[2] = 0f;
		this.a[3] = 0f;
		this.b[0] = this.b[1] = this.b[2] = this.b[3] = 0f;
		this.c[0] = this.c[1] = this.c[2] = 1f;
		this.c[3] = 0f;
		this.d[0] = 1f;
		this.d[1] = 0f;
		this.d[2] = 0f;
		this.d[3] = 0f;
		this.spread = 0f;
		this.clipX0 = clipX0;
		this.clipY0 = clipY0;
		this.clipX1 = clipX1;
		this.clipY1 = clipY1;
		this.feather = feather;
		this.rotated = false;
		this.rotCos = 1f;
		this.rotSin = 0f;
		return this;
	}

	// ------------------------------------------------------------------ fill

	public ShapeElement color(int argb) {
		this.cTL = this.cTR = this.cBR = this.cBL = argb;
		return this;
	}

	public ShapeElement gradientV(int top, int bottom) {
		this.cTL = this.cTR = top;
		this.cBL = this.cBR = bottom;
		return this;
	}

	public ShapeElement gradientH(int left, int right) {
		this.cTL = this.cBL = left;
		this.cTR = this.cBR = right;
		return this;
	}

	public ShapeElement gradient(int topLeft, int topRight, int bottomRight, int bottomLeft) {
		this.cTL = topLeft;
		this.cTR = topRight;
		this.cBR = bottomRight;
		this.cBL = bottomLeft;
		return this;
	}

	/** Multiplies the alpha of every color (fill and secondary). */
	public ShapeElement alpha(float alpha) {
		if (alpha >= 1f) {
			return this;
		}
		this.cTL = Colors.mulAlpha(this.cTL, alpha);
		this.cTR = Colors.mulAlpha(this.cTR, alpha);
		this.cBR = Colors.mulAlpha(this.cBR, alpha);
		this.cBL = Colors.mulAlpha(this.cBL, alpha);
		this.c[3] *= Math.max(alpha, 0f);
		return this;
	}

	// ------------------------------------------------------------------ geometry

	public ShapeElement radius(float r) {
		return this.radii(r, r, r, r);
	}

	public ShapeElement radii(float topLeft, float topRight, float bottomRight, float bottomLeft) {
		float max = Math.min(this.w, this.h) * 0.5f;
		this.b[0] = clampRadius(topLeft, max);
		this.b[1] = clampRadius(topRight, max);
		this.b[2] = clampRadius(bottomRight, max);
		this.b[3] = clampRadius(bottomLeft, max);
		return this;
	}

	private static float clampRadius(float r, float max) {
		return Math.max(0f, Math.min(r, max));
	}

	/** Fully rounded ends (capsule / circle). */
	public ShapeElement pill() {
		return this.radius(Math.min(this.w, this.h) * 0.5f);
	}

	// ------------------------------------------------------------------ border & glow

	/** Inner border of the given width drawn over the fill (premultiplied "over"). */
	public ShapeElement border(float width, int argb) {
		this.a[2] = Math.max(width, 0f);
		this.setSecondary(argb);
		return this;
	}

	/** Border alpha multiplier at the bottom edge (top stays 1): light-from-above highlight. */
	public ShapeElement borderFade(float bottomAlpha) {
		this.d[0] = bottomAlpha;
		return this;
	}

	/** Soft glow along the inside edge, using the secondary (border) color. */
	public ShapeElement innerGlow(float size, float strength) {
		this.d[1] = Math.max(size, 0f);
		this.d[2] = Math.max(strength, 0f);
		return this;
	}

	public ShapeElement secondary(int argb) {
		this.setSecondary(argb);
		return this;
	}

	private void setSecondary(int argb) {
		this.c[0] = Colors.red(argb) / 255f;
		this.c[1] = Colors.green(argb) / 255f;
		this.c[2] = Colors.blue(argb) / 255f;
		this.c[3] = Colors.alpha(argb) / 255f;
	}

	/** Turns the shape into a soft shadow/glow with the given blur radius. */
	public ShapeElement soft(float blur) {
		this.a[3] = Math.max(blur, 0f);
		// The gaussian tail reaches ~3 sigma = 1.5 * blur.
		this.margin = Math.max(this.margin, this.a[3] * 1.6f);
		return this;
	}

	/** Grows the shape before blurring (CSS box-shadow spread). */
	public ShapeElement spread(float spread) {
		this.spread = spread;
		return this;
	}

	/** 0 = normal over-compositing, 1 = purely additive light. */
	public ShapeElement additive(float amount) {
		this.d[3] = Math.max(0f, Math.min(amount, 1f));
		return this;
	}

	public ShapeElement texture(TextureSetup setup) {
		this.textureSetup = setup;
		return this;
	}

	/** Image UV rectangle (IMAGE pipeline only; stored in the secondary slot). */
	public ShapeElement uv(float u0, float v0, float u1, float v1) {
		this.c[0] = u0;
		this.c[1] = v0;
		this.c[2] = u1;
		this.c[3] = v1;
		return this;
	}

	// ------------------------------------------------------------------ arcs & lines (raw params)

	/** Raw parameter access for the ARC / LINE pipelines. */
	public ShapeElement params(float a0, float a1, float a2, float a3) {
		this.a[0] = a0;
		this.a[1] = a1;
		this.a[2] = a2;
		this.a[3] = a3;
		return this;
	}

	public ShapeElement paramsB(float b0, float b1, float b2, float b3) {
		this.b[0] = b0;
		this.b[1] = b1;
		this.b[2] = b2;
		this.b[3] = b3;
		return this;
	}

	/** Rotates the local frame around the box center (used for lines). */
	public ShapeElement rotation(float cos, float sin) {
		this.rotated = true;
		this.rotCos = cos;
		this.rotSin = sin;
		return this;
	}

	public ShapeElement margin(float margin) {
		this.margin = margin;
		return this;
	}

	// ------------------------------------------------------------------ submit

	public void submit() {
		GuiGraphics g = this.graphics;
		if (g == null) {
			return;
		}
		this.graphics = null;

		if (this.spread != 0f) {
			this.x -= this.spread;
			this.y -= this.spread;
			this.w = Math.max(this.w + this.spread * 2f, 0f);
			this.h = Math.max(this.h + this.spread * 2f, 0f);
			this.a[0] = this.w * 0.5f;
			this.a[1] = this.h * 0.5f;
			float max = Math.min(this.w, this.h) * 0.5f;
			for (int i = 0; i < 4; i++) {
				this.b[i] = clampRadius(this.b[i] + this.spread, max);
			}
		}

		float qx0 = this.x - this.margin;
		float qy0 = this.y - this.margin;
		float qx1 = this.x + this.w + this.margin;
		float qy1 = this.y + this.h + this.margin;

		// Transformed bounds in GUI space.
		float minX = Float.MAX_VALUE;
		float minY = Float.MAX_VALUE;
		float maxX = -Float.MAX_VALUE;
		float maxY = -Float.MAX_VALUE;
		for (int i = 0; i < 4; i++) {
			float lx = (i == 0 || i == 3) ? qx0 : qx1;
			float ly = (i < 2) ? qy0 : qy1;
			float px = this.localToPoseX(lx, ly);
			float py = this.localToPoseY(lx, ly);
			float tx = this.pose.m00 * px + this.pose.m10 * py + this.pose.m20;
			float ty = this.pose.m01 * px + this.pose.m11 * py + this.pose.m21;
			minX = Math.min(minX, tx);
			minY = Math.min(minY, ty);
			maxX = Math.max(maxX, tx);
			maxY = Math.max(maxY, ty);
		}

		float scale = Minecraft.getInstance().getWindow().getGuiScale();
		// Cull against the soft clip region (framebuffer pixels).
		if (maxX * scale <= this.clipX0 || maxY * scale <= this.clipY0 || minX * scale >= this.clipX1 || minY * scale >= this.clipY1) {
			return;
		}
		// Bounds drive vanilla's layering; tighten them to the clip region.
		minX = Math.max(minX, this.clipX0 / scale);
		minY = Math.max(minY, this.clipY0 / scale);
		maxX = Math.min(maxX, this.clipX1 / scale);
		maxY = Math.min(maxY, this.clipY1 / scale);

		int bx0 = (int) Math.floor(minX);
		int by0 = (int) Math.floor(minY);
		int bx1 = (int) Math.ceil(maxX);
		int by1 = (int) Math.ceil(maxY);
		if (bx1 <= bx0 || by1 <= by0) {
			return;
		}
		ScreenRectangle rect = new ScreenRectangle(bx0, by0, bx1 - bx0, by1 - by0);
		this.bounds = this.scissor != null ? this.scissor.intersection(rect) : rect;
		if (this.bounds == null) {
			return;
		}
		g.guiRenderState.submitGuiElement(this);
	}

	private float centerX() {
		return this.x + this.w * 0.5f;
	}

	private float centerY() {
		return this.y + this.h * 0.5f;
	}

	/** Maps a point of the (possibly rotated) local box into pose space. */
	private float localToPoseX(float lx, float ly) {
		if (!this.rotated) {
			return lx;
		}
		float cx = this.centerX();
		float cy = this.centerY();
		return cx + (lx - cx) * this.rotCos - (ly - cy) * this.rotSin;
	}

	private float localToPoseY(float lx, float ly) {
		if (!this.rotated) {
			return ly;
		}
		float cx = this.centerX();
		float cy = this.centerY();
		return cy + (lx - cx) * this.rotSin + (ly - cy) * this.rotCos;
	}

	// ------------------------------------------------------------------ GuiElementRenderState

	@Override
	public void buildVertices(VertexConsumer consumer) {
		if (!(consumer instanceof BufferBuilderAccessor access)) {
			return;
		}
		float qx0 = this.x - this.margin;
		float qy0 = this.y - this.margin;
		float qx1 = this.x + this.w + this.margin;
		float qy1 = this.y + this.h + this.margin;
		this.vertex(consumer, access, qx0, qy0, this.cTL);
		this.vertex(consumer, access, qx0, qy1, this.cBL);
		this.vertex(consumer, access, qx1, qy1, this.cBR);
		this.vertex(consumer, access, qx1, qy0, this.cTR);
	}

	private void vertex(VertexConsumer consumer, BufferBuilderAccessor access, float lx, float ly, int color) {
		float px = this.localToPoseX(lx, ly);
		float py = this.localToPoseY(lx, ly);
		float tx = this.pose.m00 * px + this.pose.m10 * py + this.pose.m20;
		float ty = this.pose.m01 * px + this.pose.m11 * py + this.pose.m21;
		consumer.addVertex(tx, ty, this.feather).setColor(color).setUv(lx - this.centerX(), ly - this.centerY());
		put(access.xeys$beginElement(XeysVertexFormats.PARAMS_A), this.a);
		put(access.xeys$beginElement(XeysVertexFormats.PARAMS_B), this.b);
		put(access.xeys$beginElement(XeysVertexFormats.PARAMS_C), this.c);
		put(access.xeys$beginElement(XeysVertexFormats.PARAMS_D), this.d);
		long p = access.xeys$beginElement(XeysVertexFormats.CLIP);
		if (p != -1L) {
			MemoryUtil.memPutFloat(p, this.clipX0);
			MemoryUtil.memPutFloat(p + 4L, this.clipY0);
			MemoryUtil.memPutFloat(p + 8L, this.clipX1);
			MemoryUtil.memPutFloat(p + 12L, this.clipY1);
		}
	}

	private static void put(long p, float[] v) {
		if (p != -1L) {
			MemoryUtil.memPutFloat(p, v[0]);
			MemoryUtil.memPutFloat(p + 4L, v[1]);
			MemoryUtil.memPutFloat(p + 8L, v[2]);
			MemoryUtil.memPutFloat(p + 12L, v[3]);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return this.pipeline;
	}

	@Override
	public TextureSetup textureSetup() {
		return this.textureSetup;
	}

	@Override
	public @Nullable ScreenRectangle scissorArea() {
		return this.scissor;
	}

	@Override
	public @Nullable ScreenRectangle bounds() {
		return this.bounds;
	}
}
