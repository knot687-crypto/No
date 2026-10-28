package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.DamageDirectionModule;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
abstract class PlayerMixin {
	/** The server tells the client which way a hit came from; the damage indicator points there. */
	@Inject(method = "animateHurt", at = @At("TAIL"))
	private void xeys$hurtFrom(float yaw, CallbackInfo ci) {
		if ((Object) this == Minecraft.getInstance().player) {
			DamageDirectionModule.onLocalHurt(yaw);
		}
	}
}
