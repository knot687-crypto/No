package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/**
 * Lightly smoothed mouse look: every movement still arrives in full, spread over a few frames. Lighter
 * than vanilla's cinematic camera and steady at any frame rate.
 */
public final class SmoothLookModule extends Module {
	private final NumberSetting smoothing = this.add(new NumberSetting("smoothing", "Smoothing", 35f, 5f, 100f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Look"));

	public SmoothLookModule() {
		super("smooth_look", "Smooth Look", Category.ANIMATIONS, Icons.MOUSE, "Gently smoothed camera turning");
	}

	private void apply() {
		AnimHooks.lookSmoothing = this.isEnabled() ? this.smoothing.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.lookSmoothing = 0f;
	}
}
