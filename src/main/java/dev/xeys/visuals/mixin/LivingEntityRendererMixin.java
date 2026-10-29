package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.LivingStateAccess;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
abstract class LivingEntityRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",
		at = @At("TAIL"))
	private void xeys$hurtProgress(LivingEntity entity, LivingEntityRenderState state, float partialTick, CallbackInfo ci) {
		float hurt = entity.hurtTime > 0 && entity.hurtDuration > 0 ? Math.max(0f, (entity.hurtTime - partialTick) / entity.hurtDuration) : 0f;
		((LivingStateAccess) state).xeys$setHurt(hurt);
	}

	/** With a death style chosen, the body does not tip over; the style animates it instead (below). */
	@ModifyExpressionValue(method = "setupRotations", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;getFlipDegrees()F", ordinal = 0))
	private float xeys$noDeathFlip(float degrees) {
		return AnimHooks.deathStyle != null ? 0f : degrees;
	}

	@Inject(method = "setupRotations(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;FF)V",
		at = @At("TAIL"))
	private void xeys$squashAndDeath(LivingEntityRenderState state, PoseStack pose, float bodyRot, float scale, CallbackInfo ci) {
		AnimHooks.DeathStyle style = AnimHooks.deathStyle;
		if (style != null && state.deathTime > 0f) {
			// The pose is in units of the entity's scale here, with y up from its feet.
			float t = Math.min(1f, state.deathTime / 20f);
			float e = t * t * (3f - 2f * t);
			float h = state.boundingBoxHeight / scale;
			switch (style) {
				case SINK -> {
					pose.translate(0f, -e * h * 0.7f, 0f);
					float s = 1f - e * 0.35f;
					pose.scale(s, s, s);
				}
				case SHRINK -> {
					float s = Math.max(0.02f, 1f - e);
					pose.translate(0f, h * 0.5f * (1f - s), 0f);
					pose.scale(s, s, s);
				}
				case ASCEND -> {
					pose.translate(0f, e * 1.4f / scale, 0f);
					pose.mulPose(Axis.YP.rotationDegrees(e * 180f));
					float s = 1f - e * 0.55f;
					pose.scale(s, s, s);
				}
				case SPIN -> {
					pose.mulPose(Axis.YP.rotationDegrees(e * e * 900f));
					float s = Math.max(0.02f, 1f - e * 0.9f);
					pose.scale(s, s, s);
				}
			}
		}
		float hurt = ((LivingStateAccess) state).xeys$hurt();
		if (AnimHooks.squash > 0f && hurt > 0f) {
			// A quick squash that springs back: wide and short, then a little tall.
			float p = 1f - hurt;
			float wave = (float) (Math.sin(p * Math.PI * 2.0) * Math.exp(-p * 3.0));
			float k = wave * 0.22f * AnimHooks.squash;
			pose.scale(1f + k, 1f - k, 1f + k);
		}
	}
}
