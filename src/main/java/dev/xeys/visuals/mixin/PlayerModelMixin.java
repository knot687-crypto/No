package dev.xeys.visuals.mixin;

import dev.xeys.visuals.anim.Emotes;
import dev.xeys.visuals.cosmetic.Cosmetics;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerModel.class)
abstract class PlayerModelMixin {
	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V", at = @At("TAIL"))
	private void xeys$emote(AvatarRenderState state, CallbackInfo ci) {
		if (Emotes.active() && Cosmetics.isSelf(state.id)) {
			Emotes.pose((PlayerModel) (Object) this);
		}
	}
}
