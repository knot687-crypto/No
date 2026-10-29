package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.cosmetic.NameTags;
import net.minecraft.client.renderer.feature.NameTagFeatureRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(NameTagFeatureRenderer.Storage.class)
abstract class NameTagStorageMixin {
	@WrapOperation(method = "add", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;scale(FFF)V"))
	private void xeys$scale(PoseStack stack, float x, float y, float z, Operation<Void> original) {
		float s = NameTags.active ? NameTags.scale : 1f;
		original.call(stack, x * s, y * s, z * s);
	}

	/** The backing color; the bright text drawn over see-through tags keeps its empty backing. */
	@ModifyArg(method = "add", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/SubmitNodeStorage$NameTagSubmit;<init>(Lorg/joml/Matrix4f;FFLnet/minecraft/network/chat/Component;IIID)V"),
		index = 6)
	private int xeys$backing(int background) {
		return NameTags.active && background != 0 ? NameTags.backing : background;
	}
}
