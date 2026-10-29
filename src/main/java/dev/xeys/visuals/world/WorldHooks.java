package dev.xeys.visuals.world;

import dev.xeys.visuals.core.event.Events;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.level.biome.Biome;
import org.jspecify.annotations.Nullable;

/**
 * Client-only visual overrides of the world: time of day, weather and environment attributes. Only the
 * client level is ever affected; the integrated server's world (same JVM) is never touched.
 */
public final class WorldHooks {
	/** Overridden day time, or negative for none. */
	private static long dayTime = -1L;
	/** Overridden rain and thunder levels (0..1), or negative for none. */
	private static float rain = -1f;
	private static float thunder = -1f;
	/** Precipitation drawn everywhere while weather is overridden, or null for each biome's own. */
	private static Biome.@Nullable Precipitation precipitation;

	/** Sun and moon size multipliers. */
	public static float sunScale = 1f;
	public static float moonScale = 1f;
	/** Multiplier on how often leaves drift from leaf blocks. */
	public static float leafChance = 1f;
	/** Enchantment glint speed and strength multipliers. */
	public static float glintSpeed = 1f;
	public static float glintStrength = 1f;

	private WorldHooks() {
	}

	public static void setDayTime(long time) {
		dayTime = time;
	}

	public static void clearDayTime() {
		dayTime = -1L;
	}

	public static void setWeather(float rainLevel, float thunderLevel) {
		rain = rainLevel;
		thunder = thunderLevel;
	}

	public static void clearWeather() {
		rain = -1f;
		thunder = -1f;
		precipitation = null;
	}

	public static void setPrecipitation(Biome.@Nullable Precipitation type) {
		precipitation = type;
	}

	public static Biome.@Nullable Precipitation precipitation() {
		return rain >= 0f ? precipitation : null;
	}

	public static boolean overridesDayTime() {
		return dayTime >= 0L;
	}

	public static long dayTime() {
		return dayTime;
	}

	public static boolean overridesWeather() {
		return rain >= 0f;
	}

	public static float rain() {
		return rain;
	}

	public static float thunder() {
		return thunder;
	}

	/** Whether {@code system} is the client level's attribute system (the server has its own). */
	public static boolean isClientSystem(Object system) {
		ClientLevel level = Minecraft.getInstance().level;
		return level != null && level.environmentAttributes() == system;
	}

	public static boolean hasEnvironmentModifiers() {
		return !Events.ENVIRONMENT.isEmpty();
	}

	public static Object modify(EnvironmentAttribute<?> attribute, Object value) {
		Object v = value;
		for (Object l : Events.ENVIRONMENT.snapshot()) {
			v = ((Events.Environment) l).modifyEnvironment(attribute, v);
		}
		return v;
	}
}
