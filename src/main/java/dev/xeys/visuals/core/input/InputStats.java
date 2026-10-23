package dev.xeys.visuals.core.input;

/** Click timestamps for CPS readouts. Fed by the mouse mixin. */
public final class InputStats {
	private static final int CAPACITY = 64;
	private static final long[] LEFT = new long[CAPACITY];
	private static final long[] RIGHT = new long[CAPACITY];
	private static int leftHead;
	private static int rightHead;

	private InputStats() {
	}

	public static void onClick(int button) {
		long now = System.nanoTime();
		if (button == 0) {
			LEFT[leftHead] = now;
			leftHead = (leftHead + 1) % CAPACITY;
		} else if (button == 1) {
			RIGHT[rightHead] = now;
			rightHead = (rightHead + 1) % CAPACITY;
		}
	}

	public static int leftCps() {
		return count(LEFT);
	}

	public static int rightCps() {
		return count(RIGHT);
	}

	private static int count(long[] buffer) {
		long cutoff = System.nanoTime() - 1_000_000_000L;
		int n = 0;
		for (long t : buffer) {
			if (t != 0L && t >= cutoff) {
				n++;
			}
		}
		return n;
	}
}
