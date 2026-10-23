package dev.xeys.visuals.ui.menu;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.AddressMode;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.xeys.visuals.Xeys;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.texture.MipTexture;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.concurrent.CompletableFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jspecify.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

/**
 * GPU side of the main-menu sky: owns the moon textures and draws {@link MoonScene} in a single
 * full-screen pass straight into the main render target, before the deferred GUI is flushed (so glass
 * panels on the menu blur the moon and stars behind them).
 */
public final class MoonSky {
	private static final Identifier ALBEDO = Xeys.id("textures/menu/moon_albedo.jpg");
	private static final Identifier NORMAL = Xeys.id("textures/menu/moon_normal.jpg");
	/** 5 vec4 + 2 x vec4[3]. */
	private static final int UBO_SIZE = 16 * 5 + 16 * 6;
	private static final UniformSlots CONFIG = new UniformSlots("XEYS moon config", UBO_SIZE, 1);

	private static MipTexture.@Nullable Uploaded albedo;
	private static MipTexture.@Nullable CpuImage albedoCpu;
	private static MipTexture.@Nullable Uploaded normal;
	private static @Nullable GpuSampler sampler;
	private static @Nullable GpuTexture placeholder;
	private static @Nullable GpuTextureView placeholderView;
	private static @Nullable GpuTexture skyCache;
	private static @Nullable GpuTextureView skyCacheView;
	private static int skyWidth;
	private static int skyHeight;
	private static @Nullable GpuTexture frameCache;
	private static @Nullable GpuTextureView frameCacheView;
	private static double frameCacheTime = -1.0;

	private MoonSky() {
	}

	public static boolean texturesReady() {
		return albedo != null && normal != null;
	}

	/** A 512-wide copy of the moon surface for CPU rendering (HUD moon icons), or null before loading. */
	public static MipTexture.@Nullable CpuImage albedoCpu() {
		return albedoCpu;
	}

	// ------------------------------------------------------------------ loading

	public static PreparableReloadListener reloader() {
		return (state, prepareExecutor, barrier, applyExecutor) -> {
			ResourceManager manager = state.resourceManager();
			CompletableFuture<MipTexture> a = CompletableFuture.supplyAsync(() -> decode(manager, ALBEDO), prepareExecutor);
			CompletableFuture<MipTexture> n = CompletableFuture.supplyAsync(() -> decode(manager, NORMAL), prepareExecutor);
			return a.thenCombine(n, (x, y) -> new MipTexture[] {x, y})
				.thenCompose(barrier::wait)
				.thenAcceptAsync(MoonSky::apply, applyExecutor);
		};
	}

	private static @Nullable MipTexture decode(ResourceManager manager, Identifier id) {
		try (InputStream in = manager.getResourceOrThrow(id).open()) {
			return MipTexture.decode(id.toString(), in.readAllBytes(), 16);
		} catch (IOException | RuntimeException e) {
			Xeys.LOG.error("Failed to load menu texture {}", id, e);
			return null;
		}
	}

	private static void apply(MipTexture[] decoded) {
		MipTexture.Uploaded.closeQuietly(albedo);
		MipTexture.Uploaded.closeQuietly(normal);
		albedo = null;
		normal = null;
		if (decoded[0] != null && decoded[1] != null) {
			albedoCpu = decoded[0].copyLevel(512);
			albedo = decoded[0].upload();
			normal = decoded[1].upload();
		} else {
			for (MipTexture t : decoded) {
				if (t != null) {
					t.close();
				}
			}
		}
	}

	private static GpuSampler sampler() {
		if (sampler == null) {
			GpuDevice device = RenderSystem.getDevice();
			int aniso = Math.max(1, Math.min(Integer.getInteger("xeys.moonAniso", 8), device.getMaxSupportedAnisotropy()));
			// Longitude wraps, latitude clamps; anisotropy keeps the limb crisp.
			sampler = device.createSampler(AddressMode.REPEAT, AddressMode.CLAMP_TO_EDGE, FilterMode.LINEAR, FilterMode.LINEAR, aniso,
				OptionalDouble.empty());
		}
		return sampler;
	}

	private static GpuTextureView placeholder() {
		if (placeholderView == null) {
			GpuDevice device = RenderSystem.getDevice();
			placeholder = device.createTexture(() -> "XEYS moon placeholder", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
				TextureFormat.RGBA8, 1, 1, 1, 1);
			ByteBuffer px = MemoryUtil.memAlloc(4);
			try {
				px.put((byte) 128).put((byte) 128).put((byte) 255).put((byte) 255).flip();
				device.createCommandEncoder().writeToTexture(placeholder, px, NativeImage.Format.RGBA, 0, 0, 0, 0, 1, 1);
			} finally {
				MemoryUtil.memFree(px);
			}
			placeholderView = device.createTextureView(placeholder);
		}
		return placeholderView;
	}

	/** (Re)renders the static sky into the half-resolution cache when the window size changed. */
	private static GpuTextureView skyCache(int width, int height) {
		if (skyCacheView != null && width == skyWidth && height == skyHeight) {
			return skyCacheView;
		}
		if (skyCacheView != null) {
			skyCacheView.close();
			skyCache.close();
		}
		GpuDevice device = RenderSystem.getDevice();
		skyWidth = width;
		skyHeight = height;
		skyCache = device.createTexture(() -> "XEYS sky cache", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
			TextureFormat.RGBA8, Math.max(1, width / 2), Math.max(1, height / 2), 1, 1);
		skyCacheView = device.createTextureView(skyCache);
		try (RenderPass pass = device.createCommandEncoder().createRenderPass(() -> "XEYS sky cache", skyCacheView, OptionalInt.empty())) {
			pass.setPipeline(XeysPipelines.MOON_NEBULA);
			pass.setUniform("MoonConfig", CONFIG.slot(0));
			pass.draw(0, 3);
		}
		return skyCacheView;
	}

	// ------------------------------------------------------------------ drawing

	/** Draws the sky immediately into the main render target. Returns false if unavailable. */
	public static boolean draw(MoonScene scene) {
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		return render(scene, main.getColorTextureView(), main.width, main.height);
	}

	/**
	 * Draws the sky through a cache refreshed at most {@code hz} times per second. For backdrops that
	 * sit under vanilla's heavy menu blur, where per-frame detail is invisible: the cost drops to one
	 * copy per frame.
	 */
	public static boolean drawCached(MoonScene scene, float hz) {
		if (!XeysPipelines.isUsable(XeysPipelines.BLIT)) {
			return draw(scene);
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		GpuDevice device = RenderSystem.getDevice();
		boolean resized = frameCache == null || frameCache.getWidth(0) != main.width || frameCache.getHeight(0) != main.height;
		if (resized) {
			if (frameCacheView != null) {
				frameCacheView.close();
				frameCache.close();
			}
			frameCache = device.createTexture(() -> "XEYS backdrop cache", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_RENDER_ATTACHMENT,
				TextureFormat.RGBA8, main.width, main.height, 1, 1);
			frameCacheView = device.createTextureView(frameCache);
			frameCacheTime = -1.0;
		}
		double now = dev.xeys.visuals.anim.Clock.time();
		if (frameCacheTime < 0.0 || now - frameCacheTime >= 1.0 / hz) {
			if (!render(scene, frameCacheView, main.width, main.height)) {
				return false;
			}
			frameCacheTime = now;
		}
		try (RenderPass pass = device.createCommandEncoder().createRenderPass(() -> "XEYS backdrop blit", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(XeysPipelines.BLIT);
			pass.bindTexture("InSampler", frameCacheView, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));
			pass.draw(0, 3);
		}
		return true;
	}

	private static boolean render(MoonScene scene, GpuTextureView target, int width, int height) {
		if (!XeysPipelines.isUsable(XeysPipelines.MOON_SKY) || !XeysPipelines.isUsable(XeysPipelines.MOON_NEBULA)) {
			return false;
		}
		boolean ready = texturesReady();
		float moonVisibility = ready ? scene.moonVisibility() : 0f;

		CONFIG.write(1, (slot, b) -> {
			b.putVec4(width, height, scene.time(), scene.brightness());
			b.putVec4(scene.moonX(), scene.moonY(), scene.moonRadius(), scene.rotation());
			b.putVec4(scene.sunX(), scene.sunY(), scene.sunZ(), scene.tilt());
			b.putVec4(scene.parallaxX(), scene.parallaxY(), scene.starBrightness(), scene.glow());
			b.putVec4(scene.relief(), moonVisibility, scene.exposure(), scene.nebula());
			for (int i = 0; i < MoonScene.METEORS; i++) {
				b.putVec4(scene.meteorX(i), scene.meteorY(i), scene.meteorAngle(i), scene.meteorAlpha(i));
			}
			for (int i = 0; i < MoonScene.METEORS; i++) {
				b.putVec4(scene.meteorLength(i), scene.meteorWidth(i), 0f, 0f);
			}
		});

		GpuTextureView albedoView = ready ? albedo.view() : placeholder();
		GpuTextureView normalView = ready ? normal.view() : placeholder();
		GpuTextureView sky = skyCache(width, height);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder()
				.createRenderPass(() -> "XEYS moon sky", target, OptionalInt.empty())) {
			pass.setPipeline(XeysPipelines.MOON_SKY);
			pass.bindTexture("MoonAlbedo", albedoView, sampler());
			pass.bindTexture("MoonNormal", normalView, sampler());
			pass.bindTexture("SkyCache", sky, RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR));
			pass.setUniform("MoonConfig", CONFIG.slot(0));
			pass.draw(0, 3);
		}
		return true;
	}
}
