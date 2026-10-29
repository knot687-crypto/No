package dev.xeys.visuals.render.blur;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.textures.GpuSampler;
import dev.xeys.visuals.render.RenderFrame;
import dev.xeys.visuals.render.XeysPipelines;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.render.TextureSetup;
import org.jspecify.annotations.Nullable;

/**
 * Frosted-glass backdrop: a dual-Kawase blur of everything rendered before the GUI flush (the world, or
 * an immediate-mode menu background). Only computed on frames where a glass element was submitted.
 * Glass samples the half-resolution result with bilinear filtering, so the cost is independent of how
 * many glass panels are on screen.
 */
public final class SceneBlur {
	private static final BlurChain CHAIN = new BlurChain("XEYS scene blur");
	private static TextureSetup textureSetup = TextureSetup.noTexture();
	/** Number of downsample levels; blur radius roughly doubles per level. */
	private static int levels = 4;
	private static float offset = 1.6f;
	private static boolean enabled = true;

	private SceneBlur() {
	}

	public static void configure(int newLevels, float newOffset, boolean on) {
		levels = Math.max(1, Math.min(newLevels, BlurChain.MAX_LEVELS));
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
		if (CHAIN.ensure(main.width, main.height) || textureSetup == TextureSetup.noTexture()) {
			textureSetup = TextureSetup.singleTexture(CHAIN.result(), sampler());
		}
		RenderFrame.requestSceneBlur();
		return textureSetup;
	}

	public static GpuSampler sampler() {
		return BlurChain.sampler();
	}

	public static void release() {
		textureSetup = TextureSetup.noTexture();
		CHAIN.release();
	}

	/** Runs the blur chain. Render thread, outside any render pass. */
	public static void process() {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (!CHAIN.ready(main.width, main.height)) {
			return;
		}
		CHAIN.run(main.getColorTextureView(), levels, offset, null, 0f);
	}
}
