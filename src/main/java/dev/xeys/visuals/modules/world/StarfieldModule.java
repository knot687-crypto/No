package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.buffers.GpuBuffer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.mesh.DynamicMesh;
import dev.xeys.visuals.render.sky.SkyContext;
import dev.xeys.visuals.render.sky.SkyDraw;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.util.RandomSource;
import org.joml.Matrix4f;

/**
 * A richer night sky: thousands of soft, twinkling stars in true star colors, turning with vanilla's
 * sky. The mesh is built once per density change; twinkling happens on the GPU.
 */
public final class StarfieldModule extends Module implements WorldEvents.Sky {
	/** Star colors from hot to cool, weighted toward white. */
	private static final int[] COLORS = {0xFF9DB8FF, 0xFFC4D4FF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFF4E0, 0xFFFFE2B8, 0xFFFFC98F};

	private final NumberSetting density = this.add(new NumberSetting("density", "Density", 2000f, 500f, 5000f, 100f).onChange(v -> this.dirty = true)
		.group("Stars"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 50f, 250f, 5f).unit("%").group("Stars"));
	private final NumberSetting brightness = this.add(new NumberSetting("brightness", "Brightness", 100f, 20f, 200f, 5f).unit("%").group("Stars"));
	private final NumberSetting twinkle = this.add(new NumberSetting("twinkle", "Twinkle", 100f, 0f, 300f, 5f).unit("%").group("Stars"));
	private final BoolSetting colored = this.add(new BoolSetting("colored", "Star Colors", true).onChange(v -> this.dirty = true).group("Stars"));
	private final DynamicMesh mesh = new DynamicMesh("XEYS starfield", 2000 * 4);
	private final Matrix4f rotation = new Matrix4f();
	private boolean dirty = true;
	private int vertices;

	public StarfieldModule() {
		super("starfield", "Starfield", Category.WORLD, Icons.STARS, "Thousands of twinkling, colored stars");
	}

	@Override
	public void onSky(SkyContext sky) {
		float visibility = sky.night() * sky.rainBrightness;
		if (visibility < 0.01f || !XeysPipelines.isUsable(XeysPipelines.SKY_STARS)) {
			return;
		}
		GpuBuffer buffer = this.mesh.buffer();
		if (this.dirty || buffer == null || buffer.isClosed()) {
			this.build();
			buffer = this.mesh.upload();
			this.vertices = this.mesh.vertexCount();
			this.dirty = false;
		}
		if (buffer == null) {
			return;
		}
		// The same turn as vanilla's stars.
		this.rotation.rotationY((float) Math.toRadians(-90.0)).rotateX(sky.starAngle);
		float twinkleSpeed = this.twinkle.value() / 100f;
		SkyDraw.draw(XeysPipelines.SKY_STARS, buffer, this.vertices, this.rotation, (float) (Clock.time() % 10000.0),
			visibility * this.brightness.value() / 100f, 0.45f * this.size.value() / 100f, twinkleSpeed, 0, 0);
	}

	private void build() {
		DynamicMesh m = this.mesh.begin();
		RandomSource random = RandomSource.create(0x5EED5L);
		int n = this.density.intValue();
		float r = SkyDraw.RADIUS;
		for (int i = 0; i < n; i++) {
			double x = random.nextGaussian();
			double y = random.nextGaussian();
			double z = random.nextGaussian();
			double len = Math.sqrt(x * x + y * y + z * z);
			if (len < 1e-6) {
				continue;
			}
			float cx = (float) (x / len * r);
			float cy = (float) (y / len * r);
			float cz = (float) (z / len * r);
			// Many faint stars, few bright ones.
			float b = (float) Math.pow(random.nextFloat(), 1.8);
			int alpha = Math.round(255f * (0.3f + 0.7f * b));
			int rgb = this.colored.on() ? COLORS[random.nextInt(COLORS.length)] & 0xFFFFFF : 0xFFFFFF;
			int argb = (alpha << 24) | rgb;
			m.vertex(cx, cy, cz, -1f, -1f, argb);
			m.vertex(cx, cy, cz, 1f, -1f, argb);
			m.vertex(cx, cy, cz, 1f, 1f, argb);
			m.vertex(cx, cy, cz, -1f, 1f, argb);
		}
	}

	@Override
	protected void onEnable() {
		this.dirty = true;
	}
}
