package dev.xeys.visuals.anim;

import dev.xeys.visuals.util.Colors;

/** A color that transitions perceptually (OKLab) between targets. */
public final class AnimatedColor {
	private final Anim t = new Anim(1f);
	private int from;
	private int to;
	private float seconds;

	public AnimatedColor(int initial, float seconds) {
		this.from = initial;
		this.to = initial;
		this.seconds = seconds;
	}

	public AnimatedColor to(int target) {
		if (target == this.to) {
			return this;
		}
		this.from = this.get();
		this.to = target;
		this.t.restart(0f, 1f, this.seconds, Easing.OUT_CUBIC);
		return this;
	}

	public AnimatedColor snap(int color) {
		this.from = color;
		this.to = color;
		this.t.snap(1f);
		return this;
	}

	public int get() {
		float p = this.t.get();
		return p >= 1f ? this.to : Colors.lerpOklab(this.from, this.to, p);
	}
}
