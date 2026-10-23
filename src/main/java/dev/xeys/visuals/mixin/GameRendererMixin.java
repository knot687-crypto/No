package dev.xeys.visuals.mixin;

import dev.xeys.visuals.render.RenderFrame;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.render.state.GuiRenderState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Shadow
	@Final
	private GuiRenderState guiRenderState;

	@Inject(method = "render", at = @At("HEAD"))
	private void xeys$beginFrame(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderFrame.beginFrame();
	}

	@Inject(
		method = "render",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/render/GuiRenderer;render(Lcom/mojang/blaze3d/buffers/GpuBufferSlice;)V")
	)
	private void xeys$beforeGuiFlush(DeltaTracker deltaTracker, boolean renderLevel, CallbackInfo ci) {
		RenderFrame.beforeGuiFlush(this.guiRenderState);
	}
}
