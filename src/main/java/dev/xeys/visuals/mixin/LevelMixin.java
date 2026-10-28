package dev.xeys.visuals.mixin;

import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Visual time and weather overrides on the client level. What the client believes about real weather
 * ({@code isRaining}, used by e.g. Riptide) keeps reading the true values.
 */
@Mixin(Level.class)
abstract class LevelMixin {
	@Shadow
	protected float rainLevel;

	@Shadow
	protected float thunderLevel;

	@Shadow
	public abstract boolean canHaveWeather();

	@Inject(method = "getDayTime", at = @At("RETURN"), cancellable = true)
	private void xeys$dayTime(CallbackInfoReturnable<Long> cir) {
		if (WorldHooks.overridesDayTime() && (Object) this instanceof ClientLevel) {
			cir.setReturnValue(WorldHooks.dayTime());
		}
	}

	@Inject(method = "getRainLevel", at = @At("RETURN"), cancellable = true)
	private void xeys$rain(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (WorldHooks.overridesWeather() && (Object) this instanceof ClientLevel) {
			cir.setReturnValue(WorldHooks.rain());
		}
	}

	@Inject(method = "getThunderLevel", at = @At("RETURN"), cancellable = true)
	private void xeys$thunder(float partialTick, CallbackInfoReturnable<Float> cir) {
		if (WorldHooks.overridesWeather() && (Object) this instanceof ClientLevel) {
			cir.setReturnValue(WorldHooks.thunder());
		}
	}

	@Inject(method = "isRaining", at = @At("HEAD"), cancellable = true)
	private void xeys$isRaining(CallbackInfoReturnable<Boolean> cir) {
		if (WorldHooks.overridesWeather() && (Object) this instanceof ClientLevel) {
			cir.setReturnValue(this.canHaveWeather() && this.rainLevel > 0.2F);
		}
	}

	@Inject(method = "isThundering", at = @At("HEAD"), cancellable = true)
	private void xeys$isThundering(CallbackInfoReturnable<Boolean> cir) {
		if (WorldHooks.overridesWeather() && (Object) this instanceof ClientLevel) {
			cir.setReturnValue(this.canHaveWeather() && this.thunderLevel * this.rainLevel > 0.9F);
		}
	}
}
