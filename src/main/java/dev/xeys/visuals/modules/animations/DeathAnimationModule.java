package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;

/** Dying creatures sink into the ground, shrink away, float up or spin out instead of tipping over. */
public final class DeathAnimationModule extends Module {
	private final EnumSetting<AnimHooks.DeathStyle> style = this.add(new EnumSetting<>("style", "Style", AnimHooks.DeathStyle.SINK)
		.onChange(v -> this.apply()).group("Death"));

	public DeathAnimationModule() {
		super("death_animation", "Death Animation", Category.ANIMATIONS, Icons.GHOST, "Sink, shrink, ascend or spin out on death");
	}

	private void apply() {
		AnimHooks.deathStyle = this.isEnabled() ? this.style.get() : null;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.deathStyle = null;
	}
}
