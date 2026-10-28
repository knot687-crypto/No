package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/** Darkened (or tinted) screen edges that draw the eye to the center. */
public final class VignetteModule extends ScreenEffectModule {
	public enum Shape {
		ROUND,
		SCREEN
	}

	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 45f, 0f, 100f, 1f).unit("%").group("Vignette"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 72f, 20f, 120f, 1f).unit("%")
		.describe("How far from the center the darkening begins").group("Vignette"));
	private final NumberSetting softness = this.add(new NumberSetting("softness", "Softness", 45f, 5f, 90f, 1f).unit("%").group("Vignette"));
	private final EnumSetting<Shape> shape = this.add(new EnumSetting<>("shape", "Shape", Shape.ROUND)
		.describe("A circle, or an oval that follows the screen's shape").group("Vignette"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFF000000).noAlpha().group("Vignette"));

	public VignetteModule() {
		super("vignette", "Vignette", Icons.APERTURE, "Softly darkened screen edges");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		fx.vignette = this.strength.value() / 100f;
		fx.vignetteRadius = this.radius.value() / 100f;
		fx.vignetteSoftness = this.softness.value() / 100f;
		fx.vignetteRoundness = this.shape.is(Shape.ROUND) ? 1f : 0f;
		fx.vignetteColor = this.color.color();
	}
}
