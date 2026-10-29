package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.mesh.DynamicMesh;
import dev.xeys.visuals.render.sky.SkyContext;
import dev.xeys.visuals.render.sky.SkyDraw;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/** Meteors streak across the night sky now and then, a bright head trailing a fading tail. */
public final class ShootingStarsModule extends Module implements WorldEvents.Sky {
	private static final int MAX = 8;
	private static final int STEPS = 6;

	private final NumberSetting rate = this.add(new NumberSetting("rate", "Frequency", 8f, 1f, 60f, 1f).unit(" / min").group("Meteors"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Length", 100f, 40f, 250f, 5f).unit("%").group("Meteors"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 100f, 40f, 250f, 5f).unit("%").group("Meteors"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Tail Color", 0xFFB9D4FF).noAlpha().group("Meteors"));
	private final BoolSetting nightOnly = this.add(new BoolSetting("night_only", "Only at Night", true).group("Meteors"));
	private final DynamicMesh mesh = new DynamicMesh("XEYS meteors", MAX * STEPS * 4);
	private final RandomSource random = RandomSource.create();
	private final Matrix4f identity = new Matrix4f();
	// Each meteor: start point (unit vector), rotation axis (unit), angular speed, arc length, life, age.
	private final float[] px = new float[MAX];
	private final float[] py = new float[MAX];
	private final float[] pz = new float[MAX];
	private final float[] ax = new float[MAX];
	private final float[] ay = new float[MAX];
	private final float[] az = new float[MAX];
	private final float[] omega = new float[MAX];
	private final float[] arc = new float[MAX];
	private final float[] life = new float[MAX];
	private final float[] age = new float[MAX];
	private final float[] bright = new float[MAX];
	private int count;

	public ShootingStarsModule() {
		super("shooting_stars", "Shooting Stars", Category.WORLD, Icons.ROCKET, "Meteors now and then streak across the night sky");
	}

	@Override
	public void onSky(SkyContext sky) {
		float visibility = (this.nightOnly.on() ? sky.night() : 1f) * sky.rainBrightness;
		float dt = Math.min(Clock.delta(), 0.1f);
		for (int i = 0; i < this.count; i++) {
			this.age[i] += dt;
			if (this.age[i] >= this.life[i]) {
				this.remove(i--);
			}
		}
		if (visibility > 0.2f && this.count < MAX && this.random.nextFloat() < this.rate.value() / 60f * dt) {
			this.spawn();
		}
		if (this.count == 0 || visibility < 0.01f || !XeysPipelines.isUsable(XeysPipelines.SKY_METEOR)) {
			return;
		}
		this.build();
		GpuBuffer buffer = this.mesh.upload();
		if (buffer != null) {
			SkyDraw.draw(XeysPipelines.SKY_METEOR, buffer, this.mesh.vertexCount(), this.identity, 0f, visibility, 0f, 0f, this.color.color(), 0);
		}
	}

	private void spawn() {
		int i = this.count++;
		double azimuth = this.random.nextDouble() * Math.PI * 2.0;
		double elevation = Math.toRadians(28.0 + this.random.nextDouble() * 40.0);
		float sx = (float) (Math.sin(azimuth) * Math.cos(elevation));
		float sy = (float) Math.sin(elevation);
		float sz = (float) (-Math.cos(azimuth) * Math.cos(elevation));
		// A random great circle through the start point, travelled downward.
		float rx = this.random.nextFloat() - 0.5f;
		float ry = this.random.nextFloat() - 0.5f;
		float rz = this.random.nextFloat() - 0.5f;
		float kx = sy * rz - sz * ry;
		float ky = sz * rx - sx * rz;
		float kz = sx * ry - sy * rx;
		float kl = (float) Math.sqrt(kx * kx + ky * ky + kz * kz);
		if (kl < 1e-3f) {
			this.count--;
			return;
		}
		kx /= kl;
		ky /= kl;
		kz /= kl;
		// The direction of travel is k x p; flip so it heads toward the horizon.
		float ty = kz * sx - kx * sz;
		if (ty > 0f) {
			kx = -kx;
			ky = -ky;
			kz = -kz;
		}
		this.px[i] = sx;
		this.py[i] = sy;
		this.pz[i] = sz;
		this.ax[i] = kx;
		this.ay[i] = ky;
		this.az[i] = kz;
		float s = this.speed.value() / 100f;
		this.omega[i] = (float) Math.toRadians(30.0 + this.random.nextDouble() * 30.0) * s;
		this.arc[i] = (float) Math.toRadians(7.0 + this.random.nextDouble() * 7.0) * this.length.value() / 100f;
		this.life[i] = 0.55f + this.random.nextFloat() * 0.6f;
		this.age[i] = 0f;
		this.bright[i] = 0.6f + this.random.nextFloat() * 0.4f;
	}

	private void remove(int i) {
		int last = --this.count;
		this.px[i] = this.px[last];
		this.py[i] = this.py[last];
		this.pz[i] = this.pz[last];
		this.ax[i] = this.ax[last];
		this.ay[i] = this.ay[last];
		this.az[i] = this.az[last];
		this.omega[i] = this.omega[last];
		this.arc[i] = this.arc[last];
		this.life[i] = this.life[last];
		this.age[i] = this.age[last];
		this.bright[i] = this.bright[last];
	}

	private void build() {
		DynamicMesh m = this.mesh.begin();
		float r = SkyDraw.RADIUS;
		float halfWidth = 0.32f;
		for (int i = 0; i < this.count; i++) {
			float t = this.age[i] / this.life[i];
			float fade = Math.min(1f, t / 0.12f) * Math.min(1f, (1f - t) / 0.35f);
			int alpha = Math.round(255f * fade * this.bright[i]);
			float head = this.omega[i] * this.age[i];
			float tail = Math.max(0f, head - this.arc[i]);
			for (int s = 0; s < STEPS; s++) {
				float a0 = tail + (head - tail) * s / STEPS;
				float a1 = tail + (head - tail) * (s + 1) / STEPS;
				float u0 = (float) s / STEPS;
				float u1 = (float) (s + 1) / STEPS;
				this.point(m, i, a0, r, -halfWidth, u0, -1f, alpha);
				this.point(m, i, a1, r, -halfWidth, u1, -1f, alpha);
				this.point(m, i, a1, r, halfWidth, u1, 1f, alpha);
				this.point(m, i, a0, r, halfWidth, u0, 1f, alpha);
			}
		}
	}

	/** The start point rotated by {@code angle} about the meteor's axis, pushed sideways by {@code side}. */
	private void point(DynamicMesh m, int i, float angle, float r, float side, float u, float v, int alpha) {
		float cos = (float) Math.cos(angle);
		float sin = (float) Math.sin(angle);
		float kx = this.ax[i];
		float ky = this.ay[i];
		float kz = this.az[i];
		float x = this.px[i];
		float y = this.py[i];
		float z = this.pz[i];
		// Rodrigues' rotation; p is perpendicular to k, so the k(k.p) term vanishes.
		float cx = ky * z - kz * y;
		float cy = kz * x - kx * z;
		float cz = kx * y - ky * x;
		float qx = x * cos + cx * sin;
		float qy = y * cos + cy * sin;
		float qz = z * cos + cz * sin;
		// Sideways is the rotation axis itself: perpendicular to both the point and its motion.
		m.vertex((qx + kx * side / r) * r, (qy + ky * side / r) * r, (qz + kz * side / r) * r, u, v, (alpha << 24) | 0xFFFFFF);
	}
}
