package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/** A soft colored light along the screen edges, optionally breathing. */
public final class AmbientGlowModule extends ScreenEffectModule {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).noAlpha().group("Glow"));
	private final NumberSetting intensity = this.add(new NumberSetting("intensity", "Intensity", 22f, 5f, 80f, 1f).unit("%").group("Glow"));
	private final BoolSetting breathing = this.add(new BoolSetting("breathing", "Breathing", true).describe("Slowly brightens and fades")
		.group("Glow"));

	public AmbientGlowModule() {
		super("ambient_glow", "Ambient Glow", Icons.SUN_DIM, "Soft colored light along the screen edges");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		float b = this.breathing.on() ? 0.65f + 0.35f * (float) Math.sin(Clock.time() * Math.PI * 2.0 / 6.0) : 1f;
		fx.glowColor = this.color.color();
		fx.glow = this.intensity.value() / 100f * b;
	}
}
