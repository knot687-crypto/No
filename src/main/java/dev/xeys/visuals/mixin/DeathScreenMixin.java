package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.DeathScreenModule;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DeathScreen.class)
abstract class DeathScreenMixin {
	@Shadow
	@Final
	private @Nullable Component causeOfDeath;

	@Shadow
	@Final
	private Component deathScore;

	@Shadow
	@Final
	private boolean hardcore;

	@Shadow
	private int delayTicker;

	@Inject(method = "init", at = @At("TAIL"))
	private void xeys$layout(CallbackInfo ci) {
		DeathScreenModule.arrange((DeathScreen) (Object) this);
	}

	@Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
	private void xeys$background(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		if (DeathScreenModule.drawBackground(graphics, (DeathScreen) (Object) this, this.delayTicker + partialTick)) {
			ci.cancel();
		}
	}

	@Inject(method = "visitText", at = @At("HEAD"), cancellable = true)
	private void xeys$text(ActiveTextCollector collector, CallbackInfo ci) {
		if (DeathScreenModule.replacesText()) {
			ci.cancel();
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void xeys$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		DeathScreenModule.drawText(graphics, (DeathScreen) (Object) this, this.causeOfDeath, this.deathScore, this.hardcore,
			this.delayTicker + partialTick);
	}
}
