package dev.xeys.visuals.modules;

import dev.xeys.visuals.core.module.ModuleManager;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.modules.client.HudStyleModule;
import dev.xeys.visuals.modules.client.InterfaceModule;
import dev.xeys.visuals.modules.client.MainMenuModule;
import dev.xeys.visuals.modules.client.NotificationsModule;
import dev.xeys.visuals.modules.client.ThemeModule;

/**
 * Creates and registers every module. Direct references for the few that other systems read every
 * frame; everything else is reached through {@link ModuleManager}.
 */
public final class Modules {
	public static ThemeModule THEME;
	public static InterfaceModule INTERFACE;
	public static HudStyleModule HUD_STYLE;
	public static MainMenuModule MAIN_MENU;
	public static NotificationsModule NOTIFICATIONS;

	private Modules() {
	}

	public static void init() {
		// Client
		THEME = ModuleManager.register(new ThemeModule());
		INTERFACE = ModuleManager.register(new InterfaceModule());
		HUD_STYLE = ModuleManager.register(new HudStyleModule());
		MAIN_MENU = ModuleManager.register(new MainMenuModule());
		NOTIFICATIONS = ModuleManager.register(new NotificationsModule());
		HudStyle.bind(HUD_STYLE);

		ModuleCatalog.registerAll();
	}
}
