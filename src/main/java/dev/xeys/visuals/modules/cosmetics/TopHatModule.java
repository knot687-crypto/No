package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.Anchors;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;

/** A tall silk top hat with a colored band. */
public final class TopHatModule extends Module implements WorldEvents.PlayerLayer {
	private final ColorSetting hat = this.add(new ColorSetting("hat", "Hat", 0xFF141416).noAlpha().group("Top Hat"));
	private final ColorSetting band = this.add(new ColorSetting("band", "Band", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Top Hat"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height", 100f, 60f, 160f, 5f).unit("%").group("Top Hat"));
	private final NumberSetting tilt = this.add(new NumberSetting("tilt", "Tilt", 6f, -20f, 20f, 1f).unit("°").group("Top Hat"));

	public TopHatModule() {
		super("top_hat", "Top Hat", Category.COSMETICS, Icons.CYLINDER, "A tall silk top hat with a colored band");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		c.pose.translate(0f, top + 0.3f / 16f, 0f);
		c.pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(this.tilt.value()));
		int body = this.hat.color() | 0xFF000000;
		int sheen = Colors.lerp(body, 0xFFFFFFFF, 0.12f);
		int stripe = this.band.color() | 0xFF000000;
		float h = 6.5f / 16f * this.height.value() / 100f;
		int light = c.light;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.SOLID, (pose, vc) -> {
			// Brim, then the crown, then a band just above the brim.
			CosmeticMesh.cone(vc, pose, 0f, 0f, 0f, 7f / 16f, 7f / 16f, -0.5f / 16f, 32, body, body, light, true);
			CosmeticMesh.disc(vc, pose, 0f, -0.5f / 16f, 0f, 7f / 16f, 32, sheen, light);
			CosmeticMesh.cone(vc, pose, 0f, -0.5f / 16f, 0f, 4.2f / 16f, 4.5f / 16f, -h, 32, body, sheen, light, false);
			CosmeticMesh.disc(vc, pose, 0f, -0.5f / 16f - h, 0f, 4.5f / 16f, 32, sheen, light);
			CosmeticMesh.cone(vc, pose, 0f, -0.55f / 16f, 0f, 4.3f / 16f, 4.35f / 16f, -1.3f / 16f, 32, stripe, stripe, light, false);
		});
	}
}
