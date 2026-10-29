package dev.xeys.visuals.render.blur;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import java.util.OptionalInt;
import org.jspecify.annotations.Nullable;

/**
 * A dual-Kawase blur chain: textures at 1/2, 1/4 ... of the source resolution and the passes between
 * them. The chain downsamples to the deepest level, then upsamples back to level 1, which holds the
 * result. Cost is a handful of tiny passes; the radius roughly doubles per level.
 */
public final class BlurChain {
	public static final int MAX_LEVELS = 6;

	private final String label;
	private final GpuTexture[] textures = new GpuTexture[MAX_LEVELS + 1];
	private final GpuTextureView[] views = new GpuTextureView[MAX_LEVELS + 1];
	private final UniformSlots uniforms;
	private int width;
	private int height;

	public BlurChain(String label) {
		this.label = label;
		this.uniforms = new UniformSlots(label + " config", 16, MAX_LEVELS * 2);
	}

	public static GpuSampler sampler() {
		return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
	}

	/** Whether the textures exist for a {@code w}x{@code h} source. */
	public boolean ready(int w, int h) {
		return w == this.width && h == this.height && this.textures[1] != null && !this.textures[1].isClosed();
	}

	/** Creates (or recreates) the level textures for a {@code w}x{@code h} source. Returns true when they changed. */
	public boolean ensure(int w, int h) {
		if (this.ready(w, h)) {
			return false;
		}
		this.release();
		this.width = w;
		this.height = h;
		GpuDevice device = RenderSystem.getDevice();
		for (int i = 1; i <= MAX_LEVELS; i++) {
			int lw = Math.max(1, w >> i);
			int lh = Math.max(1, h >> i);
			int level = i;
			this.textures[i] = device.createTexture(() -> this.label + " level " + level,
				GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT, TextureFormat.RGBA8, lw, lh, 1, 1);
			this.views[i] = device.createTextureView(this.textures[i]);
		}
		return true;
	}

	/** The blurred result (level 1, half resolution). Valid after {@link #ensure}. */
	public @Nullable GpuTextureView result() {
		return this.views[1];
	}

	public void release() {
		for (int i = 1; i <= MAX_LEVELS; i++) {
			if (this.views[i] != null) {
				this.views[i].close();
				this.views[i] = null;
			}
			if (this.textures[i] != null) {
				this.textures[i].close();
				this.textures[i] = null;
			}
		}
		this.width = 0;
		this.height = 0;
	}

	/**
	 * Blurs {@code source} (at the size given to {@link #ensure}) into level 1. {@code firstDown}, when not
	 * null, replaces the first downsample (e.g. a bloom threshold) and receives {@code param} in the
	 * config's fourth float. Render thread, outside any render pass.
	 */
	public void run(GpuTextureView source, int levels, float offset, @Nullable RenderPipeline firstDown, float param) {
		this.run(source, levels, offset, firstDown, param, XeysPipelines.BLUR_UP);
	}

	/**
	 * As {@link #run(GpuTextureView, int, float, RenderPipeline, float)} with a custom upsample: an
	 * additively blended one sums every level (a tight glow plus a wide halo, as bloom wants).
	 */
	public void run(GpuTextureView source, int levels, float offset, @Nullable RenderPipeline firstDown, float param, RenderPipeline up) {
		if (this.textures[1] == null || this.textures[1].isClosed()) {
			return;
		}
		int n = Math.max(1, Math.min(levels, MAX_LEVELS));
		// Slots 0..n-1: downsample from level i (source size), slots n..2n-2: upsample from level i+1.
		this.uniforms.write(n * 2 - 1, (slot, b) -> {
			int srcLevel = slot < n ? slot : (n - (slot - n));
			int sw = Math.max(1, this.width >> srcLevel);
			int sh = Math.max(1, this.height >> srcLevel);
			b.putVec2(0.5f / sw, 0.5f / sh);
			b.putFloat(offset);
			b.putFloat(param);
		});
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		GpuTextureView src = source;
		for (int i = 1; i <= n; i++) {
			RenderPipeline pipeline = i == 1 && firstDown != null ? firstDown : XeysPipelines.BLUR_DOWN;
			this.pass(encoder, pipeline, src, this.views[i], i - 1);
			src = this.views[i];
		}
		for (int i = n - 1, slot = n; i >= 1; i--, slot++) {
			this.pass(encoder, up, this.views[i + 1], this.views[i], slot);
		}
	}

	private void pass(CommandEncoder encoder, RenderPipeline pipeline, GpuTextureView src, GpuTextureView dst, int slot) {
		try (RenderPass pass = encoder.createRenderPass(() -> this.label, dst, OptionalInt.empty())) {
			pass.setPipeline(pipeline);
			pass.bindTexture("InSampler", src, sampler());
			pass.setUniform("BlurConfig", this.uniforms.slot(slot));
			pass.draw(0, 3);
		}
	}
}
