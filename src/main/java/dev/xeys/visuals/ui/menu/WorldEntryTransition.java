package dev.xeys.visuals.ui.menu;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.render.ScreenFade;
import dev.xeys.visuals.render.world.CameraFx;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;

/**
 * The arrival into a world: once loading screens are gone, the view fades in from deep-space black
 * while the field of view eases out from a slight zoom, so entering the game feels continuous with the
 * menu instead of a hard cut.
 */
public final class WorldEntryTransition {
	private static final Anim ZOOM = new Anim(1f);
	private static boolean pending;

	private WorldEntryTransition() {
	}

	public static void register() {
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> pending = true);
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> pending = false);
		CameraFx.addFovModifier(partial -> ZOOM.get());
	}

	/** Per frame: starts the transition on the first frame the world is actually visible. */
	public static void frame() {
		if (!pending) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (mc.level != null && mc.player != null && mc.screen == null && mc.getOverlay() == null) {
			pending = false;
			var settings = dev.xeys.visuals.modules.client.MainMenuModule.get();
			if (MenuHooks.enabled() && (settings == null || settings.arrival.on())) {
				ScreenFade.fadeFrom(0xFF020203, 1.6f, Easing.OUT_CUBIC);
				ZOOM.restart(0.8f, 1f, 2.0f, Easing.OUT_CUBIC);
			}
		}
	}
}
