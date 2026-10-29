package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.Anchors;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.renderer.LightTexture;

/** Curved horns that sweep up and back from the head, optionally glowing at the tips. */
public final class HornsModule extends Module implements WorldEvents.PlayerLayer {
	private static final int SEGMENTS = 7;

	private final ColorSetting base = this.add(new ColorSetting("base", "Base", 0xFF1C1416).noAlpha().group("Horns"));
	private final ColorSetting tip = this.add(new ColorSetting("tip", "Tip", 0xFFFF3D5A, ColorSetting.Mode.ACCENT).noAlpha().group("Horns"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Length", 100f, 50f, 180f, 5f).unit("%").group("Horns"));
	private final NumberSetting curl = this.add(new NumberSetting("curl", "Curl", 100f, 0f, 200f, 5f).unit("%").group("Horns"));
	private final BoolSetting glow = this.add(new BoolSetting("glow", "Glowing Tips", true).group("Horns"));

	public HornsModule() {
		super("horns", "Horns", Category.COSMETICS, Icons.FLAME, "Curved horns sweeping back from the head");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		int b = this.base.color() | 0xFF000000;
		int t = this.tip.color() | 0xFF000000;
		float len = this.length.value() / 100f;
		float bend = this.curl.value() / 100f;
		boolean tipGlow = this.glow.on();
		int light = c.light;
		PoseStack pose = c.pose;
		for (int side = -1; side <= 1; side += 2) {
			pose.pushPose();
			pose.translate(side * 2.4f / 16f, top + 1.2f / 16f, -0.8f / 16f);
			// Outward, then a little back.
			pose.mulPose(com.mojang.math.Axis.ZP.rotation(side * 0.35f));
			int s = side;
			for (int i = 0; i < SEGMENTS; i++) {
				float f0 = (float) i / SEGMENTS;
				float f1 = (float) (i + 1) / SEGMENTS;
				float r0 = (1.1f - f0 * 0.9f) / 16f;
				float r1 = (1.1f - f1 * 0.9f) / 16f;
				float seg = 1.05f / 16f * len;
				int c0 = Colors.lerp(b, t, f0 * f0);
				int c1 = Colors.lerp(b, t, f1 * f1);
				boolean last = i == SEGMENTS - 1;
				c.collector.submitCustomGeometry(pose, last && tipGlow ? Cosmetics.EMISSIVE : Cosmetics.SOLID, (p, vc) ->
					CosmeticMesh.cone(vc, p, 0f, 0f, 0f, r0, r1, -seg, 10, c0, c1, last && tipGlow ? LightTexture.FULL_BRIGHT : light, false));
				pose.translate(0f, -seg, 0f);
				// Each segment curls further back and outward.
				pose.mulPose(com.mojang.math.Axis.XP.rotation(0.22f * bend));
				pose.mulPose(com.mojang.math.Axis.ZP.rotation(s * 0.05f * bend));
			}
			pose.popPose();
		}
	}
}
