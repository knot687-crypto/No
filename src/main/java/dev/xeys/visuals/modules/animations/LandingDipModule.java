package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** The camera dips and springs back when you land, more after longer falls. */
public final class LandingDipModule extends Module {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 25f, 300f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Dip"));

	public LandingDipModule() {
		super("landing_dip", "Landing Dip", Category.ANIMATIONS, Icons.CHEVRON_DOWN, "A springy camera dip when you land");
	}

	private void apply() {
		CameraMotion.landDip = this.isEnabled() ? this.strength.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.landDip = 0f;
	}
}
