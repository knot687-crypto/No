package dev.xeys.visuals.modules.world;

import dev.xeys.visuals.core.event.Events;
import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.post.ScreenFx;
import dev.xeys.visuals.ui.Icons;

/**
 * Film-style color grading of the world: a choice of looks, or full control of white balance, vibrance,
 * saturation and contrast. Brightness stays within a small range so it never acts as a "fullbright".
 */
public final class ColorGradingModule extends Module implements Events.ScreenEffect {
	public enum Look {
		CUSTOM(0f, 0f, 0f, 1f, 1f, 0f),
		CINEMATIC(0.18f, -0.05f, 0.1f, 0.92f, 1.12f, -0.02f),
		VIBRANT(0.05f, 0f, 0.45f, 1.12f, 1.06f, 0.01f),
		MOODY(-0.25f, 0.06f, -0.1f, 0.8f, 1.15f, -0.04f),
		WARM(0.45f, 0.05f, 0.15f, 1f, 1.02f, 0.01f),
		COOL(-0.4f, -0.04f, 0.1f, 0.96f, 1.04f, 0f),
		PASTEL(0.1f, 0.06f, -0.2f, 0.85f, 0.88f, 0.04f);

		final float temperature;
		final float tint;
		final float vibrance;
		final float saturation;
		final float contrast;
		final float brightness;

		Look(float temperature, float tint, float vibrance, float saturation, float contrast, float brightness) {
			this.temperature = temperature;
			this.tint = tint;
			this.vibrance = vibrance;
			this.saturation = saturation;
			this.contrast = contrast;
			this.brightness = brightness;
		}
	}

	private final EnumSetting<Look> look = this.add(new EnumSetting<>("look", "Look", Look.CINEMATIC).group("Grade"));
	private final NumberSetting temperature = this.add(new NumberSetting("temperature", "Temperature", 0f, -100f, 100f, 1f)
		.describe("Cool to warm").visibleWhen(this::custom).group("White Balance"));
	private final NumberSetting tint = this.add(new NumberSetting("tint", "Tint", 0f, -100f, 100f, 1f).describe("Green to magenta")
		.visibleWhen(this::custom).group("White Balance"));
	private final NumberSetting vibrance = this.add(new NumberSetting("vibrance", "Vibrance", 0f, -100f, 100f, 1f).visibleWhen(this::custom)
		.group("Color"));
	private final NumberSetting saturation = this.add(new NumberSetting("saturation", "Saturation", 100f, 0f, 200f, 1f).unit("%")
		.visibleWhen(this::custom).group("Color"));
	private final NumberSetting contrast = this.add(new NumberSetting("contrast", "Contrast", 100f, 60f, 150f, 1f).unit("%")
		.visibleWhen(this::custom).group("Tone"));
	private final NumberSetting brightness = this.add(new NumberSetting("brightness", "Brightness", 0f, -10f, 10f, 0.5f).unit("%").decimals(1)
		.visibleWhen(this::custom).group("Tone"));
	private final NumberSetting strength = this.add(new NumberSetting("strength", "Strength", 100f, 0f, 100f, 1f).unit("%")
		.describe("How strongly the look is applied").visibleWhen(() -> !this.custom()).group("Grade"));

	public ColorGradingModule() {
		super("color_grading", "Color Grading", Category.WORLD, Icons.BLEND, "Cinematic color looks for the world");
	}

	private boolean custom() {
		return this.look.is(Look.CUSTOM);
	}

	@Override
	public void contribute(ScreenFx fx, float partialTick) {
		if (this.custom()) {
			fx.temperature += this.temperature.value() / 100f;
			fx.tintShift += this.tint.value() / 100f;
			fx.vibrance += this.vibrance.value() / 100f;
			fx.saturation *= this.saturation.value() / 100f;
			fx.contrast *= this.contrast.value() / 100f;
			fx.brightness += this.brightness.value() / 100f;
			return;
		}
		Look l = this.look.get();
		float s = this.strength.value() / 100f;
		fx.temperature += l.temperature * s;
		fx.tintShift += l.tint * s;
		fx.vibrance += l.vibrance * s;
		fx.saturation *= 1f + (l.saturation - 1f) * s;
		fx.contrast *= 1f + (l.contrast - 1f) * s;
		fx.brightness += l.brightness * s;
	}
}
