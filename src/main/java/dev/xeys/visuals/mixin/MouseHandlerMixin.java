package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.xeys.visuals.anim.AnimHooks;
import dev.xeys.visuals.anim.Zoom;
import dev.xeys.visuals.core.Keybinds;
import dev.xeys.visuals.core.input.InputStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.player.LocalPlayer;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Inject(method = "onButton", at = @At("HEAD"))
	private void xeys$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window != mc.getWindow().handle()) {
			return;
		}
		if (action == GLFW.GLFW_PRESS && mc.screen == null) {
			InputStats.onClick(info.button());
		}
		Keybinds.onMouse(info.button(), action);
	}

	/** Look smoothing and zoom sensitivity, applied to each frame's mouse motion. */
	@WrapOperation(method = "turnPlayer", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void xeys$turn(LocalPlayer player, double dx, double dy, Operation<Void> original, @Local(argsOnly = true) double frameSeconds) {
		AnimHooks.turn(player, dx, dy, frameSeconds);
	}

	/** While zooming, the wheel changes the zoom instead of the hotbar slot. */
	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void xeys$zoomScroll(long window, double dx, double dy, CallbackInfo ci) {
		Minecraft mc = Minecraft.getInstance();
		if (window == mc.getWindow().handle() && mc.screen == null && Zoom.scroll(dy)) {
			ci.cancel();
		}
	}
}
