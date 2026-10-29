package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.cosmetic.Anchors;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;

/** Cat, fox or bunny ears that twitch now and then. */
public final class EarsModule extends Module implements WorldEvents.PlayerLayer {
	public enum Kind {
		CAT(2.2f, 3.2f, 0.12f),
		FOX(2.8f, 4.2f, 0.18f),
		BUNNY(1.6f, 7.0f, 0.05f);

		final float width;
		final float height;
		final float outward;

		Kind(float width, float height, float outward) {
			this.width = width;
			this.height = height;
			this.outward = outward;
		}
	}

	private final EnumSetting<Kind> kind = this.add(new EnumSetting<>("kind", "Ears", Kind.CAT).group("Ears"));
	private final ColorSetting outer = this.add(new ColorSetting("outer", "Fur", 0xFF2A2A2E).noAlpha().group("Ears"));
	private final ColorSetting inner = this.add(new ColorSetting("inner", "Inner", 0xFFF2A7B8).noAlpha().group("Ears"));
	private final BoolSetting twitch = this.add(new BoolSetting("twitch", "Twitch", true).group("Ears"));

	public EarsModule() {
		super("ears", "Ears", Category.COSMETICS, Icons.CAT, "Cat, fox or bunny ears");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		float top = Anchors.head(c);
		Kind k = this.kind.get();
		int fur = this.outer.color() | 0xFF000000;
		int pink = this.inner.color() | 0xFF000000;
		int light = c.light;
		// A quick flick every few seconds, alternating sides.
		float flick = 0f;
		if (this.twitch.on()) {
			float t = c.time % 4.2f;
			flick = t < 0.25f ? (float) Math.sin(t / 0.25f * Math.PI) * 0.35f : 0f;
		}
		float left = flick;
		float right = c.time % 8.4f >= 4.2f ? 0f : flick * 0.4f;
		PoseStack pose = c.pose;
		for (int side = -1; side <= 1; side += 2) {
			float tilt = (side < 0 ? left : right) + k.outward;
			pose.pushPose();
			pose.translate(side * 2.6f / 16f, top + 0.2f / 16f, 0.2f / 16f);
			pose.mulPose(com.mojang.math.Axis.ZP.rotation(side * tilt));
			pose.mulPose(com.mojang.math.Axis.XP.rotation(-0.12f));
			c.collector.submitCustomGeometry(pose, Cosmetics.SOLID, (p, vc) -> ear(vc, p, k, fur, pink, light));
			pose.popPose();
		}
	}

	/** A thin triangular prism with a lighter inner face toward the front. */
	private static void ear(VertexConsumer vc, PoseStack.Pose pose, Kind k, int fur, int pink, int light) {
		float w = k.width / 16f;
		float h = k.height / 16f;
		float d = 0.6f / 16f;
		if (k == Kind.BUNNY) {
			CosmeticMesh.box(vc, pose, -w, -h, -d, w, 0f, d, fur, light);
			CosmeticMesh.box(vc, pose, -w * 0.55f, -h * 0.9f, -d - 0.02f / 16f, w * 0.55f, -h * 0.1f, -d, pink, light);
			return;
		}
		// Front (inner) and back faces, then the two slanted sides and the base.
		CosmeticMesh.triangle(vc, pose, -w, 0f, -d, 0f, -h, -d, w, 0f, -d, pink, light);
		CosmeticMesh.triangle(vc, pose, w, 0f, d, 0f, -h, d, -w, 0f, d, fur, light);
		CosmeticMesh.quad(vc, pose, -w, 0f, -d, -w, 0f, d, 0f, -h, d, 0f, -h, -d, fur, light);
		CosmeticMesh.quad(vc, pose, w, 0f, d, w, 0f, -d, 0f, -h, -d, 0f, -h, d, fur, light);
	}
}
