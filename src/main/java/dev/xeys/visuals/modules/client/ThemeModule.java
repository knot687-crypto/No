package dev.xeys.visuals.modules.client;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.blur.SceneBlur;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.Icons;

/** The client-wide accent color and glass material. */
public final class ThemeModule extends Module {
	public final ColorSetting accent = this.add(new ColorSetting("accent", "Accent Color", Theme.DEFAULT_ACCENT).noAlpha()
		.describe("One refined color used for highlights across the whole client").group("Color"));
	public final BoolSetting frosted = this.add(new BoolSetting("frosted", "Frosted Glass", true)
		.describe("Blur the scene behind panels. Turn off on very weak GPUs").group("Glass"));
	public final NumberSetting blur = this.add(new NumberSetting("blur", "Blur Strength", 4f, 1f, 6f, 1f)
		.describe("Each step roughly doubles the blur radius").group("Glass").visibleWhen(() -> this.frosted.on()));
	public final NumberSetting spread = this.add(new NumberSetting("spread", "Blur Softness", 1.6f, 0.8f, 3f, 0.1f)
		.group("Glass").visibleWhen(() -> this.frosted.on()));

	public ThemeModule() {
		super("theme", "Theme", Category.CLIENT, Icons.PALETTE, "Accent color and the glass material of every panel");
		this.alwaysOn();
		this.accent.onChange(c -> Theme.setAccent(c));
		this.frosted.onChange(v -> this.applyBlur());
		this.blur.onChange(v -> this.applyBlur());
		this.spread.onChange(v -> this.applyBlur());
	}

	private void applyBlur() {
		SceneBlur.configure(this.blur.intValue(), this.spread.value(), this.frosted.on());
	}

	@Override
	protected void onEnable() {
		Theme.setAccent(this.accent.base());
		this.applyBlur();
	}
}
