package dev.xeys.visuals.core.event;

import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.render.post.ScreenFx;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Typed event hubs. A module receives an event simply by implementing the interface; it is subscribed
 * only while enabled, so disabled modules cost nothing per frame.
 */
public final class Events {
	/** End of every client tick (20 Hz), in or out of a world. */
	public interface ClientTick {
		void onClientTick();
	}

	/** Every frame, before GUI extraction. */
	public interface FrameStart {
		void onFrameStart();
	}

	/** Adds to the combined screen-effects pass (world image only, under the HUD). */
	public interface ScreenEffect {
		void contribute(ScreenFx fx, float partialTick);
	}

	/** 2D effects drawn over the world, under the HUD elements (hit markers, damage indicators...). */
	public interface Overlay {
		void renderOverlay(GuiGraphics g, float partialTick);

		/** Cards and alerts that must not hide behind HUD elements draw after them. */
		default boolean aboveHud() {
			return false;
		}
	}

	public static final Listeners<ClientTick> CLIENT_TICK = new Listeners<>();
	public static final Listeners<FrameStart> FRAME_START = new Listeners<>();
	public static final Listeners<ScreenEffect> SCREEN_FX = new Listeners<>();
	public static final Listeners<Overlay> OVERLAY = new Listeners<>();

	private Events() {
	}

	public static void subscribe(Module module) {
		if (module instanceof ClientTick l) {
			CLIENT_TICK.add(l);
		}
		if (module instanceof FrameStart l) {
			FRAME_START.add(l);
		}
		if (module instanceof ScreenEffect l) {
			SCREEN_FX.add(l);
		}
		if (module instanceof Overlay l) {
			OVERLAY.add(l);
		}
		WorldEvents.subscribe(module);
	}

	public static void unsubscribe(Module module) {
		if (module instanceof ClientTick l) {
			CLIENT_TICK.remove(l);
		}
		if (module instanceof FrameStart l) {
			FRAME_START.remove(l);
		}
		if (module instanceof ScreenEffect l) {
			SCREEN_FX.remove(l);
		}
		if (module instanceof Overlay l) {
			OVERLAY.remove(l);
		}
		WorldEvents.unsubscribe(module);
	}

	public static void clientTick() {
		Object[] items = CLIENT_TICK.snapshot();
		for (Object item : items) {
			((ClientTick) item).onClientTick();
		}
	}

	public static void renderOverlays(GuiGraphics g, float partialTick, boolean aboveHud) {
		Object[] items = OVERLAY.snapshot();
		for (Object item : items) {
			Overlay o = (Overlay) item;
			if (o.aboveHud() == aboveHud) {
				o.renderOverlay(g, partialTick);
			}
		}
	}

	public static void frameStart() {
		Object[] items = FRAME_START.snapshot();
		for (Object item : items) {
			((FrameStart) item).onFrameStart();
		}
	}
}
