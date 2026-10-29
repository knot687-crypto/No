package dev.xeys.visuals.render.sky;

import com.mojang.blaze3d.buffers.GpuBuffer;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector4f;

/**
 * Draws XEYS sky layers (aurora, meteors, stars) inside vanilla's sky pass: after the sun, moon and
 * stars, before any terrain, into the main target. Geometry lives on a sphere of radius 100 around the
 * camera, like vanilla's celestial bodies.
 */
public final class SkyDraw {
	public static final float RADIUS = 100f;

	private static final SkyContext CONTEXT = new SkyContext();
	private static final UniformSlots UNIFORMS = new UniformSlots("XEYS sky fx", 48, 1);
	private static final Matrix4f MODEL_VIEW = new Matrix4f();
	private static final Matrix4f IDENTITY = new Matrix4f();
	private static final Vector4f WHITE = new Vector4f(1f, 1f, 1f, 1f);
	private static final Vector3f ZERO = new Vector3f();
	private static final float[] PARAMS = new float[4];
	private static int colorA;
	private static int colorB;
	private static final UniformSlots.SlotWriter WRITER = (slot, b) -> {
		b.putVec4(PARAMS[0], PARAMS[1], PARAMS[2], PARAMS[3]);
		putColor(b, colorA);
		putColor(b, colorB);
	};

	private SkyDraw() {
	}

	/** Called from the sky pass; runs every sky listener. */
	public static void render(float sunAngle, float moonAngle, float starAngle, float rainBrightness, float starBrightness) {
		if (WorldEvents.SKY.isEmpty()) {
			return;
		}
		SkyContext c = CONTEXT;
		c.sunAngle = sunAngle;
		c.moonAngle = moonAngle;
		c.starAngle = starAngle;
		c.rainBrightness = rainBrightness;
		c.starBrightness = starBrightness;
		c.partialTick = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Object l : WorldEvents.SKY.snapshot()) {
			((WorldEvents.Sky) l).onSky(c);
		}
	}

	/**
	 * Draws {@code vertexCount} vertices (quads) from {@code vertices} with {@code pipeline}, rotated by
	 * {@code rotation} relative to the camera's view. {@code p0..p3} and the two colors reach the shader
	 * as its {@code SkyFx} block.
	 */
	public static void draw(RenderPipeline pipeline, GpuBuffer vertices, int vertexCount, Matrix4fc rotation, float p0, float p1, float p2,
							float p3, int a, int b) {
		if (vertexCount < 4 || !XeysPipelines.isUsable(pipeline)) {
			return;
		}
		RenderTarget main = Minecraft.getInstance().getMainRenderTarget();
		if (main.getDepthTextureView() == null) {
			return;
		}
		MODEL_VIEW.set(RenderSystem.getModelViewStack()).mul(rotation);
		GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(MODEL_VIEW, WHITE, ZERO, IDENTITY);
		PARAMS[0] = p0;
		PARAMS[1] = p1;
		PARAMS[2] = p2;
		PARAMS[3] = p3;
		colorA = a;
		colorB = b;
		UNIFORMS.write(1, WRITER);
		RenderSystem.AutoStorageIndexBuffer quads = RenderSystem.getSequentialBuffer(VertexFormat.Mode.QUADS);
		int indexCount = vertexCount / 4 * 6;
		GpuBuffer indices = quads.getBuffer(indexCount);
		try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "XEYS sky", main.getColorTextureView(),
			OptionalInt.empty(), main.getDepthTextureView(), OptionalDouble.empty())) {
			pass.setPipeline(pipeline);
			RenderSystem.bindDefaultUniforms(pass);
			pass.setUniform("DynamicTransforms", transforms);
			pass.setUniform("SkyFx", UNIFORMS.slot(0));
			pass.setVertexBuffer(0, vertices);
			pass.setIndexBuffer(indices, quads.type());
			pass.drawIndexed(0, 0, indexCount, 1);
		}
	}

	private static void putColor(Std140Builder b, int argb) {
		b.putVec4(((argb >> 16) & 0xFF) / 255f, ((argb >> 8) & 0xFF) / 255f, (argb & 0xFF) / 255f, ((argb >>> 24) & 0xFF) / 255f);
	}
}
