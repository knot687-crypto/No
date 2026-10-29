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
}
