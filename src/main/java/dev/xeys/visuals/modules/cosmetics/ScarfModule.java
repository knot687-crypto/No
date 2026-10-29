package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
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

/** A striped scarf around your neck whose loose ends stream behind you as you move. */
public final class ScarfModule extends Module implements WorldEvents.PlayerLayer {
	private static final int SEGMENTS = 7;

	private final ColorSetting main = this.add(new ColorSetting("main", "Color", 0xFFB3243C).noAlpha().group("Scarf"));
	private final ColorSetting stripe = this.add(new ColorSetting("stripe", "Stripes", 0xFFF2EEE8).noAlpha().group("Scarf"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Length", 100f, 50f, 180f, 5f).unit("%").group("Scarf"));

	public ScarfModule() {
		super("scarf", "Scarf", Category.COSMETICS, Icons.RIBBON, "A striped scarf with ends that stream behind you");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		Anchors.body(c);
		int a = this.main.color() | 0xFF000000;
		int b = this.stripe.color() | 0xFF000000;
		int light = c.light;
		float back = Anchors.back(c);
		float front = back;
		float half = (c.state.chestEquipment.isEmpty() ? 4.4f : 5.2f) / 16f;
		float h0 = -0.2f / 16f;
		float h1 = 2.0f / 16f;
		float t = 0.6f / 16f;
		// The wrap around the neck: front, back and two sides.
		c.collector.submitCustomGeometry(c.pose, Cosmetics.SOLID, (pose, vc) -> {
			CosmeticMesh.box(vc, pose, -half, h0, -front - t, half, h1, -front, a, light);
			CosmeticMesh.box(vc, pose, -half, h0, back, half, h1, back + t, a, light);
			CosmeticMesh.box(vc, pose, -half - t, h0, -front - t, -half, h1, back + t, a, light);
			CosmeticMesh.box(vc, pose, half, h0, -front - t, half + t, h1, back + t, a, light);
		});
		// Two loose ends falling from the back, swinging out with speed and a slow idle drift.
		float speed = Math.min(1f, c.state.walkAnimationSpeed);
		float lift = 18f + 55f * speed;
		float seg = 1.5f / 16f * this.length.value() / 100f;
		PoseStack pose = c.pose;
		for (int end = 0; end < 2; end++) {
			pose.pushPose();
			pose.translate(end == 0 ? 1.4f / 16f : 2.8f / 16f, h1 * 0.6f, back + t);
			pose.mulPose(Axis.XP.rotationDegrees(lift + end * 6f));
			for (int i = 0; i < SEGMENTS; i++) {
				int col = (i % 3 == 1) ? b : a;
				float w = (1.1f - end * 0.15f) / 16f;
				c.collector.submitCustomGeometry(pose, Cosmetics.SOLID, (p, vc) ->
					CosmeticMesh.box(vc, p, -w, 0f, -0.3f / 16f, w, seg * 1.08f, 0.3f / 16f, col, light));
				pose.translate(0f, seg, 0f);
				float flutter = (float) Math.sin(c.time * (5.0 + speed * 6.0) - i * 0.8 + end) * (4f + 10f * speed);
				pose.mulPose(Axis.XP.rotationDegrees(flutter * 0.4f));
				pose.mulPose(Axis.ZP.rotationDegrees(flutter * 0.6f));
			}
			pose.popPose();
		}
	}
}
