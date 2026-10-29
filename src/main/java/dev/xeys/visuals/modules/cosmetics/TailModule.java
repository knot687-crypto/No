package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
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

/** A fox, cat or wolf tail that sways as you stand and swings as you walk. */
public final class TailModule extends Module implements WorldEvents.PlayerLayer {
	public enum Kind {
		FOX(12, 1.15f, 2.9f, 8f),
		CAT(14, 1.05f, 0.95f, 7f),
		WOLF(11, 1.1f, 2.3f, 3f);

		final int segments;
		final float segment;
		final float thickness;
		final float curl;

		Kind(int segments, float segment, float thickness, float curl) {
			this.segments = segments;
			this.segment = segment;
			this.thickness = thickness;
			this.curl = curl;
		}
	}

	private final EnumSetting<Kind> kind = this.add(new EnumSetting<>("kind", "Tail", Kind.FOX).group("Tail"));
	private final ColorSetting base = this.add(new ColorSetting("base", "Base", 0xFFE0782E).noAlpha().group("Tail"));
	private final ColorSetting tip = this.add(new ColorSetting("tip", "Tip", 0xFFF7F2EA).noAlpha().group("Tail"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Length", 100f, 50f, 160f, 5f).unit("%").group("Tail"));
	private final NumberSetting sway = this.add(new NumberSetting("sway", "Sway", 100f, 0f, 200f, 5f).unit("%").group("Tail"));

	public TailModule() {
		super("tail", "Tail", Category.COSMETICS, Icons.SQUIRREL, "A fox, cat or wolf tail that sways");
	}

	@Override
	public void submitOnPlayer(CosmeticContext c) {
		Anchors.body(c);
		Kind k = this.kind.get();
		PoseStack pose = c.pose;
		pose.translate(0f, 10.5f / 16f, Anchors.back(c) - 0.4f / 16f);
		// Down and back, then curling up along its length.
		pose.mulPose(Axis.XP.rotationDegrees(55f));
		float swayAmount = this.sway.value() / 100f;
		float walk = (float) Math.sin(c.state.walkAnimationPos * 0.6662f) * Math.min(1f, c.state.walkAnimationSpeed) * 0.5f;
		int b = this.base.color() | 0xFF000000;
		int t = this.tip.color() | 0xFF000000;
		float seg = k.segment / 16f * this.length.value() / 100f;
		int light = c.light;
		for (int i = 0; i < k.segments; i++) {
			float f = (float) i / (k.segments - 1);
			// Fox and wolf tails are bushiest in the middle; a cat's tapers.
			float bulge = k == Kind.CAT ? 1f - f * 0.45f : 0.55f + (float) Math.sin(f * Math.PI * 0.9) * 0.6f;
			float r = k.thickness / 16f * bulge * 0.5f;
			float tipMix = k == Kind.CAT ? f * 0.3f : Math.max(0f, (f - 0.72f) / 0.28f);
			int col = Colors.lerp(b, t, tipMix);
			int colNext = Colors.lerp(b, t, Math.max(0f, k == Kind.CAT ? (f + 1f / k.segments) * 0.3f : (f + 1f / k.segments - 0.72f) / 0.28f));
			float rr = r;
			c.collector.submitCustomGeometry(pose, Cosmetics.SOLID, (p, vc) -> CosmeticMesh.box(vc, p, -rr, 0f, -rr, rr, seg * 1.15f, rr, col, colNext,
				light));
			pose.translate(0f, seg, 0f);
			float swing = ((float) Math.sin(c.time * 2.1 - i * 0.45) * 7f + walk * 20f) * swayAmount * f;
			pose.mulPose(Axis.ZP.rotationDegrees(swing));
			pose.mulPose(Axis.XP.rotationDegrees(k.curl));
		}
	}
}
