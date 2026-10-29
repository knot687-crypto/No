package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** The camera leans gently toward the side you strafe to. */
public final class StrafeTiltModule extends Module {
	private final NumberSetting tilt = this.add(new NumberSetting("tilt", "Tilt", 2.5f, 0.5f, 8f, 0.5f).unit("°").decimals(1)
		.describe("Lean at full sprinting speed").onChange(v -> this.apply()).group("Tilt"));

	public StrafeTiltModule() {
		super("strafe_tilt", "Strafe Tilt", Category.ANIMATIONS, Icons.AXIS_3D, "The camera leans as you strafe");
	}

	private void apply() {
		CameraMotion.strafeTilt = this.isEnabled() ? this.tilt.value() : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.strafeTilt = 0f;
	}
}
