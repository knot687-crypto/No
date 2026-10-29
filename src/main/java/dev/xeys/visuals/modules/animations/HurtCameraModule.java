package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** How the camera reacts when you are hurt: a stronger or weaker tilt, and a head snap. */
public final class HurtCameraModule extends Module {
	private final NumberSetting tilt = this.add(new NumberSetting("tilt", "Tilt", 160f, 0f, 300f, 5f).unit("%")
		.describe("Vanilla's damage tilt is 100%").onChange(v -> this.apply()).group("Hurt"));
	private final NumberSetting kick = this.add(new NumberSetting("kick", "Head Snap", 2f, 0f, 6f, 0.25f).unit("°").decimals(2)
		.describe("The view snaps up a little when hit").onChange(v -> this.apply()).group("Hurt"));

	public HurtCameraModule() {
		super("hurt_camera", "Hurt Camera", Category.ANIMATIONS, Icons.CIRCLE_ALERT, "Stronger hurt tilt and a head snap when hit");
	}

	private void apply() {
		AnimHooks.hurtTilt = this.isEnabled() ? this.tilt.value() / 100f : 1f;
		CameraMotion.hurtKick = this.isEnabled() ? this.kick.value() : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.hurtTilt = 1f;
		CameraMotion.hurtKick = 0f;
	}
}
