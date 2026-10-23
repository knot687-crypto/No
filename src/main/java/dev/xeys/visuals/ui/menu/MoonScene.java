package dev.xeys.visuals.ui.menu;

import dev.xeys.visuals.anim.Anim;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
import dev.xeys.visuals.anim.Spring;
import java.util.Random;

/**
 * Animated state of the main-menu sky, in framebuffer pixels. Updated once per frame by the menu and
 * consumed by {@link MoonSky}.
 */
public final class MoonScene {
	public static final int METEORS = 3;
	/** One full turn every ten minutes: slow enough to feel majestic, fast enough to notice. */
	private static final double ROTATION_PERIOD = 600.0;

	private final Random random = new Random();
	private final Spring parallaxX = new Spring(0f, 40f, 1f);
	private final Spring parallaxY = new Spring(0f, 40f, 1f);
	private final Anim intro = new Anim(0f);
	private final Anim moonIn = new Anim(0f);
	private final Anim exit = new Anim(0f);
	/** Shared so the moon's rotation is continuous across every screen that shows it. */
	private static final double ROTATION_OFFSET = -Clock.time() * (Math.PI * 2.0 / ROTATION_PERIOD);

	private final float[] mx = new float[METEORS];
	private final float[] my = new float[METEORS];
	private final float[] mAngle = new float[METEORS];
	private final float[] mSpeed = new float[METEORS];
	private final float[] mLength = new float[METEORS];
	private final float[] mWidth = new float[METEORS];
	private final float[] mAge = new float[METEORS];
	private final float[] mLife = new float[METEORS];
	private double nextMeteor;

	private boolean meteorsEnabled = true;
	private float width = 1920f;
	private float height = 1080f;

	public MoonScene() {
		this.nextMeteor = Clock.time() + 2.5 + this.random.nextDouble() * 3.0;
	}

	/** Plays the entrance: a cinematic version on first launch, a quick one when returning. */
	public void playIntro(boolean cinematic) {
		if (cinematic) {
			this.intro.restart(0f, 1f, 1.6f, Easing.OUT_CUBIC);
			this.moonIn.restart(0f, 1f, 2.4f, Easing.OUT_EXPO);
		} else {
			this.intro.restart(0.35f, 1f, 0.5f, Easing.OUT_CUBIC);
			this.moonIn.restart(0.6f, 1f, 0.7f, Easing.OUT_CUBIC);
		}
		this.exit.snap(0f);
	}

	public MoonScene meteors(boolean enabled) {
		this.meteorsEnabled = enabled;
		return this;
	}

	public void playExit(float seconds) {
		this.exit.restart(0f, 1f, seconds, Easing.IN_OUT_CUBIC);
	}

	public float exitProgress() {
		return this.exit.get();
	}

	public float introProgress() {
		return this.intro.get();
	}

	/** Call once per frame with the framebuffer size and the cursor offset from center in [-1, 1]. */
	public void update(float fbWidth, float fbHeight, float cursorX, float cursorY) {
		this.width = fbWidth;
		this.height = fbHeight;
		float res = fbHeight / 1080f;
		this.parallaxX.target(-cursorX * 26f * res);
		this.parallaxY.target(-cursorY * 18f * res);
		this.updateMeteors(res);
	}

	private void updateMeteors(float res) {
		float dt = Clock.delta();
		for (int i = 0; i < METEORS; i++) {
			if (this.mLife[i] > 0f) {
				this.mAge[i] += dt;
				this.mx[i] += (float) Math.cos(this.mAngle[i]) * this.mSpeed[i] * dt;
				this.my[i] += (float) Math.sin(this.mAngle[i]) * this.mSpeed[i] * dt;
				if (this.mAge[i] >= this.mLife[i]) {
					this.mLife[i] = 0f;
				}
			}
		}
		if (this.meteorsEnabled && Clock.time() >= this.nextMeteor && this.exit.get() <= 0f) {
			this.spawnMeteor(res);
			this.nextMeteor = Clock.time() + 4.0 + this.random.nextDouble() * 7.0;
		}
	}

	private void spawnMeteor(float res) {
		for (int i = 0; i < METEORS; i++) {
			if (this.mLife[i] <= 0f) {
				boolean leftToRight = this.random.nextBoolean();
				float angleDeg = 18f + this.random.nextFloat() * 22f;
				this.mAngle[i] = (float) Math.toRadians(leftToRight ? angleDeg : 180f - angleDeg);
				this.mx[i] = this.width * (leftToRight ? 0.05f + this.random.nextFloat() * 0.45f : 0.5f + this.random.nextFloat() * 0.45f);
				this.my[i] = this.height * (0.04f + this.random.nextFloat() * 0.3f);
				this.mSpeed[i] = (950f + this.random.nextFloat() * 750f) * res;
				this.mLength[i] = (150f + this.random.nextFloat() * 170f) * res;
				this.mWidth[i] = (0.9f + this.random.nextFloat() * 0.5f) * Math.max(res, 0.8f);
				this.mLife[i] = 0.55f + this.random.nextFloat() * 0.5f;
				this.mAge[i] = 0f;
				return;
			}
		}
	}

	// ------------------------------------------------------------------ values read by MoonSky

	public float time() {
		return Clock.time(3600.0);
	}

	public float brightness() {
		return this.intro.get() * (1f - this.exit.get());
	}

	public float moonVisibility() {
		return this.moonIn.get();
	}

	private float zoom() {
		float e = this.exit.get();
		float in = this.moonIn.get();
		return (0.94f + 0.06f * in) * (1f + 0.42f * e * e);
	}

	public float moonX() {
		return this.width * 0.5f + this.parallaxX.get() * 0.35f;
	}

	public float moonY() {
		return this.height * 0.40f + this.parallaxY.get() * 0.35f + (1f - this.moonIn.get()) * this.height * 0.02f;
	}

	public float moonRadius() {
		return this.height * 0.26f * this.zoom();
	}

	public float rotation() {
		// Starts with the familiar near side facing us.
		return (float) ((Clock.time() * (Math.PI * 2.0 / ROTATION_PERIOD) + ROTATION_OFFSET) % (Math.PI * 2.0));
	}

	public float tilt() {
		return 0.11f;
	}

	// Sun from the upper left and slightly in front: a waning gibbous with a crisp terminator.
	public float sunX() {
		return -0.78f;
	}

	public float sunY() {
		return 0.30f;
	}

	public float sunZ() {
		return 0.55f;
	}

	public float parallaxX() {
		return this.parallaxX.get();
	}

	public float parallaxY() {
		return this.parallaxY.get();
	}

	public float starBrightness() {
		return 1.0f;
	}

	public float glow() {
		return 1.0f;
	}

	public float relief() {
		return 1.7f;
	}

	public float exposure() {
		return 2.35f;
	}

	public float nebula() {
		return 1.0f;
	}

	public float meteorX(int i) {
		return this.mx[i];
	}

	public float meteorY(int i) {
		return this.my[i];
	}

	public float meteorAngle(int i) {
		return this.mAngle[i];
	}

	public float meteorAlpha(int i) {
		if (this.mLife[i] <= 0f) {
			return 0f;
		}
		float t = this.mAge[i] / this.mLife[i];
		float in = Math.min(t / 0.12f, 1f);
		float out = t > 0.55f ? 1f - (t - 0.55f) / 0.45f : 1f;
		return Math.max(0f, in * out);
	}

	public float meteorLength(int i) {
		float t = this.mLife[i] <= 0f ? 0f : this.mAge[i] / this.mLife[i];
		return this.mLength[i] * Math.min(1f, 0.35f + t * 1.6f);
	}

	public float meteorWidth(int i) {
		return this.mWidth[i];
	}
}
