package dev.xeys.visuals.mixin;

import com.mojang.blaze3d.pipeline.RenderTarget;
import dev.xeys.visuals.modules.screen.ScreenshotFlashModule;
import java.io.File;
import java.util.function.Consumer;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Screenshot.class)
abstract class ScreenshotMixin {
	@Inject(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V", at = @At("HEAD"))
	private static void xeys$grab(File dir, @Nullable String name, RenderTarget target, int downscale, Consumer<Component> consumer, CallbackInfo ci) {
		ScreenshotFlashModule.onScreenshot(target);
	}
}
