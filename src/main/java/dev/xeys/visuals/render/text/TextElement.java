package dev.xeys.visuals.render.text;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.mixin.BufferBuilderAccessor;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.XeysVertexFormats;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import org.joml.Matrix3x2f;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/** One run of SDF text (any length) as a single pooled render state. */
final class TextElement implements GuiElementRenderState {
	final Matrix3x2f pose = new Matrix3x2f();
	char[] chars = new char[64];
	int length;
	FontAtlas atlas;
	TextureSetup textureSetup = TextureSetup.noTexture();
	@Nullable ScreenRectangle scissor;
	@Nullable ScreenRectangle bounds;
	float x;
	float baseline;
	float size;
	float tracking;
	int color;
	float weight;
	float outline;
	float softness;
	float c0;
	float c1;
	float c2;
	float c3;
	float clipX0;
	float clipY0;
	float clipX1;
	float clipY1;
	float feather;

	void setText(CharSequence text, int start, int end) {
		int n = end - start;
		if (this.chars.length < n) {
			this.chars = new char[Math.max(n, this.chars.length * 2)];
		}
		for (int i = 0; i < n; i++) {
			this.chars[i] = text.charAt(start + i);
		}
		this.length = n;
	}

	@Override
	public void buildVertices(VertexConsumer consumer) {
		if (!(consumer instanceof BufferBuilderAccessor access)) {
			return;
		}
		FontAtlas a = this.atlas;
		float invW = 1f / a.width;
		float invH = 1f / a.height;
		float pen = this.x;
		int prev = -1;
		for (int i = 0; i < this.length; i++) {
			int cp = this.chars[i];
			FontAtlas.Glyph g = a.glyph(cp);
			if (g == null) {
				g = a.glyph('?');
				if (g == null) {
					continue;
				}
				cp = '?';
			}
			if (prev >= 0) {
				pen += a.kern(prev, cp) * this.size + this.tracking * this.size;
			}
			if (g.visible()) {
				float gx0 = pen + g.xOff() * this.size;
				float gy0 = this.baseline + g.yOff() * this.size;
				float gx1 = gx0 + g.w() * this.size;
				float gy1 = gy0 + g.h() * this.size;
				float u0 = g.atlasX() * invW;
				float v0 = g.atlasY() * invH;
				float u1 = (g.atlasX() + g.atlasW()) * invW;
				float v1 = (g.atlasY() + g.atlasH()) * invH;
				this.vertex(consumer, access, gx0, gy0, u0, v0);
				this.vertex(consumer, access, gx0, gy1, u0, v1);
				this.vertex(consumer, access, gx1, gy1, u1, v1);
				this.vertex(consumer, access, gx1, gy0, u1, v0);
			}
			pen += g.advance() * this.size;
			prev = cp;
		}
	}

	private void vertex(VertexConsumer consumer, BufferBuilderAccessor access, float lx, float ly, float u, float v) {
		float tx = this.pose.m00 * lx + this.pose.m10 * ly + this.pose.m20;
		float ty = this.pose.m01 * lx + this.pose.m11 * ly + this.pose.m21;
		consumer.addVertex(tx, ty, this.feather).setColor(this.color).setUv(u, v);
		long p = access.xeys$beginElement(XeysVertexFormats.PARAMS_A);
		if (p != -1L) {
			MemoryUtil.memPutFloat(p, this.weight);
			MemoryUtil.memPutFloat(p + 4L, this.outline);
			MemoryUtil.memPutFloat(p + 8L, this.softness);
			MemoryUtil.memPutFloat(p + 12L, this.atlas.emPx);
		}
		p = access.xeys$beginElement(XeysVertexFormats.PARAMS_C);
		if (p != -1L) {
			MemoryUtil.memPutFloat(p, this.c0);
			MemoryUtil.memPutFloat(p + 4L, this.c1);
			MemoryUtil.memPutFloat(p + 8L, this.c2);
			MemoryUtil.memPutFloat(p + 12L, this.c3);
		}
		p = access.xeys$beginElement(XeysVertexFormats.CLIP);
		if (p != -1L) {
			MemoryUtil.memPutFloat(p, this.clipX0);
			MemoryUtil.memPutFloat(p + 4L, this.clipY0);
			MemoryUtil.memPutFloat(p + 8L, this.clipX1);
			MemoryUtil.memPutFloat(p + 12L, this.clipY1);
		}
	}

	@Override
	public RenderPipeline pipeline() {
		return XeysPipelines.TEXT;
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
