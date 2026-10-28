package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.TooltipStyleModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.tooltip.TooltipRenderUtil;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TooltipRenderUtil.class)
abstract class TooltipRenderUtilMixin {
	@Inject(method = "renderTooltipBackground", at = @At("HEAD"), cancellable = true)
	private static void xeys$tooltip(GuiGraphics graphics, int x, int y, int width, int height, @Nullable Identifier style, CallbackInfo ci) {
		if (TooltipStyleModule.render(graphics, x, y, width, height, style)) {
			ci.cancel();
		}
	}
}
