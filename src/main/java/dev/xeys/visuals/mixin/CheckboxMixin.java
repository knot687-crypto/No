package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.xeys.visuals.modules.screen.WidgetStyleModule;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Checkbox.class)
abstract class CheckboxMixin {
	@WrapOperation(method = "renderContents", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/gui/GuiGraphics;blitSprite(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIIII)V"))
	private void xeys$box(GuiGraphics graphics, RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h, int color,
						  Operation<Void> original) {
		Checkbox self = (Checkbox) (Object) this;
		if (!WidgetStyleModule.drawCheckbox(graphics, self, x, y, w, self.selected())) {
			original.call(graphics, pipeline, sprite, x, y, w, h, color);
		}
	}
}
