package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.hud.PotionHudModule;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
abstract class GuiMixin {
	/** The Potion Effects HUD replaces the vanilla corner icons when asked to. */
	@Inject(method = "renderEffects", at = @At("HEAD"), cancellable = true)
	private void xeys$hideEffects(GuiGraphics graphics, DeltaTracker delta, CallbackInfo ci) {
		if (PotionHudModule.hidesVanillaIcons()) {
			ci.cancel();
		}
	}
}
