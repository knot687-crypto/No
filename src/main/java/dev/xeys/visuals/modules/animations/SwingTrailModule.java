package dev.xeys.visuals.modules.animations;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.anim.AnimHooks;
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
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Each swing leaves a crescent of light in the air in front of you. The arc is placed in the world
 * where you swung, so it stays behind as you turn, and fades as its tail catches up with its head.
 */
public final class SwingTrailModule extends Module implements Events.FrameStart, WorldEvents.EndMain {
	private static final int SEGMENTS = 28;
	/** The arc's span in local angle and its tilt (degrees), for a right-handed swing: upper right to lower left. */
	private static final float FROM = -12f;
	private static final float TO = 192f;
	private static final float TILT = 24f * Mth.DEG_TO_RAD;

	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Trail"));
	private final NumberSetting width = this.add(new NumberSetting("width", "Width", 100f, 30f, 250f, 5f).unit("%").group("Trail"));
	private final NumberSetting size = this.add(new NumberSetting("size", "Size", 100f, 50f, 180f, 5f).unit("%").group("Trail"));
	private final BoolSetting weaponsOnly = this.add(new BoolSetting("weapons", "Weapons Only", true)
		.describe("Only with swords, axes, tridents and maces").group("Trail"));
	/** The swing's frame at its start: eye position and the camera's right, up and forward axes. */
	private final Vec3[] frame = new Vec3[4];
	private double startedAt = -1.0;
	private int side = 1;
	private int lastSwingTime;
	private boolean wasSwinging;

	public SwingTrailModule() {
		super("swing_trail", "Swing Trail", Category.ANIMATIONS, Icons.SPARKLE, "A crescent of light follows each swing");
	}

	@Override
	public void onFrameStart() {
		LocalPlayer player = Minecraft.getInstance().player;
		if (player == null) {
			this.startedAt = -1.0;
			return;
		}
		boolean swinging = player.swinging;
		boolean started = swinging && (!this.wasSwinging || player.swingTime < this.lastSwingTime);
		this.wasSwinging = swinging;
		this.lastSwingTime = player.swingTime;
		if (started && (!this.weaponsOnly.on() || isWeapon(player.getMainHandItem()))) {
			this.start(player);
		}
	}

	private void start(LocalPlayer player) {
		Minecraft mc = Minecraft.getInstance();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		Vec3 eye = player.getEyePosition(partial);
		Vec3 forward = player.getViewVector(partial);
		Vec3 right = forward.cross(new Vec3(0.0, 1.0, 0.0));
		if (right.lengthSqr() < 1e-6) {
			right = new Vec3(1.0, 0.0, 0.0);
		}
		right = right.normalize();
		Vec3 up = right.cross(forward).normalize();
		this.frame[0] = eye;
		this.frame[1] = right;
		this.frame[2] = up;
		this.frame[3] = forward;
		this.side = player.getMainArm() == HumanoidArm.RIGHT ? 1 : -1;
		this.startedAt = Clock.time();
	}

	private static boolean isWeapon(ItemStack stack) {
		return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.AXES) || stack.is(Items.TRIDENT) || stack.is(Items.MACE);
	}

	@Override
	public void onEndMain(WorldRenderContext context) {
		if (this.startedAt < 0.0 || !WorldGlow.beamsUsable()) {
			return;
		}
		float life = 0.4f / Math.max(0.25f, AnimHooks.swingSpeed);
		float t = (float) ((Clock.time() - this.startedAt) / life);
		if (t >= 1f) {
			this.startedAt = -1.0;
			return;
		}
		// The head sweeps across quickly; the tail follows and catches up as the arc fades.
		float head = 1f - cube(1f - Math.min(1f, t / 0.62f));
		float tail = 1f - sq(1f - Mth.clamp((t - 0.2f) / 0.8f, 0f, 1f));
		if (head - tail < 0.01f) {
			return;
		}
		Vec3 cam = context.worldState().cameraRenderState.pos;
		VertexConsumer vc = context.consumers().getBuffer(WorldGlow.BEAMS);
		PoseStack.Pose pose = context.matrices().last();
		float scale = this.size.value() / 100f;
		float halfWidth = 0.11f * this.width.value() / 100f * scale;
		int rgb = this.color.color() & 0xFFFFFF;
		float px = 0f;
		float py = 0f;
		float pz = 0f;
		float pv = 0f;
		for (int i = 0; i <= SEGMENTS; i++) {
			float s = head - (head - tail) * i / SEGMENTS;
			float a = Mth.lerp(s, FROM, TO) * Mth.DEG_TO_RAD;
			// A flattened crescent that wraps around you: nearer at the ends, farthest in front.
			float lx = Mth.cos(a) * 1.15f * scale;
			float ly = Mth.sin(a) * 0.48f * scale - 0.12f;
			float lz = (0.62f + 0.55f * Mth.sin(a)) * scale + 0.5f;
			// Tilted, then mirrored for a left-handed swing.
			float rx = (lx * Mth.cos(TILT) - ly * Mth.sin(TILT)) * this.side;
			float ry = lx * Mth.sin(TILT) + ly * Mth.cos(TILT);
			float x = (float) (this.frame[0].x - cam.x + this.frame[1].x * rx + this.frame[2].x * ry + this.frame[3].x * lz);
			float y = (float) (this.frame[0].y - cam.y + this.frame[1].y * rx + this.frame[2].y * ry + this.frame[3].y * lz);
			float z = (float) (this.frame[0].z - cam.z + this.frame[1].z * rx + this.frame[2].z * ry + this.frame[3].z * lz);
			// v runs from the head (brightest) to the tail (faded out) along the beam shader's ramp.
			float v = 0.04f + 0.96f * i / SEGMENTS;
			if (i > 0) {
				this.quad(vc, pose, px, py, pz, pv, x, y, z, v, halfWidth, rgb);
			}
			px = x;
			py = y;
			pz = z;
			pv = v;
		}
	}

	private void quad(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float v0, float x1, float y1, float z1, float v1,
					  float halfWidth, int rgb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		// Perpendicular to the arc and to the line of sight.
		float sx = dy * z0 - dz * y0;
		float sy = dz * x0 - dx * z0;
		float sz = dx * y0 - dy * x0;
		float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (sl < 1e-7f) {
			return;
		}
		float w0 = halfWidth * (1f - v0 * 0.75f) / sl;
		float w1 = halfWidth * (1f - v1 * 0.75f) / sl;
		int c = Colors.withAlpha(rgb, 255);
		vc.addVertex(pose, x0 - sx * w0, y0 - sy * w0, z0 - sz * w0).setUv(-1f, v0).setColor(c);
		vc.addVertex(pose, x0 + sx * w0, y0 + sy * w0, z0 + sz * w0).setUv(1f, v0).setColor(c);
		vc.addVertex(pose, x1 + sx * w1, y1 + sy * w1, z1 + sz * w1).setUv(1f, v1).setColor(c);
		vc.addVertex(pose, x1 - sx * w1, y1 - sy * w1, z1 - sz * w1).setUv(-1f, v1).setColor(c);
	}

	private static float sq(float x) {
		return x * x;
	}

	private static float cube(float x) {
		return x * x * x;
	}

	@Override
	protected void onDisable() {
		this.startedAt = -1.0;
	}
}
