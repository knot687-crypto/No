package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.anim.Easing;
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
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/** A ring of light ripples out across the ground when you jump and when you land. */
public final class JumpCirclesModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private static final int MAX = 12;
	private static final float LIFE = 0.75f;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Circles"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 1.3f, 0.5f, 3f, 0.1f).unit(" blocks").decimals(1).group("Circles"));
	private final BoolSetting onLand = this.add(new BoolSetting("on_land", "On Landing", true).group("Circles"));
	private final double[] cx = new double[MAX];
	private final double[] cy = new double[MAX];
	private final double[] cz = new double[MAX];
	private final double[] born = new double[MAX];
	private int count;
	private boolean wasOnGround = true;
	private double lastY;

	public JumpCirclesModule() {
		super("jump_circles", "Jump Circles", Category.COSMETICS, Icons.CIRCLE_DOT, "Rings ripple out when you jump and land");
	}

	@Override
	public void onFrameStart() {
		LocalPlayer player = Minecraft.getInstance().player;
		double now = Clock.time();
		for (int i = 0; i < this.count; i++) {
			if (now - this.born[i] > LIFE) {
				this.remove(i--);
			}
		}
		if (player == null) {
			return;
		}
		boolean ground = player.onGround();
		boolean rising = player.getDeltaMovement().y > 0.05 || player.getY() > this.lastY + 1e-4;
		if (this.wasOnGround && !ground && rising) {
			this.spawn(player, now);
		} else if (!this.wasOnGround && ground && this.onLand.on()) {
			this.spawn(player, now);
		}
		this.wasOnGround = ground;
		this.lastY = player.getY();
	}

	private void spawn(LocalPlayer player, double now) {
		if (this.count == MAX) {
			this.remove(0);
		}
		int i = this.count++;
		this.cx[i] = player.getX();
		this.cy[i] = player.getY() + 0.02;
		this.cz[i] = player.getZ();
		this.born[i] = now;
	}

	private void remove(int i) {
		int last = --this.count;
		this.cx[i] = this.cx[last];
		this.cy[i] = this.cy[last];
		this.cz[i] = this.cz[last];
		this.born[i] = this.born[last];
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		if (this.count == 0 || !WorldGlow.ringsUsable()) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.RINGS);
		PoseStack.Pose pose = context.matrices().last();
		double now = Clock.time();
		int rgb = this.color.color() & 0xFFFFFF;
		float max = this.size.value();
		for (int i = 0; i < this.count; i++) {
			float t = (float) ((now - this.born[i]) / LIFE);
			float x = (float) (this.cx[i] - cam.x);
			float y = (float) (this.cy[i] - cam.y);
			float z = (float) (this.cz[i] - cam.z);
			// Two ripples, the second trailing the first.
			for (int r = 0; r < 2; r++) {
				float tt = Math.max(0f, (t - r * 0.18f) / (1f - r * 0.18f));
				if (tt <= 0f) {
					continue;
				}
				float radius = 0.2f + (max - 0.2f) * Easing.OUT_CUBIC.apply(tt) * (r == 0 ? 1f : 0.7f);
				float alpha = (float) Math.pow(1f - tt, 1.6) * (r == 0 ? 1f : 0.6f);
				WorldGlow.flatRing(vc, pose, x, y, z, radius, Colors.withAlpha(rgb, alpha));
			}
		}
	}

	@Override
	protected void onDisable() {
		this.count = 0;
	}
}
