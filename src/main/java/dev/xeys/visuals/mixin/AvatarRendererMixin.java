package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.cosmetic.CosmeticHooks;
import net.minecraft.client.renderer.entity.player.AvatarRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.Avatar;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AvatarRenderer.class)
abstract class AvatarRendererMixin {
	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/Avatar;Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;F)V",
		at = @At("TAIL"))
	private void xeys$cosmetics(Avatar avatar, AvatarRenderState state, float partialTick, CallbackInfo ci) {
		CosmeticHooks.extract(avatar, state);
	}

	@Inject(method = "scale(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;)V", at = @At("TAIL"))
	private void xeys$scale(AvatarRenderState state, PoseStack poseStack, CallbackInfo ci) {
		float s = CosmeticHooks.scale(state);
		if (s != 1f) {
			poseStack.scale(s, s, s);
		}
	}
}
