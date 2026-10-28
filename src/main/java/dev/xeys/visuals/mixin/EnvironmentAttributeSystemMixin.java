package dev.xeys.visuals.mixin;

import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributeSystem;
import net.minecraft.world.attribute.SpatialAttributeInterpolator;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EnvironmentAttributeSystem.class)
abstract class EnvironmentAttributeSystemMixin {
	/** Visual environment overrides for the client world only (never the integrated server's). */
	@Inject(method = "getValue(Lnet/minecraft/world/attribute/EnvironmentAttribute;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/attribute/SpatialAttributeInterpolator;)Ljava/lang/Object;",
		at = @At("RETURN"), cancellable = true)
	private void xeys$value(EnvironmentAttribute<?> attribute, Vec3 pos, @Nullable SpatialAttributeInterpolator interpolator,
							CallbackInfoReturnable<Object> cir) {
		if (WorldHooks.hasEnvironmentModifiers() && WorldHooks.isClientSystem(this)) {
			cir.setReturnValue(WorldHooks.modify(attribute, cir.getReturnValue()));
		}
	}

	@Inject(method = "getDimensionValue", at = @At("RETURN"), cancellable = true)
	private void xeys$dimensionValue(EnvironmentAttribute<?> attribute, CallbackInfoReturnable<Object> cir) {
		if (WorldHooks.hasEnvironmentModifiers() && WorldHooks.isClientSystem(this)) {
			cir.setReturnValue(WorldHooks.modify(attribute, cir.getReturnValue()));
		}
	}
}
