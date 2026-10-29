package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** The camera glides down and up when you sneak and stand instead of dropping. */
public final class SmoothSneakModule extends Module {
	private final NumberSetting smoothness = this.add(new NumberSetting("smoothness", "Smoothness", 60f, 0f, 100f, 5f).unit("%")
		.describe("0% is vanilla").onChange(v -> this.apply()).group("Sneak"));

	public SmoothSneakModule() {
		super("smooth_sneak", "Smooth Sneak", Category.ANIMATIONS, Icons.PERSON_STANDING, "Glide the camera when sneaking");
	}

	private void apply() {
		// Vanilla moves the eye half way to its target each tick.
		AnimHooks.eyeFollow = this.isEnabled() ? 0.5f - 0.42f * this.smoothness.value() / 100f : 0.5f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.eyeFollow = 0.5f;
	}
}
