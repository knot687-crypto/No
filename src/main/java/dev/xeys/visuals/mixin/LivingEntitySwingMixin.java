package dev.xeys.visuals.mixin;

import dev.xeys.visuals.anim.AnimHooks;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
abstract class LivingEntitySwingMixin {
	/** Your own swing's on-screen length; the attack itself and other players are untouched. */
	@Inject(method = "getCurrentSwingDuration", at = @At("RETURN"), cancellable = true)
	private void xeys$swingSpeed(CallbackInfoReturnable<Integer> cir) {
		if (AnimHooks.swingSpeed != 1f && AnimHooks.isSelf(this)) {
			cir.setReturnValue(Math.max(1, Math.round(cir.getReturnValueI() / AnimHooks.swingSpeed)));
		}
	}
}
