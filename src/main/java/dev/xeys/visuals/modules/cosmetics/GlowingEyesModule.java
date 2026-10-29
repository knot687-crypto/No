package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.renderer.LightTexture;

/** Your eyes glow: two bright pixels on the face with a soft halo, blinking now and then. */
public final class GlowingEyesModule extends Module implements WorldEvents.PlayerLayer {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Eyes"));
	private final NumberSetting row = this.add(new NumberSetting("row", "Eye Row", 4f, 2f, 6f, 1f)
		.describe("Pixel row of the eyes on your skin, counted from the top of the head").group("Eyes"));
	private final NumberSetting halo = this.add(new NumberSetting("halo", "Glow", 100f, 0f, 200f, 5f).unit("%").group("Eyes"));
	private final BoolSetting blink = this.add(new BoolSetting("blink", "Blink", true).group("Eyes"));

	public GlowingEyesModule() {
		super("glowing_eyes", "Glowing Eyes", Category.COSMETICS, Icons.EYE, "Your eyes glow in the dark");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		c.model.head.translateAndRotate(c.pose);
		// Closed for a moment every few seconds.
		if (this.blink.on() && c.time % 5.3f < 0.12f) {
			return;
		}
		int col = this.color.color() | 0xFF000000;
		float y0 = (-8f + this.row.value()) / 16f;
		float y1 = y0 + 1f / 16f;
		// In front of the skin's hat layer.
		float z = -4.62f / 16f;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.EMISSIVE, (pose, vc) -> {
			CosmeticMesh.quad(vc, pose, -3f / 16f, y0, z, -3f / 16f, y1, z, -1f / 16f, y1, z, -1f / 16f, y0, z, col, LightTexture.FULL_BRIGHT);
			CosmeticMesh.quad(vc, pose, 1f / 16f, y0, z, 1f / 16f, y1, z, 3f / 16f, y1, z, 3f / 16f, y0, z, col, LightTexture.FULL_BRIGHT);
		});
		float g = this.halo.value() / 100f;
		if (g > 0f && WorldGlow.dotsUsable()) {
			int glow = Colors.withAlpha(col, Math.min(1f, 0.6f * g));
			float r = 2.2f / 16f * (0.6f + 0.4f * g);
			float cy = (y0 + y1) * 0.5f;
			c.collector.submitCustomGeometry(c.pose, WorldGlow.DOTS, (pose, vc) -> {
				WorldGlow.sprite(vc, pose, -2f / 16f, cy, z - 0.01f, r, 0f, 0f, 0f, r, 0f, glow);
				WorldGlow.sprite(vc, pose, 2f / 16f, cy, z - 0.01f, r, 0f, 0f, 0f, r, 0f, glow);
			});
		}
	}
}
