package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** How much your hands lag behind as you turn, and trail behind jumps and falls. */
public final class HandSwayModule extends Module {
	private final NumberSetting sway = this.add(new NumberSetting("sway", "Turn Sway", 160f, 0f, 300f, 5f).unit("%")
		.describe("Vanilla is 100%; 0% keeps the hands locked to the view").onChange(v -> this.apply()).group("Sway"));
	private final NumberSetting inertia = this.add(new NumberSetting("inertia", "Fall Inertia", 100f, 0f, 200f, 5f).unit("%")
		.describe("The hands rise as you fall and dip as you jump").onChange(v -> this.apply()).group("Sway"));

	public HandSwayModule() {
		super("hand_sway", "Hand Sway", Category.ANIMATIONS, Icons.HAND, "Hands lag behind turns, jumps and falls");
	}

	private void apply() {
		HandFx.sway = this.isEnabled() ? this.sway.value() / 100f : 1f;
		HandFx.inertia = this.isEnabled() ? this.inertia.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		HandFx.sway = 1f;
		HandFx.inertia = 0f;
	}
}
