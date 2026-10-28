package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.PauseMenuModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.PauseScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PauseScreen.class)
abstract class PauseScreenMixin {
	@Inject(method = "init", at = @At("TAIL"))
	private void xeys$layout(CallbackInfo ci) {
		PauseMenuModule.arrange((PauseScreen) (Object) this);
	}

	@Inject(method = "renderBackground", at = @At("TAIL"))
	private void xeys$card(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		PauseMenuModule.decorate(graphics, (PauseScreen) (Object) this);
	}
}
