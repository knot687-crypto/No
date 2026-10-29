package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.xeys.visuals.anim.HandFx;
import dev.xeys.visuals.anim.Zoom;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
abstract class ItemInHandRendererMixin {
	@Shadow
	private ItemStack mainHandItem;
	@Shadow
	private ItemStack offHandItem;

	@Shadow
	private boolean shouldInstantlyReplaceVisibleItem(ItemStack shown, ItemStack held) {
		throw new AssertionError();
	}

	/** How far the hands lag behind the camera. */
	@ModifyConstant(method = "renderHandsWithItems", constant = @Constant(floatValue = 0.1F))
	private float xeys$sway(float lag) {
		return lag * HandFx.sway;
	}

	@Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void xeys$hideWhileZoomed(float partialTick, PoseStack pose, SubmitNodeCollector collector, LocalPlayer player, int light, CallbackInfo ci) {
		if (Zoom.hidesHand()) {
			ci.cancel();
		}
	}

	@Inject(method = "renderArmWithItem", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", shift = At.Shift.AFTER))
	private void xeys$viewModel(AbstractClientPlayer player, float partialTick, float pitch, InteractionHand hand, float swing, ItemStack stack,
								float equip, PoseStack pose, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		HandFx.blockingNow = false;
		HumanoidArm arm = hand == InteractionHand.MAIN_HAND ? player.getMainArm() : player.getMainArm().getOpposite();
		HandFx.applyHand(pose, arm, hand);
	}

	/** The arm's rest transform; with block-hitting on, the sword then takes the old block pose. */
	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;applyItemArmTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/entity/HumanoidArm;F)V"))
	private void xeys$armTransform(ItemInHandRenderer self, PoseStack pose, HumanoidArm arm, float equip, Operation<Void> original,
								   @Local(argsOnly = true) AbstractClientPlayer player, @Local(argsOnly = true) InteractionHand hand,
								   @Local(argsOnly = true) ItemStack stack, @Local(argsOnly = true, ordinal = 2) float swing) {
		original.call(self, pose, arm, equip);
		if (HandFx.wantsBlock(player, hand, stack)) {
			HandFx.blockingNow = true;
			HandFx.applyBlock(pose, arm == HumanoidArm.RIGHT ? 1 : -1, swing);
		}
	}

	/** Swing styles; while block-hitting the swing is already part of the block pose. */
	@WrapOperation(method = "renderArmWithItem", at = @At(value = "INVOKE",
		target = "Lnet/minecraft/client/renderer/ItemInHandRenderer;swingArm(FLcom/mojang/blaze3d/vertex/PoseStack;ILnet/minecraft/world/entity/HumanoidArm;)V"))
	private void xeys$swing(ItemInHandRenderer self, float f, PoseStack pose, int side, HumanoidArm arm, Operation<Void> original) {
		if (HandFx.blockingNow) {
			return;
		}
		if (f > 0f && HandFx.swing(pose, f, side)) {
			return;
		}
		original.call(self, HandFx.smoothProgress(f), pose, side, arm);
	}

	/**
	 * With an equip style other than vanilla's dip, the shown item is replaced at once (vanilla then
	 * never lowers the hand for a switch) and the style's own animation starts.
	 */
	@Inject(method = "tick", at = @At("HEAD"))
	private void xeys$equip(CallbackInfo ci) {
		LocalPlayer player = Minecraft.getInstance().player;
		if (HandFx.equipStyle == HandFx.EquipStyle.VANILLA || player == null) {
			return;
		}
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (!this.shouldInstantlyReplaceVisibleItem(this.mainHandItem, main)) {
			HandFx.equipped(true);
		}
		if (!this.shouldInstantlyReplaceVisibleItem(this.offHandItem, off)) {
			HandFx.equipped(false);
		}
		this.mainHandItem = main;
		this.offHandItem = off;
	}

	/** The hand lowering while the attack recharges. */
	@ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemSwapScale(F)F"))
	private float xeys$cooldownDip(float recharge) {
		return HandFx.cooldownDip && !HandFx.oldNoCooldownDip ? recharge : 1f;
	}
}
