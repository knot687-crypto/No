package dev.xeys.visuals.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.world.level.block.LeavesBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LeavesBlock.class)
abstract class LeavesBlockMixin {
	/** Client-side ambience only: animateTick runs on the client level. */
	@ModifyExpressionValue(method = "makeFallingLeavesParticles", at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/LeavesBlock;leafParticleChance:F"))
	private float xeys$leafChance(float chance) {
		return Math.min(1f, chance * WorldHooks.leafChance);
	}
}
