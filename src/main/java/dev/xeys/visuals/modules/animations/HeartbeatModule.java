package dev.xeys.visuals.modules.animations;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.world.CameraFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** At low health the view pulses with a heartbeat, a quick double beat that speeds up as health drops. */
public final class HeartbeatModule extends Module implements Events.FrameStart {
	private final NumberSetting threshold = this.add(new NumberSetting("threshold", "Below", 3f, 1f, 10f, 0.5f).unit(" hearts").decimals(1)
		.group("Trigger"));
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 25f, 200f, 5f).unit("%").group("Pulse"));
	private final BoolSetting quicken = this.add(new BoolSetting("quicken", "Speed Up", true)
		.describe("The beat quickens as health drops").group("Pulse"));
	private float severity;
	private double phase;
	private float pulse;

	public HeartbeatModule() {
		super("heartbeat", "Heartbeat", Category.ANIMATIONS, Icons.HEART, "The view pulses with your heartbeat at low health");
		CameraFx.addFovModifier(partial -> 1f - this.pulse);
	}

	@Override
	public void onFrameStart() {
		Player p = Minecraft.getInstance().player;
		float target = 0f;
		if (p != null && p.isAlive() && !p.isCreative() && !p.isSpectator()) {
			float limit = this.threshold.value() * 2f;
			float health = p.getHealth() + p.getAbsorptionAmount();
			if (health < limit) {
				target = (float) Math.sqrt(1f - health / limit);
			}
		}
		this.severity += (target - this.severity) * Clock.smoothing(4f);
		if (this.severity < 0.002f) {
			this.pulse = 0f;
			return;
		}
		float bpm = this.quicken.on() ? 66f + 64f * this.severity : 80f;
		this.phase = (this.phase + Clock.delta() * bpm / 60f) % 1.0;
		float t = (float) this.phase;
		// "Lub-dub": a strong beat and a softer one just after.
		float lub = (float) Math.exp(-Math.pow((t - 0.06f) / 0.045f, 2.0));
		float dub = 0.55f * (float) Math.exp(-Math.pow((t - 0.26f) / 0.055f, 2.0));
		this.pulse = (lub + dub) * 0.035f * this.strength.value() / 100f * this.severity;
	}

	@Override
	protected void onDisable() {
		this.pulse = 0f;
		this.severity = 0f;
	}
}
