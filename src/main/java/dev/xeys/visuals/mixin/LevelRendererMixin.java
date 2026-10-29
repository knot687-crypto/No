package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.resource.GraphicsResourceAllocator;
import dev.xeys.visuals.render.post.WorldPost;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelRenderer;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
	/** The level's view and projection, for reprojecting the next frame (motion blur, depth of field). */
	@Inject(method = "renderLevel", at = @At("HEAD"))
	private void xeys$captureMatrices(GraphicsResourceAllocator allocator, DeltaTracker deltaTracker, boolean renderOutline, Camera camera,
									  Matrix4f modelView, Matrix4f projection, Matrix4f cullingProjection, GpuBufferSlice fog, Vector4f fogColor,
									  boolean renderSky, CallbackInfo ci) {
		WorldPost.captureLevel(modelView, projection, camera.position());
	}
}
