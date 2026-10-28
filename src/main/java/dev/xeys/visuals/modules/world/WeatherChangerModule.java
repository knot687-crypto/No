package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.anim.Clock;
import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.client.Minecraft;

/** Shows clear skies, rain or a thunderstorm regardless of the real weather (client only), easing between them. */
public final class WeatherChangerModule extends Module implements Events.FrameStart {
	public enum Weather {
		CLEAR,
		RAIN,
		THUNDERSTORM
	}

	private final EnumSetting<Weather> weather = this.add(new EnumSetting<>("weather", "Weather", Weather.CLEAR).group("Weather"));
	private float rain = -1f;
	private float thunder;

	public WeatherChangerModule() {
		super("weather_changer", "Weather Changer", Category.WORLD, Icons.CLOUD_SUN, "Choose the weather you see (only for you)");
	}

	@Override
	public void onFrameStart() {
		var level = Minecraft.getInstance().level;
		if (level == null) {
			WorldHooks.clearWeather();
			this.rain = -1f;
			return;
		}
		float targetRain = this.weather.is(Weather.CLEAR) ? 0f : 1f;
		float targetThunder = this.weather.is(Weather.THUNDERSTORM) ? 1f : 0f;
		if (this.rain < 0f) {
			// Start from the real weather so switching on eases instead of popping.
			WorldHooks.clearWeather();
			this.rain = level.getRainLevel(1f);
			this.thunder = level.getThunderLevel(1f);
		}
		float k = Clock.smoothing(1.2f);
		this.rain += (targetRain - this.rain) * k;
		this.thunder += (targetThunder - this.thunder) * k;
		WorldHooks.setWeather(this.rain, this.thunder);
	}

	@Override
	protected void onDisable() {
		WorldHooks.clearWeather();
		this.rain = -1f;
	}
}
