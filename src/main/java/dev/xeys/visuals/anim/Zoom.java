package dev.xeys.visuals.anim;

import dev.xeys.visuals.core.Keybinds;
import dev.xeys.visuals.render.world.CameraFx;
import net.minecraft.client.Minecraft;

/**
 * Smooth zoom while the zoom key is held: the field of view eases in and out, the wheel adjusts how far,
 * and mouse sensitivity drops in proportion so aiming stays steady.
 */
public final class Zoom {
	public static boolean enabled;
	/** The zoom the key starts at; the wheel changes {@link #level} from there. */
	public static float configured = 4f;
	public static float level = 4f;
	public static float speed = 1f;
	public static boolean scrollable = true;
	public static boolean steadyAim = true;
	/** Look smoothing while zoomed in. */
	public static boolean cinematic;
	public static boolean hideHand;
	/** Releasing the key forgets wheel adjustments. */
	public static boolean resetOnRelease = true;
	private static float current = 1f;
	private static float target = 1f;
	private static boolean held;
	private static boolean registered;

	private Zoom() {
	}

	public static void register() {
		if (!registered) {
			registered = true;
			CameraFx.addFovModifier(partial -> 1f / current);
		}
	}

	/** Per frame: eases toward the zoom while the key is held, back to 1 when released. */
	public static void frame() {
		boolean down = enabled && Keybinds.zoom != null && Keybinds.zoom.isDown() && Minecraft.getInstance().screen == null;
		if (held && !down && resetOnRelease) {
			level = configured;
		}
		held = down;
		target = down ? level : 1f;
		// Ease in log space so zooming in and out feel equally quick.
		float k = Clock.smoothing(12f * speed);
		double log = Math.log(current) + (Math.log(target) - Math.log(current)) * k;
		current = (float) Math.exp(log);
		if (Math.abs(current - target) < 1e-3f) {
			current = target;
		}
		AnimHooks.sensitivity = steadyAim ? 1f / current : 1f;
	}

	public static boolean zooming() {
		return target > 1f;
	}

	/** How far into the zoom the view is, 0 to 1. */
	private static float amount() {
		return level > 1f ? Math.max(0f, Math.min(1f, (current - 1f) / (level - 1f))) : 0f;
	}

	public static float lookSmoothing() {
		return enabled && cinematic ? 0.6f * amount() : 0f;
	}

	public static boolean hidesHand() {
		return enabled && hideHand && current > 1.05f;
	}

	/** Mouse wheel while zooming; true when it was used. */
	public static boolean scroll(double amount) {
		if (!enabled || !scrollable || !zooming() || amount == 0.0) {
			return false;
		}
		level = (float) Math.max(1.5, Math.min(30.0, level * Math.pow(1.18, Math.signum(amount))));
		return true;
	}

	public static void reset() {
		current = 1f;
		target = 1f;
		held = false;
		level = configured;
		AnimHooks.sensitivity = 1f;
	}
}
