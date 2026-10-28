package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** A brief colored wash over the screen when you take damage. */
public final class DamageFlashModule extends ScreenEffectModule {
	private final ColorSetting color = this.add(new ColorSetting("color", "Color", 0xFFE53935).noAlpha().group("Flash"));
	private final NumberSetting intensity = this.add(new NumberSetting("intensity", "Intensity", 30f, 5f, 80f, 1f).unit("%").group("Flash"));
	private final NumberSetting duration = this.add(new NumberSetting("duration", "Duration", 0.35f, 0.1f, 1.2f, 0.05f).unit("s").decimals(2)
		.group("Flash"));
	private int lastHurtTime;
	private double flashAt = -10.0;

	public DamageFlashModule() {
		super("damage_flash", "Damage Flash", Icons.ZAP, "A quick colored flash when you are hurt");
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		Player p = Minecraft.getInstance().player;
		int hurt = p != null ? p.hurtTime : 0;
		if (hurt > this.lastHurtTime) {
			this.flashAt = Clock.time();
		}
		this.lastHurtTime = hurt;
		float t = (float) ((Clock.time() - this.flashAt) / this.duration.value());
		if (t >= 1f) {
			return;
		}
		float a = (1f - t) * (1f - t);
		fx.flashColor = this.color.color();
		fx.flash = Math.max(fx.flash, this.intensity.value() / 100f * a);
	}
}
