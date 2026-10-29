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
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** Glowing footprints mark where you walk, fading behind you. */
public final class FootstepsModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private static final int MAX = 48;
	private static final double STRIDE = 0.62;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Footsteps"));
	private final NumberSetting life = this.add(new NumberSetting("life", "Linger", 4f, 1f, 12f, 0.5f).unit(" s").decimals(1).group("Footsteps"));
	private final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 90f, 20f, 100f, 5f).unit("%").group("Footsteps"));
	private final double[] fx = new double[MAX];
	private final double[] fy = new double[MAX];
	private final double[] fz = new double[MAX];
	private final float[] dirX = new float[MAX];
	private final float[] dirZ = new float[MAX];
	private final double[] born = new double[MAX];
	private int count;
	private double walked;
	private double lastX = Double.NaN;
	private double lastZ;
	private boolean left;

	public FootstepsModule() {
		super("footsteps", "Footsteps", Category.COSMETICS, Icons.FOOTPRINTS, "Glowing footprints where you walk");
	}

	@Override
	public void onFrameStart() {
		LocalPlayer player = Minecraft.getInstance().player;
		double now = Clock.time();
		for (int i = 0; i < this.count; i++) {
			if (now - this.born[i] > this.life.value()) {
				this.remove(i--);
			}
		}
		if (player == null) {
			return;
		}
		double x = player.getX();
		double z = player.getZ();
		if (Double.isNaN(this.lastX)) {
			this.lastX = x;
			this.lastZ = z;
			return;
		}
		double dx = x - this.lastX;
		double dz = z - this.lastZ;
		double d = Math.sqrt(dx * dx + dz * dz);
		this.lastX = x;
		this.lastZ = z;
		if (d > 4.0 || !player.onGround() || player.isPassenger()) {
			this.walked = 0.0;
			return;
		}
		this.walked += d;
		if (this.walked >= STRIDE && d > 1e-4) {
			this.walked -= STRIDE;
			this.step(player, (float) (dx / d), (float) (dz / d), now);
		}
	}

	private void step(LocalPlayer player, float dirXv, float dirZv, double now) {
		if (this.count == MAX) {
			this.remove(0);
		}
		int i = this.count++;
		// Alternate feet, a hand's width either side of the path.
		float side = this.left ? 0.13f : -0.13f;
		this.left = !this.left;
		this.fx[i] = player.getX() - dirZv * side;
		this.fy[i] = player.getY() + 0.015;
		this.fz[i] = player.getZ() + dirXv * side;
		this.dirX[i] = dirXv;
		this.dirZ[i] = dirZv;
		this.born[i] = now;
	}

	private void remove(int i) {
		int last = --this.count;
		this.fx[i] = this.fx[last];
		this.fy[i] = this.fy[last];
		this.fz[i] = this.fz[last];
		this.dirX[i] = this.dirX[last];
		this.dirZ[i] = this.dirZ[last];
		this.born[i] = this.born[last];
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		if (this.count == 0 || !WorldGlow.dotsUsable()) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.DOTS);
		PoseStack.Pose pose = context.matrices().last();
		double now = Clock.time();
		int rgb = this.color.color() & 0xFFFFFF;
		float base = this.opacity.value() / 100f;
		float life = this.life.value();
		for (int i = 0; i < this.count; i++) {
			float t = (float) ((now - this.born[i]) / life);
			float alpha = base * Math.min(1f, (1f - t) * 2.5f) * Math.min(1f, t * 20f + 0.3f);
			float x = (float) (this.fx[i] - cam.x);
			float y = (float) (this.fy[i] - cam.y);
			float z = (float) (this.fz[i] - cam.z);
			// An oval along the walking direction.
			float ax = this.dirX[i] * 0.24f;
			float az = this.dirZ[i] * 0.24f;
			float bx = -this.dirZ[i] * 0.12f;
			float bz = this.dirX[i] * 0.12f;
			WorldGlow.sprite(vc, pose, x, y, z, ax, 0f, az, bx, 0f, bz, Colors.withAlpha(rgb, alpha));
		}
	}

	@Override
	protected void onDisable() {
		this.count = 0;
		this.lastX = Double.NaN;
	}
}
