package dev.xeys.visuals.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeAccessor {
	@Accessor("destroyBlockPos")
	BlockPos xeys$destroyBlockPos();

	@Accessor("destroyProgress")
	float xeys$destroyProgress();

	@Accessor("isDestroying")
	boolean xeys$isDestroying();
}
