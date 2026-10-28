package dev.xeys.visuals.world;

import dev.xeys.visuals.anim.Clock;
import net.minecraft.world.phys.AABB;

/**
 * The animated box around the targeted block, shared by the outline and overlay so they glide together.
 * Kept in doubles (world coordinates reach millions) and integrated as a critically damped spring.
 */
public final class BlockTarget {
	private static final double[] VALUE = new double[6];
	private static final double[] VELOCITY = new double[6];
	private static final double[] TARGET = new double[6];
	private static long frame = -1L;
	private static boolean valid;
	private static long lastSeenFrame = -10L;

	private BlockTarget() {
	}

	/** Updates once per frame toward {@code box}; {@code glide} false snaps. */
	public static void update(AABB box, boolean glide, float stiffness) {
		if (frame == Clock.frame()) {
			return;
		}
		boolean continuous = lastSeenFrame >= Clock.frame() - 2;
		frame = Clock.frame();
		lastSeenFrame = frame;
		TARGET[0] = box.minX;
		TARGET[1] = box.minY;
		TARGET[2] = box.minZ;
		TARGET[3] = box.maxX;
		TARGET[4] = box.maxY;
		TARGET[5] = box.maxZ;
		if (!glide || !valid || !continuous) {
			for (int i = 0; i < 6; i++) {
				VALUE[i] = TARGET[i];
				VELOCITY[i] = 0.0;
			}
			valid = true;
			return;
		}
		double dt = Math.min(Clock.delta(), 0.05);
		int steps = Math.max(1, (int) Math.ceil(dt / (1.0 / 240.0)));
		double h = dt / steps;
		double k = stiffness;
		double c = 2.0 * Math.sqrt(k);
		for (int s = 0; s < steps; s++) {
			for (int i = 0; i < 6; i++) {
				double a = k * (TARGET[i] - VALUE[i]) - c * VELOCITY[i];
				VELOCITY[i] += a * h;
				VALUE[i] += VELOCITY[i] * h;
			}
		}
	}

	/** Whether the box is still moving toward its target. */
	public static boolean moving() {
		double m = 0.0;
		for (int i = 0; i < 6; i++) {
			m = Math.max(m, Math.abs(TARGET[i] - VALUE[i]));
		}
		return m > 0.002;
	}

	public static double minX() {
		return VALUE[0];
	}

	public static double minY() {
		return VALUE[1];
	}

	public static double minZ() {
		return VALUE[2];
	}

	public static double maxX() {
		return VALUE[3];
	}

	public static double maxY() {
		return VALUE[4];
	}

	public static double maxZ() {
		return VALUE[5];
	}
}
