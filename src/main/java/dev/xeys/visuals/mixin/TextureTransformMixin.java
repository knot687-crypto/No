package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.client.renderer.rendertype.TextureTransform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureTransform.class)
abstract class TextureTransformMixin {
	@ModifyExpressionValue(method = "setupGlintTexturing", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;"))
	private static Object xeys$glintSpeed(Object speed) {
		return WorldHooks.glintSpeed == 1f ? speed : (Object) (((Double) speed) * WorldHooks.glintSpeed);
	}
}
