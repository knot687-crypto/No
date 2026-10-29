package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Creatures squash and spring back like jelly when hurt. */
public final class HitSquashModule extends Module {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 25f, 250f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Squash"));

	public HitSquashModule() {
		super("hit_squash", "Hit Squash", Category.ANIMATIONS, Icons.CUBOID, "Hurt creatures squash and spring back");
	}

	private void apply() {
		AnimHooks.squash = this.isEnabled() ? this.strength.value() / 100f : 0f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.squash = 0f;
	}
}
