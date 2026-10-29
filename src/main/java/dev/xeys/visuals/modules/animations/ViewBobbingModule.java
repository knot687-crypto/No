package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Separate walking bob for the view and for the hand, e.g. a calm view with a lively hand. */
public final class ViewBobbingModule extends Module {
	private final NumberSetting world = this.add(new NumberSetting("world", "View", 50f, 0f, 200f, 5f).unit("%")
		.describe("Bobbing of the world as you walk").onChange(v -> this.apply()).group("Bobbing"));
	private final NumberSetting hand = this.add(new NumberSetting("hand", "Hand", 100f, 0f, 200f, 5f).unit("%")
		.describe("Bobbing of your hand as you walk").onChange(v -> this.apply()).group("Bobbing"));
	private final BoolSetting always = this.add(new BoolSetting("always", "Always On", true)
		.describe("Bob even when View Bobbing is off in Video Settings").onChange(v -> this.apply()).group("Bobbing"));

	public ViewBobbingModule() {
		super("view_bobbing", "View Bobbing", Category.ANIMATIONS, Icons.FOOTPRINTS, "Tune walking bob for the view and hand");
	}

	private void apply() {
		AnimHooks.worldBob = this.isEnabled() ? this.world.value() / 100f : 1f;
		AnimHooks.handBob = this.isEnabled() ? this.hand.value() / 100f : 1f;
		AnimHooks.forceBob = this.isEnabled() && this.always.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		AnimHooks.worldBob = 1f;
		AnimHooks.handBob = 1f;
		AnimHooks.forceBob = false;
	}
}
