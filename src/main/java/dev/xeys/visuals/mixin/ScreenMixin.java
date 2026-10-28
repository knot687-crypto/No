package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.InventoryBackgroundModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screen.class)
abstract class ScreenMixin {
	/** The dark gradient behind in-game screens (inventories, containers) can be restyled. */
	@Inject(method = "renderTransparentBackground", at = @At("HEAD"), cancellable = true)
	private void xeys$background(GuiGraphics graphics, CallbackInfo ci) {
		if (InventoryBackgroundModule.render(graphics, (Screen) (Object) this)) {
			ci.cancel();
		}
	}
}
