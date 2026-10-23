package dev.xeys.visuals.mixin;

import dev.xeys.visuals.ui.menu.MoonBackdrop;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PanoramaRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PanoramaRenderer.class)
public abstract class PanoramaRendererMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void xeys$moonBackdrop(GuiGraphics graphics, int width, int height, boolean spin, CallbackInfo ci) {
		if (MoonBackdrop.render()) {
			ci.cancel();
		}
	}
}
