package dev.xeys.visuals.modules.cosmetics;

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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/** A little wisp of light follows you over your shoulder, bobbing and shedding sparks. */
public final class CompanionModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Wisp"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 50f, 200f, 5f).unit("%").group("Wisp"));
	private final NumberSetting sparks = this.add(new NumberSetting("sparks", "Sparks", 100f, 0f, 300f, 10f).unit("%").group("Wisp"));
	private final GlowSwarm trail = new GlowSwarm(64);
	private final RandomSource random = RandomSource.create();
	private double x;
	private double y;
	private double z;
	private double vx;
	private double vy;
	private double vz;
	private boolean placed;
	private float sparkTimer;

	public CompanionModule() {
		super("companion", "Companion", Category.COSMETICS, Icons.GHOST, "A wisp of light follows you around");
	}

	@Override
	public void onFrameStart() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		if (player == null) {
			this.placed = false;
			this.trail.clear();
			return;
		}
		float dt = Math.min(Clock.delta(), 0.05f);
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		// Hover behind the right shoulder, turning with the body.
		double yaw = Math.toRadians(Mth.lerp(partial, player.yBodyRotO, player.yBodyRot));
		double t = Clock.time();
		double side = -0.75;
		double back = -0.55;
		double tx = Mth.lerp(partial, player.xOld, player.getX()) + Math.cos(yaw) * side - Math.sin(yaw) * back;
		double ty = Mth.lerp(partial, player.yOld, player.getY()) + player.getBbHeight() + 0.15 + Math.sin(t * 2.2) * 0.12;
		double tz = Mth.lerp(partial, player.zOld, player.getZ()) + Math.sin(yaw) * side + Math.cos(yaw) * back;
		double dx = tx - this.x;
		double dy = ty - this.y;
		double dz = tz - this.z;
		if (!this.placed || dx * dx + dy * dy + dz * dz > 400.0) {
			this.x = tx;
			this.y = ty;
			this.z = tz;
			this.vx = this.vy = this.vz = 0.0;
			this.placed = true;
		} else {
			// A critically damped spring: it lags a little, then settles without overshooting.
			double k = 36.0;
			double c = 12.0;
			this.vx += (k * dx - c * this.vx) * dt;
			this.vy += (k * dy - c * this.vy) * dt;
			this.vz += (k * dz - c * this.vz) * dt;
			this.x += this.vx * dt;
			this.y += this.vy * dt;
			this.z += this.vz * dt;
		}
		GlowSwarm s = this.trail;
		for (int i = 0; i < s.count; i++) {
			s.age[i] += dt;
			if (s.age[i] >= s.life[i]) {
				s.remove(i--);
				continue;
			}
			s.x[i] += s.vx[i] * dt;
			s.y[i] += s.vy[i] * dt;
			s.z[i] += s.vz[i] * dt;
		}
		this.sparkTimer += dt * this.sparks.value() / 100f;
		while (this.sparkTimer > 0.06f) {
			this.sparkTimer -= 0.06f;
			int i = s.add(this.x, this.y, this.z, 0.5f + this.random.nextFloat() * 0.5f, 0.5f + this.random.nextFloat() * 0.6f, 0f);
			if (i >= 0) {
				s.vx[i] = (this.random.nextFloat() - 0.5f) * 0.3f;
				s.vy[i] = -0.15f - this.random.nextFloat() * 0.25f;
				s.vz[i] = (this.random.nextFloat() - 0.5f) * 0.3f;
			}
		}
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (!this.placed || player == null || player.isInvisible() || !WorldGlow.dotsUsable()) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		WorldGlow.face(Minecraft.getInstance().gameRenderer.getMainCamera());
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.DOTS);
		PoseStack.Pose pose = context.matrices().last();
		int rgb = this.color.color() & 0xFFFFFF;
		float s = this.size.value() / 100f;
		float px = (float) (this.x - cam.x);
		float py = (float) (this.y - cam.y);
		float pz = (float) (this.z - cam.z);
		float pulse = 0.85f + 0.15f * (float) Math.sin(Clock.time() * 3.1);
		WorldGlow.dot(vc, pose, px, py, pz, 0.42f * s * pulse, Colors.withAlpha(rgb, 0.35f));
		WorldGlow.dot(vc, pose, px, py, pz, 0.13f * s, Colors.withAlpha(Colors.lerp(rgb | 0xFF000000, 0xFFFFFFFF, 0.5f), 1f));
		GlowSwarm t = this.trail;
		for (int i = 0; i < t.count; i++) {
			float f = 1f - t.age[i] / t.life[i];
			WorldGlow.dot(vc, pose, (float) (t.x[i] - cam.x), (float) (t.y[i] - cam.y), (float) (t.z[i] - cam.z), 0.04f * s * t.size[i],
				Colors.withAlpha(rgb, f * 0.9f));
		}
	}

	@Override
	protected void onDisable() {
		this.placed = false;
		this.trail.clear();
	}
}
