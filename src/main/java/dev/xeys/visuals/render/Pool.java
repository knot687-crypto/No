package dev.xeys.visuals.render;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Per-frame object pool. Render states handed to the GUI renderer live until the end-of-frame flush, so
 * every pool is rewound at the start of the next frame and its objects reused: steady-state rendering
 * allocates nothing.
 */
public final class Pool<T> {
	private static final List<Pool<?>> POOLS = new ArrayList<>();

	private final Supplier<T> factory;
	private final ArrayList<T> items = new ArrayList<>();
	private int used;

	private Pool(Supplier<T> factory) {
		this.factory = factory;
	}

	public static <T> Pool<T> create(Supplier<T> factory) {
		Pool<T> pool = new Pool<>(factory);
		POOLS.add(pool);
		return pool;
	}

	public T obtain() {
		if (this.used == this.items.size()) {
			this.items.add(this.factory.get());
		}
		return this.items.get(this.used++);
	}

	static void resetAll() {
		for (int i = 0, n = POOLS.size(); i < n; i++) {
			POOLS.get(i).used = 0;
		}
	}
}
