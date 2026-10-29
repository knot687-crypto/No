package dev.xeys.visuals.modules.cosmetics;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.cosmetic.CosmeticHooks;
import dev.xeys.visuals.ui.Icons;

/** Shrink or grow your own model, as you see it (hitboxes and the camera are unchanged). */
public final class PlayerScaleModule extends Module {
	private final NumberSetting scale = this.add(new NumberSetting("scale", "Scale", 60f, 30f, 150f, 5f).unit("%").onChange(v -> this.apply())
		.group("Model"));

	public PlayerScaleModule() {
		super("player_scale", "Mini Me", Category.COSMETICS, Icons.MINIMIZE_2, "Shrink or grow your own model (visual only)");
	}

	private void apply() {
		CosmeticHooks.selfScale = this.isEnabled() ? this.scale.value() / 100f : 1f;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CosmeticHooks.selfScale = 1f;
	}
}
