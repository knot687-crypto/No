package dev.xeys.visuals.anim;

/**
 * Time-based tween of a float. Retargeting mid-flight starts from the current value, so interrupted
 * transitions never jump. Driven by {@link Clock}, so it completes even while not being drawn.
 */
public final class Anim {
	private float from;
	private float to;
	private double start;
	private float duration;
	private Easing easing = Easing.OUT_CUBIC;

	public Anim(float initial) {
		this.from = initial;
		this.to = initial;
	}

	/** Animates toward {@code target} over {@code seconds}; no-op if already heading there. */
	public Anim to(float target, float seconds, Easing easing) {
		if (target == this.to) {
			return this;
		}
		this.from = this.get();
		this.to = target;
		this.start = Clock.time();
		this.duration = Math.max(seconds, 0f);
		this.easing = easing;
		return this;
	}

	public Anim to(float target, float seconds) {
		return this.to(target, seconds, Easing.OUT_CUBIC);
	}

	/** Restarts from {@code from} to {@code target} regardless of the current state. */
	public Anim restart(float from, float target, float seconds, Easing easing) {
		this.from = from;
		this.to = target;
		this.start = Clock.time();
		this.duration = Math.max(seconds, 0f);
		this.easing = easing;
		return this;
	}

	public Anim snap(float value) {
		this.from = value;
		this.to = value;
		this.duration = 0f;
		return this;
	}

	public float get() {
		if (this.duration <= 0f) {
			return this.to;
		}
		float t = (float) ((Clock.time() - this.start) / this.duration);
		if (t >= 1f) {
			return this.to;
		}
		return this.from + (this.to - this.from) * this.easing.apply(Math.max(t, 0f));
	}

	/** Linear progress in [0, 1] of the current transition. */
	public float progress() {
		if (this.duration <= 0f) {
			return 1f;
		}
		return (float) Math.max(0.0, Math.min(1.0, (Clock.time() - this.start) / this.duration));
	}

	public float target() {
		return this.to;
	}

	public boolean done() {
		return this.progress() >= 1f;
	}
}
