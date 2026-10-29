package dev.xeys.visuals.world;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import dev.xeys.visuals.Xeys;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import org.jspecify.annotations.Nullable;

/**
 * Recolored enchantment glint textures. The glint's brightness pattern is kept and tinted with one
 * color; the recolored copies replace vanilla's textures under the same ids until {@link #restore()}.
 */
public final class GlintTextures {
	private static final Identifier[] IDS = {ItemRenderer.ENCHANTED_GLINT_ITEM, ItemRenderer.ENCHANTED_GLINT_ARMOR};
	private static final @Nullable NativeImage[] ORIGINALS = new NativeImage[IDS.length];
	private static final @Nullable GlintTexture[] TEXTURES = new GlintTexture[IDS.length];
	/** The color currently applied, or 0 when vanilla's textures are in place. */
	private static int applied;

	private GlintTextures() {
	}

	public static int applied() {
		return applied;
	}

	/** Tints both glints with {@code argb}. Render thread. */
	public static void apply(int argb) {
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		float tr = ((argb >> 16) & 0xFF) / 255f;
		float tg = ((argb >> 8) & 0xFF) / 255f;
		float tb = (argb & 0xFF) / 255f;
		for (int i = 0; i < IDS.length; i++) {
			NativeImage src = original(i);
			if (src == null) {
				continue;
			}
			GlintTexture tex = TEXTURES[i];
			if (tex == null || tex.getPixels() == null || tex.getPixels().getWidth() != src.getWidth() || tex.getPixels().getHeight() != src.getHeight()) {
				int index = i;
				tex = new GlintTexture(() -> "XEYS glint " + index, src.getWidth(), src.getHeight());
				TEXTURES[i] = tex;
				textures.register(IDS[i], tex);
			}
			NativeImage out = tex.getPixels();
			for (int y = 0; y < src.getHeight(); y++) {
				for (int x = 0; x < src.getWidth(); x++) {
					int p = src.getPixel(x, y);
					// The glint's brightness is its strongest channel; keep it, change the hue.
					float l = Math.max(((p >> 16) & 0xFF), Math.max((p >> 8) & 0xFF, p & 0xFF)) / 255f;
					int r = Math.min(255, Math.round(tr * l * 255f));
					int g = Math.min(255, Math.round(tg * l * 255f));
					int b = Math.min(255, Math.round(tb * l * 255f));
					out.setPixel(x, y, (p & 0xFF000000) | (r << 16) | (g << 8) | b);
				}
			}
			tex.upload();
		}
		applied = argb | 0xFF000000;
	}

	/** Puts vanilla's glints back; they reload from the resource packs on next use. */
	public static void restore() {
		if (applied == 0) {
			return;
		}
		TextureManager textures = Minecraft.getInstance().getTextureManager();
		for (int i = 0; i < IDS.length; i++) {
			if (TEXTURES[i] != null) {
				textures.release(IDS[i]);
				TEXTURES[i] = null;
			}
		}
		applied = 0;
	}

	/** Resource packs changed: forget the cached originals and re-tint from the new ones. */
	public static void onReload() {
		for (int i = 0; i < IDS.length; i++) {
			if (ORIGINALS[i] != null) {
				ORIGINALS[i].close();
				ORIGINALS[i] = null;
			}
		}
		int color = applied;
		if (color != 0) {
			restore();
			apply(color);
		}
	}

	private static @Nullable NativeImage original(int i) {
		if (ORIGINALS[i] != null) {
			return ORIGINALS[i];
		}
		Resource resource = Minecraft.getInstance().getResourceManager().getResource(IDS[i]).orElse(null);
		if (resource == null) {
			return null;
		}
		try (InputStream in = resource.open()) {
			ORIGINALS[i] = NativeImage.read(in);
		} catch (IOException e) {
			Xeys.LOG.warn("Could not read glint texture {}", IDS[i], e);
		}
		return ORIGINALS[i];
	}

	/** A dynamic texture sampled like vanilla's glint: repeating and smoothly filtered. */
	private static final class GlintTexture extends DynamicTexture {
		GlintTexture(Supplier<String> label, int width, int height) {
			super(label, width, height, true);
			this.sampler = RenderSystem.getSamplerCache().getRepeat(FilterMode.LINEAR);
		}
	}
}
