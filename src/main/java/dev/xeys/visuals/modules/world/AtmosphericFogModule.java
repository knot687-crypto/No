package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;

/**
 * Atmosphere: a colored distance haze. It can only bring fog closer, never push it away, and it leaves
 * water, lava, blindness and darkness fog alone.
 */
public final class AtmosphericFogModule extends Module implements Events.Environment {
	private final ColorSetting color = this.add(new ColorSetting("color", "Fog Color", 0xFFB9C8E6).noAlpha().group("Fog"));
	private final NumberSetting tint = this.add(new NumberSetting("tint", "Color Amount", 40f, 0f, 100f, 1f).unit("%").group("Fog"));
	private final BoolSetting haze = this.add(new BoolSetting("haze", "Distance Haze", true).group("Haze"));
	private final NumberSetting distance = this.add(new NumberSetting("distance", "Haze Distance", 160f, 32f, 512f, 8f).unit(" blocks")
		.describe("Where the haze becomes solid").visibleWhen(this.haze::on).group("Haze"));
	private final NumberSetting start = this.add(new NumberSetting("start", "Haze Start", 25f, 0f, 90f, 1f).unit("%")
		.describe("Fraction of the distance where the haze begins").visibleWhen(this.haze::on).group("Haze"));

	public AtmosphericFogModule() {
		super("atmospheric_fog", "Atmospheric Fog", Category.WORLD, Icons.CLOUD_FOG, "Colored distance haze for atmosphere");
	}

	@Override
	public Object modifyEnvironment(EnvironmentAttribute<?> attribute, Object value) {
		if (attribute == EnvironmentAttributes.FOG_COLOR && value instanceof Integer c) {
			return Colors.withAlpha(Colors.lerpOklab(c, this.color.color(), this.tint.value() / 100f), Colors.alpha(c));
		}
		if (this.haze.on()) {
			float end = this.distance.value();
			if (attribute == EnvironmentAttributes.FOG_END_DISTANCE && value instanceof Float f) {
				return Math.min(f, end);
			}
			if (attribute == EnvironmentAttributes.FOG_START_DISTANCE && value instanceof Float f) {
				return Math.min(f, end * this.start.value() / 100f);
			}
		}
		return value;
	}
}
