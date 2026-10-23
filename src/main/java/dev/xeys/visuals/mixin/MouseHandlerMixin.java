package dev.xeys.visuals.mixin;

import dev.xeys.visuals.core.Keybinds;
import dev.xeys.visuals.core.input.InputStats;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
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
}
