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
		dev.xeys.visuals.core.combat.HitTracker.frame();
		dev.xeys.visuals.anim.Zoom.frame();
		dev.xeys.visuals.anim.Emotes.frame();
		dev.xeys.visuals.anim.HandFx.frame();
		dev.xeys.visuals.anim.CameraMotion.frame();
		dev.xeys.visuals.core.event.Events.frameStart();
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

	/** Runs right after the level is drawn, while its depth buffer is intact and before the hand. */
	public static void afterLevel() {
		dev.xeys.visuals.render.post.WorldPost.process();
	}

	/** Runs after the world (and any immediate background) is drawn, right before the GUI flush. */
	public static void beforeGuiFlush(GuiRenderState state) {
		// Screen effects grade the world image first, so glass panels blur the graded result.
		dev.xeys.visuals.render.post.ScreenEffects.process();
		dev.xeys.visuals.ui.notify.Notifications.render(state);
		ScreenFade.render(state);
		if (sceneBlurRequested) {
			SceneBlur.process();
		}
	}
}
