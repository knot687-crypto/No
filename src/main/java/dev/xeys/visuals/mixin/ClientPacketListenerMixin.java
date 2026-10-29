package dev.xeys.visuals.mixin;

import dev.xeys.visuals.anim.CameraMotion;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
abstract class ClientPacketListenerMixin {
	/** At the tail, so it runs once on the game thread (the network thread bails out at the head). */
	@Inject(method = "handleExplosion", at = @At("TAIL"))
	private void xeys$explosionShake(ClientboundExplodePacket packet, CallbackInfo ci) {
		CameraMotion.explosion(packet.center(), packet.radius());
	}
}
