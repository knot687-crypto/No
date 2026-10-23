package dev.xeys.visuals.core.event;

import java.util.Arrays;

/**
 * Copy-on-write listener list. Dispatch iterates a plain array snapshot (no iterator allocation), and
 * listeners may safely unregister themselves while being dispatched.
 */
public final class Listeners<T> {
	private Object[] items = new Object[0];

	public synchronized void add(T listener) {
		for (Object o : this.items) {
			if (o == listener) {
				return;
			}
		}
		Object[] next = Arrays.copyOf(this.items, this.items.length + 1);
		next[next.length - 1] = listener;
		this.items = next;
	}

	public synchronized void remove(T listener) {
		for (int i = 0; i < this.items.length; i++) {
			if (this.items[i] == listener) {
				Object[] next = new Object[this.items.length - 1];
				System.arraycopy(this.items, 0, next, 0, i);
				System.arraycopy(this.items, i + 1, next, i, this.items.length - i - 1);
				this.items = next;
				return;
			}
		}
	}

	/** Snapshot for dispatch: iterate with an index loop and cast each element. */
	public Object[] snapshot() {
		return this.items;
	}

	public boolean isEmpty() {
		return this.items.length == 0;
	}
}
