package dev.xeys.visuals.mixin;

import dev.xeys.visuals.render.world.CameraFx;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererFovMixin {
	@Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
	private void xeys$applyFov(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Float> cir) {
		float m = CameraFx.fovMultiplier(partialTick);
		if (m != 1f) {
			cir.setReturnValue(cir.getReturnValueF() * m);
		}
	}
}
