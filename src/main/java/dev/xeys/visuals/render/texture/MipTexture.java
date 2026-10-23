package dev.xeys.visuals.render.texture;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import org.jspecify.annotations.Nullable;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

/**
 * An RGBA image decoded (PNG or JPEG) with a full CPU-generated mip chain, ready to upload. Decoding and
 * mip generation happen off the render thread; {@link #upload} must run on it.
 */
public final class MipTexture implements AutoCloseable {
	private final String label;
	private final int width;
	private final int height;
	private final ByteBuffer[] levels;

	private MipTexture(String label, int width, int height, ByteBuffer[] levels) {
		this.label = label;
		this.width = width;
		this.height = height;
		this.levels = levels;
	}

	public static MipTexture decode(String label, byte[] encoded, int maxLevels) throws IOException {
		ByteBuffer src = MemoryUtil.memAlloc(encoded.length);
		ByteBuffer pixels;
		int w;
		int h;
		try (MemoryStack stack = MemoryStack.stackPush()) {
			src.put(encoded).flip();
			IntBuffer pw = stack.mallocInt(1);
			IntBuffer ph = stack.mallocInt(1);
			IntBuffer pc = stack.mallocInt(1);
			pixels = STBImage.stbi_load_from_memory(src, pw, ph, pc, 4);
			if (pixels == null) {
				throw new IOException("Could not decode " + label + ": " + STBImage.stbi_failure_reason());
			}
			w = pw.get(0);
			h = ph.get(0);
		} finally {
			MemoryUtil.memFree(src);
		}

		int count = 1;
		while (count < maxLevels && (w >> count) >= 1 && (h >> count) >= 1) {
			count++;
		}
		ByteBuffer[] levels = new ByteBuffer[count];
		// Copy level 0 out of stb's allocation so every level is freed the same way.
		levels[0] = MemoryUtil.memAlloc(w * h * 4);
		MemoryUtil.memCopy(pixels, levels[0]);
		STBImage.stbi_image_free(pixels);
		for (int i = 1; i < count; i++) {
			levels[i] = downsample(levels[i - 1], Math.max(1, w >> (i - 1)), Math.max(1, h >> (i - 1)));
		}
		return new MipTexture(label, w, h, levels);
	}

	/** 2x2 box filter. Averaged in gamma space, which is what GL mip generation does for RGBA8. */
	private static ByteBuffer downsample(ByteBuffer src, int sw, int sh) {
		int dw = Math.max(1, sw >> 1);
		int dh = Math.max(1, sh >> 1);
		ByteBuffer dst = MemoryUtil.memAlloc(dw * dh * 4);
		long s = MemoryUtil.memAddress(src);
		long d = MemoryUtil.memAddress(dst);
		for (int y = 0; y < dh; y++) {
			int y0 = Math.min(y * 2, sh - 1);
			int y1 = Math.min(y * 2 + 1, sh - 1);
			for (int x = 0; x < dw; x++) {
				int x0 = Math.min(x * 2, sw - 1);
				int x1 = Math.min(x * 2 + 1, sw - 1);
				long a = s + ((long) y0 * sw + x0) * 4L;
				long b = s + ((long) y0 * sw + x1) * 4L;
				long c = s + ((long) y1 * sw + x0) * 4L;
				long e = s + ((long) y1 * sw + x1) * 4L;
				long o = d + ((long) y * dw + x) * 4L;
				for (int ch = 0; ch < 4; ch++) {
					int sum = (MemoryUtil.memGetByte(a + ch) & 0xFF) + (MemoryUtil.memGetByte(b + ch) & 0xFF)
						+ (MemoryUtil.memGetByte(c + ch) & 0xFF) + (MemoryUtil.memGetByte(e + ch) & 0xFF);
					MemoryUtil.memPutByte(o + ch, (byte) ((sum + 2) >> 2));
				}
			}
		}
		return dst;
	}

	public int width() {
		return this.width;
	}

	public int height() {
		return this.height;
	}

	/** Uploads to a new GPU texture and frees the CPU copy. Render thread. */
	public Uploaded upload() {
		GpuDevice device = RenderSystem.getDevice();
		GpuTexture texture = device.createTexture(() -> this.label, GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
			TextureFormat.RGBA8, this.width, this.height, 1, this.levels.length);
		CommandEncoder encoder = device.createCommandEncoder();
		for (int i = 0; i < this.levels.length; i++) {
			int w = Math.max(1, this.width >> i);
			int h = Math.max(1, this.height >> i);
			encoder.writeToTexture(texture, this.levels[i], NativeImage.Format.RGBA, i, 0, 0, 0, w, h);
		}
		this.close();
		return new Uploaded(texture, device.createTextureView(texture));
	}

	@Override
	public void close() {
		for (int i = 0; i < this.levels.length; i++) {
			if (this.levels[i] != null) {
				MemoryUtil.memFree(this.levels[i]);
				this.levels[i] = null;
			}
		}
	}

	public record Uploaded(GpuTexture texture, GpuTextureView view) implements AutoCloseable {
		@Override
		public void close() {
			this.view.close();
			this.texture.close();
		}

		public static void closeQuietly(@Nullable Uploaded uploaded) {
			if (uploaded != null) {
				uploaded.close();
			}
		}
	}
}
