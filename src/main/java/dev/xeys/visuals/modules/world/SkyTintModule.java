package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;

/** Recolors the sky and the sunrise/sunset glow, and brightens the stars. */
public final class SkyTintModule extends Module implements Events.Environment {
	private final ColorSetting sky = this.add(new ColorSetting("sky", "Sky Color", 0xFF6C8CFF).noAlpha().group("Sky"));
	private final NumberSetting skyAmount = this.add(new NumberSetting("sky_amount", "Sky Tint", 45f, 0f, 100f, 1f).unit("%").group("Sky"));
	private final ColorSetting sunset = this.add(new ColorSetting("sunset", "Sunset Color", 0xFFFF6A4D).noAlpha().group("Sunset"));
	private final NumberSetting sunsetAmount = this.add(new NumberSetting("sunset_amount", "Sunset Tint", 50f, 0f, 100f, 1f).unit("%")
		.group("Sunset"));
	private final NumberSetting stars = this.add(new NumberSetting("stars", "Star Brightness", 160f, 0f, 300f, 5f).unit("%").group("Night"));

	public SkyTintModule() {
		super("sky_tint", "Sky Tint", Category.WORLD, Icons.PALETTE, "Recolor the sky and sunsets, brighten the stars");
	}

	@Override
	public Object modifyEnvironment(EnvironmentAttribute<?> attribute, Object value) {
		if (attribute == EnvironmentAttributes.SKY_COLOR && value instanceof Integer c) {
			return Colors.withAlpha(Colors.lerpOklab(c, this.sky.color(), this.skyAmount.value() / 100f), Colors.alpha(c));
		}
		if (attribute == EnvironmentAttributes.SUNRISE_SUNSET_COLOR && value instanceof Integer c) {
			// The alpha is the glow's strength at this moment; keep it and shift only the hue.
			return Colors.withAlpha(Colors.lerpOklab(c | 0xFF000000, this.sunset.color(), this.sunsetAmount.value() / 100f), Colors.alpha(c));
		}
		if (attribute == EnvironmentAttributes.STAR_BRIGHTNESS && value instanceof Float b) {
			return Math.min(1f, b * this.stars.value() / 100f);
		}
		return value;
	}
}
