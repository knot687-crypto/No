package dev.xeys.visuals.anim;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Camera motion layered onto the view like vanilla's bobbing: strafe tilt, landing dip, jump kick,
 * elytra roll, hit recoil, hurt kick, shake and water sway. Settings are written by the camera modules
 * (0 = off); {@link #frame()} reads the player each frame and {@link #apply} adds the result to the view.
 * Rolls turn about the view's center, so aim is never offset; pitch kicks are brief and settle back.
 */
public final class CameraMotion {
	/** Roll toward the side you strafe to, in degrees at a sprint. */
	public static float strafeTilt;
	/** Landing dip strength (1 = normal). */
	public static float landDip;
	/** Nod as you jump, in degrees. */
	public static float jumpKick;
	/** Banking into elytra turns (1 = normal). */
	public static float elytraRoll;
	/** Head snap when you are hurt, in degrees. */
	public static float hurtKick;
	/** Shake strength, and what shakes the camera. */
	public static float shake;
	public static boolean shakeOnHurt = true;
	public static boolean shakeOnExplosion = true;
	public static boolean shakeOnLanding = true;
	/** Gentle sway while underwater (1 = normal). */
	public static float waterSway;

	private static final Spring ROLL = new Spring(0f, 90f, 1f);
	private static final Spring DIP = new Spring(0f, 70f, 0.55f);
	private static final Spring KICK = new Spring(0f, 110f, 0.7f);
	private static final Spring GLIDE = new Spring(0f, 40f, 1f);
	private static final Spring RECOIL = new Spring(0f, 160f, 0.65f);
	/** Peak displacement per unit impulse for the kick and recoil springs, so settings read in degrees. */
	private static final float KICK_PER_IMPULSE = 0.0437f;
	private static final float RECOIL_PER_IMPULSE = 0.038f;
	private static float trauma;
	private static boolean wasOnGround = true;
	private static float fallFrom;
	private static float lastYaw;
	private static int lastHurt;

	private CameraMotion() {
	}

	/** Current shake (0..1) and landing dip (blocks), for readouts and tests. */
	public static float trauma() {
		return trauma;
	}

	public static float landingDip() {
		return DIP.get();
	}

	/** Adds shake (0..1); it decays over about a second. */
	public static void addTrauma(float amount) {
		trauma = Math.min(1f, trauma + amount);
	}

	/** Your hit landed: a small upward kick of about {@code degrees}. */
	public static void recoil(float degrees) {
		RECOIL.impulse(-degrees / RECOIL_PER_IMPULSE);
	}

	/** An explosion nearby shakes the camera, more the closer it is. */
	public static void explosion(Vec3 center, float radius) {
		if (shake <= 0f || !shakeOnExplosion) {
			return;
		}
		Vec3 eye = Minecraft.getInstance().gameRenderer.getMainCamera().position();
		float reach = Math.max(6f, radius * 4f);
		float near = 1f - (float) center.distanceTo(eye) / reach;
		if (near > 0f) {
			addTrauma(0.35f + near * 0.65f);
		}
	}

	public static void frame() {
		LocalPlayer player = Minecraft.getInstance().player;
		float dt = Math.min(Clock.delta(), 0.1f);
		trauma = Math.max(0f, trauma - dt * 1.1f);
		if (player == null) {
			return;
		}
		float yaw = player.getYRot();
		double rad = Math.toRadians(yaw);
		Vec3 v = player.getDeltaMovement();
		// Sideways speed relative to where the player faces (positive to the right).
		double side = v.x * -Math.cos(rad) + v.z * -Math.sin(rad);
		ROLL.target(strafeTilt > 0f ? (float) Math.max(-1.0, Math.min(1.0, side / 0.2)) * strafeTilt : 0f);
		boolean ground = player.onGround();
		if (!ground) {
			fallFrom = Math.max(fallFrom, (float) player.fallDistance);
		}
		if (!wasOnGround && ground) {
			if (landDip > 0f && fallFrom > 0.4f) {
				DIP.impulse(Math.min(fallFrom, 6f) * 0.8f * landDip);
			}
			if (shake > 0f && shakeOnLanding && fallFrom > 4f) {
				addTrauma(Math.min(1f, (fallFrom - 3f) / 12f));
			}
		}
		if (wasOnGround && !ground && v.y > 0.05 && jumpKick > 0f) {
			KICK.impulse(jumpKick / KICK_PER_IMPULSE);
		}
		if (ground) {
			fallFrom = 0f;
		}
		wasOnGround = ground;
		// Elytra: bank into turns, from how fast the heading changes.
		float turnRate = dt > 0f ? wrap(yaw - lastYaw) / dt : 0f;
		lastYaw = yaw;
		GLIDE.target(elytraRoll > 0f && player.isFallFlying() ? Math.max(-35f, Math.min(35f, turnRate * 0.12f)) * elytraRoll : 0f);
		if (player.hurtTime > lastHurt && player.hurtTime >= player.hurtDuration - 1) {
			if (shake > 0f && shakeOnHurt) {
				addTrauma(0.45f);
			}
			if (hurtKick > 0f) {
				KICK.impulse(-hurtKick / KICK_PER_IMPULSE);
			}
		}
		lastHurt = player.hurtTime;
	}

	public static void apply(PoseStack pose, float partialTick) {
		float roll = ROLL.get() + GLIDE.get();
		float pitch = KICK.get() + RECOIL.get();
		float yaw = 0f;
		if (shake > 0f && trauma > 0f) {
			double t = Clock.time() * 24.0;
			float s = trauma * trauma * shake;
			roll += (float) (Math.sin(t * 1.3) + Math.sin(t * 2.9 + 1.7) * 0.5) * 3.2f * s;
			pitch += (float) (Math.sin(t * 1.7 + 0.4) + Math.sin(t * 3.1) * 0.4) * 1.6f * s;
			yaw += (float) (Math.sin(t * 1.1 + 2.3) + Math.sin(t * 2.3 + 0.8) * 0.4) * 1.6f * s;
		}
		float lift = DIP.get();
		LocalPlayer player = Minecraft.getInstance().player;
		if (waterSway > 0f && player != null && player.isUnderWater()) {
			double t = Clock.time();
			roll += (float) Math.sin(t * 0.9) * 1.6f * waterSway;
			pitch += (float) Math.sin(t * 0.7 + 1.1) * 0.6f * waterSway;
			lift += (float) Math.sin(t * 1.3) * 0.02f * waterSway;
		}
		if (roll != 0f) {
			pose.mulPose(Axis.ZP.rotationDegrees(roll));
		}
		if (pitch != 0f) {
			pose.mulPose(Axis.XP.rotationDegrees(pitch));
		}
		if (yaw != 0f) {
			pose.mulPose(Axis.YP.rotationDegrees(yaw));
		}
		if (lift != 0f) {
			// Moving the world up is the camera dipping down.
			pose.translate(0f, lift, 0f);
		}
	}

	private static float wrap(float degrees) {
		float d = degrees % 360f;
		if (d >= 180f) {
			d -= 360f;
		}
		if (d < -180f) {
			d += 360f;
		}
		return d;
	}
}
