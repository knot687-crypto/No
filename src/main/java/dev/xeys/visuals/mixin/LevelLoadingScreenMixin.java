package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xeys.visuals.modules.screen.LoadingScreenModule;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.multiplayer.LevelLoadTracker;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.ChunkLoadStatusView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
abstract class LevelLoadingScreenMixin {
	@Shadow
	private LevelLoadTracker loadTracker;

	@Shadow
	private float smoothedProgress;

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;renderChunks(Lnet/minecraft/client/gui/GuiGraphics;IIIILnet/minecraft/server/level/progress/ChunkLoadStatusView;)V"))
	private void xeys$chunks(GuiGraphics graphics, int x, int y, int size, int gap, ChunkLoadStatusView view, Operation<Void> original) {
		if (!LoadingScreenModule.active()) {
			original.call(graphics, x, y, size, gap, view);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;drawCenteredString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;III)V"))
	private void xeys$label(GuiGraphics graphics, Font font, Component text, int x, int y, int color, Operation<Void> original) {
		if (!LoadingScreenModule.active()) {
			original.call(graphics, font, text, x, y, color);
		}
	}

	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;drawProgressBar(Lnet/minecraft/client/gui/GuiGraphics;IIIIF)V"))
	private void xeys$bar(LevelLoadingScreen self, GuiGraphics graphics, int x, int y, int w, int h, float progress, Operation<Void> original) {
		if (!LoadingScreenModule.active()) {
			original.call(self, graphics, x, y, w, h, progress);
		}
	}

	@Inject(method = "render", at = @At("TAIL"))
	private void xeys$render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		LoadingScreenModule.draw(graphics, (LevelLoadingScreen) (Object) this, this.loadTracker.hasProgress() ? this.smoothedProgress : -1f);
	}
}
