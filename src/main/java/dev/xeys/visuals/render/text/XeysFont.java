package dev.xeys.visuals.render.text;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.gui.render.TextureSetup;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/** A baked SDF font resident on the GPU, plus metric queries. */
public final class XeysFont {
	private final String name;
	private @Nullable FontAtlas atlas;
	private @Nullable GpuTexture texture;
	private @Nullable GpuTextureView view;
	private TextureSetup textureSetup = TextureSetup.noTexture();

	XeysFont(String name) {
		this.name = name;
	}

	public String name() {
		return this.name;
	}

	public boolean ready() {
		return this.atlas != null && this.view != null;
	}

	@Nullable FontAtlas atlas() {
		return this.atlas;
	}

	@Nullable GpuTextureView view() {
		return this.view;
	}

	TextureSetup textureSetup() {
		return this.textureSetup;
	}

	static GpuSampler sampler() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR, true);
	}

	/** Uploads a baked atlas. Render thread. */
	void upload(FontAtlas baked) {
		this.release();
		var device = RenderSystem.getDevice();
		GpuTexture tex = device.createTexture(() -> "XEYS font " + this.name, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
			TextureFormat.RED8, baked.width, baked.height, 1, baked.pixels.length);
		CommandEncoder encoder = device.createCommandEncoder();
		for (int level = 0; level < baked.pixels.length; level++) {
			byte[] data = baked.pixels[level];
			int w = Math.max(1, baked.width >> level);
			int h = Math.max(1, baked.height >> level);
			ByteBuffer buf = MemoryUtil.memAlloc(data.length);
			try {
				buf.put(data).flip();
				encoder.writeToTexture(tex, buf, NativeImage.Format.LUMINANCE, level, 0, 0, 0, w, h);
			} finally {
				MemoryUtil.memFree(buf);
			}
		}
		this.texture = tex;
		this.view = device.createTextureView(tex);
		this.textureSetup = TextureSetup.singleTexture(this.view, sampler());
		this.atlas = baked;
	}

	void release() {
		this.textureSetup = TextureSetup.noTexture();
		if (this.view != null) {
			this.view.close();
			this.view = null;
		}
		if (this.texture != null) {
			this.texture.close();
			this.texture = null;
		}
	}

	// ------------------------------------------------------------------ metrics (em units scaled by size)

	public float ascent(float size) {
		return this.atlas == null ? size * 0.8f : this.atlas.ascent * size;
	}

	public float descent(float size) {
		return this.atlas == null ? -size * 0.2f : this.atlas.descent * size;
	}

	public float lineHeight(float size) {
		return this.atlas == null ? size * 1.2f : (this.atlas.ascent - this.atlas.descent + this.atlas.lineGap) * size;
	}

	public float capHeight(float size) {
		return this.atlas == null ? size * 0.7f : this.atlas.capHeight * size;
	}

	public float xHeight(float size) {
		return this.atlas == null ? size * 0.5f : this.atlas.xHeight * size;
	}

	public float width(CharSequence text, float size) {
		return this.width(text, 0, text.length(), size, 0f);
	}

	public float width(CharSequence text, float size, float tracking) {
		return this.width(text, 0, text.length(), size, tracking);
	}

	/** Advance width of a range, including kerning and tracking (em units of tracking per glyph gap). */
	public float width(CharSequence text, int start, int end, float size, float tracking) {
		FontAtlas a = this.atlas;
		if (a == null) {
			return (end - start) * size * 0.55f;
		}
		float x = 0f;
		int prev = -1;
		int count = 0;
		for (int i = start; i < end; i++) {
			int cp = text.charAt(i);
			FontAtlas.Glyph g = a.glyph(cp);
			if (g == null) {
				g = a.glyph('?');
				if (g == null) {
					continue;
				}
				cp = '?';
			}
			if (prev >= 0) {
				x += a.kern(prev, cp);
			}
			x += g.advance();
			prev = cp;
			count++;
		}
		if (count > 1) {
			x += tracking * (count - 1);
		}
		return x * size;
	}

	public float width(char c, float size) {
		FontAtlas a = this.atlas;
		if (a == null) {
			return size * 0.55f;
		}
		FontAtlas.Glyph g = a.glyph(c);
		return g == null ? 0f : g.advance() * size;
	}

	/** Index of the last character that fits in {@code maxWidth} (for truncation / caret placement). */
	public int fit(CharSequence text, float size, float maxWidth) {
		FontAtlas a = this.atlas;
		if (a == null) {
			return Math.min(text.length(), (int) (maxWidth / (size * 0.55f)));
		}
		float x = 0f;
		int prev = -1;
		for (int i = 0; i < text.length(); i++) {
			int cp = text.charAt(i);
			FontAtlas.Glyph g = a.glyph(cp);
			if (g == null) {
				continue;
			}
			float adv = (prev >= 0 ? a.kern(prev, cp) : 0f) + g.advance();
			if ((x + adv) * size > maxWidth) {
				return i;
			}
			x += adv;
			prev = cp;
		}
		return text.length();
	}
}
