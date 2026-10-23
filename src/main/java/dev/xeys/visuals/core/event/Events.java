package dev.xeys.visuals.core.event;

import dev.xeys.visuals.core.module.Module;

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

	public static final Listeners<ClientTick> CLIENT_TICK = new Listeners<>();
	public static final Listeners<FrameStart> FRAME_START = new Listeners<>();

	private Events() {
	}

	public static void subscribe(Module module) {
		if (module instanceof ClientTick l) {
			CLIENT_TICK.add(l);
		}
		if (module instanceof FrameStart l) {
			FRAME_START.add(l);
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
		WorldEvents.unsubscribe(module);
	}

	public static void clientTick() {
		Object[] items = CLIENT_TICK.snapshot();
		for (Object item : items) {
			((ClientTick) item).onClientTick();
		}
	}

	public static void frameStart() {
		Object[] items = FRAME_START.snapshot();
		for (Object item : items) {
			((FrameStart) item).onFrameStart();
		}
	}
}
