package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.Anchors;
import dev.xeys.visuals.cosmetic.CosmeticContext;
import dev.xeys.visuals.cosmetic.CosmeticMesh;
import dev.xeys.visuals.cosmetic.Cosmetics;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.rendertype.RenderType;

/**
 * Wings on your back: feathered angel wings, dragon wings or wings of light. They breathe slowly at
 * rest, beat when you are airborne and fold when you sneak.
 */
public final class WingsModule extends Module implements WorldEvents.PlayerLayer {
	public enum Style {
		ANGEL,
		DRAGON,
		ENERGY
	}

	private static final int FEATHERS = 13;

	private final EnumSetting<Style> style = this.add(new EnumSetting<>("style", "Style", Style.ANGEL).group("Wings"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFF2F3F8).noAlpha()
		.describe("Feathers, membrane or light").group("Wings"));
	private final ColorSetting accent = this.add(new ColorSetting("accent", "Accent", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha()
		.describe("Wing tips and edges").group("Wings"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 50f, 160f, 5f).unit("%").group("Wings"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Flap Speed", 100f, 20f, 300f, 5f).unit("%").group("Wings"));

	public WingsModule() {
		super("wings", "Wings", Category.COSMETICS, Icons.FEATHER, "Angel, dragon or energy wings that flap");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		if (c.state.isFallFlying) {
			return;
		}
		Anchors.body(c);
		float back = Anchors.back(c);
		boolean airborne = c.self && Minecraft.getInstance().player != null && !Minecraft.getInstance().player.onGround();
		float t = c.time * this.speed.value() / 100f;
		float sweep;
		float lift;
		if (c.state.isCrouching) {
			sweep = 58f;
			lift = -6f;
		} else if (airborne) {
			float s = (float) Math.sin(t * 7.0);
			sweep = 14f + 16f * s;
			lift = 10f + 28f * (float) Math.sin(t * 7.0 + 0.7);
		} else {
			float s = (float) Math.sin(t * 1.5);
			sweep = 24f + 6f * s;
			lift = 12f + 7f * (float) Math.sin(t * 1.5 + 0.6);
		}
		float len = 0.95f * this.size.value() / 100f;
		Style st = this.style.get();
		int main = this.color.color() | 0xFF000000;
		int edge = this.accent.color() | 0xFF000000;
		int light = st == Style.ENERGY ? LightTexture.FULL_BRIGHT : c.light;
		RenderType type = switch (st) {
			case ANGEL -> Cosmetics.SOLID;
			case DRAGON -> Cosmetics.TRANSLUCENT;
			case ENERGY -> Cosmetics.EMISSIVE;
		};
		PoseStack pose = c.pose;
		for (int side = -1; side <= 1; side += 2) {
			pose.pushPose();
			pose.translate(side * 1.2f / 16f, 3f / 16f, back + 0.3f / 16f);
			pose.mulPose(Axis.YP.rotationDegrees(-side * sweep));
			pose.mulPose(Axis.ZP.rotationDegrees(-side * lift));
			int s = side;
			c.collector.submitCustomGeometry(pose, type, (p, vc) -> {
				if (st == Style.DRAGON) {
					dragon(vc, p, s, len, main, edge, light);
				} else {
					feathers(vc, p, s, len, main, edge, light, st == Style.ENERGY);
				}
			});
			pose.popPose();
		}
	}

	/** Two rows of feathers hanging from an arched leading edge. */
	private static void feathers(VertexConsumer vc, PoseStack.Pose pose, int side, float len, int main, int edge, int light, boolean energy) {
		for (int row = 0; row < 2; row++) {
			boolean coverts = row == 1;
			for (int i = 0; i < FEATHERS; i++) {
				float t = 0.06f + 0.94f * i / (FEATHERS - 1);
				// Leading edge point and its tangent.
				float px = side * t * len;
				float py = -len * 0.34f * (float) Math.sin(t * Math.PI * 0.85);
				float pz = t * 0.1f * len + (coverts ? -0.012f : 0.004f * i);
				float tx = side * len;
				float ty = -len * 0.34f * (float) (Math.cos(t * Math.PI * 0.85) * Math.PI * 0.85);
				float tl = (float) Math.sqrt(tx * tx + ty * ty);
				tx /= tl;
				ty /= tl;
				// Feathers hang down, fanning outward toward the tip.
				float dx = side * (0.12f + 0.75f * t);
				float dy = 1f;
				float dl = (float) Math.sqrt(dx * dx + dy * dy);
				dx /= dl;
				dy /= dl;
				float fl = len * (0.4f + 0.42f * (float) Math.sin(t * Math.PI)) * (coverts ? 0.48f : 1f);
				float w = len * (coverts ? 0.19f : 0.16f);
				int base = coverts ? Colors.lerp(main, 0xFFFFFFFF, 0.25f) : main;
				int tip = energy ? Colors.lerp(edge, 0xFF000000, 0.75f) : Colors.lerp(main, edge, coverts ? 0.15f : 0.45f);
				if (energy) {
					base = Colors.lerp(edge, 0xFFFFFFFF, coverts ? 0.3f : 0.1f);
				}
				float ax = px - tx * w * 0.5f;
				float ay = py - ty * w * 0.5f;
				float bx = px + tx * w * 0.5f;
				float by = py + ty * w * 0.5f;
				float ex = px + dx * fl;
				float ey = py + dy * fl;
				CosmeticMesh.vertex(vc, pose, ax, ay, pz, 0f, 0f, base, light, 0f, 0f, 1f);
				CosmeticMesh.vertex(vc, pose, ex - tx * w * 0.42f, ey - ty * w * 0.42f, pz, 0f, 1f, tip, light, 0f, 0f, 1f);
				CosmeticMesh.vertex(vc, pose, ex + tx * w * 0.42f, ey + ty * w * 0.42f, pz, 1f, 1f, tip, light, 0f, 0f, 1f);
				CosmeticMesh.vertex(vc, pose, bx, by, pz, 1f, 0f, base, light, 0f, 0f, 1f);
				if (energy) {
					// Light is drawn one-sided; repeat with the other winding so both wings show.
					CosmeticMesh.vertex(vc, pose, bx, by, pz, 1f, 0f, base, light, 0f, 0f, -1f);
					CosmeticMesh.vertex(vc, pose, ex + tx * w * 0.42f, ey + ty * w * 0.42f, pz, 1f, 1f, tip, light, 0f, 0f, -1f);
					CosmeticMesh.vertex(vc, pose, ex - tx * w * 0.42f, ey - ty * w * 0.42f, pz, 0f, 1f, tip, light, 0f, 0f, -1f);
					CosmeticMesh.vertex(vc, pose, ax, ay, pz, 0f, 0f, base, light, 0f, 0f, -1f);
				}
			}
		}
	}

	/** Four finger bones from the wrist with a translucent membrane between them. */
	private static void dragon(VertexConsumer vc, PoseStack.Pose pose, int side, float len, int main, int edge, int light) {
		int membrane = Colors.withAlpha(Colors.lerp(main, 0xFF000000, 0.55f), 0.82f);
		int bone = Colors.lerp(main, 0xFF000000, 0.75f) | 0xFF000000;
		float wx = side * 0.45f * len;
		float wy = -0.32f * len;
		float[][] tips = {
			{side * 1.0f * len, -0.18f * len},
			{side * 0.92f * len, 0.22f * len},
			{side * 0.7f * len, 0.5f * len},
			{side * 0.38f * len, 0.62f * len},
		};
		// Membrane: the root and wrist to the first tip, then between neighbouring tips.
		CosmeticMesh.triangle(vc, pose, 0f, 0f, 0.01f, wx, wy, 0.01f, tips[0][0], tips[0][1], 0.01f, membrane, light);
		for (int i = 0; i < tips.length - 1; i++) {
			// The membrane sags a little between fingers.
			float mx = (tips[i][0] + tips[i + 1][0]) * 0.5f * 0.82f + wx * 0.18f;
			float my = (tips[i][1] + tips[i + 1][1]) * 0.5f * 0.82f + wy * 0.18f;
			CosmeticMesh.quad(vc, pose, wx, wy, 0.01f, tips[i][0], tips[i][1], 0.01f, mx, my, 0.01f, tips[i + 1][0], tips[i + 1][1], 0.01f,
				membrane, light);
		}
		CosmeticMesh.triangle(vc, pose, 0f, 0f, 0.01f, wx, wy, 0.01f, tips[3][0], tips[3][1], 0.01f, membrane, light);
		// Bones: the arm, then each finger, tipped with the accent.
		bone(vc, pose, 0f, 0f, wx, wy, 0.05f * len, bone, bone, light);
		for (float[] tip : tips) {
			bone(vc, pose, wx, wy, tip[0], tip[1], 0.025f * len, bone, edge, light);
		}
	}

	private static void bone(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float x1, float y1, float w, int c0, int c1, int light) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float l = (float) Math.sqrt(dx * dx + dy * dy);
		float nx = -dy / l * w;
		float ny = dx / l * w;
		CosmeticMesh.vertex(vc, pose, x0 - nx, y0 - ny, 0f, 0f, 0f, c0, light, 0f, 0f, 1f);
		CosmeticMesh.vertex(vc, pose, x1 - nx * 0.4f, y1 - ny * 0.4f, 0f, 0f, 1f, c1, light, 0f, 0f, 1f);
		CosmeticMesh.vertex(vc, pose, x1 + nx * 0.4f, y1 + ny * 0.4f, 0f, 1f, 1f, c1, light, 0f, 0f, 1f);
		CosmeticMesh.vertex(vc, pose, x0 + nx, y0 + ny, 0f, 1f, 0f, c0, light, 0f, 0f, 1f);
	}
}
