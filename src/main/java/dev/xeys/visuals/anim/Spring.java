package dev.xeys.visuals.anim;

/**
 * Damped harmonic spring, integrated with fixed sub-steps so the motion is identical at 30 or 300 FPS.
 * Use for anything that follows a moving target (cursor-relative motion, drag, sliding indicators).
 */
public final class Spring {
	private static final float STEP = 1f / 480f;

	private float value;
	private float velocity;
	private float target;
	private float stiffness;
	private float damping;
	private long frame = -1L;
	private float accumulator;

	/**
	 * @param stiffness spring constant (higher = faster), e.g. 220
	 * @param dampingRatio 1 = critically damped (no overshoot), &lt; 1 = bouncy
	 */
	public Spring(float initial, float stiffness, float dampingRatio) {
		this.value = initial;
		this.target = initial;
		this.stiffness = stiffness;
		this.damping = 2f * dampingRatio * (float) Math.sqrt(stiffness);
	}

	public static Spring smooth(float initial) {
		return new Spring(initial, 260f, 1f);
	}

	public static Spring bouncy(float initial) {
		return new Spring(initial, 320f, 0.62f);
	}

	public Spring target(float target) {
		this.target = target;
		return this;
	}

	public Spring snap(float value) {
		this.value = value;
		this.target = value;
		this.velocity = 0f;
		return this;
	}

	public Spring impulse(float velocity) {
		this.velocity += velocity;
		return this;
	}

	public float target() {
		return this.target;
	}

	public float velocity() {
		this.update();
		return this.velocity;
	}

	public float get() {
		this.update();
		return this.value;
	}

	public boolean settled() {
		this.update();
		return Math.abs(this.value - this.target) < 1e-3f && Math.abs(this.velocity) < 1e-3f;
	}

	private void update() {
		long now = Clock.frame();
		if (now == this.frame) {
			return;
		}
		this.frame = now;
		this.accumulator += Clock.delta();
		int steps = 0;
		while (this.accumulator >= STEP && steps < 240) {
			float force = -this.stiffness * (this.value - this.target) - this.damping * this.velocity;
			this.velocity += force * STEP;
			this.value += this.velocity * STEP;
			this.accumulator -= STEP;
			steps++;
		}
		if (steps >= 240) {
			this.accumulator = 0f;
		}
		if (Math.abs(this.value - this.target) < 1e-4f && Math.abs(this.velocity) < 1e-4f) {
			this.value = this.target;
			this.velocity = 0f;
		}
	}
}
