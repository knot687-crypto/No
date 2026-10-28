package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.material.FogType;

/** A gentle refraction ripple (and optional teal tint) while your camera is under water. */
public final class UnderwaterRippleModule extends ScreenEffectModule {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 3f, 0.5f, 10f, 0.5f).unit("px").decimals(1)
		.describe("At 1080p").group("Ripple"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 1f, 0.2f, 3f, 0.1f).unit("x").decimals(1).group("Ripple"));
	private final BoolSetting tint = this.add(new BoolSetting("tint", "Teal Tint", true).group("Ripple"));
	private float amount;

	public UnderwaterRippleModule() {
		super("underwater_ripple", "Underwater Ripple", Icons.WAVES, "Soft refraction while under water");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Minecraft mc = Minecraft.getInstance();
		boolean under = mc.gameRenderer.getMainCamera().getFluidInCamera() == FogType.WATER;
		this.amount += ((under ? 1f : 0f) - this.amount) * Clock.smoothing(5f);
		if (this.amount < 0.002f) {
			return;
		}
		// The shader scales the ripple with the resolution.
		fx.wave = this.strength.value() * this.amount;
		fx.waveSpeed = this.speed.value();
		if (this.tint.on() && fx.tintAmount <= 0f) {
			fx.tint = 0xFF3FB6B0;
			fx.tintAmount = 0.25f * this.amount;
		}
	}
}
