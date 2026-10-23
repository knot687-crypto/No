package dev.xeys.visuals.mixin;

import dev.xeys.visuals.ui.menu.MenuHooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	/** Swaps every vanilla title screen for the XEYS moon menu, wherever it is created. */
	@ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
	private @Nullable Screen xeys$replaceTitleScreen(@Nullable Screen screen) {
		return MenuHooks.replace(screen);
	}
}
