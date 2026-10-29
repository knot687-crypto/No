package dev.xeys.visuals.render.post;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuSampler;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.textures.TextureFormat;
import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.WorldEvents;
import dev.xeys.visuals.render.UniformSlots;
import dev.xeys.visuals.render.XeysPipelines;
import dev.xeys.visuals.render.blur.BlurChain;
import java.util.OptionalInt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

/**
 * Post-processing of the world image itself, after the level is drawn and before the hand, HUD and
 * menus: camera motion blur (reprojected from the depth buffer), depth of field and bloom, all
 * composited in one pass. Frames with no active effect cost nothing beyond two matrix copies.
 */
public final class WorldPost {
	private static final int UBO_SIZE = 64 * 2 + 16 * 6;
	private static final UniformSlots UNIFORMS = new UniformSlots("XEYS world post", UBO_SIZE, 1);
	private static final WorldFx FX = new WorldFx();
	private static final BlurChain DOF = new BlurChain("XEYS depth of field");
	private static final BlurChain BLOOM = new BlurChain("XEYS bloom");
	/** Moving further than this between frames is a teleport, not motion. */
	private static final double TELEPORT = 6.0;

	private static final Matrix4f VIEW_PROJ = new Matrix4f();
	private static final Matrix4f PREV_VIEW_PROJ = new Matrix4f();
	private static final Matrix4f INV_VIEW_PROJ = new Matrix4f();
	private static double camX;
	private static double camY;
	private static double camZ;
	private static double prevX;
	private static double prevY;
	private static double prevZ;
	private static int captured;
	private static @Nullable GpuTexture copy;
	private static @Nullable GpuTextureView copyView;

	private WorldPost() {
	}

	/** Records this frame's level matrices; the previous frame's become the reprojection source. */
	public static void captureLevel(Matrix4fc view, Matrix4fc projection, Vec3 camera) {
		PREV_VIEW_PROJ.set(VIEW_PROJ);
		prevX = camX;
		prevY = camY;
		prevZ = camZ;
		VIEW_PROJ.set(projection).mul(view);
		camX = camera.x;
		camY = camera.y;
		camZ = camera.z;
		captured = Math.min(captured + 1, 2);
	}

	/** Forgets the previous frame (after a world change), so nothing reprojects across it. */
	public static void resetHistory() {
		captured = 0;
	}

	/** Collects this frame's effects and applies them. Render thread, outside any render pass. */
	public static void process() {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || WorldEvents.WORLD_POST.isEmpty() || !XeysPipelines.isUsable(XeysPipelines.WORLD_POST)) {
			return;
		}
		FX.reset();
		float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
		for (Object l : WorldEvents.WORLD_POST.snapshot()) {
			((WorldEvents.WorldPostEffect) l).contributeWorld(FX, partial);
		}
		if (!FX.active()) {
			return;
		}
		RenderTarget main = mc.getMainRenderTarget();
		if (main.getDepthTextureView() == null) {
			return;
		}
		int w = main.width;
		int h = main.height;
		ensureCopy(w, h);
		CommandEncoder encoder = RenderSystem.getDevice().createCommandEncoder();
		encoder.copyTextureToTexture(main.getColorTexture(), copy, 0, 0, 0, 0, 0, w, h);

		boolean blurs = XeysPipelines.isUsable(XeysPipelines.BLUR_DOWN) && XeysPipelines.isUsable(XeysPipelines.BLUR_UP);
		boolean dof = FX.dof > 0f && blurs;
		boolean bloom = FX.bloom > 0f && blurs && XeysPipelines.isUsable(XeysPipelines.BLOOM_PREFILTER)
			&& XeysPipelines.isUsable(XeysPipelines.BLOOM_UP);
		if (dof) {
			DOF.ensure(w, h);
			DOF.run(copyView, FX.dofLevels, 1.4f, null, 0f);
		}
		if (bloom) {
			BLOOM.ensure(w, h);
			BLOOM.run(copyView, FX.bloomLevels, 1.0f, XeysPipelines.BLOOM_PREFILTER, FX.bloomThreshold, XeysPipelines.BLOOM_UP);
		}
		write(w, h, dof, bloom);

		GpuSampler linear = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
		GpuSampler nearest = RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST);
		try (RenderPass pass = encoder.createRenderPass(() -> "XEYS world post", main.getColorTextureView(), OptionalInt.empty())) {
			pass.setPipeline(XeysPipelines.WORLD_POST);
			pass.bindTexture("SceneSampler", copyView, linear);
			pass.bindTexture("DepthSampler", main.getDepthTextureView(), nearest);
			pass.bindTexture("DofSampler", dof ? DOF.result() : copyView, linear);
			pass.bindTexture("BloomSampler", bloom ? BLOOM.result() : copyView, linear);
			pass.setUniform("WorldPost", UNIFORMS.slot(0));
			pass.draw(0, 3);
		}
	}

	private static void write(int w, int h, boolean dof, boolean bloom) {
		INV_VIEW_PROJ.set(VIEW_PROJ).invert();
		float dx = (float) (camX - prevX);
		float dy = (float) (camY - prevY);
		float dz = (float) (camZ - prevZ);
		float motion = 0f;
		if (FX.motion > 0f && captured >= 2 && dx * dx + dy * dy + dz * dz < TELEPORT * TELEPORT) {
			// Blur the motion of a fixed shutter time, so the streak length does not depend on frame rate.
			float dt = Math.max(Clock.delta(), 1f / 1000f);
			motion = Math.min(FX.motion * (1f / 60f) / dt, 4f);
		}
		float m = motion;
		WorldFx f = FX;
		UNIFORMS.write(1, (slot, b) -> {
			b.putMat4f(INV_VIEW_PROJ);
			b.putMat4f(PREV_VIEW_PROJ);
			b.putVec4(dx, dy, dz, m);
			b.putVec4(f.motionSamples, f.motionMax, 0f, 0f);
			b.putVec4(Math.max(f.focus, 0.1f), f.aperture, dof ? f.dof : 0f, 0f);
			// Every level was summed into the result; average them so strength means the same at any radius.
			b.putVec4(bloom ? f.bloom / Math.max(1, f.bloomLevels) : 0f, 0f, 0f, 0f);
			b.putVec4(((f.bloomTint >> 16) & 0xFF) / 255f, ((f.bloomTint >> 8) & 0xFF) / 255f, (f.bloomTint & 0xFF) / 255f, 1f);
			b.putVec4(w, h, 1f / w, 1f / h);
		});
	}

	private static void ensureCopy(int w, int h) {
		if (copy != null && !copy.isClosed() && copy.getWidth(0) == w && copy.getHeight(0) == h) {
			return;
		}
		if (copyView != null) {
			copyView.close();
		}
		if (copy != null) {
			copy.close();
		}
		GpuDevice device = RenderSystem.getDevice();
		copy = device.createTexture(() -> "XEYS world post source", GpuTexture.USAGE_TEXTURE_BINDING | GpuTexture.USAGE_COPY_DST,
			TextureFormat.RGBA8, w, h, 1, 1);
		copyView = device.createTextureView(copy);
	}
}
