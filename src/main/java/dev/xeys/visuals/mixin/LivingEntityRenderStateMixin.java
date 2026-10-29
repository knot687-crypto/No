package dev.xeys.visuals.mixin;

import dev.xeys.visuals.anim.LivingStateAccess;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LivingEntityRenderState.class)
abstract class LivingEntityRenderStateMixin implements LivingStateAccess {
	@Unique
	private float xeys$hurtProgress;

	@Override
	public float xeys$hurt() {
		return this.xeys$hurtProgress;
	}

	@Override
	public void xeys$setHurt(float hurt) {
		this.xeys$hurtProgress = hurt;
	}
}
