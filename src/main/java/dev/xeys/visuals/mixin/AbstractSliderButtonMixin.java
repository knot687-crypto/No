package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.platform.cursor.CursorTypes;
import dev.xeys.visuals.modules.screen.WidgetStyleModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractSliderButton.class)
abstract class AbstractSliderButtonMixin {
	@Shadow
	protected double value;

	@Shadow
	private boolean dragging;

	@Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
	private void xeys$slider(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
		AbstractSliderButton self = (AbstractSliderButton) (Object) this;
		if (WidgetStyleModule.drawSlider(graphics, self, (float) this.value, this.dragging)) {
			if (self.isHovered()) {
				graphics.requestCursor(this.dragging ? CursorTypes.RESIZE_EW : CursorTypes.POINTING_HAND);
			}
			ci.cancel();
		}
	}
}
