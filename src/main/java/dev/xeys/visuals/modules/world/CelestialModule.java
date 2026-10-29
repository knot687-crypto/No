package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.world.WorldHooks;
import net.minecraft.world.attribute.EnvironmentAttribute;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;

/** Sun and moon size, and the moon phase you see (client only). */
public final class CelestialModule extends Module implements Events.Environment {
	public enum Phase {
		REAL(null),
		FULL(MoonPhase.FULL_MOON),
		WANING_GIBBOUS(MoonPhase.WANING_GIBBOUS),
		THIRD_QUARTER(MoonPhase.THIRD_QUARTER),
		WANING_CRESCENT(MoonPhase.WANING_CRESCENT),
		NEW(MoonPhase.NEW_MOON),
		WAXING_CRESCENT(MoonPhase.WAXING_CRESCENT),
		FIRST_QUARTER(MoonPhase.FIRST_QUARTER),
		WAXING_GIBBOUS(MoonPhase.WAXING_GIBBOUS);

		final MoonPhase phase;

		Phase(MoonPhase phase) {
			this.phase = phase;
		}
	}

	private final NumberSetting sun = this.add(new NumberSetting("sun_size", "Sun Size", 100f, 40f, 300f, 5f).unit("%").onChange(v -> this.apply())
		.group("Sun"));
	private final NumberSetting moon = this.add(new NumberSetting("moon_size", "Moon Size", 140f, 40f, 300f, 5f).unit("%").onChange(v -> this.apply())
		.group("Moon"));
	private final EnumSetting<Phase> phase = this.add(new EnumSetting<>("phase", "Moon Phase", Phase.REAL).group("Moon"));

	public CelestialModule() {
		super("celestial", "Sun & Moon", Category.WORLD, Icons.MOON_STAR, "Resize the sun and moon, pick the moon phase");
	}

	private void apply() {
		if (this.isEnabled()) {
			WorldHooks.sunScale = this.sun.value() / 100f;
			WorldHooks.moonScale = this.moon.value() / 100f;
		}
	}

	@Override
	public Object modifyEnvironment(EnvironmentAttribute<?> attribute, Object value) {
		MoonPhase p = this.phase.get().phase;
		if (p != null && attribute == EnvironmentAttributes.MOON_PHASE) {
			return p;
		}
		return value;
	}

	@Override
	protected void onEnable() {
		this.apply();
	}

	@Override
	protected void onDisable() {
		WorldHooks.sunScale = 1f;
		WorldHooks.moonScale = 1f;
	}
}
