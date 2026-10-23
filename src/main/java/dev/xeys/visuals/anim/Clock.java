package dev.xeys.visuals.anim;

/**
 * Frame clock. Sampled once per frame so every animation in a frame sees the same instant, which is what
 * keeps grouped motion perfectly in sync.
 */
public final class Clock {
	private static final long START = System.nanoTime();
	private static long last = START;
	private static double time;
	private static float delta = 1f / 60f;
	private static long frame;

	private Clock() {
	}

	public static void tick() {
		long now = System.nanoTime();
		// Clamp so a long stall (loading, alt-tab) never makes animations jump.
		delta = (float) Math.min(Math.max((now - last) / 1.0e9, 0.0), 0.1);
		last = now;
		time = (now - START) / 1.0e9;
		frame++;
	}

	/** Seconds since start, stable within a frame. */
	public static double time() {
		return time;
	}

	/** Time wrapped to a period, for shader-friendly float precision. */
	public static float time(double period) {
		return (float) (time % period);
	}

	/** Seconds elapsed since the previous frame (clamped to 100 ms). */
	public static float delta() {
		return delta;
	}

	public static long frame() {
		return frame;
	}

	/** Exponential smoothing factor for a given response speed, frame-rate independent. */
	public static float smoothing(float speed) {
		return 1f - (float) Math.exp(-speed * delta);
	}
}
