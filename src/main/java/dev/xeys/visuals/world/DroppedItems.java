package dev.xeys.visuals.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;

/**
 * How dropped items are posed. Item Physics lays flat items down on the ground at a random resting angle;
 * Dropped Item Style tunes vanilla's bob, spin and size. Both feed the item renderer mixin.
 */
public final class DroppedItems {
	/** Item Physics on. */
	public static boolean physics;
	/** Dropped Item Style: scale, bob height multiplier and spin speed multiplier. */
	public static float scale = 1f;
	public static float bob = 1f;
	public static float spin = 1f;

	private DroppedItems() {
	}

	private static boolean styled() {
		return scale != 1f || bob != 1f || spin != 1f;
	}

	private static boolean flat(ItemEntityRenderState state) {
		// Flat items (most of them) are a sixteenth thick; blocks keep standing upright.
		return state.item.getModelBoundingBox().getZsize() <= 0.07;
	}

	/** Returns true if it applied its own translation instead of vanilla's. */
	public static boolean transformBob(PoseStack pose, ItemEntityRenderState state, float vanillaY) {
		if (physics) {
			AABB box = state.item.getModelBoundingBox();
			if (flat(state)) {
				// Lying on the ground: lifted by its half-thickness so it rests on the surface.
				pose.translate(0f, (float) (box.getZsize() * 0.5 * scale) + 0.002f, 0f);
			} else {
				pose.translate(0f, (float) -box.minY * scale + 0.002f, 0f);
			}
			if (scale != 1f) {
				pose.scale(scale, scale, scale);
			}
			return true;
		}
		if (styled()) {
			AABB box = state.item.getModelBoundingBox();
			float base = -((float) box.minY) + 0.0625f;
			float wave = Mth.sin(state.ageInTicks / 10f + state.bobOffset) * 0.1f + 0.1f;
			pose.translate(0f, base * scale + wave * bob, 0f);
			if (scale != 1f) {
				pose.scale(scale, scale, scale);
			}
			return true;
		}
		return false;
	}

	public static boolean transformSpin(PoseStack pose, ItemEntityRenderState state) {
		if (physics) {
			// A stable random yaw per item entity, then flat items tip over onto the ground.
			pose.mulPose(Axis.YP.rotation(state.bobOffset * 4.7f));
			if (flat(state)) {
				pose.mulPose(Axis.XP.rotationDegrees(90f));
			}
			return true;
		}
		if (spin != 1f) {
			pose.mulPose(Axis.YP.rotation(ItemEntity.getSpin(state.ageInTicks * spin, state.bobOffset)));
			return true;
		}
		return false;
	}
}
