package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Fmt;
import java.time.LocalDateTime;
import java.time.format.TextStyle;
import java.util.Locale;

/** Real-world time, 12 or 24 hour, with optional seconds and date. */
public final class ClockModule extends InfoHudModule {
	private final BoolSetting twelveHour = this.add(new BoolSetting("twelve_hour", "12-Hour", false).group("Format"));
	private final BoolSetting seconds = this.add(new BoolSetting("seconds", "Seconds", false).group("Format"));
	private final BoolSetting date = this.add(new BoolSetting("date", "Date", false).group("Format"));

	public ClockModule() {
		super("clock", "Clock", Icons.CLOCK, "Your local time, with optional date", "", 2f, HudPosition.Anchor.TOP_RIGHT, -6f, 6f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		LocalDateTime now = LocalDateTime.now();
		if (this.date.on()) {
			out.append(now.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault())).append(' ').append(now.getDayOfMonth()).append(' ')
				.append(now.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())).append("  ");
		}
		int h = now.getHour();
		if (this.twelveHour.on()) {
			int h12 = h % 12 == 0 ? 12 : h % 12;
			out.append(h12);
		} else {
			Fmt.pad2(out, h);
		}
		out.append(':');
		Fmt.pad2(out, now.getMinute());
		if (this.seconds.on()) {
			out.append(':');
			Fmt.pad2(out, now.getSecond());
		}
		if (this.twelveHour.on()) {
			out.append(h < 12 ? " AM" : " PM");
		}
	}
}
