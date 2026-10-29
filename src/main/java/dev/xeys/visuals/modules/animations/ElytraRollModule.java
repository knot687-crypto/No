package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** While gliding, the camera banks into turns like an aircraft. */
public final class ElytraRollModule extends Module {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 25f, 300f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Roll"));

	public ElytraRollModule() {
		super("elytra_roll", "Elytra Roll", Category.ANIMATIONS, Icons.PLANE, "Bank into turns while gliding");
	}

	private void apply() {
		CameraMotion.elytraRoll = this.isEnabled() ? this.strength.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.elytraRoll = 0f;
	}
}
