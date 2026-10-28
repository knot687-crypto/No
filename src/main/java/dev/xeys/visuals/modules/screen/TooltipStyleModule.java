package dev.xeys.visuals.modules.screen;

import dev.xeys.visuals.core.module.Category;
import dev.xeys.visuals.core.module.Module;
import dev.xeys.visuals.core.setting.BoolSetting;
import dev.xeys.visuals.core.setting.ColorSetting;
import dev.xeys.visuals.core.setting.NumberSetting;
import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.ui.Icons;
import dev.xeys.visuals.util.Colors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/** Rounded dark tooltips with a soft shadow and an accent edge, replacing the vanilla purple frame. */
public final class TooltipStyleModule extends Module {
	private static TooltipStyleModule instance;

	private final ColorSetting background = this.add(new ColorSetting("background", "Background", 0xF20C0C0F).group("Tooltip"));
	private final ColorSetting border = this.add(new ColorSetting("border", "Border", 0xFFFFFFFF, ColorSetting.Mode.ACCENT).group("Tooltip"));
	private final NumberSetting borderStrength = this.add(new NumberSetting("border_strength", "Border Strength", 55f, 0f, 100f, 1f).unit("%")
		.group("Tooltip"));
	private final NumberSetting radius = this.add(new NumberSetting("radius", "Corner Radius", 5f, 0f, 10f, 0.5f).unit("px").decimals(1)
		.group("Tooltip"));
	private final BoolSetting shadow = this.add(new BoolSetting("shadow", "Shadow", true).group("Tooltip"));
	private final BoolSetting keepCustom = this.add(new BoolSetting("keep_custom", "Keep Custom Styles", true)
		.describe("Items with their own tooltip style keep it").group("Tooltip"));

	public TooltipStyleModule() {
		super("tooltip_style", "Tooltip Style", Category.SCREEN, Icons.MESSAGE_SQUARE, "Rounded dark tooltips with an accent edge");
		instance = this;
	}

	/** Draws the tooltip background around content at (x, y, w, h); false keeps vanilla's. */
	public static boolean render(GuiGraphics g, int x, int y, int w, int h, @Nullable Identifier style) {
		TooltipStyleModule m = instance;
		if (m == null || !m.isEnabled() || (style != null && m.keepCustom.on())) {
			return false;
		}
		float bx = x - 4f;
		float by = y - 4f;
		float bw = w + 8f;
		float bh = h + 8f;
		float r = m.radius.value();
		if (m.shadow.on()) {
			Draw.shadow(g, bx, by, bw, bh, r, 0f, 3f, 10f, 0f, Colors.withAlpha(0x000000, 0.5f));
		}
		int edge = Colors.mulAlpha(m.border.color(), m.borderStrength.value() / 100f);
		var e = Draw.rect(g, bx, by, bw, bh).radius(r).color(m.background.color());
		if (Colors.alpha(edge) > 0) {
			e.border(1f, edge).borderFade(0.25f);
		}
		// The inner glow shares the border color: a faint wash of accent along the inside edge.
		e.innerGlow(6f, 0.08f).submit();
		return true;
	}
}
