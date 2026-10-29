package dev.xeys.visuals.anim;

import dev.xeys.visuals.core.Keybinds;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/** Third-person emotes for your own player, played while the emote key is held (only you see them). */
public final class Emotes {
	public enum Emote {
		WAVE,
		CHEER,
		CLAP,
		DAB,
		SALUTE,
		POINT,
		BOW,
		SPIN
	}

	public static boolean enabled;
	public static Emote emote = Emote.WAVE;
	/** Switch to the front camera while emoting, so you can see it. */
	public static boolean showCamera = true;
	/** 0..1 blend in and out of the pose. */
	private static float blend;
	private static double startedAt;
	private static boolean wasDown;
	private static @Nullable CameraType cameraBefore;

	private Emotes() {
	}

	public static void frame() {
		Minecraft mc = Minecraft.getInstance();
		boolean down = enabled && Keybinds.emote != null && Keybinds.emote.isDown() && mc.screen == null;
		if (down && !wasDown) {
			startedAt = Clock.time();
			if (showCamera && mc.options.getCameraType().isFirstPerson()) {
				cameraBefore = mc.options.getCameraType();
				mc.options.setCameraType(CameraType.THIRD_PERSON_FRONT);
			}
		}
		if (!down && wasDown && cameraBefore != null) {
			mc.options.setCameraType(cameraBefore);
			cameraBefore = null;
		}
		wasDown = down;
		blend += ((down ? 1f : 0f) - blend) * Clock.smoothing(10f);
		if (!enabled) {
			blend = 0f;
		}
	}

	public static boolean active() {
		return blend > 0.01f;
	}

	/** Extra body turn for spinning, in degrees. */
	public static float spin() {
		if (!active() || emote != Emote.SPIN) {
			return 0f;
		}
		return (float) ((Clock.time() - startedAt) * 540.0 % 360.0) * blend;
	}

	/** Poses the model's arms, head and body after vanilla's animation (sleeves and pants follow). */
	public static void pose(PlayerModel m) {
		if (!active()) {
			return;
		}
		float t = (float) (Clock.time() - startedAt);
		float b = blend;
		switch (emote) {
			case WAVE -> {
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, -2.75f);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, 0f);
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, -0.15f + 0.42f * Mth.sin(t * 9f));
				m.head.zRot = Mth.lerp(b, m.head.zRot, 0.1f * Mth.sin(t * 4.5f));
			}
			case CHEER -> {
				float bounce = Mth.sin(t * 8f);
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, -2.8f + 0.25f * bounce);
				m.leftArm.xRot = Mth.lerp(b, m.leftArm.xRot, -2.8f - 0.25f * bounce);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, 0f);
				m.leftArm.yRot = Mth.lerp(b, m.leftArm.yRot, 0f);
				// Raised arms spread outward with negative roll on the right, positive on the left.
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, -0.45f);
				m.leftArm.zRot = Mth.lerp(b, m.leftArm.zRot, 0.45f);
				m.head.xRot = Mth.lerp(b, m.head.xRot, -0.3f);
			}
			case CLAP -> {
				float c = 0.5f + 0.5f * Mth.sin(t * 15f);
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, -1.3f);
				m.leftArm.xRot = Mth.lerp(b, m.leftArm.xRot, -1.3f);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, -0.12f - 0.36f * c);
				m.leftArm.yRot = Mth.lerp(b, m.leftArm.yRot, 0.12f + 0.36f * c);
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, 0f);
				m.leftArm.zRot = Mth.lerp(b, m.leftArm.zRot, 0f);
			}
			case DAB -> {
				m.head.xRot = Mth.lerp(b, m.head.xRot, 0.55f);
				m.head.yRot = Mth.lerp(b, m.head.yRot, -0.55f);
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, -1.45f);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, -0.75f);
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, 0f);
				m.leftArm.xRot = Mth.lerp(b, m.leftArm.xRot, -2.4f);
				m.leftArm.yRot = Mth.lerp(b, m.leftArm.yRot, 0f);
				m.leftArm.zRot = Mth.lerp(b, m.leftArm.zRot, 0.9f);
			}
			case SALUTE -> {
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, -2.35f);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, -0.65f);
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, 0.4f);
			}
			case POINT -> {
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, m.head.xRot - 1.5708f);
				m.rightArm.yRot = Mth.lerp(b, m.rightArm.yRot, m.head.yRot - 0.08f);
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, 0f);
			}
			case BOW -> {
				// Vanilla's crouch, deeper at the head, with the arms hanging forward.
				m.body.xRot = Mth.lerp(b, m.body.xRot, 0.5f);
				m.body.y = Mth.lerp(b, m.body.y, m.body.y + 3.2f);
				m.head.xRot = Mth.lerp(b, m.head.xRot, 0.7f);
				m.head.y = Mth.lerp(b, m.head.y, m.head.y + 4.2f);
				m.rightArm.y = Mth.lerp(b, m.rightArm.y, m.rightArm.y + 3.2f);
				m.leftArm.y = Mth.lerp(b, m.leftArm.y, m.leftArm.y + 3.2f);
				m.rightArm.xRot = Mth.lerp(b, m.rightArm.xRot, 0.4f);
				m.leftArm.xRot = Mth.lerp(b, m.leftArm.xRot, 0.4f);
				m.rightLeg.z = Mth.lerp(b, m.rightLeg.z, m.rightLeg.z + 4f);
				m.leftLeg.z = Mth.lerp(b, m.leftLeg.z, m.leftLeg.z + 4f);
				m.rightLeg.y = Mth.lerp(b, m.rightLeg.y, m.rightLeg.y + 0.2f);
				m.leftLeg.y = Mth.lerp(b, m.leftLeg.y, m.leftLeg.y + 0.2f);
			}
			case SPIN -> {
				m.rightArm.zRot = Mth.lerp(b, m.rightArm.zRot, 1.35f);
				m.leftArm.zRot = Mth.lerp(b, m.leftArm.zRot, -1.35f);
			}
		}
	}

	public static void reset() {
		Minecraft mc = Minecraft.getInstance();
		if (cameraBefore != null) {
			mc.options.setCameraType(cameraBefore);
			cameraBefore = null;
		}
		blend = 0f;
		wasDown = false;
	}
}
