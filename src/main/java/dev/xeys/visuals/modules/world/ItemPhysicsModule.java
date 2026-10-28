package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.DroppedItems;

/** Dropped items lie flat on the ground at a natural angle instead of floating and spinning. */
public final class ItemPhysicsModule extends Module {
	public ItemPhysicsModule() {
		super("item_physics", "Item Physics", Category.WORLD, Icons.PACKAGE, "Dropped items lie flat on the ground");
	}

	@Override
	protected void onEnable() {
		DroppedItems.physics = true;
	}

	@Override
	protected void onDisable() {
		DroppedItems.physics = false;
	}
}
