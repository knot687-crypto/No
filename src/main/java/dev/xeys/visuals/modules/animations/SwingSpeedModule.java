package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** How fast your own swing animation plays. Only the animation changes; attacks are untouched. */
public final class SwingSpeedModule extends Module {
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 75f, 25f, 300f, 5f).unit("%")
		.describe("Below 100% the swing is slower and smoother").onChange(v -> this.apply()).group("Swing"));

	public SwingSpeedModule() {
		super("swing_speed", "Swing Speed", Category.ANIMATIONS, Icons.FAST_FORWARD, "Slow down or speed up your swing animation");
	}

	private void apply() {
		AnimHooks.swingSpeed = this.isEnabled() ? this.speed.value() / 100f : 1f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.swingSpeed = 1f;
	}
}
