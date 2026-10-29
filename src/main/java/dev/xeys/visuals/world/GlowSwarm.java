package dev.xeys.visuals.world;

/**
 * A fixed-size pool of drifting particles in struct-of-arrays form (fireflies, motes). Removing swaps
 * the last particle in, so iteration stays dense and nothing allocates per frame.
 */
public final class GlowSwarm {
	public final int capacity;
	public int count;
	public final double[] x;
	public final double[] y;
	public final double[] z;
	public final float[] vx;
	public final float[] vy;
	public final float[] vz;
	public final float[] age;
	public final float[] life;
	public final float[] size;
	public final float[] phase;

	public GlowSwarm(int capacity) {
		this.capacity = capacity;
		this.x = new double[capacity];
		this.y = new double[capacity];
		this.z = new double[capacity];
		this.vx = new float[capacity];
		this.vy = new float[capacity];
		this.vz = new float[capacity];
		this.age = new float[capacity];
		this.life = new float[capacity];
		this.size = new float[capacity];
		this.phase = new float[capacity];
	}

	/** Index of a new particle (age 0, still at rest), or -1 when the pool is full. */
	public int add(double px, double py, double pz, float lifetime, float particleSize, float particlePhase) {
		if (this.count >= this.capacity) {
			return -1;
		}
		int i = this.count++;
		this.x[i] = px;
		this.y[i] = py;
		this.z[i] = pz;
		this.vx[i] = 0f;
		this.vy[i] = 0f;
		this.vz[i] = 0f;
		this.age[i] = 0f;
		this.life[i] = lifetime;
		this.size[i] = particleSize;
		this.phase[i] = particlePhase;
		return i;
	}

	public void remove(int i) {
		int last = --this.count;
		if (i == last) {
			return;
		}
		this.x[i] = this.x[last];
		this.y[i] = this.y[last];
		this.z[i] = this.z[last];
		this.vx[i] = this.vx[last];
		this.vy[i] = this.vy[last];
		this.vz[i] = this.vz[last];
		this.age[i] = this.age[last];
		this.life[i] = this.life[last];
		this.size[i] = this.size[last];
		this.phase[i] = this.phase[last];
	}

	public void clear() {
		this.count = 0;
	}

	/** 0..1: fades in after birth and out before death over {@code seconds}. */
	public float fade(int i, float seconds) {
		float edge = Math.min(this.age[i], this.life[i] - this.age[i]) / seconds;
		return Math.max(0f, Math.min(1f, edge));
	}
}
