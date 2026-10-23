package dev.xeys.visuals.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.nio.ByteBuffer;
import org.jspecify.annotations.Nullable;

/**
 * A single-level RGBA texture filled from the CPU (generated icons, pack icons). The GPU texture is
 * kept and rewritten in place while the size stays the same. Render thread only.
 */
public final class CpuTexture implements AutoCloseable {
	private final String label;
	private @Nullable GpuTexture texture;
	private @Nullable GpuTextureView view;
	private int width;
	private int height;

	public CpuTexture(String label) {
		this.label = label;
	}

	private GpuTexture ensure(int w, int h) {
		if (this.texture == null || w != this.width || h != this.height) {
			this.close();
			GpuDevice device = RenderSystem.getDevice();
			this.texture = device.createTexture(() -> this.label, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
				TextureFormat.RGBA8, w, h, 1, 1);
			this.view = device.createTextureView(this.texture);
			this.width = w;
			this.height = h;
		}
		return this.texture;
	}

	/** Uploads straight-alpha RGBA bytes ({@code w * h * 4}, starting at the buffer's position 0). */
	public void upload(ByteBuffer rgba, int w, int h) {
		GpuTexture tex = this.ensure(w, h);
		RenderSystem.getDevice().createCommandEncoder().writeToTexture(tex, rgba, NativeImage.Format.RGBA, 0, 0, 0, 0, w, h);
	}

	public void upload(NativeImage image) {
		GpuTexture tex = this.ensure(image.getWidth(), image.getHeight());
		RenderSystem.getDevice().createCommandEncoder().writeToTexture(tex, image);
	}

	public @Nullable GpuTextureView view() {
		return this.view;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	@Override
	public void close() {
		if (this.view != null) {
			this.view.close();
			this.view = null;
		}
		if (this.texture != null) {
			this.texture.close();
			this.texture = null;
		}
		this.width = 0;
		this.height = 0;
	}
}
