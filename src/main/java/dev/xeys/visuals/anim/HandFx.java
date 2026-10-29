package dev.xeys.visuals.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * First-person hand and item animation, set by the animation modules and read by the hand renderer
 * hooks. Neutral values change nothing. Everything here is visual: the item you hold and how you swing
 * it on the server are untouched.
 */
public final class HandFx {
	public enum SwingStyle {
		VANILLA,
		SMOOTH,
		PUNCH,
		SLASH,
		CHOP,
		SPIN
	}

	public enum EquipStyle {
		VANILLA,
		INSTANT,
		SLIDE,
		SPIN,
		POP
	}

	/** Where vanilla holds the main hand (right side); view model turns and scales happen about it. */
	private static final float ANCHOR_X = 0.56f;
	private static final float ANCHOR_Y = -0.52f;
	private static final float ANCHOR_Z = -0.72f;
	private static final float EQUIP_SECONDS = 0.32f;

	/** View model offset (blocks), rotation (degrees) and scale; mirrored for the left hand. */
	public static float offX;
	public static float offY;
	public static float offZ;
	public static float rotX;
	public static float rotY;
	public static float rotZ;
	public static float scale = 1f;
	/** The view model applies to the off hand too. */
	public static boolean bothHands = true;
	/** How much the hands lag behind the camera as it turns (vanilla 1). */
	public static float sway = 1f;
	/** How much the hands trail jumps and falls (0 = off). */
	public static float inertia;
	/** Idle hand bob amplitude (0 = off) and speed. */
	public static float breathing;
	public static float breathingSpeed = 1f;
	/** How a newly selected item comes into view. */
	public static EquipStyle equipStyle = EquipStyle.VANILLA;
	/** The hand lowers while the attack recharges (vanilla); either module can turn it off. */
	public static boolean cooldownDip = true;
	public static boolean oldNoCooldownDip;
	/** Holding use with a sword shows the old sword-block pose, swings included. */
	public static boolean blockHit;
	public static SwingStyle swingStyle = SwingStyle.VANILLA;

	/** Set while the main hand is drawn in the block pose, so its swing is applied the old way. */
	public static boolean blockingNow;

	/**
	 * Swing keyframes for the right hand (mirrored for the left): time, then x, y, z in blocks and roll,
	 * pitch, yaw in degrees, eased between keys. A chop raises the sword and brings it down through the
	 * middle of the view; a slash lays it flat and sweeps it across from right to left.
	 */
	private static final float[][] CHOP_KEYS = {
		{0f, 0f, 0f, 0f, 0f, 0f, 0f},
		{0.3f, -0.25f, 0.18f, 0.02f, 0f, 12f, 0f},
		{0.55f, -0.25f, -0.28f, -0.15f, 0f, -135f, 0f},
		{1f, 0f, 0f, 0f, 0f, 0f, 0f}
	};
	private static final float[][] SLASH_KEYS = {
		{0f, 0f, 0f, 0f, 0f, 0f, 0f},
		{0.22f, 0.08f, 0.42f, 0.04f, -75f, 0f, -35f},
		{0.55f, -0.62f, 0.32f, -0.04f, -85f, 0f, 175f},
		{1f, 0f, 0f, 0f, 0f, 0f, 0f}
	};

	private static final Spring FALL = new Spring(0f, 120f, 0.6f);
	private static double mainEquippedAt = -1.0;
	private static double offEquippedAt = -1.0;

	private HandFx() {
	}

	/** Per frame: the hands' fall inertia follows your vertical speed. */
	public static void frame() {
		LocalPlayer player = Minecraft.getInstance().player;
		float target = 0f;
		if (inertia > 0f && player != null && !player.onGround()) {
			float vy = (float) player.getDeltaMovement().y;
			target = Mth.clamp(-vy * 0.12f, -0.06f, 0.14f) * inertia;
		}
		FALL.target(target);
	}

	/** A new item was selected in that hand; its equip animation starts now. */
	public static void equipped(boolean mainHand) {
		if (equipStyle == EquipStyle.VANILLA || equipStyle == EquipStyle.INSTANT) {
			return;
		}
		if (mainHand) {
			mainEquippedAt = Clock.time();
		} else {
			offEquippedAt = Clock.time();
		}
	}

	/** Applied right after a hand's pose is pushed: breathing, inertia, the equip style and the view model. */
	public static void applyHand(PoseStack pose, HumanoidArm arm, InteractionHand hand) {
		int side = arm == HumanoidArm.RIGHT ? 1 : -1;
		boolean main = hand == InteractionHand.MAIN_HAND;
		if (breathing > 0f) {
			Player player = Minecraft.getInstance().player;
			float still = player == null ? 1f : 1f - Math.min(1f, (float) player.getDeltaMovement().horizontalDistance() * 6f);
			double t = Clock.time() * breathingSpeed;
			float k = breathing * still;
			pose.translate(side * Math.sin(t * 1.1) * 0.004f * k, Math.sin(t * 2.2) * 0.012f * k, 0f);
			pose.mulPose(Axis.ZP.rotationDegrees((float) Math.sin(t * 1.1) * 0.6f * k * side));
		}
		if (inertia > 0f) {
			float fall = FALL.get();
			if (fall != 0f) {
				pose.translate(0f, fall, 0f);
			}
		}
		double at = main ? mainEquippedAt : offEquippedAt;
		if (at >= 0.0) {
			float t = (float) ((Clock.time() - at) / EQUIP_SECONDS);
			if (t < 1f) {
				applyEquip(pose, side, Math.max(0f, t));
			}
		}
		if (!main && !bothHands) {
			return;
		}
		if (offX != 0f || offY != 0f || offZ != 0f) {
			pose.translate(side * offX, offY, offZ);
		}
		if (rotX != 0f || rotY != 0f || rotZ != 0f || scale != 1f) {
			// Turn and scale the hand in place, about where it is held.
			pose.translate(side * ANCHOR_X, ANCHOR_Y, ANCHOR_Z);
			if (rotX != 0f) {
				pose.mulPose(Axis.XP.rotationDegrees(rotX));
			}
			if (rotY != 0f) {
				pose.mulPose(Axis.YP.rotationDegrees(side * rotY));
			}
			if (rotZ != 0f) {
				pose.mulPose(Axis.ZP.rotationDegrees(side * rotZ));
			}
			if (scale != 1f) {
				pose.scale(scale, scale, scale);
			}
			pose.translate(-side * ANCHOR_X, -ANCHOR_Y, -ANCHOR_Z);
		}
	}

	/** The new item coming into place; {@code t} runs 0 to 1. */
	private static void applyEquip(PoseStack pose, int side, float t) {
		float away = (1f - t) * (1f - t) * (1f - t);
		switch (equipStyle) {
			case SLIDE -> pose.translate(side * 0.85f * away, -0.06f * away, 0f);
			case SPIN -> {
				pose.translate(0f, -0.45f * away, 0f);
				pose.translate(side * ANCHOR_X, ANCHOR_Y, ANCHOR_Z);
				pose.mulPose(Axis.YP.rotationDegrees(side * 220f * away));
				pose.translate(-side * ANCHOR_X, -ANCHOR_Y, -ANCHOR_Z);
			}
			case POP -> {
				// Ease out with a little overshoot, so the item pops into place.
				float u = t - 1f;
				float s = Math.max(0.001f, 1f + 2.70158f * u * u * u + 1.70158f * u * u);
				pose.translate(side * ANCHOR_X, ANCHOR_Y, ANCHOR_Z);
				pose.scale(s, s, s);
				pose.translate(-side * ANCHOR_X, -ANCHOR_Y, -ANCHOR_Z);
			}
			default -> {
			}
		}
	}

	/** Whether the main hand should show the old sword-block pose. */
	public static boolean wantsBlock(Player player, InteractionHand hand, ItemStack stack) {
		return blockHit && hand == InteractionHand.MAIN_HAND && !player.isUsingItem() && stack.is(ItemTags.SWORDS)
			&& Minecraft.getInstance().options.keyUse.isDown();
	}

	/**
	 * The sword-block pose, after the arm's rest transform; the swing tilts the blocked sword. The swing
	 * turns are vanilla's attack turns at under half strength, which keeps the dip on screen with
	 * today's item transforms, as the old block-hit did.
	 */
	public static void applyBlock(PoseStack pose, int side, float swing) {
		float g = Mth.sin(swing * swing * (float) Math.PI) * 0.45f;
		float h = Mth.sin(Mth.sqrt(swing) * (float) Math.PI) * 0.45f;
		pose.mulPose(Axis.YP.rotationDegrees(side * (45f + g * -20f)));
		pose.mulPose(Axis.ZP.rotationDegrees(side * h * -20f));
		pose.mulPose(Axis.XP.rotationDegrees(h * -80f));
		pose.mulPose(Axis.YP.rotationDegrees(side * -45f));
		pose.translate(side * -0.14142136f, 0.08f, 0.14142136f);
		pose.mulPose(Axis.XP.rotationDegrees(-102.25f));
		pose.mulPose(Axis.YP.rotationDegrees(side * 13.365f));
		pose.mulPose(Axis.ZP.rotationDegrees(side * 78.05f));
	}

	/** A custom swing; false means "use vanilla's". */
	public static boolean swing(PoseStack pose, float f, int side) {
		switch (swingStyle) {
			case PUNCH -> {
				float s = Mth.sin(Mth.sqrt(f) * (float) Math.PI);
				pose.translate(side * -0.06f * s, 0.04f * s, -0.38f * s);
				pose.mulPose(Axis.XP.rotationDegrees(-18f * s));
				return true;
			}
			case SLASH -> {
				keyframes(pose, SLASH_KEYS, f, side);
				return true;
			}
			case CHOP -> {
				keyframes(pose, CHOP_KEYS, f, side);
				return true;
			}
			case SPIN -> {
				float e = smooth(f);
				float arc = Mth.sin(f * (float) Math.PI);
				pose.translate(side * -0.12f * arc, 0.08f * arc, -0.1f * arc);
				pose.mulPose(Axis.XP.rotationDegrees(-360f * e));
				return true;
			}
			default -> {
				return false;
			}
		}
	}

	private static void keyframes(PoseStack pose, float[][] keys, float f, int side) {
		int i = 0;
		while (i < keys.length - 2 && f > keys[i + 1][0]) {
			i++;
		}
		float[] a = keys[i];
		float[] b = keys[i + 1];
		float u = smooth(Mth.clamp((f - a[0]) / (b[0] - a[0]), 0f, 1f));
		pose.translate(side * Mth.lerp(u, a[1], b[1]), Mth.lerp(u, a[2], b[2]), Mth.lerp(u, a[3], b[3]));
		pose.mulPose(Axis.YP.rotationDegrees(side * Mth.lerp(u, a[6], b[6])));
		pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(u, a[5], b[5])));
		pose.mulPose(Axis.ZP.rotationDegrees(side * Mth.lerp(u, a[4], b[4])));
	}

	/** Progress remapped for the smooth style: eased in and out, without vanilla's snap. */
	public static float smoothProgress(float f) {
		return swingStyle == SwingStyle.SMOOTH ? smooth(f) : f;
	}

	private static float smooth(float t) {
		return t * t * (3f - 2f * t);
	}
}
