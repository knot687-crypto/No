package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;

/** A 2D screen effect: while enabled it adds its part to the combined screen-effects pass. */
abstract class ScreenEffectModule extends Module implements Events.ScreenEffect {
	protected ScreenEffectModule(String id, String name, char icon, String description) {
		super(id, name, Category.SCREEN, icon, description);
	}
}
