package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.world.GlowSwarm;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;

/** Tiny motes of dust float in the air around you, catching the light. */
public final class DustMotesModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private static final int CAPACITY = 300;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFF1D6).noAlpha().group("Motes"));
	private final NumberSetting density = this.add(new NumberSetting("density", "Density", 90f, 10f, 300f, 10f).group("Motes"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 40f, 300f, 5f).unit("%").group("Motes"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 45f, 10f, 100f, 5f).unit("%").group("Motes"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 9f, 4f, 20f, 1f).unit(" blocks").group("Motes"));
	private final GlowSwarm swarm = new GlowSwarm(CAPACITY);
	/** Per mote: how lit its spot is (0..1), refreshed as it drifts. */
	private final float[] light = new float[CAPACITY];
	private final RandomSource random = RandomSource.create();
	private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
	private ClientLevel lastLevel;
	private float daylight;

	public DustMotesModule() {
		super("dust_motes", "Dust Motes", Category.WORLD, Icons.SPARKLE, "Specks of dust drift in the air and catch the light");
	}

	@Override
	public void onFrameStart() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			this.swarm.clear();
			return;
		}
		if (level != this.lastLevel) {
			this.swarm.clear();
			this.lastLevel = level;
		}
		float dt = Math.min(Clock.delta(), 0.1f);
		Vec3 eye = mc.gameRenderer.getMainCamera().position();
		float stars = level.environmentAttributes().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, eye);
		this.daylight = 1f - Math.min(1f, stars * 2f);
		double r = this.radius.value();
		int wanted = Math.min(this.density.intValue(), CAPACITY);
		for (int n = 0; n < 6 && this.swarm.count < wanted; n++) {
			this.spawn(level, eye, r);
		}
		GlowSwarm s = this.swarm;
		double t = Clock.time();
		for (int i = 0; i < s.count; i++) {
			s.age[i] += dt;
			double dx = s.x[i] - eye.x;
			double dy = s.y[i] - eye.y;
			double dz = s.z[i] - eye.z;
			if (s.age[i] >= s.life[i] || dx * dx + dy * dy + dz * dz > r * r * 1.6) {
				this.remove(i--);
				continue;
			}
			// A slow, smooth flow field plus a faint settling.
			float p = s.phase[i];
			float fx = 0.05f * (float) Math.sin(t * 0.31 + s.y[i] * 0.7 + p);
			float fy = 0.025f * (float) Math.sin(t * 0.23 + s.x[i] * 0.6 + p * 1.3) - 0.008f;
			float fz = 0.05f * (float) Math.cos(t * 0.27 + s.x[i] * 0.5 + p * 0.7);
			double nx = s.x[i] + (s.vx[i] + fx) * dt;
			double ny = s.y[i] + (s.vy[i] + fy) * dt;
			double nz = s.z[i] + (s.vz[i] + fz) * dt;
			this.probe.set(nx, ny, nz);
			if (!level.getBlockState(this.probe).isAir()) {
				this.remove(i--);
				continue;
			}
			s.x[i] = nx;
			s.y[i] = ny;
			s.z[i] = nz;
			if ((i + (int) (t * 4.0)) % 16 == 0) {
				this.light[i] = this.lightAt(level);
			}
		}
	}

	/** Removes mote {@code i}, keeping its light value paired with the mote swapped into its slot. */
	private void remove(int i) {
		this.light[i] = this.light[this.swarm.count - 1];
		this.swarm.remove(i);
	}

	private float lightAt(ClientLevel level) {
		float sky = level.getBrightness(LightLayer.SKY, this.probe) / 15f * this.daylight;
		float block = level.getBrightness(LightLayer.BLOCK, this.probe) / 15f;
		return Math.max(sky, block * 0.85f);
	}

	private void spawn(ClientLevel level, Vec3 eye, double r) {
		double x = eye.x + (this.random.nextDouble() * 2.0 - 1.0) * r;
		double y = eye.y + (this.random.nextDouble() * 2.0 - 1.2) * r * 0.6;
		double z = eye.z + (this.random.nextDouble() * 2.0 - 1.0) * r;
		this.probe.set(x, y, z);
		if (!level.getBlockState(this.probe).isAir()) {
			return;
		}
		float s = (0.6f + this.random.nextFloat() * 0.8f) * this.size.value() / 100f;
		int i = this.swarm.add(x, y, z, 6f + this.random.nextFloat() * 9f, s, this.random.nextFloat() * 100f);
		if (i >= 0) {
			this.swarm.vx[i] = (this.random.nextFloat() - 0.5f) * 0.06f;
			this.swarm.vy[i] = (this.random.nextFloat() - 0.5f) * 0.03f;
			this.swarm.vz[i] = (this.random.nextFloat() - 0.5f) * 0.06f;
			this.light[i] = this.lightAt(level);
		}
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		if (this.swarm.count == 0 || !WorldGlow.dotsUsable()) {
			return;
		}
		WorldGlow.face(Minecraft.getInstance().gameRenderer.getMainCamera());
		Vec3 cam = context.worldState().cameraRenderState.pos;
		PoseStack.Pose pose = context.matrices().last();
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.DOTS);
		int rgb = this.color.color() & 0xFFFFFF;
		float base = this.opacity.value() / 100f;
		GlowSwarm s = this.swarm;
		for (int i = 0; i < s.count; i++) {
			// Motes glint as they turn: a slow shimmer per mote.
			float shimmer = 0.6f + 0.4f * (float) Math.sin(Clock.time() * 1.7 + s.phase[i] * 3.0);
			float a = s.fade(i, 1.5f) * base * shimmer * (0.15f + 0.85f * this.light[i]);
			if (a < 0.01f) {
				continue;
			}
			WorldGlow.dot(vc, pose, (float) (s.x[i] - cam.x), (float) (s.y[i] - cam.y), (float) (s.z[i] - cam.z), 0.035f * s.size[i],
				Colors.withAlpha(rgb, Math.round(255f * a)));
		}
	}

	@Override
	protected void onDisable() {
		this.swarm.clear();
	}
}
