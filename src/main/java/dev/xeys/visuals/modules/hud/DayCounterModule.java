package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;
import net.minecraft.client.Minecraft;

/** In-game day number and time of day. */
public final class DayCounterModule extends InfoHudModule {
	private final BoolSetting time = this.add(new BoolSetting("time", "Time of Day", true).group("Display"));

	public DayCounterModule() {
		super("day_counter", "Day Counter", Icons.SUN_MOON, "In-game day and time of day", "Day", 2f, HudPosition.Anchor.TOP_RIGHT, -6f, 72f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		var level = Minecraft.getInstance().level;
		long dayTime = level != null ? level.getDayTime() : 30000L;
		out.append(dayTime / 24000L + 1L);
		if (this.time.on()) {
			// Minecraft day starts at 06:00.
			long ticks = (dayTime % 24000L + 6000L) % 24000L;
			int hours = (int) (ticks / 1000L);
			int minutes = (int) ((ticks % 1000L) * 60L / 1000L);
			out.append("  ");
			Fmt.pad2(out, hours).append(':');
			Fmt.pad2(out, minutes);
		}
	}
}
