package dev.xeys.visuals.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.xeys.visuals.render.XeysPipelines;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.joml.Vector3fc;

/**
 * Additive glow sprites in the world: camera-facing dots and vertical beams, faded by fog and hidden by
 * terrain. Coordinates are camera-relative. Call {@link #face(Camera)} once before drawing dots.
 */
public final class WorldGlow {
	public static final RenderType DOTS = RenderType.create("xeys_glow_dots", RenderSetup.builder(XeysPipelines.WORLD_GLOW_DOT).createRenderSetup());
	public static final RenderType BEAMS = RenderType.create("xeys_glow_beams", RenderSetup.builder(XeysPipelines.WORLD_GLOW_BEAM).createRenderSetup());
	public static final RenderType RINGS = RenderType.create("xeys_glow_rings", RenderSetup.builder(XeysPipelines.WORLD_GLOW_RING).createRenderSetup());

	private static float leftX;
	private static float leftY;
	private static float leftZ;
	private static float upX;
	private static float upY;
	private static float upZ;

	private WorldGlow() {
	}

	public static boolean dotsUsable() {
		return XeysPipelines.isUsable(XeysPipelines.WORLD_GLOW_DOT);
	}

	public static boolean beamsUsable() {
		return XeysPipelines.isUsable(XeysPipelines.WORLD_GLOW_BEAM);
	}

	public static boolean ringsUsable() {
		return XeysPipelines.isUsable(XeysPipelines.WORLD_GLOW_RING);
	}

	/** Takes the camera's orientation for billboarding. */
	public static void face(Camera camera) {
		Vector3fc l = camera.leftVector();
		Vector3fc u = camera.upVector();
		leftX = l.x();
		leftY = l.y();
		leftZ = l.z();
		upX = u.x();
		upY = u.y();
		upZ = u.z();
	}

	/** A round glow of {@code radius} blocks, facing the camera. */
	public static void dot(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float radius, int argb) {
		float lx = leftX * radius;
		float ly = leftY * radius;
		float lz = leftZ * radius;
		float ux = upX * radius;
		float uy = upY * radius;
		float uz = upZ * radius;
		vc.addVertex(pose, x + lx - ux, y + ly - uy, z + lz - uz).setUv(-1f, -1f).setColor(argb);
		vc.addVertex(pose, x + lx + ux, y + ly + uy, z + lz + uz).setUv(-1f, 1f).setColor(argb);
		vc.addVertex(pose, x - lx + ux, y - ly + uy, z - lz + uz).setUv(1f, 1f).setColor(argb);
		vc.addVertex(pose, x - lx - ux, y - ly - uy, z - lz - uz).setUv(1f, -1f).setColor(argb);
	}

	/** A vertical beam rising {@code height} blocks from (x, y, z), turned about its axis to face the camera. */
	public static void beam(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float height, float halfWidth, int argb) {
		float len = (float) Math.sqrt(x * x + z * z);
		float sx;
		float sz;
		if (len < 1e-4f) {
			sx = leftX;
			sz = leftZ;
		} else {
			sx = z / len;
			sz = -x / len;
		}
		sx *= halfWidth;
		sz *= halfWidth;
		vc.addVertex(pose, x - sx, y, z - sz).setUv(-1f, 0f).setColor(argb);
		vc.addVertex(pose, x + sx, y, z + sz).setUv(1f, 0f).setColor(argb);
		vc.addVertex(pose, x + sx, y + height, z + sz).setUv(1f, 1f).setColor(argb);
		vc.addVertex(pose, x - sx, y + height, z - sz).setUv(-1f, 1f).setColor(argb);
	}

	/**
	 * A sprite on the quad centered at (x, y, z) and spanned by the half-axes a and b, in any orientation.
	 * Drawn into {@link #RINGS} it is a ring at 80% of the half-axes; into {@link #DOTS}, a soft dot.
	 */
	public static void sprite(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float ax, float ay, float az, float bx, float by,
							  float bz, int argb) {
		vc.addVertex(pose, x - ax - bx, y - ay - by, z - az - bz).setUv(-1f, -1f).setColor(argb);
		vc.addVertex(pose, x - ax + bx, y - ay + by, z - az + bz).setUv(-1f, 1f).setColor(argb);
		vc.addVertex(pose, x + ax + bx, y + ay + by, z + az + bz).setUv(1f, 1f).setColor(argb);
		vc.addVertex(pose, x + ax - bx, y + ay - by, z + az - bz).setUv(1f, -1f).setColor(argb);
	}

	/** A flat ring lying in the horizontal plane. */
	public static void flatRing(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float radius, int argb) {
		float r = radius / 0.8f;
		sprite(vc, pose, x, y, z, r, 0f, 0f, 0f, 0f, r, argb);
	}

	/** A soft dot lying in the horizontal plane (footprints, ground glows). */
	public static void flatDot(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float rx, float rz, int argb) {
		vc.addVertex(pose, x - rx, y, z - rz).setUv(-1f, -1f).setColor(argb);
		vc.addVertex(pose, x - rx, y, z + rz).setUv(-1f, 1f).setColor(argb);
		vc.addVertex(pose, x + rx, y, z + rz).setUv(1f, 1f).setColor(argb);
		vc.addVertex(pose, x + rx, y, z - rz).setUv(1f, -1f).setColor(argb);
	}

	/**
	 * A glowing segment between two camera-relative points, turned to face the camera, with an even
	 * brightness along its length (draw into {@link #BEAMS}).
	 */
	public static void segment(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, float halfWidth,
							   int argb) {
		float dx = x1 - x0;
		float dy = y1 - y0;
		float dz = z1 - z0;
		// Perpendicular to both the segment and the line of sight to its middle.
		float mx = (x0 + x1) * 0.5f;
		float my = (y0 + y1) * 0.5f;
		float mz = (z0 + z1) * 0.5f;
		float sx = dy * mz - dz * my;
		float sy = dz * mx - dx * mz;
		float sz = dx * my - dy * mx;
		float l = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
		if (l < 1e-6f) {
			return;
		}
		sx = sx / l * halfWidth;
		sy = sy / l * halfWidth;
		sz = sz / l * halfWidth;
		vc.addVertex(pose, x0 - sx, y0 - sy, z0 - sz).setUv(-1f, 0.3f).setColor(argb);
		vc.addVertex(pose, x0 + sx, y0 + sy, z0 + sz).setUv(1f, 0.3f).setColor(argb);
		vc.addVertex(pose, x1 + sx, y1 + sy, z1 + sz).setUv(1f, 0.3f).setColor(argb);
		vc.addVertex(pose, x1 - sx, y1 - sy, z1 - sz).setUv(-1f, 0.3f).setColor(argb);
	}
}
