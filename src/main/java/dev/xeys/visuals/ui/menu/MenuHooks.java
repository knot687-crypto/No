package dev.xeys.visuals.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.TitleScreen;
import org.jspecify.annotations.Nullable;

public final class MenuHooks {
	private static boolean enabled = !Boolean.getBoolean("xeys.disableMenu");

	private MenuHooks() {
	}

	public static void setEnabled(boolean value) {
		enabled = value;
	}

	public static boolean enabled() {
		return enabled;
	}

	public static @Nullable Screen replace(@Nullable Screen screen) {
		// Exact class match: subclasses (including our own) and demo mode keep their behaviour.
		if (enabled && screen != null && screen.getClass() == TitleScreen.class && !Minecraft.getInstance().isDemo()) {
			return new MoonMenuScreen();
		}
		return screen;
	}
}
