package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Lens-style color fringing toward the corners, optionally pulsing when you are hit. */
public final class ChromaticAberrationModule extends ScreenEffectModule {
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 3f, 0.5f, 12f, 0.5f).unit("px").decimals(1)
		.describe("Color offset at the corners, at 1080p").group("Lens"));
	private final NumberSetting falloff = this.add(new NumberSetting("falloff", "Falloff", 1.8f, 0.5f, 4f, 0.1f).decimals(1)
		.describe("Higher keeps the center sharper").group("Lens"));
	private final BoolSetting hitPulse = this.add(new BoolSetting("hit_pulse", "Hit Pulse", true)
		.describe("The fringe briefly widens when you take damage").group("Lens"));
	private float pulse;

	public ChromaticAberrationModule() {
		super("chromatic_aberration", "Chromatic Aberration", Icons.CONTRAST, "Lens-like color fringing at the edges");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Player p = Minecraft.getInstance().player;
		float target = this.hitPulse.on() && p != null && p.hurtTime > 0 ? (p.hurtTime - partialTick) / 10f : 0f;
		this.pulse += (target - this.pulse) * Clock.smoothing(18f);
		float k = Minecraft.getInstance().getWindow().getHeight() / 1080f;
		fx.chroma = (this.strength.value() + this.pulse * 6f) * k;
		fx.chromaFalloff = this.falloff.value();
	}
}
