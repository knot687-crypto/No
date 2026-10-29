package dev.xeys.visuals.mixin;

import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.client.renderer.WeatherEffectRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(WeatherEffectRenderer.class)
abstract class WeatherEffectRendererMixin {
	/** Visual weather override: draw this precipitation everywhere (the renderer only; biomes are untouched). */
	@Inject(method = "getPrecipitationAt", at = @At("RETURN"), cancellable = true)
	private void xeys$precipitation(Level level, BlockPos pos, CallbackInfoReturnable<Biome.Precipitation> cir) {
		Biome.Precipitation forced = WorldHooks.precipitation();
		if (forced != null && level.getChunkSource().hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
			cir.setReturnValue(forced);
		}
	}
}
