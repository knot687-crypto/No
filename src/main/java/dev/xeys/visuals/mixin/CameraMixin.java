package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xeys.visuals.anim.AnimHooks;
import net.minecraft.client.Camera;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(Camera.class)
abstract class CameraMixin {
	/** How quickly the eye height follows sneaking and standing. */
	@ModifyConstant(method = "tick", constant = @Constant(floatValue = 0.5F))
	private float xeys$eyeFollow(float follow) {
		return AnimHooks.eyeFollow;
	}

	/** Third-person distance, eased when switching perspective. */
	@WrapOperation(method = "setup", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Camera;getMaxZoom(F)F"))
	private float xeys$distance(Camera camera, float distance, Operation<Float> original) {
		return original.call(camera, distance * AnimHooks.cameraDistance);
	}
}
