package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/** Movement speed in blocks per second, smoothed over ticks. */
public final class SpeedModule extends InfoHudModule implements Events.ClientTick {
	private final BoolSetting vertical = this.add(new BoolSetting("vertical", "Include Vertical", false).group("Display"));
	private double lastX;
	private double lastY;
	private double lastZ;
	private double smoothed;
	private boolean primed;

	public SpeedModule() {
		super("speed", "Speed", Icons.WIND, "Movement speed in blocks per second", "Speed", 10f, HudPosition.Anchor.TOP_LEFT, 6f, 170f);
	}

	@Override
	public void onClientTick() {
		Player p = Minecraft.getInstance().player;
		if (p == null) {
			this.primed = false;
			return;
		}
		if (this.primed) {
			double dx = p.getX() - this.lastX;
			double dy = this.vertical.on() ? p.getY() - this.lastY : 0.0;
			double dz = p.getZ() - this.lastZ;
			double bps = Math.sqrt(dx * dx + dy * dy + dz * dz) * 20.0;
			this.smoothed += (bps - this.smoothed) * 0.35;
		}
		this.lastX = p.getX();
		this.lastY = p.getY();
		this.lastZ = p.getZ();
		this.primed = true;
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		double v = Minecraft.getInstance().player == null ? 5.61 : this.smoothed;
		Fmt.fixed(out, v < 0.05 ? 0.0 : v, 2).append(" b/s");
	}
}
