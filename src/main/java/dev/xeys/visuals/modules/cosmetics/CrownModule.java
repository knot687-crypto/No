package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
import net.minecraft.client.renderer.LightTexture;

/** A gold crown with jeweled points. */
public final class CrownModule extends Module implements WorldEvents.PlayerLayer {
	private static final int POINTS = 8;

	private final ColorSetting metal = this.add(new ColorSetting("metal", "Metal", 0xFFF2C14E).noAlpha().group("Crown"));
	private final ColorSetting gems = this.add(new ColorSetting("gems", "Gems", 0xFFFF3D5A, ColorSetting.Mode.ACCENT).noAlpha().group("Crown"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 70f, 140f, 5f).unit("%").group("Crown"));

	public CrownModule() {
		super("crown", "Crown", Category.COSMETICS, Icons.CROWN, "A gold crown with glowing gems");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		float s = this.size.value() / 100f;
		c.pose.translate(0f, top + 0.6f / 16f, 0f);
		c.pose.scale(s, s, s);
		int gold = this.metal.color() | 0xFF000000;
		int dark = Colors.lerp(gold, 0xFF000000, 0.35f);
		int bright = Colors.lerp(gold, 0xFFFFFFFF, 0.35f);
		int light = c.light;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.SOLID, (pose, vc) -> this.crown(vc, pose, dark, gold, bright, light));
		int gem = this.gems.color() | 0xFF000000;
		c.collector.submitCustomGeometry(c.pose, Cosmetics.EMISSIVE, (pose, vc) -> this.gems(vc, pose, gem));
	}

	/** A band around the head with triangular points rising from it. */
	private void crown(VertexConsumer vc, PoseStack.Pose pose, int dark, int gold, int bright, int light) {
		float r = 4.9f / 16f;
		float band = 2.2f / 16f;
		float point = 2.6f / 16f;
		CosmeticMesh.cone(vc, pose, 0f, 0f, 0f, r, r, -band, POINTS * 2, dark, gold, light, false);
		CosmeticMesh.cone(vc, pose, 0f, 0f, 0f, r * 0.94f, r * 0.94f, -band, POINTS * 2, dark, dark, light, false);
		for (int i = 0; i < POINTS; i++) {
			double a0 = Math.PI * 2.0 * i / POINTS;
			double a1 = Math.PI * 2.0 * (i + 1) / POINTS;
			double am = (a0 + a1) * 0.5;
			float x0 = (float) Math.cos(a0) * r;
			float z0 = (float) Math.sin(a0) * r;
			float x1 = (float) Math.cos(a1) * r;
			float z1 = (float) Math.sin(a1) * r;
			float xm = (float) Math.cos(am) * r;
			float zm = (float) Math.sin(am) * r;
			CosmeticMesh.triangle(vc, pose, x0, -band, z0, xm, -band - point, zm, x1, -band, z1, bright, light);
		}
	}

	/** A small glowing gem at the tip of every point and around the band. */
	private void gems(VertexConsumer vc, PoseStack.Pose pose, int gem) {
		float r = 5.0f / 16f;
		float band = 2.2f / 16f;
		float point = 2.6f / 16f;
		float g = 0.45f / 16f;
		for (int i = 0; i < POINTS; i++) {
			double am = Math.PI * 2.0 * (i + 0.5) / POINTS;
			float x = (float) Math.cos(am) * r;
			float z = (float) Math.sin(am) * r;
			CosmeticMesh.box(vc, pose, x - g, -band - point - g, z - g, x + g, -band - point + g, z + g, gem, LightTexture.FULL_BRIGHT);
			double ab = Math.PI * 2.0 * i / POINTS;
			float bx = (float) Math.cos(ab) * r;
			float bz = (float) Math.sin(ab) * r;
			CosmeticMesh.box(vc, pose, bx - g, -band * 0.5f - g, bz - g, bx + g, -band * 0.5f + g, bz + g, gem, LightTexture.FULL_BRIGHT);
		}
	}
}
