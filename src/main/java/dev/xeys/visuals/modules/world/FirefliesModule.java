package dev.xeys.visuals.modules.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import dev.xeys.visuals.world.GlowSwarm;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/** Glowing fireflies drift and blink just above the ground around you, after dark. */
public final class FirefliesModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private static final int CAPACITY = 200;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFD4FF6A).noAlpha().group("Fireflies"));
	private final NumberSetting density = this.add(new NumberSetting("density", "Density", 45f, 5f, 200f, 5f).group("Fireflies"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 40f, 250f, 5f).unit("%").group("Fireflies"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Radius", 20f, 8f, 48f, 1f).unit(" blocks").group("Fireflies"));
	private final BoolSetting nightOnly = this.add(new BoolSetting("night_only", "Only at Night", true).group("Fireflies"));
	private final GlowSwarm swarm = new GlowSwarm(CAPACITY);
	private final RandomSource random = RandomSource.create();
	private final BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
	private float visibility;
	private ClientLevel lastLevel;

	public FirefliesModule() {
		super("fireflies", "Fireflies", Category.WORLD, Icons.SPARKLES, "Blinking fireflies around you at night");
	}

	@Override
	public void onFrameStart() {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		LocalPlayer player = mc.player;
		if (level == null || player == null) {
			this.swarm.clear();
			return;
		}
		if (level != this.lastLevel) {
			this.swarm.clear();
			this.lastLevel = level;
		}
		float dt = Math.min(Clock.delta(), 0.1f);
		Vec3 eye = player.getEyePosition();
		float night = 1f;
		if (this.nightOnly.on()) {
			float stars = level.environmentAttributes().getValue(EnvironmentAttributes.STAR_BRIGHTNESS, eye);
			night = Math.min(1f, stars * 2.2f);
		}
		float target = night * (1f - level.getRainLevel(1f));
		this.visibility += (target - this.visibility) * Clock.smoothing(1.5f);

		double r = this.radius.value();
		int wanted = Math.round(this.density.value() * this.visibility);
		for (int n = 0; n < 4 && this.swarm.count < Math.min(wanted, CAPACITY); n++) {
			this.spawn(level, player, r);
		}
		double limit = r * 1.4;
		for (int i = 0; i < this.swarm.count; i++) {
			GlowSwarm s = this.swarm;
			s.age[i] += dt;
			double dx = s.x[i] - player.getX();
			double dz = s.z[i] - player.getZ();
			if (s.age[i] >= s.life[i] || dx * dx + dz * dz > limit * limit) {
				s.remove(i--);
				continue;
			}
			// Wander: a gentle random push, damped, with a slow vertical bob.
			float push = 1.6f * dt;
			s.vx[i] += (this.random.nextFloat() - 0.5f) * push;
			s.vy[i] += (this.random.nextFloat() - 0.5f) * push * 0.6f;
			s.vz[i] += (this.random.nextFloat() - 0.5f) * push;
			float damp = (float) Math.exp(-dt * 1.2f);
			s.vx[i] *= damp;
			s.vy[i] *= damp;
			s.vz[i] *= damp;
			double nx = s.x[i] + s.vx[i] * dt;
			double ny = s.y[i] + (s.vy[i] + 0.06f * (float) Math.sin(Clock.time() * 0.9 + s.phase[i])) * dt;
			double nz = s.z[i] + s.vz[i] * dt;
			if (level.getBlockState(this.probe.set(nx, ny, nz)).isAir()) {
				s.x[i] = nx;
				s.y[i] = ny;
				s.z[i] = nz;
			} else {
				s.vx[i] = -s.vx[i];
				s.vy[i] = Math.abs(s.vy[i]);
				s.vz[i] = -s.vz[i];
			}
		}
	}

	private void spawn(ClientLevel level, LocalPlayer player, double r) {
		double angle = this.random.nextDouble() * Math.PI * 2.0;
		double dist = 3.0 + Math.sqrt(this.random.nextDouble()) * (r - 3.0);
		double x = player.getX() + Math.cos(angle) * dist;
		double z = player.getZ() + Math.sin(angle) * dist;
		int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, (int) Math.floor(x), (int) Math.floor(z));
		double y = ground + 0.4 + this.random.nextDouble() * 2.2;
		// Underground or far below the surface: stay near the player's own level instead.
		if (y > player.getY() + 14.0) {
			y = player.getY() + this.random.nextDouble() * 2.5;
		}
		if (!level.getBlockState(this.probe.set(x, y, z)).isAir()) {
			return;
		}
		float s = (0.85f + this.random.nextFloat() * 0.3f) * this.size.value() / 100f;
		this.swarm.add(x, y, z, 7f + this.random.nextFloat() * 12f, s, this.random.nextFloat() * 100f);
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
		double t = Clock.time();
		GlowSwarm s = this.swarm;
		for (int i = 0; i < s.count; i++) {
			// Fireflies flash: mostly dim, with a bright pulse every few seconds.
			double wave = Math.sin(t * (1.1 + (s.phase[i] % 1.0) * 0.8) + s.phase[i]);
			float blink = 0.18f + 0.82f * (float) Math.pow(Math.max(0.0, wave), 6.0);
			float a = s.fade(i, 1.2f) * blink * this.visibility;
			if (a < 0.01f) {
				continue;
			}
			float x = (float) (s.x[i] - cam.x);
			float y = (float) (s.y[i] - cam.y);
			float z = (float) (s.z[i] - cam.z);
			WorldGlow.dot(vc, pose, x, y, z, 0.32f * s.size[i], Colors.withAlpha(rgb, Math.round(255f * a * 0.32f)));
			WorldGlow.dot(vc, pose, x, y, z, 0.07f * s.size[i], Colors.withAlpha(rgb, Math.round(255f * a)));
		}
	}

	@Override
	protected void onDisable() {
		this.swarm.clear();
		this.visibility = 0f;
	}
}
