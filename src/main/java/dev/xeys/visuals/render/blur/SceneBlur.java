package dev.xeys.visuals.render.blur;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.xeys.visuals.render.RenderFrame;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;

/**
 * Frosted-glass backdrop: a dual-Kawase blur of everything rendered before the GUI flush (the world, or
 * an immediate-mode menu background). Only computed on frames where a glass element was submitted.
 *
 * <p>Level {@code i} is a texture at {@code 1/2^i} resolution. The chain downsamples to the deepest
 * level then upsamples back to level 1, which glass samples with bilinear filtering. Cost is a handful of
 * tiny passes, independent of how many glass panels are on screen.
 */
public final class SceneBlur {
	private static final int MAX_LEVELS = 6;
	private static final GpuTexture[] TEXTURES = new GpuTexture[MAX_LEVELS + 1];
	private static final GpuTextureView[] VIEWS = new GpuTextureView[MAX_LEVELS + 1];
	private static final UniformSlots UNIFORMS = new UniformSlots("XEYS blur config", 16, MAX_LEVELS * 2);
	private static TextureSetup textureSetup = TextureSetup.noTexture();
	private static int width;
	private static int height;
	/** Number of downsample levels; blur radius roughly doubles per level. */
	private static int levels = 4;
	private static float offset = 1.6f;
	private static boolean enabled = true;

	private SceneBlur() {
	}

	public static void configure(int newLevels, float newOffset, boolean on) {
		levels = Math.max(1, Math.min(newLevels, MAX_LEVELS));
		offset = Math.max(0.25f, newOffset);
		enabled = on;
	}

	public static boolean isEnabled() {
		return enabled && XeysPipelines.isUsable(XeysPipelines.BLUR_DOWN) && XeysPipelines.isUsable(XeysPipelines.BLUR_UP)
			&& XeysPipelines.isUsable(XeysPipelines.GLASS);
	}

	/**
	 * Ensures the blur targets exist for this frame and schedules the blur. Returns the texture glass
	 * should sample, or null when glass is unavailable (callers fall back to a tinted fill).
	 */
	public static @Nullable TextureSetup acquire() {
		if (!isEnabled()) {
			return null;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		ensureTargets(main.width, main.height);
		RenderFrame.requestSceneBlur();
		return textureSetup;
	}

	public static GpuSampler sampler() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}

	private static void ensureTargets(int w, int h) {
		if (w == width && h == height && TEXTURES[1] != null && !TEXTURES[1].isClosed()) {
			return;
		}
		release();
		width = w;
		height = h;
		GpuDevice device = RenderSystem.getDevice();
		for (int i = 1; i <= MAX_LEVELS; i++) {
			int lw = Math.max(1, w >> i);
			int lh = Math.max(1, h >> i);
			int level = i;
			TEXTURES[i] = device.createTexture(() -> "XEYS blur level " + level,
				GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, lw, lh, 1, 1);
			VIEWS[i] = device.createTextureView(TEXTURES[i]);
		}
		textureSetup = TextureSetup.singleTexture(VIEWS[1], sampler());
	}

	public static void release() {
		textureSetup = TextureSetup.noTexture();
		for (int i = 1; i <= MAX_LEVELS; i++) {
			if (VIEWS[i] != null) {
				VIEWS[i].close();
				VIEWS[i] = null;
			}
			if (TEXTURES[i] != null) {
				TEXTURES[i].close();
				TEXTURES[i] = null;
			}
		}
		width = 0;
		height = 0;
	}

	/** Runs the blur chain. Render thread, outside any render pass. */
	public static void process() {
		if (TEXTURES[1] == null || TEXTURES[1].isClosed()) {
			return;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (main.width != width || main.height != height) {
			return;
		}
		int n = Math.min(levels, MAX_LEVELS);
		// Slots 0..n-1: downsample from level i (source size), slots n..2n-2: upsample from level i+1.
		UNIFORMS.write(n * 2 - 1, (slot, b) -> {
			int srcLevel = slot < n ? slot : (n - (slot - n));
			int sw = Math.max(1, width >> srcLevel);
			int sh = Math.max(1, height >> srcLevel);
			b.putVec2(0.5f / sw, 0.5f / sh);
			b.putFloat(offset);
		});

		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		GpuTextureView source = main.getColorTextureView();
		for (int i = 1; i <= n; i++) {
			pass(encoder, XeysPipelines.BLUR_DOWN, source, VIEWS[i], i - 1);
			source = VIEWS[i];
		}
		for (int i = n - 1, slot = n; i >= 1; i--, slot++) {
			pass(encoder, XeysPipelines.BLUR_UP, VIEWS[i + 1], VIEWS[i], slot);
		}
	}

	private static void pass(CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView src, GpuTextureView dst, int slot) {
		try (RenderPass pass = encoder.createRenderPass(() -> "XEYS scene blur", dst, OptionalInt.empty())) {
			pass.setPipeline(pipeline);
			pass.bindTexture("InSampler", src, sampler());
			pass.setUniform("BlurConfig", UNIFORMS.slot(slot));
			pass.draw(0, 3);
		}
	}
}
