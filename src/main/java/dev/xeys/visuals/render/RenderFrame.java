package dev.xeys.visuals.render;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.render.blur.SceneBlur;

/** Per-frame lifecycle hooks, called from {@code GameRenderer#render}. */
public final class RenderFrame {
	private static boolean sceneBlurRequested;

	private RenderFrame() {
	}

	public static void beginFrame() {
		Clock.tick();
		Pool.resetAll();
		ClipStack.reset();
		sceneBlurRequested = false;
	}

	/** Called by glass elements: the scene behind the GUI must be blurred before the GUI is flushed. */
	public static void requestSceneBlur() {
		sceneBlurRequested = true;
	}

	public static boolean sceneBlurRequested() {
		return sceneBlurRequested;
	}

	/** Runs after the world (and any immediate background) is drawn, right before the GUI flush. */
	public static void beforeGuiFlush() {
		if (sceneBlurRequested) {
			SceneBlur.process();
		}
	}
}
