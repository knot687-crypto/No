package dev.xeys.visuals.ui.menu;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;

/**
 * Replaces the vanilla panorama behind every menu screen (world list, options, multiplayer...) with the
 * moon sky, so the whole pre-game experience shares one backdrop. Vanilla then applies its own menu
 * blur on top, which frosts the moon behind those screens.
 */
public final class MoonBackdrop {
	private static final MoonScene SCENE = new MoonScene().meteors(false);
	/** Refresh rate of the cached backdrop; it sits under vanilla's blur, so motion detail is invisible. */
	private static final float REFRESH_HZ = 12f;
	private static @Nullable Screen lastScreen;

	private MoonBackdrop() {
	}

	/** Draws the backdrop; returns false when the panorama should be drawn instead. */
	public static boolean render() {
		var settings = dev.xeys.visuals.modules.client.MainMenuModule.get();
		if (!MenuHooks.enabled() || (settings != null && !settings.backdrop.on()) || Boolean.getBoolean("xeys.noBackdrop")) {
			return false;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.screen != lastScreen) {
			lastScreen = mc.screen;
			SCENE.playIntro(false);
		}
		var window = mc.getWindow();
		float cx = (float) (mc.mouseHandler.xpos() / Math.max(1, window.getScreenWidth())) * 2f - 1f;
		float cy = (float) (mc.mouseHandler.ypos() / Math.max(1, window.getScreenHeight())) * 2f - 1f;
		SCENE.update(window.getWidth(), window.getHeight(), cx * 0.5f, cy * 0.5f);
		return MoonSky.drawCached(SCENE, REFRESH_HZ);
	}
}
