package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.xeys.visuals.modules.screen.ScreenTransitionsModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(GameRenderer.class)
abstract class GameRendererScreenMixin {
	/** Wraps the active screen's rendering in its opening transition. */
	@WrapOperation(method = "render", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/screens/Screen;renderWithTooltipAndSubtitles(Lnet/minecraft/client/gui/GuiGraphics;IIF)V"))
	private void xeys$transition(Screen screen, GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Operation<Void> original) {
		boolean pushed = ScreenTransitionsModule.begin(graphics, screen);
		try {
			original.call(screen, graphics, mouseX, mouseY, partialTick);
		} finally {
			if (pushed) {
				graphics.pose().popMatrix();
			}
		}
	}
}
