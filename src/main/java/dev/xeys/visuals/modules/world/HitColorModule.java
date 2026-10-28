package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.platform.NativeImage;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.mixin.OverlayTextureAccessor;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;

/** The color mobs and players flash when hurt, instead of vanilla's red. */
public final class HitColorModule extends Module implements Events.FrameStart {
	/** Vanilla's hurt tint (ARGB). */
	private static final int VANILLA = 0xB2FF0000;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFF3D5A).noAlpha().group("Hurt Tint"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Strength", 70f, 10f, 95f, 1f).unit("%").group("Hurt Tint"));
	private int applied;

	public HitColorModule() {
		super("hit_color", "Hit Color", Category.WORLD, Icons.DROPLET, "Change the tint entities flash when hurt");
	}

	@Override
	public void onFrameStart() {
		int argb = Colors.withAlpha(this.color.color(), this.opacity.value() / 100f);
		if (argb != this.applied) {
			write(argb);
			this.applied = argb;
		}
	}

	@Override
	protected void onDisable() {
		write(VANILLA);
		this.applied = 0;
	}

	/** Rewrites the hurt rows (the top half) of the shared entity overlay texture. */
	private static void write(int argb) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.gameRenderer == null) {
			return;
		}
		DynamicTexture texture = ((OverlayTextureAccessor) mc.gameRenderer.overlayTexture()).xeys$texture();
		NativeImage pixels = texture.getPixels();
		if (pixels == null) {
			return;
		}
		for (int y = 0; y < 8; y++) {
			for (int x = 0; x < 16; x++) {
				pixels.setPixel(x, y, argb);
			}
		}
		texture.upload();
	}
}
