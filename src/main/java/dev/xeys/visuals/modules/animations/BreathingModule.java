package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Your hands rise and fall gently while you stand still, as if breathing. */
public final class BreathingModule extends Module {
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 100f, 10f, 250f, 5f).unit("%").onChange(v -> this.apply())
		.group("Breathing"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 100f, 25f, 300f, 5f).unit("%").onChange(v -> this.apply())
		.group("Breathing"));

	public BreathingModule() {
		super("breathing", "Idle Breathing", Category.ANIMATIONS, Icons.WIND, "A gentle idle sway of the hands while standing");
	}

	private void apply() {
		HandFx.breathing = this.isEnabled() ? this.amount.value() / 100f : 0f;
		HandFx.breathingSpeed = this.speed.value() / 100f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.breathing = 0f;
		HandFx.breathingSpeed = 1f;
	}
}
