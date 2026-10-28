package dev.xeys.visuals.render.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import org.jspecify.annotations.Nullable;

/**
 * Runs the combined screen-effects pass over the world image, after the world is drawn and before the
 * GUI (so the HUD and menus stay untouched). The world image is copied once, then the effects shader
 * writes the result back. Frames with no active effect cost nothing.
 */
public final class ScreenEffects {
	private static final int UBO_SIZE = 16 * 13;
	private static final UniformSlots UNIFORMS = new UniformSlots("XEYS screen fx", UBO_SIZE, 1);
	private static final ScreenFx FX = new ScreenFx();
	private static @Nullable GpuTexture copy;
	private static @Nullable GpuTextureView copyView;

	private ScreenEffects() {
	}

	/** Collects this frame's contributions and applies them. Render thread, outside any render pass. */
	public static void process() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || !XeysPipelines.isUsable(XeysPipelines.SCREEN_FX)) {
			return;
		}
		FX.reset();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Object[] contributors = Events.SCREEN_FX.snapshot();
		for (Object c : contributors) {
			((Events.ScreenEffect) c).contribute(FX, partial);
		}
		if (!FX.active()) {
			return;
		}
		RenderTarget main = mc.getMainRenderTarget();
		ensureCopy(main.width, main.height);
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToTexture(main.getColorTexture(), copy, 0, 0, 0, 0, 0, main.width, main.height);
		write(main.width, main.height);
		try (RenderPass pass = encoder.createRenderPass(() -> "XEYS screen fx", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(XeysPipelines.SCREEN_FX);
			pass.bindTexture("InSampler", copyView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.setUniform("ScreenFx", UNIFORMS.slot(0));
			pass.draw(0, 3);
		}
	}

	private static void ensureCopy(int w, int h) {
		if (copy != null && !copy.isClosed() && copy.getWidth(0) == w && copy.getHeight(0) == h) {
			return;
		}
		if (copyView != null) {
			copyView.close();
		}
		if (copy != null) {
			copy.close();
		}
		GpuDevice device = RenderSystem.getDevice();
		copy = device.createTexture(() -> "XEYS screen fx source", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
			TextureFormat.RGBA8, w, h, 1, 1);
		copyView = device.createTextureView(copy);
	}

	private static void write(int w, int h) {
		ScreenFx f = FX;
		UNIFORMS.write(1, (slot, b) -> {
			b.putVec4(w, h, Clock.time(3600.0), 0f);
			b.putVec4(f.vignette, f.vignetteRadius, f.vignetteSoftness, f.vignetteRoundness);
			putColor(b, f.vignetteColor, 0f);
			b.putVec4(f.grain, f.grainSize, f.grainSpeed, 0f);
			b.putVec4(f.chroma, f.chromaFalloff, 0f, 0f);
			b.putVec4(f.scanlines, f.scanSpacing, f.scanSpeed, 0f);
			b.putVec4(f.saturation, f.contrast, f.brightness, f.sharpen);
			putColor(b, f.tint, f.tintAmount);
			b.putVec4(f.mono, f.monoMode, 0f, 0f);
			putColor(b, f.edgeColor, f.edge);
			putColor(b, f.flashColor, f.flash);
			b.putVec4(f.pixelSize, f.letterbox, f.wave, f.waveSpeed);
			putColor(b, f.glowColor, f.glow);
		});
	}

	private static void putColor(com.mojang.blaze3d.buffers.Std140Builder b, int argb, float w) {
		b.putVec4(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, w);
	}
}
