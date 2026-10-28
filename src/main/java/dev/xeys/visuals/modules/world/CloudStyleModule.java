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

/** Cloud color, opacity and height. */
public final class CloudStyleModule extends Module implements Events.Environment {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF).noAlpha().group("Clouds"));
	private final NumberSetting tint = this.add(new NumberSetting("tint", "Color Amount", 60f, 0f, 100f, 1f).unit("%").group("Clouds"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 100f, 10f, 100f, 1f).unit("%").group("Clouds"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height Offset", 0f, -96f, 128f, 4f).unit(" blocks").group("Clouds"));

	public CloudStyleModule() {
		super("cloud_style", "Cloud Style", Category.WORLD, Icons.CLOUDY, "Tint clouds and change their height");
	}

	@Override
	public Object modifyEnvironment(EnvironmentAttribute<?> attribute, Object value) {
		if (attribute == EnvironmentAttributes.CLOUD_COLOR && value instanceof Integer c) {
			int rgb = Colors.lerpOklab(c | 0xFF000000, this.color.color(), this.tint.value() / 100f);
			return Colors.withAlpha(rgb, Math.round(Colors.alpha(c) * this.opacity.value() / 100f));
		}
		if (attribute == EnvironmentAttributes.CLOUD_HEIGHT && value instanceof Float h) {
			return h + this.height.value();
		}
		return value;
	}
}
