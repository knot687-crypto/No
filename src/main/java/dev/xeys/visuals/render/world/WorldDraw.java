package dev.xeys.visuals.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** World-space drawing helpers for camera-relative lines and boxes. */
public final class WorldDraw {
	private WorldDraw() {
	}

	/** One line segment (vanilla line format: position, color, normal, width in framebuffer pixels). */
	public static void line(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, float width) {
		float nx = x1 - x0;
		float ny = y1 - y0;
		float nz = z1 - z0;
		float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len < 1e-6f) {
			return;
		}
		nx /= len;
		ny /= len;
		nz /= len;
		vc.addVertex(pose, x0, y0, z0).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
		vc.addVertex(pose, x1, y1, z1).setColor(color).setNormal(pose, nx, ny, nz).setLineWidth(width);
	}

	/** The 12 edges of a box; with {@code corners} > 0 only that fraction of each edge at both ends. */
	public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, float width,
						   float corners) {
		edge(vc, pose, x0, y0, z0, x1, y0, z0, color, width, corners);
		edge(vc, pose, x0, y0, z1, x1, y0, z1, color, width, corners);
		edge(vc, pose, x0, y1, z0, x1, y1, z0, color, width, corners);
		edge(vc, pose, x0, y1, z1, x1, y1, z1, color, width, corners);
		edge(vc, pose, x0, y0, z0, x0, y1, z0, color, width, corners);
		edge(vc, pose, x1, y0, z0, x1, y1, z0, color, width, corners);
		edge(vc, pose, x0, y0, z1, x0, y1, z1, color, width, corners);
		edge(vc, pose, x1, y0, z1, x1, y1, z1, color, width, corners);
		edge(vc, pose, x0, y0, z0, x0, y0, z1, color, width, corners);
		edge(vc, pose, x1, y0, z0, x1, y0, z1, color, width, corners);
		edge(vc, pose, x0, y1, z0, x0, y1, z1, color, width, corners);
		edge(vc, pose, x1, y1, z0, x1, y1, z1, color, width, corners);
	}

	private static void edge(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, float width,
							 float corners) {
		if (corners <= 0f || corners >= 0.5f) {
			line(vc, pose, x0, y0, z0, x1, y1, z1, color, width);
			return;
		}
		float dx = (x1 - x0) * corners;
		float dy = (y1 - y0) * corners;
		float dz = (z1 - z0) * corners;
		line(vc, pose, x0, y0, z0, x0 + dx, y0 + dy, z0 + dz, color, width);
		line(vc, pose, x1 - dx, y1 - dy, z1 - dz, x1, y1, z1, color, width);
	}

	/** Every edge of a voxel shape, offset by (ox, oy, oz). */
	public static void shape(VertexConsumer vc, PoseStack.Pose pose, VoxelShape shape, double ox, double oy, double oz, int color, float width) {
		shape.forAllEdges((ax, ay, az, bx, by, bz) -> line(vc, pose, (float) (ax + ox), (float) (ay + oy), (float) (az + oz), (float) (bx + ox),
			(float) (by + oy), (float) (bz + oz), color, width));
	}

	/** The six faces of a box as quads (for translucent fills). */
	public static void filledBox(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color) {
		quad(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, color);
		quad(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, color);
		quad(vc, pose, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, color);
		quad(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, color);
		quad(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, color);
		quad(vc, pose, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, color);
	}

	public static void filledBox(VertexConsumer vc, PoseStack.Pose pose, AABB box, double ox, double oy, double oz, int color) {
		filledBox(vc, pose, (float) (box.minX + ox), (float) (box.minY + oy), (float) (box.minZ + oz), (float) (box.maxX + ox), (float) (box.maxY + oy),
			(float) (box.maxZ + oz), color);
	}

	public static void quad(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
							float cz, float dx, float dy, float dz, int color) {
		vc.addVertex(pose, ax, ay, az).setColor(color);
		vc.addVertex(pose, bx, by, bz).setColor(color);
		vc.addVertex(pose, cx, cy, cz).setColor(color);
		vc.addVertex(pose, dx, dy, dz).setColor(color);
	}
}
