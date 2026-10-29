package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Underwater, the camera drifts and rolls slowly with the current. */
public final class WaterSwayModule extends Module {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 25f, 300f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Sway"));

	public WaterSwayModule() {
		super("water_sway", "Underwater Sway", Category.ANIMATIONS, Icons.DROPLETS, "A slow drift of the camera underwater");
	}

	private void apply() {
		CameraMotion.waterSway = this.isEnabled() ? this.strength.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.waterSway = 0f;
	}
}
