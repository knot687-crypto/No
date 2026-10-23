package dev.xeys.visuals.render;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.render.blur.SceneBlur;
import dev.xeys.visuals.ui.menu.WorldEntryTransition;
import net.minecraft.client.gui.render.state.GuiRenderState;

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
		WorldEntryTransition.frame();
		if (DEBUG_FRAMES && Clock.frame() % 30 == 0) {
			net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
			dev.xeys.visuals.Xeys.LOG.info("frame {} screen {} fps {}", Clock.frame(),
				mc.screen == null ? "null" : mc.screen.getClass().getSimpleName(), mc.getFps());
		}
	}

	private static final boolean DEBUG_FRAMES = Boolean.getBoolean("xeys.debugFrames");

	/** Called by glass elements: the scene behind the GUI must be blurred before the GUI is flushed. */
	public static void requestSceneBlur() {
		sceneBlurRequested = true;
	}

	public static boolean sceneBlurRequested() {
		return sceneBlurRequested;
	}

	/** Runs after the world (and any immediate background) is drawn, right before the GUI flush. */
	public static void beforeGuiFlush(GuiRenderState state) {
		ScreenFade.render(state);
		if (sceneBlurRequested) {
			SceneBlur.process();
		}
	}
}
