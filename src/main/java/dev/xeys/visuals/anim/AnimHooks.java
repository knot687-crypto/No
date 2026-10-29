package dev.xeys.visuals.anim;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * Small camera, input and entity animation settings read by the vanilla hooks. Neutral values change
 * nothing; each animation module writes its own and restores it when switched off.
 */
public final class AnimHooks {
	public enum DeathStyle {
		SINK,
		SHRINK,
		ASCEND,
		SPIN
	}

	/** View bobbing amount for the world and for the hand (vanilla 1). */
	public static float worldBob = 1f;
	public static float handBob = 1f;
	/** Bob even when vanilla's View Bobbing option is off. */
	public static boolean forceBob;
	/** The amount for whatever is being bobbed right now; set around each vanilla bob. */
	public static float bobScale = 1f;
	/** Damage tilt strength (vanilla 1). */
	public static float hurtTilt = 1f;
	/** How fast the eye height follows sneaking each tick (vanilla 0.5). */
	public static float eyeFollow = 0.5f;
	/** Third-person camera distance multiplier, eased by the perspective glide. */
	public static float cameraDistance = 1f;
	/** Mouse look smoothing: 0 is raw input, toward 1 is heavier (cinematic). */
	public static float lookSmoothing;
	/** Mouse sensitivity multiplier (zoom lowers it). */
	public static float sensitivity = 1f;
	/** Entities squash when hurt (0 = off). */
	public static float squash;
	/** How dying entities leave; null keeps vanilla's tip-over. */
	public static @Nullable DeathStyle deathStyle;
	/** Armor flashes red with its wearer when hurt, as it used to. */
	public static boolean redArmor;
	/** Swing speed for the local player (visual only). */
	public static float swingSpeed = 1f;

	private static double pendingX;
	private static double pendingY;

	private AnimHooks() {
	}

	/** Turns the view: applies sensitivity and look smoothing to one frame's mouse motion. */
	public static void turn(Player player, double dx, double dy, double frameSeconds) {
		dx *= sensitivity;
		dy *= sensitivity;
		float smoothing = Math.max(lookSmoothing, Zoom.lookSmoothing());
		if (smoothing <= 0f) {
			if (pendingX != 0.0 || pendingY != 0.0) {
				dx += pendingX;
				dy += pendingY;
				pendingX = 0.0;
				pendingY = 0.0;
			}
			player.turn(dx, dy);
			return;
		}
		// Every bit of input is applied eventually; smoothing only spreads it over a few frames.
		pendingX += dx;
		pendingY += dy;
		double rate = 4.0 + (1.0 - smoothing) * 40.0;
		double k = 1.0 - Math.exp(-rate * Math.max(frameSeconds, 1e-4));
		double ox = pendingX * k;
		double oy = pendingY * k;
		pendingX -= ox;
		pendingY -= oy;
		player.turn(ox, oy);
	}

	public static boolean isSelf(Object entity) {
		return entity == Minecraft.getInstance().player;
	}
}
