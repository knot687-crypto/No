package dev.xeys.visuals.mixin;

import dev.xeys.visuals.modules.screen.WidgetStyleModule;
import java.util.function.Supplier;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractButton.class)
abstract class AbstractButtonMixin {
	@Shadow
	private @Nullable Supplier<Boolean> overrideRenderHighlightedSprite;

	/** The standard button background (most vanilla buttons, cycle buttons, option buttons). */
	@Inject(method = "renderDefaultSprite", at = @At("HEAD"), cancellable = true)
	private void xeys$sprite(GuiGraphics graphics, CallbackInfo ci) {
		AbstractButton self = (AbstractButton) (Object) this;
		boolean highlighted = this.overrideRenderHighlightedSprite != null ? this.overrideRenderHighlightedSprite.get() : self.isHoveredOrFocused();
		if (WidgetStyleModule.drawButton(graphics, self, highlighted)) {
			ci.cancel();
		}
	}

	/** Its label, drawn in the client font, only for buttons whose background was just restyled. */
	@Inject(method = "renderDefaultLabel", at = @At("HEAD"), cancellable = true)
	private void xeys$label(ActiveTextCollector collector, CallbackInfo ci) {
		if (WidgetStyleModule.drawButtonLabel((AbstractButton) (Object) this)) {
			ci.cancel();
		}
	}
}
