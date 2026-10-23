package dev.xeys.visuals.mixin;

import dev.xeys.visuals.core.Keybinds;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardHandlerMixin {
	@Inject(method = "keyPress", at = @At("HEAD"))
	private void xeys$moduleKeybinds(long window, int action, KeyEvent event, CallbackInfo ci) {
		if (window == Minecraft.getInstance().getWindow().handle()) {
			Keybinds.onKey(event.key(), action);
		}
	}
}
