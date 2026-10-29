package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.GlintTextures;
import dev.xeys.visuals.world.WorldHooks;

/** The color, speed and strength of the enchantment glint on items and armor. */
public final class EnchantGlintModule extends Module implements Events.FrameStart {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Glint"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 100f, 25f, 400f, 5f).unit("%").group("Glint"));
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 10f, 200f, 5f).unit("%").group("Glint"));
	private double lastApply = -1.0;

	public EnchantGlintModule() {
		super("enchant_glint", "Enchant Glint", Category.WORLD, Icons.WAND_SPARKLES, "Recolor the enchantment shimmer");
	}

	@Override
	public void onFrameStart() {
		WorldHooks.glintSpeed = this.speed.value() / 100f;
		WorldHooks.glintStrength = this.strength.value() / 100f;
		int c = this.color.color() | 0xFF000000;
		// Re-tint when the color changes; animated colors refresh at most ten times a second.
		if (c != GlintTextures.applied() && Clock.time() - this.lastApply > 0.1) {
			GlintTextures.apply(c);
			this.lastApply = Clock.time();
		}
	}

	@Override
	protected void onDisable() {
		GlintTextures.restore();
		WorldHooks.glintSpeed = 1f;
		WorldHooks.glintStrength = 1f;
		this.lastApply = -1.0;
	}
}
