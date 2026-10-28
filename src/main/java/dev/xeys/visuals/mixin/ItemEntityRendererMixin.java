package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.world.DroppedItems;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import org.joml.Quaternionfc;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemEntityRenderer.class)
abstract class ItemEntityRendererMixin {
	/** The bob: lift, height and scale of a dropped item. */
	@WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(FFF)V", ordinal = 0))
	private void xeys$bob(PoseStack pose, float x, float y, float z, Operation<Void> original, @Local(argsOnly = true) ItemEntityRenderState state) {
		if (!DroppedItems.transformBob(pose, state, y)) {
			original.call(pose, x, y, z);
		}
	}

	/** The spin: replaced by a resting rotation when items lie flat. */
	@WrapOperation(method = "submit", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;mulPose(Lorg/joml/Quaternionfc;)V", ordinal = 0))
	private void xeys$spin(PoseStack pose, Quaternionfc rotation, Operation<Void> original, @Local(argsOnly = true) ItemEntityRenderState state) {
		if (!DroppedItems.transformSpin(pose, state)) {
			original.call(pose, rotation);
		}
	}
}
