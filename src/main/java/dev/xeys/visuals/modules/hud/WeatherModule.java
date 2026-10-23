package dev.xeys.visuals.modules.hud;

import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.hud.HudPosition;
import dev.xeys.visuals.hud.HudStyle;
import dev.xeys.visuals.hud.InfoHudModule;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.ui.Icons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;

/** Current weather where you stand: clear, rain, snow or thunderstorm, with a matching icon. */
public final class WeatherModule extends InfoHudModule {
	private final BoolSetting intensity = this.add(new BoolSetting("intensity", "Intensity", false)
		.describe("How heavy the rain or snow is right now").group("Display"));
	private char weatherIcon = Icons.SUN;

	public WeatherModule() {
		super("weather", "Weather", Icons.CLOUD_SUN, "The weather where you stand", "", 2f, HudPosition.Anchor.TOP_RIGHT, -6f, 116f);
	}

	@Override
	protected void update(StringBuilder out, boolean preview) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || mc.player == null) {
			this.weatherIcon = Icons.CLOUD_RAIN;
			out.append("Rain");
			return;
		}
		float rain = level.getRainLevel(1f);
		boolean night = level.isDarkOutside();
		if (rain < 0.2f) {
			this.weatherIcon = level.dimensionType().hasFixedTime() ? Icons.CLOUD_FOG : (night ? Icons.MOON_STAR : Icons.SUN);
			out.append("Clear");
			return;
		}
		BlockPos pos = mc.player.blockPosition();
		Biome.Precipitation precipitation = level.getBiome(pos).value().getPrecipitationAt(pos, level.getSeaLevel());
		if (level.isThundering()) {
			this.weatherIcon = Icons.CLOUD_LIGHTNING;
			out.append("Thunderstorm");
		} else if (precipitation == Biome.Precipitation.SNOW) {
			this.weatherIcon = Icons.CLOUD_SNOW;
			out.append("Snow");
		} else if (precipitation == Biome.Precipitation.NONE) {
			this.weatherIcon = Icons.CLOUDY;
			out.append("Overcast");
		} else {
			this.weatherIcon = Icons.CLOUD_RAIN;
			out.append("Rain");
		}
		if (this.intensity.on()) {
			out.append("  ").append(Math.round(rain * 100f)).append('%');
		}
	}

	@Override
	protected void drawIcon(GuiGraphics g, float x, float centerY, float size) {
		Text.icon(g, this.weatherIcon, x + size * 0.5f, centerY, size, HudStyle.accent());
	}
}
