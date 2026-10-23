package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.buffers.GpuBufferSlice;
import dev.xeys.visuals.render.RenderFrame;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.render.GuiRenderer;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "render", at = @At("HEAD"))
	private void xeys$beginFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderFrame.beginFrame();
	}

	@Inject(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
	)
	private void xeys$beforeGuiFlush(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderFrame.beforeGuiFlush();
	}
}
