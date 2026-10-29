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
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.renderer.LightTexture;

/** A wide, translucent conical hat that fades from its rim to its tip, with a glowing edge. */
public final class ChinaHatModule extends Module implements WorldEvents.PlayerLayer {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xA0FFFFFF, ColorSetting.Mode.ACCENT).group("Hat"));
	private final NumberSetting width = this.add(new NumberSetting("width", "Width", 100f, 60f, 180f, 5f).unit("%").group("Hat"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height", 100f, 40f, 200f, 5f).unit("%").group("Hat"));
	private final NumberSetting spin = this.add(new NumberSetting("spin", "Spin", 20f, 0f, 200f, 5f).unit("°/s")
		.describe("A slow turn; the gradient makes it shimmer").group("Hat"));

	public ChinaHatModule() {
		super("china_hat", "China Hat", Category.COSMETICS, Icons.TRIANGLE, "A translucent cone hat with a glowing rim");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		c.pose.translate(0f, top + 0.4f / 16f, 0f);
		c.pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(c.time * this.spin.value()));
		float r = 9f / 16f * this.width.value() / 100f;
		float h = 3.8f / 16f * this.height.value() / 100f;
		int rim = this.color.color();
		int tip = Colors.withAlpha(Colors.lerp(rim, 0xFFFFFFFF, 0.3f), Math.round(Colors.alpha(rim) * 0.25f));
		int light = LightTexture.FULL_BRIGHT;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.TRANSLUCENT, (pose, vc) ->
			CosmeticMesh.cone(vc, pose, 0f, 0f, 0f, r, 0f, -h, 48, rim, tip, light, false));
		if (WorldGlow.ringsUsable()) {
			int glow = Colors.withAlpha(rim, 0.85f);
			c.collector.submitCustomGeometry(c.pose, WorldGlow.RINGS, (pose, vc) -> WorldGlow.flatRing(vc, pose, 0f, 0f, 0f, r, glow));
		}
	}
}
