package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.CameraMotion;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** The camera shakes when you are hurt, when explosions go off nearby and when you land hard. */
public final class CameraShakeModule extends Module {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 10f, 200f, 5f).unit("%")
		.onChange(v -> this.apply()).group("Shake"));
	private final BoolSetting hurt = this.add(new BoolSetting("hurt", "When Hurt", true).onChange(v -> this.apply()).group("Shake"));
	private final BoolSetting explosions = this.add(new BoolSetting("explosions", "Explosions", true)
		.describe("Stronger the closer the blast").onChange(v -> this.apply()).group("Shake"));
	private final BoolSetting landing = this.add(new BoolSetting("landing", "Hard Landings", true)
		.describe("After falls of four blocks or more").onChange(v -> this.apply()).group("Shake"));

	public CameraShakeModule() {
		super("camera_shake", "Camera Shake", Category.ANIMATIONS, Icons.VIBRATE, "Shake on hits, explosions and hard landings");
	}

	private void apply() {
		CameraMotion.shake = this.isEnabled() ? this.strength.value() / 100f : 0f;
		CameraMotion.shakeOnHurt = this.hurt.on();
		CameraMotion.shakeOnExplosion = this.explosions.on();
		CameraMotion.shakeOnLanding = this.landing.on();
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		CameraMotion.shake = 0f;
	}
}
