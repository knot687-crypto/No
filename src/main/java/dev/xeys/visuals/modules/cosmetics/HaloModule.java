package dev.xeys.visuals.modules.cosmetics;

import com.mojang.math.Axis;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.Anchors;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.renderer.LightTexture;

/** A glowing ring floating above your head. */
public final class HaloModule extends Module implements WorldEvents.PlayerLayer {
	public enum Style {
		LIGHT,
		GOLDEN
	}

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.LIGHT).group("Halo"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Halo"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 50f, 200f, 5f).unit("%").group("Halo"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height", 3f, 0.5f, 8f, 0.5f).unit(" px").decimals(1)
		.describe("How far above your head it floats").group("Halo"));
	private final NumberSetting tilt = this.add(new NumberSetting("tilt", "Tilt", 12f, 0f, 45f, 1f).unit("°").group("Halo"));
	private final BoolSetting bob = this.add(new BoolSetting("bob", "Float", true).describe("Drift gently up and down").group("Halo"));

	public HaloModule() {
		super("halo", "Halo", Category.COSMETICS, Icons.CIRCLE, "A glowing ring floats above your head");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		float drift = this.bob.on() ? (float) Math.sin(c.time * 1.7) * 0.018f : 0f;
		c.pose.translate(0f, top - this.height.value() / 16f + drift, 0f);
		c.pose.mulPose(Axis.XP.rotationDegrees(-this.tilt.value()));
		float r = 0.2f * this.size.value() / 100f;
		int col = this.color.color() | 0xFF000000;
		if (this.style.is(Style.GOLDEN)) {
			int gold = 0xFFFFD25C;
			c.collector.submitCustomGeometry(c.pose, Cosmetics.SOLID, (pose, vc) ->
				CosmeticMesh.torus(vc, pose, 0f, 0f, 0f, r, r * 0.09f, 40, 8, gold, LightTexture.FULL_BRIGHT));
			col = Colors.lerp(col, gold, 0.5f);
		} else {
			int core = Colors.lerp(col, 0xFFFFFFFF, 0.55f);
			c.collector.submitCustomGeometry(c.pose, Cosmetics.EMISSIVE, (pose, vc) ->
				CosmeticMesh.torus(vc, pose, 0f, 0f, 0f, r, r * 0.05f, 40, 6, core, LightTexture.FULL_BRIGHT));
		}
		if (WorldGlow.ringsUsable()) {
			int glow = Colors.withAlpha(col, 0.9f);
			c.collector.submitCustomGeometry(c.pose, WorldGlow.RINGS, (pose, vc) -> WorldGlow.flatRing(vc, pose, 0f, 0f, 0f, r, glow));
		}
	}
}
