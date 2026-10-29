package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.WorldHooks;

/** More leaves drift down from the trees around you. */
public final class FallingLeavesModule extends Module {
	private final NumberSetting amount = this.add(new NumberSetting("amount", "Amount", 400f, 150f, 2000f, 50f).unit("%")
		.describe("How many leaves fall, compared to vanilla").onChange(v -> this.apply()).group("Leaves"));

	public FallingLeavesModule() {
		super("falling_leaves", "Falling Leaves", Category.WORLD, Icons.LEAF, "More leaves drift down from trees");
	}

	private void apply() {
		if (this.isEnabled()) {
			WorldHooks.leafChance = this.amount.value() / 100f;
		}
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		WorldHooks.leafChance = 1f;
	}
}
