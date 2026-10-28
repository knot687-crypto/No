package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.WorldHooks;
import java.time.LocalTime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;

/** Shows the world at a time of your choosing (client only): a fixed hour, a time-lapse, or your real clock. */
public final class TimeChangerModule extends Module implements Events.FrameStart {
	public enum Mode {
		FIXED,
		TIME_LAPSE,
		REAL_TIME
	}

	private final EnumSetting<Mode> mode = this.add(new EnumSetting<>("mode", "Mode", Mode.FIXED).group("Time"));
	private final NumberSetting hour = this.add(new NumberSetting("hour", "Time of Day", 12f, 0f, 23.75f, 0.25f).unit("h").decimals(2)
		.describe("0 is midnight, 12 is noon").visibleWhen(() -> this.mode.is(Mode.FIXED)).group("Time"));
	private final NumberSetting speed = this.add(new NumberSetting("speed", "Speed", 20f, 2f, 200f, 1f).unit("x")
		.describe("How much faster the day runs").visibleWhen(() -> this.mode.is(Mode.TIME_LAPSE)).group("Time"));
	private double lapseOffset;

	public TimeChangerModule() {
		super("time_changer", "Time Changer", Category.WORLD, Icons.SUN_MOON, "See the world at any time of day (only for you)");
	}

	/** Ticks since 06:00 for an hour of the day (Minecraft's day starts at sunrise). */
	private static long ticksFor(double hourOfDay) {
		double h = ((hourOfDay - 6.0) % 24.0 + 24.0) % 24.0;
		return Math.round(h * 1000.0);
	}

	@Override
	public void onFrameStart() {
		ClientLevel level = Minecraft.getInstance().level;
		if (level == null) {
			WorldHooks.clearDayTime();
			return;
		}
		long real = level.getLevelData().getDayTime();
		long dayBase = Math.floorDiv(real, 24000L) * 24000L;
		long time = switch (this.mode.get()) {
			case FIXED -> dayBase + ticksFor(this.hour.value());
			case REAL_TIME -> {
				LocalTime now = LocalTime.now();
				yield dayBase + ticksFor(now.getHour() + now.getMinute() / 60.0 + now.getSecond() / 3600.0);
			}
			case TIME_LAPSE -> {
				this.lapseOffset += Clock.delta() * 20.0 * (this.speed.value() - 1f);
				this.lapseOffset %= 24000.0 * 8.0;
				yield real + (long) this.lapseOffset;
			}
		};
		WorldHooks.setDayTime(time);
	}

	@Override
	protected void onDisable() {
		WorldHooks.clearDayTime();
		this.lapseOffset = 0.0;
	}
}
