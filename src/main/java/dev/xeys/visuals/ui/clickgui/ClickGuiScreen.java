package dev.xeys.visuals.ui.clickgui;

import dev.xeys.visuals.render.Draw;
import dev.xeys.visuals.render.text.Fonts;
import dev.xeys.visuals.render.text.Text;
import dev.xeys.visuals.theme.Theme;
import dev.xeys.visuals.ui.core.UiScale;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jspecify.annotations.Nullable;

/** Placeholder until the ClickGUI lands. */
public final class ClickGuiScreen extends Screen {
	private final @Nullable Screen parent;

	public ClickGuiScreen(@Nullable Screen parent) {
		super(Component.literal("XEYS Visuals"));
		this.parent = parent;
	}

	@Override
	public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
		UiScale.push(g);
		float W = UiScale.width();
		float H = UiScale.height();
		Draw.glass(g, W / 2 - 300, H / 2 - 80, 600, 160, Theme.GLASS_TINT).radius(Theme.R_XL).border(1f, Theme.GLASS_BORDER).submit();
		Text.drawMiddle(g, Fonts.SEMIBOLD, "XEYS Visuals", W / 2, H / 2 - 12, 20, Theme.TEXT, Text.Align.CENTER);
		Text.drawMiddle(g, Fonts.REGULAR, "Press Esc to go back", W / 2, H / 2 + 22, 13, Theme.TEXT_MUTED, Text.Align.CENTER);
		UiScale.pop(g);
	}

	@Override
	public void onClose() {
		this.minecraft.setScreen(this.parent);
	}
}
