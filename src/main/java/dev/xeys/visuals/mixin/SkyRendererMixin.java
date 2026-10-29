package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.render.sky.SkyDraw;
import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.client.renderer.SkyRenderer;
import net.minecraft.world.level.MoonPhase;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SkyRenderer.class)
abstract class SkyRendererMixin {
	/** XEYS sky layers, after vanilla's celestial bodies and before any terrain. */
	@Inject(method = "renderSunMoonAndStars", at = @At("TAIL"))
	private void xeys$skyLayers(PoseStack poseStack, float sunAngle, float moonAngle, float starAngle, MoonPhase moonPhase, float rainBrightness,
								float starBrightness, CallbackInfo ci) {
		SkyDraw.render(sunAngle, moonAngle, starAngle, rainBrightness, starBrightness);
	}

	@ModifyConstant(method = "renderSun", constant = @Constant(floatValue = 30.0F))
	private float xeys$sunSize(float size) {
		return size * WorldHooks.sunScale;
	}

	@ModifyConstant(method = "renderMoon", constant = @Constant(floatValue = 20.0F))
	private float xeys$moonSize(float size) {
		return size * WorldHooks.moonScale;
	}
}
