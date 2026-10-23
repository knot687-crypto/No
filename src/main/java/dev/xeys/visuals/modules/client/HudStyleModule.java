package dev.xeys.visuals.modules.client;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.EnumSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.ui.Icons;

/** Global look of every HUD element: one place to restyle the whole HUD. */
public final class HudStyleModule extends Module {
	public enum Background {
		GLASS,
		SOLID,
		OUTLINE,
		NONE
	}

	public enum Weight {
		REGULAR,
		MEDIUM,
		SEMIBOLD,
		BOLD
	}

	public final EnumSetting<Background> background = this.add(new EnumSetting<>("background", "Background", Background.GLASS)
		.describe("Frosted glass, solid surface, outline only or no panel").group("Panel"));
	public final NumberSetting opacity = this.add(new NumberSetting("opacity", "Opacity", 70f, 0f, 100f, 1f).unit("%")
		.describe("Strength of the panel tint").group("Panel"));
	public final NumberSetting radius = this.add(new NumberSetting("radius", "Corner Radius", 5f, 0f, 12f, 0.5f).unit("px").group("Panel"));
	public final BoolSetting border = this.add(new BoolSetting("border", "Highlight Border", true).describe("Thin light edge along the top").group("Panel"));
	public final BoolSetting shadow = this.add(new BoolSetting("shadow", "Drop Shadow", true).group("Panel"));
	public final NumberSetting padding = this.add(new NumberSetting("padding", "Padding", 5f, 2f, 10f, 0.5f).unit("px").group("Panel"));

	public final ColorSetting text = this.add(new ColorSetting("text", "Text Color", 0xFFF4F4F6).group("Text"));
	public final ColorSetting accent = this.add(new ColorSetting("accent", "Accent Color", 0xFFFFFFFF, ColorSetting.Mode.ACCENT)
		.describe("Highlights, icons and bars. Follows the theme accent by default").group("Text"));
	public final EnumSetting<Weight> weight = this.add(new EnumSetting<>("weight", "Font Weight", Weight.MEDIUM).group("Text"));
	public final NumberSetting textSize = this.add(new NumberSetting("text_size", "Text Size", 8.5f, 6f, 12f, 0.25f).unit("px").group("Text"));
	public final BoolSetting textShadow = this.add(new BoolSetting("text_shadow", "Text Shadow", false)
		.describe("Soft shadow behind text; useful without a panel").group("Text"));

	public final BoolSetting hideInDebug = this.add(new BoolSetting("hide_in_debug", "Hide With Debug Screen", true).group("Behavior"));

	public HudStyleModule() {
		super("hud_style", "HUD Style", Category.CLIENT, Icons.PAINTBRUSH, "Panels, text and colors shared by every HUD element");
		this.alwaysOn();
	}
}
