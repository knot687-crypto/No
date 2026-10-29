package dev.xeys.visuals.modules.cosmetics;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.WorldGlow;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/** A ribbon of light streams behind you as you move, fading as it ages. */
public final class TrailsModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	public enum Height {
		FEET(0.12f),
		WAIST(0.85f),
		HEAD(1.75f);

		final float offset;

		Height(float offset) {
			this.offset = offset;
		}
	}

	private static final int MAX = 96;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Trail"));
	private final EnumSetting<Height> height = this.add(new EnumSetting<>("height", "Height", Height.WAIST).group("Trail"));
	private final NumberSetting length = this.add(new NumberSetting("length", "Length", 1.2f, 0.3f, 3f, 0.1f).unit(" s").decimals(1)
		.describe("How long the trail lingers").group("Trail"));
	private final NumberSetting width = this.add(new NumberSetting("width", "Width", 100f, 30f, 250f, 5f).unit("%").group("Trail"));
	private final double[] px = new double[MAX];
	private final double[] py = new double[MAX];
	private final double[] pz = new double[MAX];
	private final double[] pt = new double[MAX];
	private int head;
	private int count;

	public TrailsModule() {
		super("trails", "Trails", Category.COSMETICS, Icons.WIND, "A ribbon of light streams behind you");
	}

	@Override
	public void onFrameStart() {
		Minecraft mc = Minecraft.getInstance();
		LocalPlayer player = mc.player;
		double now = Clock.time();
		// Forget points older than the trail's length.
		while (this.count > 0 && now - this.pt[this.oldest()] > this.length.value()) {
			this.count--;
		}
		if (player == null) {
			this.count = 0;
			return;
		}
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		double x = Mth.lerp(partial, player.xOld, player.getX());
		double y = Mth.lerp(partial, player.yOld, player.getY()) + this.height.get().offset;
		double z = Mth.lerp(partial, player.zOld, player.getZ());
		if (this.count > 0) {
			int last = this.head;
			double dx = x - this.px[last];
			double dy = y - this.py[last];
			double dz = z - this.pz[last];
			double d2 = dx * dx + dy * dy + dz * dz;
			if (d2 > 36.0) {
				// Teleported: start over rather than draw a streak across the world.
				this.count = 0;
			} else if (d2 < 0.0025) {
				return;
			}
		}
		this.head = (this.head + 1) % MAX;
		this.px[this.head] = x;
		this.py[this.head] = y;
		this.pz[this.head] = z;
		this.pt[this.head] = now;
		this.count = Math.min(this.count + 1, MAX);
	}

	private int oldest() {
		return Math.floorMod(this.head - this.count + 1, MAX);
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		if (this.count < 2 || !WorldGlow.beamsUsable()) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.BEAMS);
		PoseStack.Pose pose = context.matrices().last();
		double now = Clock.time();
		float life = this.length.value();
		float w = 0.11f * this.width.value() / 100f;
		for (int k = 0; k < this.count - 1; k++) {
			int i = Math.floorMod(this.head - k, MAX);
			int j = Math.floorMod(this.head - k - 1, MAX);
			float x0 = (float) (this.px[i] - cam.x);
			float y0 = (float) (this.py[i] - cam.y);
			float z0 = (float) (this.pz[i] - cam.z);
			float x1 = (float) (this.px[j] - cam.x);
			float y1 = (float) (this.py[j] - cam.y);
			float z1 = (float) (this.pz[j] - cam.z);
			float a0 = (float) Math.min(1.0, (now - this.pt[i]) / life);
			float a1 = (float) Math.min(1.0, (now - this.pt[j]) / life);
			// Side vectors at each end: perpendicular to the segment and to the line of sight.
			float dx = x1 - x0;
			float dy = y1 - y0;
			float dz = z1 - z0;
			float sx = dy * z0 - dz * y0;
			float sy = dz * x0 - dx * z0;
			float sz = dx * y0 - dy * x0;
			float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
			if (sl < 1e-6f) {
				continue;
			}
			float w0 = w * (1f - a0 * 0.6f) / sl;
			float w1 = w * (1f - a1 * 0.6f) / sl;
			int c0 = Colors.withAlpha(this.color.color(a0) & 0xFFFFFF, 255);
			int c1 = Colors.withAlpha(this.color.color(a1) & 0xFFFFFF, 255);
			// The beam shader fades with v: newest (0) is brightest after a short fade-in.
			float v0 = 0.04f + a0 * 0.96f;
			float v1 = 0.04f + a1 * 0.96f;
			vc.addVertex(pose, x0 - sx * w0, y0 - sy * w0, z0 - sz * w0).setUv(-1f, v0).setColor(c0);
			vc.addVertex(pose, x0 + sx * w0, y0 + sy * w0, z0 + sz * w0).setUv(1f, v0).setColor(c0);
			vc.addVertex(pose, x1 + sx * w1, y1 + sy * w1, z1 + sz * w1).setUv(1f, v1).setColor(c1);
			vc.addVertex(pose, x1 - sx * w1, y1 - sy * w1, z1 - sz * w1).setUv(-1f, v1).setColor(c1);
		}
	}

	@Override
	protected void onDisable() {
		this.count = 0;
	}
}
