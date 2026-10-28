package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Below a health threshold the edges glow red with a heartbeat and colors drain; stronger as health drops. */
public final class LowHealthModule extends ScreenEffectModule {
	private final NumberSetting threshold = this.add(new NumberSetting("threshold", "Below", 4f, 1f, 10f, 0.5f).unit(" hearts").decimals(1)
		.group("Trigger"));
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFB3121B).noAlpha().group("Look"));
	private final NumberSetting intensity = this.add(new NumberSetting("intensity", "Intensity", 75f, 10f, 100f, 1f).unit("%").group("Look"));
	private final NumberSetting desaturate = this.add(new NumberSetting("desaturate", "Drain Color", 45f, 0f, 100f, 1f).unit("%")
		.describe("How much color fades at the lowest health").group("Look"));
	private final BoolSetting heartbeat = this.add(new BoolSetting("heartbeat", "Heartbeat", true)
		.describe("The glow beats, faster as health drops").group("Look"));
	private float severity;
	private double phase;

	public LowHealthModule() {
		super("low_health", "Low Health Effect", Icons.HEART_PULSE, "A red heartbeat at the edges when health runs low");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Player p = Minecraft.getInstance().player;
		float target = 0f;
		if (p != null && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
			float limit = this.threshold.value() * 2f;
			float health = p.getHealth() + p.getAbsorptionAmount();
			if (health < limit) {
				target = 1f - health / limit;
			}
		}
		this.severity += (target - this.severity) * Clock.smoothing(4f);
		if (this.severity < 0.002f) {
			return;
		}
		float s = this.severity;
		float beat = 1f;
		if (this.heartbeat.on()) {
			// "Lub-dub": two pulses per beat, from 70 to 140 beats per minute.
			this.phase = (this.phase + Clock.delta() * (70f + 70f * s) / 60f) % 1.0;
			float t = (float) this.phase;
			float lub = (float) Math.exp(-Math.pow((t - 0.08f) / 0.05f, 2.0));
			float dub = 0.6f * (float) Math.exp(-Math.pow((t - 0.3f) / 0.06f, 2.0));
			beat = 0.7f + 0.45f * (lub + dub);
		}
		fx.edgeColor = this.color.color();
		fx.edge = Math.max(fx.edge, this.intensity.value() / 100f * (0.35f + 0.65f * s) * beat);
		fx.saturation *= 1f - this.desaturate.value() / 100f * s;
	}
}
