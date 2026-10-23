package dev.xeys.visuals.modules.client;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Glass toast notifications (module toggles, profile actions). */
public final class NotificationsModule extends Module {
	public enum Corner {
		TOP_RIGHT,
		BOTTOM_RIGHT,
		TOP_CENTER
	}

	public final BoolSetting moduleToggles = this.add(new BoolSetting("module_toggles", "Module Toggles", true)
		.describe("Show a toast when a module is turned on or off"));
	public final EnumSetting<Corner> corner = this.add(new EnumSetting<>("corner", "Position", Corner.TOP_RIGHT));
	public final NumberSetting duration = this.add(new NumberSetting("duration", "Duration", 2.4f, 1f, 6f, 0.2f).unit("s"));

	private static NotificationsModule instance;

	public NotificationsModule() {
		super("notifications", "Notifications", Category.CLIENT, Icons.BELL, "Glass toasts for module toggles and client events");
		this.enabledByDefault();
		instance = this;
	}

	public static NotificationsModule get() {
		return instance;
	}
}
