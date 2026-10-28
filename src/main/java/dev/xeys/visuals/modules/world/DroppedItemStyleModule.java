package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.DroppedItems;

/** Size, bobbing and spin speed of dropped items. */
public final class DroppedItemStyleModule extends Module {
	private final NumberSetting scale = this.add(new NumberSetting("scale", "Size", 125f, 50f, 250f, 5f).unit("%").onChange(v -> this.apply())
		.group("Dropped Items"));
	private final NumberSetting bob = this.add(new NumberSetting("bob", "Bobbing", 100f, 0f, 200f, 5f).unit("%").onChange(v -> this.apply())
		.group("Dropped Items"));
	private final NumberSetting spin = this.add(new NumberSetting("spin", "Spin Speed", 100f, 0f, 300f, 5f).unit("%").onChange(v -> this.apply())
		.group("Dropped Items"));

	public DroppedItemStyleModule() {
		super("dropped_item_style", "Dropped Item Style", Category.WORLD, Icons.BOX, "Bigger, calmer or livelier dropped items");
	}

	private void apply() {
		if (this.isEnabled()) {
			DroppedItems.scale = this.scale.value() / 100f;
			DroppedItems.bob = this.bob.value() / 100f;
			DroppedItems.spin = this.spin.value() / 100f;
		}
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		DroppedItems.scale = 1f;
		DroppedItems.bob = 1f;
		DroppedItems.spin = 1f;
	}
}
