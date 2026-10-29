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
import org.joml.Matrix4f;

/**
 * Northern lights: slowly swaying curtains of light hanging in the night sky, bright along their lower
 * edge and fading upward, with drifting vertical rays.
 */
public final class AuroraModule extends Module implements WorldEvents.Sky {
	private static final int CURTAINS = 3;
	private static final int SEGMENTS = 80;
	/** Per curtain: center azimuth, half span (degrees), base elevation, height, phase. */
	private static final float[][] SHAPE = {
		{-28f, 52f, 17f, 24f, 0.0f},
		{14f, 46f, 24f, 20f, 2.1f},
		{52f, 38f, 13f, 17f, 4.3f}
	};

	private final ColorSetting lower = this.add(new ColorSetting("lower", "Lower Color", 0xFF3DFFA8).noAlpha().group("Colors"));
	private final ColorSetting upper = this.add(new ColorSetting("upper", "Upper Color", 0xFF8C5CFF).noAlpha().group("Colors"));
	private final NumberSetting intensity = this.add(new NumberSetting("intensity", "Intensity", 100f, 10f, 200f, 5f).unit("%").group("Aurora"));
	private final NumberSetting height = this.add(new NumberSetting("height", "Height", 100f, 50f, 180f, 5f).unit("%").group("Aurora"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Motion", 100f, 0f, 300f, 5f).unit("%").group("Aurora"));
	private final NumberSetting direction = this.add(new NumberSetting("direction", "Direction", 0f, 0f, 355f, 5f).unit("°")
		.describe("Compass bearing of the curtains (0 is north)").group("Aurora"));
	private final BoolSetting nightOnly = this.add(new BoolSetting("night_only", "Only at Night", true).group("Aurora"));
	private final DynamicMesh mesh = new DynamicMesh("XEYS aurora", CURTAINS * SEGMENTS * 4);
	private final Matrix4f rotation = new Matrix4f();
	private double time;

	public AuroraModule() {
		super("aurora", "Aurora", Category.WORLD, Icons.WAVES, "Northern lights dance across the night sky");
	}

	@Override
	public void onSky(SkyContext sky) {
		float night = this.nightOnly.on() ? sky.night() : 1f;
		float visibility = night * sky.rainBrightness;
		if (visibility < 0.01f || !XeysPipelines.isUsable(XeysPipelines.SKY_AURORA)) {
			return;
		}
		this.time += Clock.delta() * (this.speed.value() / 100f);
		this.build((float) this.time);
		GpuBuffer buffer = this.mesh.upload();
		if (buffer == null) {
			return;
		}
		this.rotation.rotationY((float) Math.toRadians(-this.direction.value()));
		SkyDraw.draw(XeysPipelines.SKY_AURORA, buffer, this.mesh.vertexCount(), this.rotation, (float) (this.time % 10000.0),
			visibility * this.intensity.value() / 100f, 0f, 0f, this.lower.color(), this.upper.color());
	}

	/** Rebuilds the curtains for time {@code t}: each a ribbon whose base elevation waves along its length. */
	private void build(float t) {
		DynamicMesh m = this.mesh.begin();
		float r = SkyDraw.RADIUS;
		float heightScale = this.height.value() / 100f;
		for (int c = 0; c < CURTAINS; c++) {
			float[] s = SHAPE[c];
			float center = s[0];
			float span = s[1];
			float base = s[2];
			float tall = s[3] * heightScale;
			float phase = s[4];
			for (int i = 0; i < SEGMENTS; i++) {
				float u0 = (float) i / SEGMENTS;
				float u1 = (float) (i + 1) / SEGMENTS;
				this.edge(m, r, center, span, base, tall, phase, t, u0, c, true);
				this.edge(m, r, center, span, base, tall, phase, t, u1, c, true);
				this.edge(m, r, center, span, base, tall, phase, t, u1, c, false);
				this.edge(m, r, center, span, base, tall, phase, t, u0, c, false);
			}
		}
	}

	private void edge(DynamicMesh m, float r, float center, float span, float base, float tall, float phase, float t, float u, int curtain,
					  boolean bottom) {
		double az = Math.toRadians(center + (u * 2f - 1f) * span + 3.0 * Math.sin(u * 7.0 + t * 0.21 + phase));
		double wave = 4.5 * Math.sin(u * 9.0 + t * 0.35 + phase) + 2.0 * Math.sin(u * 23.0 - t * 0.6 + phase * 1.7);
		double elevation = Math.toRadians(base + wave + (bottom ? 0.0 : tall * (0.82 + 0.18 * Math.sin(u * 5.0 + t * 0.27 + phase))));
		float cos = (float) Math.cos(elevation);
		float x = (float) Math.sin(az) * cos * r;
		float y = (float) Math.sin(elevation) * r;
		float z = (float) -Math.cos(az) * cos * r;
		// Fade the ends of each curtain; alpha carries it to the shader.
		float ends = smooth(Math.min(u, 1f - u) / 0.16f);
		int alpha = Math.round(255f * ends);
		m.vertex(x, y, z, u + curtain * 3.7f, bottom ? 0f : 1f, (alpha << 24) | 0xFFFFFF);
	}

	private static float smooth(float x) {
		float c = Math.max(0f, Math.min(1f, x));
		return c * c * (3f - 2f * c);
	}
}
