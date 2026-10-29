package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.CameraMotion;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.entity.ClientAvatarState;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Slice;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
abstract class GameRendererCameraMixin {
	/** Camera motion joins the view transform, like bobbing (the level only; the hand stays steady). */
	@Inject(method = "renderLevel", at = @At(value = "INVOKE", target = "Lorg/joml/Matrix4f;mul(Lorg/joml/Matrix4fc;)Lorg/joml/Matrix4f;", ordinal = 0))
	private void xeys$cameraMotion(DeltaTracker deltaTracker, CallbackInfo ci, @Local PoseStack poseStack) {
		CameraMotion.apply(poseStack, deltaTracker.getGameTimeDeltaPartialTick(true));
	}

	/** View bobbing can show even with vanilla's option off. */
	@ModifyExpressionValue(method = {"renderLevel", "renderItemInHand"}, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;", ordinal = 0),
		slice = @Slice(from = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;bobView()Lnet/minecraft/client/OptionInstance;")))
	private Object xeys$forceBob(Object enabled) {
		return AnimHooks.forceBob ? Boolean.TRUE : enabled;
	}

	@WrapOperation(method = "renderLevel", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void xeys$worldBob(GameRenderer self, PoseStack pose, float partialTick, Operation<Void> original) {
		AnimHooks.bobScale = AnimHooks.worldBob;
		original.call(self, pose, partialTick);
		AnimHooks.bobScale = 1f;
	}

	@WrapOperation(method = "renderItemInHand", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GameRenderer;bobView(Lcom/mojang/blaze3d/vertex/PoseStack;F)V"))
	private void xeys$handBob(GameRenderer self, PoseStack pose, float partialTick, Operation<Void> original) {
		AnimHooks.bobScale = AnimHooks.handBob;
		original.call(self, pose, partialTick);
		AnimHooks.bobScale = 1f;
	}

	@WrapOperation(method = "bobView", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/entity/ClientAvatarState;getInterpolatedBob(F)F"))
	private float xeys$bobbing(ClientAvatarState state, float partialTick, Operation<Float> original) {
		return original.call(state, partialTick) * AnimHooks.bobScale;
	}

	@WrapOperation(method = "bobHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
	private Object xeys$hurtTilt(OptionInstance<?> option, Operation<Object> original) {
		Object value = original.call(option);
		return AnimHooks.hurtTilt == 1f || !(value instanceof Double d) ? value : (Object) (d * AnimHooks.hurtTilt);
	}
}
