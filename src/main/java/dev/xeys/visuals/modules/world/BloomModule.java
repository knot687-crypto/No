package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.WorldFx;
import dev.xeys.visuals.ui.Icons;

/** A soft glow around bright parts of the world: torches, lava, the sun, lit windows. */
public final class BloomModule extends Module implements WorldEvents.WorldPostEffect {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 70f, 5f, 200f, 5f).unit("%").group("Bloom"));
	private final NumberSetting threshold = this.add(new NumberSetting("threshold", "Threshold", 72f, 30f, 95f, 1f).unit("%")
		.describe("How bright something must be to glow").group("Bloom"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 5f, 3f, 6f, 1f).describe("How far the glow spreads")
		.group("Bloom"));
	private final ColorSetting tint = this.add(new ColorSetting("tint", "Tint", 0xFFFFFFFF).noAlpha().group("Bloom"));

	public BloomModule() {
		super("bloom", "Bloom", Category.WORLD, Icons.SUN_MEDIUM, "Bright lights and highlights softly glow");
	}

	@Override
	public void contributeWorld(WorldFx fx, float partialTick) {
		fx.bloom = this.strength.value() / 100f;
		fx.bloomThreshold = this.threshold.value() / 100f;
		fx.bloomLevels = this.radius.intValue();
		fx.bloomTint = this.tint.color();
	}
}
