package dev.xeys.visuals.cosmetic;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Shapes for cosmetics in the entity vertex format (lit by the entity shaders, colored per vertex on a
 * white texture). Coordinates are in the current pose's space. Triangles are quads with a repeated
 * corner, since entity render types draw quads.
 */
public final class CosmeticMesh {
	private CosmeticMesh() {
	}

	public static void vertex(VertexConsumer vc, PoseStack.Pose pose, float x, float y, float z, float u, float v, int color, int light,
							  float nx, float ny, float nz) {
		vc.addVertex(pose, x, y, z).setColor(color).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
	}

	/** A flat quad; the normal follows from the corner order (counter-clockwise when seen from its front). */
	public static void quad(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
							float cz, float dx, float dy, float dz, int color, int light) {
		float ux = bx - ax;
		float uy = by - ay;
		float uz = bz - az;
		float wx = dx - ax;
		float wy = dy - ay;
		float wz = dz - az;
		float nx = uy * wz - uz * wy;
		float ny = uz * wx - ux * wz;
		float nz = ux * wy - uy * wx;
		float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		if (len > 1e-7f) {
			nx /= len;
			ny /= len;
			nz /= len;
		} else {
			ny = -1f;
		}
		vertex(vc, pose, ax, ay, az, 0f, 0f, color, light, nx, ny, nz);
		vertex(vc, pose, bx, by, bz, 0f, 1f, color, light, nx, ny, nz);
		vertex(vc, pose, cx, cy, cz, 1f, 1f, color, light, nx, ny, nz);
		vertex(vc, pose, dx, dy, dz, 1f, 0f, color, light, nx, ny, nz);
	}

	public static void triangle(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx,
								float cy, float cz, int color, int light) {
		quad(vc, pose, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, color, light);
	}

	/** An axis-aligned box. */
	public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int color, int light) {
		box(vc, pose, x0, y0, z0, x1, y1, z1, color, color, light);
	}

	/** An axis-aligned box whose faces at y0 and y1 differ in color, with the sides blending between them. */
	public static void box(VertexConsumer vc, PoseStack.Pose pose, float x0, float y0, float z0, float x1, float y1, float z1, int c0, int c1,
						   int light) {
		face(vc, pose, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, c0, c0, 0f, -1f, 0f, light);
		face(vc, pose, x0, y1, z0, x0, y1, z1, x1, y1, z1, x1, y1, z0, c1, c1, 0f, 1f, 0f, light);
		face(vc, pose, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, c0, c1, 0f, 0f, -1f, light);
		face(vc, pose, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, c0, c1, 0f, 0f, 1f, light);
		face(vc, pose, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, c0, c1, -1f, 0f, 0f, light);
		face(vc, pose, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, c0, c1, 1f, 0f, 0f, light);
	}

	/** A face whose first two corners take {@code ca} where they sit at the box's y0 side, else {@code cb}. */
	private static void face(VertexConsumer vc, PoseStack.Pose pose, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy,
							 float cz, float dx, float dy, float dz, int ca, int cb, float nx, float ny, float nz, int light) {
		float y0 = Math.min(Math.min(ay, by), Math.min(cy, dy));
		vertex(vc, pose, ax, ay, az, 0f, 0f, ay == y0 ? ca : cb, light, nx, ny, nz);
		vertex(vc, pose, bx, by, bz, 0f, 1f, by == y0 ? ca : cb, light, nx, ny, nz);
		vertex(vc, pose, cx, cy, cz, 1f, 1f, cy == y0 ? ca : cb, light, nx, ny, nz);
		vertex(vc, pose, dx, dy, dz, 1f, 0f, dy == y0 ? ca : cb, light, nx, ny, nz);
	}

	/**
	 * A cone (or a frustum when {@code topRadius} > 0) around the y axis at (cx, cy, cz), rising by
	 * {@code height} (negative is up in model space). Smooth normals.
	 */
	public static void cone(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float baseRadius, float topRadius, float height,
							int segments, int baseColor, int topColor, int light, boolean capBase) {
		float slope = (baseRadius - topRadius) / Math.abs(height);
		float up = Math.signum(height);
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2.0 * i / segments;
			double a1 = Math.PI * 2.0 * (i + 1) / segments;
			float c0 = (float) Math.cos(a0);
			float s0 = (float) Math.sin(a0);
			float c1 = (float) Math.cos(a1);
			float s1 = (float) Math.sin(a1);
			// Outward normal, tilted by the slope toward the tip.
			float n = (float) (1.0 / Math.sqrt(1.0 + slope * slope));
			float ny = up * slope * n;
			vertex(vc, pose, cx + c0 * baseRadius, cy, cz + s0 * baseRadius, 0f, 0f, baseColor, light, c0 * n, ny, s0 * n);
			vertex(vc, pose, cx + c0 * topRadius, cy + height, cz + s0 * topRadius, 0f, 1f, topColor, light, c0 * n, ny, s0 * n);
			vertex(vc, pose, cx + c1 * topRadius, cy + height, cz + s1 * topRadius, 1f, 1f, topColor, light, c1 * n, ny, s1 * n);
			vertex(vc, pose, cx + c1 * baseRadius, cy, cz + s1 * baseRadius, 1f, 0f, baseColor, light, c1 * n, ny, s1 * n);
			if (capBase) {
				vertex(vc, pose, cx, cy, cz, 0.5f, 0.5f, baseColor, light, 0f, -up, 0f);
				vertex(vc, pose, cx + c1 * baseRadius, cy, cz + s1 * baseRadius, 1f, 0f, baseColor, light, 0f, -up, 0f);
				vertex(vc, pose, cx + c0 * baseRadius, cy, cz + s0 * baseRadius, 0f, 0f, baseColor, light, 0f, -up, 0f);
				vertex(vc, pose, cx, cy, cz, 0.5f, 0.5f, baseColor, light, 0f, -up, 0f);
			}
		}
	}

	/** A flat disc in the xz plane at height y. */
	public static void disc(VertexConsumer vc, PoseStack.Pose pose, float cx, float y, float cz, float radius, int segments, int color, int light) {
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2.0 * i / segments;
			double a1 = Math.PI * 2.0 * (i + 1) / segments;
			vertex(vc, pose, cx, y, cz, 0.5f, 0.5f, color, light, 0f, -1f, 0f);
			vertex(vc, pose, cx + (float) Math.cos(a0) * radius, y, cz + (float) Math.sin(a0) * radius, 0f, 0f, color, light, 0f, -1f, 0f);
			vertex(vc, pose, cx + (float) Math.cos(a1) * radius, y, cz + (float) Math.sin(a1) * radius, 1f, 0f, color, light, 0f, -1f, 0f);
			vertex(vc, pose, cx, y, cz, 0.5f, 0.5f, color, light, 0f, -1f, 0f);
		}
	}

	/** A torus around the y axis: {@code radius} to the tube's center, {@code tube} the tube's radius. */
	public static void torus(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float radius, float tube, int segments, int sides,
							 int color, int light) {
		for (int i = 0; i < segments; i++) {
			double a0 = Math.PI * 2.0 * i / segments;
			double a1 = Math.PI * 2.0 * (i + 1) / segments;
			for (int j = 0; j < sides; j++) {
				double b0 = Math.PI * 2.0 * j / sides;
				double b1 = Math.PI * 2.0 * (j + 1) / sides;
				torusVertex(vc, pose, cx, cy, cz, radius, tube, a0, b0, color, light);
				torusVertex(vc, pose, cx, cy, cz, radius, tube, a0, b1, color, light);
				torusVertex(vc, pose, cx, cy, cz, radius, tube, a1, b1, color, light);
				torusVertex(vc, pose, cx, cy, cz, radius, tube, a1, b0, color, light);
			}
		}
	}

	private static void torusVertex(VertexConsumer vc, PoseStack.Pose pose, float cx, float cy, float cz, float radius, float tube, double a, double b,
									int color, int light) {
		float ca = (float) Math.cos(a);
		float sa = (float) Math.sin(a);
		float cb = (float) Math.cos(b);
		float sb = (float) Math.sin(b);
		float nx = ca * cb;
		float ny = sb;
		float nz = sa * cb;
		vertex(vc, pose, cx + (radius + tube * cb) * ca, cy + tube * sb, cz + (radius + tube * cb) * sa, (float) (a / (Math.PI * 2)),
			(float) (b / (Math.PI * 2)), color, light, nx, ny, nz);
	}
}
